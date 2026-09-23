package com.ryzer.ryzergen.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Common tags that NeoForge's own Tags class does not define. Ore tags live on {@link OreType}. */
public final class ModTags {
    public static final TagKey<Item> INGOTS_STEEL = common("ingots/steel");
    /** Vanilla has no charcoal block, but several mods add one. */
    public static final TagKey<Item> STORAGE_BLOCKS_CHARCOAL = common("storage_blocks/charcoal");

    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private ModTags() {}
}
