package com.ryzer.ryzergen.machine.breeder;

import com.ryzer.ryzergen.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Breeder fuel (design section 9). Like the station's fuel rods it carries its own burn, so an
 * assembly taken out part-used keeps what is left and shows it as a bar. Safe to carry (rule 10).
 */
public class BreederFuelItem extends Item {
    public BreederFuelItem(Properties properties) {
        super(properties);
    }

    public static float fraction(ItemStack stack) {
        return Mth.clamp(BreederReactor.fuelLeft(stack) / (float) BreederReactor.FUEL_LIFE, 0, 1);
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
        if (stack.has(ModDataComponents.FUEL_LEFT.get())) {
            tooltip.add(Component.translatable("item.ryzergen.fuel_rod.fuel", Math.round(fraction(stack) * 100))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
