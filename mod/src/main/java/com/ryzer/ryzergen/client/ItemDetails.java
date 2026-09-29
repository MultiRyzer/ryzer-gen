package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.pool.HotFuel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Item tooltips, as Mekanism and many other mods do them: a short summary always shows under the
 * name, and the details show while Shift is held ("Hold Shift for details" otherwise). All of it
 * comes from the lang file, so giving an item details needs no code:
 * <ul>
 *   <li>{@code tooltip.ryzergen.<id>}: the summary line</li>
 *   <li>{@code tooltip.ryzergen.<id>.details.1}, {@code .2} and on: the detail lines</li>
 * </ul>
 * Lines are grey, with colour for what matters, marked in the lang text:
 * {@code <v>} values (cyan), {@code <g>} benefits (green), {@code <w>} cautions (gold),
 * {@code <r>} dangers (red), {@code <k>} keys and controls (yellow), and {@code <i>} a note, such as
 * a real-world basis (dark grey italic). Items still add their own live readouts (fuel left,
 * charge, dose), which sit between the summary and the details.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class ItemDetails {
    private static final Pattern TAG = Pattern.compile("<([vgwrki])>(.*?)</\\1>");
    private static final String HOLD_SHIFT = "tooltip.ryzergen.hold_shift";

    private ItemDetails() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!id.getNamespace().equals(RyzerGen.MOD_ID)) {
            return;
        }
        Language language = Language.getInstance();
        String key = "tooltip.ryzergen." + id.getPath();
        List<Component> tooltip = event.getToolTip();
        if (language.has(key) && !tooltip.isEmpty()) {
            tooltip.add(1, line(language.getOrDefault(key)));
        }
        // Spent fuel fresh from a reactor says so first: nothing will process it until it cools.
        if (HotFuel.isHot(event.getItemStack()) && !tooltip.isEmpty()) {
            tooltip.add(1, line(language.getOrDefault("tooltip.ryzergen.hot")));
        }
        if (!language.has(key + ".details.1")) {
            return;
        }
        List<Component> details = new ArrayList<>();
        if (Screen.hasShiftDown()) {
            for (int n = 1; language.has(key + ".details." + n); n++) {
                details.add(line(language.getOrDefault(key + ".details." + n)));
            }
        } else {
            details.add(line(language.getOrDefault(HOLD_SHIFT)).withStyle(ChatFormatting.DARK_GRAY));
        }
        // Above the advanced tooltip's lines (F3+H), which end with the item's id.
        int at = tooltip.size();
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().equals(id.toString())) {
                at = i;
                break;
            }
        }
        tooltip.addAll(at, details);
    }

    /** A grey line with its marked parts coloured. */
    private static MutableComponent line(String text) {
        MutableComponent line = Component.empty().withStyle(ChatFormatting.GRAY);
        Matcher matcher = TAG.matcher(text);
        int from = 0;
        while (matcher.find()) {
            if (matcher.start() > from) {
                line.append(Component.literal(text.substring(from, matcher.start())));
            }
            MutableComponent part = Component.literal(matcher.group(2));
            switch (matcher.group(1)) {
                case "v" -> part.withStyle(ChatFormatting.AQUA);
                case "g" -> part.withStyle(ChatFormatting.GREEN);
                case "w" -> part.withStyle(ChatFormatting.GOLD);
                case "r" -> part.withStyle(ChatFormatting.RED);
                case "k" -> part.withStyle(ChatFormatting.YELLOW);
                default -> part.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
            }
            line.append(part);
            from = matcher.end();
        }
        if (from < text.length()) {
            line.append(Component.literal(text.substring(from)));
        }
        return line;
    }
}
