package com.ryzer.ryzergen.machine.fission;

import com.ryzer.ryzergen.advancement.Milestone;
import com.ryzer.ryzergen.registry.ModTriggers;
import net.minecraft.server.level.ServerPlayer;
import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.machine.fission.StationReactor.Channel;
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
import org.jetbrains.annotations.Nullable;

/**
 * The formed station's controls: the 5 x 5 core grid (each channel a slot for its rod or block, its
 * plan set with the tool buttons), power and redstone, and the live readings. The grid is laid out
 * as seen from the station's front, so the front row of channels is the bottom row here.
 */
public class StationControlMenu extends AbstractContainerMenu {
    public static final int BUTTON_POWER = 0;
    public static final int BUTTON_REDSTONE = 1;
    /** The safety interlock switch: off is overdrive. */
    public static final int BUTTON_SAFETY = 2;
    /** Plus channel * 8 + channel type: set that channel's plan. */
    public static final int BUTTON_PLAN = 100;
    /** Plus channel: take out what is in it, or clear its plan if it is empty. */
    public static final int BUTTON_CLEAR = 400;

    public static final int DATA_TEMPERATURE = 0;
    public static final int DATA_STATUS = 1;
    public static final int DATA_OUTPUT_LOW = 2;
    public static final int DATA_OUTPUT_HIGH = 3;
    public static final int DATA_EFFICIENCY = 4;
    public static final int DATA_WATER = 5;
    public static final int DATA_ENERGY_LOW = 6;
    public static final int DATA_ENERGY_HIGH = 7;
    public static final int DATA_ENABLED = 8;
    public static final int DATA_REDSTONE = 9;
    public static final int DATA_SAFETIES = 10;
    /** Seconds left before a flux tilt makes the core unstable. */
    public static final int DATA_TILT = 11;
    public static final int DATA_PLAN = 12;
    public static final int DATA_COUNT = DATA_PLAN + StationReactor.CHANNELS;

    public static final int GRID_X = 8;
    public static final int GRID_Y = 18;
    /** The panel is wider than a chest, to fit the planning readout; the inventory is centred. */
    public static final int WIDTH = 216;
    public static final int INVENTORY_X = (WIDTH - 162) / 2 + 1;
    public static final int INVENTORY_Y = 140;

    private final @Nullable StationCoreBlockEntity core;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client side: a stand-in store whose rules read the plan from the synced data. */
    public StationControlMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new SimpleContainerData(DATA_COUNT), null, ContainerLevelAccess.NULL);
    }

    public StationControlMenu(int containerId, Inventory inventory, StationCoreBlockEntity core, ContainerLevelAccess access) {
        this(containerId, inventory, core, liveData(core.runner()), core.runner().channels, access);
    }

    private StationControlMenu(int containerId, Inventory inventory, @Nullable StationCoreBlockEntity core, ContainerData data,
                               @Nullable IItemHandler channels, ContainerLevelAccess access) {
        super(ModMenus.STATION_CONTROL.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.core = core;
        this.data = data;
        this.access = access;
        IItemHandler grid = channels != null ? channels : new ItemStackHandler(StationReactor.CHANNELS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return plan(slot).accepts(stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            addSlot(new ChannelSlot(grid, i));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
    }

    /** Where a channel sits on screen: front row at the bottom, as the player at the front sees it. */
    public static int slotX(int channel) {
        return GRID_X + (StationReactor.GRID - 1 - channel % StationReactor.GRID) * 18;
    }

    public static int slotY(int channel) {
        return GRID_Y + (StationReactor.GRID - 1 - channel / StationReactor.GRID) * 18;
    }

    /** A channel's slot: one item, and only what its plan takes. */
    private class ChannelSlot extends SlotItemHandler {
        ChannelSlot(IItemHandler handler, int channel) {
            super(handler, channel, slotX(channel), slotY(channel));
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return plan(getSlotIndex()).accepts(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static ContainerData liveData(StationRunner runner) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (index >= DATA_PLAN) {
                    return runner.types[index - DATA_PLAN].ordinal();
                }
                return switch (index) {
                    case DATA_TEMPERATURE -> Math.round(runner.temperature);
                    case DATA_STATUS -> runner.status.ordinal();
                    case DATA_OUTPUT_LOW -> runner.output & 0xFFFF;
                    case DATA_OUTPUT_HIGH -> runner.output >>> 16;
                    case DATA_EFFICIENCY -> runner.efficiencyPermille;
                    case DATA_WATER -> runner.water.getFluidAmount();
                    case DATA_ENERGY_LOW -> runner.energy().getEnergyStored() & 0xFFFF;
                    case DATA_ENERGY_HIGH -> runner.energy().getEnergyStored() >>> 16;
                    case DATA_ENABLED -> runner.enabled ? 1 : 0;
                    case DATA_REDSTONE -> runner.redstoneMode.ordinal();
                    case DATA_SAFETIES -> runner.safeties ? 1 : 0;
                    case DATA_TILT -> (runner.tiltTicksLeft() + 19) / 20;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Read-only on the server.
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    public Channel plan(int channel) {
        return Channel.byId(data.get(DATA_PLAN + channel));
    }

    public Channel[] plan() {
        Channel[] plan = new Channel[StationReactor.CHANNELS];
        for (int i = 0; i < plan.length; i++) {
            plan[i] = plan(i);
        }
        return plan;
    }

    public ItemStack[] rods() {
        ItemStack[] rods = new ItemStack[StationReactor.CHANNELS];
        for (int i = 0; i < rods.length; i++) {
            rods[i] = slots.get(i).getItem();
        }
        return rods;
    }

    private int wide(int low, int high) {
        return (data.get(high) & 0xFFFF) << 16 | (data.get(low) & 0xFFFF);
    }

    public int temperature() {
        return data.get(DATA_TEMPERATURE);
    }

    public StationRunner.Status status() {
        int id = data.get(DATA_STATUS);
        return id >= 0 && id < StationRunner.Status.values().length ? StationRunner.Status.values()[id] : StationRunner.Status.OFFLINE;
    }

    public int output() {
        return wide(DATA_OUTPUT_LOW, DATA_OUTPUT_HIGH);
    }

    public int efficiencyPermille() {
        return data.get(DATA_EFFICIENCY);
    }

    public int water() {
        return data.get(DATA_WATER);
    }

    public int energy() {
        return wide(DATA_ENERGY_LOW, DATA_ENERGY_HIGH);
    }

    public boolean enabled() {
        return data.get(DATA_ENABLED) != 0;
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byId(data.get(DATA_REDSTONE));
    }

    public boolean safeties() {
        return data.get(DATA_SAFETIES) != 0;
    }

    public int tiltSeconds() {
        return data.get(DATA_TILT);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (core == null) {
            return false;
        }
        StationRunner runner = core.runner();
        if (id == BUTTON_POWER) {
            runner.enabled = !runner.enabled;
            core.setChanged();
            return true;
        }
        if (id == BUTTON_SAFETY) {
            runner.toggleSafeties();
            if (!runner.safeties && player instanceof ServerPlayer serverPlayer) {
                ModTriggers.MILESTONE.get().trigger(serverPlayer, Milestone.STATION_OVERDRIVE);
            }
            return true;
        }
        if (id == BUTTON_REDSTONE) {
            runner.redstoneMode = runner.redstoneMode.next();
            core.setChanged();
            return true;
        }
        if (id >= BUTTON_CLEAR && id < BUTTON_CLEAR + StationReactor.CHANNELS) {
            ItemStack returned = runner.clear(id - BUTTON_CLEAR);
            if (!returned.isEmpty()) {
                player.getInventory().placeItemBackInInventory(returned);
            }
            return true;
        }
        int plan = id - BUTTON_PLAN;
        if (plan >= 0 && plan < StationReactor.CHANNELS * 8) {
            ItemStack returned = runner.setType(plan / 8, Channel.byId(plan % 8));
            if (!returned.isEmpty()) {
                player.getInventory().placeItemBackInInventory(returned);
            }
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < StationReactor.CHANNELS) {
            if (!moveItemStackTo(stack, StationReactor.CHANNELS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Into the first channel planned for it, one at a time.
            boolean moved = false;
            for (int i = 0; i < StationReactor.CHANNELS && !stack.isEmpty(); i++) {
                Slot target = slots.get(i);
                if (!target.hasItem() && target.mayPlace(stack)) {
                    target.setByPlayer(stack.split(1));
                    moved = true;
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
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
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof StationCoreBlock
                && level.getBlockState(pos).getValue(StationPartBlock.FORMED) && player.distanceToSqr(pos.getCenter()) < 20 * 20, true);
    }
}
