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

    /**
     * Whether a fluid is a gas: ours by density, anyone else's by density or the {@code c:gaseous}
     * tag, since not every mod gives its steam a negative density.
     */
    public static boolean isGas(FluidStack stack) {
        return !stack.isEmpty() && (stack.getFluid().getFluidType().isLighterThanAir() || stack.is(Tags.Fluids.GASEOUS));
    }

    private ModFluids() {}
}
