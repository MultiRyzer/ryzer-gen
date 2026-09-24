package com.ryzer.ryzergen.registry;

import com.mojang.serialization.Codec;
import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RyzerGen.MOD_ID);

    /** Ticks of burn left in a sealed fuel core. Missing means a fresh, full core. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FUEL_LEFT =
            COMPONENTS.registerComponentType("fuel_left",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {}
}
