package com.ryzer.ryzergen.compat.accessories;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * The only door into the Accessories mod. Checks it is loaded before touching its classes, so the
 * mod runs fine without it (design rule 9). Items are made wearable purely by data: the ring is
 * tagged accessories:ring and the Geiger counter accessories:belt.
 */
public final class AccessoriesCompat {
    private static final boolean LOADED = ModList.get().isLoaded("accessories");

    private AccessoriesCompat() {}

    public static boolean isEquipped(LivingEntity entity, Item item) {
        return LOADED && Equipped.check(entity, item);
    }

    /** The first stack of {@code item} worn in an accessory slot, or empty. */
    public static ItemStack firstEquipped(LivingEntity entity, Item item) {
        return LOADED ? Equipped.first(entity, item) : ItemStack.EMPTY;
    }

    /** Kept in its own class so Accessories' classes load only when the mod is present. */
    private static final class Equipped {
        static boolean check(LivingEntity entity, Item item) {
            return io.wispforest.accessories.api.AccessoriesCapability.getOptionally(entity)
                    .map(capability -> capability.isEquipped(item))
                    .orElse(false);
        }

        static ItemStack first(LivingEntity entity, Item item) {
            return io.wispforest.accessories.api.AccessoriesCapability.getOptionally(entity)
                    .map(capability -> capability.getFirstEquipped(item))
                    .map(io.wispforest.accessories.api.slot.SlotEntryReference::stack)
                    .orElse(ItemStack.EMPTY);
        }
    }
}
