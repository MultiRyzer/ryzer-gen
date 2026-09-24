package com.ryzer.ryzergen.cable;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * What every cable and pipe keeps: which sides the wrench disconnected, the network it delivers to
 * (cached until any cable changes), what each side moved this tick for the panel, and the panel
 * itself. Subclasses decide what moves and how.
 *
 * @param <C> the capability carried, such as an energy storage or an item handler
 */
public abstract class CableBlockEntity<C> extends BlockEntity implements MenuProvider {
    // Synced to the open panel: per side, its mode and what it moved this tick (as two halves).
    public static final int DATA_PER_SIDE = 3;
    public static final int DATA_MAX_LOW = 18;
    public static final int DATA_MAX_HIGH = 19;
    public static final int DATA_RECEIVERS = 20;
    public static final int DATA_BUFFERS = 21;
    public static final int DATA_COUNT = 22;

    private final Set<Direction> disabled = EnumSet.noneOf(Direction.class);
    protected final List<Target<C>> targets = new ArrayList<>();
    private int networkVersion = -1;
    /** One cable picked per network, for work that must run once per tick. */
    protected boolean leader;
    protected int roundRobin;
    /** What each side pulled this tick, for the panel. */
    protected final int[] moved = new int[6];

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < 6 * DATA_PER_SIDE) {
                Direction side = Direction.from3DDataValue(index / DATA_PER_SIDE);
                return switch (index % DATA_PER_SIDE) {
                    case 0 -> getBlockState().getValue(CableBlock.SIDES.get(side)).ordinal();
                    case 1 -> moved[side.get3DDataValue()] & 0xFFFF;
                    default -> moved[side.get3DDataValue()] >>> 16;
                };
            }
            return switch (index) {
                case DATA_MAX_LOW -> panelMax() & 0xFFFF;
                case DATA_MAX_HIGH -> panelMax() >>> 16;
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

    protected record Target<C>(CableNetwork.Endpoint endpoint, BlockCapabilityCache<C, @Nullable Direction> cache) {}

    protected CableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The capability this cable carries, looked up on the blocks it delivers to. */
    protected abstract BlockCapability<C, @Nullable Direction> capability();

    /** Which panel to show. */
    public abstract CableKind kind();

    /** The limit per extract side, in the panel's unit (FE per tick, items per second). */
    protected abstract int panelMax();

    /** How many receivers (or, with {@code buffers}, storage blocks) the network reaches. */
    protected int count(boolean buffers) {
        if (buffers || !refreshTargets()) {
            return 0;
        }
        int count = 0;
        for (Target<C> target : targets) {
            if (target.cache().getCapability() != null) {
                count++;
            }
        }
        return count;
    }

    public boolean isDisabled(Direction side) {
        return disabled.contains(side);
    }

    public void setDisabled(Direction side, boolean off) {
        if (off ? disabled.add(side) : disabled.remove(side)) {
            setChanged();
        }
    }

    protected CableSide side(Direction dir) {
        return getBlockState().getValue(CableBlock.SIDES.get(dir));
    }

    /** Rebuilds the cached list of blocks to deliver to if any cable changed. False on the client. */
    protected boolean refreshTargets() {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        if (networkVersion != CableNetwork.version()) {
            targets.clear();
            CableNetwork.Scan scan = CableNetwork.scan(server, worldPosition);
            leader = scan.leader().equals(worldPosition);
            for (CableNetwork.Endpoint endpoint : scan.endpoints()) {
                targets.add(new Target<>(endpoint, BlockCapabilityCache.create(capability(), server, endpoint.pos(), endpoint.side())));
            }
            networkVersion = CableNetwork.version();
        }
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CableMenu(kind(), containerId, worldPosition, data, ContainerLevelAccess.create(level, worldPosition));
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
