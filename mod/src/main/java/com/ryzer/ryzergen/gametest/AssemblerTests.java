package com.ryzer.ryzergen.gametest;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.assembler.AssemblerBlockEntity;
import com.ryzer.ryzergen.machine.assembler.AssemblerLayout;
import com.ryzer.ryzergen.machine.assembler.AssemblerPart;
import com.ryzer.ryzergen.machine.assembler.AssemblerPartBlock;
import com.ryzer.ryzergen.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The Assembler, on a test server (./gradlew runGameTestServer). Each test builds a cell facing
 * north from its parts, sets its patterns, and drives it through its ports: energy and ingredients
 * in on the east end, results out on the west. One makes sticks and crafting tables from planks in
 * turn; one is fed only half a pattern and must wait, naming what it lacks; one is given no power
 * and must take nothing from its buffer.
 */
@GameTestHolder(RyzerGen.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AssemblerTests {
    private static final BlockPos CONTROLLER = new BlockPos(5, 1, 5);

    private AssemblerTests() {}

    /** Builds the cell round the controller (relative position) and returns the controller. */
    private static AssemblerBlockEntity build(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(CONTROLLER);
        helper.getLevel().setBlockAndUpdate(at, ModBlocks.ASSEMBLER_CONTROLLER.get().defaultBlockState()
                .setValue(AssemblerPartBlock.FACING, Direction.NORTH));
        for (int index = 1; index <= AssemblerLayout.CELLS; index++) {
            BlockPos cell = AssemblerLayout.cell(index);
            AssemblerPart part = AssemblerLayout.partAt(cell);
            if (part != AssemblerPart.CONTROLLER) {
                helper.getLevel().setBlockAndUpdate(AssemblerLayout.toWorld(at, Direction.NORTH, cell), part.block().defaultBlockState());
            }
        }
        AssemblerBlockEntity cell = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(cell.isFormed(), "the cell forms once every part is in");
        return cell;
    }

    private static BlockPos port(AssemblerLayout.Port port) {
        return AssemblerLayout.toWorld(CONTROLLER, Direction.NORTH, port.cell());
    }

    private static IItemHandler items(GameTestHelper helper, AssemblerLayout.Port port) {
        Direction face = port.face(Direction.NORTH);
        return helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(port(port)), face);
    }

    private static IEnergyStorage energy(GameTestHelper helper) {
        AssemblerLayout.Port port = AssemblerLayout.Port.ENERGY;
        return helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(port(port)), port.face(Direction.NORTH));
    }

    private static int count(IItemHandler handler, Item item) {
        int n = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            n += stack.is(item) ? stack.getCount() : 0;
        }
        return n;
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 600)
    public static void craftsItsPatternsInTurn(GameTestHelper helper) {
        AssemblerBlockEntity cell = build(helper);
        // Pattern 1: sticks (two planks, one over the other). Pattern 2: a crafting table (four planks).
        cell.setGhost(0, 0, new ItemStack(Items.OAK_PLANKS));
        cell.setGhost(0, 3, new ItemStack(Items.OAK_PLANKS));
        for (int c : new int[] {0, 1, 3, 4}) {
            cell.setGhost(1, c, new ItemStack(Items.OAK_PLANKS));
        }
        IItemHandler in = items(helper, AssemblerLayout.Port.ITEMS_IN);
        IItemHandler out = items(helper, AssemblerLayout.Port.ITEMS_OUT);
        helper.assertTrue(in != null && out != null && energy(helper) != null, "the ports take items and energy");
        helper.assertTrue(ItemHandlerHelper.insertItemStacked(in, new ItemStack(Items.COBBLESTONE, 4), true).getCount() == 4,
                "the buffer refuses what no pattern uses");
        helper.assertTrue(ItemHandlerHelper.insertItemStacked(in, new ItemStack(Items.OAK_PLANKS, 12), false).isEmpty(),
                "the buffer takes the planks");
        helper.onEachTick(() -> energy(helper).receiveEnergy(1_000, false));
        helper.succeedWhen(() -> {
            int sticks = count(out, Items.STICK);
            int tables = count(out, Items.CRAFTING_TABLE);
            // 12 planks, the patterns taking turns: sticks (2), a table (4), sticks, a table = 8 sticks and 2 tables.
            helper.assertTrue(sticks == 8 && tables == 2, "it makes both, in turn (sticks " + sticks + ", tables " + tables + ")");
            helper.assertTrue(count(in, Items.OAK_PLANKS) == 0, "every plank is used");
        });
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 200)
    public static void waitsForWhatItLacks(GameTestHelper helper) {
        AssemblerBlockEntity cell = build(helper);
        // A torch: coal over a stick. Only the stick is fed.
        cell.setGhost(0, 1, new ItemStack(Items.COAL));
        cell.setGhost(0, 4, new ItemStack(Items.STICK));
        IItemHandler in = items(helper, AssemblerLayout.Port.ITEMS_IN);
        ItemHandlerHelper.insertItemStacked(in, new ItemStack(Items.STICK, 4), false);
        helper.onEachTick(() -> energy(helper).receiveEnergy(1_000, false));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(cell.status() == AssemblerBlockEntity.Status.STARVED, "it waits (" + cell.status() + ")");
            helper.assertTrue(cell.waiting() == 0 && cell.missingMask() == 1 << 1, "it names the coal it lacks (mask " + cell.missingMask() + ")");
            helper.assertTrue(count(in, Items.STICK) == 4, "and takes nothing while it waits");
            helper.succeed();
        });
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 200)
    public static void doesNothingWithoutPower(GameTestHelper helper) {
        AssemblerBlockEntity cell = build(helper);
        cell.setGhost(0, 0, new ItemStack(Items.OAK_PLANKS));
        cell.setGhost(0, 3, new ItemStack(Items.OAK_PLANKS));
        IItemHandler in = items(helper, AssemblerLayout.Port.ITEMS_IN);
        IItemHandler out = items(helper, AssemblerLayout.Port.ITEMS_OUT);
        ItemHandlerHelper.insertItemStacked(in, new ItemStack(Items.OAK_PLANKS, 2), false);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(count(out, Items.STICK) == 0, "no power, no sticks");
            helper.assertTrue(cell.status() == AssemblerBlockEntity.Status.NO_POWER, "it says so (" + cell.status() + ")");
            helper.assertTrue(count(in, Items.OAK_PLANKS) == 2, "and the planks stay in the buffer");
            helper.succeed();
        });
    }
}
