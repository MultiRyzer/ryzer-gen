package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Our fluids. Steam is a gas: lighter than air (negative density), never placed in the world and
 * with no bucket, carried only by gas pipes and held in pressure tanks. It is tagged {@code c:steam},
 * so other mods' steam users and tanks accept it.
 *
 * <p>Liquid sodium (design section 9) is the breeder's coolant: a liquid metal, melted from the ingot
 * in the Electrorefiner's molten salt cell. Like steam it is never placed in the world and has no
 * bucket (sodium burns in air and with water), so it lives in pipes and tanks. Tagged
 * {@code c:sodium}, so other mods' sodium fits.
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, RyzerGen.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, RyzerGen.MOD_ID);

    public static final TagKey<Fluid> STEAM_TAG = FluidTags.create(ResourceLocation.fromNamespaceAndPath("c", "steam"));

    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = TYPES.register("steam", () -> new FluidType(
            FluidType.Properties.create().density(-1000).viscosity(200).temperature(473).canSwim(false).canDrown(false)
                    .canPushEntity(false).supportsBoating(false)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STEAM =
            FLUIDS.register("steam", () -> new BaseFlowingFluid.Source(steamProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_STEAM =
            FLUIDS.register("flowing_steam", () -> new BaseFlowingFluid.Flowing(steamProperties()));

    private static BaseFlowingFluid.Properties steamProperties() {
        return new BaseFlowingFluid.Properties(STEAM_TYPE, STEAM, FLOWING_STEAM);
    }

    public static final TagKey<Fluid> SODIUM_TAG = FluidTags.create(ResourceLocation.fromNamespaceAndPath("c", "sodium"));

    /** Real liquid sodium: a little lighter than water and about as runny, kept molten above 98 C (371 K). */
    public static final DeferredHolder<FluidType, FluidType> SODIUM_TYPE = TYPES.register("sodium", () -> new FluidType(
            FluidType.Properties.create().density(927).viscosity(700).temperature(371).canSwim(false).canDrown(false)
                    .supportsBoating(false)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SODIUM =
            FLUIDS.register("sodium", () -> new BaseFlowingFluid.Source(sodiumProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SODIUM =
            FLUIDS.register("flowing_sodium", () -> new BaseFlowingFluid.Flowing(sodiumProperties()));

    private static BaseFlowingFluid.Properties sodiumProperties() {
        return new BaseFlowingFluid.Properties(SODIUM_TYPE, SODIUM, FLOWING_SODIUM);
    }

    /**
     * Whether a fluid is a gas: ours by density, anyone else's by density or the {@code c:gaseous}
     * tag, since not every mod gives its steam a negative density.
     */
    public static boolean isGas(FluidStack stack) {
        return !stack.isEmpty() && (stack.getFluid().getFluidType().isLighterThanAir() || stack.is(Tags.Fluids.GASEOUS));
    }

    private ModFluids() {}
}
