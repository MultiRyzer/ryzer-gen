package com.ryzer.ryzergen.machine.microreactor;

import com.mojang.serialization.MapCodec;
import com.ryzer.ryzergen.client.MicroreactorHumSound;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** The reactor heart: a microreactor part that also carries the machine's block entity. */
public class ReactorHeartBlock extends MicroreactorPartBlock implements EntityBlock {
    public static final MapCodec<ReactorHeartBlock> CODEC = simpleCodec(ReactorHeartBlock::new);

    public ReactorHeartBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ReactorHeartBlockEntity heart) {
            heart.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorHeartBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.REACTOR_HEART.get()) {
            return null;
        }
        BlockEntityTicker<ReactorHeartBlockEntity> ticker = level.isClientSide
                ? MicroreactorHumSound::clientTick
                : ReactorHeartBlockEntity::serverTick;
        return (BlockEntityTicker<T>) ticker;
    }
}
