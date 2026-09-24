package com.ryzer.ryzergen.radiation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Where radiation comes from (design rule 10): running reactors, which report themselves every tick
 * they run, and meltdown sites, which are saved with the world and fade over time. Nothing else
 * emits: ores, fuel, depleted cores and waste are safe.
 */
public final class RadiationSources {
    /** A point source: strength is the dose rate in mSv per second at one block away. */
    public record Source(Vec3 pos, float strength) {}

    private record Live(Vec3 pos, float strength, long lastSeen) {}

    private static final Map<ResourceKey<Level>, Map<Vec3, Live>> RUNNING = new HashMap<>();

    private RadiationSources() {}

    /** Called by a running reactor each tick. Its entry lapses a couple of seconds after it stops. */
    public static void emit(ServerLevel level, Vec3 pos, float strength) {
        RUNNING.computeIfAbsent(level.dimension(), key -> new HashMap<>())
                .put(pos, new Live(pos, strength, level.getGameTime()));
    }

    /** A meltdown leaves a contaminated area that fades to nothing over {@code ticks}. */
    public static void contaminate(ServerLevel level, Vec3 pos, float strength, long ticks) {
        Contamination data = Contamination.get(level);
        data.sites.add(new Site(pos, strength, level.getGameTime(), level.getGameTime() + ticks));
        data.setDirty();
    }

    /** Every source in this level right now, with meltdown sites at their faded strength. */
    public static List<Source> sources(ServerLevel level) {
        List<Source> sources = new ArrayList<>();
        long now = level.getGameTime();
        Map<Vec3, Live> running = RUNNING.get(level.dimension());
        if (running != null) {
            running.values().removeIf(live -> now - live.lastSeen() > 40);
            for (Live live : running.values()) {
                sources.add(new Source(live.pos(), live.strength()));
            }
        }
        Contamination data = Contamination.get(level);
        Iterator<Site> sites = data.sites.iterator();
        while (sites.hasNext()) {
            Site site = sites.next();
            if (now >= site.end()) {
                sites.remove();
                data.setDirty();
                continue;
            }
            float left = (site.end() - now) / (float) (site.end() - site.start());
            sources.add(new Source(site.pos(), site.strength() * left));
        }
        return sources;
    }

    private record Site(Vec3 pos, float strength, long start, long end) {}

    /** Meltdown sites, saved with each dimension. */
    private static final class Contamination extends SavedData {
        private final List<Site> sites = new ArrayList<>();

        static Contamination get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(Contamination::new, Contamination::load, null), "ryzergen_contamination");
        }

        static Contamination load(CompoundTag tag, HolderLookup.Provider registries) {
            Contamination data = new Contamination();
            for (Tag entry : tag.getList("sites", Tag.TAG_COMPOUND)) {
                CompoundTag site = (CompoundTag) entry;
                data.sites.add(new Site(new Vec3(site.getDouble("x"), site.getDouble("y"), site.getDouble("z")),
                        site.getFloat("strength"), site.getLong("start"), site.getLong("end")));
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (Site site : sites) {
                CompoundTag entry = new CompoundTag();
                entry.putDouble("x", site.pos().x);
                entry.putDouble("y", site.pos().y);
                entry.putDouble("z", site.pos().z);
                entry.putFloat("strength", site.strength());
                entry.putLong("start", site.start());
                entry.putLong("end", site.end());
                list.add(entry);
            }
            tag.put("sites", list);
            return tag;
        }
    }
}
