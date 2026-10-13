package com.ryzer.ryzergen.machine.assembler;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.client.AssemblerGhostPreview;
import com.ryzer.ryzergen.machine.MachineEnergyStorage;
import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.machine.SoundSwitch;
import com.ryzer.ryzergen.machine.SpeedModuleItem;
import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Assembler controller's block entity (design section 14). Before the cell forms it builds it:
 * feed its store frames and the arm and it places them itself, bottom layer first. Once formed it
 * runs it: nine patterns, each a 3 x 3 grid of ghost items set on its screen, any crafting-table
 * recipe; a buffer that pipes fill, which takes only what the patterns use and holds one stack of
 * each item, so a mixed line never clogs it; and the arm, which works through the patterns in turn,
 * crafting whichever its buffer can make and its output has room for.
 *
 * <p>A craft is a cycle the players see: the arm carries each ingredient from the in-feed belt to
 * its fixture on the bed (where the recipe puts it), half a second a trip, the grid becomes the
 * result, and the arm carries that to the out-feed belt. The ingredients leave the buffer when the
 * cycle starts and the result reaches the output when it ends. Each speed module lays one more
 * craft's worth on each fixture (a cycle crafts that many at once) and the draw grows with the
 * square of the speed, as on every machine that takes them. Real basis: a robot assembly cell.
 */
public class AssemblerBlockEntity extends BlockEntity implements MenuProvider, SoundSwitch.Holder {
    public static final int PART_SLOTS = 9;
    public static final int PATTERNS = 9;
    public static final int GRID = 9;
    public static final int BUFFER = 10;
    public static final int OUTPUTS = 6;
    public static final int ENERGY_CAPACITY = 20_000;
    private static final int ENERGY_INTAKE = 2_000;
    /** Ticks per trip of the arm. */
    public static final int TRIP_TICKS = 10;
    /** Blocks placed per build step, and ticks between steps. */
    private static final int PER_STEP = 3;
    private static final int STEP_TICKS = 2;
    /** How often an idle cell looks for a pattern it can make. */
    private static final int LOOK_TICKS = 10;

    /** The sound switch (the speaker key on its screen). */
    private final SoundSwitch sound = new SoundSwitch(this);

    @Override
    public SoundSwitch sound() {
        return sound;
    }

    /** What the cell is doing, for its screen, its stack light and its look. */
    public enum Status {
        OFF, REDSTONE, NO_PATTERN, STARVED, OUTPUT_FULL, NO_POWER, WORKING;

        public static Status byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : OFF;
        }
    }

    public static final int DATA_MISSING_FRAME = 0;
    public static final int DATA_MISSING_ARM = 1;
    public static final int DATA_BLOCKED = 2;
    public static final int DATA_FORMED = 3;
    public static final int DATA_ENERGY_LOW = 4;
    public static final int DATA_ENERGY_HIGH = 5;
    public static final int DATA_STATUS = 6;
    public static final int DATA_ENABLED = 7;
    public static final int DATA_REDSTONE = 8;
    public static final int DATA_SPEED = 9;
    public static final int DATA_PROGRESS = 10;
    public static final int DATA_TOTAL = 11;
    public static final int DATA_ACTIVE = 12;
    public static final int DATA_WAITING = 13;
    public static final int DATA_MISSING_MASK = 14;
    public static final int DATA_COUNT = 15;

    /** Build order: the design's cells bottom layer first, each layer front to back. */
    private static final List<BlockPos> ORDER = new ArrayList<>();

    static {
        for (int index = 1; index <= AssemblerLayout.CELLS; index++) {
            ORDER.add(AssemblerLayout.cell(index));
        }
        ORDER.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ).thenComparingInt(BlockPos::getX));
    }

    private final ItemStackHandler parts = new ItemStackHandler(PART_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return partFor(stack) != null;
        }
    };

    /** The speed modules. */
    private final ItemStackHandler tools = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return ProcessingMachine.MAX_MODULES;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof SpeedModuleItem;
        }
    };

    /** Ingredients waiting: only what a pattern uses, one stack of each item. */
    private final ItemStackHandler buffer = new ItemStackHandler(BUFFER) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (!wanted(stack)) {
                return false;
            }
            ItemStack here = getStackInSlot(slot);
            if (!here.isEmpty()) {
                return ItemStack.isSameItemSameComponents(here, stack);
            }
            for (int other = 0; other < getSlots(); other++) {
                if (other != slot && ItemStack.isSameItemSameComponents(getStackInSlot(other), stack)) {
                    return false;
                }
            }
            return true;
        }
    };

    private final ItemStackHandler output = new ItemStackHandler(OUTPUTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final MachineEnergyStorage energy = new MachineEnergyStorage(ENERGY_CAPACITY, ENERGY_INTAKE, this::setChanged);

    /** The patterns' ghost grids, by pattern and cell (row by row from the top left). */
    private final ItemStack[][] patterns = new ItemStack[PATTERNS][GRID];
    /** Each pattern's recipe, looked up when needed and forgotten when the pattern changes. */
    @SuppressWarnings("unchecked")
    private final RecipeHolder<CraftingRecipe>[] recipes = new RecipeHolder[PATTERNS];
    private final boolean[] looked = new boolean[PATTERNS];
    /** For each pattern, which of its cells each item can fill (a bit a cell), worked out once per item. */
    @SuppressWarnings("unchecked")
    private final Map<Item, Integer>[] fitting = new Map[PATTERNS];
    /** Each pattern's result, worked out once (the screen and the rack show them every frame). */
    private final ItemStack[] results = new ItemStack[PATTERNS];

    private boolean enabled = true;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    private Status status = Status.NO_PATTERN;
    /** The pattern the next look starts from, so the patterns take turns. */
    private int nextPattern;

    // The cycle under way (active < 0 when none).
    private int active = -1;
    private int batch;
    private int progress;
    private int total;
    /** What the cycle took from the buffer, by cell, and what it made (held until it reaches the output). */
    private final ItemStack[] bed = new ItemStack[GRID];
    private final List<ItemStack> made = new ArrayList<>();
    /** The pattern an idle cell is waiting on, and the cells whose item it lacks (a bit each). */
    private int waiting = -1;
    private int missingMask;
    private boolean syncNeeded = true;
    private long lastWork = Long.MIN_VALUE / 2;
    private final int[] missing = new int[2];
    private int blocked;

    // Client: what the renderer animates, from the last update.
    private long syncedAt;
    private int syncedProgress;
    private boolean syncedRunning;
    private ItemStack syncedResult = ItemStack.EMPTY;
    /** Each cycle's number (counted at its start), and the last whose result went onto the out-feed (client). */
    private int cycleId;
    private int droppedId = -1;
    /** Results riding the out-feed belt (client): each one once, from where the arm set it down into the hood. */
    private final List<Drop> drops = new ArrayList<>();
    /** What the server laid on the bed this cycle (client), and for which cycle. */
    private final ItemStack[] shown = new ItemStack[GRID];
    private int shownCycle = -1;

    /** A result on the out-feed, and when the arm set it down. */
    public record Drop(ItemStack stack, long at) {}

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_MISSING_FRAME, DATA_MISSING_ARM -> missing[index];
                case DATA_BLOCKED -> blocked;
                case DATA_FORMED -> isFormed() ? 1 : 0;
                case DATA_ENERGY_LOW -> energy.getEnergyStored() & 0xFFFF;
                case DATA_ENERGY_HIGH -> energy.getEnergyStored() >>> 16;
                case DATA_STATUS -> status.ordinal();
                case DATA_ENABLED -> enabled ? 1 : 0;
                case DATA_REDSTONE -> redstoneMode.ordinal();
                case DATA_SPEED -> speed();
                case DATA_PROGRESS -> progress;
                case DATA_TOTAL -> total;
                case DATA_ACTIVE -> active;
                case DATA_WAITING -> waiting;
                case DATA_MISSING_MASK -> missingMask;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLER.get(), pos, state);
        for (ItemStack[] pattern : patterns) {
            java.util.Arrays.fill(pattern, ItemStack.EMPTY);
        }
        java.util.Arrays.fill(bed, ItemStack.EMPTY);
        java.util.Arrays.fill(shown, ItemStack.EMPTY);
    }

    /** Which buildable part an item is, or null. The controller itself is never stocked. */
    public static @Nullable AssemblerPart partFor(ItemStack stack) {
        for (AssemblerPart part : new AssemblerPart[] {AssemblerPart.FRAME, AssemblerPart.ARM}) {
            if (stack.is(part.block().asItem())) {
                return part;
            }
        }
        return null;
    }

    public boolean isFormed() {
        return AssemblerPartBlock.isFormed(getBlockState());
    }

    public Direction facing() {
        return getBlockState().getValue(AssemblerPartBlock.FACING);
    }

    public BlockPos cellPos(BlockPos cell) {
        return AssemblerLayout.toWorld(worldPosition, facing(), cell);
    }

    /** 1, plus one for each speed module. */
    public int speed() {
        return 1 + Math.min(ProcessingMachine.MAX_MODULES, tools.getStackInSlot(0).getCount());
    }

    /** FE per tick while the arm works. */
    public int draw() {
        return draw(speed());
    }

    /** FE per tick while the arm works, at a speed. */
    public static int draw(int speed) {
        return Config.get(Config.ASSEMBLER_DRAW) * speed * speed;
    }

    // ---------------------------------------------------------------- patterns

    public ItemStack ghost(int pattern, int cell) {
        return patterns[pattern][cell];
    }

    /** Sets one cell of a pattern's grid (one of the item, or nothing), from the screen. */
    public void setGhost(int pattern, int cell, ItemStack stack) {
        patterns[pattern][cell] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        patternChanged(pattern);
    }

    public void clearPattern(int pattern) {
        java.util.Arrays.fill(patterns[pattern], ItemStack.EMPTY);
        patternChanged(pattern);
    }

    private void patternChanged(int pattern) {
        recipes[pattern] = null;
        looked[pattern] = false;
        results[pattern] = null;
        fitting[pattern] = null;
        syncNeeded = true;
        setChanged();
    }

    public boolean patternEmpty(int pattern) {
        for (ItemStack stack : patterns[pattern]) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** The pattern's grid as the crafting table would see it, one of each item. */
    private CraftingInput input(int pattern) {
        List<ItemStack> items = new ArrayList<>(GRID);
        for (ItemStack stack : patterns[pattern]) {
            items.add(stack.copyWithCount(1));
        }
        return CraftingInput.of(3, 3, items);
    }

    /** The recipe a pattern makes, or null if it makes none. Works on both sides. */
    public @Nullable RecipeHolder<CraftingRecipe> recipe(int pattern) {
        if (level == null || patternEmpty(pattern)) {
            return null;
        }
        if (!looked[pattern]) {
            looked[pattern] = true;
            recipes[pattern] = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input(pattern), level).orElse(null);
        }
        return recipes[pattern];
    }

    /** What a pattern makes (one craft), or nothing. */
    public ItemStack result(int pattern) {
        if (results[pattern] == null) {
            RecipeHolder<CraftingRecipe> recipe = recipe(pattern);
            if (level == null) {
                return ItemStack.EMPTY;
            }
            results[pattern] = recipe == null ? ItemStack.EMPTY : recipe.value().assemble(input(pattern), level.registryAccess());
        }
        return results[pattern];
    }

    /**
     * The cells of a pattern an item can fill, a bit a cell: the cells set with that item, and any
     * other the recipe takes it in, found by putting it in the grid and asking the recipe. So a
     * pattern set with one mod's lead takes any lead the recipe's tag (c:ingots/lead) allows.
     */
    private int cellsFor(int pattern, Item item) {
        if (fitting[pattern] == null) {
            fitting[pattern] = new HashMap<>();
        }
        Integer known = fitting[pattern].get(item);
        if (known != null) {
            return known;
        }
        int mask = 0;
        RecipeHolder<CraftingRecipe> recipe = recipe(pattern);
        for (int c = 0; c < GRID; c++) {
            ItemStack ghost = patterns[pattern][c];
            if (ghost.isEmpty()) {
                continue;
            }
            if (ghost.is(item)) {
                mask |= 1 << c;
            } else if (recipe != null && level != null) {
                List<ItemStack> items = new ArrayList<>(GRID);
                for (int k = 0; k < GRID; k++) {
                    items.add(k == c ? new ItemStack(item) : patterns[pattern][k].copyWithCount(1));
                }
                if (recipe.value().matches(CraftingInput.of(3, 3, items), level)) {
                    mask |= 1 << c;
                }
            }
        }
        fitting[pattern].put(item, mask);
        return mask;
    }

    /** Whether any pattern uses an item, so the buffer takes it. */
    private boolean wanted(ItemStack stack) {
        for (int p = 0; p < PATTERNS; p++) {
            if (!patternEmpty(p) && cellsFor(p, stack.getItem()) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Which buffer slot fills each cell of a pattern for {@code crafts} crafts, each cell from one
     * slot (the item it was set with first, then any other that fits), or null if the buffer cannot.
     * {@code lacking} collects the cells it cannot fill.
     */
    private int @Nullable [] plan(int pattern, int crafts, int[] lacking) {
        int[] left = new int[BUFFER];
        for (int slot = 0; slot < BUFFER; slot++) {
            left[slot] = buffer.getStackInSlot(slot).getCount();
        }
        int[] plan = new int[GRID];
        java.util.Arrays.fill(plan, -1);
        boolean ok = true;
        for (int c = 0; c < GRID; c++) {
            ItemStack ghost = patterns[pattern][c];
            if (ghost.isEmpty()) {
                continue;
            }
            int pick = -1;
            for (int pass = 0; pass < 2 && pick < 0; pass++) {
                for (int slot = 0; slot < BUFFER && pick < 0; slot++) {
                    ItemStack stack = buffer.getStackInSlot(slot);
                    if (stack.isEmpty() || left[slot] < crafts) {
                        continue;
                    }
                    boolean same = stack.is(ghost.getItem());
                    if (pass == 0 ? same : !same && (cellsFor(pattern, stack.getItem()) & (1 << c)) != 0) {
                        pick = slot;
                    }
                }
            }
            if (pick < 0) {
                ok = false;
                lacking[0] |= 1 << c;
                continue;
            }
            left[pick] -= crafts;
            plan[c] = pick;
        }
        return ok ? plan : null;
    }

    // ---------------------------------------------------------------- ports

    /** What pipes see on a port face of a formed Assembler. */
    public static @Nullable IItemHandler itemsAt(Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        AssemblerLayout.Port port = portAt(state, side);
        AssemblerBlockEntity controller = port == null ? null : AssemblerPartBlock.controller(level, pos, state);
        if (controller == null) {
            return null;
        }
        return switch (port) {
            case ITEMS_IN -> controller.itemsIn;
            case ITEMS_OUT -> controller.itemsOut;
            default -> null;
        };
    }

    public static @Nullable IEnergyStorage energyAt(Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        AssemblerBlockEntity controller = portAt(state, side) == AssemblerLayout.Port.ENERGY
                ? AssemblerPartBlock.controller(level, pos, state) : null;
        return controller == null ? null : controller.energy;
    }

    private static @Nullable AssemblerLayout.Port portAt(BlockState state, @Nullable Direction side) {
        if (side == null || !(state.getBlock() instanceof AssemblerPartBlock) || !AssemblerPartBlock.isFormed(state)) {
            return null;
        }
        BlockPos cell = AssemblerLayout.cell(state.getValue(AssemblerPartBlock.CELL));
        for (AssemblerLayout.Port port : AssemblerLayout.Port.values()) {
            if (port.cell().equals(cell) && port.face(state.getValue(AssemblerPartBlock.FACING)) == side) {
                return port;
            }
        }
        return null;
    }

    /** The parts store, for pipes, while the cell is still to be built. */
    public @Nullable IItemHandler partsFor(@Nullable Direction side) {
        return isFormed() ? null : parts;
    }

    /** Ingredients in: into the buffer only, nothing out. */
    private final IItemHandler itemsIn = new IItemHandler() {
        @Override
        public int getSlots() {
            return BUFFER;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return buffer.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return buffer.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return buffer.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return buffer.isItemValid(slot, stack);
        }
    };

    /** Results out: from the output only, nothing in. */
    private final IItemHandler itemsOut = new IItemHandler() {
        @Override
        public int getSlots() {
            return OUTPUTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return output.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return output.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return output.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    // ---------------------------------------------------------------- ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, AssemblerBlockEntity cell) {
        ServerLevel server = (ServerLevel) level;
        if (!AssemblerPartBlock.isFormed(state)) {
            if (level.getGameTime() % STEP_TICKS == 0) {
                cell.buildStep(server);
            }
            if (level.getGameTime() % 10 == 0) {
                cell.survey(level);
            }
            return;
        }
        cell.work(server);
        if (level.getGameTime() % 10 == 0) {
            cell.pushOutput(server);
        }
        boolean working = level.getGameTime() - cell.lastWork < 40;
        if (level.getGameTime() % 10 == 0 && state.getValue(AssemblerPartBlock.WORKING) != working) {
            AssemblerStructure.setWorking(server, pos, cell.facing(), working);
        }
        if (cell.syncNeeded) {
            cell.syncNeeded = false;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, AssemblerBlockEntity cell) {
        AssemblerGhostPreview.track(cell);
        if (cell.active >= 0 && cell.droppedId != cell.cycleId && cell.cycleTime(0) >= cell.total) {
            cell.drop(cell.syncedResult);
        }
        cell.drops.removeIf(d -> level.getGameTime() - d.at() > 200);
        // Sparks off the gripper as it sets a part down, now and then.
        if (cell.active >= 0 && cell.syncedRunning && level.random.nextInt(30) == 0) {
            var at = AssemblerLayout.designToWorld(pos, cell.facing(), AssemblerLayout.FIX_X[1], AssemblerLayout.BED_TOP + 1,
                    AssemblerLayout.FIX_Z[1]);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 0, 0.02, 0);
        }
    }

    /** One tick of the formed cell. */
    private void work(ServerLevel level) {
        if (!made.isEmpty() && active < 0) {
            deliver();
        }
        Status was = status;
        if (!enabled) {
            status = Status.OFF;
        } else if (!redstoneMode.allows(level.hasNeighborSignal(worldPosition))) {
            status = Status.REDSTONE;
        } else if (active >= 0) {
            if (energy.consume(draw())) {
                lastWork = level.getGameTime();
                if (status != Status.WORKING) {
                    syncNeeded = true;
                }
                status = Status.WORKING;
                step(level);
            } else {
                if (status != Status.NO_POWER) {
                    syncNeeded = true;
                }
                status = Status.NO_POWER;
            }
        } else if (!made.isEmpty()) {
            status = Status.OUTPUT_FULL;
        } else if (level.getGameTime() % LOOK_TICKS == 0) {
            look(level);
        }
        if (status != was) {
            syncNeeded = true;
        }
    }

    /** Advances the cycle a tick: the grid becomes the result after the last trip in, and it is done after the trip out. */
    private void step(ServerLevel level) {
        progress++;
        int craftAt = total - TRIP_TICKS;
        if (progress == craftAt) {
            craft(level);
        }
        if (progress % TRIP_TICKS == TRIP_TICKS - 1 && !sound.muted()) {
            level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.15F, 1.6F + level.random.nextFloat() * 0.2F);
        }
        if (progress >= total) {
            active = -1;
            progress = 0;
            total = 0;
            deliver();
            syncNeeded = true;
        }
    }

    /** The grid becomes the result: the recipe checked again on what was laid out, as the crafting table would. */
    private void craft(ServerLevel level) {
        List<ItemStack> items = new ArrayList<>(GRID);
        for (ItemStack stack : bed) {
            items.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        }
        CraftingInput input = CraftingInput.of(3, 3, items);
        RecipeHolder<CraftingRecipe> recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
        if (recipe == null) {
            // The recipe went (a datapack reload): the parts go back where they came from.
            abandonCycle();
            return;
        }
        ItemStack one = recipe.value().assemble(input, level.registryAccess());
        made.clear();
        addSplit(made, one, one.getCount() * batch);
        for (ItemStack rest : recipe.value().getRemainingItems(input)) {
            if (!rest.isEmpty()) {
                addSplit(made, rest, rest.getCount() * batch);
            }
        }
        java.util.Arrays.fill(bed, ItemStack.EMPTY);
        syncNeeded = true;
        if (!sound.muted()) {
            level.playSound(null, worldPosition, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.35F, 1.4F);
        }
    }

    private static void addSplit(List<ItemStack> into, ItemStack kind, int count) {
        while (count > 0) {
            int n = Math.min(count, kind.getMaxStackSize());
            into.add(kind.copyWithCount(n));
            count -= n;
        }
    }

    /** Moves what the last cycle made into the output, as far as it fits. */
    private void deliver() {
        made.replaceAll(stack -> ItemHandlerHelper.insertItemStacked(output, stack, false));
        made.removeIf(ItemStack::isEmpty);
    }

    /** An idle cell looks for its next craft, the patterns taking turns. */
    private void look(ServerLevel level) {
        int speed = speed();
        boolean anyPattern = false;
        boolean fullOutput = false;
        int firstShort = -1;
        int shortMask = 0;
        for (int k = 0; k < PATTERNS; k++) {
            int p = (nextPattern + k) % PATTERNS;
            RecipeHolder<CraftingRecipe> recipe = recipe(p);
            if (recipe == null) {
                continue;
            }
            anyPattern = true;
            int can = speed;
            int[] plan = null;
            int[] lacking = new int[1];
            for (; can > 0 && plan == null; can--) {
                lacking[0] = 0;
                plan = plan(p, can, lacking);
            }
            can++;
            if (plan == null) {
                if (firstShort < 0) {
                    firstShort = p;
                    shortMask = lacking[0];
                }
                continue;
            }
            if (!fits(p, recipe, can)) {
                fullOutput = true;
                continue;
            }
            if (energy.getEnergyStored() < draw()) {
                // Nothing leaves the buffer until there is power to work it.
                status = Status.NO_POWER;
                return;
            }
            start(p, can, plan);
            nextPattern = (p + 1) % PATTERNS;
            waiting = -1;
            missingMask = 0;
            return;
        }
        status = !anyPattern ? Status.NO_PATTERN : fullOutput ? Status.OUTPUT_FULL : Status.STARVED;
        int newWaiting = status == Status.STARVED ? firstShort : -1;
        int newMask = status == Status.STARVED ? shortMask : 0;
        if (newWaiting != waiting || newMask != missingMask) {
            waiting = newWaiting;
            missingMask = newMask;
            syncNeeded = true;
        }
    }

    /** Whether {@code crafts} of a pattern, and what they leave (empty buckets), fit in the output. */
    private boolean fits(int pattern, RecipeHolder<CraftingRecipe> recipe, int crafts) {
        CraftingInput input = input(pattern);
        List<ItemStack> out = new ArrayList<>();
        ItemStack one = recipe.value().assemble(input, level.registryAccess());
        addSplit(out, one, one.getCount() * crafts);
        for (ItemStack rest : recipe.value().getRemainingItems(input)) {
            if (!rest.isEmpty()) {
                addSplit(out, rest, rest.getCount() * crafts);
            }
        }
        NonNullList<ItemStack> copy = NonNullList.withSize(OUTPUTS, ItemStack.EMPTY);
        for (int slot = 0; slot < OUTPUTS; slot++) {
            copy.set(slot, output.getStackInSlot(slot).copy());
        }
        ItemStackHandler trial = new ItemStackHandler(copy);
        for (ItemStack stack : out) {
            if (!ItemHandlerHelper.insertItemStacked(trial, stack, false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Starts a cycle: the ingredients for {@code crafts} leave the buffer for the bed. */
    private void start(int pattern, int crafts, int[] plan) {
        for (int c = 0; c < GRID; c++) {
            bed[c] = plan[c] < 0 ? ItemStack.EMPTY : buffer.extractItem(plan[c], crafts, false);
        }
        active = pattern;
        batch = crafts;
        progress = 0;
        cycleId++;
        int trips = 0;
        for (ItemStack stack : bed) {
            trips += stack.isEmpty() ? 0 : 1;
        }
        total = (trips + 1) * TRIP_TICKS;
        status = Status.WORKING;
        syncNeeded = true;
        setChanged();
    }

    /** Ends a cycle early (the cell broken, or its recipe gone): the parts on the bed go back. */
    public void abandonCycle() {
        for (int c = 0; c < GRID; c++) {
            if (!bed[c].isEmpty()) {
                ItemStack left = ItemHandlerHelper.insertItemStacked(buffer, bed[c], false);
                if (!left.isEmpty()) {
                    made.add(left);
                }
                bed[c] = ItemStack.EMPTY;
            }
        }
        active = -1;
        progress = 0;
        total = 0;
        syncNeeded = true;
        setChanged();
    }

    /** Pushes results out of the output port into whatever is beside it. */
    private void pushOutput(ServerLevel level) {
        Direction face = AssemblerLayout.Port.ITEMS_OUT.face(facing());
        BlockPos target = AssemblerLayout.toWorld(worldPosition, facing(), AssemblerLayout.Port.ITEMS_OUT.cell()).relative(face);
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, face.getOpposite());
        if (handler == null) {
            return;
        }
        for (int slot = 0; slot < OUTPUTS; slot++) {
            ItemStack stack = output.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                output.setStackInSlot(slot, ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false));
            }
        }
    }

    // ---------------------------------------------------------------- controls

    public void togglePower() {
        enabled = !enabled;
        syncNeeded = true;
        setChanged();
    }

    public void cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        setChanged();
    }

    // ---------------------------------------------------------------- building

    private void buildStep(ServerLevel level) {
        int placed = 0;
        for (BlockPos cell : ORDER) {
            if (placed >= PER_STEP) {
                return;
            }
            AssemblerPart part = AssemblerLayout.partAt(cell);
            if (part == AssemblerPart.CONTROLLER) {
                continue;
            }
            BlockPos pos = cellPos(cell);
            BlockState there = level.getBlockState(pos);
            if (there.is(part.block()) || !there.canBeReplaced() || !takePart(part)) {
                continue;
            }
            level.setBlock(pos, part.block().defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.5F, 0.9F + level.random.nextFloat() * 0.2F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
            placed++;
            if (isRemoved() || isFormed()) {
                return;
            }
        }
    }

    private boolean takePart(AssemblerPart part) {
        for (int slot = 0; slot < parts.getSlots(); slot++) {
            if (partFor(parts.getStackInSlot(slot)) == part) {
                parts.extractItem(slot, 1, false);
                return true;
            }
        }
        return false;
    }

    private void survey(Level level) {
        missing[0] = missing[1] = 0;
        blocked = 0;
        for (BlockPos cell : ORDER) {
            AssemblerPart part = AssemblerLayout.partAt(cell);
            if (part == AssemblerPart.CONTROLLER) {
                continue;
            }
            BlockState there = level.getBlockState(cellPos(cell));
            if (there.is(part.block())) {
                continue;
            }
            missing[part == AssemblerPart.FRAME ? 0 : 1]++;
            if (!there.canBeReplaced()) {
                blocked++;
            }
        }
    }

    // ---------------------------------------------------------------- contents

    public void dropParts(Level level, BlockPos pos) {
        drop(level, pos, parts);
    }

    /** Everything the controller holds, dropped when it is broken. */
    public void dropAll(Level level, BlockPos pos) {
        drop(level, pos, parts);
        drop(level, pos, tools);
        drop(level, pos, buffer);
        drop(level, pos, output);
        for (int c = 0; c < GRID; c++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), bed[c]);
            bed[c] = ItemStack.EMPTY;
        }
        for (ItemStack stack : made) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        made.clear();
    }

    private static void drop(Level level, BlockPos pos, ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(slot));
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    // ---------------------------------------------------------------- what the renderer reads (client)

    /** The pattern being made, or -1. */
    public int active() {
        return active;
    }

    public int batch() {
        return batch;
    }

    /** Ticks into the cycle now, from the last update, standing still while it is short of power. */
    public float cycleTime(float partialTick) {
        if (level == null || active < 0) {
            return 0;
        }
        float t = syncedProgress + (syncedRunning ? level.getGameTime() - syncedAt + partialTick : 0);
        return Math.min(t, total);
    }

    public int total() {
        return total;
    }

    /** What the grid became, once crafted (client). */
    public ItemStack syncedResult() {
        return syncedResult;
    }

    public Status status() {
        return status;
    }

    public int waiting() {
        return waiting;
    }

    public int missingMask() {
        return missingMask;
    }

    /** What lies in a cell of the bed this cycle (client): the real item, or the pattern's own. */
    public ItemStack laid(int cell) {
        return shown[cell].isEmpty() && active >= 0 ? ghost(active, cell) : shown[cell];
    }

    /** Results riding the out-feed (client). */
    public List<Drop> drops() {
        return drops;
    }

    /** Client: the arm has set this cycle's result down on the out-feed. */
    private void drop(ItemStack result) {
        droppedId = cycleId;
        if (!result.isEmpty() && level != null) {
            drops.add(new Drop(result.copyWithCount(1), level.getGameTime()));
        }
    }

    // ---------------------------------------------------------------- saving and syncing

    private ListTag savePatterns(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int p = 0; p < PATTERNS; p++) {
            ListTag grid = new ListTag();
            for (int c = 0; c < GRID; c++) {
                grid.add(patterns[p][c].isEmpty() ? new CompoundTag() : (CompoundTag) patterns[p][c].save(registries));
            }
            list.add(grid);
        }
        return list;
    }

    private void loadPatterns(ListTag list, HolderLookup.Provider registries) {
        for (int p = 0; p < PATTERNS; p++) {
            ListTag grid = p < list.size() ? list.getList(p) : new ListTag();
            for (int c = 0; c < GRID; c++) {
                CompoundTag tag = c < grid.size() ? grid.getCompound(c) : new CompoundTag();
                patterns[p][c] = tag.isEmpty() ? ItemStack.EMPTY : ItemStack.parseOptional(registries, tag);
            }
            recipes[p] = null;
            looked[p] = false;
            results[p] = null;
            fitting[p] = null;
        }
    }

    private static ListTag saveStacks(List<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                list.add(stack.save(registries));
            }
        }
        return list;
    }

    private static void loadStacks(ListTag list, List<ItemStack> into, HolderLookup.Provider registries) {
        into.clear();
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!stack.isEmpty()) {
                into.add(stack);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        sound.save(tag);
        tag.put("parts", parts.serializeNBT(registries));
        tag.put("tools", tools.serializeNBT(registries));
        tag.put("buffer", buffer.serializeNBT(registries));
        tag.put("output", output.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        tag.put("patterns", savePatterns(registries));
        tag.putBoolean("enabled", enabled);
        tag.putInt("redstone", redstoneMode.ordinal());
        tag.putInt("next", nextPattern);
        tag.putInt("active", active);
        tag.putInt("batch", batch);
        tag.putInt("progress", progress);
        tag.putInt("total", total);
        CompoundTag bedTag = new CompoundTag();
        for (int c = 0; c < GRID; c++) {
            if (!bed[c].isEmpty()) {
                bedTag.put(String.valueOf(c), bed[c].save(registries));
            }
        }
        tag.put("bed", bedTag);
        tag.put("made", saveStacks(made, registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sound.load(tag);
        parts.deserializeNBT(registries, tag.getCompound("parts"));
        tools.deserializeNBT(registries, tag.getCompound("tools"));
        buffer.deserializeNBT(registries, tag.getCompound("buffer"));
        output.deserializeNBT(registries, tag.getCompound("output"));
        energy.setStored(tag.getInt("energy"));
        loadPatterns(tag.getList("patterns", Tag.TAG_LIST), registries);
        enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        redstoneMode = RedstoneMode.byId(tag.getInt("redstone"));
        nextPattern = tag.getInt("next");
        active = tag.contains("active") ? tag.getInt("active") : -1;
        batch = tag.getInt("batch");
        progress = tag.getInt("progress");
        total = tag.getInt("total");
        CompoundTag bedTag = tag.getCompound("bed");
        for (int c = 0; c < GRID; c++) {
            String key = String.valueOf(c);
            bed[c] = bedTag.contains(key) ? ItemStack.parseOptional(registries, bedTag.getCompound(key)) : ItemStack.EMPTY;
        }
        loadStacks(tag.getList("made", Tag.TAG_COMPOUND), made, registries);
    }

    /** Clients get the patterns (for the screen and the rack), the cycle (for the arm) and the belts. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        sound.save(tag);
        tag.put("patterns", savePatterns(registries));
        tag.putInt("status", status.ordinal());
        tag.putInt("active", active);
        tag.putInt("batch", batch);
        tag.putInt("progress", progress);
        tag.putInt("total", total);
        tag.putBoolean("running", status == Status.WORKING);
        tag.putInt("waiting", waiting);
        tag.putInt("missing", missingMask);
        if (!made.isEmpty()) {
            tag.put("result", made.get(0).save(registries));
        }
        // What is really on the bed (another mod's lead, say), for the arm to carry.
        ListTag onBed = new ListTag();
        for (ItemStack stack : bed) {
            onBed.add(stack.isEmpty() ? new CompoundTag() : (CompoundTag) stack.copyWithCount(1).save(registries));
        }
        tag.put("bed", onBed);
        tag.putInt("cycle", cycleId);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        sound.load(tag);
        loadPatterns(tag.getList("patterns", Tag.TAG_LIST), registries);
        // A cycle that ended before the client saw it reach its end still sets its result down.
        int newCycle = tag.getInt("cycle");
        int newActive = tag.getInt("active");
        if (active >= 0 && droppedId != cycleId && (newActive < 0 || newCycle != cycleId)) {
            drop(syncedResult);
        }
        cycleId = newCycle;
        status = Status.byId(tag.getInt("status"));
        active = newActive;
        batch = tag.getInt("batch");
        total = tag.getInt("total");
        syncedProgress = tag.getInt("progress");
        syncedRunning = tag.getBoolean("running");
        syncedAt = level == null ? 0 : level.getGameTime();
        waiting = tag.getInt("waiting");
        missingMask = tag.getInt("missing");
        syncedResult = tag.contains("result") ? ItemStack.parseOptional(registries, tag.getCompound("result")) : ItemStack.EMPTY;
        ListTag onBed = tag.getList("bed", Tag.TAG_COMPOUND);
        for (int c = 0; c < GRID; c++) {
            CompoundTag item = c < onBed.size() ? onBed.getCompound(c) : new CompoundTag();
            // Once crafted the server's bed is empty; keep what was laid out until the next cycle.
            if (!item.isEmpty() || newCycle != shownCycle) {
                shown[c] = item.isEmpty() ? ItemStack.EMPTY : ItemStack.parseOptional(registries, item);
            }
        }
        shownCycle = newCycle;
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.ryzergen.assembler");
    }

    /** Unbuilt, the screen is the parts store; formed, it is the cell's patterns and stores. */
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        ContainerLevelAccess access = ContainerLevelAccess.create(level, worldPosition);
        return isFormed() ? new AssemblerMenu(containerId, inventory, worldPosition, tools, buffer, output, data, access)
                : new AssemblerBuildMenu(containerId, inventory, parts, data, access);
    }
}
