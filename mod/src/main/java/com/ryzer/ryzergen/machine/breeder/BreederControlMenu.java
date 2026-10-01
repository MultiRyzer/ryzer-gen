package com.ryzer.ryzergen.machine.breeder;

import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.machine.breeder.BreederReactor.Position;
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
 * The formed breeder's controls, laid out as the station's: the core's 19 positions in hex rows
 * (each a slot for its assembly, its plan set with the tool buttons), the pumps' flow, power and
 * redstone, and the live readings. The back row of the core is the top row here.
 */
public class BreederControlMenu extends AbstractContainerMenu {
    public static final int BUTTON_POWER = 0;
    public static final int BUTTON_REDSTONE = 1;
    public static final int BUTTON_FLOW_DOWN = 2;
    public static final int BUTTON_FLOW_UP = 3;
    /** Plus position * 8 + position type: set that position's plan. */
    public static final int BUTTON_PLAN = 100;
    /** Plus position: take out what is in it, or clear its plan if it is empty. */
    public static final int BUTTON_CLEAR = 400;

    public static final int DATA_TEMPERATURE = 0;
    public static final int DATA_STATUS = 1;
    public static final int DATA_OUTPUT_LOW = 2;
    public static final int DATA_OUTPUT_HIGH = 3;
    public static final int DATA_EFFICIENCY = 4;
    public static final int DATA_SODIUM = 5;
    public static final int DATA_ENERGY_LOW = 6;
    public static final int DATA_ENERGY_HIGH = 7;
    public static final int DATA_ENABLED = 8;
    public static final int DATA_REDSTONE = 9;
    public static final int DATA_FLOW = 10;
    public static final int DATA_PUMPING = 11;
    public static final int DATA_PLAN = 12;
    public static final int DATA_COUNT = DATA_PLAN + BreederReactor.POSITIONS;

    public static final int WIDTH = 216;
    public static final int INVENTORY_X = (WIDTH - 162) / 2 + 1;
    public static final int INVENTORY_Y = 140;

    private final @Nullable BreederCoreBlockEntity core;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client side: a stand-in store whose rules read the plan from the synced data. */
    public BreederControlMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new SimpleContainerData(DATA_COUNT), null, ContainerLevelAccess.NULL);
    }

    public BreederControlMenu(int containerId, Inventory inventory, BreederCoreBlockEntity core, ContainerLevelAccess access) {
        this(containerId, inventory, core, liveData(core.runner()), core.runner().positions, access);
    }

    private BreederControlMenu(int containerId, Inventory inventory, @Nullable BreederCoreBlockEntity core, ContainerData data,
                               @Nullable IItemHandler positions, ContainerLevelAccess access) {
        super(ModMenus.BREEDER_CONTROL.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.core = core;
        this.data = data;
        this.access = access;
        IItemHandler grid = positions != null ? positions : new ItemStackHandler(BreederReactor.POSITIONS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return plan(slot).accepts(stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        for (int i = 0; i < BreederReactor.POSITIONS; i++) {
            addSlot(new PositionSlot(grid, i));
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

    /** Where a position sits on screen: hex rows, half a slot apart, matching gui_textures.py (breeder_control). */
    public static int slotX(int position) {
        return 8 + 9 * (2 * BreederReactor.q(position) + BreederReactor.r(position) + 2 * BreederReactor.RADIUS);
    }

    public static int slotY(int position) {
        return 18 + (BreederReactor.r(position) + BreederReactor.RADIUS) * 18;
    }

    /** A position's slot: one item, and only what its plan takes. */
    private class PositionSlot extends SlotItemHandler {
        PositionSlot(IItemHandler handler, int position) {
            super(handler, position, slotX(position), slotY(position));
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

    private static ContainerData liveData(BreederRunner runner) {
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
                    case DATA_SODIUM -> runner.sodium.getFluidAmount();
                    case DATA_ENERGY_LOW -> runner.energy().getEnergyStored() & 0xFFFF;
                    case DATA_ENERGY_HIGH -> runner.energy().getEnergyStored() >>> 16;
                    case DATA_ENABLED -> runner.enabled ? 1 : 0;
                    case DATA_REDSTONE -> runner.redstoneMode.ordinal();
                    case DATA_FLOW -> runner.flow;
                    case DATA_PUMPING -> runner.pumping;
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

    public Position plan(int position) {
        return Position.byId(data.get(DATA_PLAN + position));
    }

    public Position[] plan() {
        Position[] plan = new Position[BreederReactor.POSITIONS];
        for (int i = 0; i < plan.length; i++) {
            plan[i] = plan(i);
        }
        return plan;
    }

    public ItemStack[] items() {
        ItemStack[] items = new ItemStack[BreederReactor.POSITIONS];
        for (int i = 0; i < items.length; i++) {
            items[i] = slots.get(i).getItem();
        }
        return items;
    }

    private int wide(int low, int high) {
        return (data.get(high) & 0xFFFF) << 16 | (data.get(low) & 0xFFFF);
    }

    public int temperature() {
        return data.get(DATA_TEMPERATURE);
    }

    public BreederRunner.Status status() {
        int id = data.get(DATA_STATUS);
        return id >= 0 && id < BreederRunner.Status.values().length ? BreederRunner.Status.values()[id] : BreederRunner.Status.OFFLINE;
    }

    /** Net power out (the steam plant's, less the pumps'): negative while the pumps run on the buffer. */
    public int output() {
        return wide(DATA_OUTPUT_LOW, DATA_OUTPUT_HIGH);
    }

    public int efficiencyPermille() {
        return data.get(DATA_EFFICIENCY);
    }

    public int sodium() {
        return data.get(DATA_SODIUM);
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

    /** The pumps' flow, in percent. */
    public int flow() {
        return data.get(DATA_FLOW);
    }

    public int pumping() {
        return data.get(DATA_PUMPING);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (core == null) {
            return false;
        }
        BreederRunner runner = core.runner();
        if (id == BUTTON_POWER) {
            runner.enabled = !runner.enabled;
            core.setChanged();
            return true;
        }
        if (id == BUTTON_REDSTONE) {
            runner.redstoneMode = runner.redstoneMode.next();
            core.setChanged();
            return true;
        }
        if (id == BUTTON_FLOW_DOWN || id == BUTTON_FLOW_UP) {
            runner.setFlow(runner.flow + (id == BUTTON_FLOW_UP ? 1 : -1) * BreederRunner.FLOW_STEP);
            return true;
        }
        if (id >= BUTTON_CLEAR && id < BUTTON_CLEAR + BreederReactor.POSITIONS) {
            ItemStack returned = runner.clear(id - BUTTON_CLEAR);
            if (!returned.isEmpty()) {
                player.getInventory().placeItemBackInInventory(returned);
            }
            return true;
        }
        int plan = id - BUTTON_PLAN;
        if (plan >= 0 && plan < BreederReactor.POSITIONS * 8) {
            ItemStack returned = runner.setType(plan / 8, Position.byId(plan % 8));
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
        if (index < BreederReactor.POSITIONS) {
            if (!moveItemStackTo(stack, BreederReactor.POSITIONS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Into the first position planned for it, one at a time.
            boolean moved = false;
            for (int i = 0; i < BreederReactor.POSITIONS && !stack.isEmpty(); i++) {
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
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof BreederCoreBlock
                && level.getBlockState(pos).getValue(BreederPartBlock.FORMED) && player.distanceToSqr(pos.getCenter()) < 20 * 20, true);
    }
}
