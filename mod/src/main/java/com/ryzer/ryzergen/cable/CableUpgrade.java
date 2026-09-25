package com.ryzer.ryzergen.cable;

import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.world.item.ItemStack;

/**
 * Fittings: the upgrade a cable or pipe takes on one side, raising the limit for what comes in
 * there (pulled by an extract side, or pushed in by a generator). One slot per side, one fitting in
 * it; each tier is crafted from the one before, so a better fitting replaces a worse one. The same
 * three fittings serve every kind of cable and pipe.
 *
 * <p>Real basis: better conductors and bigger bores. Silver is the best common conductor, heavy
 * busbars carry a power station's current, and superconductors carry it with no loss at all.
 */
public enum CableUpgrade {
    NONE("none", 1),
    SILVER("silver", 8),
    BUSBAR("busbar", 32),
    CRYOGENIC("cryogenic", 1024);

    private final String id;
    private final int multiplier;

    CableUpgrade(String id, int multiplier) {
        this.id = id;
        this.multiplier = multiplier;
    }

    public String id() {
        return id;
    }

    /** How many times the base rate a side with this fitting can move. */
    public int multiplier() {
        return multiplier;
    }

    public static CableUpgrade of(ItemStack stack) {
        if (stack.is(ModItems.SILVER_FITTINGS.get())) {
            return SILVER;
        }
        if (stack.is(ModItems.BUSBAR_FITTINGS.get())) {
            return BUSBAR;
        }
        if (stack.is(ModItems.CRYOGENIC_FITTINGS.get())) {
            return CRYOGENIC;
        }
        return NONE;
    }

    public static CableUpgrade byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : NONE;
    }

    /** A rate with this fitting, kept inside an int. */
    public int scale(int base) {
        return (int) Math.min(Integer.MAX_VALUE, (long) base * multiplier);
    }

    /**
     * Item pipes move in batches every so often. A fitting multiplies their throughput: the batch
     * grows up to a stack, then the wait shortens, down to every tick. Returns {batch, interval}.
     */
    public int[] itemSchedule(int batch, int interval) {
        long target = (long) batch * multiplier;
        int divide = (int) Math.max(1, Math.min(interval, target / 64));
        return new int[] {(int) Math.min(Integer.MAX_VALUE, target / divide), Math.max(1, interval / divide)};
    }
}
