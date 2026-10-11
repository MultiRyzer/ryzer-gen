package com.ryzer.ryzergen.datagen;

import java.util.List;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

/** sounds.json. Some events borrow pitched vanilla sounds; the rest play our own loops. */
public class ModSoundProvider extends SoundDefinitionsProvider {
    public ModSoundProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, RyzerGen.MOD_ID, existingFiles);
    }

    /** Our recordings play at 65% of their levelled loudness (turned down 35% after play-testing). */
    private static final float RECORDING_VOLUME = 0.65F;

    @Override
    public void registerSounds() {
        // A short, dry tick: the note block's hi-hat pitched right up.
        add(ModSounds.DOSIMETER_CLICK, definition()
                .subtitle("subtitles.ryzergen.dosimeter_click")
                .with(sound(ResourceLocation.withDefaultNamespace("note/hat")).pitch(2.0F).volume(0.5F)));
        // A low, steady hum: the beacon's drone pitched well down.
        add(ModSounds.MICROREACTOR_HUM, definition()
                .subtitle("subtitles.ryzergen.microreactor_hum")
                .with(sound(ResourceLocation.withDefaultNamespace("block/beacon/ambient")).pitch(0.55F).volume(0.8F)));
        // The mega drill: the beacon powering up, slowed, as the charge runs down the shaft; then our own
        // recordings (art/tools/machine_sounds.py): an energy beam's shot as the beam fires, cut at three
        // playback speeds so a faster drill's shot fits its shorter heating, and a boulder's impact as a
        // layer bursts.
        add(ModSounds.MEGA_DRILL_CHARGE, definition()
                .subtitle("subtitles.ryzergen.mega_drill_charge")
                .with(sound(ResourceLocation.withDefaultNamespace("block/beacon/activate")).pitch(0.7F).attenuationDistance(48)));
        for (var fire : List.of(ModSounds.MEGA_DRILL_FIRE, ModSounds.MEGA_DRILL_FIRE_X2, ModSounds.MEGA_DRILL_FIRE_X4)) {
            add(fire, definition()
                    .subtitle("subtitles.ryzergen.mega_drill_fire")
                    .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, fire.getId().getPath())).attenuationDistance(48)));
        }
        add(ModSounds.MEGA_DRILL_BURST, definition()
                .subtitle("subtitles.ryzergen.mega_drill_burst")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "mega_drill_burst")).attenuationDistance(48)));
        // Our own recordings are cut into loops by art/tools/machine_sounds.py, which also credits them.
        // The coolant-loss alarm: sixteen beeps of a depressurisation alarm. The client loops it and
        // raises the pitch towards meltdown. Heard from 64 blocks, so a runaway core is hard to miss.
        add(ModSounds.MICROREACTOR_ALARM, definition()
                .subtitle("subtitles.ryzergen.microreactor_alarm")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "microreactor_alarm")).volume(RECORDING_VOLUME).attenuationDistance(64)));
        // The fission station's alarm: a klaxon, heard from further away.
        add(ModSounds.STATION_ALARM, definition()
                .subtitle("subtitles.ryzergen.station_alarm")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "station_alarm")).volume(RECORDING_VOLUME).attenuationDistance(96)));
        // The station's turbine: a windy drone, heard across a base.
        add(ModSounds.STATION_HUM, definition()
                .subtitle("subtitles.ryzergen.station_hum")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "station_hum")).volume(RECORDING_VOLUME).attenuationDistance(48)));
        // The fuel cycle machines at work: grinding, a pump, a hydraulic press.
        add(ModSounds.CORE_CRACKER, definition()
                .subtitle("subtitles.ryzergen.core_cracker")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "core_cracker")).volume(RECORDING_VOLUME)));
        add(ModSounds.REPROCESSOR, definition()
                .subtitle("subtitles.ryzergen.reprocessor")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "reprocessor")).volume(RECORDING_VOLUME)));
        add(ModSounds.FUEL_FABRICATOR, definition()
                .subtitle("subtitles.ryzergen.fuel_fabricator")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "fuel_fabricator")).volume(RECORDING_VOLUME)));
        // Pumping brine: the reprocessor's pump until the extractor has a recording of its own.
        add(ModSounds.LITHIUM_EXTRACTOR, definition()
                .subtitle("subtitles.ryzergen.lithium_extractor")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "reprocessor")).volume(RECORDING_VOLUME * 0.8F).pitch(1.15F)));
        // The cell's current: the turbine's drone, higher and quieter, as an electrical hum until the
        // Electrorefiner has a recording of its own.
        add(ModSounds.ELECTROREFINER, definition()
                .subtitle("subtitles.ryzergen.electrorefiner")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "station_hum")).volume(RECORDING_VOLUME * 0.5F).pitch(1.6F)));
            // The reactor announcer, processed by art/tools/voice_lines.py.
        add(ModSounds.VOICE_REACTOR_ONLINE, definition()
                .subtitle("subtitles.ryzergen.voice_reactor_online")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/reactor_online"))));
        add(ModSounds.VOICE_STATION_ONLINE, definition()
                .subtitle("subtitles.ryzergen.voice_station_online")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/station_online"))));
        add(ModSounds.VOICE_SAFETIES_ON, definition()
                .subtitle("subtitles.ryzergen.voice_safeties_on")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/safeties_on"))));
        add(ModSounds.VOICE_CONTROLLED_SHUTDOWN, definition()
                .subtitle("subtitles.ryzergen.voice_controlled_shutdown")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/controlled_shutdown"))));
        add(ModSounds.VOICE_SAFETIES_OFF, definition()
                .subtitle("subtitles.ryzergen.voice_safeties_off")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/safeties_off"))));
        add(ModSounds.VOICE_OVERHEAT, definition()
                .subtitle("subtitles.ryzergen.voice_overheat")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/overheat"))));
        add(ModSounds.VOICE_FLUX_TILT, definition()
                .subtitle("subtitles.ryzergen.voice_flux_tilt")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/flux_tilt"))));
        add(ModSounds.VOICE_SCRAM, definition()
                .subtitle("subtitles.ryzergen.voice_scram")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/scram"))));
        add(ModSounds.VOICE_COOLANT_LOSS, definition()
                .subtitle("subtitles.ryzergen.voice_coolant_loss")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/coolant_loss"))));
        add(ModSounds.VOICE_MELTDOWN_RISK, definition()
                .subtitle("subtitles.ryzergen.voice_meltdown_risk")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/meltdown_risk"))));
        add(ModSounds.VOICE_NO_FUEL, definition()
                .subtitle("subtitles.ryzergen.voice_no_fuel")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/no_fuel"))));
        add(ModSounds.VOICE_STEADY_STATE_1, definition()
                .subtitle("subtitles.ryzergen.voice_steady_state_1")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/steady_state_1"))));
        add(ModSounds.VOICE_STEADY_STATE_2, definition()
                .subtitle("subtitles.ryzergen.voice_steady_state_2")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/steady_state_2"))));
        add(ModSounds.VOICE_STEADY_STATE_3, definition()
                .subtitle("subtitles.ryzergen.voice_steady_state_3")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/steady_state_3"))));
        add(ModSounds.VOICE_BREEDER_ONLINE, definition()
                .subtitle("subtitles.ryzergen.voice_breeder_online")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/breeder_online"))));
        add(ModSounds.VOICE_SODIUM_LOW, definition()
                .subtitle("subtitles.ryzergen.voice_sodium_low")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/sodium_low"))));
        add(ModSounds.VOICE_HOT_SPOT, definition()
                .subtitle("subtitles.ryzergen.voice_hot_spot")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/hot_spot"))));
        add(ModSounds.VOICE_SODIUM_FIRE, definition()
                .subtitle("subtitles.ryzergen.voice_sodium_fire")
                .with(sound(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "voice/sodium_fire"))));
    }
}
