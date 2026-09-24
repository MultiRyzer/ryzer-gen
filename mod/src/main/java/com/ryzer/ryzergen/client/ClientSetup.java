package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.battery.HomeBatteryScreen;
import com.ryzer.ryzergen.cable.CableScreen;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterScreen;
import com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterScreen;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorScreen;
import com.ryzer.ryzergen.machine.fission.StationControlScreen;
import com.ryzer.ryzergen.machine.fission.StationCoreScreen;
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
        event.register(ModMenus.STATION_CONTROL.get(), StationControlScreen::new);
        event.register(ModMenus.MICROREACTOR.get(), MicroreactorScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.PRESSURE_TANK.get(), PressureTankRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STATION_CORE.get(), StationRenderer::new);
    }

    /** The station's geometry is read from resources, so drop it when they reload. */
    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> StationGeometry.clear());
    }

    /** Steam's look in GUIs and tanks: a pale animated haze. */
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
    }

    private ClientSetup() {}
}
