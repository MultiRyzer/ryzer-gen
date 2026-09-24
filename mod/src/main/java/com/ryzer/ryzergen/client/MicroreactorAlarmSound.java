package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlockEntity;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import static com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlockEntity.MAX_TEMPERATURE;
import static com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlockEntity.OVERDRIVE_TEMPERATURE;

/**
 * The coolant-loss alarm. It loops one beep cycle and raises the pitch as the core heats towards
 * meltdown, so the beeping speeds up and the tone bends upwards together, faster and faster until
 * the explosion. Stops by itself when the coolant loss is over.
 */
public class MicroreactorAlarmSound extends AbstractTickableSoundInstance {
    /** Pitch at the start of the runaway, and at meltdown (Minecraft's limit). */
    private static final float START_PITCH = 0.7F;
    private static final float MELTDOWN_PITCH = 2.0F;

    private final ReactorHeartBlockEntity heart;

    private MicroreactorAlarmSound(ReactorHeartBlockEntity heart) {
        super(ModSounds.MICROREACTOR_ALARM.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.heart = heart;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.pitch = START_PITCH;
        BlockPos pos = heart.getBlockPos();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
    }

    @Override
    public void tick() {
        if (heart.isRemoved() || heart.status() != ReactorHeartBlockEntity.Status.COOLANT_LOSS) {
            stop();
            return;
        }
        float progress = Mth.clamp((heart.temperature() - OVERDRIVE_TEMPERATURE) / (float) (MAX_TEMPERATURE - OVERDRIVE_TEMPERATURE), 0, 1);
        // Eased so it speeds up hardest in the last seconds.
        pitch = START_PITCH + (MELTDOWN_PITCH - START_PITCH) * progress * progress;
    }

    /** Starts the alarm when the heart reports a coolant loss and none is playing. */
    public static void update(ReactorHeartBlockEntity heart) {
        if (heart.status() != ReactorHeartBlockEntity.Status.COOLANT_LOSS) {
            return;
        }
        if (heart.clientAlarm instanceof MicroreactorAlarmSound alarm && !alarm.isStopped()) {
            return;
        }
        MicroreactorAlarmSound alarm = new MicroreactorAlarmSound(heart);
        heart.clientAlarm = alarm;
        Minecraft.getInstance().getSoundManager().play(alarm);
    }
}
