package com.ryzer.ryzergen.client.terminal;

import com.ryzer.ryzergen.machine.fission.StationReactor.Channel;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The control screen's sounds (synthesised by art/tools/console_sounds.py, after the reference page's
 * Web Audio recipes): played on the client only, for the player at the screen. Notes climb a
 * pentatonic scale from C4; the game pitches a sample only an octave either way, so the scale is
 * two samples, C5 and C6. A short queue plays the notes of a run (the stars) on time.
 */
public final class ConsoleSounds {
    private static final int[] SCALE = {0, 2, 4, 7, 9};
    /** Queued sounds: when (ms), the sound, its pitch and volume. */
    private record Queued(long at, Supplier<SoundEvent> sound, float pitch, float volume) {}

    private final List<Queued> queue = new ArrayList<>();
    private boolean muted;

    public void mute(boolean muted) {
        this.muted = muted;
    }

    private void play(Supplier<SoundEvent> sound, float pitch, float volume, int delayMs) {
        if (delayMs <= 0) {
            if (!muted) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound.get(), pitch, volume));
            }
        } else {
            queue.add(new Queued(System.currentTimeMillis() + delayMs, sound, pitch, volume));
        }
    }

    /** Plays what is due. Call every frame. */
    public void update() {
        long now = System.currentTimeMillis();
        for (int i = queue.size() - 1; i >= 0; i--) {
            Queued q = queue.get(i);
            if (q.at() <= now) {
                queue.remove(i);
                play(q.sound(), q.pitch(), q.volume(), 0);
            }
        }
    }

    public void tick() {
        play(ModSounds.CONSOLE_TICK, 1, 0.35F, 0);
    }

    public void place(Channel part) {
        play(switch (part) {
            case FUEL -> ModSounds.CONSOLE_PLACE_FUEL;
            case MODERATOR -> ModSounds.CONSOLE_PLACE_GRAPHITE;
            case COOLANT -> ModSounds.CONSOLE_PLACE_WATER;
            default -> ModSounds.CONSOLE_CLEAR;
        }, 1, 0.6F, 0);
    }

    /** Step {@code k} of the pentatonic scale from C4 (5 steps an octave). */
    public void note(int k, float volume, int delayMs) {
        int semitones = Math.floorDiv(k, 5) * 12 + SCALE[Math.floorMod(k, 5)];
        if (semitones <= 24) {
            play(ModSounds.CONSOLE_NOTE, (float) Math.pow(2, (semitones - 12) / 12.0), volume, delayMs);
        } else {
            play(ModSounds.CONSOLE_NOTE_HIGH, (float) Math.pow(2, (semitones - 24) / 12.0), volume, delayMs);
        }
    }

    public void down() {
        play(ModSounds.CONSOLE_DOWN, 1, 0.5F, 0);
    }

    public void buzz() {
        play(ModSounds.CONSOLE_BUZZ, 1, 0.5F, 0);
    }

    /** A star won: a chime per star, climbing. */
    public void star(int stars) {
        for (int k = 0; k < stars; k++) {
            note(10 + k * 2, 0.7F, k * 90);
        }
    }

    public void fanfare() {
        play(ModSounds.CONSOLE_FANFARE, 1, 0.8F, 0);
    }

    public void inject() {
        play(ModSounds.CONSOLE_INJECT, 1, 0.7F, 0);
    }

    public void eject() {
        play(ModSounds.CONSOLE_EJECT, 1, 0.7F, 0);
    }
}
