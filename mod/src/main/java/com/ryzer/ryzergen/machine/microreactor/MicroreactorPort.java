package com.ryzer.ryzergen.machine.microreactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The fixed connection points on an assembled microreactor. Pipes and cables only connect here,
 * on one face of one block, and only while the structure is formed. They are all on the back
 * block, the plant behind the jar: as you face the front, inputs on your left and outputs on your
 * right (the machine's own right and left), energy out on the back.
 */
public enum MicroreactorPort {
    /** Cable socket on the back of the lower back block. */
    ENERGY_OUT(MicroreactorSlot.LOWER_BACK, Side.BACK),
    /** Water in, on the lower back block's flank to your left: water only, and pipes cannot drain it. */
    COOLANT_IN(MicroreactorSlot.LOWER_BACK, Side.RIGHT),
    /** Steam out, on the lower back block's flank to your right: the boiled coolant leaves here as steam. */
    STEAM_OUT(MicroreactorSlot.LOWER_BACK, Side.LEFT),
    /** Fuel in, on the fuel bay's flank to your left (the upper back block): fresh cores only, nothing comes out. */
    FUEL_IN(MicroreactorSlot.UPPER_BACK, Side.RIGHT),
    /**
     * Spent cores out, on the fuel bay's flank to your right: the reactor pushes them out, and pipes
     * can pull them. Two ports, not one hatch, so the pipe bringing fresh cores and the pipe taking
     * spent ones away are separate lines, and fresh cores can never wander into the spent-core store.
     */
    FUEL_OUT(MicroreactorSlot.UPPER_BACK, Side.LEFT);

    /** A side of the machine, named as the machine faces: its right is your left as you face it. */
    private enum Side { BACK, RIGHT, LEFT }

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
            case RIGHT -> facing.getClockWise();
            case LEFT -> facing.getCounterClockWise();
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
