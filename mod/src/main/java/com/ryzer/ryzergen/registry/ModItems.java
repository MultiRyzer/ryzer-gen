package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.cable.CableUpgrade;
import com.ryzer.ryzergen.cable.FittingItem;
import com.ryzer.ryzergen.machine.SpeedModuleItem;
import com.ryzer.ryzergen.battery.BatteryChemistry;
import com.ryzer.ryzergen.battery.BatteryModuleItem;
import com.ryzer.ryzergen.item.WrenchItem;
import com.ryzer.ryzergen.radiation.DosimeterRingItem;
import com.ryzer.ryzergen.radiation.GeigerCounterItem;
import com.ryzer.ryzergen.machine.fission.FuelRodItem;
import com.ryzer.ryzergen.machine.fission.TargetRodItem;
import com.ryzer.ryzergen.machine.microreactor.FuelCoreItem;
import com.ryzer.ryzergen.material.OreType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RyzerGen.MOD_ID);

    public static final DeferredItem<BlockItem> ALLOY_SMELTER = ITEMS.registerSimpleBlockItem(ModBlocks.ALLOY_SMELTER);
    public static final DeferredItem<BlockItem> ELECTRIC_ALLOY_SMELTER = ITEMS.registerSimpleBlockItem(ModBlocks.ELECTRIC_ALLOY_SMELTER);
    public static final DeferredItem<BlockItem> ENERGY_CABLE = ITEMS.registerSimpleBlockItem(ModBlocks.ENERGY_CABLE);
    public static final DeferredItem<BlockItem> ITEM_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.ITEM_PIPE);
    public static final DeferredItem<BlockItem> FLUID_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.FLUID_PIPE);
    public static final DeferredItem<BlockItem> GAS_PIPE = ITEMS.registerSimpleBlockItem(ModBlocks.GAS_PIPE);
    public static final DeferredItem<BlockItem> INTAKE_PUMP = ITEMS.registerSimpleBlockItem(ModBlocks.INTAKE_PUMP);
    public static final DeferredItem<BlockItem> PRESSURE_TANK = ITEMS.registerSimpleBlockItem(ModBlocks.PRESSURE_TANK);
    public static final DeferredItem<BlockItem> CORE_CRACKER = ITEMS.registerSimpleBlockItem(ModBlocks.CORE_CRACKER);
    public static final DeferredItem<BlockItem> REPROCESSOR = ITEMS.registerSimpleBlockItem(ModBlocks.REPROCESSOR);
    public static final DeferredItem<BlockItem> FUEL_FABRICATOR = ITEMS.registerSimpleBlockItem(ModBlocks.FUEL_FABRICATOR);
    public static final DeferredItem<BlockItem> LITHIUM_EXTRACTOR = ITEMS.registerSimpleBlockItem(ModBlocks.LITHIUM_EXTRACTOR);
    public static final DeferredItem<BlockItem> WASTE_CASK = ITEMS.registerSimpleBlockItem(ModBlocks.WASTE_CASK);
    public static final DeferredItem<BlockItem> FLUID_TANK = ITEMS.registerSimpleBlockItem(ModBlocks.FLUID_TANK);
    public static final DeferredItem<BlockItem> STATION_CORE = ITEMS.registerSimpleBlockItem(ModBlocks.STATION_CORE);
    public static final DeferredItem<BlockItem> CREATIVE_BATTERY = ITEMS.registerSimpleBlockItem(ModBlocks.CREATIVE_BATTERY);
    public static final DeferredItem<BlockItem> CREATIVE_WATER_TANK = ITEMS.registerSimpleBlockItem(ModBlocks.CREATIVE_WATER_TANK);
    public static final DeferredItem<BlockItem> FUSION_PREVIEW = ITEMS.registerSimpleBlockItem(ModBlocks.FUSION_PREVIEW);
    public static final DeferredItem<BlockItem> SUN_GATE_PREVIEW = ITEMS.registerSimpleBlockItem(ModBlocks.SUN_GATE_PREVIEW);
    public static final DeferredItem<BlockItem> STATION_CASING = ITEMS.registerSimpleBlockItem(ModBlocks.STATION_CASING);
    public static final DeferredItem<BlockItem> STATION_GLASS = ITEMS.registerSimpleBlockItem(ModBlocks.STATION_GLASS);
    public static final DeferredItem<BlockItem> TURBINE_ROTOR = ITEMS.registerSimpleBlockItem(ModBlocks.TURBINE_ROTOR);
    public static final DeferredItem<WrenchItem> WRENCH = ITEMS.registerItem("wrench", WrenchItem::new);
    public static final DeferredItem<BlockItem> HOME_BATTERY = ITEMS.registerSimpleBlockItem(ModBlocks.HOME_BATTERY);
    public static final DeferredItem<BatteryModuleItem> LEAD_ACID_MODULE = ITEMS.registerItem("lead_acid_module",
            properties -> new BatteryModuleItem(BatteryChemistry.LEAD_ACID, properties));
    public static final DeferredItem<DosimeterRingItem> DOSIMETER_RING = ITEMS.registerItem("dosimeter_ring", DosimeterRingItem::new);
    public static final DeferredItem<GeigerCounterItem> GEIGER_COUNTER = ITEMS.registerItem("geiger_counter", GeigerCounterItem::new);
    /** Hold it to see what flows into every pipe and cable nearby, against each input's limit. */
    public static final DeferredItem<com.ryzer.ryzergen.scanner.FlowScannerItem> FLOW_SCANNER =
            ITEMS.registerItem("flow_scanner", com.ryzer.ryzergen.scanner.FlowScannerItem::new);
    /** Creative-only test tool: launches the Dyson swarm round the sun and closes it (see sky/SunSwarm). */
    public static final DeferredItem<com.ryzer.ryzergen.sky.SwarmControllerItem> SWARM_CONTROLLER =
            ITEMS.registerItem("swarm_controller", com.ryzer.ryzergen.sky.SwarmControllerItem::new);
    public static final DeferredItem<Item> GRAPHITE = ITEMS.registerSimpleItem("graphite");
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    /** Tier 1 circuit: iron, copper and redstone. */
    public static final DeferredItem<Item> BASIC_CONTROL_BOARD = ITEMS.registerSimpleItem("basic_control_board");

    // Microreactor fuel chain: silicon carbide, TRISO pellets, sealed fuel core, depleted core.
    public static final DeferredItem<Item> SILICON_CARBIDE = ITEMS.registerSimpleItem("silicon_carbide");
    public static final DeferredItem<Item> TRISO_PELLETS = ITEMS.registerSimpleItem("triso_pellets");
    public static final DeferredItem<FuelCoreItem> SEALED_FUEL_CORE = ITEMS.registerItem("sealed_fuel_core", FuelCoreItem::new);
    /** Safe to carry (design rule 10). Waits for the fuel cycle's core cracker. */
    public static final DeferredItem<Item> DEPLETED_FUEL_CORE = ITEMS.registerSimpleItem("depleted_fuel_core",
            new Item.Properties().stacksTo(16));

    // Fuel cycle (tier 2): silicon and the advanced board, then what reprocessing makes and the
    // fuel rods the fabricator presses for the fission reactor.
    /** Made from sand and coal in the electric alloy smelter only. */
    public static final DeferredItem<Item> SILICON = ITEMS.registerSimpleItem("silicon");
    public static final DeferredItem<SpeedModuleItem> SPEED_MODULE = ITEMS.registerItem("speed_module", SpeedModuleItem::new);
    // Cable fittings, one line for every kind of cable and pipe; each tier is crafted from the last.
    public static final DeferredItem<FittingItem> SILVER_FITTINGS = ITEMS.registerItem("silver_fittings",
            properties -> new FittingItem(CableUpgrade.SILVER, properties));
    public static final DeferredItem<FittingItem> BUSBAR_FITTINGS = ITEMS.registerItem("busbar_fittings",
            properties -> new FittingItem(CableUpgrade.BUSBAR, properties));
    public static final DeferredItem<FittingItem> CRYOGENIC_FITTINGS = ITEMS.registerItem("cryogenic_fittings",
            properties -> new FittingItem(CableUpgrade.CRYOGENIC, properties));
    /** Tier 2 circuit: steel, gold and silicon. */
    public static final DeferredItem<Item> ADVANCED_CONTROL_BOARD = ITEMS.registerSimpleItem("advanced_control_board");
    /** A cracked depleted core: the fuel kernels, freed from their casing for the reprocessor. */
    public static final DeferredItem<Item> SPENT_KERNELS = ITEMS.registerSimpleItem("spent_kernels");
    public static final DeferredItem<Item> PLUTONIUM_NUGGET = ITEMS.registerSimpleItem("plutonium_nugget");
    public static final DeferredItem<Item> PLUTONIUM_INGOT = ITEMS.registerSimpleItem("plutonium_ingot");
    /** Fission products set in glass. Safe to carry (design rule 10); goes in the waste cask. */
    public static final DeferredItem<Item> FISSION_WASTE = ITEMS.registerSimpleItem("fission_waste");
    /** Fission reactor fuel: burns fast, and its spent rods reprocess into plutonium. */
    public static final DeferredItem<FuelRodItem> URANIUM_FUEL_ROD = ITEMS.registerItem("uranium_fuel_rod", FuelRodItem::new,
            new Item.Properties().stacksTo(16));
    /** Fission reactor fuel: uranium with a nugget of plutonium. Hotter, and lasts much longer. */
    public static final DeferredItem<FuelRodItem> MOX_FUEL_ROD = ITEMS.registerItem("mox_fuel_rod", FuelRodItem::new,
            new Item.Properties().stacksTo(16));
    /** What the fission station's fuel rods become. Safe to carry (rule 10); they go to reprocessing. */
    public static final DeferredItem<Item> SPENT_URANIUM_ROD = ITEMS.registerSimpleItem("spent_uranium_rod",
            new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> SPENT_MOX_ROD = ITEMS.registerSimpleItem("spent_mox_rod",
            new Item.Properties().stacksTo(16));
    /** Lithium for tritium (design section 10), from salt and water in the Lithium Extractor. */
    public static final DeferredItem<Item> LITHIUM_DUST = ITEMS.registerSimpleItem("lithium_dust");
    /** Lithium in an aluminium and steel rod, bred into tritium in a target channel of the station's core. */
    public static final DeferredItem<TargetRodItem> LITHIUM_TARGET_ROD = ITEMS.registerItem("lithium_target_rod", TargetRodItem::new,
            new Item.Properties().stacksTo(16));
    /** A bred target rod, full of tritium, ready for extraction. Safe to carry (rule 10). */
    public static final DeferredItem<Item> IRRADIATED_TARGET_ROD = ITEMS.registerSimpleItem("irradiated_target_rod",
            new Item.Properties().stacksTo(16));
    /** The fission station's brake: silver soaks up neutrons (tier 3's new material). */
    public static final DeferredItem<Item> CONTROL_ROD = ITEMS.registerSimpleItem("control_rod",
            new Item.Properties().stacksTo(16));
    /** Nine graphite: storage, and the fission station's moderator. */
    public static final DeferredItem<BlockItem> GRAPHITE_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.GRAPHITE_BLOCK);

    public static final DeferredItem<BlockItem> REACTOR_HEART = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_HEART);
    public static final DeferredItem<BlockItem> REACTOR_MACHINE_UNIT = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_MACHINE_UNIT);
    public static final DeferredItem<BlockItem> COOLANT_JACKET = ITEMS.registerSimpleBlockItem(ModBlocks.COOLANT_JACKET);

    public static final Map<OreType, DeferredItem<BlockItem>> STONE_ORES = new EnumMap<>(OreType.class);
    public static final Map<OreType, DeferredItem<BlockItem>> DEEPSLATE_ORES = new EnumMap<>(OreType.class);
    /** What each ore drops: raw metal, a gem or a dust. */
    public static final Map<OreType, DeferredItem<Item>> ORE_DROPS = new EnumMap<>(OreType.class);
    public static final Map<OreType, DeferredItem<Item>> INGOTS = new EnumMap<>(OreType.class);

    // Registration order is the creative tab order: ores, then drops, then ingots.
    static {
        for (OreType ore : OreType.values()) {
            STONE_ORES.put(ore, ITEMS.registerSimpleBlockItem(ModBlocks.STONE_ORES.get(ore)));
            DEEPSLATE_ORES.put(ore, ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_ORES.get(ore)));
        }
        for (OreType ore : OreType.values()) {
            ORE_DROPS.put(ore, ITEMS.registerSimpleItem(ore.dropName()));
        }
        for (OreType ore : OreType.values()) {
            if (ore.hasIngot()) {
                INGOTS.put(ore, ITEMS.registerSimpleItem(ore.ingotName()));
            }
        }
    }

    private ModItems() {}
}
