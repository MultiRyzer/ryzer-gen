package com.ryzer.ryzergen.machine.sun;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A creative-only preview of the sun gate (design section 11), before the real machine exists:
 * place it where the middle of the gate should be and it draws the concept design round itself, 11
 * blocks across, the console on the side facing you. The sun doubles as the swarm's orrery, so
 * right-click steps the swarm coverage it shows (0 to 100% in tenths; sneak to step back). Nothing
 * is built and it does nothing else; turn it with the wrench, break it to remove it.
 */
public class SunGatePreviewBlock extends BaseEntityBlock {
    public static final MapCodec<SunGatePreviewBlock> CODEC = simpleCodec(SunGatePreviewBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** Swarm coverage shown, in tenths. */
    public static final IntegerProperty COVERAGE = IntegerProperty.create("coverage", 0, 10);

    public SunGatePreviewBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(COVERAGE, 3));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COVERAGE);
    }

    /** The front (with the console) faces the player who placed it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            int step = player.isShiftKeyDown() ? 10 : 1;
            int coverage = (state.getValue(COVERAGE) + step) % 11;
            level.setBlock(pos, state.setValue(COVERAGE, coverage), Block.UPDATE_CLIENTS);
            player.displayClientMessage(Component.translatable("message.ryzergen.sun_gate_preview.coverage", coverage * 10), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SunGatePreviewBlockEntity(pos, state);
    }
}
