package com.ryzer.ryzergen.machine.alloysmelter;

import com.ryzer.ryzergen.recipe.AlloyingRecipe;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import com.ryzer.ryzergen.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * Burns furnace fuel to cook two inputs into one output. Pipes connect like a furnace:
 * inputs from the top, fuel from the sides, output from the bottom.
 */
public class AlloySmelterBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_A = 0;
    public static final int INPUT_B = 1;
    public static final int FUEL = 2;
    public static final int OUTPUT = 3;
    public static final int SLOTS = 4;
    public static final int DATA_COUNT = 4;

    private final ItemStackHandler items = createItems(this::setChanged);
    private final IItemHandler inputs = new RangedWrapper(items, INPUT_A, INPUT_B + 1);
    private final IItemHandler fuel = new RangedWrapper(items, FUEL, FUEL + 1);
    private final IItemHandler output = new RangedWrapper(items, OUTPUT, OUTPUT + 1);

    private int litTime;
    private int litDuration;
    private int cookProgress;
    private int cookTotal;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> cookProgress;
                case 3 -> cookTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> litTime = value;
                case 1 -> litDuration = value;
                case 2 -> cookProgress = value;
                case 3 -> cookTotal = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_SMELTER.get(), pos, state);
    }

    /** Shared with the client-side menu so slot rules match on both sides. */
    public static ItemStackHandler createItems(Runnable onChanged) {
        return new ItemStackHandler(SLOTS) {
            @Override
            protected void onContentsChanged(int slot) {
                onChanged.run();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                // The same item is never allowed in both inputs, so a hopper feeding one item cannot fill both and jam.
                return switch (slot) {
                    case INPUT_A -> !ItemStack.isSameItemSameComponents(getStackInSlot(INPUT_B), stack);
                    case INPUT_B -> !ItemStack.isSameItemSameComponents(getStackInSlot(INPUT_A), stack);
                    case FUEL -> stack.getBurnTime(RecipeType.SMELTING) > 0;
                    default -> false;
                };
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlloySmelterBlockEntity smelter) {
        boolean wasLit = smelter.isLit();
        boolean changed = false;
        if (wasLit) {
            smelter.litTime--;
        }

        AlloyingRecipe recipe = smelter.findRecipe(level);
        boolean canCraft = recipe != null && smelter.hasRoomFor(recipe.result());

        if (!smelter.isLit() && canCraft) {
            changed = smelter.burnFuel();
        }

        if (smelter.isLit() && canCraft) {
            smelter.cookTotal = recipe.cookingTime();
            if (++smelter.cookProgress >= smelter.cookTotal) {
                smelter.craft(recipe);
                smelter.cookProgress = 0;
            }
            changed = true;
        } else if (smelter.cookProgress > 0) {
            // Like a furnace: progress cools off while out of fuel, and resets if the inputs are taken out.
            smelter.cookProgress = canCraft ? Math.max(0, smelter.cookProgress - 2) : 0;
            changed = true;
        }

        if (wasLit != smelter.isLit()) {
            level.setBlock(pos, state.setValue(AlloySmelterBlock.LIT, smelter.isLit()), Block.UPDATE_ALL);
            changed = true;
        }
        if (changed || wasLit) {
            setChanged(level, pos, state);
        }
    }

    private boolean isLit() {
        return litTime > 0;
    }

    private @Nullable AlloyingRecipe findRecipe(Level level) {
        ItemStack a = items.getStackInSlot(INPUT_A);
        ItemStack b = items.getStackInSlot(INPUT_B);
        if (a.isEmpty() || b.isEmpty()) {
            return null;
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.ALLOYING_TYPE.get(), new AlloyingRecipe.Input(a, b), level)
                .map(RecipeHolder::value)
                .orElse(null);
    }

    private boolean hasRoomFor(ItemStack result) {
        ItemStack out = items.getStackInSlot(OUTPUT);
        return out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    private boolean burnFuel() {
        ItemStack stack = items.getStackInSlot(FUEL);
        int burnTime = stack.getBurnTime(RecipeType.SMELTING);
        if (burnTime <= 0) {
            return false;
        }
        litTime = burnTime;
        litDuration = burnTime;
        // A lava bucket leaves its empty bucket behind.
        ItemStack remainder = stack.getCraftingRemainingItem();
        items.setStackInSlot(FUEL, stack.getCount() == 1 && !remainder.isEmpty() ? remainder : stack.copyWithCount(stack.getCount() - 1));
        return true;
    }

    private void craft(AlloyingRecipe recipe) {
        AlloyingRecipe.Input input = new AlloyingRecipe.Input(items.getStackInSlot(INPUT_A), items.getStackInSlot(INPUT_B));
        boolean inOrder = recipe.matchesInOrder(input);
        items.extractItem(INPUT_A, (inOrder ? recipe.first() : recipe.second()).count(), false);
        items.extractItem(INPUT_B, (inOrder ? recipe.second() : recipe.first()).count(), false);
        ItemStack out = items.getStackInSlot(OUTPUT);
        items.setStackInSlot(OUTPUT, out.isEmpty()
                ? recipe.result().copy()
                : out.copyWithCount(out.getCount() + recipe.result().getCount()));
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == null) {
            return items;
        }
        return switch (side) {
            case UP -> inputs;
            case DOWN -> output;
            default -> fuel;
        };
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < SLOTS; slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(slot));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ryzergen.alloy_smelter");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AlloySmelterMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
        tag.putInt("lit_time", litTime);
        tag.putInt("lit_duration", litDuration);
        tag.putInt("cook_progress", cookProgress);
        tag.putInt("cook_total", cookTotal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("inventory"));
        litTime = tag.getInt("lit_time");
        litDuration = tag.getInt("lit_duration");
        cookProgress = tag.getInt("cook_progress");
        cookTotal = tag.getInt("cook_total");
    }
}
