package com.ryzer.ryzergen.machine.drill;

import com.ryzer.ryzergen.registry.ModBlocks;
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
 * Forms and breaks mega drills. A drill forms when every cell of {@link MegaDrillLayout} round an
 * unformed controller holds its part (frame, and the controller itself); breaking any part of a
 * formed drill takes the whole thing apart again, back to separate blocks.
 */
public final class MegaDrillStructure {
    /** Set while a drill forms or breaks, so the blocks it changes do not start it again. */
    private static boolean busy;

    private MegaDrillStructure() {}

    /**
     * Called when a part is placed: forms the drill it completes, if there is one. The controller
     * sits on the base's level, so it is never above the part, and at most the footprint away.
     */
    static void tryForm(ServerLevel level, BlockPos placed) {
        if (busy) {
            return;
        }
        int reach = MegaDrillLayout.SIZE;
        for (BlockPos candidate : BlockPos.betweenClosed(placed.offset(-reach, 1 - MegaDrillLayout.HEIGHT, -reach),
                placed.offset(reach, 0, reach))) {
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof MegaDrillControllerBlock && !MegaDrillPartBlock.isFormed(state)) {
                BlockPos controller = candidate.immutable();
                if (isComplete(level, controller, state.getValue(MegaDrillPartBlock.FACING))) {
                    form(level, controller, state.getValue(MegaDrillPartBlock.FACING));
                    return;
                }
            }
        }
    }

    /** Whether every cell round an unformed controller holds its part, none of them in another drill. */
    static boolean isComplete(ServerLevel level, BlockPos controller, Direction facing) {
        for (MegaDrillLayout.Cell cell : MegaDrillLayout.CELL_LIST) {
            if (cell.pos().equals(MegaDrillLayout.CONTROLLER)) {
                continue;
            }
            BlockState state = level.getBlockState(MegaDrillLayout.toWorld(controller, facing, cell.pos()));
            if (!state.is(ModBlocks.DRILL_FRAME.get()) || MegaDrillPartBlock.isFormed(state)) {
                return false;
            }
        }
        return true;
    }

    private static void form(ServerLevel level, BlockPos controller, Direction facing) {
        busy = true;
        try {
            if (level.getBlockEntity(controller) instanceof MegaDrillBlockEntity entity) {
                entity.dropParts(level, controller.relative(facing));
            }
            for (int index = 1; index <= MegaDrillLayout.CELLS; index++) {
                BlockPos pos = MegaDrillLayout.toWorld(controller, facing, MegaDrillLayout.cell(index).pos());
                BlockState state = level.getBlockState(pos).setValue(MegaDrillPartBlock.FACING, facing)
                        .setValue(MegaDrillPartBlock.CELL, index).setValue(MegaDrillPartBlock.LIT, false);
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
            refreshPorts(level, controller, facing);
        } finally {
            busy = false;
        }
        Vec3 centre = Vec3.atCenterOf(MegaDrillLayout.toWorld(controller, facing, new BlockPos(4, 5, 4)));
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 0.9F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, 80, 2.5, 4, 2.5, 0.05);
    }

    /** Called when a part of a formed drill is removed; {@code was} is the part's state before. */
    static void breakApart(ServerLevel level, BlockPos removed, BlockState was) {
        if (busy) {
            return;
        }
        Direction facing = was.getValue(MegaDrillPartBlock.FACING);
        BlockPos controller = MegaDrillLayout.controllerFrom(removed, facing, MegaDrillLayout.cell(was.getValue(MegaDrillPartBlock.CELL)).pos());
        busy = true;
        try {
            for (int index = 1; index <= MegaDrillLayout.CELLS; index++) {
                BlockPos pos = MegaDrillLayout.toWorld(controller, facing, MegaDrillLayout.cell(index).pos());
                BlockState state = level.getBlockState(pos);
                if (!pos.equals(removed) && state.getBlock() instanceof MegaDrillPartBlock
                        && state.getValue(MegaDrillPartBlock.CELL) == index && state.getValue(MegaDrillPartBlock.FACING) == facing) {
                    level.setBlock(pos, state.setValue(MegaDrillPartBlock.CELL, 0).setValue(MegaDrillPartBlock.LIT, false), Block.UPDATE_CLIENTS);
                }
            }
            refreshPorts(level, controller, facing);
        } finally {
            busy = false;
        }
        if (level.getBlockEntity(controller) instanceof MegaDrillBlockEntity entity) {
            entity.brokenApart();
        }
        level.playSound(null, controller.above(2), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
    }

    /** Lights (or darkens) the base's parts, so the drill lights the ground round it while it runs. */
    static void setLit(ServerLevel level, BlockPos controller, Direction facing, boolean lit) {
        for (int index = 1; index <= MegaDrillLayout.CELLS; index++) {
            MegaDrillLayout.Cell cell = MegaDrillLayout.cell(index);
            if (!cell.lit()) {
                continue;
            }
            BlockPos pos = MegaDrillLayout.toWorld(controller, facing, cell.pos());
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof MegaDrillPartBlock && state.getValue(MegaDrillPartBlock.CELL) == index
                    && state.getValue(MegaDrillPartBlock.LIT) != lit) {
                level.setBlock(pos, state.setValue(MegaDrillPartBlock.LIT, lit), Block.UPDATE_CLIENTS);
            }
        }
    }

    /**
     * Ports appear or vanish when the drill forms or breaks, so pipes must look again. All states are
     * set first; neighbours are told only once the whole drill is consistent.
     */
    private static void refreshPorts(ServerLevel level, BlockPos controller, Direction facing) {
        for (MegaDrillLayout.Port port : MegaDrillLayout.Port.values()) {
            BlockPos pos = MegaDrillLayout.toWorld(controller, facing, port.cell());
            level.invalidateCapabilities(pos);
            BlockState state = level.getBlockState(pos);
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }
}
