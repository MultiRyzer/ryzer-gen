package com.ryzer.ryzergen.machine.breeder;

import com.ryzer.ryzergen.registry.ModBlocks;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/** The kinds of block a breeder reactor is built from. */
public enum BreederPart {
    /** The plinth, the legs and the girder belt. */
    FRAME(() -> ModBlocks.BREEDER_FRAME.get()),
    /** The sphere's shell. */
    SHELL(() -> ModBlocks.BREEDER_SHELL.get()),
    CORE(() -> ModBlocks.BREEDER_CORE.get());

    private final Supplier<Block> block;

    BreederPart(Supplier<Block> block) {
        this.block = block;
    }

    public Block block() {
        return block.get();
    }
}
