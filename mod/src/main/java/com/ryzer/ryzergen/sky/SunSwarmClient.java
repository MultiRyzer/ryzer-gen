package com.ryzer.ryzergen.sky;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The swarm as this client last heard it, and how far along its animation is. Everything here is
 * worked out from the game time, so every player sees the same moment. The numbers:
 * <ul>
 *   <li>coverage: how many of the swarm's panels are up, 0 to 1 (they launch in turn)</li>
 *   <li>closing: how far the shell has closed, 0 to 1</li>
 *   <li>landed: how far one panel has flown from its orbit into its tile of the shell, 0 to 1</li>
 *   <li>collapse: how far the enclosed sun has collapsed, and wormhole: how far the wormhole is open</li>
 *   <li>dim: how much of the sun's light is cut off, 0 to 1 (the sky and the world darken by it)</li>
 * </ul>
 * The shell's tiles are laid out round the line from the sun to the ground (client/SkySun): theta
 * 0 faces the ground, pi is the far side. The far side lands first and the tiles facing the ground
 * last, so the last gap closes like an iris towards you.
 */
public final class SunSwarmClient {
    /** The share of the light the finished swarm catches before the shell closes. */
    private static final float SWARM_DIM = 0.12F;
    /** The share of the daylight the new star gives back through the open wormhole. */
    private static final float WORMHOLE_LIGHT = 0.2F;
    /** Rings of tiles in the shell, from the side facing the ground round to the far side. */
    public static final int SHELL_BANDS = 16;
    /** How long one panel's flight takes, as a share of the closing. */
    private static final float FLIGHT = 0.35F;

    private static volatile SunSwarm.Phase phase = SunSwarm.Phase.NONE;
    private static volatile long start;

    private SunSwarmClient() {}

    public static void receive(SunSwarmPayload payload, IPayloadContext context) {
        SunSwarm.Phase[] phases = SunSwarm.Phase.values();
        phase = payload.phase() >= 0 && payload.phase() < phases.length ? phases[payload.phase()] : SunSwarm.Phase.NONE;
        start = payload.start();
    }

    /** Forgets the swarm when leaving a world. */
    public static void clear() {
        phase = SunSwarm.Phase.NONE;
    }

    public static SunSwarm.Phase phase() {
        return phase;
    }

    public static boolean active() {
        return phase != SunSwarm.Phase.NONE;
    }

    private static float progress(long gameTime, float partialTick, int ticks) {
        return Math.max(0, Math.min(1, (gameTime - start + partialTick) / ticks));
    }

    public static float coverage(long gameTime, float partialTick) {
        return switch (phase) {
            case NONE -> 0;
            case LAUNCHING -> progress(gameTime, partialTick, SunSwarm.LAUNCH_TICKS);
            default -> 1;
        };
    }

    public static float closing(long gameTime, float partialTick) {
        return switch (phase) {
            case CLOSING -> progress(gameTime, partialTick, SunSwarm.CLOSE_TICKS);
            case CLOSED, COLLAPSING, WORMHOLE -> 1;
            default -> 0;
        };
    }

    /** How far the enclosed sun has collapsed, 0 to 1 (1 once the wormhole is open). */
    public static float collapse(long gameTime, float partialTick) {
        return switch (phase) {
            case COLLAPSING -> progress(gameTime, partialTick, SunSwarm.COLLAPSE_TICKS);
            case WORMHOLE -> 1;
            default -> 0;
        };
    }

    /** When in the collapse the wormhole pops open, after the white has held for a beat. */
    public static final float POP = 0.8F;

    /**
     * How far the wormhole has opened: nothing until the pop, then it bursts open and bounces into
     * shape like an unstable warp, a damped spring overshooting to about 1.2 before it settles at 1.
     */
    public static float wormhole(long gameTime, float partialTick) {
        float t = collapse(gameTime, partialTick);
        if (t < POP) {
            return 0;
        }
        if (phase == SunSwarm.Phase.WORMHOLE) {
            return 1;
        }
        double s = (t - POP) / (1 - POP);
        return (float) (1 - Math.exp(-4.5 * s) * Math.cos(9 * s));
    }

    /** Whether the sun is enclosed or gone, so the moon has no light to show. */
    public static boolean sunGone() {
        return phase == SunSwarm.Phase.CLOSED || phase == SunSwarm.Phase.COLLAPSING || phase == SunSwarm.Phase.WORMHOLE;
    }

    /** How far the panel for a tile {@code theta} round from the ground-facing side has landed. */
    public static float landed(double theta, float closing) {
        float from = 0.05F + 0.5F * (float) (1 - theta / Math.PI);
        return smooth(from, from + FLIGHT, closing);
    }

    /** The share of the shell still open: each band's area weighted by how far its panels have yet to land. */
    public static float open(float closing) {
        double open = 0;
        for (int j = 0; j < SHELL_BANDS; j++) {
            double t0 = Math.PI * j / SHELL_BANDS, t1 = Math.PI * (j + 1) / SHELL_BANDS;
            open += (Math.cos(t0) - Math.cos(t1)) / 2 * (1 - landed((t0 + t1) / 2, closing));
        }
        return (float) open;
    }

    public static float dim(long gameTime, float partialTick) {
        return switch (phase) {
            case NONE -> 0;
            case LAUNCHING -> SWARM_DIM * coverage(gameTime, partialTick);
            case SWARM -> SWARM_DIM;
            case CLOSING -> 1 - (1 - SWARM_DIM) * open(closing(gameTime, partialTick));
            case CLOSED -> 1;
            // A little of the new star's light comes through the wormhole.
            case COLLAPSING, WORMHOLE -> 1 - WORMHOLE_LIGHT * Math.min(1, wormhole(gameTime, partialTick));
        };
    }

    /** Smoothstep: 0 below {@code from}, 1 above {@code to}, eased between. */
    public static float smooth(float from, float to, float x) {
        float t = Math.max(0, Math.min(1, (x - from) / (to - from)));
        return t * t * (3 - 2 * t);
    }
}
