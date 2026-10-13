package com.ryzer.ryzergen.gametest;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.drill.MegaDrillBlockEntity;
import com.ryzer.ryzergen.machine.drill.MegaDrillLayout;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The mega drill, end to end, on a test server with no player (run with ./gradlew runGameTestServer).
 * The test lays a stone floor with an iron ore, a chest of loot and a water source in the pit's wall,
 * places a controller on it and feeds it frame by pipe, so the drill builds and forms itself; then it
 * fits a Mk III head and four speed modules, keeps its ports fed (energy, water, a tip) and lets it mine
 * down to the bottom of the world and on into the deep bore. It passes once: the ore and the loot came
 * out of the output port, the footings stand cast into support piers, the water in the wall set into obsidian, the tip
 * wore, and the deep bore has brought up at least two cycles' ore.
 */
@GameTestHolder(RyzerGen.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MegaDrillTests {
    /** The template is 20 x 24 x 20 of air; the test lays its own floor, 6 blocks deep. */
    private static final int FLOOR = 6;
    /** The controller, front and centre of the base, on the floor; the pit's middle is 4 blocks behind it. */
    private static final BlockPos CONTROLLER = new BlockPos(9, FLOOR, 6);
    private static final BlockPos CENTRE = CONTROLLER.offset(0, 0, 4);

    private MegaDrillTests() {}

    @GameTest(template = "mega_drill_floor", timeoutTicks = 6000)
    public static void megaDrillMinesItsPit(GameTestHelper helper) {
        for (int x = 0; x < 20; x++) {
            for (int z = 0; z < 20; z++) {
                for (int y = 0; y < FLOOR; y++) {
                    helper.setBlock(x, y, z, Blocks.STONE);
                }
            }
        }
        BlockPos ore = CENTRE.offset(0, -4, 0);
        BlockPos chest = CENTRE.offset(2, -5, 2);
        // In the ring just outside the pit, on a layer the drill mines.
        BlockPos wall = CENTRE.offset(MegaDrillLayout.LOW - 1, -3, 0);
        BlockPos footing = CENTRE.offset(-3, -2, -3);
        helper.setBlock(ore, Blocks.IRON_ORE);
        helper.setBlock(chest, Blocks.CHEST);
        if (helper.getBlockEntity(chest) instanceof ChestBlockEntity box) {
            box.setItem(0, new ItemStack(Items.DIAMOND, 3));
        }
        helper.setBlock(wall, Blocks.WATER);

        helper.setBlock(CONTROLLER, ModBlocks.MEGA_DRILL_CONTROLLER.get().defaultBlockState());
        MegaDrillBlockEntity drill = helper.getBlockEntity(CONTROLLER);
        IItemHandler parts = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(CONTROLLER), null);
        helper.assertTrue(parts != null, "the unformed controller takes frame by pipe");
        for (int i = 0; i < 4; i++) {
            ItemStack left = parts.insertItem(i, new ItemStack(ModItems.DRILL_FRAME.get(), 64), false);
            helper.assertTrue(left.isEmpty(), "the parts store takes a stack of frame");
        }
        drill.tools().insertItem(MegaDrillBlockEntity.SLOT_HEAD, new ItemStack(ModItems.DRILL_HEAD_MK3.get()), false);
        drill.tools().insertItem(MegaDrillBlockEntity.SLOT_MODULES, new ItemStack(ModItems.SPEED_MODULE.get(), 4), false);

        // The ports: energy, tips and water in on the east (the front's left, facing north), output on the west.
        BlockPos energyPort = port(MegaDrillLayout.Port.ENERGY);
        BlockPos tipPort = port(MegaDrillLayout.Port.TIPS);
        BlockPos waterPort = port(MegaDrillLayout.Port.WATER);
        BlockPos outputPort = port(MegaDrillLayout.Port.OUTPUT);
        // Raw iron, diamonds, everything, and everything when the deep bore began (-1 until then).
        int[] mined = {0, 0, 0, -1};
        helper.onEachTick(() -> {
            if (!drill.isFormed()) {
                return;
            }
            IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
                    helper.absolutePos(energyPort), Direction.EAST);
            IFluidHandler water = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                    helper.absolutePos(waterPort), Direction.EAST);
            IItemHandler tips = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(tipPort), Direction.EAST);
            IItemHandler output = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(outputPort), Direction.WEST);
            helper.assertTrue(energy != null && water != null && tips != null && output != null, "every port answers on its face");
            energy.receiveEnergy(Integer.MAX_VALUE, false);
            water.fill(new FluidStack(Fluids.WATER, 16_000), IFluidHandler.FluidAction.EXECUTE);
            if (tips.getStackInSlot(0).isEmpty()) {
                tips.insertItem(0, new ItemStack(ModItems.TUNGSTEN_TIP.get()), false);
            }
            // Pull everything out of the output port, as a pipe would, and count what matters.
            if (mined[3] < 0 && drill.status() == MegaDrillBlockEntity.Status.BORING) {
                mined[3] = mined[2];
            }
            for (int slot = 0; slot < output.getSlots(); slot++) {
                ItemStack taken = output.extractItem(slot, 64, false);
                mined[2] += taken.getCount();
                if (mined[3] >= 0) {
                    continue;
                }
                if (taken.is(Items.RAW_IRON)) {
                    mined[0] += taken.getCount();
                } else if (taken.is(Items.DIAMOND)) {
                    mined[1] += taken.getCount();
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(drill.isFormed(), "the drill builds itself and forms");
            helper.assertTrue(mined[3] >= 0, "the drill mines to the bottom and bores on (now " + drill.status() + ")");
            helper.assertTrue(mined[2] - mined[3] >= 6, "the deep bore brings up ore (" + (mined[2] - mined[3]) + " so far)");
            helper.assertTrue(mined[0] > 0, "the iron ore comes up as raw iron");
            helper.assertTrue(mined[1] == 3, "the chest's diamonds come up (" + mined[1] + ")");
            helper.assertBlockPresent(Blocks.AIR, ore);
            helper.assertBlockPresent(ModBlocks.DRILL_PIER.get(), footing);
            helper.assertBlockPresent(Blocks.OBSIDIAN, wall);
            ItemStack tip = drill.tools().getStackInSlot(MegaDrillBlockEntity.SLOT_TIP);
            helper.assertTrue(!tip.isEmpty() && tip.getDamageValue() > 0, "the tip wears");
        });
    }

    /** A port's cell in the test, for a controller facing north at {@link #CONTROLLER}. */
    private static BlockPos port(MegaDrillLayout.Port port) {
        return MegaDrillLayout.toWorld(CONTROLLER, Direction.NORTH, port.cell());
    }
}
