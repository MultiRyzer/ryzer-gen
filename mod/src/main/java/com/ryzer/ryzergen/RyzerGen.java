package com.ryzer.ryzergen;

import com.mojang.logging.LogUtils;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterBlockEntity;
import com.ryzer.ryzergen.registry.ModBiomeModifiers;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModCreativeTabs;
import com.ryzer.ryzergen.registry.ModItems;
import com.ryzer.ryzergen.registry.ModMenus;
import com.ryzer.ryzergen.registry.ModRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

@Mod(RyzerGen.MOD_ID)
public class RyzerGen {
    public static final String MOD_ID = "ryzergen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RyzerGen(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModRecipes.TYPES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModBiomeModifiers.SERIALIZERS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(RyzerGen::registerCapabilities);
        LOGGER.info("Ryzer Gen loaded");
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ALLOY_SMELTER.get(),
                AlloySmelterBlockEntity::getItemHandler);
    }
}
