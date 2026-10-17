package com.ryzer.ryzergen.gametest;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.creative.CreativeSourceBlock;
import com.ryzer.ryzergen.showcase.Showcase;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/**
 * The showcase site's machines ({@link Showcase}), each built alone on an empty template (./gradlew
 * runGameTestServer): placed part by part as the showcase command places them, every one forms by
 * its own rules. Forming changes every part's state (its cell, slot or formed flag), so a part
 * still as it was placed means the machine did not form. The creative supplies are left out.
 */
@GameTestHolder(RyzerGen.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShowcaseTests {
    private ShowcaseTests() {}

    private static void forms(GameTestHelper helper, String name) {
        Showcase.Machine machine = Showcase.ROW.stream().filter(m -> m.name().equals(name)).findFirst().orElseThrow();
        Map<BlockPos, BlockState> layout = machine.layout();
        int lx = Integer.MAX_VALUE, lz = Integer.MAX_VALUE;
        for (BlockPos pos : layout.keySet()) {
            lx = Math.min(lx, pos.getX());
            lz = Math.min(lz, pos.getZ());
        }
        BlockPos origin = helper.absolutePos(new BlockPos(1 - lx, 1, 1 - lz));
        Showcase.placeAlone(helper.getLevel(), machine, origin);
        helper.succeedWhen(() -> {
            for (Map.Entry<BlockPos, BlockState> e : layout.entrySet()) {
                BlockState placed = e.getValue();
                if (placed.getBlock() instanceof CreativeSourceBlock) {
                    continue;
                }
                BlockPos pos = origin.offset(e.getKey());
                helper.assertTrue(!helper.getLevel().getBlockState(pos).equals(placed),
                        name + " formed (part at " + e.getKey() + " still as placed: " + placed + ")");
            }
        });
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 40)
    public static void showcaseMicroreactorForms(GameTestHelper helper) {
        forms(helper, "Microreactor");
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 40)
    public static void showcaseContainerBatteryForms(GameTestHelper helper) {
        forms(helper, "Container Battery");
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 40)
    public static void showcasePoolForms(GameTestHelper helper) {
        forms(helper, "Cooling Pool");
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 40)
    public static void showcaseAssemblerForms(GameTestHelper helper) {
        forms(helper, "Assembler");
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 40)
    public static void showcaseStationForms(GameTestHelper helper) {
        forms(helper, "Fission Station");
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 40)
    public static void showcaseBreederForms(GameTestHelper helper) {
        forms(helper, "Breeder Reactor");
    }

    @GameTest(template = "mega_drill_floor", timeoutTicks = 40)
    public static void showcaseMegaDrillForms(GameTestHelper helper) {
        forms(helper, "Mega Drill");
    }
}
