package com.ryzer.ryzergen.machine;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.advancement.Milestone;
import com.ryzer.ryzergen.registry.ModTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * A reactor's steady-state streak (design section 5): the fuel loads it has burned without
 * downtime, which raise its electrical output up to its tier's cap. Fuel burns and heat flows as
 * before, so the bonus is more energy from the same fuel.
 *
 * <p>Each tick the reactor says whether it is running (and how much of a load it burned), paused
 * (standing by on a full buffer: not the player's fault) or down (it should be running and is not).
 * More than 5 seconds down costs a quarter of the streak, once per stop; an unloaded chunk or a
 * stopped server simply does not tick. The bonus reaches a quarter of the cap's gain at 8 loads,
 * half at 32 and all of it at 128, growing smoothly between. Real basis: a reactor refuelled while
 * running (CANDU) runs far more of the time, and one that stops sits in the xenon "iodine pit".
 */
public final class SteadyState {
    /** Each tier's cap, from the config. */
    public enum Tier {
        MICROREACTOR, STATION, BREEDER;

        ModConfigSpec.IntValue cap() {
            return switch (this) {
                case MICROREACTOR -> Config.STEADY_STATE_MICROREACTOR_CAP;
                case STATION -> Config.STEADY_STATE_STATION_CAP;
                case BREEDER -> Config.STEADY_STATE_BREEDER_CAP;
            };
        }
    }

    public enum State { RUNNING, PAUSED, DOWN }

    /** Loads at which the bonus reaches a quarter, a half and all of the tier's gain. */
    private static final float[] MILESTONES = {8, 32, 128};
    private static final float[] SHARES = {0.25F, 0.5F, 1};
    /** Downtime this long counts: a quick swap by pipe does not. */
    private static final int GRACE_TICKS = 100;
    /** Each stop keeps this share of the streak. */
    private static final float KEPT = 0.75F;

    private float loads;
    private int downTicks;

    public float loads() {
        return loads;
    }

    /**
     * One tick. {@code burned} is the share of a full fuel load burned this tick, while running.
     * Returns the milestone just reached (1 to 3), or 0.
     */
    public int tick(State state, float burned) {
        if (!Config.get(Config.STEADY_STATE_ENABLED)) {
            return 0;
        }
        if (state == State.RUNNING) {
            downTicks = 0;
            float before = loads;
            loads += burned;
            for (int i = 0; i < MILESTONES.length; i++) {
                float at = milestone(i);
                if (before < at && loads >= at) {
                    return i + 1;
                }
            }
        } else if (state == State.DOWN) {
            if (++downTicks == GRACE_TICKS) {
                loads *= KEPT;
            }
        }
        return 0;
    }

    /** Ends the streak (a meltdown). */
    public void reset() {
        loads = 0;
        downTicks = 0;
    }

    /** The output multiplier now, 1 with no streak, up to the tier's cap. */
    public float bonus(Tier tier) {
        if (!Config.get(Config.STEADY_STATE_ENABLED)) {
            return 1;
        }
        float gain = Config.get(tier.cap()) / 100F - 1;
        return 1 + gain * share();
    }

    /** How much of the tier's gain the streak has reached, 0 to 1. */
    public float share() {
        if (loads <= 0) {
            return 0;
        }
        if (loads < milestone(0)) {
            return SHARES[0] * loads / milestone(0);
        }
        for (int i = 1; i < MILESTONES.length; i++) {
            if (loads < milestone(i)) {
                // Between milestones the share grows evenly with each doubling of the streak.
                double along = Math.log(loads / milestone(i - 1)) / Math.log(milestone(i) / milestone(i - 1));
                return (float) (SHARES[i - 1] + (SHARES[i] - SHARES[i - 1]) * along);
            }
        }
        return 1;
    }

    /** For a screen: the streak in tenths of a load, the bonus in thousandths, the next milestone in tenths. */
    public int loadsTenths() {
        return Math.min(Short.MAX_VALUE, Math.round(loads * 10));
    }

    public int bonusPermille(Tier tier) {
        return Math.round(bonus(tier) * 1000);
    }

    public int nextTenths() {
        return Math.min(Short.MAX_VALUE, Math.round(nextMilestone() * 10));
    }

    /** The next milestone in loads, or 0 past the last. */
    public float nextMilestone() {
        for (int i = 0; i < MILESTONES.length; i++) {
            if (loads < milestone(i)) {
                return milestone(i);
            }
        }
        return 0;
    }

    /**
     * Calls a milestone the tick returned: the announcer's line, and at the last, the tier's
     * advancement for whoever is near.
     */
    public static void reached(ServerLevel level, Vec3 at, Announcer announcer, int milestone, Milestone cap) {
        switch (milestone) {
            case 1 -> announcer.say(level, at, Announcer.Line.STEADY_STATE_1);
            case 2 -> announcer.say(level, at, Announcer.Line.STEADY_STATE_2);
            case 3 -> {
                announcer.say(level, at, Announcer.Line.STEADY_STATE_3);
                ModTriggers.MILESTONE.get().triggerNearby(level, at, cap);
            }
            default -> { }
        }
    }

    private static float milestone(int i) {
        return MILESTONES[i] * Config.get(Config.STEADY_STATE_MILESTONE_PERCENT) / 100F;
    }

    public void save(CompoundTag tag) {
        tag.putFloat("streak", loads);
        tag.putInt("streak_down", downTicks);
    }

    public void load(CompoundTag tag) {
        loads = tag.getFloat("streak");
        downTicks = tag.getInt("streak_down");
    }
}
