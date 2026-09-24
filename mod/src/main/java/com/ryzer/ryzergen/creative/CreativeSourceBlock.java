package com.ryzer.ryzergen.creative;

import com.mojang.serialization.MapCodec;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Creative-only test blocks, with no recipe: the creative battery (endless power) and the creative
 * water tank (endless water). Each pushes into whatever touches it and can be drained without end,
 * so a machine can be tried out without building its supply first.
 */
public class CreativeSourceBlock extends BaseEntityBlock {
    public static final MapCodec<CreativeSourceBlock> CODEC = simpleCodec(CreativeSourceBlock::new);

    public CreativeSourceBlock(Properties properties) {
        super(properties);
    }

    /** False: the creative battery. The creative water tank says true. */
    public boolean givesWater() {
        return false;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeSourceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CREATIVE_SOURCE.get(), CreativeSourceBlockEntity::serverTick);
    }
}
