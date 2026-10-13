package com.ryzer.ryzergen.machine;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Energy buffer for powered machines. Accepts energy from outside, never gives it back, and lets the
 * machine spend it. Tells the block entity to save whenever the stored amount changes.
 */
public class MachineEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;
    /** What arrived since the machine last took its intake, and in the interval before that. */
    private int receivedNow;
    private int receivedBefore;

    public MachineEnergyStorage(int capacity, int maxReceive, Runnable onChanged) {
        super(capacity, maxReceive, 0);
        this.onChanged = onChanged;
    }

    /** Spends energy if there is enough for all of it. */
    public boolean consume(int amount) {
        if (energy < amount) {
            return false;
        }
        energy -= amount;
        onChanged.run();
        return true;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int received = super.receiveEnergy(toReceive, simulate);
        if (received > 0 && !simulate) {
            receivedNow += received;
            onChanged.run();
        }
        return received;
    }

    /** Changes the buffer's size and intake (a gated machine's follow its draw); any excess is lost. */
    public void setLimits(int capacity, int maxReceive) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        if (energy > capacity) {
            energy = capacity;
            onChanged.run();
        }
    }

    /**
     * What arrived over the last two ticks, for a gated machine, whose gate measures the supply it
     * gets rather than what it has stored (so a trickle cannot bank up while it waits). Taken once a
     * tick, before it spends: the two intervals between its ticks, whichever side of its tick the
     * cables push on.
     */
    public int takeIntake() {
        int total = receivedBefore + receivedNow;
        receivedBefore = receivedNow;
        receivedNow = 0;
        return total;
    }

    /** Spends everything held (a gated machine short of its draw). */
    public void drain() {
        if (energy > 0) {
            energy = 0;
            onChanged.run();
        }
    }

    public void setStored(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }
}
