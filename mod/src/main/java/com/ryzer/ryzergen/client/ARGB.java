package com.ryzer.ryzergen.client;

/** Small colour helpers for GUI drawing. Colours are packed 0xAARRGGBB. */
public final class ARGB {
    private ARGB() {}

    public static int lerp(float t, int from, int to) {
        int a = channel(from, to, 24, t);
        int r = channel(from, to, 16, t);
        int g = channel(from, to, 8, t);
        int b = channel(from, to, 0, t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    /** Towards black by {@code amount} (0 to 1), keeping alpha. */
    public static int darken(int colour, float amount) {
        return lerp(amount, colour, colour & 0xFF000000);
    }

    /** Towards white by {@code amount} (0 to 1), keeping alpha. */
    public static int lighten(int colour, float amount) {
        return lerp(amount, colour, colour | 0x00FFFFFF);
    }

    private static int channel(int from, int to, int shift, float t) {
        int a = from >>> shift & 0xFF;
        int b = to >>> shift & 0xFF;
        return Math.round(a + (b - a) * t) & 0xFF;
    }
}
