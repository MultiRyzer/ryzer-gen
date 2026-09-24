package com.ryzer.ryzergen.cable;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/**
 * Gas pipe: the fluid pipe for gases only, such as steam. Kept apart from liquids as real plants do,
 * so water can never end up in a steam line.
 */
public class GasPipeBlock extends FluidPipeBlock {
    public static final MapCodec<GasPipeBlock> CODEC = simpleCodec(GasPipeBlock::new);

    public GasPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean carriesGas() {
        return true;
    }
}
