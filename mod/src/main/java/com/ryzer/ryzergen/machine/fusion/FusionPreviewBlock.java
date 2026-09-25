package com.ryzer.ryzergen.machine.fusion;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * A creative-only preview of the fusion reactor (design section 10b), before the real multiblock
 * exists: place it where the middle of the reactor should be and it draws the whole concept design
 * round itself, 18 blocks across, the ports on the side facing you. Nothing is built and it does
 * nothing else; turn it with the wrench, break it to remove it.
 */
public class FusionPreviewBlock extends BaseEntityBlock {
    public static final MapCodec<FusionPreviewBlock> CODEC = simpleCodec(FusionPreviewBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public FusionPreviewBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The front (with the ports) faces the player who placed it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionPreviewBlockEntity(pos, state);
    }
}
