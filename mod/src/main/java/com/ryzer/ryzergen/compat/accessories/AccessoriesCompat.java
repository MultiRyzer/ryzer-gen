package com.ryzer.ryzergen.compat.accessories;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

/**
 * The only door into the Accessories mod. Checks it is loaded before touching its classes, so the
 * mod runs fine without it (design rule 9). The ring is made wearable purely by data: it is tagged
 * accessories:ring.
 */
public final class AccessoriesCompat {
    private static final boolean LOADED = ModList.get().isLoaded("accessories");

    private AccessoriesCompat() {}

    public static boolean isEquipped(LivingEntity entity, Item item) {
        return LOADED && Equipped.check(entity, item);
    }

    /** Kept in its own class so Accessories' classes load only when the mod is present. */
    private static final class Equipped {
        static boolean check(LivingEntity entity, Item item) {
            return io.wispforest.accessories.api.AccessoriesCapability.getOptionally(entity)
                    .map(capability -> capability.isEquipped(item))
                    .orElse(false);
        }
    }
}
