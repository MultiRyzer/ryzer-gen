package com.ryzer.ryzergen.creative;

import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** The creative battery's or a creative tank's workings: never runs out, swallows whatever comes in. */
public class CreativeSourceBlockEntity extends BlockEntity {
    /** FE per tick pushed into each neighbour, and mB of fluid. */
    private static final int ENERGY_PUSH = 1_000_000;
    private static final int FLUID_PUSH = 16_000;

    /** Gives any amount and takes any amount (and voids it), so cable networks treat it as a battery. */
    private static final IEnergyStorage ENERGY = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            return amount;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return amount;
        }

        @Override
        public int getEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    /** A tank always full of its fluid, which swallows whatever is poured in. */
    private record Endless(Fluid fluid) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return new FluidStack(fluid, Integer.MAX_VALUE);
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return resource.getAmount();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return resource.is(fluid) ? resource.copy() : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return new FluidStack(fluid, maxDrain);
        }
    }

    private @Nullable Endless tank;

    public CreativeSourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_SOURCE.get(), pos, state);
    }

    /** The fluid this block gives, or null for the battery. */
    private @Nullable Fluid fluid() {
        return getBlockState().getBlock() instanceof CreativeSourceBlock block ? block.fluid() : null;
    }

    public @Nullable IEnergyStorage energyFor(@Nullable Direction side) {
        return fluid() == null ? ENERGY : null;
    }

    public @Nullable IFluidHandler fluidFor(@Nullable Direction side) {
        Fluid fluid = fluid();
        if (fluid == null) {
            return null;
        }
        if (tank == null || tank.fluid() != fluid) {
            tank = new Endless(fluid);
        }
        return tank;
    }

    /** Pushes into every neighbour that takes it, so plain (non-extracting) pipes and cables work too. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, CreativeSourceBlockEntity source) {
        Fluid fluid = source.fluid();
        for (Direction dir : Direction.values()) {
            BlockPos next = pos.relative(dir);
            if (fluid != null) {
                IFluidHandler receiver = level.getCapability(Capabilities.FluidHandler.BLOCK, next, dir.getOpposite());
                if (receiver != null) {
                    receiver.fill(new FluidStack(fluid, FLUID_PUSH), IFluidHandler.FluidAction.EXECUTE);
                }
            } else {
                IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, next, dir.getOpposite());
                if (receiver != null && receiver.canReceive()) {
                    receiver.receiveEnergy(ENERGY_PUSH, false);
                }
            }
        }
    }
}
