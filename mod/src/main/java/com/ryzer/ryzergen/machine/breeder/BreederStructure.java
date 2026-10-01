package com.ryzer.ryzergen.machine.breeder;

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
 * Forms and breaks breeder reactors. A reactor forms when every space in {@link BreederLayout} round
 * an unformed control core holds its part; breaking any part of a formed reactor takes the whole
 * thing apart again, back to separate blocks.
 */
public final class BreederStructure {
    /** Any part is at most this far across, and this far up, from its core. */
    private static final int REACH = BreederLayout.SIZE;
    private static final int HEIGHT = BreederLayout.HEIGHT;

    private BreederStructure() {}

    /** Called when any part is placed: forms the reactor it completes, if there is one. */
    static void tryForm(ServerLevel level, BlockPos placed) {
        BlockPos core = findCore(level, placed, false);
        if (core != null) {
            form(level, core, level.getBlockState(core).getValue(BreederCoreBlock.FACING));
        }
    }

    /** Called when a part of a formed reactor is removed; {@code was} is the part's state before. */
    static void breakApart(ServerLevel level, BlockPos removed, BlockState was) {
        BlockPos core;
        Direction facing;
        if (was.getBlock() instanceof BreederCoreBlock) {
            // The core itself is gone already, so it cannot be searched for.
            core = removed;
            facing = was.getValue(BreederCoreBlock.FACING);
        } else {
            core = findCore(level, removed, true);
            if (core == null) {
                RyzerGen.LOGGER.warn("Breeder part broken at {}, but no formed core owns it", removed);
                return;
            }
            facing = level.getBlockState(core).getValue(BreederCoreBlock.FACING);
        }
        for (BlockPos cell : BreederLayout.PARTS.keySet()) {
            setFormed(level, BreederLayout.toWorld(core, facing, cell), false);
        }
        level.playSound(null, core.above(4), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
    }

    /**
     * The core whose reactor includes {@code pos}: formed ones when breaking, unformed ones whose
     * reactor is now complete when placing.
     */
    private static @Nullable BlockPos findCore(ServerLevel level, BlockPos pos, boolean formed) {
        for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-REACH, -HEIGHT, -REACH), pos.offset(REACH, 0, REACH))) {
            BlockState state = level.getBlockState(candidate);
            if (!(state.getBlock() instanceof BreederCoreBlock) || state.getValue(BreederPartBlock.FORMED) != formed) {
                continue;
            }
            BlockPos core = candidate.immutable();
            Direction facing = state.getValue(BreederCoreBlock.FACING);
            if (formed ? contains(core, facing, pos) || core.equals(pos) : isComplete(level, core, facing)) {
                return core;
            }
        }
        return null;
    }

    private static boolean contains(BlockPos core, Direction facing, BlockPos pos) {
        for (BlockPos cell : BreederLayout.PARTS.keySet()) {
            if (BreederLayout.toWorld(core, facing, cell).equals(pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isComplete(ServerLevel level, BlockPos core, Direction facing) {
        for (Map.Entry<BlockPos, BreederPart> entry : BreederLayout.PARTS.entrySet()) {
            BlockState state = level.getBlockState(BreederLayout.toWorld(core, facing, entry.getKey()));
            if (!state.is(entry.getValue().block()) || state.getValue(BreederPartBlock.FORMED)) {
                return false;
            }
        }
        return true;
    }

    private static void form(ServerLevel level, BlockPos core, Direction facing) {
        for (BlockPos cell : BreederLayout.PARTS.keySet()) {
            setFormed(level, BreederLayout.toWorld(core, facing, cell), true);
        }
        // The parts store's job is done: whatever is left pops out in front of the core.
        if (level.getBlockEntity(core) instanceof BreederCoreBlockEntity entity) {
            entity.dropContents(level, core.relative(facing));
        }
        BlockPos middle = BreederLayout.toWorld(core, facing, new BlockPos(4, 4, 4));
        level.playSound(null, middle, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.playSound(null, middle, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, middle.getX() + 0.5, middle.getY() + 0.5, middle.getZ() + 0.5, 80, 3, 3, 3, 0.05);
    }

    private static void setFormed(ServerLevel level, BlockPos pos, boolean formed) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BreederPartBlock && state.getValue(BreederPartBlock.FORMED) != formed) {
            level.setBlock(pos, state.setValue(BreederPartBlock.FORMED, formed).setValue(BreederPartBlock.LIT, false), Block.UPDATE_CLIENTS);
        }
    }
}
