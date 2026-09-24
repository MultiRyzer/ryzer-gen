package com.ryzer.ryzergen.machine.fission;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModDataComponents;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.util.Mth;
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
 */
public final class StationReactor {
    public static final int GRID = 5;
    public static final int CHANNELS = GRID * GRID;

    /** Thermal FE per tick a fuel rod makes on its own. MOX runs hotter. */
    public static final float URANIUM_HEAT = 1000;
    public static final float MOX_HEAT = 1400;
    /** Ticks a rod lasts burning at 100%: uranium burns fast (and its spent rods breed plutonium), MOX lasts. */
    public static final int URANIUM_LIFE = 36_000;
    public static final int MOX_LIFE = 144_000;
    /** Thermal FE per tick one coolant channel can carry away. */
    public static final float COOLANT_CAPACITY = 3000;
    private static final float MODERATOR_BONUS = 0.4F;
    private static final float FUEL_BONUS = 0.2F;
    private static final float CONTROL_CUT = 0.35F;
    private static final float MIN_FACTOR = 0.1F;
    public static final float IDLE_TEMPERATURE = 150;
    public static final float FULL_LOAD_TEMPERATURE = 600;
    /**
     * The most power found for a core of each fuel (searching layouts offline with
     * art/tools/core_optimiser.py), for the screen's rating. At 100% output.
     */
    public static final float BEST_URANIUM = 8006;
    public static final float BEST_MOX = 9053;
    /** The temperatures those best layouts settle at, to scale them for overdrive. */
    private static final float BEST_URANIUM_TEMPERATURE = 544;
    private static final float BEST_MOX_TEMPERATURE = 490;
    /** Overdrive: more heat and burn per rod, and more heat carried per coolant channel. */
    public static final float OVERDRIVE_HEAT = 1.3F;

    /** What a channel is planned to hold. Coolant channels are pipes of water; empty ones do nothing. */
    public enum Channel {
        EMPTY, FUEL, MODERATOR, CONTROL, COOLANT;

        public static Channel byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : EMPTY;
        }

        /** Whether an item may go in a channel of this type. */
        public boolean accepts(ItemStack stack) {
            return switch (this) {
                case FUEL -> isFreshFuel(stack);
                case MODERATOR -> stack.is(ModItems.GRAPHITE_BLOCK.get());
                case CONTROL -> stack.is(ModItems.CONTROL_ROD.get());
                case COOLANT, EMPTY -> false;
            };
        }
    }

    /**
     * A layout worked out: per channel, the heat each fuel rod makes, how hard it burns, and the load
     * on each coolant channel; then the totals. {@code stranded} is heat no coolant can carry (a rod
     * with no coolant beside it, or more than a channel's capacity): any at all and the core cannot
     * hold steady. {@code working} is how many coolant channels carry heat.
     */
    public record Analysis(float[] heat, float[] burn, float[] load, float generation, float stranded,
                           int working, int coolants, int fuelRods, int moxRods, boolean overdrive) {
        /** The fraction of the working coolant's capacity in use, which sets the core's temperature. */
        public float loadFraction() {
            return working == 0 ? 0 : generation / (working * capacity(overdrive));
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
            float averageLife = (moxRods * MOX_LIFE + (fuelRods - moxRods) * URANIUM_LIFE) / (float) fuelRods;
            return averageLife / (burnt / fuelRods) / 1200F;
        }

        /** Planned power against the best found for this fuel, 0 to 1. */
        public float rating() {
            if (fuelRods == 0) {
                return 0;
            }
            float mox = moxRods / (float) fuelRods;
            float best = (BEST_URANIUM * (1 - mox) + BEST_MOX * mox) * multiplier();
            if (overdrive) {
                // The same layout is best in overdrive (heat and cooling scale together), just stronger.
                float settled = BEST_URANIUM_TEMPERATURE * (1 - mox) + BEST_MOX_TEMPERATURE * mox;
                best *= OVERDRIVE_HEAT * efficiency(settled, true) / efficiency(settled, false);
            }
            return Math.min(1, plannedOutput() / best);
        }
    }

    private StationReactor() {}

    private static float multiplier() {
        return Config.get(Config.STATION_OUTPUT) / 100F;
    }

    /** Thermal FE per tick one coolant channel can carry, after the config's output setting. */
    public static float capacity(boolean overdrive) {
        return COOLANT_CAPACITY * multiplier() * (overdrive ? OVERDRIVE_HEAT : 1);
    }

    public static boolean isFreshFuel(ItemStack stack) {
        return stack.is(ModItems.URANIUM_FUEL_ROD.get()) || stack.is(ModItems.MOX_FUEL_ROD.get());
    }

    public static boolean isSpent(ItemStack stack) {
        return stack.is(ModItems.SPENT_URANIUM_ROD.get()) || stack.is(ModItems.SPENT_MOX_ROD.get());
    }

    public static int life(ItemStack rod) {
        return rod.is(ModItems.MOX_FUEL_ROD.get()) ? MOX_LIFE : URANIUM_LIFE;
    }

    /** Ticks of burn left in a rod (a fresh one has its full life). */
    public static int fuelLeft(ItemStack rod) {
        return rod.getOrDefault(ModDataComponents.FUEL_LEFT.get(), life(rod));
    }

    /** The spent rod a fuel rod becomes. */
    public static ItemStack spentFor(ItemStack rod) {
        return new ItemStack(rod.is(ModItems.MOX_FUEL_ROD.get()) ? ModItems.SPENT_MOX_ROD.get() : ModItems.SPENT_URANIUM_ROD.get());
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
        int moxRods = 0;
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
                    default -> { }
                }
            }
            boolean mox = rods[i].is(ModItems.MOX_FUEL_ROD.get());
            heat[i] = (mox ? MOX_HEAT : URANIUM_HEAT) * Math.max(MIN_FACTOR, heatFactor) * multiplier() * boost;
            burn[i] = Math.max(MIN_FACTOR, burnFactor) * boost;
            generation += heat[i];
            fuelRods++;
            moxRods += mox ? 1 : 0;
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
        int working = 0;
        for (int i = 0; i < CHANNELS; i++) {
            if (load[i] > 0) {
                working++;
                stranded += Math.max(0, load[i] - capacity(overdrive));
            }
        }
        return new Analysis(heat, burn, load, generation, stranded, working, coolants, fuelRods, moxRods, overdrive);
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
