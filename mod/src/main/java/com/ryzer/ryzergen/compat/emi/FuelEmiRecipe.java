package com.ryzer.ryzergen.compat.emi;

import com.ryzer.ryzergen.compat.RecipeViewerPages;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The Reactor Fuel page in EMI, laid out like the JEI one. */
public class FuelEmiRecipe extends BasicEmiRecipe {
    private final Component power;
    private final Component life;

    public FuelEmiRecipe(EmiRecipeCategory category, ResourceLocation id, RecipeViewerPages.Fuel fuel) {
        super(category, id, 160, 42);
        inputs.add(EmiStack.of(fuel.fuel()));
        outputs.add(EmiStack.of(fuel.spent()));
        catalysts.add(EmiStack.of(fuel.reactor()));
        power = fuel.power();
        life = fuel.life();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 0, 0);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 26, 1);
        widgets.addSlot(outputs.get(0), 60, 0).recipeContext(this);
        widgets.addSlot(catalysts.get(0), 142, 0).catalyst(true);
        widgets.addText(power, 0, 23, 0xFF404040, false);
        widgets.addText(life, 0, 33, 0xFF404040, false);
    }
}
