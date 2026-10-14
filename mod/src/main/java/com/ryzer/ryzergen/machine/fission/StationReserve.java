package com.ryzer.ryzergen.machine.fission;

import com.ryzer.ryzergen.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Grows and shrinks a fission station's coolant reserve (design section 8). The reserve counts its
 * rings of bays from the back's centre outwards and stops at the first ring that is not complete, so
 * it always grows symmetrically: 1 bay, 3 or 5. Only a formed station has a reserve; its blocks form
 * with their bays and show again when a bay is lost.
 */
public final class StationReserve {
    private StationReserve() {}

    /** Complete rings of reserve blocks round a station whose core is at {@code core}, 0 to 3. */
    public static int rings(BlockGetter level, BlockPos core, Direction facing) {
        int rings = 0;
        for (Set<BlockPos> ring : StationLayout.RESERVE) {
            for (BlockPos cell : ring) {
                if (!level.getBlockState(StationLayout.toWorld(core, facing, cell)).is(ModBlocks.COOLANT_RESERVE.get())) {
                    return rings;
                }
            }
            rings++;
        }
        return rings;
    }

    /** A reserve block was placed or broken at {@code pos}: the station it belongs to, if any, looks again. */
    static void changed(ServerLevel level, BlockPos pos) {
        // The reserve is at most 3 layers high and reaches 14 blocks across from the core.
        for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-14, -2, -14), pos.offset(14, 0, 14))) {
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof StationCoreBlock && state.getValue(StationPartBlock.FORMED)
                    && level.getBlockEntity(candidate) instanceof StationCoreBlockEntity core && owns(core, pos)) {
                refresh(level, core);
                return;
            }
        }
    }

    private static boolean owns(StationCoreBlockEntity core, BlockPos pos) {
        for (Set<BlockPos> ring : StationLayout.RESERVE) {
            for (BlockPos cell : ring) {
                if (StationLayout.toWorld(core.getBlockPos(), core.facing(), cell).equals(pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Counts a formed station's reserve again and forms its blocks to match. */
    static void refresh(ServerLevel level, StationCoreBlockEntity core) {
        int rings = rings(level, core.getBlockPos(), core.facing());
        int before = core.runner().reserveBays();
        setFormed(level, core.getBlockPos(), core.facing(), rings);
        int bays = StationLayout.reserveBays(rings);
        core.runner().setReserveBays(bays);
        if (bays > before) {
            level.playSound(null, StationLayout.toWorld(core.getBlockPos(), core.facing(), new BlockPos(6, 1, 12)),
                    SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.8F, 1.1F);
        }
    }

    /** Shows every reserve block again: the station has come apart. */
    static void release(ServerLevel level, BlockPos core, Direction facing) {
        setFormed(level, core, facing, 0);
    }

    /** Forms the blocks of the first {@code rings} rings and unforms the rest. */
    private static void setFormed(ServerLevel level, BlockPos core, Direction facing, int rings) {
        for (int ring = 0; ring < StationLayout.RESERVE.size(); ring++) {
            boolean formed = ring < rings;
            for (BlockPos cell : StationLayout.RESERVE.get(ring)) {
                BlockPos pos = StationLayout.toWorld(core, facing, cell);
                BlockState state = level.getBlockState(pos);
                if (state.is(ModBlocks.COOLANT_RESERVE.get()) && state.getValue(StationPartBlock.FORMED) != formed) {
                    level.setBlock(pos, state.setValue(StationPartBlock.FORMED, formed), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
