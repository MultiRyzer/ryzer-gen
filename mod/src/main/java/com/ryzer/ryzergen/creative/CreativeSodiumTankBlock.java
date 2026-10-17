package com.ryzer.ryzergen.creative;

import com.mojang.serialization.MapCodec;
import com.ryzer.ryzergen.registry.ModFluids;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.material.Fluid;

/** The creative sodium tank: endless liquid sodium, for testing the breeder. See {@link CreativeSourceBlock}. */
public class CreativeSodiumTankBlock extends CreativeSourceBlock {
    public static final MapCodec<CreativeSodiumTankBlock> CODEC = simpleCodec(CreativeSodiumTankBlock::new);

    public CreativeSodiumTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public Fluid fluid() {
        return ModFluids.SODIUM.get();
    }
}
