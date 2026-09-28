package com.ryzer.ryzergen.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * What every cable and pipe keeps: which sides the wrench disconnected, the fitting on each side,
 * the network it belongs to (shared by all its cables, see {@link CableNetwork}), what each side
 * moved lately for the panel, and the panel itself. Subclasses decide what moves and how. Only
 * cables with an extract side tick; the rest only act when something is pushed into them.
 *
 * <p>Fittings (see {@link CableUpgrade}) raise the limit for what comes in on their side, pulled by
 * an extract side or pushed in by a generator. They sync to clients, and the cable's model adds
 * each one to the side it is on (see {@link #FITTINGS}).
 *
 * @param <C> the capability carried, such as an energy storage or an item handler
 */
public abstract class CableBlockEntity<C> extends BlockEntity implements MenuProvider {
    // Synced to the open panel: per side, its mode and fitting, and what it moved this tick (as two halves).
    public static final int DATA_PER_SIDE = 3;
    public static final int DATA_MAX_LOW = 18;
    public static final int DATA_MAX_HIGH = 19;
    public static final int DATA_RECEIVERS = 20;
    public static final int DATA_BUFFERS = 21;
    public static final int DATA_COUNT = 22;

    /** The fitting on each side (by 3D data value), for the cable's model. */
    public static final ModelProperty<CableUpgrade[]> FITTINGS = new ModelProperty<>();

    private final Set<Direction> disabled = EnumSet.noneOf(Direction.class);
    private final ItemStack[] fittings = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    /** What the network delivers to: its shared list, so every cable reads the same one. */
    protected List<Target<C>> targets = List.of();
    private CableNetwork.@Nullable Net net;
    protected int roundRobin;
    /** What each side moved last tick, for the panel. */
    protected final int[] moved = new int[6];
    /** What was pushed in on each side this tick, so far. Becomes {@link #moved} when the next tick is first touched. */
    protected final int[] pushed = new int[6];
    /** The game tick {@link #pushed} is counting, so the panel's numbers roll over without ticking every cable. */
    private long countingTick = Long.MIN_VALUE;
    /** When something last came in on each side, so the panel knows which sides feed the network. */
    private final long[] lastIn = new long[6];

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            roll();
            if (index < 6 * DATA_PER_SIDE) {
                Direction side = Direction.from3DDataValue(index / DATA_PER_SIDE);
                return switch (index % DATA_PER_SIDE) {
                    case 0 -> getBlockState().getValue(CableBlock.SIDES.get(side)).ordinal() | upgrade(side).ordinal() << 2;
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

    /** The base limit per input, before fittings, in the panel's unit (FE per tick, items per second). */
    protected abstract int panelMax();

    /** How many receivers (or, with {@code buffers}, storage blocks) the network reaches. */
    protected int count(boolean buffers) {
        if (buffers || !refreshTargets()) {
            return 0;
        }
        int count = 0;
        for (Target<C> target : targets) {
            C capability = target.cache().getCapability();
            if (capability != null && receives(capability)) {
                count++;
            }
        }
        return count;
    }

    /** Whether a block the network reaches can take anything, so the panel counts it. */
    protected boolean receives(C capability) {
        return true;
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

    // ------------------------------------------------------------------ fittings

    public CableUpgrade upgrade(Direction side) {
        return CableUpgrade.of(fittings[side.get3DDataValue()]);
    }

    public ItemStack fitting(Direction side) {
        return fittings[side.get3DDataValue()];
    }

    public void setFitting(Direction side, ItemStack stack) {
        CableUpgrade before = upgrade(side);
        fittings[side.get3DDataValue()] = stack;
        setChanged();
        if (before != upgrade(side) && level != null && !level.isClientSide) {
            // Nearby players see the fitting change.
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Whether the block on {@code side} feeds the network, so a fitting there does something: an
     * extract side, a block that pushes in (lately), or one the network can draw on (see
     * {@link #canGive}). A side that already has a fitting counts too, so it can be taken out.
     */
    public boolean feeds(Direction side) {
        roll();
        int i = side.get3DDataValue();
        if (side(side) == CableSide.EXTRACT || !fittings[i].isEmpty()) {
            return true;
        }
        if (level != null && lastIn[i] != 0 && level.getGameTime() - lastIn[i] < 200) {
            return true;
        }
        return canGive(side);
    }

    /** Whether the network can draw on the block on {@code side} without an extract side (energy storage). */
    protected boolean canGive(Direction side) {
        return false;
    }

    /** Sides the panel lists: every side joined to a block other than a cable. Those that feed get a fitting slot. */
    public List<Direction> panelSides() {
        List<Direction> sides = new ArrayList<>();
        if (level == null) {
            return sides;
        }
        for (Direction dir : Direction.values()) {
            if (side(dir) != CableSide.NONE && !(level.getBlockState(worldPosition.relative(dir)).getBlock() instanceof CableBlock)) {
                sides.add(dir);
            }
        }
        return sides;
    }

    /** The fittings as a six-slot container for the panel, one slot per side by 3D data value. */
    public Container fittingContainer() {
        return new Container() {
            @Override
            public int getContainerSize() {
                return 6;
            }

            @Override
            public boolean isEmpty() {
                for (ItemStack stack : fittings) {
                    if (!stack.isEmpty()) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public ItemStack getItem(int slot) {
                return fittings[slot];
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                ItemStack taken = fittings[slot];
                if (!taken.isEmpty() && amount > 0) {
                    setFitting(Direction.from3DDataValue(slot), ItemStack.EMPTY);
                }
                return taken;
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                return removeItem(slot, 1);
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                setFitting(Direction.from3DDataValue(slot), stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return CableUpgrade.of(stack) != CableUpgrade.NONE;
            }

            @Override
            public void setChanged() {
                CableBlockEntity.this.setChanged();
            }

            @Override
            public boolean stillValid(Player player) {
                return !isRemoved();
            }

            @Override
            public void clearContent() {
                for (Direction dir : Direction.values()) {
                    setFitting(dir, ItemStack.EMPTY);
                }
            }
        };
    }

    public void dropFittings(Level level, BlockPos pos) {
        for (ItemStack stack : fittings) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    /** Called as each tick starts: last tick's pushes become what the panel shows for those sides. */
    /**
     * Moves what was pushed in on the tick being counted into {@link #moved}, once the game has moved
     * on: last tick's count if that was the previous tick, nothing if the cable sat idle in between.
     */
    protected void roll() {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (now == countingTick) {
            return;
        }
        boolean previous = now == countingTick + 1;
        for (int i = 0; i < 6; i++) {
            if (pushed[i] > 0) {
                lastIn[i] = countingTick;
            }
            moved[i] = previous ? pushed[i] : 0;
            pushed[i] = 0;
        }
        countingTick = now;
    }

    /**
     * Counts {@code amount} coming in on {@code side} this tick, for the panel: pushed in by a
     * neighbour or pulled by an extract side alike. Never write to {@link #moved} directly: open
     * panels update before block entities tick, so {@link #roll()} would clear it before it is read.
     */
    protected void addPushed(Direction side, int amount) {
        roll();
        pushed[side.get3DDataValue()] += amount;
    }

    @Override
    public ModelData getModelData() {
        CableUpgrade[] tiers = new CableUpgrade[6];
        for (Direction dir : CableNetwork.DIRECTIONS) {
            tiers[dir.get3DDataValue()] = upgrade(dir);
        }
        return ModelData.builder().with(FITTINGS, tiers).build();
    }

    // ------------------------------------------------------------------ network

    /** Picks up the network's shared list of blocks to deliver to, after any change to it. False on the client. */
    protected boolean refreshTargets() {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        if (net == null || !net.valid()) {
            net = CableNetwork.get(server, worldPosition);
            targets = targetsOf(server, net, capability());
        }
        return true;
    }

    /** A network's shared delivery list, built the first time any of its cables needs it. */
    @SuppressWarnings("unchecked")
    protected static <C> List<Target<C>> targetsOf(ServerLevel level, CableNetwork.Net net, BlockCapability<C, @Nullable Direction> capability) {
        if (net.targets == null) {
            List<Target<C>> list = new ArrayList<>(net.endpoints().size());
            for (CableNetwork.Endpoint endpoint : net.endpoints()) {
                list.add(new Target<>(endpoint, BlockCapabilityCache.create(capability, level, endpoint.pos(), endpoint.side())));
            }
            net.targets = List.copyOf(list);
        }
        return (List<Target<C>>) net.targets;
    }

    /** A cable coming into the world (placed, or its chunk loading) changes the networks next to it. */
    @Override
    public void onLoad() {
        super.onLoad();
        CableNetwork.changed(level, worldPosition);
    }

    /** So does its chunk unloading: the networks it was part of end at the unloaded edge. */
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        CableNetwork.changed(level, worldPosition);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        List<Direction> sides = panelSides();
        return new CableMenu(kind(), containerId, inventory, worldPosition, sides, sides.stream().filter(this::feeds).toList(),
                fittingContainer(), data, ContainerLevelAccess.create(level, worldPosition));
    }

    // ------------------------------------------------------------------ saving and syncing

    private void saveFittings(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag list = new CompoundTag();
        for (Direction dir : Direction.values()) {
            ItemStack stack = fitting(dir);
            if (!stack.isEmpty()) {
                list.put(dir.getName(), stack.save(registries));
            }
        }
        tag.put("fittings", list);
    }

    private void loadFittings(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag list = tag.getCompound("fittings");
        for (Direction dir : Direction.values()) {
            fittings[dir.get3DDataValue()] = list.contains(dir.getName())
                    ? ItemStack.parseOptional(registries, list.getCompound(dir.getName())) : ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        int mask = 0;
        for (Direction dir : disabled) {
            mask |= 1 << dir.ordinal();
        }
        tag.putInt("disabled", mask);
        saveFittings(tag, registries);
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
        loadFittings(tag, registries);
    }

    /** Clients only need the fittings, for the model. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveFittings(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadFittings(tag, registries);
        refreshModel();
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        loadFittings(packet.getTag(), registries);
        refreshModel();
    }

    /** On the client: rebuild the chunk so the model shows the new fittings. */
    private void refreshModel() {
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }
}
