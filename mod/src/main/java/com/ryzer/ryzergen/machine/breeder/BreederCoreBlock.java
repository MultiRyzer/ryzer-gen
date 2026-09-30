package com.ryzer.ryzergen.machine.breeder;

import com.mojang.serialization.MapCodec;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/**
 * The breeder reactor's control core: placed first, front and centre of the plinth, its screen
 * facing the player. The reactor is built behind it. Once formed it draws the whole reactor.
 */
public class BreederCoreBlock extends BreederPartBlock implements EntityBlock {
    public static final MapCodec<BreederCoreBlock> CODEC = simpleCodec(BreederCoreBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public BreederCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    /** Faces the player, so the front with its screen is the side they are looking at. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BreederCoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.BREEDER_CORE.get()) {
            return null;
        }
        return level.isClientSide
                ? (lvl, pos, st, be) -> BreederCoreBlockEntity.clientTick(lvl, pos, st, (BreederCoreBlockEntity) be)
                : (lvl, pos, st, be) -> BreederCoreBlockEntity.serverTick(lvl, pos, st, (BreederCoreBlockEntity) be);
    }
}
