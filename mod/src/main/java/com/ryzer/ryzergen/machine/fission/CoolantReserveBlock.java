package com.ryzer.ryzergen.machine.fission;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A block of the fission station's coolant reserve (design section 8), placed in the reserve's
 * spaces on a station's back. A bay forms once its ring is complete and the rings inside it are too;
 * formed, the blocks stop drawing themselves and the station's core draws the reserve. A station
 * part like the others, but it grows the reserve rather than forming the station.
 */
public class CoolantReserveBlock extends StationPartBlock {
    public static final MapCodec<CoolantReserveBlock> CODEC = simpleCodec(CoolantReserveBlock::new);

    public CoolantReserveBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            StationReserve.changed(server, pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Not the station part's: breaking the reserve shrinks it, it never takes the station apart.
        if (level instanceof ServerLevel server && !newState.is(this)) {
            StationReserve.changed(server, pos);
        }
    }
}
