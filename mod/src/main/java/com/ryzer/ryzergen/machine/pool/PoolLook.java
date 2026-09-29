package com.ryzer.ryzergen.machine.pool;

import net.minecraft.util.StringRepresentable;

/**
 * How a formed pool looks, set on every block of it: which of the design's three states its model
 * shows (art/tools/pool_concept.py).
 */
public enum PoolLook implements StringRepresentable {
    /** Too little water to cover the racks: the basin shows dry and the light strip is off. */
    DRY("dry"),
    /** Full, the racks empty. */
    WET("wet"),
    /** Full, with fuel cooling in the racks, glowing Cherenkov blue. */
    ACTIVE("active");

    private final String name;

    PoolLook(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
