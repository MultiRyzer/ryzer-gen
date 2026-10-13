package com.ryzer.ryzergen.machine.assembler;

import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.machine.SoundSwitch;
import com.ryzer.ryzergen.machine.SpeedModuleItem;
import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import com.ryzer.ryzergen.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import static com.ryzer.ryzergen.machine.assembler.AssemblerBlockEntity.*;

/**
 * The formed Assembler's screen: the pattern rack (nine patterns, chosen on the left) and the chosen
 * pattern's ghost grid with its result, the speed module slot with the power and redstone keys, the
 * buffer (5 x 2) and the output (3 x 2), and the player's inventory. The ghost grids are not slots:
 * the screen sends a button press with the cell, and the server sets it from the item the player
 * holds. Positions match art/tools/gui_textures.py (assembler).
 */
public class AssemblerMenu extends AbstractContainerMenu {
    public static final int BUTTON_POWER = 0;
    public static final int BUTTON_REDSTONE = 1;
    /** A ghost cell: BUTTON_GHOST + pattern * 9 + cell. Clearing a whole pattern: BUTTON_CLEAR + pattern. */
    public static final int BUTTON_GHOST = 1000;
    public static final int BUTTON_CLEAR = 2000;
    /** A ghost cell cleared whatever the player holds: BUTTON_UNSET + pattern * 9 + cell. */
    public static final int BUTTON_UNSET = 3000;
    public static final int FIRST_BUFFER = 1;
    public static final int FIRST_OUTPUT = FIRST_BUFFER + BUFFER;
    public static final int MACHINE_SLOTS = FIRST_OUTPUT + OUTPUTS;
    public static final int INVENTORY_Y = 145;
    public static final int MODULE_X = 44;
    public static final int MODULE_Y = 73;
    public static final int BUFFER_X = 8;
    public static final int OUTPUT_X = 116;
    public static final int STORE_Y = 94;

    private final BlockPos pos;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final DataSlot muted;

    public AssemblerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos(), new ItemStackHandler(1) {
            @Override
            public int getSlotLimit(int slot) {
                return ProcessingMachine.MAX_MODULES;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof SpeedModuleItem;
            }
        }, new ItemStackHandler(BUFFER), new ItemStackHandler(OUTPUTS), new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public AssemblerMenu(int containerId, Inventory inventory, BlockPos pos, IItemHandler tools, IItemHandler buffer, IItemHandler output,
                         ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.ASSEMBLER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.pos = pos;
        this.data = data;
        this.access = access;
        addSlot(new SlotItemHandler(tools, 0, MODULE_X, MODULE_Y));
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                addSlot(new SlotItemHandler(buffer, col + row * 5, BUFFER_X + col * 18, STORE_Y + row * 18));
            }
        }
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(output, col + row * 3, OUTPUT_X + col * 18, STORE_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
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
        muted = addDataSlot(SoundSwitch.slot(access));
    }

    /** The controller's block entity, on whichever side this menu is (the client's for the screen). */
    public @Nullable AssemblerBlockEntity machine(Level level) {
        return level.getBlockEntity(pos) instanceof AssemblerBlockEntity entity ? entity : null;
    }

    public int energy() {
        return (data.get(DATA_ENERGY_LOW) & 0xFFFF) | (data.get(DATA_ENERGY_HIGH) << 16);
    }

    public Status status() {
        return Status.byId(data.get(DATA_STATUS));
    }

    public boolean enabled() {
        return data.get(DATA_ENABLED) != 0;
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byId(data.get(DATA_REDSTONE));
    }

    public int speed() {
        return data.get(DATA_SPEED);
    }

    public int draw() {
        return AssemblerBlockEntity.draw(speed());
    }

    /** How far the cycle is, 0 to 1. */
    public float progress() {
        int total = data.get(DATA_TOTAL);
        return total == 0 ? 0 : data.get(DATA_PROGRESS) / (float) total;
    }

    /** The pattern being made, or -1. */
    public int active() {
        return data.get(DATA_ACTIVE);
    }

    /** The pattern an idle cell waits on, or -1, and the cells it lacks. */
    public int waiting() {
        return data.get(DATA_WAITING);
    }

    public int missingMask() {
        return data.get(DATA_MISSING_MASK);
    }

    public boolean muted() {
        return muted.get() != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (SoundSwitch.click(access, id)) {
            return true;
        }
        boolean ghost = id >= BUTTON_GHOST && id < BUTTON_GHOST + PATTERNS * GRID;
        boolean clear = id >= BUTTON_CLEAR && id < BUTTON_CLEAR + PATTERNS;
        boolean unset = id >= BUTTON_UNSET && id < BUTTON_UNSET + PATTERNS * GRID;
        if (id != BUTTON_POWER && id != BUTTON_REDSTONE && !ghost && !clear && !unset) {
            return false;
        }
        ItemStack held = getCarried();
        access.execute((level, at) -> {
            if (!(level.getBlockEntity(at) instanceof AssemblerBlockEntity cell)) {
                return;
            }
            if (id == BUTTON_POWER) {
                cell.togglePower();
            } else if (id == BUTTON_REDSTONE) {
                cell.cycleRedstoneMode();
            } else if (ghost) {
                int k = id - BUTTON_GHOST;
                cell.setGhost(k / GRID, k % GRID, held);
            } else if (unset) {
                int k = id - BUTTON_UNSET;
                cell.setGhost(k / GRID, k % GRID, ItemStack.EMPTY);
            } else {
                cell.clearPattern(id - BUTTON_CLEAR);
            }
        });
        return true;
    }

    /** Shift-click: machine slots to the player; a speed module to its slot, anything else into the buffer. */
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
        } else if (stack.getItem() instanceof SpeedModuleItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, FIRST_BUFFER, FIRST_OUTPUT, false)) {
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
        return access.evaluate((level, at) -> level.getBlockState(at).getBlock() instanceof AssemblerControllerBlock
                && player.canInteractWithBlock(at, 8.0), true);
    }
}
