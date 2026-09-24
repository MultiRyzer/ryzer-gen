package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.material.ModTags;
import com.ryzer.ryzergen.material.OreType;
import com.ryzer.ryzergen.recipe.AlloyingRecipe;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.concurrent.CompletableFuture;

/** Recipes always take common tags as input, so other mods' materials work too. */
public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ALLOY_SMELTER.get())
                .pattern("III")
                .pattern("BFB")
                .pattern("BBB")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('B', Items.BRICKS)
                .define('F', Items.FURNACE)
                .unlockedBy("has_furnace", has(Items.FURNACE))
                .save(output);

        // Steel: iron with a little carbon from coal or charcoal.
        output.accept(id("steel_ingot_from_alloying"), new AlloyingRecipe(
                SizedIngredient.of(Tags.Items.INGOTS_IRON, 1),
                SizedIngredient.of(ItemTags.COALS, 1),
                new ItemStack(ModItems.STEEL_INGOT.get()),
                AlloyingRecipe.DEFAULT_COOKING_TIME), null);

        // Graphite: carbon baked at high heat, so blast furnace only.
        blast(output, Tags.Items.STORAGE_BLOCKS_COAL, ModItems.GRAPHITE.get(), "graphite_from_coal_block");
        blast(output, ModTags.STORAGE_BLOCKS_CHARCOAL, ModItems.GRAPHITE.get(), "graphite_from_charcoal_block");

        // Silicon carbide: silica and carbon baked together, the Acheson process.
        output.accept(id("silicon_carbide_from_alloying"), new AlloyingRecipe(
                SizedIngredient.of(Tags.Items.SANDS, 1),
                SizedIngredient.of(ModTags.INGOTS_GRAPHITE, 1),
                new ItemStack(ModItems.SILICON_CARBIDE.get()),
                AlloyingRecipe.DEFAULT_COOKING_TIME), null);

        // TRISO: a uranium kernel coated in carbon and silicon carbide layers.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.TRISO_PELLETS.get(), 2)
                .requires(OreType.URANIUM.ingotTag())
                .requires(ModTags.INGOTS_GRAPHITE)
                .requires(ModTags.INGOTS_GRAPHITE)
                .requires(ModTags.GEMS_SILICON_CARBIDE)
                .unlockedBy("has_uranium", has(OreType.URANIUM.ingotTag()))
                .save(output);

        // Sealed fuel core: pellets packed in graphite inside a steel shell.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SEALED_FUEL_CORE.get())
                .pattern("SPS")
                .pattern("PGP")
                .pattern("SPS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('P', ModItems.TRISO_PELLETS.get())
                .define('G', ModTags.INGOTS_GRAPHITE)
                .unlockedBy("has_triso_pellets", has(ModItems.TRISO_PELLETS.get()))
                .save(output);

        for (OreType ore : OreType.values()) {
            if (ore.hasIngot()) {
                Item ingot = ModItems.INGOTS.get(ore).get();
                smeltAndBlast(output, ore.dropTag(), ingot, ore.ingotName() + "_from_" + ore.dropName());
                smeltAndBlast(output, ore.oreItemTag(), ingot, ore.ingotName() + "_from_ore");
            }
        }
    }

    private static void smeltAndBlast(RecipeOutput output, TagKey<Item> input, Item result, String name) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, result, 0.7f, 200)
                .unlockedBy("has_input", has(input))
                .save(output, id(name + "_smelting"));
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, result, 0.7f, 100)
                .unlockedBy("has_input", has(input))
                .save(output, id(name + "_blasting"));
    }

    private static void blast(RecipeOutput output, TagKey<Item> input, Item result, String name) {
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, result, 0.1f, 200)
                .unlockedBy("has_input", has(input))
                .save(output, id(name + "_blasting"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, path);
    }
}
