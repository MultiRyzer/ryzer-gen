package com.ryzer.ryzergen.gametest;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.item.HandbookItem;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Operator's Handbook is given once: a player joining a world for the first time gets one, and
 * joining again (with the book used up, so nothing hides a second copy) gives none.
 */
@GameTestHolder(RyzerGen.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HandbookTests {
    private HandbookTests() {}

    @GameTest(template = "mega_drill_floor")
    public static void theHandbookIsGivenOnce(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(HandbookItem.giveOnce(player), "a new player should be given the handbook");
        int first = player.getInventory().countItem(ModItems.OPERATORS_HANDBOOK.get());
        helper.assertTrue(first == 1, "a new player should get one handbook, got " + first);
        player.getInventory().clearContent();
        helper.assertFalse(HandbookItem.giveOnce(player), "joining again should not give it");
        int again = player.getInventory().countItem(ModItems.OPERATORS_HANDBOOK.get());
        helper.assertTrue(again == 0, "joining again should give no handbook, got " + again);
        helper.succeed();
    }
}
