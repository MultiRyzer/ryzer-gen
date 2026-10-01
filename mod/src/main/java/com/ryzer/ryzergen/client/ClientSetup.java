package com.ryzer.ryzergen.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import com.ryzer.ryzergen.registry.ModParticles;
import com.ryzer.ryzergen.battery.container.ContainerScreen;
import com.ryzer.ryzergen.battery.container.ContainerBuildScreen;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.renderer.BiomeColors;
import com.ryzer.ryzergen.machine.pool.PoolScreen;
import com.ryzer.ryzergen.machine.pool.PoolBuildScreen;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.compat.accessories.AccessoriesClient;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.ryzer.ryzergen.cable.CableUpgrade;
import com.ryzer.ryzergen.registry.ModBlocks;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import com.ryzer.ryzergen.machine.processing.ProcessingScreen;
import com.ryzer.ryzergen.battery.HomeBatteryScreen;
import com.ryzer.ryzergen.cable.CableScreen;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterScreen;
import com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterScreen;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorScreen;
import com.ryzer.ryzergen.machine.fission.StationControlScreen;
import com.ryzer.ryzergen.machine.fission.StationCoreScreen;
import com.ryzer.ryzergen.machine.breeder.BreederControlScreen;
import com.ryzer.ryzergen.machine.breeder.BreederCoreScreen;
import com.ryzer.ryzergen.machine.pump.IntakePumpScreen;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import com.ryzer.ryzergen.registry.ModFluids;
import com.ryzer.ryzergen.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ALLOY_SMELTER.get(), AlloySmelterScreen::new);
        event.register(ModMenus.ELECTRIC_ALLOY_SMELTER.get(), ElectricAlloySmelterScreen::new);
        event.register(ModMenus.ENERGY_CABLE.get(), CableScreen::new);
        event.register(ModMenus.ITEM_PIPE.get(), CableScreen::new);
        event.register(ModMenus.FLUID_PIPE.get(), CableScreen::new);
        event.register(ModMenus.GAS_PIPE.get(), CableScreen::new);
        event.register(ModMenus.HOME_BATTERY.get(), HomeBatteryScreen::new);
        event.register(ModMenus.INTAKE_PUMP.get(), IntakePumpScreen::new);
        event.register(ModMenus.STATION_CORE.get(), StationCoreScreen::new);
        event.register(ModMenus.BREEDER_CORE.get(), BreederCoreScreen::new);
        event.register(ModMenus.BREEDER_CONTROL.get(), BreederControlScreen::new);
        event.register(ModMenus.STATION_CONTROL.get(), StationControlScreen::new);
        event.register(ModMenus.POOL_BUILD.get(), PoolBuildScreen::new);
        event.register(ModMenus.POOL.get(), PoolScreen::new);
        event.register(ModMenus.CONTAINER_BUILD.get(), ContainerBuildScreen::new);
        event.register(ModMenus.CONTAINER_BATTERY.get(), ContainerScreen::new);
        event.register(ModMenus.MICROREACTOR.get(), MicroreactorScreen::new);
        event.register(ModMenus.CORE_CRACKER.get(), ProcessingScreen::new);
        event.register(ModMenus.REPROCESSOR.get(), ProcessingScreen::new);
        event.register(ModMenus.FUEL_FABRICATOR.get(), ProcessingScreen::new);
        event.register(ModMenus.LITHIUM_EXTRACTOR.get(), ProcessingScreen::new);
        event.register(ModMenus.ELECTROREFINER.get(), ProcessingScreen::new);
    }

    /**
     * The Spent Fuel Pool's water is vanilla's still water, which is grey: tint it with the biome's
     * water colour, as water blocks are, so a pool matches the water round it.
     */
    @SubscribeEvent
    public static void registerBlockColours(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint != 0 ? -1
                        : level != null && pos != null ? BiomeColors.getAverageWaterColor(level, pos) : 0xFF3F76E4,
                ModBlocks.POOL_LINER.get(), ModBlocks.POOL_CRANE.get(), ModBlocks.POOL_CONTROLLER.get());
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.POOL_BUBBLE.get(), PoolBubbleParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.PRESSURE_TANK.get(), PressureTankRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STATION_CORE.get(), StationRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BREEDER_CORE.get(), BreederRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.POOL_CONTROLLER.get(), PoolRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CORE_CRACKER.get(), CoreCrackerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FUEL_FABRICATOR.get(), FuelFabricatorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LITHIUM_EXTRACTOR.get(), LithiumExtractorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BATTERY_CONTROLLER.get(), ContainerFanRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FUSION_PREVIEW.get(), FusionPreviewRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SUN_GATE_PREVIEW.get(), SunGateRenderer::new);
    }

    /** Optional mods' client hooks: no renderer for the worn dosimeter ring, when Accessories is here. */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(AccessoriesClient::register);
    }

    /**
     * Each tier's fitting on each side, a small model of its own, added to cables that have one; and
     * the models machines' renderers turn (the Core Cracker's flywheels).
     */
    @SubscribeEvent
    public static void registerFittingModels(ModelEvent.RegisterAdditional event) {
        event.register(CoreCrackerRenderer.FLYWHEEL);
        event.register(FuelFabricatorRenderer.RAM);
        for (ModelResourceLocation part : PoolRenderer.CRANE_PARTS) {
            event.register(part);
        }
        for (CableUpgrade tier : CableUpgrade.values()) {
            if (tier == CableUpgrade.NONE) {
                continue;
            }
            for (Direction side : Direction.values()) {
                event.register(FittedCableModel.location(tier, side));
            }
        }
    }

    /** Every cable and pipe state's model gains its fittings. */
    @SubscribeEvent
    public static void wrapCableModels(ModelEvent.ModifyBakingResult event) {
        Block[] cables = {ModBlocks.ENERGY_CABLE.get(), ModBlocks.ITEM_PIPE.get(), ModBlocks.FLUID_PIPE.get(), ModBlocks.GAS_PIPE.get()};
        for (Block cable : cables) {
            for (BlockState state : cable.getStateDefinition().getPossibleStates()) {
                event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state), (key, model) -> new FittedCableModel(model));
            }
        }
    }

    /** The station's geometry is read from resources, so drop it when they reload. */
    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> StationGeometry.clear());
    }

    /** Steam's look in GUIs and tanks: a pale animated haze. Liquid sodium: molten silver metal. */
    @SubscribeEvent
    public static void registerFluidLooks(RegisterClientExtensionsEvent event) {
        ResourceLocation steam = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/steam");
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return steam;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return steam;
            }
        }, ModFluids.STEAM_TYPE.get());
        ResourceLocation sodium = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/sodium");
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return sodium;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return sodium;
            }
        }, ModFluids.SODIUM_TYPE.get());
    }

    private ClientSetup() {}
}
