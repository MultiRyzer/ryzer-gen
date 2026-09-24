package com.ryzer.ryzergen.compat.emi;

import com.ryzer.ryzergen.recipe.AlloyingRecipe;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** One alloy smelter recipe in EMI, laid out like the smelter: two inputs over a flame, an arrow, the output. */
public class AlloyingEmiRecipe extends BasicEmiRecipe {
    private final int cookingTime;
    private final boolean electricOnly;

    public AlloyingEmiRecipe(ResourceLocation id, AlloyingRecipe recipe) {
        // An electric-only recipe gets a line under the cook time saying so.
        super(RyzerGenEmiPlugin.ALLOYING, id, 96, recipe.electricOnly() ? 52 : 42);
        inputs.add(EmiIngredient.of(recipe.first().ingredient(), recipe.first().count()));
        inputs.add(EmiIngredient.of(recipe.second().ingredient(), recipe.second().count()));
        outputs.add(EmiStack.of(recipe.result()));
        cookingTime = recipe.cookingTime();
        electricOnly = recipe.electricOnly();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 0, 0);
        widgets.addSlot(inputs.get(1), 18, 0);
        // The flame burns for the whole cook; the arrow fills over it (a tick is 50 ms).
        widgets.addTexture(EmiTexture.EMPTY_FLAME, 11, 21);
        widgets.addAnimatedTexture(EmiTexture.FULL_FLAME, 11, 21, cookingTime * 50, false, true, true);
        widgets.addFillingArrow(42, 8, cookingTime * 50);
        widgets.addSlot(outputs.get(0), 70, 4).large(true).recipeContext(this);
        widgets.addText(Component.translatable("emi.cooking.time", cookingTime / 20F), 38, 32, 0xFF404040, false);
        if (electricOnly) {
            widgets.addText(Component.translatable("jei.ryzergen.alloying.electric_only"), 0, 43, 0xFFC0392B, false);
        }
    }
}
