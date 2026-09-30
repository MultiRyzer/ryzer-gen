package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.Preview;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.material.OreType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RyzerGen.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ryzergen"))
                    .icon(() -> ModItems.GRAPHITE.get().getDefaultInstance())
                    .displayItems((params, output) -> ordered().stream()
                            .map(Item::getDefaultInstance)
                            .filter(stack -> !Preview.hidden(stack))
                            .forEach(output::accept))
                    .build());

    private ModCreativeTabs() {}

    /**
     * The tab grouped by type, as vanilla's tabs are: tools, machines and multiblock parts, pipes and
     * storage, upgrades, fuel and rods, then ore blocks, raw ores, ingots, other materials and
     * boards, and last the creative and preview items. Within a type, things run in tier order. An
     * item missing from the list still shows, near the end, so a new one is never lost; give it a
     * place here.
     */
    private static Set<Item> ordered() {
        Set<Item> items = new LinkedHashSet<>();
        List<List<DeferredItem<? extends Item>>> types = List.of(
                // Tools
                List.of(ModItems.WRENCH, ModItems.FLOW_SCANNER, ModItems.GEIGER_COUNTER, ModItems.DOSIMETER_RING),
                // Machines and multiblock parts
                List.of(ModItems.REACTOR_HEART, ModItems.REACTOR_MACHINE_UNIT, ModItems.COOLANT_JACKET,
                        ModItems.ALLOY_SMELTER, ModItems.ELECTRIC_ALLOY_SMELTER, ModItems.INTAKE_PUMP,
                        ModItems.CORE_CRACKER, ModItems.REPROCESSOR, ModItems.FUEL_FABRICATOR, ModItems.LITHIUM_EXTRACTOR, ModItems.ELECTROREFINER,
                        ModItems.POOL_CONTROLLER, ModItems.POOL_LINER, ModItems.POOL_CRANE,
                        ModItems.BATTERY_CONTROLLER, ModItems.CONTAINER_FRAME, ModItems.THERMAL_UNIT,
                        ModItems.STATION_CORE, ModItems.STATION_CASING, ModItems.STATION_GLASS, ModItems.TURBINE_ROTOR),
                // Pipes and cables
                List.of(ModItems.ENERGY_CABLE, ModItems.ITEM_PIPE, ModItems.FLUID_PIPE, ModItems.GAS_PIPE),
                // Storage
                List.of(ModItems.HOME_BATTERY, ModItems.FLUID_TANK, ModItems.PRESSURE_TANK, ModItems.WASTE_CASK),
                // Upgrades and modules
                List.of(ModItems.LEAD_ACID_MODULE, ModItems.LFP_BATTERY_RACK, ModItems.SPEED_MODULE,
                        ModItems.SILVER_FITTINGS, ModItems.BUSBAR_FITTINGS, ModItems.CRYOGENIC_FITTINGS),
                // Fuel and rods
                List.of(ModItems.TRISO_PELLETS, ModItems.SEALED_FUEL_CORE, ModItems.DEPLETED_FUEL_CORE,
                        ModItems.URANIUM_FUEL_ROD, ModItems.MOX_FUEL_ROD, ModItems.SPENT_URANIUM_ROD, ModItems.SPENT_MOX_ROD,
                        ModItems.CONTROL_ROD, ModItems.LITHIUM_TARGET_ROD, ModItems.IRRADIATED_TARGET_ROD));
        types.forEach(type -> type.forEach(item -> items.add(item.get())));
        // Ore blocks (stone, then deepslate), raw ores, then ingots.
        for (OreType ore : OreType.values()) {
            add(items, ModItems.STONE_ORES.get(ore));
        }
        for (OreType ore : OreType.values()) {
            add(items, ModItems.DEEPSLATE_ORES.get(ore));
        }
        for (OreType ore : OreType.values()) {
            add(items, ModItems.ORE_DROPS.get(ore));
        }
        for (OreType ore : OreType.values()) {
            add(items, ModItems.INGOTS.get(ore));
        }
        List.of(ModItems.STEEL_INGOT, ModItems.PLUTONIUM_INGOT, ModItems.PLUTONIUM_NUGGET, ModItems.SODIUM_INGOT, ModItems.TRANSURANIC_METAL,
                // Other materials, then blocks made of them, then circuit boards
                ModItems.GRAPHITE, ModItems.SILICON, ModItems.SILICON_CARBIDE, ModItems.LITHIUM_DUST,
                ModItems.SPENT_KERNELS, ModItems.FISSION_WASTE, ModItems.GRAPHITE_BLOCK,
                ModItems.BASIC_CONTROL_BOARD, ModItems.ADVANCED_CONTROL_BOARD).forEach(item -> items.add(item.get()));
        // Anything not placed above, then the creative and preview items last.
        List<DeferredItem<? extends Item>> last = List.of(ModItems.CREATIVE_BATTERY, ModItems.CREATIVE_WATER_TANK,
                ModItems.SWARM_CONTROLLER, ModItems.FUSION_PREVIEW, ModItems.SUN_GATE_PREVIEW);
        Set<Item> lastItems = new LinkedHashSet<>();
        last.forEach(item -> lastItems.add(item.get()));
        ModItems.ITEMS.getEntries().forEach(entry -> {
            if (!lastItems.contains(entry.get())) {
                items.add(entry.get());
            }
        });
        items.addAll(lastItems);
        return items;
    }

    private static void add(Set<Item> items, DeferredItem<? extends Item> item) {
        if (item != null) {
            items.add(item.get());
        }
    }
}
