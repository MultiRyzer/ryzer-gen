package com.ryzer.ryzergen.machine.fission;

import com.ryzer.ryzergen.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A lithium target rod (design section 10). In a target channel of the fission station's core,
 * neutrons from the fuel beside it turn its lithium into tritium; when it is done it becomes an
 * irradiated target rod, ready for tritium extraction. Like a fuel rod it carries its own progress,
 * shown as a bar, so a rod taken out part-bred keeps it. Safe to carry (rule 10).
 */
public class TargetRodItem extends Item {
    public TargetRodItem(Properties properties) {
        super(properties);
    }

    /** How far the rod is bred, 0 to 1. */
    public static float fraction(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModDataComponents.BRED.get(), 0) / (float) StationReactor.TARGET_WORK, 0, 1);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(ModDataComponents.BRED.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13 * fraction(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xB27CF0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ryzergen.lithium_target_rod.bred", Math.round(fraction(stack) * 100))
                .withStyle(ChatFormatting.GRAY));
    }
}
