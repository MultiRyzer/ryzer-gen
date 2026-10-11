package com.ryzer.ryzergen.machine.drill;

import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
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

import static com.ryzer.ryzergen.machine.drill.MegaDrillBlockEntity.*;

/**
 * The formed drill's screen: the head, tip and speed module slots on the left, the output (6 x 2)
 * and the filter row (6) under the readout, and the player's inventory. Positions match
 * art/tools/gui_textures.py (mega_drill).
 */
public class MegaDrillMenu extends AbstractContainerMenu {
    public static final int BUTTON_POWER = 0;
    public static final int BUTTON_REDSTONE = 1;
    public static final int BUTTON_MUTE = 2;
    public static final int FIRST_OUTPUT = TOOL_SLOTS;
    public static final int FIRST_FILTER = FIRST_OUTPUT + OUTPUTS;
    public static final int MACHINE_SLOTS = FIRST_FILTER + FILTERS;
    /** Where the player's inventory starts, on this taller screen. */
    public static final int INVENTORY_Y = 123;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    public MegaDrillMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new ItemStackHandler(TOOL_SLOTS) {
            @Override
            public int getSlotLimit(int slot) {
                return slot == SLOT_MODULES ? ProcessingMachine.MAX_MODULES : 1;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return MegaDrillBlockEntity.toolFits(slot, stack);
            }
        }, new ItemStackHandler(OUTPUTS), new ItemStackHandler(FILTERS) {
            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        }, new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public MegaDrillMenu(int containerId, Inventory inventory, IItemHandler tools, IItemHandler output, IItemHandler filter,
                         ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.MEGA_DRILL.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.access = access;
        for (int slot = 0; slot < TOOL_SLOTS; slot++) {
            addSlot(new SlotItemHandler(tools, slot, 8, 17 + slot * 18));
        }
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 6; col++) {
                addSlot(new SlotItemHandler(output, col + row * 6, 28 + col * 18, 53 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        for (int col = 0; col < FILTERS; col++) {
            addSlot(new SlotItemHandler(filter, col, 28 + col * 18, 91));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
    }

    public int energy() {
        return (data.get(DATA_ENERGY_LOW) & 0xFFFF) | (data.get(DATA_ENERGY_HIGH) << 16);
    }

    public int water() {
        return data.get(DATA_WATER);
    }

    public Status status() {
        return Status.byId(data.get(DATA_STATUS));
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byId(data.get(DATA_REDSTONE));
    }

    public boolean enabled() {
        return data.get(DATA_ENABLED) != 0;
    }

    /** The layer being mined (or next to be), as a height. */
    public int layer() {
        return data.get(DATA_LAYER);
    }

    public boolean muted() {
        return data.get(DATA_MUTED) != 0;
    }

    /** Past bedrock, boring the deep crust. */
    public boolean deep() {
        return data.get(DATA_DEEP) != 0;
    }

    public int speed() {
        return data.get(DATA_SPEED);
    }

    /** FE per tick while charging or firing. */
    public int draw() {
        return (data.get(DATA_DRAW_LOW) & 0xFFFF) | (data.get(DATA_DRAW_HIGH) << 16);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_POWER && id != BUTTON_REDSTONE && id != BUTTON_MUTE) {
            return false;
        }
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof MegaDrillBlockEntity drill) {
                if (id == BUTTON_POWER) {
                    drill.togglePower();
                } else if (id == BUTTON_MUTE) {
                    drill.toggleMute();
                } else {
                    drill.cycleRedstoneMode();
                }
            }
        });
        return true;
    }

    /** Shift-click: machine slots to the player; a head, tip or speed module from the player to its slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int target = -1;
            for (int tool = 0; tool < TOOL_SLOTS; tool++) {
                if (MegaDrillBlockEntity.toolFits(tool, stack)) {
                    target = tool;
                }
            }
            if (target < 0 || !moveItemStackTo(stack, target, target + 1, false)) {
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
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof MegaDrillControllerBlock
                // Any block of the drill opens it, up to the housing a dozen blocks over the controller.
                && player.canInteractWithBlock(pos, 16.0), true);
    }
}
