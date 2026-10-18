package com.ryzer.ryzergen.client.text;

import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The mod's text markup, written in the lang file and turned into styled text here. Plain text
 * takes the caller's colour (grey in tooltips); marked parts stand out the same way everywhere:
 * <ul>
 *   <li>{@code {heat}}: a glossary word (lang {@code term.ryzergen.heat}) with its icon, in its
 *   term's colour. The glossary is {@link Term#WORDS}: one word per idea.</li>
 *   <li>{@code <heat>544°C</heat>}: any text as that term, with its icon (power, heat, coolant,
 *   steam, fuel, moderator, control, radiation, speed, breeding, time). {@code <heat->544°C</heat->}
 *   is the same without the icon, for dense screen readouts.</li>
 *   <li>{@code <n>2,000</n>}: a number, in white, ahead of its grey unit
 *   ({@code <n>2,000</n> FE/t {heat}}). {@code <b>} the same for a key noun.</li>
 *   <li>{@code <k>Use</k>}: a control, in brackets.</li>
 *   <li>{@code <i>...</i>}: a note, dark grey italic.</li>
 * </ul>
 */
public final class RichText {
    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "icons");
    private static final Pattern TOKEN = Pattern.compile("\\{([a-z_]+)}|<([a-z]+)(-?)>(.*?)</\\2\\3>");
    private static final int WHITE = 0xFFFFFF;
    private static final int INK = 0x1E2226;
    private static final int NOTE_DARK = 0x6E747C;
    private static final int NOTE_LIGHT = 0x5A6068;
    private static final int BRACKET = 0x8A9098;

    private RichText() {}

    /** Marked-up text for a dark background. */
    public static MutableComponent parse(String text) {
        return parse(text, false);
    }

    /** Marked-up text; {@code light} for a light background, which takes each term's deep shade. */
    public static MutableComponent parse(String text, boolean light) {
        MutableComponent out = Component.empty();
        Matcher m = TOKEN.matcher(text);
        int from = 0;
        while (m.find()) {
            if (m.start() > from) {
                out.append(Component.literal(text.substring(from, m.start())));
            }
            if (m.group(1) != null) {
                Term term = Term.WORDS.get(m.group(1));
                String word = Language.getInstance().getOrDefault("term.ryzergen." + m.group(1));
                if (term == null) {
                    out.append(Component.literal(m.group()));
                } else {
                    out.append(term(term, word, light));
                }
            } else {
                out.append(tagged(m.group(2), m.group(4), light, m.group(3).isEmpty()));
            }
            from = m.end();
        }
        if (from < text.length()) {
            out.append(Component.literal(text.substring(from)));
        }
        return out;
    }

    /** A lang line, with its placeholders filled ({@code %s}), marked up for a dark background. */
    public static MutableComponent tr(String key, Object... args) {
        return parse(format(key, args), false);
    }

    /** A lang line, with its placeholders filled, marked up for a light background. */
    public static MutableComponent trLight(String key, Object... args) {
        return parse(format(key, args), true);
    }

    /** Whether the lang file has this key. */
    public static boolean has(String key) {
        return Language.getInstance().has(key);
    }

    /**
     * A term's text: its icon (in the icons font) and the text in its colour. Never bold: the game
     * fakes bold by drawing each glyph twice a pixel apart, which smears small letters.
     */
    public static MutableComponent term(Term term, String text, boolean light) {
        return term(term, text, light, true);
    }

    private static MutableComponent term(Term term, String text, boolean light, boolean icon) {
        int colour = light ? term.light : term.dark;
        MutableComponent out = Component.empty();
        if (icon) {
            out.append(Component.literal(term.icon()).withStyle(Style.EMPTY.withFont(ICONS).withColor(colour).withBold(false)));
        }
        return out.append(Component.literal(text).withStyle(Style.EMPTY.withColor(colour)));
    }

    private static MutableComponent tagged(String tag, String text, boolean light, boolean icon) {
        Term term = Term.byTag(tag);
        if (term != null) {
            return term(term, text, light, icon);
        }
        return switch (tag) {
            case "n", "b" -> Component.literal(text).withStyle(Style.EMPTY.withColor(light ? INK : WHITE));
            case "k" -> Component.empty()
                    .append(Component.literal("[").withStyle(Style.EMPTY.withColor(BRACKET)))
                    .append(Component.literal(text).withStyle(Style.EMPTY.withColor(light ? INK : WHITE)))
                    .append(Component.literal("]").withStyle(Style.EMPTY.withColor(BRACKET)));
            case "i" -> Component.literal(text).withStyle(Style.EMPTY.withColor(light ? NOTE_LIGHT : NOTE_DARK).withItalic(true));
            default -> Component.literal("<" + tag + ">" + text + "</" + tag + ">");
        };
    }

    private static String format(String key, Object... args) {
        String raw = Language.getInstance().getOrDefault(key);
        if (args.length == 0) {
            return raw.replace("%%", "%");
        }
        // A Component among the values (a side's name, an item's) goes in as its text.
        Object[] values = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            values[i] = args[i] instanceof Component component ? component.getString() : args[i];
        }
        try {
            return String.format(raw, values);
        } catch (RuntimeException e) {
            return raw;
        }
    }
}
