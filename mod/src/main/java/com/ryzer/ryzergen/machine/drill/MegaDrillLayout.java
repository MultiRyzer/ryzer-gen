package com.ryzer.ryzergen.machine.drill;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where every block of a mega drill goes (design section 11c), following its concept design
 * (art/tools/mega_drill_concept.py). Drawn facing north in a 9 x 9 footprint, cells 0 to 8 across
 * (west to east) and 0 to 8 back (front to back), the pit's middle in cell (4, 4):
 *
 * <pre>
 *   0      the base ring round the pit, a foot pad in each corner, the controller front and centre
 *          (the ladder's foot), and the port consoles in the middle of each side
 *   1-3    the four buttress legs, and the ladder up the front
 *   4      the deck (7 x 7), with its handrail round the edge and a gap at the ladder
 *   5-10   the console facing the ladder, and the column (3 x 3)
 *   11-12  the collar and the drive housing (5 x 5)
 * </pre>
 *
 * Once formed the parts stop drawing themselves and the controller draws the whole drill from the
 * concept. Each part keeps a collision shape that follows what is drawn there (the base's low ring,
 * the deck's plate, the rail), so the drill can be walked on and climbed; the roof is drawn only.
 * The controller is placed first and sets the way the drill faces; the layout is turned to match.
 */
public final class MegaDrillLayout {
    public static final int SIZE = 9;
    public static final int HEIGHT = 13;
    /** The controller: front and centre of the base ring, at the foot of the ladder. */
    public static final BlockPos CONTROLLER = new BlockPos(4, 0, 0);
    /** The middle of the pit, on the base's level. */
    public static final BlockPos CENTRE = new BlockPos(4, 0, 4);
    /**
     * The area mined, from the pit's middle: the 7 x 7 under the opening in the base (the opening is
     * 6 across, half a block off the grid; the extra half block each side is hidden under the grating).
     * The legs' footings stand in its four corners.
     */
    public static final int LOW = -3;
    public static final int HIGH = 3;

    /** What collides in a cell, in the design's own turn (facing north). */
    public enum Shape {
        /** The base ring: a steel plate 6 pixels high. */
        PLATE(Block.box(0, 0, 0, 16, 6, 16)),
        /** The deck: its plate is the lower half of the block. */
        DECK(Block.box(0, 0, 0, 16, 8, 16)),
        /** A leg, a console, the column or the housing. */
        FULL(Shapes.block()),
        /** The ladder's rungs, near the back of its cell, so a player walking into it climbs. */
        LADDER(Block.box(3, 0, 9.5, 13, 16, 11.5)),
        /** The controller at the ladder's foot: the base ring and the ladder's first rungs. */
        FOOT(Shapes.or(Block.box(0, 0, 0, 16, 6, 16), Block.box(3, 0, 9.5, 13, 16, 11.5))),
        /** The handrail along the deck's west, east, north and south edges, over the deck's top. */
        RAIL_WEST(Block.box(14, 8, 0, 16, 24, 16)),
        RAIL_EAST(Block.box(0, 8, 0, 2, 24, 16)),
        RAIL_NORTH(Block.box(0, 8, 14, 16, 24, 16)),
        RAIL_SOUTH(Block.box(0, 8, 0, 16, 24, 2));

        private final VoxelShape north;

        Shape(VoxelShape north) {
            this.north = north;
        }

        /** Climbable: the ladder, from its foot. */
        public boolean ladder() {
            return this == LADDER || this == FOOT;
        }
    }

    /** A cell of the design and what collides in it. */
    public record Cell(BlockPos pos, Shape shape) {
        /** The base's level, lit (it lights the ground round it) while the drill runs. */
        public boolean lit() {
            return pos.getY() == 0;
        }
    }

    /**
     * The ports, on the consoles in the middle of the base's sides. As you face the front, inputs on
     * your left (energy, tungsten tips and water, on the east side of a drill facing north) and what
     * it mines out on your right (the west side), each in its own cell.
     */
    public enum Port {
        ENERGY(new BlockPos(SIZE - 1, 0, 3), true),
        TIPS(new BlockPos(SIZE - 1, 0, 4), true),
        WATER(new BlockPos(SIZE - 1, 0, 5), true),
        OUTPUT(new BlockPos(0, 0, 4), false);

        private final BlockPos cell;
        private final boolean input;

        Port(BlockPos cell, boolean input) {
            this.cell = cell;
            this.input = input;
        }

        public BlockPos cell() {
            return cell;
        }

        /** The way this port looks out, for a drill facing {@code facing}. */
        public Direction face(Direction facing) {
            // A player facing the front looks the opposite way to it, so their left is the front's clockwise turn.
            return input ? facing.getClockWise() : facing.getCounterClockWise();
        }
    }

    /** Every cell, in the order their numbers run (1 to {@link #CELLS}), from the base up. */
    public static final List<Cell> CELL_LIST;
    public static final int CELLS;
    /** Each cell's collision for each facing (horizontal data value), by cell number. */
    private static final VoxelShape[][] SHAPES;

    static {
        List<Cell> cells = new ArrayList<>();
        // The base ring round the pit; the controller and the consoles sit in it.
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                if (x != 0 && x != SIZE - 1 && z != 0 && z != SIZE - 1) {
                    continue;
                }
                BlockPos pos = new BlockPos(x, 0, z);
                boolean console = (x == 0 || x == SIZE - 1) && z >= 3 && z <= 5;
                cells.add(new Cell(pos, pos.equals(CONTROLLER) ? Shape.FOOT : console ? Shape.FULL : Shape.PLATE));
            }
        }
        // The foot pads' inner corners, over the footings.
        for (int x : new int[] {1, SIZE - 2}) {
            for (int z : new int[] {1, SIZE - 2}) {
                cells.add(new Cell(new BlockPos(x, 0, z), Shape.PLATE));
            }
        }
        // The legs, sloping in from the corners to the deck's underside.
        for (int sx : new int[] {-1, 1}) {
            for (int sz : new int[] {-1, 1}) {
                for (int[] step : new int[][] {{1, 1}, {1, 2}, {2, 3}}) {
                    int x = sx < 0 ? step[0] : SIZE - 1 - step[0];
                    int z = sz < 0 ? step[0] : SIZE - 1 - step[0];
                    cells.add(new Cell(new BlockPos(x, step[1], z), Shape.FULL));
                }
            }
        }
        // The ladder up the front.
        for (int y = 1; y <= 4; y++) {
            cells.add(new Cell(new BlockPos(CONTROLLER.getX(), y, 0), Shape.LADDER));
        }
        // The deck, and its handrail round the edge, with a gap where the ladder comes up.
        for (int x = 1; x < SIZE - 1; x++) {
            for (int z = 1; z < SIZE - 1; z++) {
                cells.add(new Cell(new BlockPos(x, 4, z), Shape.DECK));
            }
        }
        for (int i = 1; i < SIZE - 1; i++) {
            cells.add(new Cell(new BlockPos(0, 4, i), Shape.RAIL_WEST));
            cells.add(new Cell(new BlockPos(SIZE - 1, 4, i), Shape.RAIL_EAST));
            if (i != CONTROLLER.getX()) {
                cells.add(new Cell(new BlockPos(i, 4, 0), Shape.RAIL_NORTH));
            }
            cells.add(new Cell(new BlockPos(i, 4, SIZE - 1), Shape.RAIL_SOUTH));
        }
        // The console, its back to the column, facing the top of the ladder.
        for (int x = 3; x <= 5; x++) {
            cells.add(new Cell(new BlockPos(x, 5, 2), Shape.FULL));
        }
        // The column, then the collar and the drive housing.
        for (int y = 5; y <= 12; y++) {
            int r = y <= 10 ? 1 : 2;
            for (int x = 4 - r; x <= 4 + r; x++) {
                for (int z = 4 - r; z <= 4 + r; z++) {
                    cells.add(new Cell(new BlockPos(x, y, z), Shape.FULL));
                }
            }
        }
        CELL_LIST = Collections.unmodifiableList(cells);
        CELLS = cells.size();
        SHAPES = new VoxelShape[CELLS + 1][4];
        for (int index = 1; index <= CELLS; index++) {
            VoxelShape north = cell(index).shape().north;
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                SHAPES[index][facing.get2DDataValue()] = turn(north, facing);
            }
        }
    }

    private MegaDrillLayout() {}

    /** A cell by its number, 1 to {@link #CELLS}. */
    public static Cell cell(int index) {
        return CELL_LIST.get(index - 1);
    }

    /** A cell's number, or 0 if the design has nothing there. */
    public static int index(BlockPos pos) {
        for (int i = 0; i < CELLS; i++) {
            if (CELL_LIST.get(i).pos().equals(pos)) {
                return i + 1;
            }
        }
        return 0;
    }

    public static int index(Port port) {
        return index(port.cell());
    }

    /** What collides in a formed cell, turned to the drill's facing. */
    public static VoxelShape shape(int index, Direction facing) {
        return SHAPES[index][facing.get2DDataValue()];
    }

    /** The legs' footings, under the foot pads at the base's corners: never mined, so they stand as pillars. */
    public static boolean footing(int dx, int dz) {
        return Math.abs(dx) >= 3 && Math.abs(dx) <= 4 && Math.abs(dz) >= 3 && Math.abs(dz) <= 4;
    }

    /** The world position of a cell, for a drill whose controller is at {@code controller}. */
    public static BlockPos toWorld(BlockPos controller, Direction facing, BlockPos cell) {
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();
        return controller.relative(right, cell.getX() - CONTROLLER.getX())
                .relative(back, cell.getZ() - CONTROLLER.getZ())
                .above(cell.getY() - CONTROLLER.getY());
    }

    /** The controller's position, from any cell's position. */
    public static BlockPos controllerFrom(BlockPos pos, Direction facing, BlockPos cell) {
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();
        return pos.relative(right, CONTROLLER.getX() - cell.getX())
                .relative(back, CONTROLLER.getZ() - cell.getZ())
                .below(cell.getY() - CONTROLLER.getY());
    }

    /**
     * A shape drawn in the design's turn (facing north), turned to {@code facing} about the block's
     * middle, as {@link #toWorld} turns the cells: the design's x runs to the front's right, its z
     * to the back.
     */
    private static VoxelShape turn(VoxelShape shape, Direction facing) {
        VoxelShape turned = Shapes.empty();
        for (AABB box : shape.toAabbs()) {
            double[] a = turnPoint(box.minX, box.minZ, facing);
            double[] b = turnPoint(box.maxX, box.maxZ, facing);
            turned = Shapes.or(turned, Shapes.box(Math.min(a[0], b[0]), box.minY, Math.min(a[1], b[1]),
                    Math.max(a[0], b[0]), box.maxY, Math.max(a[1], b[1])));
        }
        return turned.optimize();
    }

    private static double[] turnPoint(double x, double z, Direction facing) {
        return switch (facing) {
            case EAST -> new double[] {1 - z, x};
            case SOUTH -> new double[] {1 - x, 1 - z};
            case WEST -> new double[] {z, 1 - x};
            default -> new double[] {x, z};
        };
    }
}
