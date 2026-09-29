package com.ryzer.ryzergen.machine.pool;

import com.ryzer.ryzergen.registry.ModBlocks;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/** The kinds of block a Spent Fuel Pool is built from. */
public enum PoolPart {
    LINER(() -> ModBlocks.POOL_LINER.get()),
    CRANE(() -> ModBlocks.POOL_CRANE.get()),
    CONTROLLER(() -> ModBlocks.POOL_CONTROLLER.get());

    private final Supplier<Block> block;

    PoolPart(Supplier<Block> block) {
        this.block = block;
    }

    public Block block() {
        return block.get();
    }
}
