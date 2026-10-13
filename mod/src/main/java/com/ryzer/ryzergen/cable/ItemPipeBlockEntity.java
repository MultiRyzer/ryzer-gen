package com.ryzer.ryzergen.cable;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.Connection;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * An item pipe. Each extract side pulls a batch of items every so often (both in the config) and
 * hands them to the other inventories on the network, a different one first each time. Items that
 * no inventory will take stay where they are, and the pipe tries the next stack instead, so one
 * stuck item never blocks the rest. Items move instantly, as in Pipez.
 *
 * <p>Each side can carry a filter and a priority (set in the pipe's panel). A filter lists up to
 * {@link #FILTER_SLOTS} items and either lets only those through or all but those; empty, it lets
 * everything through. On an extract side it decides what is pulled; on a side joined to a block, what
 * may go into it. Deliveries go to the highest priority that will take them first, round-robin among
 * equals. Both are read only as items move, and a network with none set goes the old way.
 */
public class ItemPipeBlockEntity extends CableBlockEntity<IItemHandler> {
    /** Items one side's filter can list. */
    public static final int FILTER_SLOTS = 7;
    /** Priorities run from -MAX_PRIORITY to MAX_PRIORITY. */
    public static final int MAX_PRIORITY = 9;

    /** Ticks until each extract side pulls again: a fitting shortens the wait. */
    private final int[] cooldown = new int[6];
    /** Each side's filter (by 3D data value): what it lists, whether it lets only those through, and its priority. */
    private final ItemStack[][] filters = new ItemStack[6][FILTER_SLOTS];
    private final boolean[] only = new boolean[6];
    private final int[] priority = new int[6];

    {
        for (ItemStack[] filter : filters) {
            java.util.Arrays.fill(filter, ItemStack.EMPTY);
        }
    }

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_PIPE.get(), pos, state);
    }

    @Override
    protected BlockCapability<IItemHandler, @Nullable Direction> capability() {
        return Capabilities.ItemHandler.BLOCK;
    }

    @Override
    public CableKind kind() {
        return CableKind.ITEMS;
    }

    /** Items per second one extract side can move. */
    @Override
    protected int panelMax() {
        return batch() * 20 / interval();
    }

    /** Items per second, with this side's fitting (bigger batches, then shorter waits). */
    @Override
    protected int limit(Direction side) {
        int[] schedule = schedule(side);
        return (int) Math.min(Integer.MAX_VALUE, (long) schedule[0] * 20 / Math.max(1, schedule[1]));
    }

    @Override
    protected int ticksPerUnit() {
        return 20;
    }

    /** What a block on {@code side} sees: items pushed in are passed along, nothing comes back out. */
    public @Nullable IItemHandler itemsFor(@Nullable Direction side) {
        if (side == null || side(side) != CableSide.CONNECTED) {
            return null;
        }
        BlockPos source = worldPosition.relative(side);
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (stack.isEmpty()) {
                    return stack;
                }
                offered(side);
                int count = Math.min(stack.getCount(), schedule(side)[0]);
                ItemStack left = distribute(stack.copyWithCount(count), source, null, simulate);
                if (!simulate) {
                    addPushed(side, count - left.getCount());
                }
                return stack.copyWithCount(stack.getCount() - count + left.getCount());
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return true;
            }
        };
    }

    // ------------------------------------------------------------------ filters and priority

    public ItemStack filter(Direction side, int slot) {
        return filters[side.get3DDataValue()][slot];
    }

    /** Sets one of a side's filter entries (one of the item, or nothing). */
    public void setFilter(Direction side, int slot, ItemStack stack) {
        filters[side.get3DDataValue()][slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        settingsChanged();
    }

    /** True if the side lets only what it lists through, false if all but that. */
    public boolean onlyListed(Direction side) {
        return only[side.get3DDataValue()];
    }

    public void toggleOnly(Direction side) {
        only[side.get3DDataValue()] = !only[side.get3DDataValue()];
        settingsChanged();
    }

    public int priority(Direction side) {
        return priority[side.get3DDataValue()];
    }

    public void nudgePriority(Direction side, int by) {
        int i = side.get3DDataValue();
        priority[i] = Math.max(-MAX_PRIORITY, Math.min(MAX_PRIORITY, priority[i] + by));
        settingsChanged();
    }

    /** Whether the side has a filter that holds anything back (for the panel's mark). */
    public boolean filtered(Direction side) {
        int i = side.get3DDataValue();
        if (only[i]) {
            return true;
        }
        for (ItemStack entry : filters[i]) {
            if (!entry.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code stack} gets through the side's filter. */
    public boolean passes(Direction side, ItemStack stack) {
        int i = side.get3DDataValue();
        boolean listed = false;
        for (ItemStack entry : filters[i]) {
            if (!entry.isEmpty() && stack.is(entry.getItem())) {
                listed = true;
                break;
            }
        }
        return only[i] == listed;
    }

    private void settingsChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            // The open panel reads them from the pipe's copy on the client.
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The pipe that delivers to a target: the one next to the block, on the side facing it. */
    private @Nullable ItemPipeBlockEntity pipeOf(Target<IItemHandler> target) {
        if (level == null) {
            return null;
        }
        BlockPos at = target.endpoint().pos().relative(target.endpoint().side());
        return at.equals(worldPosition) ? this : level.getBlockEntity(at) instanceof ItemPipeBlockEntity pipe ? pipe : null;
    }

    private void saveSettings(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag sides = new CompoundTag();
        for (Direction dir : Direction.values()) {
            int i = dir.get3DDataValue();
            if (!filtered(dir) && priority[i] == 0) {
                continue;
            }
            CompoundTag side = new CompoundTag();
            ListTag list = new ListTag();
            for (int slot = 0; slot < FILTER_SLOTS; slot++) {
                CompoundTag entry = new CompoundTag();
                if (!filters[i][slot].isEmpty()) {
                    entry.put("item", filters[i][slot].save(registries));
                }
                list.add(entry);
            }
            side.put("items", list);
            side.putBoolean("only", only[i]);
            side.putInt("priority", priority[i]);
            sides.put(dir.getName(), side);
        }
        tag.put("filters", sides);
    }

    private void loadSettings(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag sides = tag.getCompound("filters");
        for (Direction dir : Direction.values()) {
            int i = dir.get3DDataValue();
            CompoundTag side = sides.getCompound(dir.getName());
            ListTag list = side.getList("items", Tag.TAG_COMPOUND);
            for (int slot = 0; slot < FILTER_SLOTS; slot++) {
                CompoundTag entry = slot < list.size() ? list.getCompound(slot) : new CompoundTag();
                filters[i][slot] = entry.contains("item") ? ItemStack.parseOptional(registries, entry.getCompound("item")) : ItemStack.EMPTY;
            }
            only[i] = side.getBoolean("only");
            priority[i] = side.getInt("priority");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveSettings(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadSettings(tag, registries);
    }

    /** Clients get the filters and priorities too, for the panel. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveSettings(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        loadSettings(tag, registries);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        loadSettings(packet.getTag(), registries);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemPipeBlockEntity pipe) {
        pipe.roll();
        for (Direction dir : CableNetwork.DIRECTIONS) {
            if (state.getValue(CableBlock.SIDES.get(dir)) != CableSide.EXTRACT || --pipe.cooldown[dir.get3DDataValue()] > 0) {
                continue;
            }
            int[] schedule = pipe.schedule(dir);
            pipe.cooldown[dir.get3DDataValue()] = schedule[1];
            BlockPos sourcePos = pos.relative(dir);
            IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK, sourcePos, dir.getOpposite());
            if (source != null) {
                pipe.addPushed(dir, pipe.pull(level, source, sourcePos, dir, schedule[0]));
            }
        }
    }

    /** Moves up to {@code batch} items out of {@code source}, slot by slot, those the side's filter passes. Returns how many moved. */
    private int pull(Level level, IItemHandler source, BlockPos sourcePos, Direction side, int batch) {
        int budget = batch;
        for (int slot = 0; slot < source.getSlots() && budget > 0; slot++) {
            ItemStack offered = source.extractItem(slot, budget, true);
            if (offered.isEmpty() || !passes(side, offered)) {
                continue;
            }
            int fits = offered.getCount() - distribute(offered, sourcePos, source, true).getCount();
            if (fits <= 0) {
                continue;
            }
            ItemStack taken = source.extractItem(slot, fits, false);
            ItemStack left = distribute(taken, sourcePos, source, false);
            if (!left.isEmpty()) {
                // A receiver changed its mind between the check and the move: put it back.
                left = ItemHandlerHelper.insertItemStacked(source, left, false);
                if (!left.isEmpty()) {
                    Block.popResource(level, worldPosition, left);
                }
            }
            budget -= taken.getCount() - left.getCount();
        }
        return batch - budget;
    }

    /**
     * Offers {@code stack} to each inventory on the network in turn, highest priority first and,
     * among equals, starting one further along than last time, and returns what nobody took. Each
     * side's filter decides what may go in there. Never back into where it came from, found by
     * position or, for a block that spans two positions, by its item handler.
     */
    private ItemStack distribute(ItemStack stack, @Nullable BlockPos exceptPos, @Nullable IItemHandler exceptHandler, boolean simulate) {
        if (stack.isEmpty() || !refreshTargets()) {
            return stack;
        }
        int count = targets.size();
        ItemPipeBlockEntity[] pipes = new ItemPipeBlockEntity[count];
        int[] ranks = new int[count];
        boolean ranked = false;
        for (int i = 0; i < count; i++) {
            pipes[i] = pipeOf(targets.get(i));
            ranks[i] = pipes[i] == null ? 0 : pipes[i].priority(targets.get(i).endpoint().side().getOpposite());
            ranked |= ranks[i] != 0;
        }
        Integer[] order = new Integer[count];
        for (int k = 0; k < count; k++) {
            order[k] = (roundRobin + k) % count;
        }
        if (ranked) {
            // A stable sort, so the round-robin order holds among equals.
            java.util.Arrays.sort(order, (a, b) -> Integer.compare(ranks[b], ranks[a]));
        }
        for (int k = 0; k < count && !stack.isEmpty(); k++) {
            Target<IItemHandler> target = targets.get(order[k]);
            if (target.endpoint().pos().equals(exceptPos)) {
                continue;
            }
            ItemPipeBlockEntity pipe = pipes[order[k]];
            if (pipe != null && !pipe.passes(target.endpoint().side().getOpposite(), stack)) {
                continue;
            }
            IItemHandler receiver = target.cache().getCapability();
            if (receiver != null && receiver != exceptHandler) {
                stack = ItemHandlerHelper.insertItemStacked(receiver, stack, simulate);
            }
        }
        if (!simulate && count > 0) {
            roundRobin = (roundRobin + 1) % count;
        }
        return stack;
    }

    private static int batch() {
        return Config.get(Config.ITEM_PIPE_BATCH);
    }

    private static int interval() {
        return Config.get(Config.ITEM_PIPE_INTERVAL);
    }

    /** {batch, interval} for what comes in on {@code side}, with its fitting. */
    private int[] schedule(Direction side) {
        return upgrade(side).itemSchedule(batch(), interval());
    }
}
