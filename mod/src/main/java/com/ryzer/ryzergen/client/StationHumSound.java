package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.machine.fission.StationCoreBlockEntity;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * The fission station's turbine: a windy drone that follows the rotor. It winds up in pitch and
 * volume as the turbine spins up, and winds down with it when the station stops, then goes quiet.
 */
public class StationHumSound extends AbstractTickableSoundInstance {
    /** Below this the rotor has all but stopped, and the hum ends. */
    private static final float QUIET = 0.03F;

    private final StationCoreBlockEntity core;

    private StationHumSound(StationCoreBlockEntity core) {
        super(ModSounds.STATION_HUM.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.core = core;
        this.looping = true;
        this.delay = 0;
        // At the turbine: the middle of the footprint (centre() gives x and z), six blocks up.
        double[] centre = core.centre();
        this.x = centre[0];
        this.y = core.getBlockPos().getY() + 6.5;
        this.z = centre[1];
        follow();
    }

    private void follow() {
        float spin = core.spin();
        volume = 0.2F + 1.3F * spin;
        pitch = 0.6F + 0.4F * spin;
    }

    @Override
    public void tick() {
        if (core.isRemoved() || !core.isFormed() || core.spin() < QUIET) {
            stop();
            return;
        }
        follow();
    }

    /** Starts the hum when the rotor turns and none is playing. */
    public static void update(StationCoreBlockEntity core) {
        if (core.spin() < QUIET) {
            return;
        }
        // A sound just started can read as inactive for a moment, so only re-check now and then.
        if (core.clientHum instanceof StationHumSound hum && !hum.isStopped()
                && (core.getLevel() == null || core.getLevel().getGameTime() % 40 != 0
                || Minecraft.getInstance().getSoundManager().isActive(hum))) {
            return;
        }
        StationHumSound hum = new StationHumSound(core);
        core.clientHum = hum;
        Minecraft.getInstance().getSoundManager().play(hum);
    }
}
