package com.ryzer.ryzergen.battery.container;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.battery.BatteryModuleItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * An LFP (lithium iron phosphate) battery rack: the Container Battery's module (design section 12).
 * Right-click it into one of the container's slots. Like a home battery module, it keeps its share
 * of the charge when taken out, so energy can be carried off in it.
 */
public class LfpRackItem extends Item {
    public LfpRackItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int capacity() {
        return Config.get(Config.LFP_RACK_CAPACITY);
    }

    public static int rate() {
        return Config.get(Config.LFP_RACK_RATE);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return BatteryModuleItem.energy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * BatteryModuleItem.energy(stack) / Math.max(1, capacity()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(0.53F, 0.75F, 1F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ryzergen.battery_module.charge",
                String.format("%,d", BatteryModuleItem.energy(stack)), String.format("%,d", capacity())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.ryzergen.battery_module.rate",
                String.format("%,d", rate())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
