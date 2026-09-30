package com.ryzer.ryzergen.machine.processing;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.Preview;
import com.ryzer.ryzergen.recipe.MachineRecipe.Process;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModMenus;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The fuel cycle machines (design section 7). They share one block entity, menu and screen, and
 * differ only in what is set here: their recipe type, how many input and output slots they have,
 * the power they draw, and whether they hold water. The fuel cycle's run on microreactor power; the
 * Electrorefiner, tier 4's, needs fission power (rule 7).
 *
 * <p>A gated machine (power is the price, design section 5) makes a key material for the next tier.
 * Its draw is also its minimum: its buffer holds only {@link #GATED_BUFFER_TICKS} ticks of it, and a
 * tick without the full draw makes no progress while the power it did get is spent anyway. Real
 * basis: a process that needs a threshold (a furnace's heat, a column's cold) gets nowhere below it
 * but still burns what it is given. So a trickle cannot bank up, and only the current tier's power
 * can run it.
 */
public enum ProcessingMachine {
    /** Crushes depleted fuel cores open: spent kernels out, graphite and steel back. */
    CORE_CRACKER("core_cracker", Process.CRACKING, 1, 3, 20, 0, false, false),
    /** Separates spent fuel into uranium, plutonium and waste, with fluorite and water. Two high. */
    REPROCESSOR("reprocessor", Process.REPROCESSING, 2, 3, 80, 4_000, true, false),
    /** Makes fuel: uranium and MOX rods for the station, TRISO pellets for the microreactor. */
    FUEL_FABRICATOR("fuel_fabricator", Process.FABRICATING, 3, 1, 40, 0, false, false),
    /** Draws lithium out of salt and water, for tritium (design section 10). */
    LITHIUM_EXTRACTOR("lithium_extractor", Process.EXTRACTING, 1, 1, 40, 4_000, false, false),
    /**
     * Pyroprocessing (design section 9): a molten salt cell that plates spent MOX out into
     * transuranic metal, uranium and waste, and splits salt alone into sodium. Tier 4, so it runs
     * on fission power: gated, with a draw no microreactor can meet.
     */
    ELECTROREFINER("electrorefiner", Process.ELECTROREFINING, 2, 3, 1_500, 0, false, true);

    public static final int ENERGY_CAPACITY = 40_000;

    /** False for unfinished machines while the preview is off, so recipe viewers leave them out. */
    public boolean shown() {
        return this != ELECTROREFINER || Preview.enabled();
    }
    /** Enough for a fully upgraded reprocessor (80 FE/t at five times the speed, squared). */
    public static final int MAX_INPUT = 5_000;
    /** Speed modules one machine can hold. */
    public static final int MAX_MODULES = 4;
    /** A gated machine's buffer, in ticks of its draw: enough to ride out cable timing, no more. */
    public static final int GATED_BUFFER_TICKS = 2;

    private final String id;
    private final Process process;
    private final int inputs;
    private final int outputs;
    private final int energyPerTick;
    private final int waterCapacity;
    private final boolean tall;
    private final boolean gated;

    ProcessingMachine(String id, Process process, int inputs, int outputs, int energyPerTick, int waterCapacity, boolean tall,
                      boolean gated) {
        this.id = id;
        this.process = process;
        this.inputs = inputs;
        this.outputs = outputs;
        this.energyPerTick = energyPerTick;
        this.waterCapacity = waterCapacity;
        this.tall = tall;
        this.gated = gated;
    }

    /** The machine that runs a recipe type. */
    public static ProcessingMachine forProcess(Process process) {
        for (ProcessingMachine machine : values()) {
            if (machine.process == process) {
                return machine;
            }
        }
        throw new IllegalArgumentException("No machine runs " + process);
    }

    public String id() {
        return id;
    }

    public Process process() {
        return process;
    }

    public int inputs() {
        return inputs;
    }

    public int outputs() {
        return outputs;
    }

    /** Input and output slots. The upgrade slot comes after them. */
    public int slots() {
        return inputs + outputs;
    }

    /** Where the speed modules go, after the inputs and outputs. */
    public int upgradeSlot() {
        return inputs + outputs;
    }

    /** FE per tick while working at speed 1, as the config scales it. */
    public int energyPerTick() {
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, (long) energyPerTick * Config.drawPercent(this) / 100));
    }

    /** FE per tick before the config scales it. */
    public int baseEnergyPerTick() {
        return energyPerTick;
    }

    /** A recipe's time in ticks at speed 1, as the config scales it. */
    public int time(int recipeTime) {
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, (long) recipeTime * Config.timePercent(this) / 100));
    }

    /** Needs its full draw every tick (see the class notes). */
    public boolean gated() {
        return gated;
    }

    /** mB of water it holds, or 0 if it takes none. */
    public int waterCapacity() {
        return waterCapacity;
    }

    public boolean usesWater() {
        return waterCapacity > 0;
    }

    /** Two blocks high, like a door: the lower half holds the block entity. */
    public boolean tall() {
        return tall;
    }

    public Block block() {
        return switch (this) {
            case CORE_CRACKER -> ModBlocks.CORE_CRACKER.get();
            case REPROCESSOR -> ModBlocks.REPROCESSOR.get();
            case FUEL_FABRICATOR -> ModBlocks.FUEL_FABRICATOR.get();
            case LITHIUM_EXTRACTOR -> ModBlocks.LITHIUM_EXTRACTOR.get();
            case ELECTROREFINER -> ModBlocks.ELECTROREFINER.get();
        };
    }

    public BlockEntityType<ProcessingBlockEntity> blockEntityType() {
        return switch (this) {
            case CORE_CRACKER -> ModBlockEntities.CORE_CRACKER.get();
            case REPROCESSOR -> ModBlockEntities.REPROCESSOR.get();
            case FUEL_FABRICATOR -> ModBlockEntities.FUEL_FABRICATOR.get();
            case LITHIUM_EXTRACTOR -> ModBlockEntities.LITHIUM_EXTRACTOR.get();
            case ELECTROREFINER -> ModBlockEntities.ELECTROREFINER.get();
        };
    }

    public MenuType<ProcessingMenu> menuType() {
        return switch (this) {
            case CORE_CRACKER -> ModMenus.CORE_CRACKER.get();
            case REPROCESSOR -> ModMenus.REPROCESSOR.get();
            case FUEL_FABRICATOR -> ModMenus.FUEL_FABRICATOR.get();
            case LITHIUM_EXTRACTOR -> ModMenus.LITHIUM_EXTRACTOR.get();
            case ELECTROREFINER -> ModMenus.ELECTROREFINER.get();
        };
    }
}
