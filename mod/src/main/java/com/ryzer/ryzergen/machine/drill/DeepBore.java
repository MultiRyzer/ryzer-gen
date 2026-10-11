package com.ryzer.ryzergen.machine.drill;

import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;

/**
 * The mega drill's deep bore (design section 11c): once its shaft reaches bedrock it bores on into
 * the deep crust, breaking no more blocks, and each cycle brings up ore from a loot table set by its
 * head (packs can change them): a Mk I brings up the common ores, a Mk II adds uranium, lead, silver
 * and the gems, a Mk III the rare ones. It draws several times the shaft's power. Real basis, honestly
 * fudged: the deep crust is denser and richer in heavy metals, and superdeep boreholes found hot,
 * mineral-rich brines; the fudge is reaching it, and what it yields.
 */
public final class DeepBore {
    private static final List<ResourceKey<LootTable>> TABLES = List.of(table(1), table(2), table(3));

    private DeepBore() {}

    /** The loot table a head's mark (1 to 3) bores from. */
    public static ResourceKey<LootTable> table(int mark) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "mega_drill/deep_bore_mk" + mark));
    }

    /** One bore cycle's ore: {@code rolls} draws from the head's table. */
    public static List<ItemStack> bore(ServerLevel level, int mark, int rolls) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(TABLES.get(Math.max(1, Math.min(3, mark)) - 1));
        LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        List<ItemStack> ore = new java.util.ArrayList<>();
        for (int i = 0; i < rolls; i++) {
            ore.addAll(table.getRandomItems(params));
        }
        return ore;
    }
}
