package com.ryzer.ryzergen.compat.jei;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterScreen;
import com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterScreen;
import com.ryzer.ryzergen.recipe.AlloyingRecipe;
import com.ryzer.ryzergen.registry.ModItems;
import com.ryzer.ryzergen.registry.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI support: shows alloy smelter recipes, the same as the EMI plugin. JEI finds this through
 * {@link JeiPlugin}, so the class is only ever loaded when JEI is installed; the mod never depends
 * on it (design rule 9). ATM10 ships JEI, so this is the one most players will see.
 */
@JeiPlugin
public class RyzerGenJeiPlugin implements IModPlugin {
    public static final RecipeType<RecipeHolder<AlloyingRecipe>> ALLOYING =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "alloying"));

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AlloyingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        registration.addRecipes(ALLOYING,
                Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOYING_TYPE.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.ALLOY_SMELTER.get()), ALLOYING);
        registration.addRecipeCatalyst(new ItemStack(ModItems.ELECTRIC_ALLOY_SMELTER.get()), ALLOYING);
    }

    /** Clicking the progress arrow in either smelter opens its recipes. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(AlloySmelterScreen.class, 79, 34, 24, 17, ALLOYING);
        registration.addRecipeClickArea(ElectricAlloySmelterScreen.class, 79, 34, 24, 17, ALLOYING);
    }
}
