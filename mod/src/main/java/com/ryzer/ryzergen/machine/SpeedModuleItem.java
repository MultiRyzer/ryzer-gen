package com.ryzer.ryzergen.machine;

import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A speed module: goes in a machine's upgrade slot, up to {@link ProcessingMachine#MAX_MODULES}.
 * Each one adds another full speed, and power use per tick grows with the square of the speed, so
 * the energy per item grows with the speed too: an upgraded machine is faster, not cheaper. Real
 * basis: a faster process moves more material through the same kit, and that takes more power.
 * Upgrade, don't replace (rule 11).
 */
public class SpeedModuleItem extends Item {
    public SpeedModuleItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ryzergen.speed_module.tooltip", ProcessingMachine.MAX_MODULES)
                .withStyle(style -> style.withColor(0xFF9AA3AE)));
    }
}
