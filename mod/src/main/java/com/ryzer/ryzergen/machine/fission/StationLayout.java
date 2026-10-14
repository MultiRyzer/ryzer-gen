package com.ryzer.ryzergen.machine.fission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which part goes where in a fission station (design section 8). The design is drawn facing north
 * in a 12 x 12 footprint (cells 0 to 11 across, centred on the corner between 5 and 6), 11 high:
 *
 * <pre>
 *   0      12-wide ring of casing; its front four blocks are water in, the control core, the fuel
 *          port and energy out, and round the side, beside energy out, the output port
 *   1-4    12-wide ring of glass (the reactor chamber)
 *   5      12-wide ring of casing (the reactor head)
 *   6-7    12-wide ring of casing (the turbine band), the turbine rotor in the middle of 6
 *          (where the formed station draws the hub)
 *   8-10   11-wide ring of casing (the steam stack)
 * </pre>
 *
 * The control core builds the station itself from the parts fed into it, so the rotor can sit in
 * the middle, where it belongs. The inside stays open; the formed station draws its own floor, core,
 * rotor and stack. The core is placed first and sets the way the station faces (its front is the core's
 * face); the layout is turned to match.
 */
public final class StationLayout {
    public static final int SIZE = 12;
    public static final int HEIGHT = 11;
    /** Where the control core sits in the design (facing north): front and centre of the base. */
    public static final BlockPos CORE = new BlockPos(5, 0, 0);
    /** The turbine rotor: in the middle of the turbine band, under the drawn hub. */
    public static final BlockPos ROTOR = new BlockPos(6, 6, 6);

    /**
     * The ports on the base, on the two flat sides so the front stays clear for the core's screen.
     * As you face the front: inputs on your left (water and fuel, on the east flat of a station
     * facing north), outputs on your right (energy and spent rods, on the west flat). Two separate
     * sides also keep the pipe bringing fuel apart from the one taking spent rods away.
     */
    public enum Port {
        WATER, FUEL, ENERGY, OUTPUT;

        public boolean input() {
            return this == WATER || this == FUEL;
        }
    }

    public static final Map<BlockPos, Port> PORTS = Map.of(
            new BlockPos(11, 0, 5), Port.WATER,
            new BlockPos(11, 0, 6), Port.FUEL,
            new BlockPos(0, 0, 5), Port.ENERGY,
            new BlockPos(0, 0, 6), Port.OUTPUT);

    /** The way a port's face looks out, for a station facing {@code facing}. */
    public static Direction portFace(Direction facing, Port port) {
        // A player facing the front looks the opposite way to it, so their left is the front's clockwise turn.
        return port.input() ? facing.getClockWise() : facing.getCounterClockWise();
    }

    /** Where a port is in the world, for a station whose core is at {@code core}. */
    public static BlockPos portPos(BlockPos core, Direction facing, Port port) {
        for (Map.Entry<BlockPos, Port> entry : PORTS.entrySet()) {
            if (entry.getValue() == port) {
                return toWorld(core, facing, entry.getKey());
            }
        }
        return core;
    }

    /** Every space in the design, facing north, with the part it needs. */
    public static final Map<BlockPos, StationPart> PARTS;

    static {
        Map<BlockPos, StationPart> parts = new LinkedHashMap<>();
        for (int y = 0; y < HEIGHT; y++) {
            double radius = y >= 8 ? 5.5 : 6;
            StationPart wall = y >= 1 && y <= 4 ? StationPart.GLASS : StationPart.CASING;
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    if (onRing(x, z, radius)) {
                        parts.put(new BlockPos(x, y, z), wall);
                    }
                }
            }
        }
        parts.put(CORE, StationPart.CORE);
        parts.put(ROTOR, StationPart.ROTOR);
        PARTS = Collections.unmodifiableMap(parts);
    }

    /**
     * The coolant reserve's spaces on the station's back (design section 8), by ring of bays: 0 the
     * centre bay, 1 the pair beside it, 2 the outer pair. It grows a ring at a time, so a reserve of
     * 1, 3 or 5 bays fills rings 0, 0 to 1, or 0 to 2. A space belongs to the bay its centre lies in:
     * outside the station's ring, out to 7 blocks from the centre (the drawn face is at 6.75), in the
     * bottom two layers, and in the third only near the station, under the hood. Rings are 30 degrees
     * a bay, measured from the back's centre either way, so the two sides match.
     */
    public static final List<Set<BlockPos>> RESERVE;
    /** Bays in a reserve with this many complete rings. */
    private static final int[] RESERVE_BAYS = {0, 1, 3, 5};

    static {
        List<Set<BlockPos>> rings = List.of(new LinkedHashSet<>(), new LinkedHashSet<>(), new LinkedHashSet<>());
        for (int y = 0; y < 3; y++) {
            for (int x = -2; x < SIZE + 2; x++) {
                for (int z = SIZE / 2; z < SIZE + 4; z++) {
                    BlockPos cell = new BlockPos(x, y, z);
                    double dx = x + 0.5 - SIZE / 2.0;
                    double dz = z + 0.5 - SIZE / 2.0;
                    double r = Math.hypot(dx, dz);
                    if (PARTS.containsKey(cell) || r < 5.6 || r > (y < 2 ? 7.0 : 6.6)) {
                        continue;
                    }
                    double fromBack = Math.abs(Math.toDegrees(Math.atan2(dz, dx)) - 90);
                    int ring = fromBack <= 15 ? 0 : fromBack <= 45 ? 1 : fromBack <= 75 ? 2 : -1;
                    if (ring >= 0) {
                        rings.get(ring).add(cell);
                    }
                }
            }
        }
        RESERVE = rings.stream().map(Collections::unmodifiableSet).toList();
    }

    /** Bays in a reserve with {@code rings} complete rings (0 to 3): 0, 1, 3 or 5. */
    public static int reserveBays(int rings) {
        return RESERVE_BAYS[Math.max(0, Math.min(RESERVE.size(), rings))];
    }

    private StationLayout() {}

    private static boolean inside(int x, int z, double radius) {
        return Math.hypot(x + 0.5 - SIZE / 2.0, z + 0.5 - SIZE / 2.0) <= radius;
    }

    /** Inside the circle, with at least one side neighbour outside it. */
    private static boolean onRing(int x, int z, double radius) {
        return inside(x, z, radius) && (!inside(x + 1, z, radius) || !inside(x - 1, z, radius)
                || !inside(x, z + 1, radius) || !inside(x, z - 1, radius));
    }

    /** A design cell turned to face {@code facing}, about the centre of the footprint. */
    public static BlockPos turn(BlockPos cell, Direction facing) {
        int x = cell.getX();
        int z = cell.getZ();
        return switch (facing) {
            case EAST -> new BlockPos(SIZE - 1 - z, cell.getY(), x);
            case SOUTH -> new BlockPos(SIZE - 1 - x, cell.getY(), SIZE - 1 - z);
            case WEST -> new BlockPos(z, cell.getY(), SIZE - 1 - x);
            default -> cell;
        };
    }

    /** The world position of a design cell, for a station whose core is at {@code core}. */
    public static BlockPos toWorld(BlockPos core, Direction facing, BlockPos cell) {
        return core.offset(turn(cell, facing).subtract(turn(CORE, facing)));
    }

    /** Degrees to turn the design about the centre of its footprint to face {@code facing}. */
    public static float yRotation(Direction facing) {
        return switch (facing) {
            case EAST -> -90;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> 0;
        };
    }
}
