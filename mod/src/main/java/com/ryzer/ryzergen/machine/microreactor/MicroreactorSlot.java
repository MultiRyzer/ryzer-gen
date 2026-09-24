package com.ryzer.ryzergen.machine.microreactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/**
 * Where a part sits in an assembled microreactor, or NONE when it stands alone.
 * The structure is 1 wide, 2 deep and 2 high; the front faces the heart's facing.
 */
public enum MicroreactorSlot implements StringRepresentable {
    NONE("none", 0, 0),
    LOWER_FRONT("lower_front", 0, 0),
    UPPER_FRONT("upper_front", 0, 1),
    LOWER_BACK("lower_back", 1, 0),
    UPPER_BACK("upper_back", 1, 1);

    public static final MicroreactorSlot[] FORMED = {LOWER_FRONT, UPPER_FRONT, LOWER_BACK, UPPER_BACK};

    private final String name;
    private final int back;
    private final int up;

    MicroreactorSlot(String name, int back, int up) {
        this.name = name;
        this.back = back;
        this.up = up;
    }

    public boolean isFormed() {
        return this != NONE;
    }

    /** Position of this slot, given the lower front block of a machine facing {@code facing}. */
    public BlockPos fromOrigin(BlockPos origin, Direction facing) {
        return origin.relative(facing.getOpposite(), back).above(up);
    }

    /** The lower front block, given this slot's position. */
    public BlockPos toOrigin(BlockPos pos, Direction facing) {
        return pos.relative(facing, back).below(up);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
