package com.ryzer.ryzergen.compat.emi;

import com.ryzer.ryzergen.Preview;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.compat.RecipeViewerPages;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import com.ryzer.ryzergen.recipe.AlloyingRecipe;
import com.ryzer.ryzergen.recipe.MachineRecipe;
import com.ryzer.ryzergen.registry.ModItems;
import com.ryzer.ryzergen.registry.ModRecipes;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Locale;

/**
 * EMI support: shows alloy smelter and fuel cycle machine recipes. EMI finds this through {@link EmiEntrypoint}, so the
 * class is only ever loaded when EMI is installed; the mod never depends on it (design rule 9).
 */
@EmiEntrypoint
public class RyzerGenEmiPlugin implements EmiPlugin {
    public static final EmiStack ALLOY_SMELTER = EmiStack.of(ModItems.ALLOY_SMELTER.get());
    public static final EmiRecipeCategory ALLOYING = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "alloying"), ALLOY_SMELTER);

    @Override
    public void register(EmiRegistry registry) {
        // The processing machines' auto output tab sticks out of the panel's right edge: keep EMI's lists off it.
        registry.addExclusionArea(com.ryzer.ryzergen.machine.processing.ProcessingScreen.class, (screen, out) ->
                out.accept(new dev.emi.emi.api.widget.Bounds(screen.getGuiLeft() + com.ryzer.ryzergen.machine.processing.ProcessingScreen.TAB_X,
                        screen.getGuiTop() + com.ryzer.ryzergen.machine.processing.ProcessingScreen.TAB_Y,
                        com.ryzer.ryzergen.machine.processing.ProcessingScreen.TAB_W, com.ryzer.ryzergen.machine.processing.ProcessingScreen.TAB_H)));
        registry.addCategory(ALLOYING);
        registry.addWorkstation(ALLOYING, ALLOY_SMELTER);
        registry.addWorkstation(ALLOYING, EmiStack.of(ModItems.ELECTRIC_ALLOY_SMELTER.get()));
        for (RecipeHolder<AlloyingRecipe> holder : registry.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOYING_TYPE.get())) {
            registry.addRecipe(new AlloyingEmiRecipe(holder.id(), holder.value()));
        }
        // The Reactor Fuel page, and info pages on the multiblock parts.
        EmiRecipeCategory fuel = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "reactor_fuel"),
                EmiStack.of(ModItems.URANIUM_FUEL_ROD.get()));
        registry.addCategory(fuel);
        registry.addWorkstation(fuel, EmiStack.of(ModItems.REACTOR_HEART.get()));
        registry.addWorkstation(fuel, EmiStack.of(ModItems.STATION_CORE.get()));
        List<RecipeViewerPages.Fuel> fuels = RecipeViewerPages.fuels();
        for (int i = 0; i < fuels.size(); i++) {
            registry.addRecipe(new FuelEmiRecipe(fuel, ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "/reactor_fuel/" + i), fuels.get(i)));
        }
        for (RecipeViewerPages.Info info : RecipeViewerPages.info()) {
            registry.addRecipe(new EmiInfoRecipe(info.items().stream().<EmiIngredient>map(EmiStack::of).toList(), List.of(info.text()),
                    ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "/info/" + info.id())));
        }

        // Unfinished items stay out of the index while the preview is off.
        registry.removeEmiStacks(stack -> Preview.hidden(stack.getItemStack()));

        // The fuel cycle machines, one category each.
        for (ProcessingMachine machine : ProcessingMachine.values()) {
            if (!machine.shown()) {
                continue;
            }
            EmiStack workstation = EmiStack.of(machine.block());
            EmiRecipeCategory category = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID,
                    machine.process().name().toLowerCase(Locale.ROOT)), workstation);
            registry.addCategory(category);
            registry.addWorkstation(category, workstation);
            for (RecipeHolder<MachineRecipe> holder : registry.getRecipeManager().getAllRecipesFor(machine.process().type())) {
                registry.addRecipe(new MachineEmiRecipe(category, holder.id(), holder.value()));
            }
        }
    }
}
