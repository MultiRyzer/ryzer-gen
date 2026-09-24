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
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;
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

        // The powered smelter: the fuel smelter, rebuilt around a heating coil and a control board.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ELECTRIC_ALLOY_SMELTER.get())
                .pattern("SBS")
                .pattern("CAC")
                .pattern("SCS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('B', ModTags.CIRCUITS_BASIC)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('A', ModItems.ALLOY_SMELTER.get())
                .unlockedBy("has_alloy_smelter", has(ModItems.ALLOY_SMELTER.get()))
                .save(output);

        // Cables: copper wire with redstone, insulated in dried kelp. Cheap, as structure should be.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ENERGY_CABLE.get(), 8)
                .pattern("KKK")
                .pattern("CRC")
                .pattern("KKK")
                .define('K', Items.DRIED_KELP)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_copper", has(Tags.Items.INGOTS_COPPER))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.WRENCH.get())
                .pattern("S S")
                .pattern(" I ")
                .pattern(" I ")
                .define('S', ModTags.INGOTS_STEEL)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(output);

        // Dosimeter ring: a fluorite chip (a real thermoluminescent dosimeter material) set in an iron band.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.DOSIMETER_RING.get())
                .pattern(" F ")
                .pattern("I I")
                .pattern(" I ")
                .define('F', OreType.FLUORITE.dropTag())
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_fluorite", has(OreType.FLUORITE.dropTag()))
                .save(output);

        // Home battery: the cabinet is cheap; the cost sits in the modules (design section 12).
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HOME_BATTERY.get())
                .pattern("III")
                .pattern("C C")
                .pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('B', ModTags.CIRCUITS_BASIC)
                .unlockedBy("has_basic_control_board", has(ModTags.CIRCUITS_BASIC))
                .save(output);
        // Lead-acid: lead plates in an electrolyte (redstone stands in for the acid), with a copper terminal.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LEAD_ACID_MODULE.get())
                .pattern(" C ")
                .pattern("LRL")
                .pattern("LLL")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('L', OreType.LEAD.ingotTag())
                .unlockedBy("has_lead", has(OreType.LEAD.ingotTag()))
                .save(output);

        // Steel: iron with a little carbon from coal or charcoal.
        output.accept(id("steel_ingot_from_alloying"), new AlloyingRecipe(
                SizedIngredient.of(Tags.Items.INGOTS_IRON, 1),
                SizedIngredient.of(ItemTags.COALS, 1),
                new ItemStack(ModItems.STEEL_INGOT.get()),
                AlloyingRecipe.DEFAULT_COOKING_TIME), null);

        // Graphite: carbon baked at high heat, so blast furnace only. A block gives 3, so a fuel core
        // (7 graphite) costs about 21 coal or charcoal rather than a grind.
        graphite(output, Tags.Items.STORAGE_BLOCKS_COAL, "graphite_from_coal_block");
        // Vanilla has no charcoal block, so this recipe only loads when some mod adds one.
        graphite(output.withConditions(new NotCondition(new TagEmptyCondition(ModTags.STORAGE_BLOCKS_CHARCOAL))),
                ModTags.STORAGE_BLOCKS_CHARCOAL, "graphite_from_charcoal_block");

        // Tier 1 circuit: copper traces and redstone on an iron board.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BASIC_CONTROL_BOARD.get())
                .pattern("RCR")
                .pattern("III")
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(output);

        // Microreactor. The structure is cheap; the cost sits in the heart and the fuel.
        // Heart: lead shielding, a steel frame, a graphite moderator core, a porthole and a control board.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.REACTOR_HEART.get())
                .pattern("LGL")
                .pattern("S#S")
                .pattern("LBL")
                .define('L', OreType.LEAD.ingotTag())
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('S', ModTags.INGOTS_STEEL)
                .define('#', ModTags.INGOTS_GRAPHITE)
                .define('B', ModTags.CIRCUITS_BASIC)
                .unlockedBy("has_basic_control_board", has(ModTags.CIRCUITS_BASIC))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.REACTOR_MACHINE_UNIT.get())
                .pattern(" S ")
                .pattern("CLC")
                .pattern(" S ")
                .define('S', ModTags.INGOTS_STEEL)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('L', OreType.LEAD.ingotTag())
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(output);
        // The water bucket comes back empty.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COOLANT_JACKET.get())
                .pattern("CCC")
                .pattern("CWC")
                .pattern("CCC")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('W', Tags.Items.BUCKETS_WATER)
                .unlockedBy("has_copper", has(Tags.Items.INGOTS_COPPER))
                .save(output);

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

    private static void graphite(RecipeOutput output, TagKey<Item> input, String name) {
        SimpleCookingRecipeBuilder.generic(Ingredient.of(input), RecipeCategory.MISC, new ItemStack(ModItems.GRAPHITE.get(), 3),
                        0.1f, 200, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new)
                .unlockedBy("has_input", has(input))
                .save(output, id(name + "_blasting"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, path);
    }
}
