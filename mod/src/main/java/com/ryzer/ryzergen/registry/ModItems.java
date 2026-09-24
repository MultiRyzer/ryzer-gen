package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.battery.BatteryChemistry;
import com.ryzer.ryzergen.battery.BatteryModuleItem;
import com.ryzer.ryzergen.item.WrenchItem;
import com.ryzer.ryzergen.radiation.DosimeterRingItem;
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
    public static final DeferredItem<BlockItem> FLUID_TANK = ITEMS.registerSimpleBlockItem(ModBlocks.FLUID_TANK);
    public static final DeferredItem<WrenchItem> WRENCH = ITEMS.registerItem("wrench", WrenchItem::new);
    public static final DeferredItem<BlockItem> HOME_BATTERY = ITEMS.registerSimpleBlockItem(ModBlocks.HOME_BATTERY);
    public static final DeferredItem<BatteryModuleItem> LEAD_ACID_MODULE = ITEMS.registerItem("lead_acid_module",
            properties -> new BatteryModuleItem(BatteryChemistry.LEAD_ACID, properties));
    public static final DeferredItem<DosimeterRingItem> DOSIMETER_RING = ITEMS.registerItem("dosimeter_ring", DosimeterRingItem::new);
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
