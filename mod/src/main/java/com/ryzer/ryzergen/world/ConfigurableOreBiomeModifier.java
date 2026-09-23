package com.ryzer.ryzergen.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModBiomeModifiers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/** Adds ore features to biomes like NeoForge's add_features modifier, unless the ore is switched off in the config. */
public record ConfigurableOreBiomeModifier(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features, String ore)
        implements BiomeModifier {

    public static final MapCodec<ConfigurableOreBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(ConfigurableOreBiomeModifier::biomes),
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(ConfigurableOreBiomeModifier::features),
            Codec.STRING.fieldOf("ore").forGetter(ConfigurableOreBiomeModifier::ore)
    ).apply(instance, ConfigurableOreBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && biomes.contains(biome) && Config.isOreGenerationEnabled(ore)) {
            features.forEach(feature -> builder.getGenerationSettings()
                    .addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, feature));
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return ModBiomeModifiers.CONFIGURABLE_ORE.get();
    }
}
