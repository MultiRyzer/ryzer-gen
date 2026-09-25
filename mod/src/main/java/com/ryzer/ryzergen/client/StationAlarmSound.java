package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.machine.fission.StationCoreBlockEntity;
import com.ryzer.ryzergen.machine.fission.StationRunner;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * The fission station's klaxon. During a flux tilt it sounds slow and low, a warning with time to
 * act; once the core is unstable the pitch climbs with the heat, faster and faster until the
 * meltdown. Stops by itself when the danger is over.
 */
public class StationAlarmSound extends AbstractTickableSoundInstance {
    private static final float TILT_PITCH = 0.8F;
    /** Pitch as an unstable core passes 600°C, and at meltdown (Minecraft's limit). */
    private static final float UNSTABLE_PITCH = 0.9F;
    private static final float MELTDOWN_PITCH = 2.0F;
    private static final float UNSTABLE_FROM = 600;

    private final StationCoreBlockEntity core;

    private StationAlarmSound(StationCoreBlockEntity core) {
        super(ModSounds.STATION_ALARM.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.core = core;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.5F;
        this.pitch = TILT_PITCH;
        BlockPos pos = core.getBlockPos();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 2.5;
        this.z = pos.getZ() + 0.5;
    }

    @Override
    public void tick() {
        StationRunner.Status status = core.runner().status();
        if (core.isRemoved() || !status.alarm()) {
            stop();
            return;
        }
        if (status.critical()) {
            float progress = Mth.clamp((core.runner().temperature() - UNSTABLE_FROM)
                    / (StationRunner.MELTDOWN_TEMPERATURE - UNSTABLE_FROM), 0, 1);
            // Eased so it speeds up hardest in the last seconds.
            pitch = UNSTABLE_PITCH + (MELTDOWN_PITCH - UNSTABLE_PITCH) * progress * progress;
        } else {
            pitch = TILT_PITCH;
        }
    }

    /** Starts the alarm when the core reports danger and none is playing. */
    public static void update(StationCoreBlockEntity core) {
        if (!core.runner().status().alarm()) {
            return;
        }
        if (core.clientAlarm instanceof StationAlarmSound alarm && !alarm.isStopped()) {
            return;
        }
        StationAlarmSound alarm = new StationAlarmSound(core);
        core.clientAlarm = alarm;
        Minecraft.getInstance().getSoundManager().play(alarm);
    }
}
