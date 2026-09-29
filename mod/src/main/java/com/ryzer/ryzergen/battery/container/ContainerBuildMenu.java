package com.ryzer.ryzergen.battery.container;

import com.ryzer.ryzergen.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import com.ryzer.ryzergen.machine.pool.PoolMenu;

import static com.ryzer.ryzergen.battery.container.BatteryControllerBlockEntity.*;

/** The controller's screen before the container forms: the parts store (3 x 3) and the player's inventory. */
public class ContainerBuildMenu extends AbstractContainerMenu {
    private final IItemHandler parts;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public ContainerBuildMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStackHandler(PART_SLOTS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return BatteryControllerBlockEntity.partFor(stack) != null;
            }
        }, new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public ContainerBuildMenu(int containerId, Inventory inventory, IItemHandler parts, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.CONTAINER_BUILD.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.parts = parts;
        this.data = data;
        this.access = access;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(parts, col + row * 3, 8 + col * 18, 18 + row * 18));
            }
        }
        PoolMenu.addInventory(this::addSlot, inventory);
        addDataSlots(data);
    }

    /** Cells still to fill for a part. None once the container has formed. */
    public int missing(ContainerPart part) {
        return formed() ? 0 : data.get(part == ContainerPart.FRAME ? DATA_MISSING_FRAME : DATA_MISSING_THERMAL);
    }

    public boolean formed() {
        return data.get(DATA_FORMED) != 0;
    }

    public int blocked() {
        return data.get(DATA_BLOCKED);
    }

    /** How many of a part are in the store. */
    public int stock(ContainerPart part) {
        int count = 0;
        for (int slot = 0; slot < PART_SLOTS; slot++) {
            ItemStack stack = parts.getStackInSlot(slot);
            if (BatteryControllerBlockEntity.partFor(stack) == part) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PART_SLOTS) {
            if (!moveItemStackTo(stack, PART_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (BatteryControllerBlockEntity.partFor(stack) == null || !moveItemStackTo(stack, 0, PART_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof BatteryControllerBlock
                && player.canInteractWithBlock(pos, 8.0), true);
    }
}
