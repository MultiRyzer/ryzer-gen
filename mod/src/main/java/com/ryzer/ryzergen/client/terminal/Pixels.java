package com.ryzer.ryzergen.client.terminal;

/**
 * Colour sums for the screens' pixel art, on ARGB ints. One light for every picture, from the
 * top left as in all the house art; glows step in bands (dithered at their soft edges only), so the
 * pictures stay pixel art at any size.
 */
public final class Pixels {
    /** The house light's direction across the picture, from the top left. */
    public static final float LIGHT_X = -0.7071F;
    public static final float LIGHT_Y = -0.7071F;
    private static final int[][] BAYER = {{0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}};

    private Pixels() {}

    public static int rgb(int r, int g, int b) {
        return 0xFF000000 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static int mul(int c, float f) {
        return rgb(Math.round((c >> 16 & 0xFF) * f), Math.round((c >> 8 & 0xFF) * f), Math.round((c & 0xFF) * f));
    }

    /** {@code b} added over {@code a} as light, {@code f} of it. */
    public static int add(int a, int b, float f) {
        return rgb(Math.round((a >> 16 & 0xFF) + (b >> 16 & 0xFF) * f), Math.round((a >> 8 & 0xFF) + (b >> 8 & 0xFF) * f),
                Math.round((a & 0xFF) + (b & 0xFF) * f));
    }

    public static int lerp(int a, int b, float t) {
        return rgb(Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t),
                Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t),
                Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t));
    }

    /** How much a surface with outward normal (nx, ny) faces the light: 0 to 1. */
    public static float facing(float nx, float ny) {
        return Math.max(0, nx * LIGHT_X + ny * LIGHT_Y);
    }

    /** {@code v} (0 to 1) in clean steps, no dither: for broad glows, which stay calm. */
    public static float stepped(float v, int steps) {
        return (float) Math.floor(Math.max(0, Math.min(1, v)) * steps) / steps;
    }

    /** {@code v} (0 to 1) in steps, dithered between them: for the soft edge of a small glow. */
    public static float dithered(float v, int x, int y, int steps) {
        float s = Math.max(0, Math.min(1, v)) * steps;
        int base = (int) Math.floor(s);
        return (base + (s - base > (BAYER[y & 3][x & 3] + 0.5F) / 16 ? 1 : 0)) / (float) steps;
    }

    private static final float[] HEAT_STOPS = {0, 0.35F, 0.65F, 0.85F, 1};
    private static final int[] HEAT_COLOURS = {0xFF5A1F12, 0xFFB2431C, 0xFFF08A24, 0xFFFFD27A, 0xFFFFF6E0};

    /** Blackbody: dull red through orange to white hot, {@code t} from 0 to 1. */
    public static int heat(float t) {
        t = Math.max(0, Math.min(1, t));
        for (int i = 1; i < HEAT_STOPS.length; i++) {
            if (t <= HEAT_STOPS[i]) {
                return lerp(HEAT_COLOURS[i - 1], HEAT_COLOURS[i], (t - HEAT_STOPS[i - 1]) / (HEAT_STOPS[i] - HEAT_STOPS[i - 1]));
            }
        }
        return HEAT_COLOURS[HEAT_COLOURS.length - 1];
    }
}
