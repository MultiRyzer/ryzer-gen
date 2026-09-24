package com.ryzer.ryzergen.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import static com.ryzer.ryzergen.cable.CableBlockEntity.*;

/** A read-out only menu for one cable or pipe: what each extract side pulls, against its limit. */
public class CableMenu extends AbstractContainerMenu {
    private final CableKind kind;
    private final BlockPos pos;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client side: the cable's position comes with the open packet, so the screen can name its neighbours. */
    public CableMenu(CableKind kind, int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(kind, containerId, buffer.readBlockPos(), new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public CableMenu(CableKind kind, int containerId, BlockPos pos, ContainerData data, ContainerLevelAccess access) {
        super(kind.menuType(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.kind = kind;
        this.pos = pos;
        this.data = data;
        this.access = access;
        addDataSlots(data);
    }

    public CableKind kind() {
        return kind;
    }

    public BlockPos pos() {
        return pos;
    }

    private int wide(int low, int high) {
        return (data.get(high) & 0xFFFF) << 16 | (data.get(low) & 0xFFFF);
    }

    public CableSide mode(Direction side) {
        int id = data.get(side.get3DDataValue() * DATA_PER_SIDE);
        return id >= 0 && id < CableSide.values().length ? CableSide.values()[id] : CableSide.NONE;
    }

    /** What this side pulled last tick (FE, or items). */
    public int moved(Direction side) {
        int base = side.get3DDataValue() * DATA_PER_SIDE;
        return wide(base + 1, base + 2);
    }

    /** The most one extract side can pull: FE per tick, or items per second. */
    public int maxRate() {
        return wide(DATA_MAX_LOW, DATA_MAX_HIGH);
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
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, blockPos) -> level.getBlockState(blockPos).getBlock() instanceof CableBlock
                && player.canInteractWithBlock(blockPos, 4.0), true);
    }
}
