package com.ryzer.ryzergen.scanner;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Flow Scanner: hold it and every pipe and cable input nearby shows how much comes in, coloured
 * by how close it is to that input's limit (green idle, red full). See FlowScanner and
 * client/FlowScannerLabels.
 */
public class FlowScannerItem extends Item {
    public FlowScannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ryzergen.flow_scanner.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
