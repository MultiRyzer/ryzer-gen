package com.ryzer.ryzergen.machine.breeder;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.machine.fission.StationReactor;
import com.ryzer.ryzergen.machine.pool.HotFuel;
import com.ryzer.ryzergen.registry.ModDataComponents;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The breeder's core, as numbers (design section 9). A hexagonal lattice of 19 positions, as fast
 * reactor cores are built: a centre, a ring of 6 round it and a ring of 12 outside that. Each is
 * planned as fuel, a control rod, or a blanket (uranium, which breeds plutonium, or a lithium target
 * rod, which breeds tritium), or left empty. Shared by the block entity (which runs it) and the
 * control screen (which previews a layout as it is arranged), so both use the same sums.
 *
 * <p>Heat is in thermal FE per tick. A fuel assembly makes a base amount, 10% more for each fuel
 * assembly beside it (fast neutrons shared across the core) and 30% less for each control rod. A
 * fast reactor has no moderator: the neutrons stay fast, which is what lets it breed. Packed fuel is
 * also thriftier: the shared neutrons raise an assembly's burn by only half as much as its heat, as
 * a bigger core leaks fewer neutrons. But no assembly may run past the hot spot limit (13,000), the
 * most heat its pins can pass to the sodium: any more stays in the core, which then cannot hold
 * steady. So fuel can be packed up to three abreast, or more with a control rod to trim it. Real
 * basis: a fuel pin's linear heat rating, the limit every fast reactor core is designed round.
 *
 * <p>Breeding: every blanket assembly catches the spare neutrons of the fuel beside it, so it breeds
 * at a rate set by that fuel's heat. A blanket costs the fuel nothing (its neutrons would otherwise
 * leak out of the core), so the choice is which blanket, and where: fuel for the stations
 * (plutonium) or for fusion (tritium). Spread fuel touches more blankets and breeds more; packed
 * fuel burns less. Real basis: the breeding blankets round a fast reactor core.
 *
 * <p>Cooling is not local, as in the station: the liquid sodium flows through the whole core, so the
 * loop carries away up to its capacity, set by the pumps' flow (0 to 100%) and how full the loop
 * is. The harder the sodium works the hotter the core settles and the more efficient the steam
 * plant: 30% idle, 42% at full load. The pumps cost power, the square of their flow. So the best
 * setting sits just under full load: enough flow to carry the heat and no more.
 */
public final class BreederReactor {
    /** Hex rows of the lattice, from the back row to the front: 3, 4, 5, 4, 3. */
    public static final int RADIUS = 2;
    public static final int POSITIONS = 19;
    private static final int[] Q = new int[POSITIONS];
    private static final int[] R = new int[POSITIONS];

    static {
        int i = 0;
        for (int r = -RADIUS; r <= RADIUS; r++) {
            for (int q = Math.max(-RADIUS, -r - RADIUS); q <= Math.min(RADIUS, -r + RADIUS); q++) {
                Q[i] = q;
                R[i] = r;
                i++;
            }
        }
    }

    /** Thermal FE per tick a fuel assembly makes on its own. */
    public static final float FUEL_HEAT = 10_000;
    /** Ticks an assembly lasts burning at 100%: an hour. */
    public static final int FUEL_LIFE = 72_000;
    private static final float FUEL_BONUS = 0.1F;
    /** What each fuel neighbour adds to an assembly's burn: half its heat bonus (packed cores leak less). */
    private static final float FUEL_BURN_BONUS = 0.05F;
    /** The most heat one assembly can pass to the sodium (before the config's output setting). */
    public static final float HOT_SPOT = 13_000;
    private static final float CONTROL_CUT = 0.3F;
    private static final float MIN_FACTOR = 0.1F;
    /**
     * Breeding work per thermal FE of the fuel beside a blanket, in the station's target units
     * ({@link StationReactor#TARGET_WORK} thousands to finish one): a blanket beside two
     * assemblies takes about 40 minutes (halved from 20 on 3 Oct 2026, so a breeder is not a flood
     * of plutonium).
     */
    public static final float BREED_SHARE = 0.05F;
    /** mB of liquid sodium that fills the loop. Less than full carries less heat in step. */
    public static final int LOOP = 4_000;
    /** Thermal FE per tick the full loop carries away at 100% flow: a whole core at full load. */
    public static final float LOOP_CAPACITY = 100_000;
    /** FE per tick the pumps draw at 100% flow; less as the square of the flow. */
    public static final float PUMP_POWER = 4_000;
    public static final float IDLE_TEMPERATURE = 300;
    public static final float FULL_LOAD_TEMPERATURE = 550;

    /** What a position is planned to hold. */
    public enum Position {
        // New types go at the end: plans are saved by position in this list.
        EMPTY, FUEL, CONTROL, URANIUM, LITHIUM;

        public static Position byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : EMPTY;
        }

        /** Whether an item may go in a position of this type. */
        public boolean accepts(ItemStack stack) {
            return switch (this) {
                case FUEL -> stack.is(ModItems.BREEDER_FUEL.get());
                case CONTROL -> stack.is(ModItems.CONTROL_ROD.get());
                case URANIUM -> stack.is(ModItems.URANIUM_BLANKET.get());
                case LITHIUM -> stack.is(ModItems.LITHIUM_TARGET_ROD.get());
                case EMPTY -> false;
            };
        }

        public boolean blanket() {
            return this == URANIUM || this == LITHIUM;
        }
    }

    /**
     * A layout worked out: per position, the heat each assembly makes, how hard it burns, and how
     * fast each blanket breeds; then the total heat. {@code stranded} is heat over the hot spot
     * limit, which the sodium cannot take: any at all and the core cannot hold steady.
     */
    public record Analysis(float[] heat, float[] burn, float[] breed, float generation, float stranded, int fuel, int blankets) {
        public boolean holdsSteady() {
            return stranded <= 0.5F;
        }

        /** Whether the assembly at {@code i} is over the hot spot limit. */
        public boolean hotSpot(int i) {
            return heat[i] > BreederReactor.hotSpot() + 0.5F;
        }

        /** Minutes a fresh blanket at position {@code i} takes to breed, or 0 if nothing is breeding it. */
        public float breedMinutes(int i) {
            return breed[i] <= 0 ? 0 : StationReactor.TARGET_WORK * 1000F / breed[i] / 1200F;
        }

        /** Minutes the average assembly lasts in this layout. */
        public float fuelMinutes() {
            if (fuel == 0) {
                return 0;
            }
            float burnt = 0;
            for (float b : burn) {
                burnt += b;
            }
            return FUEL_LIFE / (burnt / fuel) / 1200F;
        }
    }

    private BreederReactor() {}

    /** The config's output setting: heat, the loop's capacity and the pumps all scale with it. */
    public static float multiplier() {
        return Config.get(Config.BREEDER_OUTPUT) / 100F;
    }

    public static int q(int i) {
        return Q[i];
    }

    public static int r(int i) {
        return R[i];
    }

    /** Rings out from the centre: 0 the centre, 1 the six round it, 2 the outer twelve. */
    public static int ring(int i) {
        return Math.max(Math.abs(Q[i]), Math.max(Math.abs(R[i]), Math.abs(Q[i] + R[i])));
    }

    /** The position at hex coordinates (q, r), or -1 off the lattice. */
    public static int at(int q, int r) {
        for (int i = 0; i < POSITIONS; i++) {
            if (Q[i] == q && R[i] == r) {
                return i;
            }
        }
        return -1;
    }

    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, -1}, {-1, 1}};

    /** The up to six positions beside position {@code i}. */
    public static int[] neighbours(int i) {
        int[] out = new int[6];
        int n = 0;
        for (int[] d : DIRECTIONS) {
            int j = at(Q[i] + d[0], R[i] + d[1]);
            if (j >= 0) {
                out[n++] = j;
            }
        }
        return java.util.Arrays.copyOf(out, n);
    }

    /** The hot spot limit after the config's output setting. */
    public static float hotSpot() {
        return HOT_SPOT * multiplier();
    }

    /** Thermal FE per tick the loop carries at this flow (0 to 1) and fill (mB of sodium). */
    public static float capacity(float flow, int sodium) {
        return LOOP_CAPACITY * multiplier() * flow * Math.min(1, sodium / (float) LOOP);
    }

    /** FE per tick the pumps draw at this flow. */
    public static float pumpPower(float flow) {
        return PUMP_POWER * multiplier() * flow * flow;
    }

    /** How much of the loop's capacity the core's heat can drive: none at 300°C, all at 550°C. */
    public static float coolingRamp(float temperature) {
        return Mth.clamp((temperature - IDLE_TEMPERATURE) / (FULL_LOAD_TEMPERATURE - IDLE_TEMPERATURE), 0, 1);
    }

    /** Steam plant efficiency: 30% with the sodium idle, 42% at full load. */
    public static float efficiency(float temperature) {
        return 0.30F + 0.12F * coolingRamp(temperature);
    }

    /** The temperature a core making {@code generation} settles at with this loop, if it can. */
    public static float settledTemperature(float generation, float capacity) {
        return IDLE_TEMPERATURE + (FULL_LOAD_TEMPERATURE - IDLE_TEMPERATURE) * Math.min(1, capacity <= 0 ? 1 : generation / capacity);
    }

    /** Power out once settled: the steam plant's share of the heat, less the pumps. 0 if the loop cannot carry it. */
    public static float plannedOutput(float generation, float flow, int sodium) {
        float capacity = capacity(flow, sodium);
        if (generation <= 0 || generation > capacity + 0.5F) {
            return 0;
        }
        return generation * efficiency(settledTemperature(generation, capacity)) - pumpPower(flow);
    }

    public static boolean isFinished(ItemStack stack) {
        return stack.is(ModItems.SPENT_BREEDER_FUEL.get()) || stack.is(ModItems.BRED_URANIUM_BLANKET.get())
                || stack.is(ModItems.IRRADIATED_TARGET_ROD.get());
    }

    /** Ticks of burn left in an assembly (a fresh one has its full life). */
    public static int fuelLeft(ItemStack fuel) {
        return fuel.getOrDefault(ModDataComponents.FUEL_LEFT.get(), FUEL_LIFE);
    }

    /** What a used-up assembly becomes: spent breeder fuel, hot until a pool cools it. */
    public static ItemStack spent() {
        return HotFuel.mark(new ItemStack(ModItems.SPENT_BREEDER_FUEL.get()));
    }

    /** What a fully bred blanket becomes. */
    public static ItemStack bred(Position type) {
        return new ItemStack(type == Position.URANIUM ? ModItems.BRED_URANIUM_BLANKET.get() : ModItems.IRRADIATED_TARGET_ROD.get());
    }

    /**
     * Works out a layout. {@code items} holds each position's item: a position counts only when its
     * item is in place.
     */
    public static Analysis analyse(Position[] types, ItemStack[] items) {
        float multiplier = multiplier();
        float[] heat = new float[POSITIONS];
        float[] burn = new float[POSITIONS];
        float generation = 0;
        float stranded = 0;
        int fuel = 0;
        int blankets = 0;
        for (int i = 0; i < POSITIONS; i++) {
            if (types[i].blanket() && types[i].accepts(items[i])) {
                blankets++;
            }
            if (types[i] != Position.FUEL || !types[i].accepts(items[i])) {
                continue;
            }
            float factor = 1;
            float burnFactor = 1;
            for (int n : neighbours(i)) {
                if (!types[n].accepts(items[n])) {
                    continue;
                }
                if (types[n] == Position.FUEL) {
                    factor += FUEL_BONUS;
                    burnFactor += FUEL_BURN_BONUS;
                } else if (types[n] == Position.CONTROL) {
                    factor -= CONTROL_CUT;
                    burnFactor -= CONTROL_CUT;
                }
            }
            heat[i] = FUEL_HEAT * Math.max(MIN_FACTOR, factor) * multiplier;
            burn[i] = Math.max(MIN_FACTOR, burnFactor);
            generation += heat[i];
            stranded += Math.max(0, heat[i] - HOT_SPOT * multiplier);
            fuel++;
        }
        float[] breed = new float[POSITIONS];
        for (int i = 0; i < POSITIONS; i++) {
            if (!types[i].blanket() || !types[i].accepts(items[i])) {
                continue;
            }
            for (int n : neighbours(i)) {
                breed[i] += heat[n] * BREED_SHARE;
            }
        }
        return new Analysis(heat, burn, breed, generation, stranded, fuel, blankets);
    }
}
