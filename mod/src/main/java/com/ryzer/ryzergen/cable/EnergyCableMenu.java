package com.ryzer.ryzergen.cable;

import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModMenus;
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

import static com.ryzer.ryzergen.cable.EnergyCableBlockEntity.*;

/** A read-out only menu for one cable: what each extract side pulls, against the cable's limit. */
public class EnergyCableMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client side: the cable's position comes with the open packet, so the screen can name its neighbours. */
    public EnergyCableMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, buffer.readBlockPos(), new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public EnergyCableMenu(int containerId, BlockPos pos, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.ENERGY_CABLE.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.pos = pos;
        this.data = data;
        this.access = access;
        addDataSlots(data);
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

    /** FE pulled through this side last tick. */
    public int moved(Direction side) {
        int base = side.get3DDataValue() * DATA_PER_SIDE;
        return wide(base + 1, base + 2);
    }

    /** The most one extract side can pull each tick. */
    public int maxRate() {
        return wide(DATA_MAX_LOW, DATA_MAX_HIGH);
    }

    public int receivers() {
        return data.get(DATA_RECEIVERS);
    }

    /** Batteries (or any storage) on the network, which take the surplus and cover shortfalls. */
    public int buffers() {
        return data.get(DATA_BUFFERS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ENERGY_CABLE.get());
    }
}
