package com.ryzer.ryzergen.machine.assembler;

import com.ryzer.ryzergen.advancement.Milestone;
import com.ryzer.ryzergen.registry.ModTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Forms and breaks Assemblers. One forms when every cell of {@link AssemblerLayout} round an
 * unformed controller holds its part; breaking any part of a formed one takes it apart again.
 */
public final class AssemblerStructure {
    /** Set while one forms, so the blocks it changes do not start forming it again. */
    private static boolean forming;

    private AssemblerStructure() {}

    /** Called when a part is placed: forms the Assembler it completes, if there is one. */
    static void tryForm(ServerLevel level, BlockPos placed) {
        if (forming) {
            return;
        }
        int reach = AssemblerLayout.SIZE;
        for (BlockPos candidate : BlockPos.betweenClosed(placed.offset(-reach, -reach, -reach), placed.offset(reach, reach, reach))) {
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof AssemblerControllerBlock && !AssemblerPartBlock.isFormed(state)) {
                BlockPos controller = candidate.immutable();
                Direction facing = state.getValue(AssemblerPartBlock.FACING);
                if (isComplete(level, controller, facing)) {
                    forming = true;
                    try {
                        form(level, controller, facing);
                    } finally {
                        forming = false;
                    }
                    return;
                }
            }
        }
    }

    private static boolean isComplete(ServerLevel level, BlockPos controller, Direction facing) {
        for (int index = 1; index <= AssemblerLayout.CELLS; index++) {
            BlockPos cell = AssemblerLayout.cell(index);
            BlockState state = level.getBlockState(AssemblerLayout.toWorld(controller, facing, cell));
            if (!state.is(AssemblerLayout.partAt(cell).block()) || AssemblerPartBlock.isFormed(state)) {
                return false;
            }
        }
        return true;
    }

    private static void form(ServerLevel level, BlockPos controller, Direction facing) {
        if (level.getBlockEntity(controller) instanceof AssemblerBlockEntity entity) {
            entity.dropParts(level, controller.relative(facing));
        }
        for (int index = 1; index <= AssemblerLayout.CELLS; index++) {
            BlockPos pos = AssemblerLayout.toWorld(controller, facing, AssemblerLayout.cell(index));
            BlockState state = level.getBlockState(pos).setValue(AssemblerPartBlock.FACING, facing)
                    .setValue(AssemblerPartBlock.CELL, index).setValue(AssemblerPartBlock.WORKING, false);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
        refreshPorts(level, controller, facing);
        Vec3 centre = centre(controller, facing);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, 40, 1.2, 1.0, 1.2, 0.05);
        ModTriggers.MILESTONE.get().triggerNearby(level, centre, Milestone.ASSEMBLER_FORMED);
    }

    /** Called when a part of a formed Assembler is removed; {@code was} is the part's state before. */
    static void breakApart(ServerLevel level, BlockPos removed, BlockState was) {
        Direction facing = was.getValue(AssemblerPartBlock.FACING);
        BlockPos controller = AssemblerLayout.controllerFrom(removed, facing, AssemblerLayout.cell(was.getValue(AssemblerPartBlock.CELL)));
        if (level.getBlockEntity(controller) instanceof AssemblerBlockEntity entity) {
            entity.abandonCycle();
        }
        for (int index = 1; index <= AssemblerLayout.CELLS; index++) {
            BlockPos pos = AssemblerLayout.toWorld(controller, facing, AssemblerLayout.cell(index));
            BlockState state = level.getBlockState(pos);
            if (!pos.equals(removed) && state.getBlock() instanceof AssemblerPartBlock
                    && state.getValue(AssemblerPartBlock.CELL) == index && state.getValue(AssemblerPartBlock.FACING) == facing) {
                level.setBlock(pos, state.setValue(AssemblerPartBlock.CELL, 0).setValue(AssemblerPartBlock.WORKING, false), Block.UPDATE_CLIENTS);
            }
        }
        refreshPorts(level, controller, facing);
        Vec3 centre = centre(controller, facing);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** Switches every block of a formed Assembler between its idle and working looks. */
    static void setWorking(ServerLevel level, BlockPos controller, Direction facing, boolean working) {
        for (int index = 1; index <= AssemblerLayout.CELLS; index++) {
            BlockPos pos = AssemblerLayout.toWorld(controller, facing, AssemblerLayout.cell(index));
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof AssemblerPartBlock && state.getValue(AssemblerPartBlock.CELL) == index
                    && state.getValue(AssemblerPartBlock.WORKING) != working) {
                level.setBlock(pos, state.setValue(AssemblerPartBlock.WORKING, working), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Ports appear or vanish when it forms or breaks, so pipes must look again. */
    private static void refreshPorts(ServerLevel level, BlockPos controller, Direction facing) {
        for (AssemblerLayout.Port port : AssemblerLayout.Port.values()) {
            BlockPos pos = AssemblerLayout.toWorld(controller, facing, port.cell());
            level.invalidateCapabilities(pos);
            BlockState state = level.getBlockState(pos);
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    /** The middle of the cell, for sounds and particles. */
    static Vec3 centre(BlockPos controller, Direction facing) {
        return Vec3.atCenterOf(AssemblerLayout.toWorld(controller, facing, AssemblerLayout.ARM));
    }
}
