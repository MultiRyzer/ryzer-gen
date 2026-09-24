package com.ryzer.ryzergen.machine.microreactor;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A sealed fuel core: TRISO pellets packed in graphite inside a steel shell. The core carries its
 * own burn time, so taking it out of a reactor halfway keeps what is left. Safe to carry
 * (design rule 10): only a running reactor gives off radiation.
 */
public class FuelCoreItem extends Item {
    public FuelCoreItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int fuelLeft(ItemStack stack) {
        return Math.min(stack.getOrDefault(ModDataComponents.FUEL_LEFT.get(), fuelLife()), fuelLife());
    }

    public static void setFuelLeft(ItemStack stack, int ticks) {
        stack.set(ModDataComponents.FUEL_LEFT.get(), Math.max(0, ticks));
    }

    public static float fraction(ItemStack stack) {
        return fuelLeft(stack) / (float) fuelLife();
    }

    private static int fuelLife() {
        return Math.max(1, Config.get(Config.MICROREACTOR_FUEL_LIFE));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(ModDataComponents.FUEL_LEFT.get()) && fuelLeft(stack) < fuelLife();
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
        tooltip.add(Component.translatable("item.ryzergen.sealed_fuel_core.fuel", Math.round(fraction(stack) * 100))
                .withStyle(ChatFormatting.GRAY));
    }
}
