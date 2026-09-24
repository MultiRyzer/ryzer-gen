package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

/** sounds.json. Our events borrow pitched vanilla sounds until the mod has audio of its own. */
public class ModSoundProvider extends SoundDefinitionsProvider {
    public ModSoundProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, RyzerGen.MOD_ID, existingFiles);
    }

    @Override
    public void registerSounds() {
        // A low, steady hum: the beacon's drone pitched well down.
        add(ModSounds.MICROREACTOR_HUM, definition()
                .subtitle("subtitles.ryzergen.microreactor_hum")
                .with(sound(ResourceLocation.withDefaultNamespace("block/beacon/ambient")).pitch(0.55F).volume(0.8F)));
        // One beep cycle cut from a nuclear alarm (art/tools/alarm_sound.py). The client loops it and
        // raises the pitch towards meltdown. Heard from 64 blocks, so a runaway core is hard to miss.
        add(ModSounds.MICROREACTOR_ALARM, definition()
                .subtitle("subtitles.ryzergen.microreactor_alarm")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "microreactor_alarm")).attenuationDistance(64)));
    }
}
