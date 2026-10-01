package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.registry.ModFluids;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/** Steam is {@code c:steam} and {@code c:gaseous}, so other mods' steam machines and gas tanks take it. */
public class ModFluidTagsProvider extends FluidTagsProvider {
    public ModFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        super(output, lookup, RyzerGen.MOD_ID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModFluids.STEAM_TAG).add(ModFluids.STEAM.get(), ModFluids.FLOWING_STEAM.get());
        tag(ModFluids.SODIUM_TAG).add(ModFluids.SODIUM.get(), ModFluids.FLOWING_SODIUM.get());
        tag(Tags.Fluids.GASEOUS).add(ModFluids.STEAM.get(), ModFluids.FLOWING_STEAM.get());
    }
}
