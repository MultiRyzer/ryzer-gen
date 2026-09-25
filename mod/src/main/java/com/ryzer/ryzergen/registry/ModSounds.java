package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Our own sound events, so resource packs can replace them. See ModSoundProvider for what each one
 * plays: some borrow pitched vanilla sounds, the rest are cut by art/tools/machine_sounds.py.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, RyzerGen.MOD_ID);

    /** Looping hum while the microreactor runs. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MICROREACTOR_HUM = register("microreactor_hum");
    /** Coolant-loss alarm while an overdriven core runs away. Looped and pitched up by the client. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MICROREACTOR_ALARM = register("microreactor_alarm");
    /** Fission station alarm: a flux tilt, then an unstable core. Looped and pitched up by the client. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STATION_ALARM = register("station_alarm");
    /** The fission station's turbine drone, following the rotor's speed. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STATION_HUM = register("station_hum");
    /** Fuel cycle machines, each looped by the client while it works. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CORE_CRACKER = register("core_cracker");
    public static final DeferredHolder<SoundEvent, SoundEvent> REPROCESSOR = register("reprocessor");
    public static final DeferredHolder<SoundEvent, SoundEvent> FUEL_FABRICATOR = register("fuel_fabricator");
    /** One Geiger counter click from the dosimeter ring. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DOSIMETER_CLICK = register("dosimeter_click");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, name)));
    }

    private ModSounds() {}
}
