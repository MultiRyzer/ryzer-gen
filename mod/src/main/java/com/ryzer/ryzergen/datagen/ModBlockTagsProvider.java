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
