package com.ryzer.ryzergen.machine.microreactor;

import com.ryzer.ryzergen.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Forms and breaks the 4-block microreactor: 1 heart, 2 machine units and 1 coolant jacket,
 * 1 wide, 2 deep and 2 high. The heart can sit in any slot and sets which way the machine faces.
 */
public final class MicroreactorStructure {
    private MicroreactorStructure() {}

    /** Called a tick after any part is placed. Forms the structure if a complete layout is found. */
    public static void tryForm(ServerLevel level, BlockPos placed) {
        // The heart is at most one block away along each axis from any other part.
        for (BlockPos pos : BlockPos.betweenClosed(placed.offset(-1, -1, -1), placed.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.REACTOR_HEART.get()) || state.getValue(MicroreactorPartBlock.SLOT).isFormed()) {
                continue;
            }
            Direction facing = state.getValue(MicroreactorPartBlock.FACING);
            for (MicroreactorSlot heartSlot : MicroreactorSlot.FORMED) {
                BlockPos origin = heartSlot.toOrigin(pos, facing);
                if (isComplete(level, origin, facing)) {
                    form(level, origin, facing);
                    return;
                }
            }
        }
    }

    private static boolean isComplete(ServerLevel level, BlockPos origin, Direction facing) {
        int hearts = 0;
        int units = 0;
        int coolant = 0;
        for (MicroreactorSlot slot : MicroreactorSlot.FORMED) {
            BlockState state = level.getBlockState(slot.fromOrigin(origin, facing));
            if (!(state.getBlock() instanceof MicroreactorPartBlock) || state.getValue(MicroreactorPartBlock.SLOT).isFormed()) {
                return false;
            }
            if (state.is(ModBlocks.REACTOR_HEART.get())) {
                hearts++;
            } else if (state.is(ModBlocks.REACTOR_MACHINE_UNIT.get())) {
                units++;
            } else if (state.is(ModBlocks.COOLANT_JACKET.get())) {
                coolant++;
            }
        }
        return hearts == 1 && units == 2 && coolant == 1;
    }

    private static void form(ServerLevel level, BlockPos origin, Direction facing) {
        for (MicroreactorSlot slot : MicroreactorSlot.FORMED) {
            BlockPos pos = slot.fromOrigin(origin, facing);
            BlockState state = level.getBlockState(pos)
                    .setValue(MicroreactorPartBlock.FACING, facing)
                    .setValue(MicroreactorPartBlock.SLOT, slot);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
        refreshPorts(level, origin, facing);
        Vec3 centre = centre(origin, facing);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.35F, 1.4F);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.5F, 1.6F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, 30, 0.45, 0.8, 0.8, 0.05);
        level.sendParticles(ParticleTypes.CLOUD, centre.x, centre.y + 1.0, centre.z, 6, 0.3, 0.1, 0.6, 0.01);
    }

    /** Called when a formed part is removed. The other three go back to separate pieces. */
    public static void breakApart(ServerLevel level, BlockPos removed, BlockState removedState) {
        Direction facing = removedState.getValue(MicroreactorPartBlock.FACING);
        BlockPos origin = removedState.getValue(MicroreactorPartBlock.SLOT).toOrigin(removed, facing);
        for (MicroreactorSlot slot : MicroreactorSlot.FORMED) {
            BlockPos pos = slot.fromOrigin(origin, facing);
            BlockState state = level.getBlockState(pos);
            if (!pos.equals(removed) && state.getBlock() instanceof MicroreactorPartBlock
                    && state.getValue(MicroreactorPartBlock.SLOT) == slot
                    && state.getValue(MicroreactorPartBlock.FACING) == facing) {
                level.setBlock(pos, state.setValue(MicroreactorPartBlock.SLOT, MicroreactorSlot.NONE)
                        .setValue(MicroreactorPartBlock.RUNNING, false), Block.UPDATE_CLIENTS);
            }
        }
        refreshPorts(level, origin, facing);
        Vec3 centre = centre(origin, facing);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.7F);
        level.sendParticles(ParticleTypes.SMOKE, centre.x, centre.y, centre.z, 12, 0.4, 0.7, 0.7, 0.01);
    }

    /**
     * The heart behind a port, if {@code side} of this block is that port on a formed machine.
     * Used by the capability providers, so pipes only ever see the port faces.
     */
    public static @Nullable ReactorHeartBlockEntity heartForPort(Level level, BlockPos pos, BlockState state,
                                                               MicroreactorPort port, @Nullable Direction side) {
        return port.isAt(state, side) ? findHeart(level, pos, state) : null;
    }

    /** The heart of the formed machine this block belongs to, wherever it sits. */
    public static @Nullable ReactorHeartBlockEntity findHeart(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(MicroreactorPartBlock.SLOT).isFormed()) {
            return null;
        }
        Direction facing = state.getValue(MicroreactorPartBlock.FACING);
        BlockPos origin = state.getValue(MicroreactorPartBlock.SLOT).toOrigin(pos, facing);
        for (MicroreactorSlot slot : MicroreactorSlot.FORMED) {
            if (level.getBlockEntity(slot.fromOrigin(origin, facing)) instanceof ReactorHeartBlockEntity heart
                    && heart.getBlockState().getValue(MicroreactorPartBlock.SLOT) == slot) {
                return heart;
            }
        }
        return null;
    }

    /**
     * Ports appear or vanish when the machine forms or breaks, so pipes must look again. All four
     * states are set quietly first; neighbours are told only once the whole machine is consistent,
     * otherwise a pipe could check a port before the heart has changed and never check again.
     */
    private static void refreshPorts(ServerLevel level, BlockPos origin, Direction facing) {
        for (MicroreactorSlot slot : MicroreactorSlot.FORMED) {
            level.invalidateCapabilities(slot.fromOrigin(origin, facing));
        }
        for (MicroreactorSlot slot : MicroreactorSlot.FORMED) {
            BlockPos pos = slot.fromOrigin(origin, facing);
            BlockState state = level.getBlockState(pos);
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    /** Switches the glow, light and hum on or off across the whole machine. */
    public static void setRunning(Level level, BlockPos origin, Direction facing, boolean running) {
        for (MicroreactorSlot slot : MicroreactorSlot.FORMED) {
            BlockPos pos = slot.fromOrigin(origin, facing);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof MicroreactorPartBlock && state.getValue(MicroreactorPartBlock.RUNNING) != running) {
                level.setBlock(pos, state.setValue(MicroreactorPartBlock.RUNNING, running), Block.UPDATE_CLIENTS);
            }
        }
    }

    public static Vec3 centre(BlockPos origin, Direction facing) {
        Vec3 lower = Vec3.atCenterOf(origin);
        Vec3 upperBack = Vec3.atCenterOf(MicroreactorSlot.UPPER_BACK.fromOrigin(origin, facing));
        return lower.add(upperBack).scale(0.5);
    }
}
