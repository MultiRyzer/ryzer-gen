package com.ryzer.ryzergen.machine.drill;

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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * A creative-only preview of the mega drill (design section 11c), before the real multiblock exists:
 * place it where the middle of the drill's pit should be and it draws the whole concept design round
 * itself, 9 blocks across and 16 high, the ladder on the side facing you. A redstone signal runs it:
 * it charges, fires and bursts the 16 x 16 round it a layer at a time, from the ground it stands on
 * down to bedrock (MegaDrillPreviewBlockEntity), lighting the ground round it; without one it stands
 * idle. Right-click steps its speed. The block itself is not drawn, as it sits in the pit under the
 * beam; aim at the pit's middle to use or break it, and turn it with the wrench.
 */
public class MegaDrillPreviewBlock extends BaseEntityBlock {
    public static final MapCodec<MegaDrillPreviewBlock> CODEC = simpleCodec(MegaDrillPreviewBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public MegaDrillPreviewBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    /** The front (with the ladder) faces the player who placed it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
    }

    /** Steps the speed shown (1x, 2x, 4x), as speed upgrades will. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MegaDrillPreviewBlockEntity drill) {
            player.displayClientMessage(Component.translatable("message.ryzergen.mega_drill_preview.speed", drill.cycleSpeed()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.MEGA_DRILL_PREVIEW.get()) {
            return null;
        }
        return level.isClientSide
                ? (lvl, pos, st, be) -> MegaDrillPreviewBlockEntity.clientTick(lvl, pos, st, (MegaDrillPreviewBlockEntity) be)
                : (lvl, pos, st, be) -> MegaDrillPreviewBlockEntity.serverTick(lvl, pos, st, (MegaDrillPreviewBlockEntity) be);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MegaDrillPreviewBlockEntity(pos, state);
    }
}
