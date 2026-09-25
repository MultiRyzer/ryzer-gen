package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.machine.processing.TallProcessingBlock;
import com.ryzer.ryzergen.battery.HomeBatteryBlock;
import com.ryzer.ryzergen.material.OreType;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.List;
import java.util.Set;

public class ModBlockLootProvider extends BlockLootSubProvider {
    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.ALLOY_SMELTER.get());
        dropSelf(ModBlocks.ELECTRIC_ALLOY_SMELTER.get());
        dropSelf(ModBlocks.ENERGY_CABLE.get());
        dropSelf(ModBlocks.ITEM_PIPE.get());
        dropSelf(ModBlocks.FLUID_PIPE.get());
        dropSelf(ModBlocks.GAS_PIPE.get());
        dropSelf(ModBlocks.INTAKE_PUMP.get());
        dropSelf(ModBlocks.PRESSURE_TANK.get());
        dropSelf(ModBlocks.FLUID_TANK.get());
        dropSelf(ModBlocks.STATION_CORE.get());
        dropSelf(ModBlocks.GRAPHITE_BLOCK.get());
        dropSelf(ModBlocks.CREATIVE_BATTERY.get());
        dropSelf(ModBlocks.CREATIVE_WATER_TANK.get());
        dropSelf(ModBlocks.FUSION_PREVIEW.get());
        dropSelf(ModBlocks.STATION_CASING.get());
        dropSelf(ModBlocks.STATION_GLASS.get());
        dropSelf(ModBlocks.TURBINE_ROTOR.get());
        // Only the lower half drops the cabinet; its modules drop from the block entity.
        add(ModBlocks.HOME_BATTERY.get(), createSinglePropConditionTable(ModBlocks.HOME_BATTERY.get(),
                HomeBatteryBlock.HALF, DoubleBlockHalf.LOWER));
        dropSelf(ModBlocks.CORE_CRACKER.get());
        dropSelf(ModBlocks.FUEL_FABRICATOR.get());
        dropSelf(ModBlocks.LITHIUM_EXTRACTOR.get());
        add(ModBlocks.REPROCESSOR.get(), createSinglePropConditionTable(ModBlocks.REPROCESSOR.get(),
                TallProcessingBlock.HALF, DoubleBlockHalf.LOWER));
        // The cask keeps its waste, like a shulker box keeps its contents.
        add(ModBlocks.WASTE_CASK.get(), createShulkerBoxDrop(ModBlocks.WASTE_CASK.get()));
        dropSelf(ModBlocks.REACTOR_HEART.get());
        dropSelf(ModBlocks.REACTOR_MACHINE_UNIT.get());
        dropSelf(ModBlocks.COOLANT_JACKET.get());
        for (OreType ore : OreType.values()) {
            Item drop = ModItems.ORE_DROPS.get(ore).get();
            for (Block block : List.of(ModBlocks.STONE_ORES.get(ore).get(), ModBlocks.DEEPSLATE_ORES.get(ore).get())) {
                add(block, ore.maxDrops() == 1
                        ? createOreDrop(block, drop)
                        : createMultipleOreDrop(block, drop, ore.minDrops(), ore.maxDrops()));
            }
        }
    }

    /** Like vanilla copper and redstone: several drops, boosted by Fortune, silk touch gives the block. */
    private LootTable.Builder createMultipleOreDrop(Block block, Item drop, int min, int max) {
        HolderLookup.RegistryLookup<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        return createSilkTouchDispatchTable(block, applyExplosionDecay(block, LootItem.lootTableItem(drop)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))
                .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
