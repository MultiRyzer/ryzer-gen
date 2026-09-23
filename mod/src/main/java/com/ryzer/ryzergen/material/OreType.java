package com.ryzer.ryzergen.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.Optional;

/**
 * Every ore the mod ships. Each one gets a stone and a deepslate ore block, a drop, and an ingot if it is a metal.
 * Worldgen rarity lives in the datagen worldgen provider.
 */
public enum OreType {
    URANIUM("uranium", "uranium", Drop.RAW, true, Tool.IRON, 1, 1, ConstantInt.of(0)),
    LEAD("lead", "lead", Drop.RAW, true, Tool.STONE, 1, 1, ConstantInt.of(0)),
    SILVER("silver", "silver", Drop.RAW, true, Tool.IRON, 1, 1, ConstantInt.of(0)),
    // The ore is bauxite. Common tags use the American spelling, so packs see it as aluminum.
    ALUMINIUM("aluminium", "aluminum", Drop.RAW, true, Tool.STONE, 1, 1, ConstantInt.of(0)),
    FLUORITE("fluorite", "fluorite", Drop.GEM, false, Tool.STONE, 1, 3, UniformInt.of(1, 3)),
    SALT("salt", "salt", Drop.DUST, false, Tool.WOOD, 2, 4, UniformInt.of(0, 1)),
    TUNGSTEN("tungsten", "tungsten", Drop.RAW, true, Tool.IRON, 1, 1, ConstantInt.of(0)),
    // Processed later for thorium and yttrium, so it has no ingot.
    MONAZITE("monazite", "monazite", Drop.RAW, false, Tool.IRON, 1, 1, ConstantInt.of(0));

    private final String id;
    private final String tagName;
    private final Drop drop;
    private final boolean hasIngot;
    private final Tool tool;
    private final int minDrops;
    private final int maxDrops;
    private final IntProvider xp;

    OreType(String id, String tagName, Drop drop, boolean hasIngot, Tool tool, int minDrops, int maxDrops, IntProvider xp) {
        this.id = id;
        this.tagName = tagName;
        this.drop = drop;
        this.hasIngot = hasIngot;
        this.tool = tool;
        this.minDrops = minDrops;
        this.maxDrops = maxDrops;
        this.xp = xp;
    }

    public String id() { return id; }
    public boolean hasIngot() { return hasIngot; }
    public Tool tool() { return tool; }
    public Drop drop() { return drop; }
    public int minDrops() { return minDrops; }
    public int maxDrops() { return maxDrops; }
    public IntProvider xp() { return xp; }

    public String dropName() { return drop == Drop.RAW ? "raw_" + id : id; }
    public String ingotName() { return id + "_ingot"; }

    public TagKey<Block> oreBlockTag() { return TagKey.create(Registries.BLOCK, common("ores/" + tagName)); }
    public TagKey<Item> oreItemTag() { return itemTag("ores/" + tagName); }
    public TagKey<Item> dropTag() { return itemTag(drop.tagFolder + "/" + tagName); }
    public TagKey<Item> ingotTag() { return itemTag("ingots/" + tagName); }

    public static Optional<OreType> byId(String id) {
        return Arrays.stream(values()).filter(ore -> ore.id.equals(id)).findFirst();
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, common(path));
    }

    private static ResourceLocation common(String path) {
        return ResourceLocation.fromNamespaceAndPath("c", path);
    }

    public enum Drop {
        RAW("raw_materials"),
        GEM("gems"),
        DUST("dusts");

        private final String tagFolder;

        Drop(String tagFolder) {
            this.tagFolder = tagFolder;
        }
    }

    /** The weakest pickaxe that can mine the ore. */
    public enum Tool { WOOD, STONE, IRON }
}
