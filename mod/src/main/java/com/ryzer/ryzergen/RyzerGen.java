package com.ryzer.ryzergen;

import com.mojang.logging.LogUtils;
import com.ryzer.ryzergen.battery.HomeBatteryBlock;
import com.ryzer.ryzergen.battery.HomeBatteryBlockEntity;
import com.ryzer.ryzergen.cable.EnergyCableBlockEntity;
import com.ryzer.ryzergen.cable.FluidPipeBlockEntity;
import com.ryzer.ryzergen.creative.CreativeSourceBlockEntity;
import com.ryzer.ryzergen.machine.fission.StationCoreBlockEntity;
import com.ryzer.ryzergen.machine.fission.StationLayout;
import com.ryzer.ryzergen.machine.fission.StationStructure;
import com.ryzer.ryzergen.machine.processing.ProcessingBlock;
import com.ryzer.ryzergen.machine.processing.ProcessingBlockEntity;
import com.ryzer.ryzergen.machine.pump.IntakePumpBlockEntity;
import com.ryzer.ryzergen.storage.PressureTankBlockEntity;
import com.ryzer.ryzergen.cable.ItemPipeBlockEntity;
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
import com.ryzer.ryzergen.registry.ModFluids;
import com.ryzer.ryzergen.registry.ModItems;
import com.ryzer.ryzergen.registry.ModMenus;
import com.ryzer.ryzergen.registry.ModTriggers;
import com.ryzer.ryzergen.registry.ModRecipes;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
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
        ModFluids.TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModTriggers.TRIGGERS.register(modEventBus);
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
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ITEM_PIPE.get(),
                ItemPipeBlockEntity::itemsFor);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.INTAKE_PUMP.get(),
                IntakePumpBlockEntity::energyFor);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.INTAKE_PUMP.get(),
                IntakePumpBlockEntity::waterFor);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CREATIVE_SOURCE.get(),
                CreativeSourceBlockEntity::energyFor);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.CREATIVE_SOURCE.get(),
                CreativeSourceBlockEntity::waterFor);
        // Fission station ports, on a formed station's base: water in, fuel in, power out (pushed,
        // and cables can also pull) on the front, and spent rods out round the side.
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, be, side) -> {
            StationCoreBlockEntity core = StationStructure.coreForPort(level, pos, StationLayout.Port.WATER);
            return core == null ? null : core.runner().waterPort();
        }, ModBlocks.STATION_CASING.get());
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
            StationCoreBlockEntity core = StationStructure.coreForPort(level, pos, StationLayout.Port.FUEL);
            return core == null ? null : core.runner().fuelPort();
        }, ModBlocks.STATION_CASING.get());
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
            StationCoreBlockEntity core = StationStructure.coreForPort(level, pos, StationLayout.Port.OUTPUT);
            return core == null ? null : core.runner().outputPort();
        }, ModBlocks.STATION_CASING.get());
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> {
            StationCoreBlockEntity core = StationStructure.coreForPort(level, pos, StationLayout.Port.ENERGY);
            return core == null ? null : core.runner().energy();
        }, ModBlocks.STATION_CASING.get());
        // The station core takes its parts by pipe while the station is still to be built.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.STATION_CORE.get(),
                StationCoreBlockEntity::itemsFor);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.PRESSURE_TANK.get(),
                PressureTankBlockEntity::fluidFor);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.FLUID_PIPE.get(),
                FluidPipeBlockEntity::fluidsFor);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.GAS_PIPE.get(),
                FluidPipeBlockEntity::fluidsFor);
        // Either half of the battery cabinet reaches the storage in the lower half.
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> {
            HomeBatteryBlockEntity battery = HomeBatteryBlock.battery(level, pos, state);
            return battery == null ? null : battery.energy();
        }, ModBlocks.HOME_BATTERY.get());

        // Fuel cycle machines: registered on the blocks so both halves of the two-high reprocessor
        // reach the block entity in its lower half.
        Block[] processing = {ModBlocks.CORE_CRACKER.get(), ModBlocks.REPROCESSOR.get(), ModBlocks.FUEL_FABRICATOR.get()};
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
            ProcessingBlockEntity machine = processingMachine(level, pos, state);
            return machine == null ? null : machine.getItemHandler(side);
        }, processing);
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> {
            ProcessingBlockEntity machine = processingMachine(level, pos, state);
            return machine == null ? null : machine.getEnergy(side);
        }, processing);
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, be, side) -> {
            ProcessingBlockEntity machine = processingMachine(level, pos, state);
            return machine == null ? null : machine.getWater(side);
        }, processing);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.WASTE_CASK.get(),
                (cask, side) -> cask.items());

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
        // Steam outlet: pipes can drain it, and the reactor pushes into them too. The capability is a
        // fluid handler, like the coolant intake; only the port face differs.
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, be, side) -> {
            ReactorHeartBlockEntity heart = MicroreactorStructure.heartForPort(level, pos, state, MicroreactorPort.STEAM_OUT, side);
            return heart == null ? null : heart.steamOutput();
        }, microreactorParts);
        // Fuel hatch on the lid: fresh cores go in, spent ones come out.
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
            ReactorHeartBlockEntity heart = MicroreactorStructure.heartForPort(level, pos, state, MicroreactorPort.FUEL, side);
            return heart == null ? null : heart.fuelPort();
        }, microreactorParts);
    }

    private static @Nullable ProcessingBlockEntity processingMachine(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof ProcessingBlock block ? block.machineAt(level, pos, state) : null;
    }
}
