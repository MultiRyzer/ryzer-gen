package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.battery.HomeBatteryBlockEntity;
import com.ryzer.ryzergen.cable.EnergyCableBlockEntity;
import com.ryzer.ryzergen.cable.FluidPipeBlockEntity;
import com.ryzer.ryzergen.creative.CreativeSourceBlockEntity;
import com.ryzer.ryzergen.machine.fission.StationCoreBlockEntity;
import com.ryzer.ryzergen.machine.processing.ProcessingBlockEntity;
import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import com.ryzer.ryzergen.machine.pump.IntakePumpBlockEntity;
import com.ryzer.ryzergen.storage.WasteCaskBlockEntity;
import com.ryzer.ryzergen.storage.PressureTankBlockEntity;
import com.ryzer.ryzergen.cable.ItemPipeBlockEntity;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterBlockEntity;
import com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterBlockEntity;
import com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RyzerGen.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloySmelterBlockEntity>> ALLOY_SMELTER =
            BLOCK_ENTITIES.register("alloy_smelter",
                    () -> BlockEntityType.Builder.of(AlloySmelterBlockEntity::new, ModBlocks.ALLOY_SMELTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricAlloySmelterBlockEntity>> ELECTRIC_ALLOY_SMELTER =
            BLOCK_ENTITIES.register("electric_alloy_smelter",
                    () -> BlockEntityType.Builder.of(ElectricAlloySmelterBlockEntity::new, ModBlocks.ELECTRIC_ALLOY_SMELTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCableBlockEntity>> ENERGY_CABLE =
            BLOCK_ENTITIES.register("energy_cable",
                    () -> BlockEntityType.Builder.of(EnergyCableBlockEntity::new, ModBlocks.ENERGY_CABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE =
            BLOCK_ENTITIES.register("item_pipe",
                    () -> BlockEntityType.Builder.of(ItemPipeBlockEntity::new, ModBlocks.ITEM_PIPE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IntakePumpBlockEntity>> INTAKE_PUMP =
            BLOCK_ENTITIES.register("intake_pump",
                    () -> BlockEntityType.Builder.of(IntakePumpBlockEntity::new, ModBlocks.INTAKE_PUMP.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressureTankBlockEntity>> PRESSURE_TANK =
            BLOCK_ENTITIES.register("pressure_tank",
                    () -> BlockEntityType.Builder.of(PressureTankBlockEntity::new,
                            ModBlocks.PRESSURE_TANK.get(), ModBlocks.FLUID_TANK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StationCoreBlockEntity>> STATION_CORE =
            BLOCK_ENTITIES.register("station_core",
                    () -> BlockEntityType.Builder.of(StationCoreBlockEntity::new, ModBlocks.STATION_CORE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeSourceBlockEntity>> CREATIVE_SOURCE =
            BLOCK_ENTITIES.register("creative_source",
                    () -> BlockEntityType.Builder.of(CreativeSourceBlockEntity::new,
                            ModBlocks.CREATIVE_BATTERY.get(), ModBlocks.CREATIVE_WATER_TANK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE =
            BLOCK_ENTITIES.register("fluid_pipe",
                    () -> BlockEntityType.Builder.of(FluidPipeBlockEntity::liquid, ModBlocks.FLUID_PIPE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPipeBlockEntity>> GAS_PIPE =
            BLOCK_ENTITIES.register("gas_pipe",
                    () -> BlockEntityType.Builder.of(FluidPipeBlockEntity::gas, ModBlocks.GAS_PIPE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HomeBatteryBlockEntity>> HOME_BATTERY =
            BLOCK_ENTITIES.register("home_battery",
                    () -> BlockEntityType.Builder.of(HomeBatteryBlockEntity::new, ModBlocks.HOME_BATTERY.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorHeartBlockEntity>> REACTOR_HEART =
            BLOCK_ENTITIES.register("reactor_heart",
                    () -> BlockEntityType.Builder.of(ReactorHeartBlockEntity::new, ModBlocks.REACTOR_HEART.get()).build(null));

    // The fuel cycle machines share one block entity class, told apart by their ProcessingMachine.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingBlockEntity>> CORE_CRACKER =
            BLOCK_ENTITIES.register("core_cracker", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new ProcessingBlockEntity(ProcessingMachine.CORE_CRACKER, pos, state), ModBlocks.CORE_CRACKER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingBlockEntity>> REPROCESSOR =
            BLOCK_ENTITIES.register("reprocessor", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new ProcessingBlockEntity(ProcessingMachine.REPROCESSOR, pos, state), ModBlocks.REPROCESSOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingBlockEntity>> FUEL_FABRICATOR =
            BLOCK_ENTITIES.register("fuel_fabricator", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new ProcessingBlockEntity(ProcessingMachine.FUEL_FABRICATOR, pos, state), ModBlocks.FUEL_FABRICATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WasteCaskBlockEntity>> WASTE_CASK =
            BLOCK_ENTITIES.register("waste_cask",
                    () -> BlockEntityType.Builder.of(WasteCaskBlockEntity::new, ModBlocks.WASTE_CASK.get()).build(null));

    private ModBlockEntities() {}
}
