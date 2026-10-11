package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.text.RichText;
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

/**
 * Item tooltips, all from the lang file, so giving an item its text needs no code. Under the name:
 * <ul>
 *   <li>{@code tooltip.ryzergen.<id>}: its role, a few words ("Fission station fuel")</li>
 *   <li>{@code tooltip.ryzergen.<id>.stat.1}, {@code .2} and on: its stat lines, always shown,
 *   number first ({@code <n>2,000</n> FE/t {heat}})</li>
 *   <li>{@code tooltip.ryzergen.<id>.details.1} and on: how it works and its real basis, while
 *   Shift is held ("Hold Shift for details" otherwise)</li>
 * </ul>
 * Lines are grey with the mod's markup ({@link RichText}): key terms bold in their colours with
 * their icons, numbers bold white. Items still add their own live readouts (fuel left, charge,
 * dose), which sit between the stats and the details.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class ItemDetails {
    private static final String HOLD_SHIFT = "tooltip.ryzergen.hold_shift";
    private static final int ROLE = 0x80868E;

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
        if (tooltip.isEmpty()) {
            return;
        }
        List<Component> head = new ArrayList<>();
        // Spent fuel fresh from a reactor says so first: nothing will process it until it cools.
        if (HotFuel.isHot(event.getItemStack())) {
            head.add(line(language.getOrDefault("tooltip.ryzergen.hot")));
        }
        if (language.has(key)) {
            // The role reads as a category under the name, a step dimmer than the stats.
            head.add(RichText.parse(language.getOrDefault(key)).withColor(ROLE));
        }
        for (int n = 1; language.has(key + ".stat." + n); n++) {
            head.add(line(language.getOrDefault(key + ".stat." + n)));
        }
        tooltip.addAll(1, head);
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

    /** A grey line with its marked parts styled. */
    private static MutableComponent line(String text) {
        return RichText.parse(text).withStyle(ChatFormatting.GRAY);
    }
}
