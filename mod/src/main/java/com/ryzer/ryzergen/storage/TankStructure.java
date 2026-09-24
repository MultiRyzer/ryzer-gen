package com.ryzer.ryzergen.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Joins tank blocks (pressure tanks, or fluid tanks) into towers. Whenever one is placed or broken,
 * the touching group of the same kind is worked out again: an exact 2 x 2 column up to
 * {@link #MAX_HEIGHT} high becomes one tower, and anything else stays as separate single tanks. The
 * contents are pooled into the tower's controller, or shared out evenly between single tanks.
 * Breaking a block spills (or vents) its share, as it would in real life.
 */
public final class TankStructure {
    public static final int MAX_HEIGHT = 16;
    private static final int SEARCH_LIMIT = 4 * MAX_HEIGHT + 32;

    private TankStructure() {}

    static void placed(ServerLevel level, BlockPos pos) {
        Block kind = level.getBlockState(pos).getBlock();
        Set<BlockPos> group = group(level, kind, pos, null);
        rebuild(level, group, collect(level, group));
    }

    static void removed(ServerLevel level, BlockPos pos, PressureTankBlockEntity removed) {
        Block kind = removed.getBlockState().getBlock();
        PressureTankBlockEntity controller = removed.controllerEntity();
        int oldBlocks = controller.blocks();
        FluidStack gas = controller.takeContents();
        // The broken block's share escapes.
        gas.shrink(gas.getAmount() / Math.max(1, oldBlocks));

        List<Set<BlockPos>> groups = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (Direction dir : Direction.values()) {
            BlockPos next = pos.relative(dir);
            if (!seen.contains(next) && level.getBlockState(next).is(kind)) {
                Set<BlockPos> group = group(level, kind, next, pos);
                seen.addAll(group);
                groups.add(group);
            }
        }
        int total = groups.stream().mapToInt(Set::size).sum();
        for (Set<BlockPos> group : groups) {
            FluidStack share = gas.isEmpty() ? FluidStack.EMPTY : gas.copyWithAmount((int) ((long) gas.getAmount() * group.size() / total));
            rebuild(level, group, merge(share, collect(level, group)));
        }
    }

    /**
     * Every tank block of one kind touching {@code start}, leaving out {@code except} (a block being
     * broken). Pressure tanks and fluid tanks never join each other.
     */
    private static Set<BlockPos> group(ServerLevel level, Block kind, BlockPos start, @Nullable BlockPos except) {
        Set<BlockPos> group = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        group.add(start);
        queue.add(start);
        while (!queue.isEmpty() && group.size() < SEARCH_LIMIT) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!next.equals(except) && !group.contains(next) && level.getBlockState(next).is(kind)) {
                    group.add(next);
                    queue.add(next);
                }
            }
        }
        return group;
    }

    /** Takes the gas out of every block in the group, pooled. */
    private static FluidStack collect(ServerLevel level, Set<BlockPos> group) {
        FluidStack gas = FluidStack.EMPTY;
        for (BlockPos pos : group) {
            if (level.getBlockEntity(pos) instanceof PressureTankBlockEntity tank) {
                gas = merge(gas, tank.takeContents());
            }
        }
        return gas;
    }

    /** Pools two amounts of gas. Two different gases do not mix: the larger one is kept. */
    private static FluidStack merge(FluidStack a, FluidStack b) {
        if (a.isEmpty()) {
            return b.copy();
        }
        if (b.isEmpty()) {
            return a;
        }
        if (FluidStack.isSameFluidSameComponents(a, b)) {
            return a.copyWithAmount(a.getAmount() + b.getAmount());
        }
        return a.getAmount() >= b.getAmount() ? a : b.copy();
    }

    private static void rebuild(ServerLevel level, Set<BlockPos> group, FluidStack gas) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : group) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        int height = maxY - minY + 1;
        boolean tower = maxX - minX == 1 && maxZ - minZ == 1 && height <= MAX_HEIGHT && group.size() == 4 * height;
        if (tower) {
            BlockPos controllerPos = new BlockPos(minX, minY, minZ);
            for (BlockPos pos : group) {
                PressureTankBlock.Corner corner = pos.getZ() == minZ
                        ? (pos.getX() == minX ? PressureTankBlock.Corner.NORTH_WEST : PressureTankBlock.Corner.NORTH_EAST)
                        : (pos.getX() == minX ? PressureTankBlock.Corner.SOUTH_WEST : PressureTankBlock.Corner.SOUTH_EAST);
                PressureTankBlock.Layer layer = height == 1 ? PressureTankBlock.Layer.SINGLE
                        : pos.getY() == minY ? PressureTankBlock.Layer.BOTTOM
                        : pos.getY() == maxY ? PressureTankBlock.Layer.TOP : PressureTankBlock.Layer.MIDDLE;
                setShape(level, pos, corner, layer);
                if (level.getBlockEntity(pos) instanceof PressureTankBlockEntity tank) {
                    if (pos.equals(controllerPos)) {
                        tank.becomeController(group.size(), height, true);
                        tank.give(gas);
                    } else {
                        tank.becomeMember(controllerPos);
                    }
                }
            }
        } else {
            int each = gas.isEmpty() ? 0 : gas.getAmount() / group.size();
            for (BlockPos pos : group) {
                setShape(level, pos, PressureTankBlock.Corner.NONE, PressureTankBlock.Layer.SINGLE);
                if (level.getBlockEntity(pos) instanceof PressureTankBlockEntity tank) {
                    tank.becomeController(1, 1, false);
                    tank.give(gas.isEmpty() ? FluidStack.EMPTY : gas.copyWithAmount(each));
                }
            }
        }
        for (BlockPos pos : group) {
            level.invalidateCapabilities(pos);
        }
    }

    private static void setShape(ServerLevel level, BlockPos pos, PressureTankBlock.Corner corner, PressureTankBlock.Layer layer) {
        BlockState state = level.getBlockState(pos);
        BlockState shaped = state.setValue(PressureTankBlock.CORNER, corner).setValue(PressureTankBlock.LAYER, layer);
        if (shaped != state) {
            level.setBlock(pos, shaped, Block.UPDATE_CLIENTS);
        }
    }
}
