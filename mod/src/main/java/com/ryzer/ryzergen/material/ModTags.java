package com.ryzer.ryzergen.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Our tags, and common tags that NeoForge's own Tags class does not define. Ore tags live on {@link OreType}. */
public final class ModTags {
    public static final TagKey<Item> INGOTS_STEEL = common("ingots/steel");
    /** Graphite as reactor mods tag it. */
    public static final TagKey<Item> INGOTS_GRAPHITE = common("ingots/graphite");
    public static final TagKey<Item> GEMS_SILICON_CARBIDE = common("gems/silicon_carbide");
    /** Tier 1 circuits. Mekanism's basic control circuit shares this tag. */
    public static final TagKey<Item> CIRCUITS_BASIC = common("circuits/basic");
    /** Vanilla has no charcoal block, but several mods add one. */
    public static final TagKey<Item> STORAGE_BLOCKS_CHARCOAL = common("storage_blocks/charcoal");

    /** Sealed fuel cores the microreactor heart accepts. Data-driven, so packs can add their own. */
    public static final TagKey<Item> MICROREACTOR_FUEL = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("ryzergen", "microreactor_fuel"));

    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private ModTags() {}
}
