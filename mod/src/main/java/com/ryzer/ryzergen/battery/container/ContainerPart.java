package com.ryzer.ryzergen.battery.container;

import com.ryzer.ryzergen.registry.ModBlocks;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/** The kinds of block a Container Battery is built from. */
public enum ContainerPart {
    FRAME(() -> ModBlocks.CONTAINER_FRAME.get()),
    THERMAL(() -> ModBlocks.THERMAL_UNIT.get()),
    CONTROLLER(() -> ModBlocks.BATTERY_CONTROLLER.get());

    private final Supplier<Block> block;

    ContainerPart(Supplier<Block> block) {
        this.block = block;
    }

    public Block block() {
        return block.get();
    }
}
