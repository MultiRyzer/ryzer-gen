package com.ryzer.ryzergen.storage;

import com.ryzer.ryzergen.registry.ModBlockEntities;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * What a waste cask holds: 16 stacks of fission waste and nothing else. Pipes and hoppers fill it
 * from any side (and can take waste back out, for later uses). The contents travel with the item
 * when it is broken, like a shulker box, so a full cask can be carried off and stored.
 */
public class WasteCaskBlockEntity extends BlockEntity {
    public static final int SLOTS = 16;
    public static final int CAPACITY = SLOTS * 64;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateGauge();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.FISSION_WASTE.get());
        }
    };

    public WasteCaskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WASTE_CASK.get(), pos, state);
    }

    public IItemHandler items() {
        return items;
    }

    public int stored() {
        int total = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            total += items.getStackInSlot(slot).getCount();
        }
        return total;
    }

    /** Lights the gauge on the side: one segment per quarter, any waste at all lights the first. */
    private void updateGauge() {
        if (level == null || level.isClientSide) {
            return;
        }
        int stored = stored();
        int fill = stored == 0 ? 0 : Math.min(WasteCaskBlock.FILL_STEPS, 1 + stored * WasteCaskBlock.FILL_STEPS / (CAPACITY + 1));
        BlockState state = getBlockState();
        if (state.getValue(WasteCaskBlock.FILL) != fill) {
            level.setBlock(worldPosition, state.setValue(WasteCaskBlock.FILL, fill), Block.UPDATE_ALL);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateGauge();
    }

    // The contents ride on the item as a container component, as a shulker box's do.
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < SLOTS; slot++) {
            list.set(slot, items.getStackInSlot(slot).copy());
        }
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(list);
        for (int slot = 0; slot < SLOTS; slot++) {
            items.setStackInSlot(slot, list.get(slot));
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("inventory");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("inventory"));
    }
}
