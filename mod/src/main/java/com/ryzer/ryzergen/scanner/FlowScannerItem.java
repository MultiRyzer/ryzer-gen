package com.ryzer.ryzergen.scanner;

import net.minecraft.world.item.Item;

/**
 * The Flow Scanner: hold it and every pipe and cable input nearby shows how much comes in, coloured
 * by how close it is to that input's limit (green idle, red full). See FlowScanner and
 * client/FlowScannerLabels. Its tooltip is in the lang file (client/ItemDetails).
 */
public class FlowScannerItem extends Item {
    public FlowScannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }
}
