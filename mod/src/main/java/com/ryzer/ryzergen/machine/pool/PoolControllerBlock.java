package com.ryzer.ryzergen.machine.pool;

import com.mojang.serialization.MapCodec;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The pool's controller: placed first, its screen facing the player, and the pool is built behind
 * and beside it (it sits front and centre, on the middle row). Before the pool forms it builds it
 * from the parts fed into it; once formed it runs the pool.
 */
public class PoolControllerBlock extends PoolPartBlock implements EntityBlock {
    public static final MapCodec<PoolControllerBlock> CODEC = simpleCodec(PoolControllerBlock::new);

    public PoolControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /** Faces the player, so the front with its windows and console is the side they see. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Unformed, the controller's own screen is its parts store. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (isFormed(state)) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PoolControllerBlockEntity controller) {
            player.openMenu(controller);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PoolControllerBlockEntity controller) {
            controller.dropAll(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Passes block events (the crane's run) to the controller, on both sides. */
    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PoolControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.POOL_CONTROLLER.get()) {
            return null;
        }
        return level.isClientSide
                ? (lvl, pos, st, be) -> PoolControllerBlockEntity.clientTick(lvl, pos, st, (PoolControllerBlockEntity) be)
                : (lvl, pos, st, be) -> PoolControllerBlockEntity.serverTick(lvl, pos, st, (PoolControllerBlockEntity) be);
    }
}
