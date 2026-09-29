package com.ryzer.ryzergen.machine.pool;

import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Where everything sits in a Spent Fuel Pool (design section 7). The design is drawn facing north:
 * cells 0 to 4 across (west to east), 0 to 2 up and 0 to 2 back (front to back), as in
 * art/tools/pool_concept.py. The controller sits front and centre on the middle row and sets the
 * way the pool faces; the crane block is top centre, in the middle of the basin; every other cell
 * is liner.
 *
 * <p>While it is built the controller sits a row lower, on the ground ({@link #GROUND}), so placing
 * it where the player stands builds the pool on the ground rather than half under it. When the last
 * part is in, it swaps with the liner above and the pool forms with its console on the middle row.
 * A pool broken apart and rebuilt keeps its controller on the middle row, with liner below: then it
 * builds from there ("raised").
 */
public final class PoolLayout {
    public static final int LONG = 5;
    public static final int HIGH = 3;
    public static final int DEEP = 3;
    public static final int CELLS = LONG * HIGH * DEEP;
    public static final BlockPos CONTROLLER = new BlockPos(2, 1, 0);
    /** Where the controller sits while the pool is built: below its place, on the ground. */
    public static final BlockPos GROUND = new BlockPos(2, 0, 0);
    public static final BlockPos CRANE = new BlockPos(2, 2, 1);

    /*
     * The rack and water in the design, in pixels (art/tools/pool_concept.py, where RACK_X, RACK_Z,
     * CELL and RACK_TOP must match these): the rack's first cell's corner, the cell size, how many
     * across, the rack's top, a rod's ends (it stands 8 pixels proud of the rack), and the water's
     * surface.
     */
    public static final int RACK_X = 10;
    public static final int RACK_Z = 7;
    public static final int CELL = 10;
    public static final int COLUMNS = 6;
    public static final int RACK_TOP = 14;
    public static final int ROD_BOTTOM = 6;
    public static final int ROD_TOP = RACK_TOP + 8;
    public static final int WATER_TOP = 38;

    /**
     * The ports, centred on their block faces in the middle row. As you face the front, inputs on
     * your left (hot fuel and water, on the east end of a pool facing north) and cooled fuel out on
     * your right (the west end): the front's clockwise and anticlockwise sides.
     */
    public enum Port {
        FUEL_IN(new BlockPos(LONG - 1, 1, 0), true),
        WATER_IN(new BlockPos(LONG - 1, 1, DEEP - 1), true),
        OUTPUT(new BlockPos(0, 1, 1), false);

        private final BlockPos cell;
        private final boolean input;

        Port(BlockPos cell, boolean input) {
            this.cell = cell;
            this.input = input;
        }

        public BlockPos cell() {
            return cell;
        }

        /** The way this port looks out, for a pool facing {@code facing}. */
        public Direction face(Direction facing) {
            return input ? facing.getClockWise() : facing.getCounterClockWise();
        }
    }

    private PoolLayout() {}

    /**
     * The part a cell needs while the pool is built: as formed if the controller is raised, else with
     * the controller on the ground and a liner in its place above.
     */
    public static PoolPart partAt(BlockPos cell, boolean raised) {
        if (raised) {
            return partAt(cell);
        }
        return cell.equals(GROUND) ? PoolPart.CONTROLLER : cell.equals(CONTROLLER) ? PoolPart.LINER : partAt(cell);
    }

    /** The cell the controller sits in: its place in the formed pool, or the ground while it is built. */
    public static BlockPos anchor(boolean raised) {
        return raised ? CONTROLLER : GROUND;
    }

    public static PoolPart partAt(BlockPos cell) {
        if (cell.equals(CONTROLLER)) {
            return PoolPart.CONTROLLER;
        }
        return cell.equals(CRANE) ? PoolPart.CRANE : PoolPart.LINER;
    }

    /** A cell's number in the block state, 1 to {@link #CELLS} (0 is an unformed part). */
    public static int index(BlockPos cell) {
        return 1 + cell.getX() + LONG * (cell.getZ() + DEEP * cell.getY());
    }

    public static BlockPos cell(int index) {
        int i = index - 1;
        return new BlockPos(i % LONG, i / (LONG * DEEP), (i / LONG) % DEEP);
    }

    /** The world position of a cell, for a formed pool whose controller is at {@code controller}. */
    public static BlockPos toWorld(BlockPos controller, Direction facing, BlockPos cell) {
        return toWorld(controller, CONTROLLER, facing, cell);
    }

    /** The world position of a cell, given where the controller is and which cell it sits in. */
    public static BlockPos toWorld(BlockPos controller, BlockPos anchor, Direction facing, BlockPos cell) {
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();
        int across = cell.getX() - anchor.getX();
        int up = cell.getY() - anchor.getY();
        int deep = cell.getZ() - anchor.getZ();
        return controller.relative(right, across).relative(back, deep).above(up);
    }

    /**
     * A point of the design, in pixels, in the world: for a formed pool whose controller is at
     * {@code controller} facing {@code facing}, turned as the blockstate turns the models.
     */
    public static Vec3 designToWorld(BlockPos controller, Direction facing, double x, double y, double z) {
        // From the controller block's centre, in blocks, facing north: +x east (right), +z south (back).
        double across = (x - CONTROLLER.getX() * 16) / 16 - 0.5;
        double deep = (z - CONTROLLER.getZ() * 16) / 16 - 0.5;
        double up = (y - CONTROLLER.getY() * 16) / 16;
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();
        return new Vec3(controller.getX() + 0.5 + across * right.getStepX() + deep * back.getStepX(),
                controller.getY() + up,
                controller.getZ() + 0.5 + across * right.getStepZ() + deep * back.getStepZ());
    }

    /** The controller's position, from any cell's position. */
    public static BlockPos controllerFrom(BlockPos pos, Direction facing, BlockPos cell) {
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();
        int across = cell.getX() - CONTROLLER.getX();
        int up = cell.getY() - CONTROLLER.getY();
        int deep = cell.getZ() - CONTROLLER.getZ();
        return pos.relative(right, -across).relative(back, -deep).below(up);
    }
}
