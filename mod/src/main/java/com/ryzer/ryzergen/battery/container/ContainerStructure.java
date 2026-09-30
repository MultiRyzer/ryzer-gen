package com.ryzer.ryzergen.battery.container;

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
 * Forms and breaks Container Batteries, as PoolStructure does pools. A container forms when every
 * cell of {@link ContainerLayout} round an unformed controller holds its part; breaking any part
 * takes it apart again. The rack modules stay in the controller while it stands, and show again
 * when the container is rebuilt.
 */
public final class ContainerStructure {
    /** Set while a container forms, so the blocks it moves do not start forming it again. */
    private static boolean forming;

    private ContainerStructure() {}

    static void tryForm(ServerLevel level, BlockPos placed) {
        if (forming) {
            return;
        }
        int reach = ContainerLayout.LONG;
        for (BlockPos candidate : BlockPos.betweenClosed(placed.offset(-reach, -ContainerLayout.HIGH, -reach),
                placed.offset(reach, ContainerLayout.HIGH, reach))) {
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof BatteryControllerBlock && !ContainerPartBlock.isFormed(state)) {
                BlockPos controller = candidate.immutable();
                Direction facing = state.getValue(ContainerPartBlock.FACING);
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

    /** Whether an unformed controller sits in its formed place (frame below it) rather than on the ground. */
    static boolean isRaised(Level level, BlockPos controller) {
        BlockState below = level.getBlockState(controller.below());
        return below.is(ContainerPart.FRAME.block()) && !ContainerPartBlock.isFormed(below);
    }

    private static boolean isComplete(ServerLevel level, BlockPos controller, Direction facing, boolean raised) {
        BlockPos anchor = ContainerLayout.anchor(raised);
        for (int index = 1; index <= ContainerLayout.CELLS; index++) {
            BlockPos cell = ContainerLayout.cell(index);
            BlockState state = level.getBlockState(ContainerLayout.toWorld(controller, anchor, facing, cell));
            if (!state.is(ContainerLayout.partAt(cell, raised).block()) || ContainerPartBlock.isFormed(state)) {
                return false;
            }
        }
        return true;
    }

    /** Forms the container; a controller built on the ground first swaps with the frame above it. */
    private static void form(ServerLevel level, BlockPos built, Direction facing, boolean raised) {
        if (level.getBlockEntity(built) instanceof BatteryControllerBlockEntity entity) {
            entity.dropParts(level, built.relative(facing));
        }
        BlockPos controller = built;
        if (!raised) {
            controller = built.above();
            level.setBlock(controller, level.getBlockState(built), Block.UPDATE_CLIENTS);
            level.setBlock(built, ContainerPart.FRAME.block().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        BatteryControllerBlockEntity entity = level.getBlockEntity(controller) instanceof BatteryControllerBlockEntity e ? e : null;
        for (int index = 1; index <= ContainerLayout.CELLS; index++) {
            BlockPos cell = ContainerLayout.cell(index);
            BlockPos pos = ContainerLayout.toWorld(controller, facing, cell);
            int slot = ContainerLayout.slotAt(cell);
            BlockState state = level.getBlockState(pos).setValue(ContainerPartBlock.FACING, facing)
                    .setValue(ContainerPartBlock.CELL, index)
                    .setValue(ContainerPartBlock.INSTALLED, entity != null && slot >= 0 && entity.installed(slot));
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
        refreshPorts(level, controller, facing);
        Vec3 centre = centre(controller, facing);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, 1.1F);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.4F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, 50, 3.0, 1.0, 1.0, 0.05);
        ModTriggers.MILESTONE.get().triggerNearby(level, centre, Milestone.CONTAINER_FORMED);
    }

    static void breakApart(ServerLevel level, BlockPos removed, BlockState was) {
        Direction facing = was.getValue(ContainerPartBlock.FACING);
        BlockPos controller = ContainerLayout.controllerFrom(removed, facing, ContainerLayout.cell(was.getValue(ContainerPartBlock.CELL)));
        for (int index = 1; index <= ContainerLayout.CELLS; index++) {
            BlockPos pos = ContainerLayout.toWorld(controller, facing, ContainerLayout.cell(index));
            BlockState state = level.getBlockState(pos);
            if (!pos.equals(removed) && state.getBlock() instanceof ContainerPartBlock
                    && state.getValue(ContainerPartBlock.CELL) == index && state.getValue(ContainerPartBlock.FACING) == facing) {
                level.setBlock(pos, state.setValue(ContainerPartBlock.CELL, 0).setValue(ContainerPartBlock.INSTALLED, false),
                        Block.UPDATE_CLIENTS);
            }
        }
        refreshPorts(level, controller, facing);
        Vec3 centre = centre(controller, facing);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
    }

    /** Shows or hides one slot's rack on the formed container. */
    static void setInstalled(ServerLevel level, BlockPos controller, Direction facing, int slot, boolean installed) {
        BlockPos pos = ContainerLayout.toWorld(controller, facing, ContainerLayout.SLOTS.get(slot));
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ContainerPartBlock && ContainerPartBlock.isFormed(state)) {
            level.setBlock(pos, state.setValue(ContainerPartBlock.INSTALLED, installed), Block.UPDATE_CLIENTS);
        }
    }

    private static void refreshPorts(ServerLevel level, BlockPos controller, Direction facing) {
        for (ContainerLayout.Port port : ContainerLayout.Port.values()) {
            BlockPos pos = ContainerLayout.toWorld(controller, facing, port.cell());
            level.invalidateCapabilities(pos);
            BlockState state = level.getBlockState(pos);
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    static Vec3 centre(BlockPos controller, Direction facing) {
        return Vec3.atCenterOf(ContainerLayout.toWorld(controller, facing, new BlockPos(3, 1, 1)));
    }
}
