package com.ryzer.ryzergen.machine.pump;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.machine.MachineEnergyStorage;
import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * The intake pump's workings: a small energy buffer, a water tank that fills while it has water
 * and power, and a push out of the port on top every tick, the way the microreactor pushes power.
 */
public class IntakePumpBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ENERGY_CAPACITY = 10_000;
    public static final int MAX_INPUT = 200;
    public static final int WATER_CAPACITY = 8_000;

    public enum Status { PUMPING, NO_WATER, NO_POWER, FULL, REDSTONE }

    public static final int DATA_ENERGY_LOW = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_WATER = 2;
    public static final int DATA_WATER_BELOW = 3;
    public static final int DATA_STATUS = 4;
    public static final int DATA_REDSTONE = 5;
    public static final int DATA_COUNT = 6;

    private final MachineEnergyStorage energy = new MachineEnergyStorage(ENERGY_CAPACITY, MAX_INPUT, this::setChanged);
    private final FluidTank water = new FluidTank(WATER_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    /** What pipes see at the water port: they can drain it but never fill it. */
    private final IFluidHandler waterOutput = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return water.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return WATER_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return water.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return water.drain(maxDrain, action);
        }
    };

    private RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    private Status status = Status.NO_WATER;
    private boolean waterBelow;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY_LOW -> energy.getEnergyStored() & 0xFFFF;
                case DATA_ENERGY_HIGH -> energy.getEnergyStored() >>> 16;
                case DATA_WATER -> water.getFluidAmount();
                case DATA_WATER_BELOW -> waterBelow ? 1 : 0;
                case DATA_STATUS -> status.ordinal();
                case DATA_REDSTONE -> redstoneMode.ordinal();
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

    public IntakePumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INTAKE_PUMP.get(), pos, state);
    }

    /** Energy goes in on any side. */
    public @Nullable IEnergyStorage energyFor(@Nullable Direction side) {
        return energy;
    }

    /** Water can be drawn from any side (it still pushes out of the top on its own). */
    public @Nullable IFluidHandler waterFor(@Nullable Direction side) {
        return waterOutput;
    }

    public void cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IntakePumpBlockEntity pump) {
        if (level.getGameTime() % 20 == 0) {
            pump.waterBelow = level.getFluidState(pos.below()).isSourceOfType(Fluids.WATER);
        }
        int rate = Config.get(Config.PUMP_RATE);
        int cost = Config.get(Config.PUMP_ENERGY);
        if (!pump.redstoneMode.allows(level.hasNeighborSignal(pos))) {
            pump.status = Status.REDSTONE;
        } else if (!pump.waterBelow) {
            pump.status = Status.NO_WATER;
        } else if (pump.water.getSpace() <= 0) {
            pump.status = Status.FULL;
        } else if (!pump.energy.consume(cost)) {
            pump.status = Status.NO_POWER;
        } else {
            pump.water.fill(new FluidStack(Fluids.WATER, Math.min(rate, pump.water.getSpace())), IFluidHandler.FluidAction.EXECUTE);
            pump.status = Status.PUMPING;
        }
        pump.push(level, pos);
        boolean running = pump.status == Status.PUMPING;
        if (running != state.getValue(IntakePumpBlock.RUNNING)) {
            level.setBlock(pos, state.setValue(IntakePumpBlock.RUNNING, running), Block.UPDATE_CLIENTS);
        }
    }

    private void push(Level level, BlockPos pos) {
        if (water.isEmpty()) {
            return;
        }
        IFluidHandler receiver = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.above(), Direction.DOWN);
        if (receiver != null) {
            water.drain(receiver.fill(water.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new IntakePumpMenu(containerId, data, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy.getEnergyStored());
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.putInt("redstone_mode", redstoneMode.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setStored(tag.getInt("energy"));
        water.readFromNBT(registries, tag.getCompound("water"));
        redstoneMode = RedstoneMode.byId(tag.getInt("redstone_mode"));
    }
}
