package com.ryzer.ryzergen.creative;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/** The creative water tank: endless water for testing. See {@link CreativeSourceBlock}. */
public class CreativeWaterTankBlock extends CreativeSourceBlock {
    public static final MapCodec<CreativeWaterTankBlock> CODEC = simpleCodec(CreativeWaterTankBlock::new);

    public CreativeWaterTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean givesWater() {
        return true;
    }
}
