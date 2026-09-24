package com.ryzer.ryzergen.battery;

import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import static com.ryzer.ryzergen.battery.HomeBatteryBlockEntity.*;

/** A read-out only menu: modules go in and out by hand on the cabinet itself, as the design says. */
public class HomeBatteryMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public HomeBatteryMenu(int containerId, Inventory inventory) {
        this(containerId, new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public HomeBatteryMenu(int containerId, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.HOME_BATTERY.get(), containerId);
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

    public BatteryChemistry module(int bay) {
        int id = data.get(DATA_MODULES + bay);
        return id >= 0 && id < BatteryChemistry.values().length ? BatteryChemistry.values()[id] : BatteryChemistry.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.HOME_BATTERY.get());
    }
}
