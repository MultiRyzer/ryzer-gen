package com.ryzer.ryzergen.compat.jei;

import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import com.ryzer.ryzergen.recipe.MachineRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Locale;

/**
 * One fuel cycle machine's recipes in JEI, laid out like its screen: the inputs in a row (and the
 * water, for the reprocessor), an arrow, the results in a column, and the time and power under it.
 */
public class MachineJeiCategory implements IRecipeCategory<RecipeHolder<MachineRecipe>> {
    private static final int OUTPUTS_X = 104;

    private final ProcessingMachine machine;
    private final RecipeType<RecipeHolder<MachineRecipe>> type;
    private final IDrawable icon;

    public MachineJeiCategory(IGuiHelper guiHelper, ProcessingMachine machine, RecipeType<RecipeHolder<MachineRecipe>> type) {
        this.machine = machine;
        this.type = type;
        icon = guiHelper.createDrawableItemStack(new ItemStack(machine.block()));
    }

    @Override
    public RecipeType<RecipeHolder<MachineRecipe>> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.category.ryzergen." + machine.process().name().toLowerCase(Locale.ROOT));
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        // Room for the large output frame, which reaches 4 px past its slot.
        return OUTPUTS_X + 24;
    }

    @Override
    public int getHeight() {
        return 66;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        MachineRecipe recipe = holder.value();
        for (int i = 0; i < recipe.inputs().size(); i++) {
            builder.addInputSlot(1 + 18 * i, 19).setStandardSlotBackground().addItemStacks(List.of(recipe.inputs().get(i).getItems()));
        }
        recipe.fluid().ifPresent(fluid -> builder.addInputSlot(1 + 18 * recipe.inputs().size(), 19).setStandardSlotBackground()
                .addIngredients(NeoForgeTypes.FLUID_STACK, List.of(fluid.getFluids())));
        int results = recipe.results().size() + (recipe.fluidResult().isPresent() ? 1 : 0);
        recipe.fluidResult().ifPresent(fluid -> builder.addOutputSlot(OUTPUTS_X + 1, 19 - 9 * (results - 1) + 18 * recipe.results().size())
                .setStandardSlotBackground().addIngredient(NeoForgeTypes.FLUID_STACK, fluid));
        for (int i = 0; i < recipe.results().size(); i++) {
            var slot = builder.addOutputSlot(OUTPUTS_X + 1, 19 - 9 * (results - 1) + 18 * i);
            // The large output frame is 26 px: fine for one result, but stacked they overlap and spill
            // past the page, so a column of results gets plain slot frames.
            (results == 1 ? slot.setOutputSlotBackground() : slot.setStandardSlotBackground()).addItemStack(recipe.results().get(i));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        int time = machine.time(holder.value().time());
        builder.addAnimatedRecipeArrowWidget(time).setPosition(74, 19);
        // addText takes the text's size; setPosition places it.
        builder.addText(Component.translatable("jei.ryzergen.machine.time", time / 20F, machine.energyPerTick()), getWidth(), 10)
                .setPosition(0, 56)
                .setColor(0xFF808080);
    }
}
