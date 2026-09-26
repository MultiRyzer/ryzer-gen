package com.ryzer.ryzergen.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * What pipes see on a machine's side: all its slots, but items only go into the slots given as
 * inputs and only come out of the ones given as outputs. So a pipe set to extract pulls results
 * and never ingredients, and a pipe pushing in never fills an output slot, whichever side it is on.
 */
public class MachineItemPort implements IItemHandler {
    private final IItemHandler items;
    private final boolean[] insert;
    private final boolean[] extract;

    /** {@code inserts} and {@code extracts} are [from, to) slot ranges, in pairs. */
    public MachineItemPort(IItemHandler items, int[] inserts, int[] extracts) {
        this.items = items;
        insert = mark(items.getSlots(), inserts);
        extract = mark(items.getSlots(), extracts);
    }

    private static boolean[] mark(int slots, int[] ranges) {
        boolean[] out = new boolean[slots];
        for (int i = 0; i + 1 < ranges.length; i += 2) {
            for (int slot = ranges[i]; slot < ranges[i + 1] && slot < slots; slot++) {
                out[slot] = true;
            }
        }
        return out;
    }

    /** One range each way: items into [from, to), results out of [from, to). */
    public static MachineItemPort of(IItemHandler items, int inFrom, int inTo, int outFrom, int outTo) {
        return new MachineItemPort(items, new int[] {inFrom, inTo}, new int[] {outFrom, outTo});
    }

    @Override
    public int getSlots() {
        return items.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return items.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return insert[slot] ? items.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extract[slot] ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return items.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return insert[slot] && items.isItemValid(slot, stack);
    }
}
