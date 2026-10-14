package com.ryzer.ryzergen.gametest;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.fission.StationCoreBlock;
import com.ryzer.ryzergen.machine.fission.StationCoreBlockEntity;
import com.ryzer.ryzergen.machine.fission.StationLayout;
import com.ryzer.ryzergen.machine.fission.StationPart;
import com.ryzer.ryzergen.machine.fission.StationPartBlock;
import com.ryzer.ryzergen.machine.fission.StationReactor;
import com.ryzer.ryzergen.machine.fission.StationRunner;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;

/**
 * The fission station's coolant reserve (design section 8), on a test server (./gradlew
 * runGameTestServer). A station is built facing north on an empty template, then:
 * <ul>
 * <li>the reserve grows only symmetrically: the pair of bays beside the centre counts for nothing
 * until the centre bay is complete, a gap in the centre bay drops it back to none, and the outer
 * pair makes five;</li>
 * <li>it fills from the station's spare water (only what is over half the tank), then holds an
 * overdrive core that has lost its cooling at 620°C while its water lasts, draining at its steady
 * rate, and lets the core heat on once it is dry.</li>
 * </ul>
 */
@GameTestHolder(RyzerGen.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StationReserveTests {
    /** The core, so the design's back (z up to 12) and sides (x from -1 to 12) fit the 20 x 20 template. */
    private static final BlockPos CORE = new BlockPos(8, 1, 2);

    private StationReserveTests() {}

    private static StationCoreBlockEntity build(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(CORE);
        level.setBlock(core, ModBlocks.STATION_CORE.get().defaultBlockState().setValue(StationCoreBlock.FACING, Direction.NORTH),
                Block.UPDATE_ALL);
        for (Map.Entry<BlockPos, StationPart> entry : StationLayout.PARTS.entrySet()) {
            if (entry.getValue() != StationPart.CORE) {
                level.setBlock(StationLayout.toWorld(core, Direction.NORTH, entry.getKey()), entry.getValue().block().defaultBlockState(),
                        Block.UPDATE_ALL);
            }
        }
        StationCoreBlockEntity entity = (StationCoreBlockEntity) level.getBlockEntity(core);
        helper.assertTrue(entity != null && entity.isFormed(), "the station forms");
        return entity;
    }

    private static void ring(GameTestHelper helper, StationCoreBlockEntity core, int ring, boolean place) {
        for (BlockPos cell : StationLayout.RESERVE.get(ring)) {
            helper.getLevel().setBlock(StationLayout.toWorld(core.getBlockPos(), Direction.NORTH, cell),
                    (place ? ModBlocks.COOLANT_RESERVE.get() : Blocks.AIR).defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static boolean formed(GameTestHelper helper, StationCoreBlockEntity core, int ring) {
        BlockPos cell = StationLayout.RESERVE.get(ring).iterator().next();
        var state = helper.getLevel().getBlockState(StationLayout.toWorld(core.getBlockPos(), Direction.NORTH, cell));
        return state.is(ModBlocks.COOLANT_RESERVE.get()) && state.getValue(StationPartBlock.FORMED);
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 100)
    public static void theReserveGrowsSymmetrically(GameTestHelper helper) {
        StationCoreBlockEntity core = build(helper);
        helper.assertTrue(StationLayout.RESERVE.stream().mapToInt(Set::size).sum() == 48, "a full reserve is 48 blocks");
        ring(helper, core, 1, true);
        helper.assertTrue(core.runner().reserveBays() == 0 && !formed(helper, core, 1), "the side pair alone counts for nothing");
        ring(helper, core, 0, true);
        helper.assertTrue(core.runner().reserveBays() == 3 && formed(helper, core, 0) && formed(helper, core, 1),
                "with the centre bay, three bays form (" + core.runner().reserveBays() + ")");
        BlockPos gap = StationLayout.toWorld(core.getBlockPos(), Direction.NORTH, StationLayout.RESERVE.get(0).iterator().next());
        helper.getLevel().setBlock(gap, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(core.runner().reserveBays() == 0 && !formed(helper, core, 1), "a gap in the centre bay drops the reserve to none");
        helper.getLevel().setBlock(gap, ModBlocks.COOLANT_RESERVE.get().defaultBlockState(), Block.UPDATE_ALL);
        ring(helper, core, 2, true);
        helper.assertTrue(core.runner().reserveBays() == 5 && formed(helper, core, 2), "the outer pair makes five");
        helper.assertTrue(core.runner().reserveCapacity() == 5 * StationRunner.RESERVE_PER_BAY, "five bays of water");
        helper.succeed();
    }

    /** Water over half the station's tank: what the reserve may take. */
    private static final int SPARE = 400;

    @GameTest(template = "mega_drill_floor", timeoutTicks = 800)
    public static void theReserveHoldsAnOverdriveCoreWhileItLasts(GameTestHelper helper) {
        StationCoreBlockEntity core = build(helper);
        ring(helper, core, 0, true);
        StationRunner runner = core.runner();
        helper.assertTrue(runner.reserveBays() == 1, "one bay");
        runner.waterPort().fill(new FluidStack(Fluids.WATER, StationRunner.WATER_CAPACITY / 2 + SPARE), IFluidHandler.FluidAction.EXECUTE);
        // An all-fuel core with no coolant channel: every rod's heat stays in the core, as if its
        // cooling were lost. Overdrive, so nothing SCRAMs it.
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            runner.setType(i, StationReactor.Channel.FUEL);
            runner.fuelPort().insertItem(i, new ItemStack(ModItems.URANIUM_FUEL_ROD.get()), false);
        }
        runner.toggleSafeties(true);
        boolean[] held = {false};
        float[] before = {-1};
        int[] heldTicks = {0};
        helper.onEachTick(() -> {
            if (runner.status() == StationRunner.Status.INJECTING) {
                held[0] = true;
                heldTicks[0]++;
                helper.assertTrue(Math.abs(runner.temperature() - StationRunner.RUNAWAY_TEMPERATURE) < 0.01F,
                        "the reserve holds the core at 620°C (" + runner.temperature() + ")");
                helper.assertTrue(before[0] < 0 || runner.reserve() < before[0], "and drains while it does");
                before[0] = runner.reserve();
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(held[0], "the reserve fired");
            helper.assertTrue(runner.reserve() <= 0 && runner.temperature() > StationRunner.RUNAWAY_TEMPERATURE + 5,
                    "dry, the core heats on (" + runner.temperature() + "°C)");
            // 400 mB at 16,000 mB a bay for 2 minutes is 3 seconds: 60 ticks.
            helper.assertTrue(heldTicks[0] >= 55 && heldTicks[0] <= 65, "it held for its water's time (" + heldTicks[0] + " ticks)");
            helper.assertTrue(runner.waterPort().getFluidInTank(0).getAmount() >= StationRunner.WATER_CAPACITY / 2,
                    "it only took the spare water");
            // Re-armed, the core SCRAMs instead of melting down on the test server.
            runner.toggleSafeties(true);
        });
    }
}
