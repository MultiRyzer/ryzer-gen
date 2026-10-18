package com.ryzer.ryzergen.machine.fission;

import com.ryzer.ryzergen.machine.pool.HotFuel;
import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModDataComponents;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The fission station's core, as numbers (design section 8, "The core puzzle"). A 5 x 5 grid of
 * channels, each planned as fuel, graphite (a moderator), water (coolant) or, in the MOX core, a
 * control rod, or left empty. Shared by the block entity (which runs it), the control screen (which
 * scores the plan as it is drawn) and the game tests, so all three use the same sums.
 *
 * <p>The station has two cores, picked on the screen's tabs, each a puzzle with one provable best
 * (art/concepts/station_screen/solver):
 * <ul>
 * <li><b>Uranium</b> (uranium or U-233 rods): a fuel rod needs water on every side it has, and makes
 * 2 heat, +1 for each graphite on its corners. A rod on the edge has fewer sides, so it needs less
 * water. The best is the lattice, 34 heat: fuel on the nine even-even channels, graphite on the
 * four odd-odd ones, water on the rest. A plain chessboard scores 26.</li>
 * <li><b>MOX</b> (MOX rods only): the same water rule; a rod makes 3 heat, +2 for each graphite and +1
 * for each MOX rod on its corners; a rod over 4 heat needs a control rod on a corner, or it runs
 * away (scores nothing, its heat stranded); every control rod costs 1 heat. The best, 47 heat, is a
 * cross: rods on the corners and round the middle, graphite on the edge middles, one control rod in
 * the centre taming the four hot rods round it. Without the cost there were three tied bests.</li>
 * </ul>
 * Real basis: a moderated thermal reactor. Graphite slows the neutrons the rods shed, so the rods it
 * sits between split more atoms; water both cools and moderates, so a rod with a dry side runs away.
 * Plutonium in MOX fuel makes it more reactive and its neutrons couple rods together, so a MOX core
 * runs hotter and leans on control rods to hold its hot spots, and each rod soaks up neutrons that
 * would otherwise make heat. The diagonal reach is the fudge that makes the puzzles.
 *
 * <p>Heat is in thermal FE per tick: a uranium heat unit is 590 (the lattice makes 20,060, about
 * 8,000 FE/t out of the turbine), U-233's two and a half times that, a MOX unit 850 (the cross makes
 * 39,950, about 16,000 FE/t out). A valid rod's heat goes, shared evenly, to the water channels round
 * it; an uncooled or runaway rod's heat reaches no water and stays in the core, heating it towards a
 * SCRAM. The harder the working water carries heat, the hotter the core settles and the more
 * efficient the turbine (Carnot): 25% idle, 40% at full load, which each best exactly reaches.
 *
 * <p>Overdrive (safeties off): the control rods come further out, so every rod makes 30% more heat
 * and burns 30% faster, and the steam leaves superheated: the turbine reaches 45% instead of 40%.
 */
public final class StationReactor {
    public static final int GRID = 5;
    public static final int CHANNELS = GRID * GRID;

    /** Thermal FE per tick one heat unit makes at 100% output: uranium, MOX and U-233. */
    public static final float URANIUM_HEAT = 590;
    public static final float MOX_HEAT = 850;
    public static final float U233_HEAT = URANIUM_HEAT * 2.5F;
    /** Ticks a rod lasts burning at 100%: uranium burns fast (and its spent rods breed plutonium), MOX lasts. */
    public static final int URANIUM_LIFE = 36_000;
    public static final int MOX_LIFE = 144_000;
    public static final int U233_LIFE = 72_000;

    /** The station's two cores, each its own puzzle with its own plan. New cores go at the end: saved by position. */
    public enum Core {
        /** 2 heat, +1 a graphite corner. The lattice: 34. */
        URANIUM(2, 1, 0, 99, 0, 34, 28, "FWFWFWMWMWFWFWFWMWMWFWFWF", URANIUM_HEAT),
        /** 3 heat, +2 a graphite corner, +1 a MOX rod corner; over 4 needs a control rod; each costs 1. The cross: 47. */
        MOX(3, 2, 1, 4, 1, 47, 40, "FWMWFWFWFWMWCWMWFWFWFWMWF", MOX_HEAT);

        public final int base;
        public final int graphite;
        public final int coupling;
        /** Heat a rod may make without a control rod on a corner. */
        public final int limit;
        public final int controlCost;
        public final int best;
        public final int twoStars;
        /** The best layout: F fuel, M graphite, W water, C control rod, row by row. */
        public final String layout;
        /** Thermal FE per tick a heat unit makes, for this core's own fuel. */
        public final float unit;

        Core(int base, int graphite, int coupling, int limit, int controlCost, int best, int twoStars, String layout, float unit) {
            this.base = base;
            this.graphite = graphite;
            this.coupling = coupling;
            this.limit = limit;
            this.controlCost = controlCost;
            this.best = best;
            this.twoStars = twoStars;
            this.layout = layout;
            this.unit = unit;
        }

        public static Core byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : URANIUM;
        }

        /** The fuels this core burns: uranium and U-233 in the uranium core, MOX in its own. */
        public boolean burns(Fuel fuel) {
            return this == MOX ? fuel == Fuel.MOX : fuel != Fuel.MOX;
        }

        /** Water channels in the best layout, which carry its heat exactly evenly: twelve in both. */
        int bestWater() {
            return (int) layout.chars().filter(c -> c == 'W').count();
        }
    }

    /** The fuels a station burns: each rod, the spent rod it becomes, its heat per unit and its life. */
    public enum Fuel {
        URANIUM(URANIUM_HEAT, URANIUM_LIFE),
        MOX(MOX_HEAT, MOX_LIFE),
        /** U-233 in thorium (the thorium line, design section 9): no plutonium when spent. */
        U233(U233_HEAT, U233_LIFE);

        public final float heat;
        public final int life;

        Fuel(float heat, int life) {
            this.heat = heat;
            this.life = life;
        }

        public Item rod() {
            return switch (this) {
                case URANIUM -> ModItems.URANIUM_FUEL_ROD.get();
                case MOX -> ModItems.MOX_FUEL_ROD.get();
                case U233 -> ModItems.U233_FUEL_ROD.get();
            };
        }

        public Item spent() {
            return switch (this) {
                case URANIUM -> ModItems.SPENT_URANIUM_ROD.get();
                case MOX -> ModItems.SPENT_MOX_ROD.get();
                case U233 -> ModItems.SPENT_U233_ROD.get();
            };
        }

        /** The fuel a fresh rod is, or null. */
        public static @Nullable Fuel of(ItemStack stack) {
            for (Fuel fuel : values()) {
                if (stack.is(fuel.rod())) {
                    return fuel;
                }
            }
            return null;
        }
    }

    /**
     * Thousands of heat-ticks a lithium target rod needs to be fully bred, for its item bar. Target
     * rods breed in the breeder's blanket now; the station no longer takes them.
     */
    public static final int TARGET_WORK = 60_000;
    public static final float IDLE_TEMPERATURE = 150;
    public static final float FULL_LOAD_TEMPERATURE = 600;
    /** Overdrive: more heat and burn per rod. */
    public static final float OVERDRIVE_HEAT = 1.3F;

    /** What a channel is planned to hold. Coolant channels are pipes of water; empty ones do nothing. */
    public enum Channel {
        // New channel types go at the end: plans are saved by position in this list. TARGET is
        // retired (target rods breed in the breeder): a plan holding one loads as empty, and its rod
        // leaves by the output port. CONTROL is the MOX core's; an older world's uranium plan
        // holding one loads it as empty the same way.
        EMPTY, FUEL, MODERATOR, CONTROL, COOLANT, TARGET;

        public static Channel byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : EMPTY;
        }

        /** Whether a plan for {@code core} may hold this. */
        public boolean plannable(Core core) {
            return this == EMPTY || this == FUEL || this == MODERATOR || this == COOLANT || (this == CONTROL && core == Core.MOX);
        }

        /** Whether an item may go in a channel of this type, in {@code core}. */
        public boolean accepts(ItemStack stack, Core core) {
            return switch (this) {
                case FUEL -> {
                    Fuel fuel = Fuel.of(stack);
                    yield fuel != null && core.burns(fuel);
                }
                case MODERATOR -> stack.is(ModItems.GRAPHITE_BLOCK.get());
                case CONTROL -> core == Core.MOX && stack.is(ModItems.CONTROL_ROD.get());
                default -> false;
            };
        }
    }

    /**
     * A layout worked out. Per channel: {@code units} the heat units a rod scores (0 if it fails),
     * {@code rodHeat} the heat units a planned rod would make (so the screen can show a failing rod's),
     * {@code cooled} whether it has water on every side, {@code wild} whether it is cooled but over
     * the limit with no control rod on a corner (MOX), {@code heat} the thermal FE/t a live rod makes
     * (failing or not), {@code burn} how hard it burns, {@code load} the heat each water channel
     * carries. Then the totals: {@code score} the valid rods' heat units less the control rods' cost
     * (the puzzle's number, against the core's best), {@code rods} live rods, {@code uncooled} those
     * without water on every side, {@code runaway} those over the limit with no control rod,
     * {@code hot} those over the limit, {@code graphite} and {@code controls} in place,
     * {@code generation} all the heat made, {@code stranded} the failing rods' heat (any at all and
     * the core cannot hold steady), {@code working} water channels carrying heat, {@code coolants}
     * water channels planned, {@code capacity} what each can carry in this core.
     */
    public record Analysis(Core core, int[] units, int[] rodHeat, boolean[] cooled, boolean[] wild, float[] heat, float[] burn,
                           float[] load, int score, int rods, int uncooled, int runaway, int hot, int graphite, int controls,
                           float generation, float stranded, int working, int coolants, float capacity, float heatScale,
                           float averageLife, boolean overdrive) {
        /** Live fuel rods. */
        public int fuelRods() {
            return rods;
        }

        /** Rods that fail: dry or running away. */
        public int failing() {
            return uncooled + runaway;
        }

        /** The fraction of the working water's capacity in use, which sets the core's temperature. */
        public float loadFraction() {
            return working == 0 ? 0 : (generation - stranded) / (working * capacity);
        }

        public boolean holdsSteady() {
            return generation > 0 && stranded <= 0.5F;
        }

        /** The temperature the core settles at, running this layout. */
        public float settledTemperature() {
            return IDLE_TEMPERATURE + (FULL_LOAD_TEMPERATURE - IDLE_TEMPERATURE) * Math.min(1, loadFraction());
        }

        /** Power out once settled, or 0 if it cannot hold steady. */
        public float plannedOutput() {
            return holdsSteady() ? generation * efficiency(settledTemperature(), overdrive) : 0;
        }

        /** Minutes the average rod lasts in this layout. */
        public float rodMinutes() {
            if (rods == 0) {
                return 0;
            }
            float burnt = 0;
            for (float b : burn) {
                burnt += b;
            }
            return averageLife / (burnt / rods) / 1200F;
        }

        /** The score against the best, 0 to 1. */
        public float rating() {
            return Mth.clamp(score / (float) core.best, 0, 1);
        }

        /** Stars: none until every rod holds, one then, two near the best, three at it. */
        public int stars() {
            if (rods == 0 || failing() > 0) {
                return 0;
            }
            return score >= core.best ? 3 : score >= core.twoStars ? 2 : 1;
        }
    }

    private StationReactor() {}

    private static float multiplier() {
        return Config.get(Config.STATION_OUTPUT) / 100F;
    }

    /**
     * Thermal FE per tick one water channel carries at full load: the core's best heat over its
     * twelve water channels (which share it exactly evenly), so each best settles at full load (600°C,
     * 40%). Scaled by the config's output setting and by the fuel's heat against the core's own.
     */
    public static float capacity(Core core, boolean overdrive, float heatScale) {
        return core.unit * core.best / core.bestWater() * multiplier() * (overdrive ? OVERDRIVE_HEAT : 1) * heatScale;
    }

    public static boolean isFreshFuel(ItemStack stack) {
        return Fuel.of(stack) != null;
    }

    public static boolean isSpent(ItemStack stack) {
        for (Fuel fuel : Fuel.values()) {
            if (stack.is(fuel.spent())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether an item in a channel planned as {@code type} is done with and leaves by the output
     * port: a spent rod, or anything the plan no longer takes (such as a target rod from an older
     * world, or the other core's parts).
     */
    public static boolean leaving(Channel type, ItemStack held, Core core) {
        return !held.isEmpty() && (isSpent(held) || !type.accepts(held, core));
    }

    public static int life(ItemStack rod) {
        Fuel fuel = Fuel.of(rod);
        return fuel == null ? URANIUM_LIFE : fuel.life;
    }

    /** Ticks of burn left in a rod (a fresh one has its full life). */
    public static int fuelLeft(ItemStack rod) {
        return rod.getOrDefault(ModDataComponents.FUEL_LEFT.get(), life(rod));
    }

    /** The spent rod a fuel rod becomes. */
    public static ItemStack spentFor(ItemStack rod) {
        Fuel fuel = Fuel.of(rod);
        return HotFuel.mark(new ItemStack((fuel == null ? Fuel.URANIUM : fuel).spent()));
    }

    /**
     * Works out a layout for {@code core}. {@code rods} holds each channel's item: a fuel channel
     * counts only with a live rod the core burns, a graphite or control channel only with its part;
     * water needs no item. With {@code rods} null it works out the plan itself, every part in place
     * and every rod the core's own fuel: the puzzle the screen scores.
     */
    public static Analysis analyse(Channel[] types, @Nullable ItemStack[] rods, boolean overdrive, Core core) {
        float boost = overdrive ? OVERDRIVE_HEAT : 1;
        int[] units = new int[CHANNELS];
        int[] rodHeat = new int[CHANNELS];
        boolean[] cooled = new boolean[CHANNELS];
        boolean[] wild = new boolean[CHANNELS];
        float[] heat = new float[CHANNELS];
        float[] burn = new float[CHANNELS];
        float[] load = new float[CHANNELS];
        Fuel[] fuels = new Fuel[CHANNELS];
        for (int i = 0; i < CHANNELS; i++) {
            fuels[i] = fuel(types, rods, i, core);
        }
        int valid = 0;
        int count = 0;
        int uncooled = 0;
        int runaway = 0;
        int hot = 0;
        int graphite = 0;
        int controls = 0;
        int coolants = 0;
        float stranded = 0;
        float heatScale = 0;
        float averageLife = 0;
        for (int i = 0; i < CHANNELS; i++) {
            if (types[i] == Channel.COOLANT) {
                coolants++;
            } else if (present(types, rods, i, Channel.MODERATOR, core)) {
                graphite++;
            } else if (present(types, rods, i, Channel.CONTROL, core)) {
                controls++;
            }
            if (types[i] != Channel.FUEL) {
                continue;
            }
            boolean wet = true;
            for (int n : neighbours(i)) {
                wet &= types[n] == Channel.COOLANT;
            }
            int u = core.base;
            boolean tamed = false;
            for (int n : corners(i)) {
                if (present(types, rods, n, Channel.MODERATOR, core)) {
                    u += core.graphite;
                } else if (fuels[n] != null) {
                    u += core.coupling;
                } else if (present(types, rods, n, Channel.CONTROL, core)) {
                    tamed = true;
                }
            }
            rodHeat[i] = u;
            cooled[i] = wet;
            boolean over = u > core.limit;
            Fuel fuel = fuels[i];
            if (fuel == null) {
                continue;
            }
            float scale = fuel.heat / core.unit;
            heat[i] = u * fuel.heat * multiplier() * boost;
            burn[i] = boost;
            count++;
            heatScale += scale;
            averageLife += fuel.life;
            hot += over ? 1 : 0;
            if (!wet) {
                uncooled++;
                stranded += heat[i];
            } else if (over && !tamed) {
                wild[i] = true;
                runaway++;
                stranded += heat[i];
            } else {
                units[i] = u;
                valid += u;
            }
        }
        int score = valid - controls * core.controlCost;
        // Control rods soak up neutrons: the valid rods' heat comes down by their cost, shared out.
        float trim = valid <= 0 ? 0 : Math.max(0, score) / (float) valid;
        float generation = stranded;
        for (int i = 0; i < CHANNELS; i++) {
            if (units[i] > 0) {
                heat[i] *= trim;
                generation += heat[i];
                int[] next = neighbours(i);
                for (int n : next) {
                    load[n] += heat[i] / next.length;
                }
            }
        }
        int working = 0;
        for (float l : load) {
            working += l > 0 ? 1 : 0;
        }
        heatScale = count == 0 ? 1 : heatScale / count;
        averageLife = count == 0 ? 0 : averageLife / count;
        // A core whose rods share their water more tightly than the best's carries the extra too,
        // at full load: the only ways to strand heat are a dry side and a runaway rod, which the screen shows.
        float capacity = Math.max(capacity(core, overdrive, heatScale), working == 0 ? 0 : (generation - stranded) / working);
        return new Analysis(core, units, rodHeat, cooled, wild, heat, burn, load, score, count, uncooled, runaway, hot, graphite,
                controls, generation, stranded, working, coolants, capacity, heatScale, averageLife, overdrive);
    }

    /** The plan alone, as the screen scores it. */
    public static Analysis plan(Channel[] types, Core core) {
        return analyse(types, null, false, core);
    }

    /** Whether channel {@code i} is planned as {@code type} and (with items given) holds its part. */
    private static boolean present(Channel[] types, @Nullable ItemStack[] rods, int i, Channel type, Core core) {
        return types[i] == type && type.plannable(core) && (rods == null || type.accepts(rods[i], core));
    }

    /** The fuel burning in channel {@code i}, or null; the core's own fuel for every planned rod when working out the plan alone. */
    private static @Nullable Fuel fuel(Channel[] types, @Nullable ItemStack[] rods, int i, Core core) {
        if (types[i] != Channel.FUEL) {
            return null;
        }
        if (rods == null) {
            return core == Core.MOX ? Fuel.MOX : Fuel.URANIUM;
        }
        Fuel fuel = Fuel.of(rods[i]);
        return fuel != null && core.burns(fuel) ? fuel : null;
    }

    /** A plan from a string of F (fuel), M (graphite), W (water), C (control rod) and anything else for empty, row by row. */
    public static Channel[] parse(String layout) {
        Channel[] plan = new Channel[CHANNELS];
        for (int i = 0; i < CHANNELS; i++) {
            char c = i < layout.length() ? layout.charAt(i) : '.';
            plan[i] = switch (c) {
                case 'F' -> Channel.FUEL;
                case 'M' -> Channel.MODERATOR;
                case 'W' -> Channel.COOLANT;
                case 'C' -> Channel.CONTROL;
                default -> Channel.EMPTY;
            };
        }
        return plan;
    }

    /** The up to four channels beside channel {@code i}. */
    public static int[] neighbours(int i) {
        int x = i % GRID;
        int z = i / GRID;
        int[] out = new int[4];
        int n = 0;
        if (x > 0) out[n++] = i - 1;
        if (x < GRID - 1) out[n++] = i + 1;
        if (z > 0) out[n++] = i - GRID;
        if (z < GRID - 1) out[n++] = i + GRID;
        return java.util.Arrays.copyOf(out, n);
    }

    /** The up to four channels on the corners of channel {@code i}. */
    public static int[] corners(int i) {
        int x = i % GRID;
        int z = i / GRID;
        int[] out = new int[4];
        int n = 0;
        for (int dz = -1; dz <= 1; dz += 2) {
            for (int dx = -1; dx <= 1; dx += 2) {
                if (x + dx >= 0 && x + dx < GRID && z + dz >= 0 && z + dz < GRID) {
                    out[n++] = i + dx + dz * GRID;
                }
            }
        }
        return java.util.Arrays.copyOf(out, n);
    }

    /**
     * Turbine efficiency: better the hotter the core (Carnot), from 25% idle to 40% at full load, or
     * 45% in overdrive, where the steam leaves superheated.
     */
    public static float efficiency(float temperature, boolean overdrive) {
        return 0.25F + (overdrive ? 0.20F : 0.15F) * coolingRamp(temperature);
    }

    /** How much of the coolant's capacity the core's heat can drive: none at 150°C, all at 600°C. */
    public static float coolingRamp(float temperature) {
        return Mth.clamp((temperature - IDLE_TEMPERATURE) / (FULL_LOAD_TEMPERATURE - IDLE_TEMPERATURE), 0, 1);
    }
}
