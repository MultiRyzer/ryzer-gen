package com.ryzer.ryzergen.machine.processing;

import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.machine.processing.ProcessingBlockEntity.Status;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import static com.ryzer.ryzergen.machine.processing.ProcessingBlockEntity.*;

/**
 * The fuel cycle machines' menu. Layout (keep in step with ProcessingScreen and
 * art/tools/gui_textures.py, processing()): inputs in a row left of the arrow, outputs in a column
 * right of it, the power and redstone keys beside the energy well on the right with the upgrade
 * slot under them, the water well on
 * the far left for machines that take water, and a status line under the inputs.
 */
public class ProcessingMenu extends AbstractContainerMenu {
    public static final int BUTTON_POWER = 0;
    public static final int BUTTON_REDSTONE = 1;
    public static final int BUTTON_AUTO_OUTPUT = 2;

    public static final int SLOT_Y = 35;
    public static final int OUTPUT_X = 113;
    public static final int UPGRADE_X = 133;
    public static final int UPGRADE_Y = 53;

    private final ProcessingMachine machine;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final int playerStart;

    /** Client side: contents and readings arrive from the server. */
    public ProcessingMenu(ProcessingMachine machine, int containerId, Inventory inventory) {
        this(machine, containerId, inventory, createItems(machine, stack -> true, () -> {}),
                new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public ProcessingMenu(ProcessingMachine machine, int containerId, Inventory inventory, IItemHandler items,
                          ContainerData data, ContainerLevelAccess access) {
        super(machine.menuType(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.machine = machine;
        this.data = data;
        this.access = access;
        for (int i = 0; i < machine.inputs(); i++) {
            addSlot(new SlotItemHandler(items, i, inputX(machine, i), SLOT_Y));
        }
        for (int i = 0; i < machine.outputs(); i++) {
            addSlot(new SlotItemHandler(items, machine.inputs() + i, OUTPUT_X, outputY(machine, i)));
        }
        addSlot(new SlotItemHandler(items, machine.upgradeSlot(), UPGRADE_X, UPGRADE_Y));
        playerStart = slots.size();
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

    /** Input slots sit in a row, centred on x 53 (the middle of the space left of the arrow). */
    public static int inputX(ProcessingMachine machine, int i) {
        return 53 - 9 * machine.inputs() + 18 * i;
    }

    /** Output slots sit in a column, centred on the arrow's row. */
    public static int outputY(ProcessingMachine machine, int i) {
        return SLOT_Y - 9 * (machine.outputs() - 1) + 18 * i;
    }

    public ProcessingMachine machine() {
        return machine;
    }

    public float progress() {
        int total = data.get(DATA_TOTAL);
        return total == 0 ? 0 : Mth.clamp(data.get(DATA_PROGRESS) / (float) total, 0, 1);
    }

    /** Ticks until the current operation finishes at the current speed, or 0 if none is under way. */
    public int ticksLeft() {
        int total = data.get(DATA_TOTAL);
        int left = total - data.get(DATA_PROGRESS);
        if (total == 0 || left <= 0) {
            return 0;
        }
        int speed = speed(modules());
        return (left + speed - 1) / speed;
    }

    public int energy() {
        return (data.get(DATA_ENERGY_HIGH) & 0xFFFF) << 16 | (data.get(DATA_ENERGY_LOW) & 0xFFFF);
    }

    public int water() {
        return data.get(DATA_WATER);
    }

    /** mB in the output tank. */
    public int tank() {
        return data.get(DATA_TANK);
    }

    /** The output tank's fluid (water while it is empty, never drawn then). */
    public Fluid tankFluid() {
        int id = data.get(DATA_TANK_FLUID);
        return id < 0 ? Fluids.EMPTY : BuiltInRegistries.FLUID.byId(id);
    }

    public Status status() {
        int id = data.get(DATA_STATUS);
        return id >= 0 && id < Status.values().length ? Status.values()[id] : Status.IDLE;
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byId(data.get(DATA_REDSTONE));
    }

    public boolean enabled() {
        return data.get(DATA_ENABLED) != 0;
    }

    public boolean autoOutput() {
        return data.get(DATA_AUTO_OUTPUT) != 0;
    }

    public int modules() {
        return data.get(DATA_MODULES);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_POWER && id != BUTTON_REDSTONE && id != BUTTON_AUTO_OUTPUT) {
            return false;
        }
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof ProcessingBlockEntity entity) {
                if (id == BUTTON_POWER) {
                    entity.togglePower();
                } else if (id == BUTTON_AUTO_OUTPUT) {
                    entity.toggleAutoOutput();
                } else {
                    entity.cycleRedstoneMode();
                }
            }
        });
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
        int playerEnd = playerStart + 36;
        int hotbarStart = playerStart + 27;
        if (index < playerStart) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.SPEED_MODULE.get())
                ? moveItemStackTo(stack, machine.upgradeSlot(), machine.upgradeSlot() + 1, false)
                : moveItemStackTo(stack, 0, machine.inputs(), false)) {
            // Moved into the machine: modules to the upgrade slot, anything else to the inputs.
        } else if (index < hotbarStart) {
            if (!moveItemStackTo(stack, hotbarStart, playerEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, playerStart, hotbarStart, false)) {
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
        return stillValid(access, player, machine.block());
    }
}
