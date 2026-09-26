package com.ryzer.ryzergen.compat.jei;

import com.ryzer.ryzergen.compat.RecipeViewerPages;
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

/** The Reactor Fuel page in JEI: a fuel, an arrow to what it burns down to, the reactor, and its power and life. */
public class FuelJeiCategory implements IRecipeCategory<RecipeViewerPages.Fuel> {
    private final IDrawable icon;

    public FuelJeiCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.URANIUM_FUEL_ROD.get()));
    }

    @Override
    public RecipeType<RecipeViewerPages.Fuel> getRecipeType() {
        return RyzerGenJeiPlugin.FUEL;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.category.ryzergen.reactor_fuel");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 160;
    }

    @Override
    public int getHeight() {
        return 42;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeViewerPages.Fuel fuel, IFocusGroup focuses) {
        builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStack(fuel.fuel());
        builder.addOutputSlot(61, 1).setOutputSlotBackground().addItemStack(fuel.spent());
        builder.addSlot(RecipeIngredientRole.CATALYST, 143, 1).setStandardSlotBackground().addItemStack(fuel.reactor());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeViewerPages.Fuel fuel, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(26, 1);
        // addText takes the text's size; setPosition places it.
        builder.addText(fuel.power(), getWidth(), 10).setPosition(0, 23).setColor(0xFF808080);
        builder.addText(fuel.life(), getWidth(), 10).setPosition(0, 33).setColor(0xFF808080);
    }
}
