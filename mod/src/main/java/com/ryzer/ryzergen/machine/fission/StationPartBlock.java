package com.ryzer.ryzergen.machine.fission;

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
 * A fission station part: casing, glass or the turbine rotor (the control core extends this). Once
 * the station forms, the parts stop drawing themselves and the core draws the whole station.
 * Placing the last part forms it; breaking any part takes it apart.
 */
public class StationPartBlock extends Block {
    public static final MapCodec<StationPartBlock> CODEC = simpleCodec(StationPartBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** Chamber glass of a running station: it gives off light, so the station lights up its surroundings. */
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public StationPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, LIT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            StationStructure.tryForm(server, pos);
        }
    }

    /**
     * Right-click the station's front panel for the core's screen: its parts store while building
     * (design section 8), its controls once formed. The panel is two blocks wide, the core and the
     * block beside it (design cells 5 and 6 on the front row); the rest of the station is only
     * structure.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos corePos = this instanceof StationCoreBlock ? pos : state.getValue(FORMED) ? panelCore(level, pos) : null;
        if (corePos == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(corePos) instanceof StationCoreBlockEntity entity) {
            player.openMenu(entity);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * The core, if {@code pos} is the panel's other half: the block beside a formed core, on its
     * clockwise side (design cell 6, next to the core in cell 5). Works on both sides, so the
     * client only swings for a click that opens the screen.
     */
    private static @Nullable BlockPos panelCore(Level level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = pos.relative(dir);
            BlockState core = level.getBlockState(candidate);
            if (core.getBlock() instanceof StationCoreBlock && core.getValue(FORMED)
                    && candidate.relative(core.getValue(StationCoreBlock.FACING).getClockWise()).equals(pos)) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !newState.is(this)) {
            if (state.getValue(FORMED)) {
                StationStructure.breakApart(server, pos, state);
            }
            if (level.getBlockEntity(pos) instanceof StationCoreBlockEntity core) {
                core.dropAll(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
