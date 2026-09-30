package com.ryzer.ryzergen.machine.pool;

import com.ryzer.ryzergen.advancement.Milestone;
import com.ryzer.ryzergen.registry.ModTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Forms and breaks Spent Fuel Pools. A pool forms when every cell of {@link PoolLayout} round an
 * unformed controller holds its part; breaking any part of a formed pool takes the whole thing
 * apart again, back to separate blocks.
 */
public final class PoolStructure {
    /** Set while a pool forms, so the blocks it moves do not start forming it again. */
    private static boolean forming;

    private PoolStructure() {}

    /** Called when a part is placed: forms the pool it completes, if there is one. */
    static void tryForm(ServerLevel level, BlockPos placed) {
        if (forming) {
            return;
        }
        int reach = PoolLayout.LONG;
        for (BlockPos candidate : BlockPos.betweenClosed(placed.offset(-reach, -PoolLayout.HIGH, -reach),
                placed.offset(reach, PoolLayout.HIGH, reach))) {
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof PoolControllerBlock && !PoolPartBlock.isFormed(state)) {
                BlockPos controller = candidate.immutable();
                Direction facing = state.getValue(PoolPartBlock.FACING);
                boolean raised = isRaised(level, controller);
                if (isComplete(level, controller, facing, raised)) {
                    forming = true;
                    try {
                        form(level, controller, facing, raised);
                    } finally {
                        forming = false;
                    }
                    return;
                }
            }
        }
    }

    /**
     * Whether an unformed controller sits in its formed place (liner below it: a pool broken apart
     * and being rebuilt) rather than on the ground.
     */
    static boolean isRaised(Level level, BlockPos controller) {
        BlockState below = level.getBlockState(controller.below());
        return below.is(PoolPart.LINER.block()) && !PoolPartBlock.isFormed(below);
    }

    private static boolean isComplete(ServerLevel level, BlockPos controller, Direction facing, boolean raised) {
        BlockPos anchor = PoolLayout.anchor(raised);
        for (int index = 1; index <= PoolLayout.CELLS; index++) {
            BlockPos cell = PoolLayout.cell(index);
            BlockState state = level.getBlockState(PoolLayout.toWorld(controller, anchor, facing, cell));
            if (!state.is(PoolLayout.partAt(cell, raised).block()) || PoolPartBlock.isFormed(state)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Forms the pool. A controller built on the ground first swaps with the liner above it, so the
     * formed pool has its console on the middle row. The parts store's leftovers drop first, in
     * front of it, since the controller moves.
     */
    private static void form(ServerLevel level, BlockPos built, Direction facing, boolean raised) {
        if (level.getBlockEntity(built) instanceof PoolControllerBlockEntity entity) {
            entity.dropParts(level, built.relative(facing));
        }
        BlockPos controller = built;
        if (!raised) {
            controller = built.above();
            level.setBlock(controller, level.getBlockState(built), Block.UPDATE_CLIENTS);
            level.setBlock(built, PoolPart.LINER.block().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        PoolLook look = level.getBlockEntity(controller) instanceof PoolControllerBlockEntity entity ? entity.look() : PoolLook.DRY;
        for (int index = 1; index <= PoolLayout.CELLS; index++) {
            BlockPos pos = PoolLayout.toWorld(controller, facing, PoolLayout.cell(index));
            BlockState state = level.getBlockState(pos).setValue(PoolPartBlock.FACING, facing)
                    .setValue(PoolPartBlock.CELL, index).setValue(PoolPartBlock.LOOK, look);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
        refreshPorts(level, controller, facing);
        Vec3 centre = centre(controller, facing);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, 1.0F);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, 40, 1.8, 1.0, 1.0, 0.05);
        ModTriggers.MILESTONE.get().triggerNearby(level, centre, Milestone.POOL_FORMED);
    }

    /** Called when a part of a formed pool is removed; {@code was} is the part's state before. */
    static void breakApart(ServerLevel level, BlockPos removed, BlockState was) {
        Direction facing = was.getValue(PoolPartBlock.FACING);
        BlockPos controller = PoolLayout.controllerFrom(removed, facing, PoolLayout.cell(was.getValue(PoolPartBlock.CELL)));
        for (int index = 1; index <= PoolLayout.CELLS; index++) {
            BlockPos pos = PoolLayout.toWorld(controller, facing, PoolLayout.cell(index));
            BlockState state = level.getBlockState(pos);
            if (!pos.equals(removed) && state.getBlock() instanceof PoolPartBlock
                    && state.getValue(PoolPartBlock.CELL) == index && state.getValue(PoolPartBlock.FACING) == facing) {
                level.setBlock(pos, state.setValue(PoolPartBlock.CELL, 0).setValue(PoolPartBlock.LOOK, PoolLook.DRY), Block.UPDATE_CLIENTS);
            }
        }
        refreshPorts(level, controller, facing);
        Vec3 centre = centre(controller, facing);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
    }

    /** Switches every block of a formed pool to a new look: dry, full, or cooling fuel. */
    static void setLook(ServerLevel level, BlockPos controller, Direction facing, PoolLook look) {
        for (int index = 1; index <= PoolLayout.CELLS; index++) {
            BlockPos pos = PoolLayout.toWorld(controller, facing, PoolLayout.cell(index));
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof PoolPartBlock && state.getValue(PoolPartBlock.CELL) == index
                    && state.getValue(PoolPartBlock.LOOK) != look) {
                level.setBlock(pos, state.setValue(PoolPartBlock.LOOK, look), Block.UPDATE_CLIENTS);
            }
        }
    }

    /**
     * Ports appear or vanish when the pool forms or breaks, so pipes must look again. All states are
     * set first; neighbours are told only once the whole pool is consistent.
     */
    private static void refreshPorts(ServerLevel level, BlockPos controller, Direction facing) {
        for (PoolLayout.Port port : PoolLayout.Port.values()) {
            BlockPos pos = PoolLayout.toWorld(controller, facing, port.cell());
            level.invalidateCapabilities(pos);
            BlockState state = level.getBlockState(pos);
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    /** The middle of the pool, for sounds and particles. */
    static Vec3 centre(BlockPos controller, Direction facing) {
        return Vec3.atCenterOf(PoolLayout.toWorld(controller, facing, PoolLayout.CRANE)).subtract(0, 1, 0);
    }
}
