package com.ryzer.ryzergen.gametest;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.processing.ProcessingBlockEntity;
import com.ryzer.ryzergen.machine.MachineClock.Look;
import com.ryzer.ryzergen.material.OreType;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * What a fuel cycle machine shows from outside (its look, which clients draw its lamp, glow and
 * moving parts from), on a test server (./gradlew runGameTestServer). An Electrorefiner, gated at
 * 1,500 FE/t, goes through each look in turn: off with no power, standby once powered with nothing
 * to do, working on salt with its full draw, and starved on two thirds of it. Each stage runs 30
 * ticks, longer than the look takes to reach clients, and is checked at its end.
 */
@GameTestHolder(RyzerGen.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineLookTests {
    private static final BlockPos AT = new BlockPos(5, 1, 5);
    private static final int STAGE = 30;

    private MachineLookTests() {}

    @GameTest(template = "mega_drill_floor", timeoutTicks = 200)
    public static void electrorefinerShowsWhatItIsDoing(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.ELECTROREFINER.get());
        ProcessingBlockEntity machine = helper.getBlockEntity(AT);
        Look[] expected = {Look.OFF, Look.STANDBY, Look.WORKING, Look.STARVED};
        int[] tick = {0};
        helper.onEachTick(() -> {
            int stage = tick[0] / STAGE;
            int at = tick[0]++ % STAGE;
            if (stage >= expected.length) {
                return;
            }
            IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(AT), Direction.EAST);
            IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(AT), Direction.UP);
            helper.assertTrue(energy != null && items != null, "the machine takes power and items on its sides");
            if (stage == 2 && at == 0) {
                ItemStack left = items.insertItem(0, new ItemStack(ModItems.ORE_DROPS.get(OreType.SALT).get(), 2), false);
                helper.assertTrue(left.isEmpty(), "the cell takes salt");
            }
            if (stage == 1 || stage == 2) {
                energy.receiveEnergy(1_500, false);
            } else if (stage == 3) {
                energy.receiveEnergy(1_000, false);
            }
            if (at == STAGE - 1) {
                helper.assertTrue(machine.clock().look() == expected[stage],
                        "stage " + stage + " shows " + expected[stage] + " (shows " + machine.clock().look() + ")");
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(tick[0] >= expected.length * STAGE, "every stage checked"));
    }
}
