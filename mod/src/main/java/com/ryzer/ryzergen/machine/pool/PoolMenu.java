package com.ryzer.ryzergen.machine.pool;

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

import java.util.function.Consumer;

import static com.ryzer.ryzergen.machine.pool.PoolControllerBlockEntity.*;

/**
 * The formed pool's screen: the 18 rack slots (6 x 3, one item each), the 6 output slots (2 x 3)
 * and the player's inventory. Positions match art/tools/gui_textures.py (spent_fuel_pool).
 */
public class PoolMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public PoolMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStackHandler(RACKS) {
            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return HotFuel.isSpentFuel(stack);
            }
        }, new ItemStackHandler(OUTPUTS), new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public PoolMenu(int containerId, Inventory inventory, IItemHandler racks, IItemHandler output, ContainerData data,
                    ContainerLevelAccess access) {
        super(ModMenus.POOL.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.access = access;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                addSlot(new SlotItemHandler(racks, col + row * 6, 8 + col * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                addSlot(new SlotItemHandler(output, col + row * 2, 134 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        addInventory(this::addSlot, inventory);
        addDataSlots(data);
    }

    /** The player's inventory and hotbar, as every Ryzer Gen screen of this height has them. */
    public static void addInventory(Consumer<Slot> add, Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                add.accept(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            add.accept(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public int water() {
        return data.get(DATA_WATER);
    }

    /** How far a rack's item has cooled, 0 to 1. */
    public float progress(int rack) {
        return data.get(DATA_PROGRESS + rack) / 1000F;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        int machine = RACKS + OUTPUTS;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machine) {
            if (!moveItemStackTo(stack, machine, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!HotFuel.isSpentFuel(stack) || !moveItemStackTo(stack, 0, RACKS, false)) {
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
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof PoolControllerBlock
                && player.canInteractWithBlock(pos, 8.0), true);
    }
}
