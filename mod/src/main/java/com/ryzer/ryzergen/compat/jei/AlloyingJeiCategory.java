package com.ryzer.ryzergen.compat.jei;

import com.ryzer.ryzergen.recipe.AlloyingRecipe;
import com.ryzer.ryzergen.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/** One alloy smelter recipe in JEI, laid out like the EMI one: two inputs over a flame, an arrow, the output. */
public class AlloyingJeiCategory implements IRecipeCategory<RecipeHolder<AlloyingRecipe>> {
    private final IDrawable icon;

    public AlloyingJeiCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.ALLOY_SMELTER.get()));
    }

    @Override
    public RecipeType<RecipeHolder<AlloyingRecipe>> getRecipeType() {
        return RyzerGenJeiPlugin.ALLOYING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.category.ryzergen.alloying");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 96;
    }

    @Override
    public int getHeight() {
        // Room for an "Electric only" line under the cook time.
        return 52;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AlloyingRecipe> holder, IFocusGroup focuses) {
        AlloyingRecipe recipe = holder.value();
        builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStacks(stacks(recipe.first()));
        builder.addInputSlot(19, 1).setStandardSlotBackground().addItemStacks(stacks(recipe.second()));
        builder.addOutputSlot(75, 9).setOutputSlotBackground().addItemStack(recipe.result());
    }

    /** Every item the ingredient accepts, each at the recipe's count. */
    private static List<ItemStack> stacks(SizedIngredient ingredient) {
        return List.of(ingredient.getItems());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<AlloyingRecipe> holder, IFocusGroup focuses) {
        int cookingTime = holder.value().cookingTime();
        // The flame burns for the whole cook; the arrow fills over it.
        builder.addAnimatedRecipeFlameWidget(cookingTime).setPosition(11, 21);
        builder.addAnimatedRecipeArrowWidget(cookingTime).setPosition(44, 9);
        builder.addText(Component.translatable("gui.jei.category.smelting.time.seconds", cookingTime / 20F), 38, 32)
                .setColor(0xFF808080);
        if (holder.value().electricOnly()) {
            builder.addText(Component.translatable("jei.ryzergen.alloying.electric_only"), 0, 43).setColor(0xFFC0392B);
        }
    }
}
