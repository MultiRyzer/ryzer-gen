package com.ryzer.ryzergen.machine.breeder;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Which part goes where in a breeder reactor (design section 9), following its concept design
 * (art/tools/breeder_concept.py). Drawn facing north in a 9 x 9 footprint (cells 0 to 8 across,
 * centred on the middle of cell 4), 8 high:
 *
 * <pre>
 *   0      no plinth: the three consoles (the control core front and centre, the inputs' console on
 *          the east edge and the outputs' on the west) and the feet of the legs
 *   1-3    a ring of frame round the edge, where the legs stand
 *   4      a ring of frame, the girder belt; its top is the walkway
 *   1-7    the sphere's shell, inside the rings, open within
 * </pre>
 *
 * The control core builds the reactor itself from the parts fed into it. Once formed the parts stop
 * drawing themselves and the core draws the whole reactor from the concept design. The core is
 * placed first and sets the way the reactor faces; the layout is turned to match.
 */
public final class BreederLayout {
    public static final int SIZE = 9;
    public static final int HEIGHT = 8;
    /** Where the control core sits in the design (facing north): front and centre of the plinth. */
    public static final BlockPos CORE = new BlockPos(4, 0, 0);
    /** The sphere's middle and radius, in blocks, as the concept draws it. */
    private static final double SPHERE_Y = 4.5;
    private static final double SPHERE_R = 10 * 16 / Math.PI / 16;
    /** The legs' and the belt's ring, in blocks, and the legs themselves (12, at 15 degrees and every 30). */
    private static final double RING_R = 3.9;
    private static final int LEGS = 12;
    private static final double LEG_R = 55.76 / 16;

    /** Every space in the design, facing north, with the part it needs. */
    public static final Map<BlockPos, BreederPart> PARTS;

    static {
        Map<BlockPos, BreederPart> parts = new LinkedHashMap<>();
        double c = SIZE / 2.0;
        // No plinth: on the ground stand the three consoles (the front one round the core, the
        // inputs' on the east edge, the outputs' on the west) and the feet of the twelve legs.
        for (int i = 3; i <= 5; i++) {
            parts.put(new BlockPos(i, 0, 0), BreederPart.FRAME);
            parts.put(new BlockPos(0, 0, i), BreederPart.FRAME);
            parts.put(new BlockPos(SIZE - 1, 0, i), BreederPart.FRAME);
        }
        for (int leg = 0; leg < LEGS; leg++) {
            double angle = Math.toRadians(15 + 30 * leg);
            BlockPos cell = new BlockPos((int) Math.floor(c + LEG_R * Math.cos(angle)), 0, (int) Math.floor(c + LEG_R * Math.sin(angle)));
            parts.putIfAbsent(cell, BreederPart.FRAME);
        }
        // The legs' ring and the belt's.
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    if (onRing(x, z, RING_R)) {
                        parts.put(new BlockPos(x, y, z), BreederPart.FRAME);
                    }
                }
            }
        }
        // The sphere's shell: spaces inside it with a side neighbour outside.
        for (int y = 1; y < HEIGHT; y++) {
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    BlockPos cell = new BlockPos(x, y, z);
                    if (!parts.containsKey(cell) && inSphere(x, y, z)
                            && (!inSphere(x + 1, y, z) || !inSphere(x - 1, y, z) || !inSphere(x, y + 1, z)
                            || !inSphere(x, y - 1, z) || !inSphere(x, y, z + 1) || !inSphere(x, y, z - 1))) {
                        parts.put(cell, BreederPart.SHELL);
                    }
                }
            }
        }
        parts.put(CORE, BreederPart.CORE);
        PARTS = Collections.unmodifiableMap(parts);
    }

    private BreederLayout() {}

    private static boolean inSphere(int x, int y, int z) {
        double c = SIZE / 2.0;
        double dx = x + 0.5 - c, dy = y + 0.5 - SPHERE_Y, dz = z + 0.5 - c;
        return Math.sqrt(dx * dx + dy * dy + dz * dz) <= SPHERE_R;
    }

    private static boolean inside(int x, int z, double radius) {
        return Math.hypot(x + 0.5 - SIZE / 2.0, z + 0.5 - SIZE / 2.0) <= radius;
    }

    /** Inside the circle, with at least one side neighbour outside it. */
    private static boolean onRing(int x, int z, double radius) {
        return inside(x, z, radius) && (!inside(x + 1, z, radius) || !inside(x - 1, z, radius)
                || !inside(x, z + 1, radius) || !inside(x, z - 1, radius));
    }

    /** A design cell turned to face {@code facing}, about the centre of the footprint. */
    public static BlockPos turn(BlockPos cell, net.minecraft.core.Direction facing) {
        int x = cell.getX();
        int z = cell.getZ();
        return switch (facing) {
            case EAST -> new BlockPos(SIZE - 1 - z, cell.getY(), x);
            case SOUTH -> new BlockPos(SIZE - 1 - x, cell.getY(), SIZE - 1 - z);
            case WEST -> new BlockPos(z, cell.getY(), SIZE - 1 - x);
            default -> cell;
        };
    }

    /** The world position of a design cell, for a reactor whose core is at {@code core}. */
    public static BlockPos toWorld(BlockPos core, net.minecraft.core.Direction facing, BlockPos cell) {
        return core.offset(turn(cell, facing).subtract(turn(CORE, facing)));
    }

    /** Degrees to turn the design about the centre of its footprint to face {@code facing}. */
    public static float yRotation(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case EAST -> -90;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> 0;
        };
    }
}
