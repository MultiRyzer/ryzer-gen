package com.ryzer.ryzergen;

import com.mojang.logging.LogUtils;
import com.ryzer.ryzergen.registry.ModCreativeTabs;
import com.ryzer.ryzergen.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(RyzerGen.MOD_ID)
public class RyzerGen {
    public static final String MOD_ID = "ryzergen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RyzerGen(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        LOGGER.info("Ryzer Gen loaded");
    }
}
