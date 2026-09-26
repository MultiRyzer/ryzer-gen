package com.ryzer.ryzergen.compat.accessories;

import com.ryzer.ryzergen.registry.ModItems;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import net.neoforged.fml.ModList;

/**
 * Client-side Accessories hooks. Only called after checking Accessories is loaded, so its classes
 * are never touched without it (design rule 9).
 */
public final class AccessoriesClient {
    private AccessoriesClient() {}

    public static void register() {
        if (ModList.get().isLoaded("accessories")) {
            Hooks.register();
        }
    }

    /** Kept in its own class so Accessories' classes load only when the mod is present. */
    private static final class Hooks {
        static void register() {
            // Nothing is drawn for a worn ring, as with other mods' rings. Registered explicitly,
            // or Accessories would fall back to its default renderer and paste the item on the hand.
            AccessoriesRendererRegistry.registerNoRenderer(ModItems.DOSIMETER_RING.get());
        }
    }
}
