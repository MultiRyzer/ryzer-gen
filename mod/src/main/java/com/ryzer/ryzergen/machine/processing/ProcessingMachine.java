package com.ryzer.ryzergen.machine.processing;

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
 * the power they draw, and whether they hold water. All run on microreactor power (rule 7).
 */
public enum ProcessingMachine {
    /** Crushes depleted fuel cores open: spent kernels out, graphite and steel back. */
    CORE_CRACKER("core_cracker", Process.CRACKING, 1, 3, 20, 0, false),
    /** Separates spent fuel into uranium, plutonium and waste, with fluorite and water. Two high. */
    REPROCESSOR("reprocessor", Process.REPROCESSING, 2, 3, 80, 4_000, true),
    /** Makes fuel: uranium and MOX rods for the station, TRISO pellets for the microreactor. */
    FUEL_FABRICATOR("fuel_fabricator", Process.FABRICATING, 3, 1, 40, 0, false);

    public static final int ENERGY_CAPACITY = 40_000;
    /** Enough for a fully upgraded reprocessor (80 FE/t at five times the speed, squared). */
    public static final int MAX_INPUT = 5_000;
    /** Speed modules one machine can hold. */
    public static final int MAX_MODULES = 4;

    private final String id;
    private final Process process;
    private final int inputs;
    private final int outputs;
    private final int energyPerTick;
    private final int waterCapacity;
    private final boolean tall;

    ProcessingMachine(String id, Process process, int inputs, int outputs, int energyPerTick, int waterCapacity, boolean tall) {
        this.id = id;
        this.process = process;
        this.inputs = inputs;
        this.outputs = outputs;
        this.energyPerTick = energyPerTick;
        this.waterCapacity = waterCapacity;
        this.tall = tall;
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

    public int energyPerTick() {
        return energyPerTick;
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
        };
    }

    public BlockEntityType<ProcessingBlockEntity> blockEntityType() {
        return switch (this) {
            case CORE_CRACKER -> ModBlockEntities.CORE_CRACKER.get();
            case REPROCESSOR -> ModBlockEntities.REPROCESSOR.get();
            case FUEL_FABRICATOR -> ModBlockEntities.FUEL_FABRICATOR.get();
        };
    }

    public MenuType<ProcessingMenu> menuType() {
        return switch (this) {
            case CORE_CRACKER -> ModMenus.CORE_CRACKER.get();
            case REPROCESSOR -> ModMenus.REPROCESSOR.get();
            case FUEL_FABRICATOR -> ModMenus.FUEL_FABRICATOR.get();
        };
    }
}
