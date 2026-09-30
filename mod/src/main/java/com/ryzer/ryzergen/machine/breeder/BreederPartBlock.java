package com.ryzer.ryzergen.machine.breeder;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A breeder reactor part: frame or shell (the control core extends this). Once the reactor forms,
 * the parts stop drawing themselves and the core draws the whole reactor. Placing the last part
 * forms it; breaking any part takes it apart.
 */
public class BreederPartBlock extends Block {
    public static final MapCodec<BreederPartBlock> CODEC = simpleCodec(BreederPartBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public BreederPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            BreederStructure.tryForm(server, pos);
        }
    }

    /**
     * Right-click the front panel for the core's screen: its parts store while building. The panel
     * is the core and the blocks either side of it; the rest of the reactor is only structure.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos corePos = this instanceof BreederCoreBlock ? pos : state.getValue(FORMED) ? panelCore(level, pos) : null;
        if (corePos == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(corePos) instanceof BreederCoreBlockEntity entity) {
            player.openMenu(entity);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The core, if {@code pos} is either side of a formed one along its front. */
    private static @Nullable BlockPos panelCore(Level level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = pos.relative(dir);
            BlockState core = level.getBlockState(candidate);
            if (core.getBlock() instanceof BreederCoreBlock && core.getValue(FORMED)
                    && core.getValue(BreederCoreBlock.FACING).getAxis() != dir.getAxis()) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !newState.is(this)) {
            if (state.getValue(FORMED)) {
                BreederStructure.breakApart(server, pos, state);
            }
            if (level.getBlockEntity(pos) instanceof BreederCoreBlockEntity core) {
                core.dropContents(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
