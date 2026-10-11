package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.machine.drill.DeepBore;
import com.ryzer.ryzergen.material.OreType;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The mega drill's deep bore tables (machine/drill/DeepBore), one draw of ore each: a Mk I head
 * brings up the common ores, a Mk II adds uranium, lead, silver and the gems, a Mk III tungsten and
 * monazite. Weights are out of the whole table, so a better head also thins the common ores a little.
 */
public class ModDeepBoreLootProvider implements LootTableSubProvider {
    private record Entry(ItemLike item, int weight, int min, int max) {}

    public ModDeepBoreLootProvider(HolderLookup.Provider registries) {
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        List<Entry> common = List.of(
                new Entry(Items.COAL, 25, 1, 2),
                new Entry(Items.RAW_IRON, 25, 1, 1),
                new Entry(Items.RAW_COPPER, 25, 1, 2),
                new Entry(drop(OreType.ALUMINIUM), 15, 1, 1),
                new Entry(Items.RAW_GOLD, 6, 1, 1),
                new Entry(Items.REDSTONE, 4, 2, 4),
                new Entry(drop(OreType.SALT), 6, 1, 2));
        List<Entry> advanced = List.of(
                new Entry(drop(OreType.URANIUM), 8, 1, 1),
                new Entry(drop(OreType.LEAD), 10, 1, 1),
                new Entry(drop(OreType.SILVER), 8, 1, 1),
                new Entry(drop(OreType.FLUORITE), 6, 1, 2),
                new Entry(Items.LAPIS_LAZULI, 4, 2, 4),
                new Entry(Items.DIAMOND, 2, 1, 1),
                new Entry(Items.EMERALD, 1, 1, 1));
        List<Entry> rare = List.of(
                new Entry(drop(OreType.TUNGSTEN), 6, 1, 1),
                new Entry(drop(OreType.MONAZITE), 4, 1, 1),
                new Entry(Items.DIAMOND, 2, 1, 1));
        List<Entry> mk2 = new ArrayList<>(common);
        mk2.addAll(advanced);
        List<Entry> mk3 = new ArrayList<>(mk2);
        mk3.addAll(rare);
        output.accept(DeepBore.table(1), table(common));
        output.accept(DeepBore.table(2), table(mk2));
        output.accept(DeepBore.table(3), table(mk3));
    }

    private static Item drop(OreType ore) {
        return ModItems.ORE_DROPS.get(ore).get();
    }

    private static LootTable.Builder table(List<Entry> entries) {
        LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
        for (Entry entry : entries) {
            LootItem.Builder<?> item = LootItem.lootTableItem(entry.item()).setWeight(entry.weight());
            if (entry.max() > 1) {
                item.apply(SetItemCountFunction.setCount(UniformGenerator.between(entry.min(), entry.max())));
            }
            pool.add(item);
        }
        return LootTable.lootTable().withPool(pool);
    }
}
