package com.ryzer.ryzergen.battery;

import com.ryzer.ryzergen.registry.ModBlockEntities;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * The home battery's contents: up to six modules and the charge they hold together. Capacity and
 * rate are the sum of the modules (no separate inverter). Charge flows in from anything that pushes
 * (the microreactor, a cable) and out to anything that pulls (a cable side set to extract).
 */
public class HomeBatteryBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_MODULES = 6;

    public static final int DATA_ENERGY_LOW = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_CAPACITY_LOW = 2;
    public static final int DATA_CAPACITY_HIGH = 3;
    public static final int DATA_IN_LOW = 4;
    public static final int DATA_IN_HIGH = 5;
    public static final int DATA_OUT_LOW = 6;
    public static final int DATA_OUT_HIGH = 7;
    /** Then one entry per bay: the chemistry's ordinal. */
    public static final int DATA_MODULES = 8;
    public static final int DATA_COUNT = DATA_MODULES + MAX_MODULES;

    private final List<BatteryChemistry> modules = new ArrayList<>();
    private int energy;
    private int inThisTick;
    private int outThisTick;
    private int lastIn;
    private int lastOut;

    private final IEnergyStorage storage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int accepted = Math.max(0, Math.min(amount, Math.min(rate() - inThisTick, capacity() - energy)));
            if (!simulate && accepted > 0) {
                energy += accepted;
                inThisTick += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int given = Math.max(0, Math.min(amount, Math.min(rate() - outThisTick, energy)));
            if (!simulate && given > 0) {
                energy -= given;
                outThisTick += given;
                setChanged();
            }
            return given;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity();
        }

        @Override
        public boolean canExtract() {
            return !modules.isEmpty();
        }

        @Override
        public boolean canReceive() {
            return !modules.isEmpty();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_MODULES) {
                int bay = index - DATA_MODULES;
                return bay < modules.size() ? modules.get(bay).ordinal() : 0;
            }
            return switch (index) {
                case DATA_ENERGY_LOW -> energy & 0xFFFF;
                case DATA_ENERGY_HIGH -> energy >>> 16;
                case DATA_CAPACITY_LOW -> capacity() & 0xFFFF;
                case DATA_CAPACITY_HIGH -> capacity() >>> 16;
                case DATA_IN_LOW -> lastIn & 0xFFFF;
                case DATA_IN_HIGH -> lastIn >>> 16;
                case DATA_OUT_LOW -> lastOut & 0xFFFF;
                case DATA_OUT_HIGH -> lastOut >>> 16;
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

    public HomeBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOME_BATTERY.get(), pos, state);
    }

    public IEnergyStorage energy() {
        return storage;
    }

    public int capacity() {
        return modules.stream().mapToInt(BatteryChemistry::capacity).sum();
    }

    public int rate() {
        return modules.stream().mapToInt(BatteryChemistry::rate).sum();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HomeBatteryBlockEntity battery) {
        battery.lastIn = battery.inThisTick;
        battery.lastOut = battery.outThisTick;
        battery.inThisTick = 0;
        battery.outThisTick = 0;
        if (level.getGameTime() % 10 == 0 && state.getValue(HomeBatteryBlock.CHARGE) != battery.chargeStep()) {
            battery.updateModel();
        }
    }

    /** Lit segments on the charge bar: any charge at all lights the first one. */
    private int chargeStep() {
        int capacity = capacity();
        if (energy <= 0 || capacity <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil((double) energy * HomeBatteryBlock.CHARGE_STEPS / capacity));
    }

    public boolean isFull() {
        return modules.size() >= MAX_MODULES;
    }

    /** Slots a module into the next free bay, bringing its charge with it. False if the stack is full. */
    public boolean addModule(ItemStack stack) {
        if (modules.size() >= MAX_MODULES || !(stack.getItem() instanceof BatteryModuleItem module)) {
            return false;
        }
        modules.add(module.chemistry());
        energy = Math.min(capacity(), energy + BatteryModuleItem.energy(stack));
        changed();
        return true;
    }

    /** Takes out the top module, with its fair share of the charge. Empty if there are none. */
    public ItemStack removeTopModule() {
        if (modules.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int total = capacity();
        BatteryChemistry chemistry = modules.removeLast();
        int share = total == 0 ? 0 : (int) ((long) energy * chemistry.capacity() / total);
        energy -= share;
        changed();
        return BatteryModuleItem.withEnergy(new ItemStack(itemFor(chemistry)), share);
    }

    /** Breaking the cabinet drops every module with its share of the charge. */
    public void dropModules(Level level, BlockPos pos) {
        while (!modules.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), removeTopModule());
        }
    }

    private static net.minecraft.world.item.Item itemFor(BatteryChemistry chemistry) {
        return switch (chemistry) {
            case LEAD_ACID, EMPTY -> ModItems.LEAD_ACID_MODULE.get();
        };
    }

    private void changed() {
        setChanged();
        updateModel();
    }

    /**
     * Shows the modules and the charge bar on the model: bays 1 to 3 on the lower half, 4 to 6 on
     * the upper, and the same charge step on both.
     */
    private void updateModel() {
        if (level == null || level.isClientSide) {
            return;
        }
        int charge = chargeStep();
        for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
            BlockPos pos = half == DoubleBlockHalf.LOWER ? worldPosition : worldPosition.above();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof HomeBatteryBlock)) {
                continue;
            }
            int first = half == DoubleBlockHalf.LOWER ? 0 : 3;
            for (int i = 0; i < 3; i++) {
                int bay = first + i;
                state = state.setValue(HomeBatteryBlock.SEGMENTS.get(i), bay < modules.size() ? modules.get(bay) : BatteryChemistry.EMPTY);
            }
            level.setBlock(pos, state.setValue(HomeBatteryBlock.CHARGE, charge), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ryzergen.home_battery");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new HomeBatteryMenu(containerId, data, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        modules.forEach(module -> list.add(StringTag.valueOf(module.getSerializedName())));
        tag.put("modules", list);
        tag.putInt("energy", energy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        modules.clear();
        for (Tag entry : tag.getList("modules", Tag.TAG_STRING)) {
            for (BatteryChemistry chemistry : BatteryChemistry.values()) {
                if (chemistry != BatteryChemistry.EMPTY && chemistry.getSerializedName().equals(entry.getAsString())) {
                    modules.add(chemistry);
                }
            }
        }
        energy = Math.min(tag.getInt("energy"), Math.max(0, capacity()));
    }
}
