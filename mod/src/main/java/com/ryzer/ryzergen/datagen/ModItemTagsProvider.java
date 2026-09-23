package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.material.OreType;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModItemTagsProvider extends ItemTagsProvider {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                               CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFiles) {
        super(output, lookup, blockTags, RyzerGen.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (OreType ore : OreType.values()) {
            copy(ore.oreBlockTag(), ore.oreItemTag());

            tag(ore.dropTag()).add(ModItems.ORE_DROPS.get(ore).get());
            tag(switch (ore.drop()) {
                case RAW -> Tags.Items.RAW_MATERIALS;
                case GEM -> Tags.Items.GEMS;
                case DUST -> Tags.Items.DUSTS;
            }).addTag(ore.dropTag());

            if (ore.hasIngot()) {
                tag(ore.ingotTag()).add(ModItems.INGOTS.get(ore).get());
                tag(Tags.Items.INGOTS).addTag(ore.ingotTag());
            }
        }
        copy(Tags.Blocks.ORES, Tags.Items.ORES);
        copy(Tags.Blocks.ORES_IN_GROUND_STONE, Tags.Items.ORES_IN_GROUND_STONE);
        copy(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE, Tags.Items.ORES_IN_GROUND_DEEPSLATE);
        copy(Tags.Blocks.ORE_RATES_SINGULAR, Tags.Items.ORE_RATES_SINGULAR);
        copy(Tags.Blocks.ORE_RATES_DENSE, Tags.Items.ORE_RATES_DENSE);
    }
}
