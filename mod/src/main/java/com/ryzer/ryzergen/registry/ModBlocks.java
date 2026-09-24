package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterBlock;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorPartBlock;
import com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlock;
import com.ryzer.ryzergen.material.OreType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RyzerGen.MOD_ID);

    public static final DeferredBlock<AlloySmelterBlock> ALLOY_SMELTER = BLOCKS.register("alloy_smelter",
            () -> new AlloySmelterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE)));

    public static final DeferredBlock<ReactorHeartBlock> REACTOR_HEART = BLOCKS.register("reactor_heart",
            () -> new ReactorHeartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).pushReaction(PushReaction.BLOCK).lightLevel(ModBlocks::microreactorLight)));
    public static final DeferredBlock<MicroreactorPartBlock> REACTOR_MACHINE_UNIT = BLOCKS.register("reactor_machine_unit",
            () -> new MicroreactorPartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).pushReaction(PushReaction.BLOCK).lightLevel(ModBlocks::microreactorLight)));
    public static final DeferredBlock<MicroreactorPartBlock> COOLANT_JACKET = BLOCKS.register("coolant_jacket",
            () -> new MicroreactorPartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).pushReaction(PushReaction.BLOCK).lightLevel(ModBlocks::microreactorLight)));

    public static final Map<OreType, DeferredBlock<DropExperienceBlock>> STONE_ORES = new EnumMap<>(OreType.class);
    public static final Map<OreType, DeferredBlock<DropExperienceBlock>> DEEPSLATE_ORES = new EnumMap<>(OreType.class);

    static {
        for (OreType ore : OreType.values()) {
            STONE_ORES.put(ore, BLOCKS.register(ore.id() + "_ore",
                    () -> new DropExperienceBlock(ore.xp(), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE))));
            DEEPSLATE_ORES.put(ore, BLOCKS.register("deepslate_" + ore.id() + "_ore",
                    () -> new DropExperienceBlock(ore.xp(), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE))));
        }
    }

    /** A running microreactor glows through its porthole and screens. */
    private static int microreactorLight(BlockState state) {
        return state.getValue(MicroreactorPartBlock.RUNNING) ? 9 : 0;
    }

    private ModBlocks() {}
}
