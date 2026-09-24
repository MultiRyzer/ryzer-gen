package com.ryzer.ryzergen.machine.fission;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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

/**
 * A fission station part: casing, glass or the turbine rotor (the control core extends this). Once
 * the station forms, the parts stop drawing themselves and the core draws the whole station.
 * Placing the last part forms it; breaking any part takes it apart.
 */
public class StationPartBlock extends Block {
    public static final MapCodec<StationPartBlock> CODEC = simpleCodec(StationPartBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public StationPartBlock(Properties properties) {
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
            StationStructure.tryForm(server, pos);
        }
    }

    /**
     * Right-click the core, or any part of a formed station, for the core's panel: its parts store
     * while building (design section 8), its controls once formed.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        boolean core = this instanceof StationCoreBlock;
        if (!core && !state.getValue(FORMED)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            BlockPos corePos = core ? pos : StationStructure.coreOf(server, pos);
            if (corePos != null && level.getBlockEntity(corePos) instanceof StationCoreBlockEntity entity) {
                player.openMenu(entity);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
