package com.ryzer.ryzergen;

import com.mojang.logging.LogUtils;
import com.ryzer.ryzergen.battery.HomeBatteryBlock;
import com.ryzer.ryzergen.battery.HomeBatteryBlockEntity;
import com.ryzer.ryzergen.cable.EnergyCableBlockEntity;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterBlockEntity;
import com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterBlockEntity;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorPort;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorStructure;
import com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlockEntity;
import com.ryzer.ryzergen.radiation.RadiationClientState;
import com.ryzer.ryzergen.radiation.RadiationPayload;
import com.ryzer.ryzergen.registry.ModAttachments;
import com.ryzer.ryzergen.registry.ModBiomeModifiers;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModCreativeTabs;
import com.ryzer.ryzergen.registry.ModDataComponents;
import com.ryzer.ryzergen.registry.ModItems;
import com.ryzer.ryzergen.registry.ModMenus;
import com.ryzer.ryzergen.registry.ModRecipes;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(RyzerGen.MOD_ID)
public class RyzerGen {
    public static final String MOD_ID = "ryzergen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RyzerGen(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModRecipes.TYPES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModBiomeModifiers.SERIALIZERS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(RyzerGen::registerCapabilities);
        modEventBus.addListener(RyzerGen::registerPayloads);
        LOGGER.info("Ryzer Gen loaded");
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(RadiationPayload.TYPE, RadiationPayload.STREAM_CODEC, RadiationClientState::receive);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ALLOY_SMELTER.get(),
                AlloySmelterBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ELECTRIC_ALLOY_SMELTER.get(),
                ElectricAlloySmelterBlockEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ELECTRIC_ALLOY_SMELTER.get(),
                ElectricAlloySmelterBlockEntity::getEnergy);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ENERGY_CABLE.get(),
                EnergyCableBlockEntity::energyFor);
        // Either half of the battery cabinet reaches the storage in the lower half.
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> {
            HomeBatteryBlockEntity battery = HomeBatteryBlock.battery(level, pos, state);
            return battery == null ? null : battery.energy();
        }, ModBlocks.HOME_BATTERY.get());

        // Microreactor ports: any part can end up in a port's slot, so these go on all three blocks.
        Block[] microreactorParts = {ModBlocks.REACTOR_HEART.get(), ModBlocks.REACTOR_MACHINE_UNIT.get(), ModBlocks.COOLANT_JACKET.get()};
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> {
            ReactorHeartBlockEntity heart = MicroreactorStructure.heartForPort(level, pos, state, MicroreactorPort.ENERGY_OUT, side);
            return heart == null ? null : heart.energyOutput();
        }, microreactorParts);
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, be, side) -> {
            ReactorHeartBlockEntity heart = MicroreactorStructure.heartForPort(level, pos, state, MicroreactorPort.COOLANT_IN, side);
            return heart == null ? null : heart.coolantInput();
        }, microreactorParts);
    }
}
