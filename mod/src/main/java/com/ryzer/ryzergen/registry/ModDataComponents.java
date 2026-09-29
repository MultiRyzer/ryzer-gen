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

    /** FE held by an item, such as a battery module taken out of its cabinet. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORED_ENERGY =
            COMPONENTS.registerComponentType("stored_energy",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** How far a lithium target rod is bred, in thousands of heat-ticks (see StationReactor.TARGET_WORK). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BRED =
            COMPONENTS.registerComponentType("bred",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** A Geiger counter with its clicks switched off (the gauge still shows). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> MUTED =
            COMPONENTS.registerComponentType("muted",
                    builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /**
     * Spent fuel fresh out of a reactor, still giving off decay heat: it must cool in a Spent Fuel
     * Pool before the Core Cracker or Reprocessor takes it. Items without it count as cooled.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HOT =
            COMPONENTS.registerComponentType("hot",
                    builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    private ModDataComponents() {}
}
