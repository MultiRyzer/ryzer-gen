package com.ryzer.ryzergen.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The networks: each group of joined cables of one kind, with the blocks it delivers to. One
 * network object is shared by all its cables and scanned once, the first time any of them needs it.
 *
 * <p>A change only invalidates the networks it touches: placing, breaking or wrenching a cable, a
 * side connecting or disconnecting, or a cable's chunk loading or unloading, clears the networks at
 * that position and its neighbours, and nothing else in the world. (Before, any change anywhere made
 * every cable of every network rescan, and each cable scanned its whole network for itself.) Scans
 * stop at unloaded chunks rather than loading them; a chunk loading later counts as a change.
 */
public final class CableNetwork {
    /** A block face things can be delivered into: the block at {@code pos}, entered from {@code side}. */
    public record Endpoint(BlockPos pos, Direction side) {}

    static final Direction[] DIRECTIONS = Direction.values();

    /** One network: its kind of cable, its cables, and what it delivers to. */
    public static final class Net {
        final Block kind;
        final List<BlockPos> members;
        final List<Endpoint> endpoints;
        /** Built by the first cable that needs them, then shared (every cable in it carries the same thing). */
        @Nullable List<?> targets;
        /** Where the end-of-tick battery pass starts, moving round each tick (energy networks). */
        int roundRobin;
        boolean valid = true;

        Net(Block kind, List<BlockPos> members, List<Endpoint> endpoints) {
            this.kind = kind;
            this.members = members;
            this.endpoints = endpoints;
        }

        public List<Endpoint> endpoints() {
            return endpoints;
        }

        public boolean valid() {
            return valid;
        }
    }

    /** One level's networks. */
    private static final class Registry {
        /** The network each cable position belongs to. */
        final Map<BlockPos, Net> byPos = new HashMap<>();
        /** The energy networks, for the end-of-tick battery pass (so it never walks every cable). */
        final Set<Net> energy = new LinkedHashSet<>();
        /** Positions to (re)scan at the end of the tick, so energy networks exist for their batteries. */
        final Set<BlockPos> pending = new LinkedHashSet<>();
    }

    private static final Map<Level, Registry> REGISTRIES = new WeakHashMap<>();

    private CableNetwork() {}

    private static Registry registry(Level level) {
        return REGISTRIES.computeIfAbsent(level, l -> new Registry());
    }

    /** The network the cable at {@code pos} belongs to, scanning it if nothing has since the last change. */
    public static Net get(ServerLevel level, BlockPos pos) {
        Registry registry = registry(level);
        Net net = registry.byPos.get(pos);
        if (net != null && net.valid) {
            return net;
        }
        net = scan(level, pos);
        for (BlockPos member : net.members) {
            registry.byPos.put(member, net);
        }
        if (net.kind instanceof EnergyCableBlock) {
            registry.energy.add(net);
        }
        return net;
    }

    /**
     * Something changed at {@code pos} (a cable placed, broken, wrenched, loaded or unloaded, or a
     * side joining or leaving): the networks there and next to it are rebuilt. Server only.
     */
    public static void changed(LevelAccessor accessor, BlockPos pos) {
        if (!(accessor instanceof ServerLevel level)) {
            return;
        }
        Registry registry = registry(level);
        invalidate(registry, registry.byPos.get(pos));
        registry.pending.add(pos.immutable());
        for (Direction dir : DIRECTIONS) {
            BlockPos next = pos.relative(dir);
            invalidate(registry, registry.byPos.get(next));
            registry.pending.add(next);
        }
    }

    private static void invalidate(Registry registry, @Nullable Net net) {
        if (net == null || !net.valid) {
            return;
        }
        net.valid = false;
        registry.energy.remove(net);
        for (BlockPos member : net.members) {
            registry.byPos.remove(member, net);
        }
    }

    /**
     * End of tick: rescans the networks changed this tick (once each, however many of their cables
     * changed), then lets each energy network top its machines up from its batteries.
     */
    static void endOfTick(ServerLevel level) {
        Registry registry = REGISTRIES.get(level);
        if (registry == null) {
            return;
        }
        if (!registry.pending.isEmpty()) {
            List<BlockPos> todo = new ArrayList<>(registry.pending);
            registry.pending.clear();
            for (BlockPos pos : todo) {
                if (level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof CableBlock) {
                    get(level, pos);
                }
            }
        }
        for (Net net : List.copyOf(registry.energy)) {
            EnergyCableBlockEntity.drain(level, net);
        }
    }

    /** Walks the joined cables of one kind from {@code start}, stopping at unloaded chunks. */
    private static Net scan(Level level, BlockPos start) {
        List<Endpoint> endpoints = new ArrayList<>();
        List<BlockPos> members = new ArrayList<>();
        // Only cables of the starting cable's own kind join up: energy cables and item pipes never mix.
        Block kind = level.getBlockState(start).getBlock();
        Set<BlockPos> seen = new java.util.HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.immutable());
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!state.is(kind)) {
                continue;
            }
            members.add(pos);
            for (Direction dir : DIRECTIONS) {
                CableSide side = state.getValue(CableBlock.SIDES.get(dir));
                if (side == CableSide.NONE) {
                    continue;
                }
                BlockPos next = pos.relative(dir);
                if (!level.isLoaded(next)) {
                    continue;
                }
                if (level.getBlockState(next).is(kind)) {
                    if (seen.add(next)) {
                        queue.add(next);
                    }
                } else if (side == CableSide.CONNECTED) {
                    endpoints.add(new Endpoint(next, dir.getOpposite()));
                }
            }
        }
        return new Net(kind, members, endpoints);
    }
}
