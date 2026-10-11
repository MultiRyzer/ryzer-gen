package com.ryzer.ryzergen.client.text;

import java.util.Locale;
import java.util.Map;

/**
 * The key terms of the mod's text, each always bold, always one colour and always with its icon
 * (a glyph of the {@code ryzergen:icons} font, art/tools/font_icons.py), so a player can tell at a
 * glance what a line is about. One colour per idea across tooltips, the progression map, the recipe
 * viewers and the screens, matching the gauges, ports and core grids: Power red as the energy gauge
 * and energy ports, Coolant blue as the coolant cells and fluid ports, Fuel green as the fuel cells,
 * Moderator amber, Control violet, Breeding pink (blankets and targets, what a reactor breeds).
 * Each has a bright shade for dark backgrounds (tooltips, the map, screen readouts) and a deep one
 * for light ones (the recipe viewers' pages).
 */
public enum Term {
    POWER(0xFF4D3D, 0xC0281C),
    HEAT(0xFF8C3A, 0xC05A10),
    COOLANT(0x4AA8FF, 0x1E64C8),
    STEAM(0xE8EEF2, 0x56606C),
    FUEL(0x44D65E, 0x1E9632),
    MODERATOR(0xE8B030, 0x9A6C08),
    CONTROL(0xA87CFF, 0x6A3CC8),
    RADIATION(0xC6F03A, 0x6A9200),
    SPEED(0x35C8F5, 0x0A86B0),
    BREEDING(0xF06CB4, 0xC03282),
    TIME(0xC8CDD3, 0x50565E);

    /** The words a lang line can insert with {@code {word}}: the glossary, one word per idea. */
    public static final Map<String, Term> WORDS = Map.ofEntries(
            Map.entry("power", POWER), Map.entry("energy", POWER),
            Map.entry("heat", HEAT), Map.entry("meltdown", HEAT),
            Map.entry("coolant", COOLANT), Map.entry("water", COOLANT), Map.entry("sodium", COOLANT),
            Map.entry("steam", STEAM),
            Map.entry("fuel", FUEL),
            Map.entry("moderator", MODERATOR),
            Map.entry("control", CONTROL),
            Map.entry("radiation", RADIATION),
            Map.entry("speed", SPEED),
            Map.entry("blanket", BREEDING), Map.entry("target", BREEDING), Map.entry("tritium", BREEDING),
            Map.entry("time", TIME));

    /** The colour on a dark background, as 0xRRGGBB. */
    public final int dark;
    /** The colour on a light background. */
    public final int light;

    Term(int dark, int light) {
        this.dark = dark;
        this.light = light;
    }

    /** Its icon: a private-use character of the icons font, U+E000 on in this order. */
    public String icon() {
        return Character.toString(0xE000 + ordinal());
    }

    /** Its markup tag, such as {@code heat} for {@code <heat>544°C</heat>}. */
    public String tag() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The colour as an ARGB int for drawing, on a dark background. */
    public int argb() {
        return 0xFF000000 | dark;
    }

    public static Term byTag(String tag) {
        for (Term term : values()) {
            if (term.tag().equals(tag)) {
                return term;
            }
        }
        return null;
    }
}
