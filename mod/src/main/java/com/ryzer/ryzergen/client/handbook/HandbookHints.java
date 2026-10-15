package com.ryzer.ryzergen.client.handbook;

import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Teaches the progression map's key: a few seconds after joining a world, a toast says "Press Y for
 * the map", every time, until the player has opened the map once. Then never again, on any world: a
 * marker file in the game folder ({@code ryzergen/map_opened}) remembers it.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class HandbookHints {
    /** Ticks after joining before the toast shows, past the join's own messages. */
    private static final int DELAY = 100;
    private static int countdown = -1;
    private static Object joinedOn;
    private static Boolean opened;
    private static HandbookToast toast;

    private HandbookHints() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Object connection = minecraft.getConnection();
        if (minecraft.player == null || connection == null) {
            joinedOn = null;
            return;
        }
        // A new connection is a new join (a world opened, or a server joined).
        if (connection != joinedOn) {
            joinedOn = connection;
            countdown = mapOpened() ? -1 : DELAY;
        }
        if (countdown >= 0 && minecraft.screen == null && --countdown < 0) {
            toast = new HandbookToast();
            minecraft.getToasts().addToast(toast);
        }
    }

    /** Whether the player has ever opened the map. */
    public static boolean mapOpened() {
        if (opened == null) {
            opened = Files.exists(marker());
        }
        return opened;
    }

    /** Called when the map opens: hides the toast and stops it for good. */
    public static void onMapOpened() {
        countdown = -1;
        if (toast != null) {
            toast.hide();
            toast = null;
        }
        if (mapOpened()) {
            return;
        }
        opened = true;
        try {
            Files.createDirectories(marker().getParent());
            Files.writeString(marker(), "The progression map has been opened; the \"Press Y\" toast no longer shows.\n");
        } catch (IOException e) {
            RyzerGen.LOGGER.warn("Could not save {}", marker(), e);
        }
    }

    private static Path marker() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(RyzerGen.MOD_ID).resolve("map_opened");
    }
}
