package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.Preview;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.material.ModTags;
import com.ryzer.ryzergen.material.OreType;
import com.ryzer.ryzergen.recipe.AlloyingRecipe;
import com.ryzer.ryzergen.recipe.MachineRecipe;
import com.ryzer.ryzergen.registry.ModFluids;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
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

        // Item pipes: glass tube round a hopper's worth of iron, the hopper being the thing it replaces.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ITEM_PIPE.get(), 8)
                .pattern("GGG")
                .pattern("IHI")
                .pattern("GGG")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output);

        // Fluid pipes: glass round copper, with a bucket as the thing they replace.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FLUID_PIPE.get(), 8)
                .pattern("GGG")
                .pattern("CBC")
                .pattern("GGG")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('B', Items.BUCKET)
                .unlockedBy("has_bucket", has(Items.BUCKET))
                .save(output);

        // Gas pipes: steel, since steam lines run under pressure, round a copper core.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GAS_PIPE.get(), 8)
                .pattern("SSS")
                .pattern("CGC")
                .pattern("SSS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('G', Tags.Items.GLASS_BLOCKS)
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(output);

        // Intake pump: a steel casing round a bucket and a copper rotor, with a board to run the motor.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INTAKE_PUMP.get())
                .pattern("SBS")
                .pattern("CRC")
                .pattern("SPS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('B', Items.BUCKET)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('P', ModTags.CIRCUITS_BASIC)
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(output);

        // Pressure tanks: a steel shell with sight glasses. Structure, so cheap: two per craft.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PRESSURE_TANK.get(), 2)
                .pattern("SGS")
                .pattern("G G")
                .pattern("SGS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('G', Tags.Items.GLASS_BLOCKS)
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(output);

        // Fluid tanks: copper with sight glasses, like old copper water tanks. Two per craft.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FLUID_TANK.get(), 2)
                .pattern("CGC")
                .pattern("G G")
                .pattern("CGC")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('G', Tags.Items.GLASS_BLOCKS)
                .unlockedBy("has_copper", has(Tags.Items.INGOTS_COPPER))
                .save(output);

        // Fission station. The shell is cheap structure (rule 3): steel-framed concrete-like casing
        // and reinforced glass, many per craft. The cost is in the core (advanced boards, so the fuel
        // cycle comes first) and in what goes into the reactor.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.STATION_CASING.get(), 16)
                .pattern("BSB")
                .pattern("SBS")
                .pattern("BSB")
                .define('B', ItemTags.STONE_BRICKS)
                .define('S', ModTags.INGOTS_STEEL)
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.STATION_GLASS.get(), 16)
                .pattern("GGG")
                .pattern("SGS")
                .pattern("GGG")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('S', ModTags.INGOTS_STEEL)
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(output);
        // Turbine rotor: steel blades round a copper-wound generator shaft.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TURBINE_ROTOR.get())
                .pattern("SCS")
                .pattern("CRC")
                .pattern("SCS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(output);
        // Control core: lead shielding round advanced boards, with graphite for the neutron sensors.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.STATION_CORE.get())
                .pattern("LAL")
                .pattern("GSG")
                .pattern("LAL")
                .define('L', OreType.LEAD.ingotTag())
                .define('A', ModTags.CIRCUITS_ADVANCED)
                .define('G', ModTags.INGOTS_GRAPHITE)
                .define('S', ModTags.INGOTS_STEEL)
                .unlockedBy("has_advanced_board", has(ModTags.CIRCUITS_ADVANCED))
                .save(output);

        // Spent Fuel Pool (design section 7), needed from the first depleted core, so it takes basic
        // boards. The liner is cheap structure (rule 3): steel-lined concrete, many per craft. The
        // cost is in the controller and the crane.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.POOL_LINER.get(), 16)
                .pattern("SBS")
                .pattern("B B")
                .pattern("SBS")
                .define('B', ItemTags.STONE_BRICKS)
                .define('S', ModTags.INGOTS_STEEL)
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(output);
        // Controller: a board to run it, glass for its readout and a cauldron for the tank.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.POOL_CONTROLLER.get())
                .pattern("SBS")
                .pattern("GCG")
                .pattern("SBS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('B', ModTags.CIRCUITS_BASIC)
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('C', Items.CAULDRON)
                .unlockedBy("has_basic_board", has(ModTags.CIRCUITS_BASIC))
                .save(output);
        // Crane: a steel bridge, painted crane yellow, a piston for the hoist and a chain for the cable.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.POOL_CRANE.get())
                .pattern("SSS")
                .pattern("DPD")
                .pattern(" C ")
                .define('S', ModTags.INGOTS_STEEL)
                .define('D', Tags.Items.DYES_YELLOW)
                .define('P', Items.PISTON)
                .define('C', Items.CHAIN)
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(output);

        // Container Battery (design section 12). The frame is cheap structure (rule 3), many per
        // craft; the cost is in the controller (advanced boards: grid storage comes with fission),
        // the thermal unit and above all the LFP racks, which need lithium.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CONTAINER_FRAME.get(), 16)
                .pattern("ISI")
                .pattern("S S")
                .pattern("ISI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('S', ModTags.INGOTS_STEEL)
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(output);
        // Controller: advanced boards to manage the racks, glass for its screen, redstone for its busbar.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BATTERY_CONTROLLER.get())
                .pattern("SAS")
                .pattern("GRG")
                .pattern("SAS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('A', ModTags.CIRCUITS_ADVANCED)
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .unlockedBy("has_advanced_board", has(ModTags.CIRCUITS_ADVANCED))
                .save(output);
        // Thermal unit: a fan behind a wire guard (iron bars), copper for the cooling coil, a board to run it.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.THERMAL_UNIT.get())
                .pattern("SCS")
                .pattern("CIC")
                .pattern("SBS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('I', Items.IRON_BARS)
                .define('B', ModTags.CIRCUITS_BASIC)
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(output);
        // LFP rack: lithium iron phosphate cells (lithium, iron, and bone meal for the phosphate) in a
        // steel case, with a copper busbar. Real basis: most grid batteries now use LFP cells.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LFP_BATTERY_RACK.get())
                .pattern("LCL")
                .pattern("IPI")
                .pattern("LSL")
                .define('L', ModTags.DUSTS_LITHIUM)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('P', Items.BONE_MEAL)
                .define('S', ModTags.INGOTS_STEEL)
                .unlockedBy("has_lithium", has(ModTags.DUSTS_LITHIUM))
                .save(output);

        // Graphite block: nine graphite, and back. The fission station's moderator.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GRAPHITE_BLOCK.get())
                .pattern("GGG")
                .pattern("GGG")
                .pattern("GGG")
                .define('G', ModTags.INGOTS_GRAPHITE)
                .unlockedBy("has_graphite", has(ModTags.INGOTS_GRAPHITE))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.GRAPHITE.get(), 9)
                .requires(ModTags.STORAGE_BLOCKS_GRAPHITE)
                .unlockedBy("has_graphite_block", has(ModTags.STORAGE_BLOCKS_GRAPHITE))
                .save(output, id("graphite_from_block"));
        // Control rod: silver (real control rods are silver, indium and cadmium) in a steel sleeve.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CONTROL_ROD.get())
                .pattern("A")
                .pattern("S")
                .pattern("A")
                .define('A', OreType.SILVER.ingotTag())
                .define('S', ModTags.INGOTS_STEEL)
                .unlockedBy("has_silver", has(OreType.SILVER.ingotTag()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.WRENCH.get())
                .pattern("S S")
                .pattern(" I ")
                .pattern(" I ")
                .define('S', ModTags.INGOTS_STEEL)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(output);

        // Geiger counter: a gas-filled glass tube over a board, with a note block for the speaker.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.GEIGER_COUNTER.get())
                .pattern(" G ")
                .pattern("IBI")
                .pattern("INI")
                .define('G', Tags.Items.GLASS_PANES)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('B', ModTags.CIRCUITS_BASIC)
                .define('N', Items.NOTE_BLOCK)
                .unlockedBy("has_uranium", has(OreType.URANIUM.dropTag()))
                .save(output);
        // Flow Scanner: a probe (copper), a screen over a basic board, in an iron case.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FLOW_SCANNER.get())
                .pattern(" C ")
                .pattern("IGI")
                .pattern("IBI")
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('G', Tags.Items.GLASS_PANES)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('B', ModTags.CIRCUITS_BASIC)
                .unlockedBy("has_basic_control_board", has(ModTags.CIRCUITS_BASIC))
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

        // Silicon: silica reduced with carbon in an arc furnace (carbothermic reduction), far hotter
        // than burning fuel reaches, so electric only.
        output.accept(id("silicon_from_alloying"), new AlloyingRecipe(
                SizedIngredient.of(Tags.Items.SANDS, 1),
                SizedIngredient.of(ItemTags.COALS, 1),
                new ItemStack(ModItems.SILICON.get()),
                300, true), null);

        // Tier 2 circuit: a silicon chip on a steel board with gold contacts.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ADVANCED_CONTROL_BOARD.get())
                .pattern("GSG")
                .pattern("TTT")
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('S', ModTags.SILICON)
                .define('T', ModTags.INGOTS_STEEL)
                .unlockedBy("has_silicon", has(ModTags.SILICON))
                .save(output);

        // Plutonium ingots and nuggets, back and forth.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PLUTONIUM_INGOT.get())
                .pattern("NNN")
                .pattern("NNN")
                .pattern("NNN")
                .define('N', ModTags.NUGGETS_PLUTONIUM)
                .unlockedBy("has_plutonium", has(ModTags.NUGGETS_PLUTONIUM))
                .save(output, id("plutonium_ingot_from_nuggets"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PLUTONIUM_NUGGET.get(), 9)
                .requires(ModTags.INGOTS_PLUTONIUM)
                .unlockedBy("has_plutonium", has(ModTags.INGOTS_PLUTONIUM))
                .save(output, id("plutonium_nuggets_from_ingot"));

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

        fuelCycle(output);

        for (OreType ore : OreType.values()) {
            if (ore.hasIngot()) {
                Item ingot = ModItems.INGOTS.get(ore).get();
                smeltAndBlast(output, ore.dropTag(), ingot, ore.ingotName() + "_from_" + ore.dropName());
                smeltAndBlast(output, ore.oreItemTag(), ingot, ore.ingotName() + "_from_ore");
            }
        }
    }

    /**
     * The fuel cycle (design section 7): the four machines, then what each one makes. Each machine
     * has its own recipe type, so packs can change one machine's recipes alone.
     */
    private static void fuelCycle(RecipeOutput output) {
        // Core Cracker: a jaw crusher (two pistons) in a steel frame. It takes a basic board, since
        // it is needed the moment the first core runs out.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CORE_CRACKER.get())
                .pattern("SBS")
                .pattern("PIP")
                .pattern("SIS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('B', ModTags.CIRCUITS_BASIC)
                .define('P', Items.PISTON)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_depleted_core", has(ModItems.DEPLETED_FUEL_CORE.get()))
                .save(output);
        // Reprocessor: a lead-lined column (shielding) round a cauldron, the dissolver, run by advanced boards.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.REPROCESSOR.get())
                .pattern("LAL")
                .pattern("SCS")
                .pattern("LAL")
                .define('L', OreType.LEAD.ingotTag())
                .define('A', ModTags.CIRCUITS_ADVANCED)
                .define('S', ModTags.INGOTS_STEEL)
                .define('C', Items.CAULDRON)
                .unlockedBy("has_advanced_board", has(ModTags.CIRCUITS_ADVANCED))
                .save(output);
        // Fuel Fabricator: a press (two pistons) with copper wiring and an advanced board.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FUEL_FABRICATOR.get())
                .pattern("SAS")
                .pattern("CPC")
                .pattern("SPS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('A', ModTags.CIRCUITS_ADVANCED)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('P', Items.PISTON)
                .unlockedBy("has_advanced_board", has(ModTags.CIRCUITS_ADVANCED))
                .save(output);
        // Waste Cask: a lead-lined steel drum round a barrel.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WASTE_CASK.get())
                .pattern("SLS")
                .pattern("LBL")
                .pattern("SLS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('L', OreType.LEAD.ingotTag())
                .define('B', Items.BARREL)
                .unlockedBy("has_waste", has(ModItems.FISSION_WASTE.get()))
                .save(output);

        // Speed module: redstone logic, copper windings and gold contacts on an advanced board.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SPEED_MODULE.get())
                .pattern("RCR")
                .pattern("GAG")
                .pattern("RCR")
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('A', ModTags.CIRCUITS_ADVANCED)
                .unlockedBy("has_advanced_board", has(ModTags.CIRCUITS_ADVANCED))
                .save(output);

        // Cable fittings: each tier is made from the one before, so a better fitting replaces a worse
        // one. Silver plating over copper, then heavy copper busbars, then superconductor.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SILVER_FITTINGS.get())
                .pattern("SCS")
                .pattern("CRC")
                .pattern("SCS")
                .define('S', OreType.SILVER.ingotTag())
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_silver", has(OreType.SILVER.ingotTag()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BUSBAR_FITTINGS.get())
                .pattern("BSB")
                .pattern("AFA")
                .pattern("BSB")
                .define('B', Tags.Items.STORAGE_BLOCKS_COPPER)
                .define('S', ModTags.INGOTS_STEEL)
                .define('A', ModTags.CIRCUITS_ADVANCED)
                .define('F', ModItems.SILVER_FITTINGS.get())
                .unlockedBy("has_silver_fittings", has(ModItems.SILVER_FITTINGS.get()))
                .save(output);
        // Superconductor: yttrium (tier 5, from monazite) kept cold with blue ice. Until the mod makes
        // yttrium this only loads when another mod provides it.
        TagKey<Item> yttrium = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/yttrium"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CRYOGENIC_FITTINGS.get())
                .pattern("YIY")
                .pattern("IFI")
                .pattern("YIY")
                .define('Y', yttrium)
                .define('I', Items.BLUE_ICE)
                .define('F', ModItems.BUSBAR_FITTINGS.get())
                .unlockedBy("has_busbar_fittings", has(ModItems.BUSBAR_FITTINGS.get()))
                .save(output.withConditions(new NotCondition(new TagEmptyCondition(yttrium))));

        // Cracking: TRISO has to be crushed open before it can be processed. The shell and packing
        // come back as graphite and steel.
        machine(output, "cracking/depleted_fuel_core", MachineRecipe.Process.CRACKING,
                List.of(SizedIngredient.of(ModItems.DEPLETED_FUEL_CORE.get(), 1)), null,
                List.of(new ItemStack(ModItems.SPENT_KERNELS.get()), new ItemStack(ModItems.GRAPHITE.get(), 2),
                        new ItemStack(ModItems.STEEL_INGOT.get(), 2)), 200);

        // Reprocessing (fluoride volatility): half the uranium back, the plutonium, the waste. A
        // station rod holds far more fuel than a microreactor core, so it gives 3 nuggets to its 1.
        SizedFluidIngredient water = SizedFluidIngredient.of(Tags.Fluids.WATER, 250);
        machine(output, "reprocessing/spent_kernels", MachineRecipe.Process.REPROCESSING,
                List.of(SizedIngredient.of(ModItems.SPENT_KERNELS.get(), 1), SizedIngredient.of(OreType.FLUORITE.dropTag(), 1)), water,
                List.of(new ItemStack(ModItems.INGOTS.get(OreType.URANIUM).get()), new ItemStack(ModItems.PLUTONIUM_NUGGET.get()),
                        new ItemStack(ModItems.FISSION_WASTE.get())), 400);
        machine(output, "reprocessing/spent_uranium_rod", MachineRecipe.Process.REPROCESSING,
                List.of(SizedIngredient.of(ModItems.SPENT_URANIUM_ROD.get(), 1), SizedIngredient.of(OreType.FLUORITE.dropTag(), 1)), water,
                List.of(new ItemStack(ModItems.INGOTS.get(OreType.URANIUM).get()), new ItemStack(ModItems.PLUTONIUM_NUGGET.get(), 3),
                        new ItemStack(ModItems.FISSION_WASTE.get())), 400);

        // Fabricating: fuel pellets sealed in steel tubes (real cladding is zirconium alloy; early
        // reactors used steel), MOX with plutonium mixed in, and TRISO pellets pressed more
        // thriftily than the crafting grid manages (3 per uranium, not 2).
        machine(output, "fabricating/uranium_fuel_rod", MachineRecipe.Process.FABRICATING,
                List.of(SizedIngredient.of(OreType.URANIUM.ingotTag(), 2), SizedIngredient.of(ModTags.INGOTS_STEEL, 1)), null,
                List.of(new ItemStack(ModItems.URANIUM_FUEL_ROD.get())), 200);
        machine(output, "fabricating/mox_fuel_rod", MachineRecipe.Process.FABRICATING,
                List.of(SizedIngredient.of(ModTags.INGOTS_PLUTONIUM, 1), SizedIngredient.of(OreType.URANIUM.ingotTag(), 1),
                        SizedIngredient.of(ModTags.INGOTS_STEEL, 1)), null,
                List.of(new ItemStack(ModItems.MOX_FUEL_ROD.get())), 200);
        machine(output, "fabricating/triso_pellets", MachineRecipe.Process.FABRICATING,
                List.of(SizedIngredient.of(OreType.URANIUM.ingotTag(), 1), SizedIngredient.of(ModTags.INGOTS_GRAPHITE, 2),
                        SizedIngredient.of(ModTags.GEMS_SILICON_CARBIDE, 1)), null,
                List.of(new ItemStack(ModItems.TRISO_PELLETS.get(), 3)), 200);
        // Lithium target rod: lithium in aluminium (real rods hold lithium aluminate pellets) in a steel tube.
        // Tritium breeding is unfinished, so its recipe loads only with the preview on.
        RecipeOutput preview = output.withConditions(Preview.CONDITION);
        machine(preview, "fabricating/lithium_target_rod", MachineRecipe.Process.FABRICATING,
                List.of(SizedIngredient.of(ModTags.DUSTS_LITHIUM, 2), SizedIngredient.of(OreType.ALUMINIUM.ingotTag(), 1),
                        SizedIngredient.of(ModTags.INGOTS_STEEL, 1)), null,
                List.of(new ItemStack(ModItems.LITHIUM_TARGET_ROD.get())), 200);

        // Lithium Extractor (design section 10): a brine column over a pump, run by an advanced board.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LITHIUM_EXTRACTOR.get())
                .pattern("SGS")
                .pattern("GCG")
                .pattern("SAS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('C', Items.CAULDRON)
                .define('A', ModTags.CIRCUITS_ADVANCED)
                .unlockedBy("has_salt", has(OreType.SALT.dropTag()))
                .save(output);
        // Extracting: salt dissolved into brine, the lithium drawn out of it. Real basis: direct
        // lithium extraction from brine, where most of the world's lithium comes from.
        machine(output, "extracting/lithium", MachineRecipe.Process.EXTRACTING,
                List.of(SizedIngredient.of(OreType.SALT.dropTag(), 2)), SizedFluidIngredient.of(Tags.Fluids.WATER, 500),
                List.of(new ItemStack(ModItems.LITHIUM_DUST.get())), 200);

        // Electrorefiner (design section 9): a molten salt cell for pyroprocessing, the breeder's way in.
        // Unfinished until the breeder lands, so it loads only with the preview on.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ELECTROREFINER.get())
                .pattern("SCS")
                .pattern("PKP")
                .pattern("SAS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('C', Items.CAULDRON)
                .define('P', OreType.LEAD.ingotTag())
                .define('K', ModTags.INGOTS_GRAPHITE)
                .define('A', ModTags.CIRCUITS_ADVANCED)
                .unlockedBy("has_spent_mox_rod", has(ModItems.SPENT_MOX_ROD.get()))
                .save(preview);
        // Spent MOX, dissolved in the salt bath: the transuranics and uranium plate out on the
        // cathodes, the fission products stay in the salt and leave as waste. A little salt tops up
        // the bath. Real basis: the electrorefiner at the heart of pyroprocessing.
        machine(preview, "electrorefining/spent_mox_rod", MachineRecipe.Process.ELECTROREFINING,
                List.of(SizedIngredient.of(ModItems.SPENT_MOX_ROD.get(), 1), SizedIngredient.of(OreType.SALT.dropTag(), 1)), null,
                List.of(new ItemStack(ModItems.TRANSURANIC_METAL.get()), new ItemStack(ModItems.INGOTS.get(OreType.URANIUM).get()),
                        new ItemStack(ModItems.FISSION_WASTE.get())), 600);
        // Breeder reactor (design section 9), behind the preview while it is built. The frame and
        // shell are cheap structure (rule 3), many per craft: steel girders and welded steel plate.
        // The cost is in the core: lead shielding round advanced boards, and transuranic metal, so
        // the Electrorefiner (and a MOX station feeding it) comes first.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BREEDER_FRAME.get(), 16)
                .pattern("SGS")
                .pattern("GSG")
                .pattern("SGS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('G', ModTags.INGOTS_GRAPHITE)
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(preview);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BREEDER_SHELL.get(), 16)
                .pattern("SSS")
                .pattern("SLS")
                .pattern("SSS")
                .define('S', ModTags.INGOTS_STEEL)
                .define('L', OreType.LEAD.ingotTag())
                .unlockedBy("has_steel", has(ModTags.INGOTS_STEEL))
                .save(preview);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BREEDER_CORE.get())
                .pattern("LAL")
                .pattern("TST")
                .pattern("LAL")
                .define('L', OreType.LEAD.ingotTag())
                .define('A', ModTags.CIRCUITS_ADVANCED)
                .define('T', ModItems.TRANSURANIC_METAL.get())
                .define('S', ModTags.INGOTS_STEEL)
                .unlockedBy("has_transuranic_metal", has(ModItems.TRANSURANIC_METAL.get()))
                .save(preview);
        // Salt alone splits into sodium (the chlorine is vented). Real basis: molten salt
        // electrolysis is how sodium is made.
        machine(preview, "electrorefining/sodium", MachineRecipe.Process.ELECTROREFINING,
                List.of(SizedIngredient.of(OreType.SALT.dropTag(), 2)), null,
                List.of(new ItemStack(ModItems.SODIUM_INGOT.get())), 200);
        // Liquid sodium for the breeder's loop: an ingot melted in the cell's molten salt (sodium melts
        // at 98 C, the cell runs at about 500 C), into its tank. 250 mB an ingot, a generous fudge so
        // filling a loop is no grind (rule 3).
        machineToFluid(preview, "electrorefining/liquid_sodium", MachineRecipe.Process.ELECTROREFINING,
                List.of(SizedIngredient.of(ModTags.INGOTS_SODIUM, 1)), new FluidStack(ModFluids.SODIUM.get(), 250), 40);
        // Breeder fuel (design section 9): transuranic metal alloyed with uranium, clad in steel, as
        // real fast reactor metal fuel is (EBR-II's uranium-plutonium alloy pins in steel tubes; ours
        // leaves out the zirconium).
        machine(preview, "fabricating/breeder_fuel", MachineRecipe.Process.FABRICATING,
                List.of(SizedIngredient.of(ModItems.TRANSURANIC_METAL.get(), 1), SizedIngredient.of(OreType.URANIUM.ingotTag(), 1),
                        SizedIngredient.of(ModTags.INGOTS_STEEL, 2)), null,
                List.of(new ItemStack(ModItems.BREEDER_FUEL.get())), 300);
        // The breeder's uranium blanket: uranium in a steel tube (real blankets use depleted uranium).
        machine(preview, "fabricating/uranium_blanket", MachineRecipe.Process.FABRICATING,
                List.of(SizedIngredient.of(OreType.URANIUM.ingotTag(), 2), SizedIngredient.of(ModTags.INGOTS_STEEL, 1)), null,
                List.of(new ItemStack(ModItems.URANIUM_BLANKET.get())), 200);
        // Spent breeder fuel goes back into transuranic metal, but half of it: a fast core burns more
        // than it makes (only the blanket gains), so the breeder never fuels itself. The rest comes
        // from the MOX station's spent fuel, which keeps the station running beside it (design
        // section 9, pillar 6: roots run deep).
        machine(preview, "electrorefining/spent_breeder_fuel", MachineRecipe.Process.ELECTROREFINING,
                List.of(SizedIngredient.of(ModItems.SPENT_BREEDER_FUEL.get(), 2), SizedIngredient.of(OreType.SALT.dropTag(), 1)), null,
                List.of(new ItemStack(ModItems.TRANSURANIC_METAL.get()), new ItemStack(ModItems.INGOTS.get(OreType.URANIUM).get()),
                        new ItemStack(ModItems.FISSION_WASTE.get(), 2)), 600);
        // A bred uranium blanket: its plutonium for MOX rods (3 nuggets, as much as a spent uranium rod
        // gives the Reprocessor), and the uranium left in it.
        machine(preview, "electrorefining/bred_uranium_blanket", MachineRecipe.Process.ELECTROREFINING,
                List.of(SizedIngredient.of(ModItems.BRED_URANIUM_BLANKET.get(), 1), SizedIngredient.of(OreType.SALT.dropTag(), 1)), null,
                List.of(new ItemStack(ModItems.PLUTONIUM_NUGGET.get(), 3), new ItemStack(ModItems.INGOTS.get(OreType.URANIUM).get())), 600);
    }

    private static void machine(RecipeOutput output, String name, MachineRecipe.Process process, List<SizedIngredient> inputs,
                                @Nullable SizedFluidIngredient fluid, List<ItemStack> results, int time) {
        output.accept(id(name), new MachineRecipe(process, inputs, Optional.ofNullable(fluid), results, Optional.empty(), time), null);
    }

    /** A machine recipe whose result is a fluid, into the machine's output tank. */
    private static void machineToFluid(RecipeOutput output, String name, MachineRecipe.Process process, List<SizedIngredient> inputs,
                                       FluidStack result, int time) {
        output.accept(id(name), new MachineRecipe(process, inputs, Optional.empty(), List.of(), Optional.of(result), time), null);
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
