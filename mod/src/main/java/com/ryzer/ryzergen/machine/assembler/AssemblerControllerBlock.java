package com.ryzer.ryzergen.machine.assembler;

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
 * The Assembler's controller: placed first on the ground, its console facing the player, and the
 * cell is built behind, beside and above it (it sits front and centre on the bottom row). Before
 * the cell forms it builds it from the parts fed into it; once formed it runs it.
 */
public class AssemblerControllerBlock extends AssemblerPartBlock implements EntityBlock {
    public static final MapCodec<AssemblerControllerBlock> CODEC = simpleCodec(AssemblerControllerBlock::new);

    public AssemblerControllerBlock(Properties properties) {
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
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AssemblerBlockEntity controller) {
            player.openMenu(controller, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AssemblerBlockEntity controller) {
            controller.dropAll(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AssemblerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.ASSEMBLER.get()) {
            return null;
        }
        return level.isClientSide
                ? (lvl, pos, st, be) -> AssemblerBlockEntity.clientTick(lvl, pos, st, (AssemblerBlockEntity) be)
                : (lvl, pos, st, be) -> AssemblerBlockEntity.serverTick(lvl, pos, st, (AssemblerBlockEntity) be);
    }
}
