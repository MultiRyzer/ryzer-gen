package com.ryzer.ryzergen.cable;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import com.ryzer.ryzergen.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

/**
 * A fluid or gas pipe. Each extract side drains up to the pipe's rate every tick and fills the other
 * tanks on the network in turn. A pipe only ever carries its own phase: the fluid pipe takes
 * liquids, the gas pipe gases, and from a tank holding both it picks out the one it carries.
 */
public class FluidPipeBlockEntity extends CableBlockEntity<IFluidHandler> {
    private final boolean gas;

    private FluidPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean gas) {
        super(type, pos, state);
        this.gas = gas;
    }

    public static FluidPipeBlockEntity liquid(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(ModBlockEntities.FLUID_PIPE.get(), pos, state, false);
    }

    public static FluidPipeBlockEntity gas(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(ModBlockEntities.GAS_PIPE.get(), pos, state, true);
    }

    @Override
    protected BlockCapability<IFluidHandler, @Nullable Direction> capability() {
        return Capabilities.FluidHandler.BLOCK;
    }

    @Override
    public CableKind kind() {
        return gas ? CableKind.GAS : CableKind.FLUID;
    }

    @Override
    protected int panelMax() {
        return rate();
    }

    private boolean carries(FluidStack stack) {
        return !stack.isEmpty() && ModFluids.isGas(stack) == gas;
    }

    /** What a block on {@code side} sees: fluid pushed in is passed along, nothing comes back out. */
    public @Nullable IFluidHandler fluidsFor(@Nullable Direction side) {
        if (side == null || side(side) != CableSide.CONNECTED) {
            return null;
        }
        BlockPos source = worldPosition.relative(side);
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int tank) {
                return rate();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return carries(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (!carries(resource)) {
                    return 0;
                }
                return distribute(resource.copyWithAmount(Math.min(resource.getAmount(), rate())), source, null, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidPipeBlockEntity pipe) {
        for (Direction dir : Direction.values()) {
            pipe.moved[dir.get3DDataValue()] = 0;
            if (state.getValue(CableBlock.SIDES.get(dir)) != CableSide.EXTRACT) {
                continue;
            }
            BlockPos sourcePos = pos.relative(dir);
            IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, sourcePos, dir.getOpposite());
            if (source != null) {
                pipe.moved[dir.get3DDataValue()] = pipe.pull(source, sourcePos);
            }
        }
    }

    /** Drains up to the rate of the first fluid in {@code source} this pipe carries. Returns mB moved. */
    private int pull(IFluidHandler source, BlockPos sourcePos) {
        for (int tank = 0; tank < source.getTanks(); tank++) {
            FluidStack inTank = source.getFluidInTank(tank);
            if (!carries(inTank)) {
                continue;
            }
            FluidStack offered = source.drain(inTank.copyWithAmount(rate()), FluidAction.SIMULATE);
            if (offered.isEmpty()) {
                continue;
            }
            int fits = distribute(offered, sourcePos, source, FluidAction.SIMULATE);
            if (fits <= 0) {
                continue;
            }
            FluidStack taken = source.drain(offered.copyWithAmount(fits), FluidAction.EXECUTE);
            return distribute(taken, sourcePos, source, FluidAction.EXECUTE);
        }
        return 0;
    }

    /**
     * Fills the network's tanks in turn, starting one further along than last time. Never back into
     * where it came from, found by position or by handler. Returns how much went in.
     */
    private int distribute(FluidStack stack, @Nullable BlockPos exceptPos, @Nullable IFluidHandler exceptHandler, FluidAction action) {
        if (stack.isEmpty() || !refreshTargets()) {
            return 0;
        }
        int sent = 0;
        int count = targets.size();
        for (int i = 0; i < count && sent < stack.getAmount(); i++) {
            Target<IFluidHandler> target = targets.get((roundRobin + i) % count);
            if (target.endpoint().pos().equals(exceptPos)) {
                continue;
            }
            IFluidHandler receiver = target.cache().getCapability();
            if (receiver != null && receiver != exceptHandler) {
                sent += receiver.fill(stack.copyWithAmount(stack.getAmount() - sent), action);
            }
        }
        if (action.execute() && count > 0) {
            roundRobin = (roundRobin + 1) % count;
        }
        return sent;
    }

    private int rate() {
        return Config.get(gas ? Config.GAS_PIPE_RATE : Config.FLUID_PIPE_RATE);
    }
}
