package com.ryzer.ryzergen.gametest;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.cable.CableBlock;
import com.ryzer.ryzergen.cable.CableNetwork;
import com.ryzer.ryzergen.cable.CableSide;
import com.ryzer.ryzergen.cable.ItemPipeBlockEntity;
import com.ryzer.ryzergen.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Item pipes' filters and priorities, on a test server (./gradlew runGameTestServer). Each test lays
 * a source chest, a pipe pulling from it into a second pipe, and two chests either side of that one.
 * With a filter on each chest's side (only iron, and all but iron), each item ends up in its own
 * chest; with a priority on one side, it gets everything before the other gets any.
 */
@GameTestHolder(RyzerGen.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PipeTests {
    private PipeTests() {}

    /** A source chest, a pipe pulling from it east into a second pipe, and chests north and south of that one. */
    private static ItemPipeBlockEntity lay(GameTestHelper helper, int z, ItemStack... stock) {
        BlockPos source = new BlockPos(2, 1, z);
        BlockPos pull = source.east();
        BlockPos split = pull.east();
        helper.setBlock(source, Blocks.CHEST);
        helper.setBlock(split.north(), Blocks.CHEST);
        helper.setBlock(split.south(), Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(source);
        for (int i = 0; i < stock.length; i++) {
            chest.setItem(i, stock[i]);
        }
        CableBlock pipe = (CableBlock) ModBlocks.ITEM_PIPE.get();
        for (BlockPos at : new BlockPos[] {pull, split}) {
            helper.setBlock(at, pipe.connect(pipe.defaultBlockState(), helper.getLevel(), helper.absolutePos(at)));
        }
        helper.setBlock(pull, helper.getBlockState(pull).setValue(CableBlock.SIDES.get(Direction.WEST), CableSide.EXTRACT));
        CableNetwork.changed(helper.getLevel(), helper.absolutePos(pull));
        return helper.getBlockEntity(split);
    }

    private static int count(GameTestHelper helper, BlockPos at, Item item) {
        ChestBlockEntity chest = helper.getBlockEntity(at);
        int n = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack stack = chest.getItem(i);
            n += stack.is(item) ? stack.getCount() : 0;
        }
        return n;
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 300)
    public static void filtersSortItemsToTheirChests(GameTestHelper helper) {
        ItemPipeBlockEntity split = lay(helper, 2, new ItemStack(Items.IRON_INGOT, 8), new ItemStack(Items.COBBLESTONE, 8));
        // North: only iron. South: all but iron.
        split.setFilter(Direction.NORTH, 0, new ItemStack(Items.IRON_INGOT));
        split.toggleOnly(Direction.NORTH);
        split.setFilter(Direction.SOUTH, 0, new ItemStack(Items.IRON_INGOT));
        BlockPos north = new BlockPos(4, 1, 1);
        BlockPos south = new BlockPos(4, 1, 3);
        helper.succeedWhen(() -> {
            helper.assertTrue(count(helper, north, Items.IRON_INGOT) == 8, "the iron goes north (" + count(helper, north, Items.IRON_INGOT) + ")");
            helper.assertTrue(count(helper, south, Items.COBBLESTONE) == 8, "the cobblestone goes south (" + count(helper, south, Items.COBBLESTONE) + ")");
            helper.assertTrue(count(helper, north, Items.COBBLESTONE) == 0 && count(helper, south, Items.IRON_INGOT) == 0,
                    "nothing goes where its filter keeps it out");
        });
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 300)
    public static void priorityFillsItsSideFirst(GameTestHelper helper) {
        ItemPipeBlockEntity split = lay(helper, 6, new ItemStack(Items.DIRT, 16));
        split.nudgePriority(Direction.SOUTH, 5);
        BlockPos north = new BlockPos(4, 1, 5);
        BlockPos south = new BlockPos(4, 1, 7);
        helper.succeedWhen(() -> {
            helper.assertTrue(count(helper, south, Items.DIRT) == 16, "the higher priority gets it all (" + count(helper, south, Items.DIRT) + ")");
            helper.assertTrue(count(helper, north, Items.DIRT) == 0, "the lower priority gets none while the higher has room");
        });
    }
}
