package com.ryzer.ryzergen.machine.microreactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The fixed connection points on an assembled microreactor. Pipes and cables only connect here,
 * on one face of one block, and only while the structure is formed.
 */
public enum MicroreactorPort {
    /** Cable socket low on the back. */
    ENERGY_OUT(MicroreactorSlot.LOWER_BACK, Side.BACK),
    /** Intake chute on top. */
    COOLANT_IN(MicroreactorSlot.UPPER_BACK, Side.UP),
    /** Steam outlet on the right-hand side: the boiled coolant leaves here as steam. */
    STEAM_OUT(MicroreactorSlot.LOWER_BACK, Side.RIGHT),
    /** Fuel hatch on the lid: fresh cores go in, and the reactor pushes spent ones back out. */
    FUEL(MicroreactorSlot.UPPER_FRONT, Side.UP);

    private enum Side { BACK, UP, RIGHT }

    private final MicroreactorSlot slot;
    private final Side side;

    MicroreactorPort(MicroreactorSlot slot, Side side) {
        this.slot = slot;
        this.side = side;
    }

    public MicroreactorSlot slot() {
        return slot;
    }

    /** The world face this port is on, for a machine facing {@code facing}. */
    public Direction face(Direction facing) {
        return switch (side) {
            case BACK -> facing.getOpposite();
            case UP -> Direction.UP;
            case RIGHT -> facing.getClockWise();
        };
    }

    /** The block this port is on, given the lower front block. */
    public BlockPos blockPos(BlockPos origin, Direction facing) {
        return slot.fromOrigin(origin, facing);
    }

    /** Whether a query on {@code side} of this block reaches this port. */
    public boolean isAt(BlockState state, @Nullable Direction side) {
        return side != null
                && state.getBlock() instanceof MicroreactorPartBlock
                && state.getValue(MicroreactorPartBlock.SLOT) == slot
                && side == face(state.getValue(MicroreactorPartBlock.FACING));
    }
}
