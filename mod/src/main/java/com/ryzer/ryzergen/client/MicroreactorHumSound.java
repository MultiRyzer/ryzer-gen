package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.machine.microreactor.MicroreactorPartBlock;
import com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlockEntity;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The looping hum of a running microreactor. Stops by itself when the reactor stops or is broken. */
public class MicroreactorHumSound extends AbstractTickableSoundInstance {
    private final ReactorHeartBlockEntity heart;

    private MicroreactorHumSound(ReactorHeartBlockEntity heart) {
        super(ModSounds.MICROREACTOR_HUM.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.heart = heart;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.7F;
        BlockPos pos = heart.getBlockPos();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
    }

    @Override
    public void tick() {
        if (heart.isRemoved() || !heart.getBlockState().getValue(MicroreactorPartBlock.RUNNING)) {
            stop();
        }
    }

    /** Client ticker for the heart: the placement guide, the alarm, and the hum while running. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, ReactorHeartBlockEntity heart) {
        MicroreactorAlarmSound.update(heart);
        if (!state.getValue(MicroreactorPartBlock.SLOT).isFormed()) {
            MicroreactorGhostPreview.track(heart);
        }
        if (!state.getValue(MicroreactorPartBlock.RUNNING)) {
            return;
        }
        if (heart.clientHum instanceof MicroreactorHumSound hum && !hum.isStopped()
                && (level.getGameTime() % 40 != 0 || Minecraft.getInstance().getSoundManager().isActive(hum))) {
            return;
        }
        MicroreactorHumSound hum = new MicroreactorHumSound(heart);
        heart.clientHum = hum;
        Minecraft.getInstance().getSoundManager().play(hum);
    }
}
