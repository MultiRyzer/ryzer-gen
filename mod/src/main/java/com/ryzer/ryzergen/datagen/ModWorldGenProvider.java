package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.registry.ModDamageTypes;
import net.minecraft.world.damagesource.DamageType;
import com.ryzer.ryzergen.material.OreType;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.world.ConfigurableOreBiomeModifier;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Ore veins. Everything here becomes plain JSON, so pack makers can retune it with a datapack.
 *
 * <p>Vanilla for comparison: iron is size 9 at 10 veins per chunk, gold is size 9 at 4 per chunk,
 * and redstone is size 8 at 4 per chunk.
 */
public class ModWorldGenProvider extends DatapackBuiltinEntriesProvider {

    /**
     * @param size    blocks per vein, roughly
     * @param count   veins per chunk
     * @param peaked  true: most common at the middle of the height range and thinning out towards the ends.
     *                false: even spread across the range.
     */
    private record Vein(OreType ore, int size, int count, boolean peaked, int minY, int maxY) {}

    private static final List<Vein> VEINS = List.of(
            // Common. Needed in bulk for shielding from tier 1 onwards.
            new Vein(OreType.LEAD, 9, 8, true, -32, 64),
            // Common and shallow. Bauxite forms near the surface.
            new Vein(OreType.ALUMINIUM, 10, 6, true, 0, 96),
            // Few but large, shallow beds, like real evaporite deposits.
            new Vein(OreType.SALT, 12, 3, false, 32, 80),
            // Uncommon. Real silver is often found alongside lead.
            new Vein(OreType.SILVER, 8, 4, true, -64, 32),
            new Vein(OreType.FLUORITE, 7, 4, false, -16, 64),
            // Rare and deep. Every fuel core needs it, so a vein is a real find.
            new Vein(OreType.URANIUM, 6, 3, true, -56, 24),
            // Rare. Only needed from tier 4.
            new Vein(OreType.MONAZITE, 5, 2, false, -32, 48),
            // Rarest and deepest. Only needed for fusion.
            new Vein(OreType.TUNGSTEN, 5, 2, true, -64, 0)
    );

    public ModWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, ModWorldGenProvider::configuredFeatures)
                .add(Registries.PLACED_FEATURE, ModWorldGenProvider::placedFeatures)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModWorldGenProvider::biomeModifiers)
                .add(Registries.DAMAGE_TYPE, context -> context.register(ModDamageTypes.RADIATION,
                        new DamageType("ryzergen.radiation", 0.0F))),
                Set.of(RyzerGen.MOD_ID));
    }

    private static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stone = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslate = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        for (Vein vein : VEINS) {
            FeatureUtils.register(context, configuredKey(vein.ore()), Feature.ORE, new OreConfiguration(List.of(
                    OreConfiguration.target(stone, ModBlocks.STONE_ORES.get(vein.ore()).get().defaultBlockState()),
                    OreConfiguration.target(deepslate, ModBlocks.DEEPSLATE_ORES.get(vein.ore()).get().defaultBlockState())
            ), vein.size()));
        }
    }

    private static void placedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        for (Vein vein : VEINS) {
            VerticalAnchor bottom = VerticalAnchor.absolute(vein.minY());
            VerticalAnchor top = VerticalAnchor.absolute(vein.maxY());
            PlacementUtils.register(context, placedKey(vein.ore()), configured.getOrThrow(configuredKey(vein.ore())),
                    CountPlacement.of(vein.count()),
                    InSquarePlacement.spread(),
                    vein.peaked() ? HeightRangePlacement.triangle(bottom, top) : HeightRangePlacement.uniform(bottom, top),
                    BiomeFilter.biome());
        }
    }

    private static void biomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
        for (Vein vein : VEINS) {
            context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, id("add_" + vein.ore().id() + "_ore")),
                    new ConfigurableOreBiomeModifier(
                            biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                            HolderSet.direct(placed.getOrThrow(placedKey(vein.ore()))),
                            vein.ore().id()));
        }
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(OreType ore) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, id(ore.id() + "_ore"));
    }

    private static ResourceKey<PlacedFeature> placedKey(OreType ore) {
        return ResourceKey.create(Registries.PLACED_FEATURE, id(ore.id() + "_ore"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, path);
    }
}
