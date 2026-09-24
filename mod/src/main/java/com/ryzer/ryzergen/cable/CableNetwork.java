package com.ryzer.ryzergen.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds what a group of joined cables delivers to. Results are cached by each cable and thrown away
 * whenever any cable changes shape, which is rare compared to how often energy moves.
 */
public final class CableNetwork {
    /** A block face energy can be delivered into: the block at {@code pos}, entered from {@code side}. */
    public record Endpoint(BlockPos pos, Direction side) {}

    /** Bumped whenever any cable connects or disconnects, so cached networks know to rebuild. */
    private static int version;

    private CableNetwork() {}

    public static int version() {
        return version;
    }

    public static void changed() {
        version++;
    }

    /** Every non-cable face the cables joined to {@code start} are connected to (extract sides excluded). */
    public static List<Endpoint> endpoints(Level level, BlockPos start) {
        List<Endpoint> endpoints = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < 4096) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof EnergyCableBlock)) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                CableSide side = state.getValue(EnergyCableBlock.SIDES.get(dir));
                if (side == CableSide.NONE) {
                    continue;
                }
                BlockPos next = pos.relative(dir);
                if (level.getBlockState(next).getBlock() instanceof EnergyCableBlock) {
                    if (seen.add(next)) {
                        queue.add(next);
                    }
                } else if (side == CableSide.CONNECTED) {
                    endpoints.add(new Endpoint(next, dir.getOpposite()));
                }
            }
        }
        return endpoints;
    }
}
