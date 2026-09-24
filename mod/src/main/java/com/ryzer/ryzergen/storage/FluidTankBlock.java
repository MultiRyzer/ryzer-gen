package com.ryzer.ryzergen.storage;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/**
 * Fluid tank: the pressure tank's twin for liquids (water, lava, anything that is not a gas), with
 * copper walls. It joins into the same 2 x 2 towers, and through the sight glass the liquid shows
 * at its level, settled at the bottom as a liquid does.
 */
public class FluidTankBlock extends PressureTankBlock {
    public static final MapCodec<FluidTankBlock> CODEC = simpleCodec(FluidTankBlock::new);

    public FluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean holdsGas() {
        return false;
    }
}
