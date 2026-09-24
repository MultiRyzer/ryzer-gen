package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Run with {@code ./gradlew runData}. Output goes to src/generated/resources and is committed. */
@EventBusSubscriber(modid = RyzerGen.MOD_ID)
public final class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFiles = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new ModBlockStateProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new ModSoundProvider(output, existingFiles));

        generator.addProvider(event.includeServer(), new ModWorldGenProvider(output, lookup));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK)), lookup));
        ModBlockTagsProvider blockTags = generator.addProvider(event.includeServer(),
                new ModBlockTagsProvider(output, lookup, existingFiles));
        generator.addProvider(event.includeServer(),
                new ModItemTagsProvider(output, lookup, blockTags.contentsGetter(), existingFiles));
        generator.addProvider(event.includeServer(), new ModRecipeProvider(output, lookup));
    }

    private DataGenerators() {}
}
