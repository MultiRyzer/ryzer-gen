package com.ryzer.ryzergen.machine.assembler;

import com.ryzer.ryzergen.registry.ModBlocks;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/** The kinds of block an Assembler is built from. */
public enum AssemblerPart {
    FRAME(() -> ModBlocks.ASSEMBLER_FRAME.get()),
    ARM(() -> ModBlocks.ASSEMBLER_ARM.get()),
    CONTROLLER(() -> ModBlocks.ASSEMBLER_CONTROLLER.get());

    private final Supplier<Block> block;

    AssemblerPart(Supplier<Block> block) {
        this.block = block;
    }

    public Block block() {
        return block.get();
    }
}
