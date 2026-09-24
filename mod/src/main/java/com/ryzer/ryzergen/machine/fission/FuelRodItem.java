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
 * A fission station fuel rod (uranium or MOX). Like the microreactor's fuel core it carries its own
 * burn, so a rod taken out part-used keeps what is left and shows it as a bar. Safe to carry
 * (design rule 10): only a running reactor gives off radiation.
 */
public class FuelRodItem extends Item {
    public FuelRodItem(Properties properties) {
        super(properties);
    }

    public static float fraction(ItemStack stack) {
        return Mth.clamp(StationReactor.fuelLeft(stack) / (float) StationReactor.life(stack), 0, 1);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(ModDataComponents.FUEL_LEFT.get()) && fraction(stack) < 1;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13 * fraction(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(fraction(stack) / 3F, 0.9F, 1F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ryzergen.fuel_rod.fuel", Math.round(fraction(stack) * 100))
                .withStyle(ChatFormatting.GRAY));
    }
}
