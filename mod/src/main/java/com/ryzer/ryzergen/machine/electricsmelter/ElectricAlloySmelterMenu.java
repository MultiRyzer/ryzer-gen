package com.ryzer.ryzergen.machine.electricsmelter;

import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModMenus;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import static com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterBlockEntity.*;

public class ElectricAlloySmelterMenu extends AbstractContainerMenu {
    private static final int PLAYER_START = SLOTS;
    private static final int HOTBAR_START = PLAYER_START + 27;
    private static final int PLAYER_END = HOTBAR_START + 9;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client side: contents and progress arrive from the server. */
    public ElectricAlloySmelterMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, createItems(() -> {}), new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public ElectricAlloySmelterMenu(int containerId, Inventory inventory, IItemHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.ELECTRIC_ALLOY_SMELTER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.access = access;

        addSlot(new SlotItemHandler(items, INPUT_A, 38, 35));
        addSlot(new SlotItemHandler(items, INPUT_B, 56, 35));
        addSlot(new SlotItemHandler(items, OUTPUT, 116, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public float progress() {
        int total = data.get(DATA_TOTAL);
        return total == 0 ? 0 : Mth.clamp(data.get(DATA_PROGRESS) / (float) total, 0, 1);
    }

    public int energy() {
        return (data.get(DATA_ENERGY_HIGH) & 0xFFFF) << 16 | (data.get(DATA_ENERGY_LOW) & 0xFFFF);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (moveItemStackTo(stack, INPUT_A, INPUT_B + 1, false)) {
            // Moved into the inputs.
        } else if (index < HOTBAR_START) {
            if (!moveItemStackTo(stack, HOTBAR_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ELECTRIC_ALLOY_SMELTER.get());
    }
}
