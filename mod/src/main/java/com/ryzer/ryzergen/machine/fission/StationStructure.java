package com.ryzer.ryzergen.machine.fission;

import com.ryzer.ryzergen.advancement.Milestone;
import com.ryzer.ryzergen.registry.ModTriggers;
import net.minecraft.world.phys.Vec3;
import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Forms and breaks fission stations. A station forms when every space in {@link StationLayout}
 * round an unformed control core holds its part; breaking any part of a formed station takes the
 * whole thing apart again, back to separate blocks.
 */
public final class StationStructure {
    /** Any part is at most this far across, and this far up, from its core. */
    private static final int REACH = StationLayout.SIZE;
    private static final int HEIGHT = StationLayout.HEIGHT;

    private StationStructure() {}

    /** Called when any part is placed: forms the station it completes, if there is one. */
    static void tryForm(ServerLevel level, BlockPos placed) {
        BlockPos core = findCore(level, placed, false);
        if (core != null) {
            form(level, core, level.getBlockState(core).getValue(StationCoreBlock.FACING));
        }
    }

    /** Called when a part of a formed station is removed; {@code was} is the part's state before. */
    static void breakApart(ServerLevel level, BlockPos removed, BlockState was) {
        BlockPos core;
        Direction facing;
        if (was.getBlock() instanceof StationCoreBlock) {
            // The core itself is gone already, so it cannot be searched for.
            core = removed;
            facing = was.getValue(StationCoreBlock.FACING);
        } else {
            core = findCore(level, removed, true);
            if (core == null) {
                RyzerGen.LOGGER.warn("Fission station part broken at {}, but no formed core owns it", removed);
                return;
            }
            facing = level.getBlockState(core).getValue(StationCoreBlock.FACING);
        }
        for (BlockPos cell : StationLayout.PARTS.keySet()) {
            setFormed(level, StationLayout.toWorld(core, facing, cell), false);
        }
        level.playSound(null, core.above(5), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
        RyzerGen.LOGGER.debug("Fission station at {} taken apart ({} broken)", core, removed);
    }

    /**
     * The core behind a port block: a formed core within a few blocks on the same level whose
     * {@code port} is at {@code pos}. Used by the port capabilities, so it looks only nearby.
     */
    public static @Nullable StationCoreBlockEntity coreForPort(net.minecraft.world.level.Level level, BlockPos pos,
                                                               StationLayout.Port port) {
        for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-6, 0, -6), pos.offset(6, 0, 6))) {
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof StationCoreBlock && state.getValue(StationPartBlock.FORMED)
                    && StationLayout.portPos(candidate, state.getValue(StationCoreBlock.FACING), port).equals(pos)
                    && level.getBlockEntity(candidate) instanceof StationCoreBlockEntity core) {
                return core;
            }
        }
        return null;
    }

    /**
     * The core whose station includes {@code pos}: formed ones when breaking, unformed ones whose
     * station is now complete when placing.
     */
    private static @Nullable BlockPos findCore(ServerLevel level, BlockPos pos, boolean formed) {
        for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-REACH, -HEIGHT, -REACH), pos.offset(REACH, 0, REACH))) {
            BlockState state = level.getBlockState(candidate);
            if (!(state.getBlock() instanceof StationCoreBlock) || state.getValue(StationPartBlock.FORMED) != formed) {
                continue;
            }
            BlockPos core = candidate.immutable();
            Direction facing = state.getValue(StationCoreBlock.FACING);
            if (formed ? contains(core, facing, pos) || core.equals(pos) : isComplete(level, core, facing)) {
                return core;
            }
        }
        return null;
    }

    private static boolean contains(BlockPos core, Direction facing, BlockPos pos) {
        for (BlockPos cell : StationLayout.PARTS.keySet()) {
            if (StationLayout.toWorld(core, facing, cell).equals(pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isComplete(ServerLevel level, BlockPos core, Direction facing) {
        for (Map.Entry<BlockPos, StationPart> entry : StationLayout.PARTS.entrySet()) {
            BlockState state = level.getBlockState(StationLayout.toWorld(core, facing, entry.getKey()));
            if (!state.is(entry.getValue().block()) || state.getValue(StationPartBlock.FORMED)) {
                return false;
            }
        }
        return true;
    }

    private static void form(ServerLevel level, BlockPos core, Direction facing) {
        for (BlockPos cell : StationLayout.PARTS.keySet()) {
            setFormed(level, StationLayout.toWorld(core, facing, cell), true);
        }
        // The parts store's job is done: whatever is left pops out in front of the core.
        if (level.getBlockEntity(core) instanceof StationCoreBlockEntity entity) {
            entity.dropContents(level, core.relative(facing));
        }
        level.playSound(null, core.above(5), SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.playSound(null, core.above(5), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, core.getX() + 0.5, core.getY() + 3, core.getZ() + 0.5, 80, 4, 2.5, 4, 0.05);
        ModTriggers.MILESTONE.get().triggerNearby(level, Vec3.atCenterOf(core.above(3)), Milestone.STATION_FORMED);
    }

    private static void setFormed(ServerLevel level, BlockPos pos, boolean formed) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof StationPartBlock && state.getValue(StationPartBlock.FORMED) != formed) {
            level.setBlock(pos, state.setValue(StationPartBlock.FORMED, formed).setValue(StationPartBlock.LIT, false), Block.UPDATE_CLIENTS);
            // Ports appear and vanish with the station.
            level.invalidateCapabilities(pos);
        }
    }
}
