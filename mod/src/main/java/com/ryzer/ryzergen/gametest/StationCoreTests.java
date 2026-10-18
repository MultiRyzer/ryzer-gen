package com.ryzer.ryzergen.gametest;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.cable.CableBlock;
import com.ryzer.ryzergen.cable.CableNetwork;
import com.ryzer.ryzergen.cable.CableSide;
import com.ryzer.ryzergen.machine.fission.StationCoreBlock;
import com.ryzer.ryzergen.machine.fission.StationCoreBlockEntity;
import com.ryzer.ryzergen.machine.fission.StationLayout;
import com.ryzer.ryzergen.machine.fission.StationPart;
import com.ryzer.ryzergen.machine.fission.StationReactor;
import com.ryzer.ryzergen.machine.fission.StationReactor.Channel;
import com.ryzer.ryzergen.machine.fission.StationReactor.Core;
import com.ryzer.ryzergen.machine.fission.StationRunner;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/**
 * The fission station's core puzzle (design section 8, "The core puzzle"), on a test server
 * (./gradlew runGameTestServer):
 * <ul>
 * <li>the rules: the lattice scores 34 and three stars, a chessboard 26, a rod on the edge needs
 * water only on the sides it has, and an uncooled rod scores nothing and strands its heat;</li>
 * <li>a running station: the lattice settles at full load and makes about 8,000 FE/t, while a core
 * of uncooled rods makes nothing and heats up;</li>
 * <li>inject fills the plan from a chest on the input port, eject empties the core into a chest on
 * the output port, each with a clear error when nothing is there;</li>
 * <li>the MOX core: the cross scores 47 and three stars, and hot rods with no control rod run away;</li>
 * <li>switching core ejects the core's parts first, and refuses while they have nowhere to go;</li>
 * <li>an older world's control rod channel loads as empty, and its rod leaves by the output port.</li>
 * </ul>
 */
@GameTestHolder(RyzerGen.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StationCoreTests {
    /** The core, so the design's back and sides fit the 20 x 20 template. */
    private static final BlockPos CORE = new BlockPos(8, 1, 2);
    /** A chessboard: fuel wherever the row and column add up to an even number, water elsewhere. */
    private static final String CHESSBOARD = "FWFWFWFWFWFWFWFWFWFWFWFWF";

    private StationCoreTests() {}

    @GameTest(template = "mega_drill_floor", timeoutTicks = 20)
    public static void theLatticeScoresThirtyFourAndThreeStars(GameTestHelper helper) {
        StationReactor.Analysis lattice = StationReactor.plan(StationReactor.parse(Core.URANIUM.layout), Core.URANIUM);
        helper.assertTrue(lattice.score() == 34, "the lattice scores 34 (" + lattice.score() + ")");
        helper.assertTrue(lattice.stars() == 3, "three stars (" + lattice.stars() + ")");
        helper.assertTrue(lattice.rods() == 9 && lattice.graphite() == 4 && lattice.coolants() == 12, "9 rods, 4 graphite, 12 water");
        helper.assertTrue(lattice.uncooled() == 0 && lattice.holdsSteady(), "every rod cooled");
        helper.assertTrue(Math.abs(lattice.settledTemperature() - StationReactor.FULL_LOAD_TEMPERATURE) < 0.5F,
                "it settles at full load (" + lattice.settledTemperature() + "°C)");
        helper.assertTrue(Math.abs(lattice.plannedOutput() - 8024) < 2, "about 8,000 FE/t out (" + lattice.plannedOutput() + ")");
        helper.succeed();
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 20)
    public static void aChessboardScoresTwentySix(GameTestHelper helper) {
        StationReactor.Analysis chess = StationReactor.plan(StationReactor.parse(CHESSBOARD), Core.URANIUM);
        helper.assertTrue(chess.score() == 26, "a chessboard scores 26 (" + chess.score() + ")");
        helper.assertTrue(chess.stars() == 1, "one star (" + chess.stars() + ")");
        StationReactor.Analysis two = StationReactor.plan(StationReactor.parse("FWFWFWMWMWFWFWFWMW.WFWFWF"), Core.URANIUM);
        helper.assertTrue(two.score() == 30 && two.stars() == 2, "three graphite: 30, two stars (" + two.score() + ")");
        helper.succeed();
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 20)
    public static void anEdgeRodNeedsFewerWaters(GameTestHelper helper) {
        // A corner rod with water on its two sides.
        StationReactor.Analysis corner = StationReactor.plan(StationReactor.parse("FW...W..................."), Core.URANIUM);
        helper.assertTrue(corner.uncooled() == 0 && corner.score() == 2, "a corner rod needs only two waters (" + corner.score() + ")");
        // A rod in the middle with three of its four sides watered.
        StationReactor.Analysis middle = StationReactor.plan(StationReactor.parse(".......W...WFW..........."), Core.URANIUM);
        helper.assertTrue(middle.uncooled() == 1 && middle.score() == 0 && middle.stars() == 0,
                "a middle rod with a dry side scores nothing");
        StationReactor.Analysis wet = StationReactor.plan(StationReactor.parse(".......W...WFW...W......."), Core.URANIUM);
        helper.assertTrue(wet.uncooled() == 0 && wet.score() == 2, "with its fourth water it counts");
        helper.succeed();
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 20)
    public static void anUncooledRodStrandsItsHeat(GameTestHelper helper) {
        // Two rods side by side: neither has water on every side.
        Channel[] plan = StationReactor.parse("FF.......................");
        StationReactor.Analysis dry = StationReactor.plan(plan, Core.URANIUM);
        helper.assertTrue(dry.score() == 0 && dry.uncooled() == 2, "no score from uncooled rods");
        helper.assertTrue(dry.stranded() > 0 && Math.abs(dry.stranded() - dry.generation()) < 0.5F, "all their heat is stranded");
        helper.assertTrue(!dry.holdsSteady() && dry.plannedOutput() == 0, "the core cannot hold steady");
        helper.succeed();
    }

    private static StationCoreBlockEntity build(GameTestHelper helper) {
        return build(helper, Direction.NORTH, CORE);
    }

    /** A station facing {@code facing}, its core at {@code at} in the template. */
    private static StationCoreBlockEntity build(GameTestHelper helper, Direction facing, BlockPos at) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(at);
        level.setBlock(core, ModBlocks.STATION_CORE.get().defaultBlockState().setValue(StationCoreBlock.FACING, facing),
                Block.UPDATE_ALL);
        for (Map.Entry<BlockPos, StationPart> entry : StationLayout.PARTS.entrySet()) {
            if (entry.getValue() != StationPart.CORE) {
                level.setBlock(StationLayout.toWorld(core, facing, entry.getKey()), entry.getValue().block().defaultBlockState(),
                        Block.UPDATE_ALL);
            }
        }
        StationCoreBlockEntity entity = (StationCoreBlockEntity) level.getBlockEntity(core);
        helper.assertTrue(entity != null && entity.isFormed(), "the station forms");
        return entity;
    }

    private static void plan(StationRunner runner, String layout) {
        Channel[] plan = StationReactor.parse(layout);
        for (int i = 0; i < plan.length; i++) {
            runner.setType(i, plan[i]);
        }
    }

    /** The space beside a port's face, where a chest or pipe goes. */
    private static BlockPos beside(StationCoreBlockEntity core, StationLayout.Port port) {
        Direction face = StationLayout.portFace(core.facing(), port);
        return StationLayout.portPos(core.getBlockPos(), core.facing(), port).relative(face);
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos at) {
        helper.getLevel().setBlock(at, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        return (ChestBlockEntity) helper.getLevel().getBlockEntity(at);
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 20)
    public static void injectAndEjectUseThePorts(GameTestHelper helper) {
        StationCoreBlockEntity core = build(helper);
        StationRunner runner = core.runner();
        ServerLevel level = helper.getLevel();
        plan(runner, Core.URANIUM.layout);
        StationRunner.Result none = runner.inject(level, core.getBlockPos(), Direction.NORTH);
        helper.assertTrue(none.command() == StationRunner.Command.NO_INPUT && none.missing() == 13,
                "with nothing on the input port, inject says so (" + none + ")");
        ChestBlockEntity input = chest(helper, beside(core, StationLayout.Port.FUEL));
        input.setItem(0, new ItemStack(ModItems.URANIUM_FUEL_ROD.get(), 9));
        input.setItem(1, new ItemStack(ModItems.GRAPHITE_BLOCK.get(), 3));
        StationRunner.Result part = runner.inject(level, core.getBlockPos(), Direction.NORTH);
        helper.assertTrue(part.command() == StationRunner.Command.INJECTED_PART && part.moved() == 12 && part.missing() == 1,
                "short a graphite, it puts in what there is (" + part + ")");
        input.setItem(1, new ItemStack(ModItems.GRAPHITE_BLOCK.get(), 1));
        StationRunner.Result all = runner.inject(level, core.getBlockPos(), Direction.NORTH);
        helper.assertTrue(all.command() == StationRunner.Command.INJECTED && all.moved() == 1, "then the last one (" + all + ")");
        StationReactor.Analysis running = StationReactor.analyse(runner.types(), runner.rods(), false, Core.URANIUM);
        helper.assertTrue(running.score() == 34 && running.stars() == 3, "the filled core is the lattice (" + running.score() + ")");

        StationRunner.Result nowhere = runner.eject(level, core.getBlockPos(), Direction.NORTH);
        helper.assertTrue(nowhere.command() == StationRunner.Command.NO_OUTPUT, "with nothing on the output port, eject says so");
        ChestBlockEntity output = chest(helper, beside(core, StationLayout.Port.OUTPUT));
        StationRunner.Result out = runner.eject(level, core.getBlockPos(), Direction.NORTH);
        helper.assertTrue(out.command() == StationRunner.Command.EJECTED && out.moved() == 13, "eject empties the core (" + out + ")");
        helper.assertTrue(output.countItem(ModItems.URANIUM_FUEL_ROD.get()) == 9 && output.countItem(ModItems.GRAPHITE_BLOCK.get()) == 4,
                "into the output chest");
        helper.assertTrue(runner.fuelPort().insertItem(0, new ItemStack(ModItems.URANIUM_FUEL_ROD.get()), true).getCount() == 1,
                "after an eject the fuel port takes nothing until the next inject");
        helper.succeed();
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 400)
    public static void theLatticeRunsAndUncooledRodsHeatTheCore(GameTestHelper helper) {
        StationCoreBlockEntity core = build(helper);
        StationRunner runner = core.runner();
        // A dry core: every channel fuel, so no rod has water.
        plan(runner, "FFFFFFFFFFFFFFFFFFFFFFFFF");
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            runner.fuelPort().insertItem(i, new ItemStack(ModItems.URANIUM_FUEL_ROD.get()), false);
        }
        runner.waterPort().fill(new FluidStack(Fluids.WATER, StationRunner.WATER_CAPACITY), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(300, () -> {
            helper.assertTrue(runner.temperature() > 300, "the stranded heat warms the core (" + runner.temperature() + "°C)");
            helper.assertTrue(runner.output() == 0, "and makes no power");
            helper.succeed();
        });
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 20)
    public static void theMoxCrossScoresFortySeven(GameTestHelper helper) {
        StationReactor.Analysis cross = StationReactor.plan(StationReactor.parse(Core.MOX.layout), Core.MOX);
        helper.assertTrue(cross.score() == 47 && cross.stars() == 3, "the cross scores 47 and three stars (" + cross.score() + ")");
        helper.assertTrue(cross.rods() == 8 && cross.graphite() == 4 && cross.controls() == 1, "8 rods, 4 graphite, 1 control rod");
        helper.assertTrue(cross.hot() == 4 && cross.runaway() == 0, "the four hot rods are tamed");
        helper.assertTrue(Math.abs(cross.settledTemperature() - StationReactor.FULL_LOAD_TEMPERATURE) < 0.5F,
                "it settles at full load (" + cross.settledTemperature() + "°C)");
        helper.assertTrue(Math.abs(cross.plannedOutput() - 15_980) < 5, "about 16,000 FE/t out (" + cross.plannedOutput() + ")");
        // The uranium core takes no control rods.
        helper.assertTrue(StationReactor.plan(StationReactor.parse(Core.MOX.layout), Core.URANIUM).controls() == 0,
                "a control rod does nothing in the uranium core");
        helper.succeed();
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 20)
    public static void hotMoxRodsNeedAControlRod(GameTestHelper helper) {
        // The cross with water in place of its control rod: the four inner rods run away.
        StationReactor.Analysis wild = StationReactor.plan(StationReactor.parse(Core.MOX.layout.replace('C', 'W')), Core.MOX);
        helper.assertTrue(wild.runaway() == 4 && wild.stars() == 0, "four runaway rods, no stars (" + wild.runaway() + ")");
        helper.assertTrue(wild.score() == 16, "only the corner rods score (" + wild.score() + ")");
        helper.assertTrue(!wild.holdsSteady(), "their heat is stranded");
        helper.succeed();
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 20)
    public static void switchingCoreEjectsFirst(GameTestHelper helper) {
        StationCoreBlockEntity core = build(helper);
        StationRunner runner = core.runner();
        ServerLevel level = helper.getLevel();
        plan(runner, Core.URANIUM.layout);
        ChestBlockEntity input = chest(helper, beside(core, StationLayout.Port.FUEL));
        input.setItem(0, new ItemStack(ModItems.URANIUM_FUEL_ROD.get(), 9));
        input.setItem(1, new ItemStack(ModItems.GRAPHITE_BLOCK.get(), 4));
        runner.inject(level, core.getBlockPos(), Direction.NORTH);
        StationRunner.Result refused = runner.switchCore(level, core.getBlockPos(), Direction.NORTH, Core.MOX);
        helper.assertTrue(refused.command() == StationRunner.Command.NO_OUTPUT && runner.core() == Core.URANIUM,
                "with nowhere for its parts to go, the core does not switch (" + refused + ")");
        ChestBlockEntity output = chest(helper, beside(core, StationLayout.Port.OUTPUT));
        StationRunner.Result switched = runner.switchCore(level, core.getBlockPos(), Direction.NORTH, Core.MOX);
        helper.assertTrue(switched.command() == StationRunner.Command.SWITCHED && switched.moved() == 13 && runner.core() == Core.MOX,
                "it ejects the 13 parts and switches (" + switched + ")");
        helper.assertTrue(output.countItem(ModItems.URANIUM_FUEL_ROD.get()) == 9, "the rods are in the output chest");
        helper.assertTrue(java.util.Arrays.equals(runner.plan(Core.URANIUM), StationReactor.parse(Core.URANIUM.layout)),
                "the uranium plan is kept");
        helper.assertTrue(runner.fuelPort().insertItem(0, new ItemStack(ModItems.URANIUM_FUEL_ROD.get()), true).getCount() == 1,
                "the MOX core takes no uranium rods");
        helper.succeed();
    }

    /** Fills the lattice from a chest on the input port: 13 parts in. */
    private static void loadLattice(GameTestHelper helper, StationCoreBlockEntity core) {
        StationRunner runner = core.runner();
        plan(runner, Core.URANIUM.layout);
        ChestBlockEntity input = chest(helper, beside(core, StationLayout.Port.FUEL));
        input.setItem(0, new ItemStack(ModItems.URANIUM_FUEL_ROD.get(), 9));
        input.setItem(1, new ItemStack(ModItems.GRAPHITE_BLOCK.get(), 4));
        StationRunner.Result in = runner.inject(helper.getLevel(), core.getBlockPos(), core.facing());
        helper.assertTrue(in.command() == StationRunner.Command.INJECTED && in.moved() == 13, "the lattice goes in (" + in + ")");
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 20)
    public static void ejectFindsTheChestOnAStationFacingEast(GameTestHelper helper) {
        // The station turned a quarter round, its back to the template's west, so the ports' faces turn with it.
        StationCoreBlockEntity core = build(helper, Direction.EAST, new BlockPos(17, 1, 8));
        loadLattice(helper, core);
        ChestBlockEntity output = chest(helper, beside(core, StationLayout.Port.OUTPUT));
        StationRunner.Result out = core.runner().eject(helper.getLevel(), core.getBlockPos(), core.facing());
        helper.assertTrue(out.command() == StationRunner.Command.EJECTED && out.moved() == 13, "eject finds the chest (" + out + ")");
        helper.assertTrue(output.countItem(ModItems.URANIUM_FUEL_ROD.get()) == 9 && output.countItem(ModItems.GRAPHITE_BLOCK.get()) == 4,
                "and the parts are in it");
        helper.succeed();
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 400)
    public static void ejectDrainsThroughAPipePullingFromThePort(GameTestHelper helper) {
        StationCoreBlockEntity core = build(helper);
        StationRunner runner = core.runner();
        ServerLevel level = helper.getLevel();
        loadLattice(helper, core);
        // A pipe pulling from the output port (its side there set to extract), into a chest beyond it.
        Direction face = StationLayout.portFace(core.facing(), StationLayout.Port.OUTPUT);
        BlockPos pipeAt = beside(core, StationLayout.Port.OUTPUT);
        ChestBlockEntity output = chest(helper, pipeAt.relative(face));
        CableBlock pipe = (CableBlock) ModBlocks.ITEM_PIPE.get();
        level.setBlock(pipeAt, pipe.connect(pipe.defaultBlockState(), level, pipeAt), Block.UPDATE_ALL);
        level.setBlock(pipeAt, level.getBlockState(pipeAt).setValue(CableBlock.SIDES.get(face.getOpposite()), CableSide.EXTRACT), Block.UPDATE_ALL);
        CableNetwork.changed(level, pipeAt);
        StationRunner.Result out = runner.eject(level, core.getBlockPos(), core.facing());
        helper.assertTrue(out.command() == StationRunner.Command.DRAINING && out.missing() == 13,
                "a pulling pipe offers nothing to push into, so the core drains instead of failing (" + out + ")");
        helper.succeedWhen(() -> {
            helper.assertTrue(output.countItem(ModItems.URANIUM_FUEL_ROD.get()) == 9 && output.countItem(ModItems.GRAPHITE_BLOCK.get()) == 4,
                    "the pipe carries all 13 parts to the chest");
            for (int i = 0; i < StationReactor.CHANNELS; i++) {
                helper.assertTrue(runner.item(i).isEmpty(), "the core is empty");
            }
        });
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 20)
    public static void anOldControlRodChannelLoadsEmpty(GameTestHelper helper) {
        StationCoreBlockEntity core = build(helper);
        ServerLevel level = helper.getLevel();
        CompoundTag tag = core.saveWithoutMetadata(level.registryAccess());
        int[] plan = tag.getIntArray("plan");
        plan[0] = Channel.CONTROL.ordinal();
        plan[1] = Channel.TARGET.ordinal();
        tag.putIntArray("plan", plan);
        CompoundTag channels = tag.getCompound("channels");
        ListTag items = new ListTag();
        CompoundTag rod = (CompoundTag) new ItemStack(ModItems.CONTROL_ROD.get()).save(level.registryAccess());
        rod.putInt("Slot", 0);
        items.add(rod);
        channels.put("Items", items);
        tag.put("channels", channels);
        core.loadWithComponents(tag, level.registryAccess());
        StationRunner runner = core.runner();
        helper.assertTrue(runner.type(0) == Channel.EMPTY && runner.type(1) == Channel.EMPTY, "the retired channels load as empty");
        helper.assertTrue(runner.outputPort().getStackInSlot(0).is(ModItems.CONTROL_ROD.get()), "and the control rod waits at the output port");
        helper.succeed();
    }
}
