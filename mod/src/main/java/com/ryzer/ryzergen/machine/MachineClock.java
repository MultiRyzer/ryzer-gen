package com.ryzer.ryzergen.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a machine shows from outside, shared by the single-block machines: on the server, its look
 * (sent to clients when it changes); on the client, the clock its moving parts, glows and lamp
 * follow (client.MachineMotion). Its spin and heat ease in and out, so parts spin up and wind down
 * and hot things warm and cool rather than switch; its beat runs faster with speed modules.
 */
public final class MachineClock {
    /**
     * Off, on and waiting (standby), working, or starved (a gated machine getting some power, but
     * short of its full draw).
     */
    public enum Look { OFF, STANDBY, WORKING, STARVED }

    /** Ticks between updates sent to clients, so a flickering supply cannot flood them. */
    private static final int SYNC_TICKS = 10;

    /** The look and speed modules clients see: kept on the server as last sent, on the client as received. */
    private Look look = Look.OFF;
    private int modules;
    private long sentAt = -SYNC_TICKS;

    private float spin;
    private float heat;
    /** Rises by one a tick while the machine works (faster with modules). A double, so it stays smooth for days. */
    private double beat;
    private double beatBefore;

    /** Server, each tick: what the machine shows now, sent to clients when it changes. */
    public void show(Level level, BlockPos pos, BlockState state, Look seen, int fitted) {
        if ((seen != look || fitted != modules) && level.getGameTime() - sentAt >= SYNC_TICKS) {
            look = seen;
            modules = fitted;
            sentAt = level.getGameTime();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** For the block entity's update tag. */
    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("look", look.ordinal());
        tag.putInt("modules", modules);
        return tag;
    }

    /** From the block entity's update tag or packet. */
    public void read(CompoundTag tag) {
        Look[] looks = Look.values();
        look = looks[Math.clamp(tag.getInt("look"), 0, looks.length - 1)];
        modules = tag.getInt("modules");
    }

    /**
     * Client, each tick: the parts ease up to speed while it works and wind down after, and the heat
     * eases towards its look's (a machine that does not send its look shows working while it works).
     */
    public void animate(boolean working) {
        animate(working, 0);
    }

    /** As {@link #animate(boolean)}, the parts idling round at {@code idle} of full speed on standby (the drill's fans). */
    public void animate(boolean working, float idle) {
        spin += ((working ? 1 : look == Look.STANDBY ? idle : 0) - spin) * 0.05F;
        float hot = switch (working ? Look.WORKING : look) {
            case OFF -> 0;
            case STANDBY -> 0.45F;
            case WORKING -> 1;
            case STARVED -> 0.6F;
        };
        heat += (hot - heat) * 0.02F;
        beatBefore = beat;
        beat += spin * (1 + 0.5F * modules);
    }

    public Look look() {
        return look;
    }

    /** How fast the moving parts go, 0 (still) to 1 (working), easing between. */
    public float spin() {
        return spin;
    }

    /** How hot it runs, 0 (cold) to 1 (working), easing between. */
    public float heat() {
        return heat;
    }

    /** The beat for this frame. */
    public double beat(float partialTick) {
        return beatBefore + (beat - beatBefore) * partialTick;
    }
}
