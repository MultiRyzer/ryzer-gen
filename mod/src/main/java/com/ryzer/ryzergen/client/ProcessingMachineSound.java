package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.machine.processing.ProcessingBlock;
import com.ryzer.ryzergen.machine.processing.ProcessingBlockEntity;
import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A fuel cycle machine's working sound, looped while it runs (the block's ACTIVE state) and
 * stopped when it pauses or is broken. Looping on the client keeps the server from sending a
 * sound every few ticks.
 */
public class ProcessingMachineSound extends AbstractTickableSoundInstance {
    private final ProcessingBlockEntity machine;

    private ProcessingMachineSound(ProcessingBlockEntity machine) {
        super(sound(machine.machine()), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.machine = machine;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.8F;
        BlockPos pos = machine.getBlockPos();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + (machine.machine().tall() ? 1.0 : 0.5);
        this.z = pos.getZ() + 0.5;
    }

    private static SoundEvent sound(ProcessingMachine machine) {
        return switch (machine) {
            case CORE_CRACKER -> ModSounds.CORE_CRACKER.get();
            case REPROCESSOR -> ModSounds.REPROCESSOR.get();
            case FUEL_FABRICATOR -> ModSounds.FUEL_FABRICATOR.get();
            case LITHIUM_EXTRACTOR -> ModSounds.LITHIUM_EXTRACTOR.get();
        };
    }

    @Override
    public void tick() {
        if (machine.isRemoved() || !machine.getBlockState().getValue(ProcessingBlock.ACTIVE)) {
            stop();
        }
    }

    /** Client ticker: starts the loop when the machine starts working and none is playing. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, ProcessingBlockEntity machine) {
        machine.turn(state.getValue(ProcessingBlock.ACTIVE));
        if (!state.getValue(ProcessingBlock.ACTIVE)) {
            return;
        }
        // A sound just started can read as inactive for a moment, so only re-check now and then.
        if (machine.clientSound instanceof ProcessingMachineSound sound && !sound.isStopped()
                && (level.getGameTime() % 40 != 0 || Minecraft.getInstance().getSoundManager().isActive(sound))) {
            return;
        }
        ProcessingMachineSound sound = new ProcessingMachineSound(machine);
        machine.clientSound = sound;
        Minecraft.getInstance().getSoundManager().play(sound);
    }
}
