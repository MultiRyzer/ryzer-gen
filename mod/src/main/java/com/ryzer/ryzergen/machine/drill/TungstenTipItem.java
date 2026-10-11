package com.ryzer.ryzergen.machine.drill;

import net.minecraft.world.item.Item;

/**
 * The mega drill's emitter tip (design section 11c). The beam melts rock at over 1,000 C, so the
 * refractory tip wears a little with every layer and is used up after about one pit from sea level
 * to bedrock; the controller takes a fresh one from its tip port, so a pipe can keep it fed. Ties
 * the drill to the mines below and gives tungsten a use. Real basis: tungsten has the highest
 * melting point of any metal, which is why it lines the hottest parts of furnaces and rockets.
 */
public class TungstenTipItem extends Item {
    /** Layers a tip lasts. */
    public static final int LAYERS = 160;

    public TungstenTipItem(Properties properties) {
        // No repair: two worn tips do not make a fresh one in the crafting grid.
        super(properties.durability(LAYERS).setNoRepair());
    }
}
