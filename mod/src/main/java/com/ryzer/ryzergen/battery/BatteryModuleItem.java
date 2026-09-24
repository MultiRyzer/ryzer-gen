package com.ryzer.ryzergen.battery;

import com.ryzer.ryzergen.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A battery module. Right-click it into a home battery cabinet. A module keeps its share of the
 * charge when taken out, so energy can be carried between cabinets or kept while swapping chemistries.
 */
public class BatteryModuleItem extends Item {
    private final BatteryChemistry chemistry;

    public BatteryModuleItem(BatteryChemistry chemistry, Properties properties) {
        super(properties.stacksTo(1));
        this.chemistry = chemistry;
    }

    public BatteryChemistry chemistry() {
        return chemistry;
    }

    public static int energy(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STORED_ENERGY.get(), 0);
    }

    public static ItemStack withEnergy(ItemStack stack, int energy) {
        if (energy > 0) {
            stack.set(ModDataComponents.STORED_ENERGY.get(), energy);
        } else {
            stack.remove(ModDataComponents.STORED_ENERGY.get());
        }
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return energy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * energy(stack) / Math.max(1, chemistry.capacity()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(0.0F, 0.75F, 1F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ryzergen.battery_module.charge",
                String.format("%,d", energy(stack)), String.format("%,d", chemistry.capacity())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.ryzergen.battery_module.rate",
                String.format("%,d", chemistry.rate())).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.ryzergen.battery_module.hint").withStyle(ChatFormatting.DARK_AQUA));
    }
}
