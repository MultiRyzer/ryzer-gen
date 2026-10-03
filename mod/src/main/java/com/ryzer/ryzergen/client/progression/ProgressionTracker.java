package com.ryzer.ryzergen.client.progression;

import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which steps of the progression map the player has reached: a step is reached once its item has
 * been in their inventory, or the game's own statistics show it crafted or picked up. The game does
 * not count what comes out of a machine's slots, so a short scan of the inventory every second
 * catches those, and what it has seen is kept per world (or server) in the game folder, under
 * {@code ryzergen/progress}, so the map remembers. Client only.
 */
public final class ProgressionTracker {
    private static final Set<Item> SEEN = new HashSet<>();
    private static @Nullable String loadedFor;
    private static boolean dirty;

    private ProgressionTracker() {}

    /** Called each client tick: scans the inventory every second and saves what is new. */
    public static void tick(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            loadedFor = null;
            return;
        }
        String key = worldKey(minecraft);
        if (!key.equals(loadedFor)) {
            load(key);
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        Set<Item> tracked = new HashSet<>();
        for (ProgressionMap.Node node : ProgressionMap.nodes()) {
            tracked.add(node.item());
        }
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && tracked.contains(stack.getItem()) && SEEN.add(stack.getItem())) {
                dirty = true;
            }
        }
        if (dirty) {
            save(key);
        }
    }

    /** Whether the player has reached a step's item. */
    public static boolean reached(Minecraft minecraft, Item item) {
        if (SEEN.contains(item)) {
            return true;
        }
        Player player = minecraft.player;
        if (player instanceof net.minecraft.client.player.LocalPlayer local) {
            var stats = local.getStats();
            return stats.getValue(Stats.ITEM_CRAFTED.get(item)) > 0 || stats.getValue(Stats.ITEM_PICKED_UP.get(item)) > 0;
        }
        return false;
    }

    /** One file per world: the save's folder name in single player, the server's address otherwise. */
    private static String worldKey(Minecraft minecraft) {
        String raw;
        if (minecraft.getSingleplayerServer() != null) {
            raw = "local_" + minecraft.getSingleplayerServer().getWorldData().getLevelName();
        } else if (minecraft.getCurrentServer() != null) {
            raw = "server_" + minecraft.getCurrentServer().ip;
        } else {
            raw = "unknown";
        }
        return raw.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static Path file(String key) {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(RyzerGen.MOD_ID).resolve("progress").resolve(key + ".txt");
    }

    private static void load(String key) {
        SEEN.clear();
        loadedFor = key;
        dirty = false;
        Path path = file(key);
        if (!Files.exists(path)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                ResourceLocation id = ResourceLocation.tryParse(line.trim());
                if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                    SEEN.add(BuiltInRegistries.ITEM.get(id));
                }
            }
        } catch (IOException e) {
            RyzerGen.LOGGER.warn("Could not read progression progress {}", path, e);
        }
    }

    private static void save(String key) {
        dirty = false;
        Path path = file(key);
        try {
            Files.createDirectories(path.getParent());
            List<String> lines = SEEN.stream().map(item -> BuiltInRegistries.ITEM.getKey(item).toString()).sorted().toList();
            Files.write(path, lines);
        } catch (IOException e) {
            RyzerGen.LOGGER.warn("Could not save progression progress {}", path, e);
        }
    }
}
