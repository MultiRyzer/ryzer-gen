package com.ryzer.ryzergen.compat.jei;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.compat.RecipeViewerPages;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterScreen;
import com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterScreen;
import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import com.ryzer.ryzergen.machine.processing.ProcessingScreen;
import com.ryzer.ryzergen.recipe.AlloyingRecipe;
import com.ryzer.ryzergen.recipe.MachineRecipe;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
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

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * JEI support: shows alloy smelter and fuel cycle machine recipes, the same as the EMI plugin. JEI finds this through
 * {@link JeiPlugin}, so the class is only ever loaded when JEI is installed; the mod never depends
 * on it (design rule 9). ATM10 ships JEI, so this is the one most players will see.
 */
@JeiPlugin
public class RyzerGenJeiPlugin implements IModPlugin {
    public static final RecipeType<RecipeHolder<AlloyingRecipe>> ALLOYING =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "alloying"));
    /** The Reactor Fuel page: what each reactor burns. */
    public static final RecipeType<RecipeViewerPages.Fuel> FUEL =
            RecipeType.create(RyzerGen.MOD_ID, "reactor_fuel", RecipeViewerPages.Fuel.class);
    /** The fuel cycle machines, one category each. */
    private static final Map<ProcessingMachine, RecipeType<RecipeHolder<MachineRecipe>>> MACHINES = new EnumMap<>(ProcessingMachine.class);

    static {
        for (ProcessingMachine machine : ProcessingMachine.values()) {
            MACHINES.put(machine, RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID,
                    machine.process().name().toLowerCase(Locale.ROOT))));
        }
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AlloyingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new FuelJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        MACHINES.forEach((machine, type) -> registration.addRecipeCategories(
                new MachineJeiCategory(registration.getJeiHelpers().getGuiHelper(), machine, type)));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        registration.addRecipes(ALLOYING,
                Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOYING_TYPE.get()));
        registration.addRecipes(FUEL, RecipeViewerPages.fuels());
        for (RecipeViewerPages.Info info : RecipeViewerPages.info()) {
            registration.addItemStackInfo(info.items(), info.text());
        }
        MACHINES.forEach((machine, type) -> registration.addRecipes(type,
                Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(machine.process().type())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.ALLOY_SMELTER.get()), ALLOYING);
        registration.addRecipeCatalyst(new ItemStack(ModItems.ELECTRIC_ALLOY_SMELTER.get()), ALLOYING);
        MACHINES.forEach((machine, type) -> registration.addRecipeCatalyst(new ItemStack(machine.block()), type));
        registration.addRecipeCatalyst(new ItemStack(ModItems.REACTOR_HEART.get()), FUEL);
        registration.addRecipeCatalyst(new ItemStack(ModItems.STATION_CORE.get()), FUEL);
    }

    /** Clicking the progress arrow in either smelter opens its recipes. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(AlloySmelterScreen.class, 79, 34, 24, 17, ALLOYING);
        registration.addRecipeClickArea(ElectricAlloySmelterScreen.class, 79, 34, 24, 17, ALLOYING);
        // The fuel cycle machines share one screen class, so the arrow opens the category of whichever machine it is.
        registration.addGuiContainerHandler(ProcessingScreen.class, new IGuiContainerHandler<>() {
            @Override
            public Collection<IGuiClickableArea> getGuiClickableAreas(ProcessingScreen screen, double mouseX, double mouseY) {
                return List.of(IGuiClickableArea.createBasic(ProcessingScreen.ARROW_X, ProcessingScreen.ARROW_Y, 24, 17,
                        MACHINES.get(screen.getMenu().machine())));
            }
        });
    }
}
