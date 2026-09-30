package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.material.OreType;
import com.ryzer.ryzergen.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        super(output, lookup, RyzerGen.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.ALLOY_SMELTER.get(), ModBlocks.ELECTRIC_ALLOY_SMELTER.get(), ModBlocks.ENERGY_CABLE.get(), ModBlocks.ITEM_PIPE.get(), ModBlocks.FLUID_PIPE.get(), ModBlocks.GAS_PIPE.get(), ModBlocks.INTAKE_PUMP.get(), ModBlocks.PRESSURE_TANK.get(), ModBlocks.FLUID_TANK.get(), ModBlocks.STATION_CORE.get(), ModBlocks.GRAPHITE_BLOCK.get(), ModBlocks.CREATIVE_BATTERY.get(), ModBlocks.CREATIVE_WATER_TANK.get(), ModBlocks.FUSION_PREVIEW.get(), ModBlocks.SUN_GATE_PREVIEW.get(), ModBlocks.STATION_CASING.get(), ModBlocks.STATION_GLASS.get(), ModBlocks.POOL_LINER.get(), ModBlocks.POOL_CRANE.get(), ModBlocks.POOL_CONTROLLER.get(), ModBlocks.CONTAINER_FRAME.get(), ModBlocks.THERMAL_UNIT.get(), ModBlocks.BATTERY_CONTROLLER.get(), ModBlocks.TURBINE_ROTOR.get(), ModBlocks.HOME_BATTERY.get(),
                ModBlocks.REACTOR_HEART.get(), ModBlocks.REACTOR_MACHINE_UNIT.get(), ModBlocks.COOLANT_JACKET.get(),
                ModBlocks.CORE_CRACKER.get(), ModBlocks.REPROCESSOR.get(), ModBlocks.FUEL_FABRICATOR.get(), ModBlocks.WASTE_CASK.get(),
                ModBlocks.LITHIUM_EXTRACTOR.get(), ModBlocks.ELECTROREFINER.get(), ModBlocks.BREEDER_CORE.get(),
                ModBlocks.BREEDER_FRAME.get(), ModBlocks.BREEDER_SHELL.get());

        for (OreType ore : OreType.values()) {
            Block stone = ModBlocks.STONE_ORES.get(ore).get();
            Block deepslate = ModBlocks.DEEPSLATE_ORES.get(ore).get();

            tag(ore.oreBlockTag()).add(stone, deepslate);
            tag(Tags.Blocks.ORES).addTag(ore.oreBlockTag());
            tag(Tags.Blocks.ORES_IN_GROUND_STONE).add(stone);
            tag(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE).add(deepslate);
            tag(ore.maxDrops() > 1 ? Tags.Blocks.ORE_RATES_DENSE : Tags.Blocks.ORE_RATES_SINGULAR).add(stone, deepslate);

            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(stone, deepslate);
            switch (ore.tool()) {
                case STONE -> tag(BlockTags.NEEDS_STONE_TOOL).add(stone, deepslate);
                case IRON -> tag(BlockTags.NEEDS_IRON_TOOL).add(stone, deepslate);
                case WOOD -> {}
            }
        }
    }
}
