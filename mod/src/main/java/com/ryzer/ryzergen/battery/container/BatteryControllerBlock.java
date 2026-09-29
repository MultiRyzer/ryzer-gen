package com.ryzer.ryzergen.battery.container;

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
 * The Container Battery's controller: placed first, on the ground, facing the player; the
 * container is built round it, and it moves up into its console once formed. Before then it builds
 * the container from the parts fed into it; after, it runs the battery.
 */
public class BatteryControllerBlock extends ContainerPartBlock implements EntityBlock {
    public static final MapCodec<BatteryControllerBlock> CODEC = simpleCodec(BatteryControllerBlock::new);

    public BatteryControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

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
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BatteryControllerBlockEntity controller) {
            player.openMenu(controller);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BatteryControllerBlockEntity controller) {
            controller.dropAll(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.BATTERY_CONTROLLER.get()) {
            return null;
        }
        return level.isClientSide
                ? (lvl, pos, st, be) -> BatteryControllerBlockEntity.clientTick(lvl, pos, st, (BatteryControllerBlockEntity) be)
                : (lvl, pos, st, be) -> BatteryControllerBlockEntity.serverTick(lvl, pos, st, (BatteryControllerBlockEntity) be);
    }
}
