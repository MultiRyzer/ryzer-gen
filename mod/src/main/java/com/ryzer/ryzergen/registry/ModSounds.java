package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Our own sound events, so resource packs can replace them. See ModSoundProvider for what each one
 * plays: some borrow pitched vanilla sounds, the alarm is built by art/tools/alarm_sound.py.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, RyzerGen.MOD_ID);

    /** Looping hum while the microreactor runs. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MICROREACTOR_HUM = register("microreactor_hum");
    /** Coolant-loss alarm while an overdriven core runs away. Looped and pitched up by the client. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MICROREACTOR_ALARM = register("microreactor_alarm");
    /** One Geiger counter click from the dosimeter ring. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DOSIMETER_CLICK = register("dosimeter_click");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, name)));
    }

    private ModSounds() {}
}
