package com.ryzer.ryzergen.machine.pump;

import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import static com.ryzer.ryzergen.machine.pump.IntakePumpBlockEntity.*;

/** A read-out menu with one control, the redstone mode. */
public class IntakePumpMenu extends AbstractContainerMenu {
    public static final int BUTTON_REDSTONE = 0;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    public IntakePumpMenu(int containerId, Inventory inventory) {
        this(containerId, new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public IntakePumpMenu(int containerId, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.INTAKE_PUMP.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.access = access;
        addDataSlots(data);
    }

    public int energy() {
        return (data.get(DATA_ENERGY_HIGH) & 0xFFFF) << 16 | (data.get(DATA_ENERGY_LOW) & 0xFFFF);
    }

    public int water() {
        return data.get(DATA_WATER);
    }

    public boolean waterBelow() {
        return data.get(DATA_WATER_BELOW) != 0;
    }

    public Status status() {
        int id = data.get(DATA_STATUS);
        return id >= 0 && id < Status.values().length ? Status.values()[id] : Status.NO_WATER;
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byId(data.get(DATA_REDSTONE));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_REDSTONE) {
            access.execute((level, pos) -> {
                if (level.getBlockEntity(pos) instanceof IntakePumpBlockEntity pump) {
                    pump.cycleRedstoneMode();
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.INTAKE_PUMP.get());
    }
}
