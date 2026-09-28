package com.ryzer.ryzergen.cable;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 */
public class ItemPipeBlockEntity extends CableBlockEntity<IItemHandler> {
    /** Ticks until each extract side pulls again: a fitting shortens the wait. */
    private final int[] cooldown = new int[6];

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
                pipe.addPushed(dir, pipe.pull(level, source, sourcePos, schedule[0]));
            }
        }
    }

    /** Moves up to {@code batch} items out of {@code source}, slot by slot. Returns how many moved. */
    private int pull(Level level, IItemHandler source, BlockPos sourcePos, int batch) {
        int budget = batch;
        for (int slot = 0; slot < source.getSlots() && budget > 0; slot++) {
            ItemStack offered = source.extractItem(slot, budget, true);
            if (offered.isEmpty()) {
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
     * Offers {@code stack} to each inventory on the network in turn, starting one further along than
     * last time, and returns what nobody took. Never back into where it came from, found by position
     * or, for a block that spans two positions, by its item handler.
     */
    private ItemStack distribute(ItemStack stack, @Nullable BlockPos exceptPos, @Nullable IItemHandler exceptHandler, boolean simulate) {
        if (stack.isEmpty() || !refreshTargets()) {
            return stack;
        }
        int count = targets.size();
        for (int i = 0; i < count && !stack.isEmpty(); i++) {
            Target<IItemHandler> target = targets.get((roundRobin + i) % count);
            if (target.endpoint().pos().equals(exceptPos)) {
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
