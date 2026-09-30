package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.advancement.Milestone;
import com.ryzer.ryzergen.advancement.MilestoneTrigger;
import com.ryzer.ryzergen.material.OreType;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The Ryzer Gen advancement tab: the progression spine (design section 5) as a checklist. It walks
 * the player from their first lead or uranium to a running microreactor, and ends on the depleted
 * core, the hook into the fuel cycle, then walks the cycle itself to the first MOX rod. Titles and descriptions are in the lang file.
 */
public class ModAdvancementProvider extends AdvancementProvider {
    public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        super(output, lookup, existingFiles, List.of(ModAdvancementProvider::generate));
    }

    private static void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper existingFiles) {
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(ModItems.INGOTS.get(OreType.URANIUM).get(), title("root"), description("root"),
                        ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/block/microreactor/steel_dark.png"),
                        AdvancementType.TASK, false, false, false)
                .addCriterion("lead", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ItemPredicate.Builder.item().of(OreType.LEAD.dropTag())))
                .addCriterion("uranium", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ItemPredicate.Builder.item().of(OreType.URANIUM.dropTag())))
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(saver, id("root"));

        // Materials.
        AdvancementHolder alloySmelter = has(saver, root, "alloy_smelter", ModItems.ALLOY_SMELTER.get(), AdvancementType.TASK);
        AdvancementHolder steel = has(saver, alloySmelter, "steel", ModItems.STEEL_INGOT.get(), AdvancementType.TASK);
        has(saver, root, "graphite", ModItems.GRAPHITE.get(), AdvancementType.TASK);
        AdvancementHolder siliconCarbide = has(saver, alloySmelter, "silicon_carbide", ModItems.SILICON_CARBIDE.get(), AdvancementType.TASK);

        // Fuel.
        AdvancementHolder uranium = has(saver, root, "uranium", ModItems.ORE_DROPS.get(OreType.URANIUM).get(), AdvancementType.TASK);
        AdvancementHolder triso = has(saver, siliconCarbide, "triso_pellets", ModItems.TRISO_PELLETS.get(), AdvancementType.TASK);
        AdvancementHolder core = has(saver, triso, "sealed_fuel_core", ModItems.SEALED_FUEL_CORE.get(), AdvancementType.TASK);
        has(saver, uranium, "dosimeter_ring", ModItems.DOSIMETER_RING.get(), AdvancementType.TASK);
        has(saver, uranium, "geiger_counter", ModItems.GEIGER_COUNTER.get(), AdvancementType.TASK);

        // The microreactor.
        AdvancementHolder formed = milestone(saver, steel, "microreactor", ModItems.REACTOR_HEART.get(),
                Milestone.MICROREACTOR_FORMED, AdvancementType.GOAL, false);
        AdvancementHolder overdrive = milestone(saver, formed, "overdrive", ModItems.REACTOR_MACHINE_UNIT.get(),
                Milestone.SAFETIES_OFF, AdvancementType.TASK, false);
        milestone(saver, overdrive, "meltdown", ModItems.DEPLETED_FUEL_CORE.get(),
                Milestone.MELTDOWN, AdvancementType.CHALLENGE, true);
        AdvancementHolder depleted = has(saver, core, "depleted_fuel_core", ModItems.DEPLETED_FUEL_CORE.get(), AdvancementType.GOAL);

        // The fission station (design section 8): fuel rods, then the station itself.
        AdvancementHolder rod = has(saver, depleted, "uranium_fuel_rod", ModItems.URANIUM_FUEL_ROD.get(), AdvancementType.TASK);
        AdvancementHolder station = milestone(saver, rod, "station", ModItems.STATION_CORE.get(),
                Milestone.STATION_FORMED, AdvancementType.GOAL, false);
        milestone(saver, station, "station_perfect", ModItems.GRAPHITE_BLOCK.get(), Milestone.STATION_PERFECT, AdvancementType.CHALLENGE, false);
        AdvancementHolder stationOverdrive = milestone(saver, station, "station_overdrive", ModItems.CONTROL_ROD.get(),
                Milestone.STATION_OVERDRIVE, AdvancementType.TASK, false);
        milestone(saver, stationOverdrive, "station_meltdown", ModItems.SPENT_URANIUM_ROD.get(), Milestone.STATION_MELTDOWN,
                AdvancementType.CHALLENGE, true);

        // The fuel cycle (design section 7): crack the core, reprocess it, make MOX.
        AdvancementHolder kernels = has(saver, depleted, "spent_kernels", ModItems.SPENT_KERNELS.get(), AdvancementType.TASK);
        AdvancementHolder plutonium = has(saver, kernels, "plutonium", ModItems.PLUTONIUM_NUGGET.get(), AdvancementType.GOAL);
        has(saver, plutonium, "mox_fuel_rod", ModItems.MOX_FUEL_ROD.get(), AdvancementType.GOAL);
        milestone(saver, depleted, "spent_fuel_pool", ModItems.POOL_CRANE.get(), Milestone.POOL_FORMED, AdvancementType.TASK, false);

        // Using the power.
        has(saver, formed, "electric_alloy_smelter", ModItems.ELECTRIC_ALLOY_SMELTER.get(), AdvancementType.TASK);
        AdvancementHolder battery = has(saver, formed, "home_battery", ModItems.HOME_BATTERY.get(), AdvancementType.TASK);
        milestone(saver, battery, "battery_full", ModItems.LEAD_ACID_MODULE.get(), Milestone.BATTERY_FULL, AdvancementType.GOAL, false);
        milestone(saver, battery, "container_battery", ModItems.LFP_BATTERY_RACK.get(), Milestone.CONTAINER_FORMED, AdvancementType.GOAL, false);
    }

    /** Earned by having the item. */
    private static AdvancementHolder has(Consumer<AdvancementHolder> saver, AdvancementHolder parent, String name,
                                         ItemLike item, AdvancementType type) {
        return Advancement.Builder.advancement()
                .parent(parent)
                .display(item, title(name), description(name), null, type, true, true, false)
                .addCriterion("has", InventoryChangeTrigger.TriggerInstance.hasItems(item))
                .save(saver, id(name));
    }

    /** Earned by doing something (see {@link Milestone}). */
    private static AdvancementHolder milestone(Consumer<AdvancementHolder> saver, AdvancementHolder parent, String name,
                                               ItemLike icon, Milestone milestone, AdvancementType type, boolean hidden) {
        return Advancement.Builder.advancement()
                .parent(parent)
                .display(icon, title(name), description(name), null, type, true, true, hidden)
                .addCriterion(milestone.getSerializedName(), MilestoneTrigger.of(milestone))
                .save(saver, id(name));
    }

    private static String id(String name) {
        return RyzerGen.MOD_ID + ":main/" + name;
    }

    private static Component title(String name) {
        return Component.translatable("advancements.ryzergen." + name + ".title");
    }

    private static Component description(String name) {
        return Component.translatable("advancements.ryzergen." + name + ".description");
    }
}
