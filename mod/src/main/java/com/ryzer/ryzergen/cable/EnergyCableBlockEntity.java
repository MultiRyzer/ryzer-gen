package com.ryzer.ryzergen.cable;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An energy cable. Cables with an extract side pull energy each tick; every cable passes on energy
 * pushed into it.
 *
 * <p>Energy storage on the network (anything that both accepts and gives energy, such as the home
 * battery) is a buffer, as in Mekanism: machines are served first and buffers take the surplus.
 * At the end of each tick the network tops machines up from its buffers, so one plain cable to a
 * battery both charges it and draws on it, with no extract side needed.
 */
public class EnergyCableBlockEntity extends CableBlockEntity<IEnergyStorage> {
    /** Network leaders with buffers, waiting to top up their machines at the end of this tick. */
    private static final Set<EnergyCableBlockEntity> PENDING_DRAIN = new LinkedHashSet<>();

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE.get(), pos, state);
    }

    @Override
    protected BlockCapability<IEnergyStorage, @Nullable Direction> capability() {
        return Capabilities.EnergyStorage.BLOCK;
    }

    @Override
    public CableKind kind() {
        return CableKind.ENERGY;
    }

    @Override
    protected int panelMax() {
        return rate();
    }

    /** What a block on {@code side} sees: pushed energy goes into the network, never back out. */
    public @Nullable IEnergyStorage energyFor(@Nullable Direction side) {
        if (side == null || side(side) != CableSide.CONNECTED) {
            return null;
        }
        BlockPos source = worldPosition.relative(side);
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int amount, boolean simulate) {
                int sent = distribute(Math.min(amount, rate(side)), source, null, simulate);
                if (!simulate) {
                    pushed[side.get3DDataValue()] += sent;
                }
                return sent;
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
                return rate(side);
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
        if (cable.refreshTargets() && cable.leader) {
            PENDING_DRAIN.add(cable);
        }
        cable.startTick();
        for (Direction dir : Direction.values()) {
            if (state.getValue(CableBlock.SIDES.get(dir)) != CableSide.EXTRACT) {
                continue;
            }
            BlockPos sourcePos = pos.relative(dir);
            IEnergyStorage source = level.getCapability(Capabilities.EnergyStorage.BLOCK, sourcePos, dir.getOpposite());
            if (source == null || !source.canExtract()) {
                continue;
            }
            int available = source.extractEnergy(cable.rate(dir), true);
            if (available > 0) {
                cable.moved[dir.get3DDataValue()] += source.extractEnergy(cable.distribute(available, sourcePos, source, false), false);
            }
        }
    }

    /**
     * Runs after every block entity has ticked, so machines have already had this tick's generation:
     * whatever they still want comes out of the network's buffers.
     */
    public static void drainBuffers(Level level) {
        var it = PENDING_DRAIN.iterator();
        while (it.hasNext()) {
            EnergyCableBlockEntity cable = it.next();
            if (cable.level != level) {
                continue;
            }
            it.remove();
            if (!cable.isRemoved() && cable.refreshTargets()) {
                cable.drain();
            }
        }
    }

    private void drain() {
        for (Target<IEnergyStorage> target : targets) {
            IEnergyStorage buffer = target.cache().getCapability();
            if (buffer == null || !isBuffer(buffer)) {
                continue;
            }
            int available = buffer.extractEnergy(drawRate(target.endpoint()), true);
            int wanted = available > 0 ? deliver(available, null, buffer, false, true) : 0;
            if (wanted > 0) {
                int drawn = buffer.extractEnergy(wanted, false);
                deliver(drawn, null, buffer, false, false);
                countDraw(target.endpoint(), drawn);
            }
        }
    }

    /** The network draws on storage without an extract side, so a battery's side feeds it. */
    @Override
    protected boolean canGive(Direction side) {
        if (level == null) {
            return false;
        }
        IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(side), side.getOpposite());
        return storage != null && storage.canExtract();
    }

    /**
     * The most the network may draw from a battery this tick: the fitting on the side of the cable
     * that touches it, like any other input. The battery's own rate limits it too.
     */
    private int drawRate(CableNetwork.Endpoint endpoint) {
        BlockPos cablePos = endpoint.pos().relative(endpoint.side());
        if (level != null && level.getBlockEntity(cablePos) instanceof CableBlockEntity<?> cable) {
            return cable.upgrade(endpoint.side().getOpposite()).scale(rate());
        }
        return rate();
    }

    /** Counts a battery's draw as coming in on the side of the cable that touches it, for that cable's panel. */
    private void countDraw(CableNetwork.Endpoint endpoint, int amount) {
        BlockPos cablePos = endpoint.pos().relative(endpoint.side());
        if (amount > 0 && level != null && level.getBlockEntity(cablePos) instanceof CableBlockEntity<?> cable) {
            cable.pushed[endpoint.side().getOpposite().get3DDataValue()] += amount;
        }
    }

    /** Storage rather than a machine: it both accepts energy and gives it back. */
    private static boolean isBuffer(IEnergyStorage storage) {
        return storage.canReceive() && storage.canExtract();
    }

    /** How many machine faces (or, with {@code buffers}, storage faces) this cable's network reaches. */
    @Override
    protected int count(boolean buffers) {
        if (!refreshTargets()) {
            return 0;
        }
        int count = 0;
        for (Target<IEnergyStorage> target : targets) {
            IEnergyStorage storage = target.cache().getCapability();
            if (storage != null && storage.canReceive() && isBuffer(storage) == buffers) {
                count++;
            }
        }
        return count;
    }

    /**
     * Shares {@code amount} across the network: machines first, then buffers with what is left. Never
     * back into where it came from, found by position ({@code exceptPos}) or, for a source that
     * spans two blocks like the battery cabinet, by its storage ({@code exceptStorage}).
     */
    private int distribute(int amount, @Nullable BlockPos exceptPos, @Nullable IEnergyStorage exceptStorage, boolean simulate) {
        if (amount <= 0 || !refreshTargets()) {
            return 0;
        }
        int sent = deliver(amount, exceptPos, exceptStorage, false, simulate);
        sent += deliver(amount - sent, exceptPos, exceptStorage, true, simulate);
        if (!simulate && !targets.isEmpty()) {
            roundRobin = (roundRobin + 1) % targets.size();
        }
        return sent;
    }

    /** One pass over either the machines or the buffers, starting at the round-robin position. */
    private int deliver(int amount, @Nullable BlockPos exceptPos, @Nullable IEnergyStorage exceptStorage, boolean buffers, boolean simulate) {
        int sent = 0;
        int count = targets.size();
        for (int i = 0; i < count && sent < amount; i++) {
            Target<IEnergyStorage> target = targets.get((roundRobin + i) % count);
            if (target.endpoint().pos().equals(exceptPos)) {
                continue;
            }
            IEnergyStorage receiver = target.cache().getCapability();
            if (receiver != null && receiver != exceptStorage && receiver.canReceive() && isBuffer(receiver) == buffers) {
                sent += receiver.receiveEnergy(amount - sent, simulate);
            }
        }
        return sent;
    }

    /** The base limit per input, before fittings. */
    private static int rate() {
        return Config.get(Config.CABLE_RATE);
    }

    /** The limit for what comes in on {@code side}, with its fitting. */
    private int rate(Direction side) {
        return upgrade(side).scale(rate());
    }
}
