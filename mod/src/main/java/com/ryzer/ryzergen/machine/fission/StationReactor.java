package com.ryzer.ryzergen.machine.fission;

import com.ryzer.ryzergen.machine.pool.HotFuel;
import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModDataComponents;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The fission station's core, as numbers (design section 8). A 5 x 5 grid of channels, each planned
 * as fuel, moderator, control rod or coolant, or left empty. Shared by the block entity (which runs
 * it) and the control screen (which previews a layout as it is arranged), so both use the same sums.
 *
 * <p>Heat is in thermal FE per tick. Each fuel rod makes a base amount, changed by what is beside
 * it: a moderator adds 40% (it slows neutrons so more of them split atoms), a neighbouring fuel rod
 * 20% (shared neutrons), a control rod takes 35% off (it soaks neutrons up). A rod burns faster with
 * fuel beside it and slower beside control rods, but a moderator's extra heat costs no extra fuel:
 * better neutron economy, so graphite is how you get more energy out of each rod.
 *
 * <p>Heat is carried away only by coolant channels touching the rod, shared evenly between them, up
 * to each channel's capacity. Heat with no coolant beside it, or more than a channel can carry, stays
 * in the core and heats it. The harder the coolant works, the hotter the core settles and the more
 * efficient the turbine (Carnot): 25% idle, 40% with the coolant at full load. So the most power
 * sits just below overheating, and control rods trim a layout back under the limit.
 * Real basis: moderated thermal reactors, simplified into a light layout puzzle.
 *
 * <p>Overdrive (safeties off): the control rods come further out, so every rod makes 30% more heat
 * and burns 30% faster. The hotter core drives 30% more heat through each coolant channel, so a
 * layout that holds steady still does, and the steam leaves superheated: the turbine reaches 45%
 * instead of 40%. About half as much power again, from the same layout.
 *
 * <p>MOX: each rod makes twice a uranium rod's heat, and the coolant carries more in step, up to
 * twice as much in an all-MOX core (scaled by the core's average heat per rod; U-233, the third
 * fuel, makes two and a half times a uranium rod's heat but lasts only an hour). So a MOX core plays exactly
 * like the same uranium core at double the power: same best layouts, same temperatures. Real basis:
 * plutonium-bearing fuel runs at a higher power density, and hotter fuel drives more heat into the
 * water (heat flux rises with the temperature difference). The factor of two is a game fudge, so
 * the fuel cycle is worth closing.
 *
 * <p>Lithium target rods (design section 10) breed tritium for fusion from the fuel beside them.
 * They soak up neutrons too, so that fuel makes 20% less heat and burns 20% slower. Real basis:
 * tritium-producing burnable absorber rods, which make the United States' tritium in a power reactor.
 */
public final class StationReactor {
    public static final int GRID = 5;
    public static final int CHANNELS = GRID * GRID;

    /** Thermal FE per tick a fuel rod makes on its own. MOX runs twice as hot. */
    public static final float URANIUM_HEAT = 1000;
    public static final float MOX_HEAT = 2000;
    public static final float U233_HEAT = 2500;
    /** Ticks a rod lasts burning at 100%: uranium burns fast (and its spent rods breed plutonium), MOX lasts. */
    public static final int URANIUM_LIFE = 36_000;
    public static final int MOX_LIFE = 144_000;
    public static final int U233_LIFE = 72_000;

    /**
     * The fuels a station burns: each rod, the spent rod it becomes, its heat and its life. The
     * coolant carries more in step with a rod's heat, so every fuel plays the same layouts.
     */
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
        public static Fuel of(ItemStack stack) {
            for (Fuel fuel : values()) {
                if (stack.is(fuel.rod())) {
                    return fuel;
                }
            }
            return null;
        }
    }
    /** Thermal FE per tick one coolant channel can carry away. */
    public static final float COOLANT_CAPACITY = 3000;
    private static final float MODERATOR_BONUS = 0.4F;
    private static final float FUEL_BONUS = 0.2F;
    private static final float CONTROL_CUT = 0.35F;
    /** What a lithium target rod takes off the fuel beside it: it soaks up neutrons too, less than a control rod. */
    private static final float TARGET_CUT = 0.2F;
    /**
     * Thousands of heat-ticks a target rod needs from the fuel beside it to be fully bred: about 20
     * minutes beside two uranium rods, 5 beside four MOX rods.
     */
    public static final int TARGET_WORK = 60_000;
    private static final float MIN_FACTOR = 0.1F;
    public static final float IDLE_TEMPERATURE = 150;
    public static final float FULL_LOAD_TEMPERATURE = 600;
    /**
     * The most power found for a core of each fuel (searching layouts offline with
     * art/tools/core_optimiser.py), for the screen's rating. At 100% output.
     */
    public static final float BEST_URANIUM = 8006;
    public static final float BEST_MOX = 16012;
    /** The temperatures those best layouts settle at, to scale them for overdrive. */
    private static final float BEST_URANIUM_TEMPERATURE = 544;
    /** Overdrive: more heat and burn per rod, and more heat carried per coolant channel. */
    public static final float OVERDRIVE_HEAT = 1.3F;

    /** What a channel is planned to hold. Coolant channels are pipes of water; empty ones do nothing. */
    public enum Channel {
        // New channel types go at the end: plans are saved by position in this list. TARGET is
        // retired (target rods breed in the breeder now) and can no longer be planned, but stays so
        // a station planned with one in an older world still loads and runs.
        EMPTY, FUEL, MODERATOR, CONTROL, COOLANT, TARGET;

        public static Channel byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : EMPTY;
        }

        /** Whether an item may go in a channel of this type. */
        public boolean accepts(ItemStack stack) {
            return switch (this) {
                case FUEL -> isFreshFuel(stack);
                case MODERATOR -> stack.is(ModItems.GRAPHITE_BLOCK.get());
                case CONTROL -> stack.is(ModItems.CONTROL_ROD.get());
                case TARGET -> stack.is(ModItems.LITHIUM_TARGET_ROD.get());
                case COOLANT, EMPTY -> false;
            };
        }
    }

    /**
     * A layout worked out: per channel, the heat each fuel rod makes, how hard it burns, and the load
     * on each coolant channel; then the totals. {@code stranded} is heat no coolant can carry (a rod
     * with no coolant beside it, or more than a channel's capacity): any at all and the core cannot
     * hold steady. {@code working} is how many coolant channels carry heat, {@code capacity} how much
     * each can carry in this core. {@code breed} is, per target channel, the fuel heat beside it,
     * which is how fast its lithium turns into tritium. {@code heatScale} is the fuel's average heat
     * against uranium's (1 all uranium, 2 all MOX), {@code averageLife} its average life in ticks.
     */
    public record Analysis(float[] heat, float[] burn, float[] load, float[] breed, float generation, float stranded,
                           int working, int coolants, int fuelRods, float heatScale, float averageLife, boolean overdrive,
                           float capacity) {
        /** Minutes a fresh target rod takes to breed in channel {@code i}, or 0 if nothing is breeding it. */
        public float breedMinutes(int i) {
            return breed[i] <= 0 ? 0 : TARGET_WORK * 1000F / breed[i] / 1200F;
        }

        /** The fraction of the working coolant's capacity in use, which sets the core's temperature. */
        public float loadFraction() {
            return working == 0 ? 0 : generation / (working * capacity);
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
            if (fuelRods == 0) {
                return 0;
            }
            float burnt = 0;
            for (float b : burn) {
                burnt += b;
            }
            return averageLife / (burnt / fuelRods) / 1200F;
        }

        /** Planned power against the best found for this fuel, 0 to 1. */
        public float rating() {
            if (fuelRods == 0) {
                return 0;
            }
            // Every fuel plays the uranium layouts scaled by its heat (MOX's best is uranium's doubled).
            float best = BEST_URANIUM * heatScale * multiplier();
            if (overdrive) {
                // The same layout is best in overdrive (heat and cooling scale together), just stronger.
                best *= OVERDRIVE_HEAT * efficiency(BEST_URANIUM_TEMPERATURE, true) / efficiency(BEST_URANIUM_TEMPERATURE, false);
            }
            return Math.min(1, plannedOutput() / best);
        }
    }

    private StationReactor() {}

    private static float multiplier() {
        return Config.get(Config.STATION_OUTPUT) / 100F;
    }

    /**
     * Thermal FE per tick one coolant channel can carry, after the config's output setting, in a
     * core whose fuel averages {@code heatScale} times a uranium rod's heat.
     */
    public static float capacity(boolean overdrive, float heatScale) {
        return COOLANT_CAPACITY * multiplier() * (overdrive ? OVERDRIVE_HEAT : 1) * heatScale;
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

    /** Done with the core and ready to leave by the output port: a spent fuel rod or a bred target rod. */
    public static boolean isFinished(ItemStack stack) {
        return isSpent(stack) || stack.is(ModItems.IRRADIATED_TARGET_ROD.get());
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
     * Works out a layout. {@code rods} holds each channel's item: a channel counts only when its
     * item is in place (a moderator channel with no graphite does nothing); coolant needs no item.
     */
    public static Analysis analyse(Channel[] types, ItemStack[] rods, boolean overdrive) {
        float boost = overdrive ? OVERDRIVE_HEAT : 1;
        float[] heat = new float[CHANNELS];
        float[] burn = new float[CHANNELS];
        float[] load = new float[CHANNELS];
        float generation = 0;
        int fuelRods = 0;
        float heatScale = 0;
        float averageLife = 0;
        int coolants = 0;
        for (int i = 0; i < CHANNELS; i++) {
            if (types[i] == Channel.COOLANT) {
                coolants++;
            }
            if (types[i] != Channel.FUEL || !isFreshFuel(rods[i])) {
                continue;
            }
            float heatFactor = 1;
            float burnFactor = 1;
            for (int n : neighbours(i)) {
                if (!types[n].accepts(rods[n])) {
                    continue;
                }
                switch (types[n]) {
                    case MODERATOR -> heatFactor += MODERATOR_BONUS;
                    case FUEL -> {
                        heatFactor += FUEL_BONUS;
                        burnFactor += FUEL_BONUS;
                    }
                    case CONTROL -> {
                        heatFactor -= CONTROL_CUT;
                        burnFactor -= CONTROL_CUT;
                    }
                    case TARGET -> {
                        heatFactor -= TARGET_CUT;
                        burnFactor -= TARGET_CUT;
                    }
                    default -> { }
                }
            }
            Fuel fuel = Fuel.of(rods[i]);
            heat[i] = fuel.heat * Math.max(MIN_FACTOR, heatFactor) * multiplier() * boost;
            burn[i] = Math.max(MIN_FACTOR, burnFactor) * boost;
            generation += heat[i];
            fuelRods++;
            heatScale += fuel.heat / URANIUM_HEAT;
            averageLife += fuel.life;
        }
        // Each rod's heat goes, shared evenly, to the coolant channels beside it.
        float stranded = 0;
        for (int i = 0; i < CHANNELS; i++) {
            if (heat[i] <= 0) {
                continue;
            }
            int[] next = neighbours(i);
            int cooled = 0;
            for (int n : next) {
                cooled += types[n] == Channel.COOLANT ? 1 : 0;
            }
            if (cooled == 0) {
                stranded += heat[i];
                continue;
            }
            for (int n : next) {
                if (types[n] == Channel.COOLANT) {
                    load[n] += heat[i] / cooled;
                }
            }
        }
        // Each target rod breeds from the heat of the fuel beside it.
        float[] breed = new float[CHANNELS];
        for (int i = 0; i < CHANNELS; i++) {
            if (types[i] != Channel.TARGET || !types[i].accepts(rods[i])) {
                continue;
            }
            for (int n : neighbours(i)) {
                breed[i] += heat[n];
            }
        }
        heatScale = fuelRods == 0 ? 1 : heatScale / fuelRods;
        averageLife = fuelRods == 0 ? 0 : averageLife / fuelRods;
        float capacity = capacity(overdrive, heatScale);
        int working = 0;
        for (int i = 0; i < CHANNELS; i++) {
            if (load[i] > 0) {
                working++;
                stranded += Math.max(0, load[i] - capacity);
            }
        }
        return new Analysis(heat, burn, load, breed, generation, stranded, working, coolants, fuelRods, heatScale, averageLife,
                overdrive, capacity);
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
