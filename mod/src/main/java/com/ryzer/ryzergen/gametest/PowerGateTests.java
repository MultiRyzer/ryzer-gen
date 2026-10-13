package com.ryzer.ryzergen.gametest;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.processing.ProcessingBlockEntity;
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
 * Power is the price (design rule 7): a gated machine runs only on its full draw, so a trickle
 * cannot bank power while it waits and spend it in bursts. On a test server (./gradlew
 * runGameTestServer), an Electrorefiner, gated at 1,500 FE/t, on a third of its draw: first idle
 * with nothing to do while its buffer fills, then with salt in it; then switched off and on in a
 * clock (six ticks charging off, two on), as a redstone clock would. It must make no progress at
 * all. Then its full draw runs it, so the gate still lets real power through.
 */
@GameTestHolder(RyzerGen.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PowerGateTests {
    private static final BlockPos AT = new BlockPos(5, 1, 5);
    private static final int TRICKLE = 500;
    private static final int FULL = 1_500;
    private static final int SALT_AT = 40;
    private static final int FULL_AT = 200;

    private PowerGateTests() {}

    @GameTest(template = "mega_drill_floor", timeoutTicks = 400)
    public static void aTrickleCannotRunAGatedMachine(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.ELECTROREFINER.get());
        ProcessingBlockEntity machine = helper.getBlockEntity(AT);
        int[] tick = {0};
        boolean[] on = {true};
        helper.onEachTick(() -> {
            int t = tick[0]++;
            IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(AT), Direction.EAST);
            IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(AT), Direction.UP);
            helper.assertTrue(energy != null && items != null, "the machine takes power and items on its sides");
            if (t == SALT_AT) {
                helper.assertTrue(items.insertItem(0, new ItemStack(ModItems.ORE_DROPS.get(OreType.SALT).get(), 2), false).isEmpty(),
                        "the cell takes salt");
            }
            if (t >= SALT_AT && t < FULL_AT) {
                // The clock: off for six ticks, charging, then on for two.
                boolean want = (t - SALT_AT) % 8 >= 6;
                if (want != on[0]) {
                    machine.togglePower();
                    on[0] = want;
                }
            } else if (t == FULL_AT && !on[0]) {
                machine.togglePower();
                on[0] = true;
            }
            energy.receiveEnergy(t < FULL_AT ? TRICKLE : FULL, false);
            if (t < FULL_AT) {
                helper.assertTrue(machine.progress() == 0, "a trickle makes no progress (tick " + t + ", progress " + machine.progress() + ")");
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(tick[0] > FULL_AT + 20 && machine.progress() > 0,
                "its full draw runs it (progress " + machine.progress() + ")"));
    }
}
