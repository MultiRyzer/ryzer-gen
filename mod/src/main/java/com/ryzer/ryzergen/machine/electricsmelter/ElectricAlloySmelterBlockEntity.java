package com.ryzer.ryzergen.machine.electricsmelter;

import com.ryzer.ryzergen.machine.MachineItemPort;
import com.ryzer.ryzergen.machine.MachineEnergyStorage;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The powered alloy smelter: the same alloying recipes, twice as fast, on FE instead of furnace fuel.
 * The fuel-burning smelter stays the way in; this is what microreactor power buys (design rule 7).
 * Pipes connect on any side: inputs in and the output out, energy in.
 */
public class ElectricAlloySmelterBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_A = 0;
    public static final int INPUT_B = 1;
    public static final int OUTPUT = 2;
    public static final int SLOTS = 3;

    public static final int ENERGY_CAPACITY = 20_000;
    public static final int MAX_INPUT = 400;
    public static final int ENERGY_PER_TICK = 20;
    /** Progress per tick: twice a furnace-fuel smelter's speed. */
    public static final int SPEED = 2;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_TOTAL = 1;
    public static final int DATA_ENERGY_LOW = 2;
    public static final int DATA_ENERGY_HIGH = 3;
    public static final int DATA_COUNT = 4;

    private final ItemStackHandler items = createItems(this::setChanged);
    private final IItemHandler inputs = MachineItemPort.of(items, INPUT_A, INPUT_B + 1, OUTPUT, OUTPUT + 1);
    private final IItemHandler output = inputs;
    private final MachineEnergyStorage energy = new MachineEnergyStorage(ENERGY_CAPACITY, MAX_INPUT, this::setChanged);

    private int progress;
    private int total;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_TOTAL -> total;
                case DATA_ENERGY_LOW -> energy.getEnergyStored() & 0xFFFF;
                case DATA_ENERGY_HIGH -> energy.getEnergyStored() >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Read-only on the server; the client copy lives in the menu.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ElectricAlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_ALLOY_SMELTER.get(), pos, state);
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
                // The same item is never allowed in both inputs, so one hopper cannot fill both and jam.
                return switch (slot) {
                    case INPUT_A -> !ItemStack.isSameItemSameComponents(getStackInSlot(INPUT_B), stack);
                    case INPUT_B -> !ItemStack.isSameItemSameComponents(getStackInSlot(INPUT_A), stack);
                    default -> false;
                };
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElectricAlloySmelterBlockEntity smelter) {
        AlloyingRecipe recipe = smelter.findRecipe(level);
        boolean working = recipe != null && smelter.hasRoomFor(recipe.result()) && smelter.energy.consume(ENERGY_PER_TICK);
        if (working) {
            smelter.total = recipe.cookingTime();
            smelter.progress += SPEED;
            if (smelter.progress >= smelter.total) {
                smelter.craft(recipe);
                smelter.progress = 0;
            }
            smelter.setChanged();
        } else if (recipe == null && smelter.progress > 0) {
            smelter.progress = 0;
            smelter.setChanged();
        }
        if (working != state.getValue(ElectricAlloySmelterBlock.ACTIVE)) {
            level.setBlock(pos, state.setValue(ElectricAlloySmelterBlock.ACTIVE, working), Block.UPDATE_ALL);
        }
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
        return side == Direction.DOWN ? output : inputs;
    }

    public IEnergyStorage getEnergy(@Nullable Direction side) {
        return energy;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < SLOTS; slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(slot));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ryzergen.electric_alloy_smelter");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ElectricAlloySmelterMenu(containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("progress", progress);
        tag.putInt("total", total);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("inventory"));
        energy.setStored(tag.getInt("energy"));
        progress = tag.getInt("progress");
        total = tag.getInt("total");
    }
}
