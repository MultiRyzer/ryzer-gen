package com.ryzer.ryzergen.sky;

import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Dyson swarm round the overworld's sun, as seen from the ground. For now it is driven by the
 * creative-only Swarm Controller (the tier that builds it for real comes later, see DESIGN.md).
 *
 * <p>The swarm launches (the shades appear round the sun in their orbits), then closes: the orbits
 * fly into a shell whose last gap shrinks shut, and the light goes out: the sky stays dark and
 * the world dim (on each client) until a new sun is lit. Real basis: a finished Dyson sphere hides
 * its star's light, but not its waste heat, so it glows faintly in the infrared, which is how
 * astronomers look for them.
 *
 * <p>After that the enclosed sun collapses and a wormhole opens where it was, looking through to
 * a new star somewhere else (the next stage will pull it through). Real basis, loosely: a wormhole
 * is a theoretical tunnel between two places, so unlike a black hole, light from the far end
 * comes through its mouth. The lensing round it is real: mass bends light, which draws the sky
 * behind into a ring (an Einstein ring).
 *
 * <p>Saved with the overworld, and sent to every player when it changes and when they join.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID)
public final class SunSwarm extends SavedData {
    /** In order; new phases go on the end, since worlds save the phase's number. */
    public enum Phase { NONE, LAUNCHING, SWARM, CLOSING, CLOSED, COLLAPSING, WORMHOLE }

    /** How long the swarm takes to launch, and the shell to close, in ticks. */
    public static final int LAUNCH_TICKS = 200;
    public static final int CLOSE_TICKS = 320;
    /** How long the enclosed sun takes to collapse and the wormhole to open, in ticks. */
    public static final int COLLAPSE_TICKS = 160;
    private static final String NAME = RyzerGen.MOD_ID + "_sun_swarm";

    private Phase phase = Phase.NONE;
    /** The game time the current phase began. */
    private long start;

    public static SunSwarm get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(SunSwarm::new, SunSwarm::load, null), NAME);
    }

    public Phase phase() {
        return phase;
    }

    /**
     * The next step: launch the swarm, close the shell, or collapse the enclosed sun into a
     * wormhole. Returns false while something is still moving, or once the wormhole is open.
     */
    public boolean advance(ServerLevel level) {
        switch (phase) {
            case NONE -> begin(level, Phase.LAUNCHING);
            case SWARM -> begin(level, Phase.CLOSING);
            case CLOSED -> begin(level, Phase.COLLAPSING);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Lights a new sun: the swarm is gone and the daylight comes back. */
    public void reset(ServerLevel level) {
        begin(level, Phase.NONE);
    }

    private void begin(ServerLevel level, Phase next) {
        phase = next;
        start = level.getGameTime();
        setDirty();
        PacketDistributor.sendToAllPlayers(payload());
    }

    private SunSwarmPayload payload() {
        return new SunSwarmPayload((byte) phase.ordinal(), start);
    }

    private void tick(ServerLevel level) {
        long elapsed = level.getGameTime() - start;
        if (phase == Phase.LAUNCHING && elapsed >= LAUNCH_TICKS) {
            begin(level, Phase.SWARM);
        } else if (phase == Phase.CLOSING && elapsed >= CLOSE_TICKS) {
            begin(level, Phase.CLOSED);
        } else if (phase == Phase.COLLAPSING && elapsed >= COLLAPSE_TICKS) {
            begin(level, Phase.WORMHOLE);
        }
    }

    // ---------------------------------------------------------------- events

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            get(level).tick(level);
        }
    }

    @SubscribeEvent
    public static void playerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, get(player.serverLevel()).payload());
        }
    }

    // ---------------------------------------------------------------- saving

    private static SunSwarm load(CompoundTag tag, HolderLookup.Provider registries) {
        SunSwarm swarm = new SunSwarm();
        int index = tag.getInt("phase");
        swarm.phase = index >= 0 && index < Phase.values().length ? Phase.values()[index] : Phase.NONE;
        swarm.start = tag.getLong("start");
        return swarm;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("phase", phase.ordinal());
        tag.putLong("start", start);
        return tag;
    }
}
