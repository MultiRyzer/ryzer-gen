package com.ryzer.ryzergen.machine.fission;

import com.ryzer.ryzergen.registry.ModBlocks;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/** The kinds of block a fission station is built from. */
public enum StationPart {
    CASING(() -> ModBlocks.STATION_CASING.get()),
    GLASS(() -> ModBlocks.STATION_GLASS.get()),
    ROTOR(() -> ModBlocks.TURBINE_ROTOR.get()),
    CORE(() -> ModBlocks.STATION_CORE.get());

    private final Supplier<Block> block;

    StationPart(Supplier<Block> block) {
        this.block = block;
    }

    public Block block() {
        return block.get();
    }
}
