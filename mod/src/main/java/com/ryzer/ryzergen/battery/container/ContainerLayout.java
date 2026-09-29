package com.ryzer.ryzergen.battery.container;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where everything sits in a Container Battery (design section 12). The design is drawn facing
 * north: cells 0 to 7 across (west to east; 0 to 6 the container, 7 the fan unit), 0 to 2 up and 0
 * to 2 back, as in art/tools/container_concept.py. The controller is the console, front and centre
 * on the middle row; the thermal unit is the fan's hub, in the middle of the east end; every other
 * cell is frame. Every front cell of the container but the console's is a module slot.
 *
 * <p>As with the pool, the controller is built on the ground below its place and swaps up with the
 * frame above it when the container forms; a container broken apart and rebuilt keeps it raised.
 */
public final class ContainerLayout {
    public static final int LONG = 8;
    public static final int HIGH = 3;
    public static final int DEEP = 3;
    public static final int CELLS = LONG * HIGH * DEEP;
    public static final BlockPos CONTROLLER = new BlockPos(3, 1, 0);
    public static final BlockPos GROUND = new BlockPos(3, 0, 0);
    public static final BlockPos THERMAL = new BlockPos(7, 1, 1);
    /** The module slots, bottom row first, each row west to east. */
    public static final List<BlockPos> SLOTS;

    static {
        List<BlockPos> slots = new ArrayList<>();
        for (int y = 0; y < HIGH; y++) {
            for (int x = 0; x < LONG - 1; x++) {
                BlockPos cell = new BlockPos(x, y, 0);
                if (!cell.equals(CONTROLLER)) {
                    slots.add(cell);
                }
            }
        }
        SLOTS = Collections.unmodifiableList(slots);
    }

    /**
     * The ports, centred on their block faces in the middle row. Two energy ports on the back, one
     * at each end; each takes energy in and gives it out, as a home battery does, so a cable's own
     * setting decides which way it flows. Coolant comes in at the fan's hub on the east end.
     */
    public enum Port {
        ENERGY_EAST(new BlockPos(6, 1, DEEP - 1)),
        ENERGY_WEST(new BlockPos(0, 1, DEEP - 1)),
        COOLANT_IN(THERMAL);

        private final BlockPos cell;

        Port(BlockPos cell) {
            this.cell = cell;
        }

        public BlockPos cell() {
            return cell;
        }

        /** The way this port looks out, for a container facing {@code facing}. */
        public Direction face(Direction facing) {
            return this == COOLANT_IN ? facing.getClockWise() : facing.getOpposite();
        }
    }

    private ContainerLayout() {}

    /** The part a formed container has in a cell. */
    public static ContainerPart partAt(BlockPos cell) {
        if (cell.equals(CONTROLLER)) {
            return ContainerPart.CONTROLLER;
        }
        return cell.equals(THERMAL) ? ContainerPart.THERMAL : ContainerPart.FRAME;
    }

    /** The part a cell needs while it is built: with the controller on the ground, unless raised. */
    public static ContainerPart partAt(BlockPos cell, boolean raised) {
        if (raised) {
            return partAt(cell);
        }
        return cell.equals(GROUND) ? ContainerPart.CONTROLLER : cell.equals(CONTROLLER) ? ContainerPart.FRAME : partAt(cell);
    }

    public static BlockPos anchor(boolean raised) {
        return raised ? CONTROLLER : GROUND;
    }

    /** A cell's number in the block state, 1 to {@link #CELLS} (0 is an unformed part). */
    public static int index(BlockPos cell) {
        return 1 + cell.getX() + LONG * (cell.getZ() + DEEP * cell.getY());
    }

    public static BlockPos cell(int index) {
        int i = index - 1;
        return new BlockPos(i % LONG, i / (LONG * DEEP), (i / LONG) % DEEP);
    }

    /** The slot a cell holds, or -1 if it is not a module slot. */
    public static int slotAt(BlockPos cell) {
        return SLOTS.indexOf(cell);
    }

    public static BlockPos toWorld(BlockPos controller, Direction facing, BlockPos cell) {
        return toWorld(controller, CONTROLLER, facing, cell);
    }

    public static BlockPos toWorld(BlockPos controller, BlockPos anchor, Direction facing, BlockPos cell) {
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();
        return controller.relative(right, cell.getX() - anchor.getX()).relative(back, cell.getZ() - anchor.getZ())
                .above(cell.getY() - anchor.getY());
    }

    /** The controller of a formed container, from any of its blocks. */
    public static BlockPos controllerFrom(BlockPos pos, Direction facing, BlockPos cell) {
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();
        return pos.relative(right, CONTROLLER.getX() - cell.getX()).relative(back, CONTROLLER.getZ() - cell.getZ())
                .above(CONTROLLER.getY() - cell.getY());
    }
}
