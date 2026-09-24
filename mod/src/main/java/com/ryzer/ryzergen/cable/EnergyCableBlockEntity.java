package com.ryzer.ryzergen.cable;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * One cable's state: which sides the wrench disconnected, and the network it delivers to. Cables
 * with an extract side pull energy each tick; every cable passes on energy pushed into it.
 */
public class EnergyCableBlockEntity extends BlockEntity {
    private final Set<Direction> disabled = EnumSet.noneOf(Direction.class);
    private final List<Target> targets = new ArrayList<>();
    private int networkVersion = -1;
    private int roundRobin;

    private record Target(CableNetwork.Endpoint endpoint, BlockCapabilityCache<IEnergyStorage, @Nullable Direction> cache) {}

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE.get(), pos, state);
    }

    public boolean isDisabled(Direction side) {
        return disabled.contains(side);
    }

    public void setDisabled(Direction side, boolean off) {
        if (off ? disabled.add(side) : disabled.remove(side)) {
            setChanged();
        }
    }

    /** What a block on {@code side} sees: pushed energy goes into the network, never back out. */
    public @Nullable IEnergyStorage energyFor(@Nullable Direction side) {
        if (side == null || getBlockState().getValue(EnergyCableBlock.SIDES.get(side)) != CableSide.CONNECTED) {
            return null;
        }
        BlockPos source = worldPosition.relative(side);
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int amount, boolean simulate) {
                return distribute(Math.min(amount, rate()), source, simulate);
            }

            @Override
            public int extractEnergy(int amount, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return 0;
            }

            @Override
            public int getMaxEnergyStored() {
                return rate();
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyCableBlockEntity cable) {
        for (Direction dir : Direction.values()) {
            if (state.getValue(EnergyCableBlock.SIDES.get(dir)) != CableSide.EXTRACT) {
                continue;
            }
            BlockPos sourcePos = pos.relative(dir);
            IEnergyStorage source = level.getCapability(Capabilities.EnergyStorage.BLOCK, sourcePos, dir.getOpposite());
            if (source == null || !source.canExtract()) {
                continue;
            }
            int available = source.extractEnergy(rate(), true);
            if (available > 0) {
                source.extractEnergy(cable.distribute(available, sourcePos, false), false);
            }
        }
    }

    /** Shares {@code amount} across the network's machines, starting somewhere new each time. */
    private int distribute(int amount, BlockPos except, boolean simulate) {
        if (!(level instanceof ServerLevel server) || amount <= 0) {
            return 0;
        }
        if (networkVersion != CableNetwork.version()) {
            targets.clear();
            for (CableNetwork.Endpoint endpoint : CableNetwork.endpoints(server, worldPosition)) {
                targets.add(new Target(endpoint, BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, server,
                        endpoint.pos(), endpoint.side())));
            }
            networkVersion = CableNetwork.version();
        }
        int sent = 0;
        int count = targets.size();
        for (int i = 0; i < count && sent < amount; i++) {
            Target target = targets.get((roundRobin + i) % count);
            if (target.endpoint().pos().equals(except)) {
                continue;
            }
            IEnergyStorage receiver = target.cache().getCapability();
            if (receiver != null && receiver.canReceive()) {
                sent += receiver.receiveEnergy(amount - sent, simulate);
            }
        }
        if (!simulate && count > 0) {
            roundRobin = (roundRobin + 1) % count;
        }
        return sent;
    }

    private static int rate() {
        return Config.get(Config.CABLE_RATE);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        int mask = 0;
        for (Direction dir : disabled) {
            mask |= 1 << dir.ordinal();
        }
        tag.putInt("disabled", mask);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        disabled.clear();
        int mask = tag.getInt("disabled");
        for (Direction dir : Direction.values()) {
            if ((mask & 1 << dir.ordinal()) != 0) {
                disabled.add(dir);
            }
        }
    }
}
