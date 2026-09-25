package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.battery.HomeBatteryBlock;
import com.ryzer.ryzergen.cable.EnergyCableBlock;
import com.ryzer.ryzergen.creative.CreativeSourceBlock;
import com.ryzer.ryzergen.creative.CreativeWaterTankBlock;
import com.ryzer.ryzergen.cable.FluidPipeBlock;
import com.ryzer.ryzergen.machine.fission.StationCoreBlock;
import com.ryzer.ryzergen.machine.fission.StationPartBlock;
import com.ryzer.ryzergen.machine.processing.ProcessingBlock;
import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import com.ryzer.ryzergen.machine.processing.TallProcessingBlock;
import com.ryzer.ryzergen.machine.pump.IntakePumpBlock;
import com.ryzer.ryzergen.storage.WasteCaskBlock;
import com.ryzer.ryzergen.storage.FluidTankBlock;
import com.ryzer.ryzergen.storage.PressureTankBlock;
import com.ryzer.ryzergen.cable.GasPipeBlock;
import com.ryzer.ryzergen.cable.ItemPipeBlock;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterBlock;
import com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterBlock;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorPartBlock;
import com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlock;
import com.ryzer.ryzergen.material.OreType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RyzerGen.MOD_ID);

    public static final DeferredBlock<AlloySmelterBlock> ALLOY_SMELTER = BLOCKS.register("alloy_smelter",
            () -> new AlloySmelterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE)));
    public static final DeferredBlock<ElectricAlloySmelterBlock> ELECTRIC_ALLOY_SMELTER = BLOCKS.register("electric_alloy_smelter",
            () -> new ElectricAlloySmelterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .lightLevel(state -> state.getValue(ElectricAlloySmelterBlock.ACTIVE) ? 7 : 0)));

    public static final DeferredBlock<ReactorHeartBlock> REACTOR_HEART = BLOCKS.register("reactor_heart",
            () -> new ReactorHeartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).pushReaction(PushReaction.BLOCK).lightLevel(ModBlocks::microreactorLight)));
    public static final DeferredBlock<EnergyCableBlock> ENERGY_CABLE = BLOCKS.register("energy_cable",
            () -> new EnergyCableBlock(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<ItemPipeBlock> ITEM_PIPE = BLOCKS.register("item_pipe",
            () -> new ItemPipeBlock(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<FluidPipeBlock> FLUID_PIPE = BLOCKS.register("fluid_pipe",
            () -> new FluidPipeBlock(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<GasPipeBlock> GAS_PIPE = BLOCKS.register("gas_pipe",
            () -> new GasPipeBlock(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<IntakePumpBlock> INTAKE_PUMP = BLOCKS.register("intake_pump",
            () -> new IntakePumpBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    // Fuel cycle machines (design section 7). Their lamp strips light while they work.
    public static final DeferredBlock<ProcessingBlock> CORE_CRACKER = BLOCKS.register("core_cracker",
            () -> new ProcessingBlock(ProcessingMachine.CORE_CRACKER, machineProperties()));
    public static final DeferredBlock<TallProcessingBlock> REPROCESSOR = BLOCKS.register("reprocessor",
            () -> new TallProcessingBlock(ProcessingMachine.REPROCESSOR, machineProperties().pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<ProcessingBlock> FUEL_FABRICATOR = BLOCKS.register("fuel_fabricator",
            () -> new ProcessingBlock(ProcessingMachine.FUEL_FABRICATOR, machineProperties()));
    public static final DeferredBlock<WasteCaskBlock> WASTE_CASK = BLOCKS.register("waste_cask",
            () -> new WasteCaskBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<PressureTankBlock> PRESSURE_TANK = BLOCKS.register("pressure_tank",
            () -> new PressureTankBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)));
    public static final DeferredBlock<FluidTankBlock> FLUID_TANK = BLOCKS.register("fluid_tank",
            () -> new FluidTankBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)));
    // Fission station parts (design section 8). All noOcclusion: once formed they stop drawing
    // themselves and the core draws the station, so they must not hide their neighbours' faces.
    public static final DeferredBlock<StationPartBlock> STATION_CASING = BLOCKS.register("station_casing",
            () -> new StationPartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<StationPartBlock> STATION_GLASS = BLOCKS.register("station_glass",
            () -> new StationPartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(1.5F).noOcclusion()
                    .lightLevel(state -> state.getValue(StationPartBlock.LIT) ? 15 : 0)
                    .isViewBlocking((state, level, pos) -> false)));
    public static final DeferredBlock<StationPartBlock> TURBINE_ROTOR = BLOCKS.register("turbine_rotor",
            () -> new StationPartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<StationCoreBlock> STATION_CORE = BLOCKS.register("station_core",
            () -> new StationCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<Block> GRAPHITE_BLOCK = BLOCKS.registerSimpleBlock("graphite_block",
            BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_BLOCK));
    // Creative-only test blocks (no recipe): endless power and endless water.
    public static final DeferredBlock<CreativeSourceBlock> CREATIVE_BATTERY = BLOCKS.register("creative_battery",
            () -> new CreativeSourceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
    public static final DeferredBlock<CreativeWaterTankBlock> CREATIVE_WATER_TANK = BLOCKS.register("creative_water_tank",
            () -> new CreativeWaterTankBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
    public static final DeferredBlock<HomeBatteryBlock> HOME_BATTERY = BLOCKS.register("home_battery",
            () -> new HomeBatteryBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<MicroreactorPartBlock> REACTOR_MACHINE_UNIT = BLOCKS.register("reactor_machine_unit",
            () -> new MicroreactorPartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).pushReaction(PushReaction.BLOCK).lightLevel(ModBlocks::microreactorLight)));
    public static final DeferredBlock<MicroreactorPartBlock> COOLANT_JACKET = BLOCKS.register("coolant_jacket",
            () -> new MicroreactorPartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).pushReaction(PushReaction.BLOCK).lightLevel(ModBlocks::microreactorLight)));

    public static final Map<OreType, DeferredBlock<DropExperienceBlock>> STONE_ORES = new EnumMap<>(OreType.class);
    public static final Map<OreType, DeferredBlock<DropExperienceBlock>> DEEPSLATE_ORES = new EnumMap<>(OreType.class);

    static {
        for (OreType ore : OreType.values()) {
            STONE_ORES.put(ore, BLOCKS.register(ore.id() + "_ore",
                    () -> new DropExperienceBlock(ore.xp(), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE))));
            DEEPSLATE_ORES.put(ore, BLOCKS.register("deepslate_" + ore.id() + "_ore",
                    () -> new DropExperienceBlock(ore.xp(), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE))));
        }
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()
                .lightLevel(state -> state.getValue(ProcessingBlock.ACTIVE) ? 5 : 0);
    }

    /** A running microreactor glows through its porthole and screens. */
    private static int microreactorLight(BlockState state) {
        return state.getValue(MicroreactorPartBlock.RUNNING) ? 9 : 0;
    }

    private ModBlocks() {}
}
