package com.ryzer.ryzergen.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.ryzer.ryzergen.cable.CableBlockEntity.*;

/**
 * The panel for one cable or pipe: a row per side joined to a machine, with what came in against
 * its limit and, on sides that feed the network, a slot for that side's fitting; then the player's
 * inventory. Layout (keep in step
 * with CableScreen and art/tools/gui_textures.py, cable): rows from y 20, 24 apart, a footer row,
 * then the inventory section.
 */
public class CableMenu extends AbstractContainerMenu {
    public static final int ROW_X = 10;
    public static final int ROW_Y = 20;
    public static final int ROW_W = 156;
    public static final int ROW_STEP = 24;
    /** The fitting slot at the right end of each row (the item's position). */
    public static final int FITTING_X = ROW_X + ROW_W - 18;
    public static final int FITTING_DY = 3;

    private final CableKind kind;
    private final BlockPos pos;
    private final List<Direction> sides;
    private final List<Direction> feeding;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final int fittingSlots;

    /** Client side: the cable's position and its rows come with the open packet. */
    public CableMenu(CableKind kind, int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(kind, containerId, inventory, buffer.readBlockPos(), readSides(buffer), readSides(buffer), new SimpleContainer(6),
                new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public CableMenu(CableKind kind, int containerId, Inventory inventory, BlockPos pos, List<Direction> sides,
                     List<Direction> feeding, Container fittings, ContainerData data, ContainerLevelAccess access) {
        super(kind.menuType(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.kind = kind;
        this.pos = pos;
        this.sides = List.copyOf(sides);
        this.feeding = List.copyOf(feeding);
        this.data = data;
        this.access = access;
        // Only sides that feed the network get a slot: a fitting on one that only receives does nothing.
        for (int row = 0; row < sides.size(); row++) {
            if (!feeding.contains(sides.get(row))) {
                continue;
            }
            addSlot(new Slot(fittings, sides.get(row).get3DDataValue(), FITTING_X, rowY(row) + FITTING_DY) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return CableUpgrade.of(stack) != CableUpgrade.NONE;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        fittingSlots = slots.size();
        int top = inventoryTop(sides.size());
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, top + 19 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, top + 77));
        }
        addDataSlots(data);
    }

    /** What the server sends when opening: the rows, as a mask of sides. */
    public static void writeSides(FriendlyByteBuf buffer, List<Direction> sides) {
        int mask = 0;
        for (Direction dir : sides) {
            mask |= 1 << dir.get3DDataValue();
        }
        buffer.writeByte(mask);
    }

    private static List<Direction> readSides(FriendlyByteBuf buffer) {
        int mask = buffer.readByte();
        List<Direction> sides = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if ((mask & 1 << dir.get3DDataValue()) != 0) {
                sides.add(dir);
            }
        }
        return sides;
    }

    public static int rowY(int row) {
        return ROW_Y + row * ROW_STEP;
    }

    /** Where the inventory section starts: after the rows (at least one) and the footer. */
    public static int inventoryTop(int rows) {
        return ROW_Y + (Math.max(rows, 1) + 1) * ROW_STEP;
    }

    public CableKind kind() {
        return kind;
    }

    public BlockPos pos() {
        return pos;
    }

    /** The rows: sides joined to a machine, in a fixed order. */
    public List<Direction> sides() {
        return sides;
    }

    /** Whether a row's side feeds the network (pulled, pushed in, or drawn on), so it takes a fitting. */
    public boolean feeds(Direction side) {
        return feeding.contains(side);
    }

    private int wide(int low, int high) {
        return (data.get(high) & 0xFFFF) << 16 | (data.get(low) & 0xFFFF);
    }

    public CableSide mode(Direction side) {
        int id = data.get(side.get3DDataValue() * DATA_PER_SIDE) & 3;
        return id < CableSide.values().length ? CableSide.values()[id] : CableSide.NONE;
    }

    public CableUpgrade upgrade(Direction side) {
        return CableUpgrade.byId(data.get(side.get3DDataValue() * DATA_PER_SIDE) >> 2);
    }

    /** What this side moved last tick (FE, items or mB), pulled or pushed in. */
    public int moved(Direction side) {
        int base = side.get3DDataValue() * DATA_PER_SIDE;
        return wide(base + 1, base + 2);
    }

    /** The base limit per input, before fittings: FE per tick, items per second, or mB per tick. */
    public int baseRate() {
        return wide(DATA_MAX_LOW, DATA_MAX_HIGH);
    }

    /** One side's limit, with its fitting. */
    public int maxRate(Direction side) {
        return upgrade(side).scale(baseRate());
    }

    public int receivers() {
        return data.get(DATA_RECEIVERS);
    }

    /** Batteries (or any storage) on an energy network, which take the surplus and cover shortfalls. */
    public int buffers() {
        return data.get(DATA_BUFFERS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerEnd = fittingSlots + 36;
        if (index < fittingSlots) {
            if (!moveItemStackTo(stack, fittingSlots, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (CableUpgrade.of(stack) != CableUpgrade.NONE) {
            // Into the first empty fitting slot, one at a time.
            boolean moved = false;
            for (int i = 0; i < fittingSlots && !moved; i++) {
                Slot target = slots.get(i);
                if (!target.hasItem()) {
                    target.setByPlayer(stack.split(1));
                    moved = true;
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else {
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
        return access.evaluate((level, blockPos) -> level.getBlockState(blockPos).getBlock() instanceof CableBlock
                && player.canInteractWithBlock(blockPos, 4.0), true);
    }
}
