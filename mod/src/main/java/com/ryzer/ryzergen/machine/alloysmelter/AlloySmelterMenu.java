package com.ryzer.ryzergen.machine.alloysmelter;

import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModMenus;
import com.ryzer.ryzergen.registry.ModRecipes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import static com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterBlockEntity.*;

public class AlloySmelterMenu extends AbstractContainerMenu {
    private static final int PLAYER_START = SLOTS;
    private static final int HOTBAR_START = PLAYER_START + 27;
    private static final int PLAYER_END = HOTBAR_START + 9;

    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Level level;

    /** Client side: contents and progress arrive from the server. */
    public AlloySmelterMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, createItems(() -> {}), new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public AlloySmelterMenu(int containerId, Inventory inventory, IItemHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.ALLOY_SMELTER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.access = access;
        this.level = inventory.player.level();

        addSlot(new SlotItemHandler(items, INPUT_A, 38, 17));
        addSlot(new SlotItemHandler(items, INPUT_B, 56, 17));
        addSlot(new SlotItemHandler(items, FUEL, 47, 53));
        addSlot(new SlotItemHandler(items, OUTPUT, 116, 35));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public boolean isLit() {
        return data.get(0) > 0;
    }

    /** Fuel left in the current item, from 1 (just lit) to 0. */
    public float litProgress() {
        int duration = data.get(1);
        return duration == 0 ? 0 : Mth.clamp(data.get(0) / (float) duration, 0, 1);
    }

    public float cookProgress() {
        int total = data.get(3);
        return total == 0 ? 0 : Mth.clamp(data.get(2) / (float) total, 0, 1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (isAlloyingIngredient(stack) && moveItemStackTo(stack, INPUT_A, INPUT_B + 1, false)) {
            // Ingredients go to the inputs first, so coal for steel is not burnt as fuel.
        } else if (stack.getBurnTime(RecipeType.SMELTING) > 0 && moveItemStackTo(stack, FUEL, FUEL + 1, false)) {
            // Moved into the fuel slot.
        } else if (index < HOTBAR_START) {
            if (!moveItemStackTo(stack, HOTBAR_START, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    private boolean isAlloyingIngredient(ItemStack stack) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOYING_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> recipe.first().ingredient().test(stack) || recipe.second().ingredient().test(stack));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ALLOY_SMELTER.get());
    }
}
