package com.ryzer.ryzergen.machine.assembler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Where everything sits in an Assembler (design section 14), a 3 x 3 x 3 open robot cell. The design
 * is drawn facing north: cells 0 to 2 across (west to east), up and back (front to back), as in
 * art/tools/assembler_concept.py, whose constants the pixel positions here must match. The
 * controller sits front and centre on the bottom row and sets the way the cell faces; the arm part
 * sits over the middle of the bottom row; every other cell is frame.
 *
 * <p>Formed, each part keeps a collision shape that follows what is drawn there: the machine bay
 * below, the corner posts, the top rim, the back wall and the pattern rack. The rest of the cell is
 * open, so the arm and the bed show.
 */
public final class AssemblerLayout {
    public static final int SIZE = 3;
    public static final int CELLS = SIZE * SIZE * SIZE;
    public static final BlockPos CONTROLLER = new BlockPos(1, 0, 0);
    public static final BlockPos ARM = new BlockPos(1, 1, 1);

    // ---------------------------------------------------------------- the design, in pixels

    /** The fixture bed's top, and its fixtures' centres: x from the west, z from the front, row 0 of the grid at the back. */
    public static final float BED_TOP = 18;
    public static final float[] FIX_X = {19, 24, 29};
    public static final float[] FIX_Z = {17, 12, 7};
    /** The belts' middles across, their run (z, from the back of the hood over their front ends) and their top. */
    public static final float IN_BELT_X = 39;
    public static final float OUT_BELT_X = 9;
    public static final float BELT_FRONT = 10.5F;
    public static final float BELT_BACK = 39;
    public static final float BELT_TOP = 19.25F;
    /** The turret's axis, the shoulder's height, the upper arm, the forearm, and the grip below the wrist. */
    public static final float PIVOT_X = 24;
    public static final float PIVOT_Z = 31;
    public static final float TURRET_Y = 18.5F;
    public static final float SHOULDER_Y = 24;
    public static final float UPPER = 15;
    public static final float FORE = 18;
    public static final float GRIP = 12.5F;
    /** Where the arm picks from the in-feed belt and drops on the out-feed, level with its axis. */
    public static final float PICK_Z = PIVOT_Z;
    /** The pattern rack's face, and its cartridges' centres (x across, y up), row 0 at the top. */
    public static final float RACK_FACE = 43;
    public static final float[] CART_X = {14, 24, 34};
    public static final float[] CART_Y = {39, 31, 23};
    /** The stack light's lamps, bottom to top (red, amber, green): x and z span, and each lamp's foot. */
    public static final float[] STACK = {41.75F, 39.75F, 44.25F, 42.25F};
    public static final float[] STACK_Y = {34, 36.5F, 39};

    /**
     * The ports, centred on their block faces on the bottom row: as you face the front, energy and
     * items in on your left (the east end of a cell facing north), items out on your right.
     */
    public enum Port {
        ENERGY(new BlockPos(2, 0, 0), true),
        ITEMS_IN(new BlockPos(2, 0, 1), true),
        ITEMS_OUT(new BlockPos(0, 0, 1), false);

        private final BlockPos cell;
        private final boolean input;

        Port(BlockPos cell, boolean input) {
            this.cell = cell;
            this.input = input;
        }

        public BlockPos cell() {
            return cell;
        }

        /** The way this port looks out, for a cell facing {@code facing}. */
        public Direction face(Direction facing) {
            return input ? facing.getClockWise() : facing.getCounterClockWise();
        }
    }

    /** Each cell's collision for each facing (horizontal data value), by cell number. */
    private static final VoxelShape[][] SHAPES = new VoxelShape[CELLS + 1][4];

    static {
        for (int index = 1; index <= CELLS; index++) {
            VoxelShape north = northShape(cell(index));
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                SHAPES[index][facing.get2DDataValue()] = turn(north, facing);
            }
        }
    }

    private AssemblerLayout() {}

    /** What is solid in a cell, facing north: the bay below; above, the posts, the rim and the rack. */
    private static VoxelShape northShape(BlockPos cell) {
        if (cell.getY() == 0) {
            return Shapes.block();
        }
        VoxelShape shape = Shapes.empty();
        boolean west = cell.getX() == 0;
        boolean east = cell.getX() == SIZE - 1;
        boolean front = cell.getZ() == 0;
        boolean back = cell.getZ() == SIZE - 1;
        if ((west || east) && (front || back)) {
            double x = west ? 0 : 14;
            double z = front ? 0 : 14;
            shape = Shapes.or(shape, Block.box(x, 0, z, x + 2, 16, z + 2));
        }
        if (cell.getY() == SIZE - 1) {
            if (front) {
                shape = Shapes.or(shape, Block.box(0, 13, 0, 16, 15, 2));
            }
            if (back) {
                shape = Shapes.or(shape, Block.box(0, 13, 14, 16, 15, 16));
            }
            if (west) {
                shape = Shapes.or(shape, Block.box(0, 13, 0, 2, 15, 16));
            }
            if (east) {
                shape = Shapes.or(shape, Block.box(14, 13, 0, 16, 15, 16));
            }
        }
        if (back) {
            // The back wall between the back posts, z 45.5 to 47.5 in the design, up to the rim.
            double wx1 = Math.max(0, 2 - cell.getX() * 16);
            double wx2 = Math.min(16, 46 - cell.getX() * 16);
            double wallTop = Math.min(16, 45 - cell.getY() * 16);
            shape = Shapes.or(shape, Block.box(wx1, 0, 13.5, wx2, wallTop, 15.5));
            // The pattern rack, x 9 to 39 and up to 43 in the design, from its face at z 43 to 45.
            double x1 = Math.max(0, 9 - cell.getX() * 16);
            double x2 = Math.min(16, 39 - cell.getX() * 16);
            double top = Math.min(16, 43 - cell.getY() * 16);
            if (x2 > x1 && top > 0) {
                shape = Shapes.or(shape, Block.box(x1, 0, 11, x2, top, 13));
            }
        }
        return shape.optimize();
    }

    /** The part a cell needs. */
    public static AssemblerPart partAt(BlockPos cell) {
        if (cell.equals(CONTROLLER)) {
            return AssemblerPart.CONTROLLER;
        }
        return cell.equals(ARM) ? AssemblerPart.ARM : AssemblerPart.FRAME;
    }

    /** A cell's number in the block state, 1 to {@link #CELLS} (0 is an unformed part). */
    public static int index(BlockPos cell) {
        return 1 + cell.getX() + SIZE * (cell.getZ() + SIZE * cell.getY());
    }

    public static BlockPos cell(int index) {
        int i = index - 1;
        return new BlockPos(i % SIZE, i / (SIZE * SIZE), (i / SIZE) % SIZE);
    }

    /** What collides in a formed cell, turned to the cell's facing. */
    public static VoxelShape shape(int index, Direction facing) {
        return SHAPES[index][facing.get2DDataValue()];
    }

    /** The world position of a cell, for an Assembler whose controller is at {@code controller}. */
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

    /** A point of the design, in pixels, in the world, for a cell whose controller is at {@code controller}. */
    public static Vec3 designToWorld(BlockPos controller, Direction facing, double x, double y, double z) {
        double across = (x - CONTROLLER.getX() * 16) / 16 - 0.5;
        double deep = (z - CONTROLLER.getZ() * 16) / 16 - 0.5;
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();
        return new Vec3(controller.getX() + 0.5 + across * right.getStepX() + deep * back.getStepX(),
                controller.getY() + y / 16,
                controller.getZ() + 0.5 + across * right.getStepZ() + deep * back.getStepZ());
    }

    /** A shape drawn facing north, turned to {@code facing} about the block's middle, as {@link #toWorld} turns the cells. */
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
