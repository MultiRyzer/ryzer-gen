package com.ryzer.ryzergen.cable;

import net.minecraft.world.item.Item;

/**
 * A cable fitting (see {@link CableUpgrade}). Goes in a side's slot in the cable's panel. Its
 * tooltip is in the lang file (client/ItemDetails).
 */
public class FittingItem extends Item {
    private final CableUpgrade tier;

    public FittingItem(CableUpgrade tier, Properties properties) {
        super(properties.stacksTo(16));
        this.tier = tier;
    }

    public CableUpgrade tier() {
        return tier;
    }
}
