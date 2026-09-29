package com.ryzer.ryzergen.battery.container;

import com.ryzer.ryzergen.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import static com.ryzer.ryzergen.battery.container.BatteryControllerBlockEntity.*;

/** The formed container's readout: racks go in and out by hand on the container itself. */
public class ContainerMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public ContainerMenu(int containerId, Inventory inventory) {
        this(containerId, new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public ContainerMenu(int containerId, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.CONTAINER_BATTERY.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.access = access;
        addDataSlots(data);
    }

    private int wide(int low, int high) {
        return (data.get(high) & 0xFFFF) << 16 | (data.get(low) & 0xFFFF);
    }

    public int energy() {
        return wide(DATA_ENERGY_LOW, DATA_ENERGY_HIGH);
    }

    public int capacity() {
        return wide(DATA_CAPACITY_LOW, DATA_CAPACITY_HIGH);
    }

    public int in() {
        return wide(DATA_IN_LOW, DATA_IN_HIGH);
    }

    public int out() {
        return wide(DATA_OUT_LOW, DATA_OUT_HIGH);
    }

    public int racks() {
        return data.get(DATA_RACKS);
    }

    public int coolant() {
        return data.get(DATA_COOLANT);
    }

    public boolean installed(int slot) {
        return (wide(DATA_MASK_LOW, DATA_MASK_HIGH) & 1 << slot) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof BatteryControllerBlock
                && player.canInteractWithBlock(pos, 10.0), true);
    }
}
