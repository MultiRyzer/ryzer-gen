package com.ryzer.ryzergen.compat.emi;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.recipe.AlloyingRecipe;
import com.ryzer.ryzergen.registry.ModItems;
import com.ryzer.ryzergen.registry.ModRecipes;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * EMI support: shows alloy smelter recipes. EMI finds this through {@link EmiEntrypoint}, so the
 * class is only ever loaded when EMI is installed; the mod never depends on it (design rule 9).
 */
@EmiEntrypoint
public class RyzerGenEmiPlugin implements EmiPlugin {
    public static final EmiStack ALLOY_SMELTER = EmiStack.of(ModItems.ALLOY_SMELTER.get());
    public static final EmiRecipeCategory ALLOYING = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "alloying"), ALLOY_SMELTER);

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(ALLOYING);
        registry.addWorkstation(ALLOYING, ALLOY_SMELTER);
        registry.addWorkstation(ALLOYING, EmiStack.of(ModItems.ELECTRIC_ALLOY_SMELTER.get()));
        for (RecipeHolder<AlloyingRecipe> holder : registry.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOYING_TYPE.get())) {
            registry.addRecipe(new AlloyingEmiRecipe(holder.id(), holder.value()));
        }
    }
}
