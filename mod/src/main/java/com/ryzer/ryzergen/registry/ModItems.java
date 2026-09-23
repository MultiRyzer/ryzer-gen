package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.material.OreType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RyzerGen.MOD_ID);

    public static final DeferredItem<Item> GRAPHITE = ITEMS.registerSimpleItem("graphite");

    public static final Map<OreType, DeferredItem<BlockItem>> STONE_ORES = new EnumMap<>(OreType.class);
    public static final Map<OreType, DeferredItem<BlockItem>> DEEPSLATE_ORES = new EnumMap<>(OreType.class);
    /** What each ore drops: raw metal, a gem or a dust. */
    public static final Map<OreType, DeferredItem<Item>> ORE_DROPS = new EnumMap<>(OreType.class);
    public static final Map<OreType, DeferredItem<Item>> INGOTS = new EnumMap<>(OreType.class);

    // Registration order is the creative tab order: ores, then drops, then ingots.
    static {
        for (OreType ore : OreType.values()) {
            STONE_ORES.put(ore, ITEMS.registerSimpleBlockItem(ModBlocks.STONE_ORES.get(ore)));
            DEEPSLATE_ORES.put(ore, ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_ORES.get(ore)));
        }
        for (OreType ore : OreType.values()) {
            ORE_DROPS.put(ore, ITEMS.registerSimpleItem(ore.dropName()));
        }
        for (OreType ore : OreType.values()) {
            if (ore.hasIngot()) {
                INGOTS.put(ore, ITEMS.registerSimpleItem(ore.ingotName()));
            }
        }
    }

    private ModItems() {}
}
