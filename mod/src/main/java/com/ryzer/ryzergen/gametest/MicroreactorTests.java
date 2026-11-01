package com.ryzer.ryzergen.gametest;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorPartBlock;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorSlot;
import com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlockEntity;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The microreactor's ports, on a test server with no player (./gradlew runGameTestServer). The test
 * places the four parts facing north (heart at the lower front, coolant jacket above it, machine
 * units behind), waits for the machine to form, and checks each port is on its face of the back
 * block and nowhere else: as you face the front, water in on your left (east) and steam out on
 * your right (west) low down, fuel in east and spent cores out west on the fuel bay above, energy
 * out on the back. Then it feeds a core and water through those ports and waits for power.
 */
@GameTestHolder(RyzerGen.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MicroreactorTests {
    private static final BlockPos LOWER_FRONT = new BlockPos(5, 1, 5);
    private static final BlockPos UPPER_FRONT = LOWER_FRONT.above();
    private static final BlockPos LOWER_BACK = LOWER_FRONT.south();
    private static final BlockPos UPPER_BACK = LOWER_BACK.above();

    private MicroreactorTests() {}

    @GameTest(template = "mega_drill_floor", timeoutTicks = 400)
    public static void microreactorPortsSitOnTheBackBlock(GameTestHelper helper) {
        place(helper, LOWER_FRONT, ModBlocks.REACTOR_HEART.get());
        place(helper, UPPER_FRONT, ModBlocks.COOLANT_JACKET.get());
        place(helper, LOWER_BACK, ModBlocks.REACTOR_MACHINE_UNIT.get());
        place(helper, UPPER_BACK, ModBlocks.REACTOR_MACHINE_UNIT.get());

        helper.succeedWhen(() -> {
            for (BlockPos pos : new BlockPos[] {LOWER_FRONT, UPPER_FRONT, LOWER_BACK, UPPER_BACK}) {
                helper.assertTrue(helper.getBlockState(pos).getValue(MicroreactorPartBlock.SLOT) != MicroreactorSlot.NONE,
                        "the four parts formed a microreactor");
            }
            // Energy out on the back of the lower back block, and only there.
            IEnergyStorage energy = cap(helper, Capabilities.EnergyStorage.BLOCK, LOWER_BACK, Direction.SOUTH);
            helper.assertTrue(energy != null, "energy port on the back of the lower back block");
            helper.assertTrue(cap(helper, Capabilities.EnergyStorage.BLOCK, LOWER_BACK, Direction.EAST) == null,
                    "no energy port on the lower back block's flank");
            // Water in on the east (your left), steam out on the west (your right).
            IFluidHandler water = cap(helper, Capabilities.FluidHandler.BLOCK, LOWER_BACK, Direction.EAST);
            helper.assertTrue(water != null && water.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE) > 0,
                    "water inlet on the lower back block's east flank");
            IFluidHandler steam = cap(helper, Capabilities.FluidHandler.BLOCK, LOWER_BACK, Direction.WEST);
            helper.assertTrue(steam != null && steam.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE) == 0,
                    "steam outlet (which takes nothing in) on the lower back block's west flank");
            // Fuel in on the fuel bay's east flank, spent cores out on its west flank.
            IItemHandler fuelIn = cap(helper, Capabilities.ItemHandler.BLOCK, UPPER_BACK, Direction.EAST);
            helper.assertTrue(fuelIn != null && fuelIn.isItemValid(0, new ItemStack(ModItems.SEALED_FUEL_CORE.get())),
                    "fuel inlet on the upper back block's east flank");
            IItemHandler fuelOut = cap(helper, Capabilities.ItemHandler.BLOCK, UPPER_BACK, Direction.WEST);
            helper.assertTrue(fuelOut != null && !fuelOut.isItemValid(0, new ItemStack(ModItems.SEALED_FUEL_CORE.get())),
                    "spent-core outlet on the upper back block's west flank");
            // The old places are closed: no fuel port on the front block, no intake on top.
            helper.assertTrue(cap(helper, Capabilities.ItemHandler.BLOCK, LOWER_FRONT, Direction.EAST) == null,
                    "no fuel port on the front block");
            helper.assertTrue(cap(helper, Capabilities.FluidHandler.BLOCK, UPPER_BACK, Direction.UP) == null,
                    "no coolant intake on top");

            // Feed it through the ports, and it makes power.
            if (fuelIn.getStackInSlot(0).isEmpty()) {
                fuelIn.insertItem(0, new ItemStack(ModItems.SEALED_FUEL_CORE.get()), false);
            }
            water.fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
            ReactorHeartBlockEntity heart = helper.getBlockEntity(LOWER_FRONT);
            helper.assertTrue(heart.status().running(), "the reactor runs on a core fed through the fuel port");
            helper.assertTrue(energy.getEnergyStored() > 0, "the reactor makes power");
        });
    }

    private static void place(GameTestHelper helper, BlockPos pos, Block block) {
        helper.setBlock(pos, block.defaultBlockState().setValue(MicroreactorPartBlock.FACING, Direction.NORTH));
    }

    private static <T> @Nullable T cap(GameTestHelper helper, BlockCapability<T, @Nullable Direction> capability, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(capability, helper.absolutePos(pos), side);
    }
}
