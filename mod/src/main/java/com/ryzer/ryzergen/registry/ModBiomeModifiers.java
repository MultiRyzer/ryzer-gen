package com.ryzer.ryzergen.registry;

import com.mojang.serialization.MapCodec;
import com.ryzer.ryzergen.Preview;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.world.ConfigurableOreBiomeModifier;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModBiomeModifiers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, RyzerGen.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ConfigurableOreBiomeModifier>> CONFIGURABLE_ORE =
            SERIALIZERS.register("configurable_ore", () -> ConfigurableOreBiomeModifier.CODEC);

    /** Recipe condition for unfinished content: see {@link Preview}. */
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, RyzerGen.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<? extends ICondition>> PREVIEW =
            CONDITIONS.register("preview", Preview::codec);

    private ModBiomeModifiers() {}
}
