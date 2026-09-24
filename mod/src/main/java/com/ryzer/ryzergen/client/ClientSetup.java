package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterScreen;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorScreen;
import com.ryzer.ryzergen.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ALLOY_SMELTER.get(), AlloySmelterScreen::new);
        event.register(ModMenus.MICROREACTOR.get(), MicroreactorScreen::new);
    }

    private ClientSetup() {}
}
