package com.ryzer.ryzergen.machine.microreactor;

import com.ryzer.ryzergen.machine.RedstoneMode;
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
import org.jetbrains.annotations.Nullable;

import static com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlockEntity.*;

public class MicroreactorMenu extends AbstractContainerMenu {
    public static final int BUTTON_POWER = 0;
    public static final int BUTTON_REDSTONE = 1;
    public static final int BUTTON_SAFETIES = 2;
    public static final int BUTTON_DUMP = 3;

    private static final int FUEL_SLOT = 0;
    private static final int PLAYER_START = 1;
    private static final int HOTBAR_START = PLAYER_START + 27;
    private static final int PLAYER_END = HOTBAR_START + 9;

    @Nullable
    private final ReactorHeartBlockEntity heart;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client side: the slot contents and readings arrive from the server. */
    public MicroreactorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, createFuelSlot(() -> {}), new SimpleContainerData(DATA_COUNT));
    }

    public MicroreactorMenu(int containerId, Inventory inventory, @Nullable ReactorHeartBlockEntity heart,
                            IItemHandler fuel, ContainerData data) {
        super(ModMenus.MICROREACTOR.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.heart = heart;
        this.data = data;
        this.access = heart == null ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(heart.getLevel(), heart.getBlockPos());

        addSlot(new SlotItemHandler(fuel, 0, 8, 21));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 106 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 164));
        }
        addDataSlots(data);
    }

    public int energy() {
        return (data.get(DATA_ENERGY_HIGH) & 0xFFFF) << 16 | (data.get(DATA_ENERGY_LOW) & 0xFFFF);
    }

    public int coolant() {
        return data.get(DATA_COOLANT);
    }

    public int temperature() {
        return data.get(DATA_TEMPERATURE);
    }

    public int output() {
        return data.get(DATA_OUTPUT);
    }

    public boolean enabled() {
        return data.get(DATA_ENABLED) != 0;
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byId(data.get(DATA_REDSTONE));
    }

    /** Fuel left in the core, from 0 to 1. */
    public float fuel() {
        return Mth.clamp(data.get(DATA_FUEL) / 1000F, 0, 1);
    }

    /** Heat-to-power efficiency, from 0 to 1. */
    public float efficiency() {
        return data.get(DATA_EFFICIENCY) / 1000F;
    }

    public boolean dumpExcess() {
        return data.get(DATA_DUMP) != 0;
    }

    public boolean safeties() {
        return data.get(DATA_SAFETIES) != 0;
    }

    public Status status() {
        int id = data.get(DATA_STATUS);
        return id >= 0 && id < Status.values().length ? Status.values()[id] : Status.OFFLINE;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (heart == null) {
            return false;
        }
        switch (id) {
            case BUTTON_POWER -> heart.togglePower();
            case BUTTON_REDSTONE -> heart.cycleRedstoneMode();
            case BUTTON_SAFETIES -> heart.toggleSafeties();
            case BUTTON_DUMP -> heart.toggleDump();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == FUEL_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (slots.get(FUEL_SLOT).mayPlace(stack) && moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
            // Moved into the fuel slot.
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
        return stillValid(access, player, ModBlocks.REACTOR_HEART.get());
    }
}
