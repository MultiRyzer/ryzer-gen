package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.material.ModTags;
import com.ryzer.ryzergen.material.OreType;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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
        tag(ModTags.INGOTS_STEEL).add(ModItems.STEEL_INGOT.get());
        tag(ModTags.DUSTS_LITHIUM).add(ModItems.LITHIUM_DUST.get());
        tag(Tags.Items.DUSTS).addTag(ModTags.DUSTS_LITHIUM);
        tag(ModTags.INGOTS_GRAPHITE).add(ModItems.GRAPHITE.get());
        tag(ModTags.GEMS_SILICON_CARBIDE).add(ModItems.SILICON_CARBIDE.get());
        tag(ModTags.MICROREACTOR_FUEL).add(ModItems.SEALED_FUEL_CORE.get());
        tag(ModTags.CIRCUITS_BASIC).add(ModItems.BASIC_CONTROL_BOARD.get());
        tag(ModTags.CIRCUITS_ADVANCED).add(ModItems.ADVANCED_CONTROL_BOARD.get());
        tag(ModTags.SILICON).add(ModItems.SILICON.get());
        tag(ModTags.STORAGE_BLOCKS_GRAPHITE).add(ModItems.GRAPHITE_BLOCK.get());
        tag(Tags.Items.STORAGE_BLOCKS).addTag(ModTags.STORAGE_BLOCKS_GRAPHITE);
        tag(ModTags.INGOTS_PLUTONIUM).add(ModItems.PLUTONIUM_INGOT.get());
        tag(ModTags.NUGGETS_PLUTONIUM).add(ModItems.PLUTONIUM_NUGGET.get());
        tag(Tags.Items.INGOTS).addTag(ModTags.INGOTS_PLUTONIUM);
        tag(Tags.Items.NUGGETS).addTag(ModTags.NUGGETS_PLUTONIUM);
        // Wearable in Accessories' ring slot (and Curios', for packs that use it). Harmless if neither is installed.
        tag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("accessories", "ring"))).add(ModItems.DOSIMETER_RING.get());
        tag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "ring"))).add(ModItems.DOSIMETER_RING.get());
        // The Geiger counter clips to a belt.
        tag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("accessories", "belt"))).add(ModItems.GEIGER_COUNTER.get());
        tag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "belt"))).add(ModItems.GEIGER_COUNTER.get());
        tag(Tags.Items.INGOTS).addTag(ModTags.INGOTS_STEEL);

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
