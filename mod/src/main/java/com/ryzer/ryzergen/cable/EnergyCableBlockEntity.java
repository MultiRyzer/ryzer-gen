package com.ryzer.ryzergen.cable;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One cable's state: which sides the wrench disconnected, and the network it delivers to. Cables
 * with an extract side pull energy each tick; every cable passes on energy pushed into it.
 *
 * <p>Energy storage on the network (anything that both accepts and gives energy, such as the home
 * battery) is a buffer, as in Mekanism: machines are served first and buffers take the surplus.
 * At the end of each tick the network tops machines up from its buffers, so one plain cable to a
 * battery both charges it and draws on it, with no extract side needed.
 */
public class EnergyCableBlockEntity extends BlockEntity implements MenuProvider {
    // Synced to the open cable panel: per side, its mode and the FE moved last tick (as two halves).
    public static final int DATA_PER_SIDE = 3;
    public static final int DATA_MAX_LOW = 18;
    public static final int DATA_MAX_HIGH = 19;
    public static final int DATA_RECEIVERS = 20;
    public static final int DATA_BUFFERS = 21;
    public static final int DATA_COUNT = 22;

    /** Network leaders with buffers, waiting to top up their machines at the end of this tick. */
    private static final Set<EnergyCableBlockEntity> PENDING_DRAIN = new LinkedHashSet<>();

    private final Set<Direction> disabled = EnumSet.noneOf(Direction.class);
    private final List<Target> targets = new ArrayList<>();
    private int networkVersion = -1;
    private boolean leader;
    private int roundRobin;
    /** FE pulled through each side last tick, for the panel. */
    private final int[] moved = new int[6];

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < 6 * DATA_PER_SIDE) {
                Direction side = Direction.from3DDataValue(index / DATA_PER_SIDE);
                return switch (index % DATA_PER_SIDE) {
                    case 0 -> getBlockState().getValue(EnergyCableBlock.SIDES.get(side)).ordinal();
                    case 1 -> moved[side.get3DDataValue()] & 0xFFFF;
                    default -> moved[side.get3DDataValue()] >>> 16;
                };
            }
            return switch (index) {
                case DATA_MAX_LOW -> rate() & 0xFFFF;
                case DATA_MAX_HIGH -> rate() >>> 16;
                case DATA_RECEIVERS -> count(false);
                case DATA_BUFFERS -> count(true);
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
                return distribute(Math.min(amount, rate()), source, null, simulate);
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
        if (cable.refreshTargets() && cable.leader) {
            PENDING_DRAIN.add(cable);
        }
        for (Direction dir : Direction.values()) {
            cable.moved[dir.get3DDataValue()] = 0;
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
                cable.moved[dir.get3DDataValue()] = source.extractEnergy(cable.distribute(available, sourcePos, source, false), false);
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
        for (Target target : targets) {
            IEnergyStorage buffer = target.cache().getCapability();
            if (buffer == null || !isBuffer(buffer)) {
                continue;
            }
            int available = buffer.extractEnergy(rate(), true);
            int wanted = available > 0 ? deliver(available, null, buffer, false, true) : 0;
            if (wanted > 0) {
                deliver(buffer.extractEnergy(wanted, false), null, buffer, false, false);
            }
        }
    }

    /** Storage rather than a machine: it both accepts energy and gives it back. */
    private static boolean isBuffer(IEnergyStorage storage) {
        return storage.canReceive() && storage.canExtract();
    }

    /** How many machine faces (or, with {@code buffers}, storage faces) this cable's network reaches. */
    private int count(boolean buffers) {
        if (!refreshTargets()) {
            return 0;
        }
        int count = 0;
        for (Target target : targets) {
            IEnergyStorage storage = target.cache().getCapability();
            if (storage != null && storage.canReceive() && isBuffer(storage) == buffers) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ryzergen.energy_cable");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new EnergyCableMenu(containerId, worldPosition, data, ContainerLevelAccess.create(level, worldPosition));
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
            Target target = targets.get((roundRobin + i) % count);
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

    /** Rebuilds the cached list of machines to deliver to if any cable changed. False on the client. */
    private boolean refreshTargets() {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        if (networkVersion != CableNetwork.version()) {
            targets.clear();
            CableNetwork.Scan scan = CableNetwork.scan(server, worldPosition);
            leader = scan.leader().equals(worldPosition);
            for (CableNetwork.Endpoint endpoint : scan.endpoints()) {
                targets.add(new Target(endpoint, BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, server,
                        endpoint.pos(), endpoint.side())));
            }
            networkVersion = CableNetwork.version();
        }
        return true;
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
