package com.ryzer.ryzergen.machine;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A reactor's voice: short spoken lines from its control system when something worth hearing
 * happens (art/sounds/VOICE-SCRIPT.md). Each machine keeps one, not saved: it remembers when each
 * line last played, so none repeats within its cooldown, and what it is saying now, so a routine
 * line never talks over a more urgent one.
 *
 * <p>Lines play on the Voice/Speech sound category, so each player can turn the announcer down or
 * off with the vanilla slider; {@code announcer.enabled} in the common config turns it off for
 * everyone.
 */
public final class Announcer {
    public enum Line {
        REACTOR_ONLINE(ModSounds.VOICE_REACTOR_ONLINE, 0, 1.6F),
        STATION_ONLINE(ModSounds.VOICE_STATION_ONLINE, 0, 3.0F),
        SAFETIES_ON(ModSounds.VOICE_SAFETIES_ON, 0, 1.4F),
        SAFETIES_OFF(ModSounds.VOICE_SAFETIES_OFF, 1, 4.8F),
        OVERHEAT(ModSounds.VOICE_OVERHEAT, 1, 2.7F),
        FLUX_TILT(ModSounds.VOICE_FLUX_TILT, 1, 2.3F),
        SCRAM(ModSounds.VOICE_SCRAM, 2, 3.2F),
        COOLANT_LOSS(ModSounds.VOICE_COOLANT_LOSS, 2, 1.9F),
        MELTDOWN_RISK(ModSounds.VOICE_MELTDOWN_RISK, 3, 4.5F);

        private final DeferredHolder<SoundEvent, SoundEvent> sound;
        private final int priority;
        private final int ticks;

        Line(DeferredHolder<SoundEvent, SoundEvent> sound, int priority, float seconds) {
            this.sound = sound;
            this.priority = priority;
            this.ticks = Math.round(seconds * 20);
        }
    }

    /** No line repeats sooner than this after it last played. */
    private static final long COOLDOWN = 20 * 20;
    /** Heard up to about this far away (vanilla scales the fade by volumes above 1). */
    private static final float VOLUME = 2.5F;

    private final long[] lastPlayed = new long[Line.values().length];
    private long busyUntil;
    private int busyPriority = -1;

    public Announcer() {
        java.util.Arrays.fill(lastPlayed, Long.MIN_VALUE / 2);
    }

    /** Says {@code line} at {@code at}, unless it spoke too recently or something more urgent is playing. */
    public void say(ServerLevel level, Vec3 at, Line line) {
        if (!Config.ANNOUNCER_ENABLED.get()) {
            return;
        }
        long now = level.getGameTime();
        if (now - lastPlayed[line.ordinal()] < COOLDOWN) {
            return;
        }
        if (now < busyUntil && line.priority <= busyPriority) {
            return;
        }
        lastPlayed[line.ordinal()] = now;
        busyUntil = now + line.ticks;
        busyPriority = line.priority;
        level.playSound(null, at.x, at.y, at.z, line.sound.get(), SoundSource.VOICE, VOLUME, 1.0F);
    }
}
