package com.ryzer.ryzergen.machine.drill;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/**
 * What the mega drill's renderer (client/MegaDrillRenderer) needs to draw a drill and its cycle,
 * shared by the creative preview and the real drill's controller. Clients work everything else out
 * from the phase, when it began and how long it lasts.
 */
public interface MegaDrillView {
    /** False while there is nothing to draw (a real drill not yet formed). */
    boolean drillShown();

    /** The way the drill's front (its ladder) faces. */
    Direction drillFacing();

    /** Running: the lamps, light lines and window lit, the emitter's tip glowing. */
    boolean drillRunning();

    DrillPhase drillPhase();

    /** How far through its phase it is, 0 to 1. */
    float drillProgress(long gameTime, float partialTick);

    /** Ticks since its phase began. */
    float drillElapsed(long gameTime, float partialTick);

    /** The layer being mined, or next to be. */
    int drillLayer();

    /** The middle of the pit, on the drill's base level: the design's centre block (4, 0, 4). */
    BlockPos drillCentre();

    /** Whether the heat is drawn over this block of the layer: the blocks that will burst. */
    boolean drillHeats(BlockGetter level, BlockPos pos, int dx, int dz);

    /** The client's GPU mesh of the drill's body, kept with the block entity and closed with it. */
    Object drillMesh();

    void drillMesh(Object mesh);
}
