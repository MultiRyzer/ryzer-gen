package com.ryzer.ryzergen.machine.drill;

import com.mojang.authlib.GameProfile;
import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.client.MegaDrillGhostPreview;
import com.ryzer.ryzergen.machine.MachineEnergyStorage;
import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.machine.SpeedModuleItem;
import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import com.ryzer.ryzergen.material.ModTags;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The mega drill's controller (design section 11c). Before the drill forms it builds it: feed its
 * store drill frame (by hand or by pipe) and it places the frame itself, bottom layer first, until
 * the last one forms the drill. Once formed it runs the drill, one layer at a time from the base's
 * level down to bedrock, through the opening in the middle of its base (the 7 x 7 under it):
 *
 * <ul>
 *   <li>Charge: power runs down the drive shaft (FE every tick).</li>
 *   <li>Fire: the beam heats the layer until it bursts (FE and water every tick).</li>
 *   <li>Burst: every block the beam can break goes at once. Ores, and what the filter keeps, go to
 *       the output; the rest is melted away. Fluids in the pit's wall are glazed to obsidian, so
 *       nothing flows in. The tip wears.</li>
 * </ul>
 *
 * Once the shaft reaches the bottom it bores on into the deep crust (DeepBore): no more blocks break,
 * and each cycle brings up ore from a table set by the head, at several times the power.
 *
 * The four footings under the legs are never mined, so the drill ends up standing on four pillars
 * of the ground's own layers. On the base's level only loose things go (plants, snow, fluids), so
 * the pipes and cables beside its ports are safe. A drill head sets the speed, speed modules add to
 * it, and the draw grows with the square of the speed. Clients get only the phase, when it began
 * and how long it lasts (MegaDrillRenderer draws the rest).
 */
public class MegaDrillBlockEntity extends BlockEntity implements MenuProvider, MegaDrillView {
    public static final int PART_SLOTS = 9;
    public static final int SLOT_HEAD = 0;
    public static final int SLOT_TIP = 1;
    public static final int SLOT_MODULES = 2;
    public static final int TOOL_SLOTS = 3;
    public static final int OUTPUTS = 12;
    public static final int FILTERS = 6;
    public static final int WATER_CAPACITY = 16_000;
    public static final int ENERGY_CAPACITY = 200_000;
    private static final int MAX_RECEIVE = 50_000;
    /** Ticks to charge, and to heat a layer until it bursts, at speed 1; the speed divides both. */
    public static final int CHARGE_TICKS = 100;
    public static final int HEAT_TICKS = 80;
    /** Blocks placed per build step, and ticks between steps. */
    private static final int PER_STEP = 3;
    private static final int STEP_TICKS = 2;
    /** Empty layers passed in one tick, at most, looking for the next with something to burst. */
    private static final int SCAN_LAYERS = 8;
    /** A hold this long is sent to clients, so their picture of the cycle stops too. */
    private static final int STALL_SYNC = 10;
    /** Ticks the drill stays lit after it stops, so a drill short of power does not flicker. */
    private static final int LIT_DELAY = 40;
    private static final int UNSET = Integer.MIN_VALUE;

    public static final int DATA_MISSING = 0;
    public static final int DATA_BLOCKED = 1;
    public static final int DATA_FORMED = 2;
    public static final int DATA_ENERGY_LOW = 3;
    public static final int DATA_ENERGY_HIGH = 4;
    public static final int DATA_WATER = 5;
    public static final int DATA_STATUS = 6;
    public static final int DATA_REDSTONE = 7;
    public static final int DATA_ENABLED = 8;
    public static final int DATA_LAYER = 9;
    public static final int DATA_SPEED = 10;
    public static final int DATA_DRAW_LOW = 11;
    public static final int DATA_DRAW_HIGH = 12;
    public static final int DATA_DEEP = 13;
    public static final int DATA_MUTED = 14;
    public static final int DATA_COUNT = 15;

    /** What the drill is doing, for its screen. */
    public enum Status {
        CHARGING, FIRING, SCANNING, NO_HEAD, NO_TIP, NO_POWER, NO_WATER, OUTPUT_FULL, REDSTONE, OFF, UNLOADED, FINISHED, BORING;

        public boolean running() {
            return this == CHARGING || this == FIRING || this == SCANNING || this == BORING;
        }

        public static Status byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : OFF;
        }
    }

    /** Build order: the design's cells bottom layer first, each layer front to back. */
    private static final List<BlockPos> ORDER = new ArrayList<>();

    static {
        for (MegaDrillLayout.Cell cell : MegaDrillLayout.CELL_LIST) {
            if (!cell.pos().equals(MegaDrillLayout.CONTROLLER)) {
                ORDER.add(cell.pos());
            }
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
            return isPart(stack);
        }
    };

    private final ItemStackHandler tools = new ItemStackHandler(TOOL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_MODULES ? ProcessingMachine.MAX_MODULES : 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return toolFits(slot, stack);
        }
    };

    private final ItemStackHandler output = new ItemStackHandler(OUTPUTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemStackHandler filter = new ItemStackHandler(FILTERS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final MachineEnergyStorage energy = new MachineEnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE, this::setChanged);

    private final FluidTank water = new FluidTank(WATER_CAPACITY, fluid -> fluid.is(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private DrillPhase phase = DrillPhase.IDLE;
    private int phaseLength = 1;
    /** Ticks of the phase done (the server's count; holds stop it). */
    private int phaseDone;
    /** When the phase would have begun had it never been held; clients draw the cycle from it. */
    private long phaseStart;
    /** Held for a while mid-phase: clients stop their picture where it is. */
    private boolean stalled;
    private int stallTicks;
    private int layer = UNSET;
    private int speed = 1;
    private boolean finished;
    /** Past bedrock: boring the deep crust for ore (DeepBore), the layer staying on the bottom one. */
    private boolean deep;
    private Status status = Status.OFF;
    /** What the last burst freed that did not fit in the output yet. */
    private final List<ItemStack> pending = new ArrayList<>();
    private RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    private boolean enabled = true;
    /** Silent: its screen's sound switch, for players who work beside it. */
    private boolean muted;
    private @Nullable GameProfile owner;
    private int litTimer;
    private int missing;
    private int blocked;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_MISSING -> missing;
                case DATA_BLOCKED -> blocked;
                case DATA_FORMED -> isFormed() ? 1 : 0;
                case DATA_ENERGY_LOW -> energy.getEnergyStored() & 0xFFFF;
                case DATA_ENERGY_HIGH -> energy.getEnergyStored() >>> 16;
                case DATA_WATER -> water.getFluidAmount();
                case DATA_STATUS -> status.ordinal();
                case DATA_REDSTONE -> redstoneMode.ordinal();
                case DATA_ENABLED -> enabled ? 1 : 0;
                case DATA_LAYER -> layer == UNSET ? centre().getY() : layer;
                case DATA_SPEED -> speed();
                case DATA_DRAW_LOW -> drawFor(speed()) & 0xFFFF;
                case DATA_DRAW_HIGH -> drawFor(speed()) >>> 16;
                case DATA_DEEP -> deep ? 1 : 0;
                case DATA_MUTED -> muted ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Read-only on the server.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MegaDrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEGA_DRILL.get(), pos, state);
    }

    public static boolean isPart(ItemStack stack) {
        return stack.is(ModBlocks.DRILL_FRAME.get().asItem());
    }

    /** What each tool slot takes: a drill head, a tungsten tip, speed modules. */
    public static boolean toolFits(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_HEAD -> stack.getItem() instanceof DrillHeadItem;
            case SLOT_TIP -> stack.getItem() instanceof TungstenTipItem;
            case SLOT_MODULES -> stack.getItem() instanceof SpeedModuleItem;
            default -> false;
        };
    }

    public boolean isFormed() {
        return MegaDrillPartBlock.isFormed(getBlockState());
    }

    public Direction facing() {
        return getBlockState().getValue(MegaDrillPartBlock.FACING);
    }

    /** The middle of the pit, on the base's level. */
    public BlockPos centre() {
        return MegaDrillLayout.toWorld(worldPosition, facing(), MegaDrillLayout.CENTRE);
    }

    /** What the drill is doing. */
    public Status status() {
        return status;
    }

    /** The head, tip and speed module slots (the screen's, and the game tests'). */
    public IItemHandler tools() {
        return tools;
    }

    void setOwner(GameProfile profile) {
        owner = profile;
        setChanged();
    }

    // ---------------------------------------------------------------- speed and cost

    /** The head's speed plus one per speed module; 0 without a head. */
    public int speed() {
        int head = DrillHeadItem.speed(tools.getStackInSlot(SLOT_HEAD));
        return head == 0 ? 0 : head + Math.min(ProcessingMachine.MAX_MODULES, tools.getStackInSlot(SLOT_MODULES).getCount());
    }

    /** FE per tick while charging or firing: the base draw times the speed squared. */
    public static int draw(int speed) {
        return (int) Math.min(Integer.MAX_VALUE, (long) Config.get(Config.DRILL_DRAW) * speed * speed);
    }

    /** The draw at a speed, now: the deep bore uses several times the shaft's. */
    private int drawFor(int speed) {
        int draw = draw(speed);
        return deep ? (int) Math.min(Integer.MAX_VALUE, (long) draw * Config.get(Config.DRILL_DEEP_DRAW) / 100) : draw;
    }

    private int chargeTicks() {
        return Math.max(1, CHARGE_TICKS * Config.get(Config.DRILL_TIME_PERCENT) / 100 / speed);
    }

    private int heatTicks() {
        return Math.max(1, HEAT_TICKS * Config.get(Config.DRILL_TIME_PERCENT) / 100 / speed);
    }

    /** Water per tick while firing: a layer's worth spread over the heating. */
    private int waterPerTick() {
        return Mth.ceil(Config.get(Config.DRILL_WATER) / (float) phaseLength);
    }

    // ---------------------------------------------------------------- ports

    public IFluidHandler waterPort() {
        return waterIn;
    }

    /** What pipes see on a port face of a formed drill. */
    public static @Nullable IItemHandler itemsAt(Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        MegaDrillLayout.Port port = portAt(state, side);
        if (port != MegaDrillLayout.Port.TIPS && port != MegaDrillLayout.Port.OUTPUT) {
            return null;
        }
        MegaDrillBlockEntity controller = MegaDrillPartBlock.controller(level, pos, state);
        return controller == null ? null : port == MegaDrillLayout.Port.TIPS ? controller.tipsIn : controller.minedOut;
    }

    public static @Nullable IFluidHandler fluidAt(Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        MegaDrillBlockEntity controller = portAt(state, side) == MegaDrillLayout.Port.WATER
                ? MegaDrillPartBlock.controller(level, pos, state) : null;
        return controller == null ? null : controller.waterIn;
    }

    public static @Nullable IEnergyStorage energyAt(Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        MegaDrillBlockEntity controller = portAt(state, side) == MegaDrillLayout.Port.ENERGY
                ? MegaDrillPartBlock.controller(level, pos, state) : null;
        return controller == null ? null : controller.energy;
    }

    /** The port on {@code side} of this block, if it is one on a formed drill. */
    private static @Nullable MegaDrillLayout.Port portAt(BlockState state, @Nullable Direction side) {
        if (side == null || !(state.getBlock() instanceof MegaDrillPartBlock) || !MegaDrillPartBlock.isFormed(state)) {
            return null;
        }
        int cell = state.getValue(MegaDrillPartBlock.CELL);
        for (MegaDrillLayout.Port port : MegaDrillLayout.Port.values()) {
            if (MegaDrillLayout.index(port) == cell && port.face(state.getValue(MegaDrillPartBlock.FACING)) == side) {
                return port;
            }
        }
        return null;
    }

    /** The parts store, for pipes, while the drill is still to be built. */
    public @Nullable IItemHandler partsFor(@Nullable Direction side) {
        return isFormed() ? null : parts;
    }

    /** Fresh tips in: into the tip slot only, nothing out. */
    private final IItemHandler tipsIn = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return tools.getStackInSlot(SLOT_TIP);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return tools.insertItem(SLOT_TIP, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return toolFits(SLOT_TIP, stack);
        }
    };

    /** What the drill mined, out: from the output slots only, nothing in. */
    private final IItemHandler minedOut = new IItemHandler() {
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

    /** Water in: fills the tank, never drained from outside. */
    private final IFluidHandler waterIn = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return water.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return WATER_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return water.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return water.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    // ---------------------------------------------------------------- ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, MegaDrillBlockEntity drill) {
        ServerLevel server = (ServerLevel) level;
        if (MegaDrillPartBlock.isFormed(state)) {
            drill.run(server);
            return;
        }
        if (level.getGameTime() % STEP_TICKS == 0) {
            drill.buildStep(server);
        }
        if (level.getGameTime() % 10 == 0) {
            drill.survey(level);
        }
    }

    /** One tick of the running drill. */
    private void run(ServerLevel level) {
        BlockPos centre = centre();
        if (layer == UNSET) {
            layer = centre.getY();
        }
        // A drill that stopped at bedrock bores on once the deep bore is turned on.
        if (finished && Config.get(Config.DRILL_DEEP_BORE)) {
            finished = false;
            deep = true;
            layer = level.getMinBuildHeight();
        }
        if (level.getGameTime() % 10 == 0) {
            flushPending();
            pushOutput(level);
        }
        Status hold = blocker(level, centre);
        if (hold != null) {
            hold(level, hold);
        } else {
            switch (phase) {
                case IDLE, DONE -> startLayer(level, centre);
                case CHARGING -> {
                    if (!energy.consume(drawFor(speed))) {
                        hold(level, Status.NO_POWER);
                    } else if (advance(level, deep ? Status.BORING : Status.CHARGING)) {
                        enter(level, DrillPhase.FIRING, heatTicks());
                        fireSound(level, centre, speed);
                    }
                }
                case FIRING -> {
                    int cool = waterPerTick();
                    if (energy.getEnergyStored() < drawFor(speed)) {
                        hold(level, Status.NO_POWER);
                    } else if (water.getFluidAmount() < cool) {
                        hold(level, Status.NO_WATER);
                    } else {
                        energy.consume(drawFor(speed));
                        water.drain(cool, IFluidHandler.FluidAction.EXECUTE);
                        if (advance(level, deep ? Status.BORING : Status.FIRING)) {
                            if (deep) {
                                bore(level, centre);
                            } else {
                                burst(level, centre);
                                layer--;
                            }
                            wearTip(level);
                            phase = DrillPhase.IDLE;
                            startLayer(level, centre);
                        }
                    }
                }
            }
        }
        light(level);
    }

    /** What stops the drill before it does anything this tick, if anything. */
    private @Nullable Status blocker(ServerLevel level, BlockPos centre) {
        if (!enabled) {
            return Status.OFF;
        }
        if (!redstoneMode.allows(level.hasNeighborSignal(worldPosition))) {
            return Status.REDSTONE;
        }
        if (finished) {
            return Status.FINISHED;
        }
        if (tools.getStackInSlot(SLOT_HEAD).isEmpty()) {
            return Status.NO_HEAD;
        }
        if (tools.getStackInSlot(SLOT_TIP).isEmpty()) {
            return Status.NO_TIP;
        }
        // Every chunk under the pit must be loaded: reading an unloaded one would load it.
        if (!level.hasChunksAt(centre.getX() + MegaDrillLayout.LOW - 1, centre.getZ() + MegaDrillLayout.LOW - 1,
                centre.getX() + MegaDrillLayout.HIGH + 1, centre.getZ() + MegaDrillLayout.HIGH + 1)) {
            return Status.UNLOADED;
        }
        return null;
    }

    /** Finds the next layer with something to burst and starts charging for it. */
    private void startLayer(ServerLevel level, BlockPos centre) {
        if (!pending.isEmpty()) {
            hold(level, Status.OUTPUT_FULL);
            return;
        }
        if (deep) {
            speed = Math.max(1, speed());
            enter(level, DrillPhase.CHARGING, chargeTicks());
            status = Status.BORING;
            sound(level, centre, ModSounds.MEGA_DRILL_CHARGE.get(), 1.5F, 0.8F);
            return;
        }
        for (int i = 0; i < SCAN_LAYERS; i++) {
            if (layer < level.getMinBuildHeight()) {
                if (Config.get(Config.DRILL_DEEP_BORE)) {
                    // The shaft is down: the beam bores on from the bottom layer into the deep crust.
                    deep = true;
                    layer = level.getMinBuildHeight();
                    startLayer(level, centre);
                    return;
                }
                finished = true;
                status = Status.FINISHED;
                enter(level, DrillPhase.DONE, 1);
                return;
            }
            if (hasWork(level, centre, layer)) {
                speed = Math.max(1, speed());
                enter(level, DrillPhase.CHARGING, chargeTicks());
                status = Status.CHARGING;
                sound(level, centre, ModSounds.MEGA_DRILL_CHARGE.get(), 1.5F, 1.0F);
                return;
            }
            layer--;
        }
        status = Status.SCANNING;
        setChanged();
    }

    private boolean hasWork(Level level, BlockPos centre, int y) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = MegaDrillLayout.LOW; dx <= MegaDrillLayout.HIGH; dx++) {
            for (int dz = MegaDrillLayout.LOW; dz <= MegaDrillLayout.HIGH; dz++) {
                pos.set(centre.getX() + dx, y, centre.getZ() + dz);
                if (bursts(level, pos, dx, dz, centre.getY())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether the beam bursts the block at {@code pos} ({@code dx}, {@code dz} from the pit's middle):
     * anything breakable, except the footings and the drill itself. On the base's own level, only
     * loose things (plants, snow, fluids), so what stands beside the drill (its pipes and cables)
     * stays. Both sides use this: clients draw the heat over the blocks that will burst.
     */
    public static boolean bursts(BlockGetter level, BlockPos pos, int dx, int dz, int base) {
        if (MegaDrillLayout.footing(dx, dz) || pos.getY() > base) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof MegaDrillPartBlock) {
            return false;
        }
        if (pos.getY() == base) {
            return state.canBeReplaced();
        }
        return state.getDestroySpeed(level, pos) >= 0;
    }

    /** Counts a tick of the phase; true once the phase is complete. */
    private boolean advance(ServerLevel level, Status running) {
        status = running;
        stallTicks = 0;
        if (stalled) {
            stalled = false;
            phaseStart = level.getGameTime() - phaseDone;
            sync(level);
        }
        phaseDone++;
        setChanged();
        return phaseDone >= phaseLength;
    }

    /** The drill cannot go on this tick; a hold that lasts is shown to clients. */
    private void hold(ServerLevel level, Status why) {
        if (status != why) {
            status = why;
            setChanged();
        }
        if (phase == DrillPhase.CHARGING || phase == DrillPhase.FIRING) {
            stallTicks++;
            if (!stalled && stallTicks >= STALL_SYNC) {
                stalled = true;
                sync(level);
            }
        }
    }

    private void enter(ServerLevel level, DrillPhase next, int length) {
        phase = next;
        phaseLength = Math.max(1, length);
        phaseDone = 0;
        phaseStart = level.getGameTime();
        stalled = false;
        stallTicks = 0;
        setChanged();
        sync(level);
    }

    private void sync(ServerLevel level) {
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Lights the base while the drill runs; it goes dark only after a while stopped. */
    private void light(ServerLevel level) {
        boolean want = status.running();
        boolean lit = getBlockState().getValue(MegaDrillPartBlock.LIT);
        if (want == lit) {
            litTimer = 0;
            return;
        }
        if (want || ++litTimer >= LIT_DELAY) {
            litTimer = 0;
            MegaDrillStructure.setLit(level, worldPosition, facing(), want);
        }
    }

    // ---------------------------------------------------------------- the burst

    /** The whole layer bursts at once: every block the beam can break goes, with its own break dust. */
    private void burst(ServerLevel level, BlockPos centre) {
        FakePlayer player = player(level);
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        if (Config.get(Config.DRILL_GLAZE)) {
            glaze(level, centre, player);
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int count = 0;
        int boiled = 0;
        for (int dx = MegaDrillLayout.LOW; dx <= MegaDrillLayout.HIGH; dx++) {
            for (int dz = MegaDrillLayout.LOW; dz <= MegaDrillLayout.HIGH; dz++) {
                pos.set(centre.getX() + dx, layer, centre.getZ() + dz);
                if (!bursts(level, pos, dx, dz, centre.getY())) {
                    continue;
                }
                BlockPos at = pos.immutable();
                BlockState state = level.getBlockState(at);
                // It breaks blocks as its owner would, so land claims and protection mods can stop it.
                if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, at, state, player)).isCanceled()) {
                    continue;
                }
                BlockEntity entity = state.hasBlockEntity() ? level.getBlockEntity(at) : null;
                if (entity != null) {
                    // What a chest or barrel holds (a dungeon's loot) comes up too, whatever the filter.
                    takeContents(level, at);
                }
                boolean ore = state.is(Tags.Blocks.ORES);
                for (ItemStack drop : Block.getDrops(state, level, at, entity, player, tool)) {
                    if (ore || keeps(drop)) {
                        addPending(drop);
                    }
                }
                // Break dust (and a break sound) from every eighth block keeps the burst affordable.
                if (count++ % 8 == 0) {
                    level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, at, Block.getId(state));
                }
                // Water the beam meets boils away in a burst of steam.
                if (state.getFluidState().isSourceOfType(Fluids.WATER)) {
                    boil(level, at, boiled++);
                }
                // A full update, so water that a removed source or floor was holding up recedes or falls.
                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        double x = centre.getX() + 0.5;
        double z = centre.getZ() + 0.5;
        level.sendParticles(ParticleTypes.LAVA, x, layer + 0.6, z, 30, 2.2, 0.2, 2.2, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, layer + 0.6, z, 20, 2.2, 0.4, 2.2, 0.02);
        sound(level, BlockPos.containing(x, layer + 0.5, z), ModSounds.MEGA_DRILL_BURST.get(), 3.0F, 1.0F);
        flushPending();
    }

    /**
     * A water source boiling off under the beam: a puff of steam and bubbles from the block, and a
     * fizz from the first few each burst (one sound a source would stack into a roar).
     */
    private static void boil(ServerLevel level, BlockPos at, int boiled) {
        double x = at.getX() + 0.5;
        double y = at.getY() + 0.6;
        double z = at.getZ() + 0.5;
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 4, 0.3, 0.2, 0.3, 0.04);
        level.sendParticles(ParticleTypes.BUBBLE_POP, x, y, z, 6, 0.35, 0.15, 0.35, 0.02);
        if (boiled < 3) {
            level.playSound(null, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.2F, 0.8F + level.random.nextFloat() * 0.3F);
        }
    }

    /** A deep bore cycle: ore from the head's table, up the shaft into the output. */
    private void bore(ServerLevel level, BlockPos centre) {
        int mark = tools.getStackInSlot(SLOT_HEAD).getItem() instanceof DrillHeadItem head ? head.mark() : 1;
        for (ItemStack ore : DeepBore.bore(level, mark, Config.get(Config.DRILL_DEEP_ORE))) {
            addPending(ore);
        }
        double x = centre.getX() + 0.5;
        double z = centre.getZ() + 0.5;
        level.sendParticles(ParticleTypes.LAVA, x, layer + 1.1, z, 20, 1.2, 0.2, 1.2, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, layer + 1.1, z, 15, 1.2, 0.4, 1.2, 0.02);
        sound(level, BlockPos.containing(x, layer + 1, z), ModSounds.MEGA_DRILL_BURST.get(), 2.0F, 0.7F);
        flushPending();
    }

    /**
     * The beam glazes the pit's wall: water or lava in the ring just outside the area, on the layer
     * being mined, sets into obsidian (volcanic glass), so nothing flows into the pit. Real basis: the
     * rock-melting drills of the 1970s left their holes lined with glass.
     */
    private void glaze(ServerLevel level, BlockPos centre, FakePlayer player) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = MegaDrillLayout.LOW - 1; dx <= MegaDrillLayout.HIGH + 1; dx++) {
            for (int dz = MegaDrillLayout.LOW - 1; dz <= MegaDrillLayout.HIGH + 1; dz++) {
                boolean edge = dx == MegaDrillLayout.LOW - 1 || dx == MegaDrillLayout.HIGH + 1
                        || dz == MegaDrillLayout.LOW - 1 || dz == MegaDrillLayout.HIGH + 1;
                if (!edge) {
                    continue;
                }
                pos.set(centre.getX() + dx, layer, centre.getZ() + dz);
                BlockState state = level.getBlockState(pos);
                if (state.getFluidState().isEmpty() || !(state.getBlock() instanceof LiquidBlock || state.canBeReplaced())) {
                    continue;
                }
                BlockPos at = pos.immutable();
                if (!NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, at, state, player)).isCanceled()) {
                    level.setBlock(at, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    /** Empties a container in the layer into what the drill brings up, so its contents are not spilled. */
    private void takeContents(ServerLevel level, BlockPos at) {
        IItemHandler held = level.getCapability(Capabilities.ItemHandler.BLOCK, at, null);
        if (held == null) {
            return;
        }
        for (int slot = 0; slot < held.getSlots(); slot++) {
            ItemStack taken = held.extractItem(slot, Integer.MAX_VALUE, false);
            while (!taken.isEmpty()) {
                addPending(taken);
                taken = held.extractItem(slot, Integer.MAX_VALUE, false);
            }
        }
    }

    /** Whether the filter keeps a drop of a block that is not an ore. */
    private boolean keeps(ItemStack drop) {
        if (drop.is(ModTags.MEGA_DRILL_KEEPS)) {
            return true;
        }
        for (int slot = 0; slot < FILTERS; slot++) {
            ItemStack kept = filter.getStackInSlot(slot);
            if (!kept.isEmpty() && ItemStack.isSameItem(kept, drop)) {
                return true;
            }
        }
        return false;
    }

    private void addPending(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (ItemStack held : pending) {
            if (ItemStack.isSameItemSameComponents(held, stack)) {
                held.grow(stack.getCount());
                return;
            }
        }
        pending.add(stack.copy());
    }

    /** Moves what the burst freed into the output, as far as there is room. */
    private void flushPending() {
        if (pending.isEmpty()) {
            return;
        }
        List<ItemStack> left = new ArrayList<>();
        for (ItemStack stack : pending) {
            // Large piles go in a stack at a time, so the output's own limits apply.
            ItemStack rest = stack.copy();
            while (!rest.isEmpty()) {
                ItemStack piece = rest.split(Math.min(rest.getCount(), rest.getMaxStackSize()));
                ItemStack back = ItemHandlerHelper.insertItemStacked(output, piece, false);
                if (!back.isEmpty()) {
                    back.grow(rest.getCount());
                    left.add(back);
                    break;
                }
            }
        }
        pending.clear();
        pending.addAll(left);
        setChanged();
    }

    /** Pushes what the drill mined out of its output port into whatever is beside it. */
    private void pushOutput(ServerLevel level) {
        MegaDrillLayout.Port port = MegaDrillLayout.Port.OUTPUT;
        Direction face = port.face(facing());
        BlockPos target = MegaDrillLayout.toWorld(worldPosition, facing(), port.cell()).relative(face);
        if (!level.isLoaded(target)) {
            return;
        }
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

    /** Plays one of the drill's sounds, unless its screen has it muted. */
    private void sound(ServerLevel level, BlockPos at, net.minecraft.sounds.SoundEvent event, float volume, float pitch) {
        if (!muted) {
            level.playSound(null, at, event, SoundSource.BLOCKS, volume, pitch);
        }
    }

    private void fireSound(ServerLevel level, BlockPos at, int speed) {
        if (!muted) {
            playFire(level, at, speed);
        }
    }

    /**
     * The beam's shot, as fast as the drill runs (its heating is shorter at speed): cut at 1, 2 and 4
     * times its playback speed, the nearest below the drill's, pitched up the rest of the way.
     */
    public static void playFire(Level level, BlockPos at, int speed) {
        int cut = speed >= 4 ? 4 : speed >= 2 ? 2 : 1;
        net.minecraft.sounds.SoundEvent event = (cut == 4 ? ModSounds.MEGA_DRILL_FIRE_X4 : cut == 2 ? ModSounds.MEGA_DRILL_FIRE_X2
                : ModSounds.MEGA_DRILL_FIRE).get();
        level.playSound(null, at, event, SoundSource.BLOCKS, 2.0F, Math.min(2.0F, speed / (float) cut));
    }

    /** The beam's heat wears the emitter's tip a little each layer (a config sets how often). */
    private void wearTip(ServerLevel level) {
        if (level.random.nextInt(100) >= Config.get(Config.DRILL_TIP_WEAR)) {
            return;
        }
        ItemStack tip = tools.getStackInSlot(SLOT_TIP).copy();
        tip.hurtAndBreak(1, level, null, item -> sound(level, worldPosition, SoundEvents.ITEM_BREAK, 1.0F, 0.8F));
        tools.setStackInSlot(SLOT_TIP, tip.isEmpty() ? ItemStack.EMPTY : tip);
    }

    /** Who the drill breaks blocks as: its owner, so land claims treat it as theirs, or a stand-in. */
    private FakePlayer player(ServerLevel level) {
        if (owner != null && owner.getId() != null) {
            return FakePlayerFactory.get(level, new GameProfile(owner.getId(), "[RyzerGenDrill]"));
        }
        return FakePlayerFactory.getMinecraft(level);
    }

    // ---------------------------------------------------------------- building

    /** Places up to a few frame blocks from the store into their cells, lowest first. */
    private void buildStep(ServerLevel level) {
        int placed = 0;
        for (BlockPos cell : ORDER) {
            if (placed >= PER_STEP) {
                return;
            }
            BlockPos pos = MegaDrillLayout.toWorld(worldPosition, facing(), cell);
            BlockState there = level.getBlockState(pos);
            if (there.is(ModBlocks.DRILL_FRAME.get()) || !there.canBeReplaced() || !takePart()) {
                continue;
            }
            level.setBlock(pos, ModBlocks.DRILL_FRAME.get().defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.5F, 0.9F + level.random.nextFloat() * 0.2F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
            placed++;
            // The last part forms the drill, which ends the build.
            if (isRemoved() || isFormed()) {
                return;
            }
        }
    }

    private boolean takePart() {
        for (int slot = 0; slot < parts.getSlots(); slot++) {
            if (isPart(parts.getStackInSlot(slot))) {
                parts.extractItem(slot, 1, false);
                return true;
            }
        }
        return false;
    }

    /** Counts what is still to place and what is in the way, for the screen. */
    private void survey(Level level) {
        missing = 0;
        blocked = 0;
        for (BlockPos cell : ORDER) {
            BlockState there = level.getBlockState(MegaDrillLayout.toWorld(worldPosition, facing(), cell));
            if (there.is(ModBlocks.DRILL_FRAME.get())) {
                continue;
            }
            missing++;
            if (!there.canBeReplaced()) {
                blocked++;
            }
        }
    }

    /** A part of the formed drill was broken: it stops where it is, and picks up there once rebuilt. */
    void brokenApart() {
        phase = DrillPhase.IDLE;
        stalled = false;
        status = Status.OFF;
        setChanged();
        if (level instanceof ServerLevel server) {
            sync(server);
        }
    }

    // ---------------------------------------------------------------- contents

    /** Empties the parts store onto the ground at {@code pos}. */
    public void dropParts(Level level, BlockPos pos) {
        drop(level, pos, parts);
    }

    /** Everything the controller holds, dropped when it is broken. The water is lost. */
    public void dropAll(Level level, BlockPos pos) {
        drop(level, pos, parts);
        drop(level, pos, tools);
        drop(level, pos, output);
        drop(level, pos, filter);
        for (ItemStack stack : pending) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        pending.clear();
    }

    private static void drop(Level level, BlockPos pos, ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(slot));
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    // ---------------------------------------------------------------- controls

    public void togglePower() {
        enabled = !enabled;
        setChanged();
    }

    public void toggleMute() {
        muted = !muted;
        setChanged();
    }

    public void cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        setChanged();
    }

    // ---------------------------------------------------------------- client

    /** The client's GPU mesh of the drill's body (a StationMesh), closed when the controller goes. */
    private Object clientMesh;

    public static void clientTick(Level level, BlockPos pos, BlockState state, MegaDrillBlockEntity drill) {
        MegaDrillGhostPreview.track(drill);
        if (!MegaDrillPartBlock.isFormed(state) || !state.getValue(MegaDrillPartBlock.LIT)) {
            return;
        }
        effects(level, drill, drill.drillCentre(), drill.layer());
    }

    /**
     * Sparks at the lens as the charge arrives; once it fires, lava spits where the beam strikes and
     * smoke rises off the layer. Shared with the preview.
     */
    public static void effects(Level level, MegaDrillView drill, BlockPos centre, int layer) {
        RandomSource random = level.random;
        double cx = centre.getX() + 0.5;
        double cz = centre.getZ() + 0.5;
        float p = drill.drillProgress(level.getGameTime(), 0);
        if (drill.drillPhase() == DrillPhase.CHARGING && p > 0.8F) {
            // The aperture in the emitter pod's foot.
            double lens = centre.getY() + 25.5 / 16.0;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, cx + random.nextGaussian() * 0.15, lens, cz + random.nextGaussian() * 0.15,
                    random.nextGaussian() * 0.05, -0.05, random.nextGaussian() * 0.05);
        }
        if (drill.drillPhase() == DrillPhase.FIRING) {
            double y = layer + 1.02;
            if (random.nextFloat() < 0.6F) {
                level.addParticle(ParticleTypes.LAVA, cx, y, cz, 0, 0, 0);
            }
            level.addParticle(ParticleTypes.SMOKE, cx + random.nextGaussian() * 0.2, y, cz + random.nextGaussian() * 0.2, 0, 0.05, 0);
            int rising = (int) (p * 5);
            for (int i = 0; i < rising; i++) {
                double x = centre.getX() + MegaDrillLayout.LOW + random.nextDouble() * (MegaDrillLayout.HIGH - MegaDrillLayout.LOW + 1);
                double z = centre.getZ() + MegaDrillLayout.LOW + random.nextDouble() * (MegaDrillLayout.HIGH - MegaDrillLayout.LOW + 1);
                level.addParticle(p > 0.6F && random.nextFloat() < 0.15F ? ParticleTypes.LAVA : ParticleTypes.SMOKE, x, y, z, 0, 0.04, 0);
            }
        }
    }

    private int layer() {
        return layer == UNSET ? centre().getY() : layer;
    }

    @Override
    public boolean drillShown() {
        return isFormed();
    }

    @Override
    public Direction drillFacing() {
        return facing();
    }

    @Override
    public boolean drillRunning() {
        return getBlockState().getValue(MegaDrillPartBlock.LIT);
    }

    @Override
    public DrillPhase drillPhase() {
        return drillRunning() ? phase : DrillPhase.IDLE;
    }

    @Override
    public float drillProgress(long gameTime, float partialTick) {
        float done = stalled ? phaseDone : gameTime - phaseStart + partialTick;
        return Mth.clamp(done / phaseLength, 0, 1);
    }

    @Override
    public float drillElapsed(long gameTime, float partialTick) {
        return stalled ? phaseDone : gameTime - phaseStart + partialTick;
    }

    @Override
    public int drillLayer() {
        return layer();
    }

    @Override
    public BlockPos drillCentre() {
        return centre();
    }

    /** The shaft's blocks that will burst; in the deep bore, the bottom layer glows where the beam bores on. */
    @Override
    public boolean drillHeats(BlockGetter level, BlockPos pos, int dx, int dz) {
        if (deep) {
            BlockState state = level.getBlockState(pos);
            return pos.getY() == layer && !MegaDrillLayout.footing(dx, dz) && !state.isAir()
                    && !(state.getBlock() instanceof MegaDrillPartBlock);
        }
        return bursts(level, pos, dx, dz, centre().getY());
    }

    @Override
    public Object drillMesh() {
        return clientMesh;
    }

    @Override
    public void drillMesh(Object mesh) {
        clientMesh = mesh;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (clientMesh instanceof AutoCloseable mesh) {
            try {
                mesh.close();
            } catch (Exception ignored) {
                // Only frees GPU memory; nothing to recover.
            }
            clientMesh = null;
        }
    }

    // ---------------------------------------------------------------- menus and saving

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.ryzergen.mega_drill");
    }

    /** Unbuilt, the screen is the parts store; formed, it is the drill's controls. */
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        ContainerLevelAccess access = ContainerLevelAccess.create(level, worldPosition);
        return isFormed() ? new MegaDrillMenu(containerId, inventory, tools, output, filter, data, access)
                : new MegaDrillBuildMenu(containerId, inventory, parts, data, access);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("parts", parts.serializeNBT(registries));
        tag.put("tools", tools.serializeNBT(registries));
        tag.put("output", output.serializeNBT(registries));
        tag.put("filter", filter.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        ListTag held = new ListTag();
        for (ItemStack stack : pending) {
            held.add(stack.save(registries));
        }
        tag.put("pending", held);
        tag.putInt("speed", speed);
        tag.putBoolean("finished", finished);
        tag.putInt("status", status.ordinal());
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("muted", muted);
        tag.putInt("redstone_mode", redstoneMode.ordinal());
        if (owner != null && owner.getId() != null) {
            tag.putUUID("owner", owner.getId());
            tag.putString("owner_name", owner.getName());
        }
        saveCycle(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        parts.deserializeNBT(registries, tag.getCompound("parts"));
        tools.deserializeNBT(registries, tag.getCompound("tools"));
        output.deserializeNBT(registries, tag.getCompound("output"));
        filter.deserializeNBT(registries, tag.getCompound("filter"));
        energy.setStored(tag.getInt("energy"));
        water.readFromNBT(registries, tag.getCompound("water"));
        pending.clear();
        for (Tag held : tag.getList("pending", Tag.TAG_COMPOUND)) {
            ItemStack.parse(registries, held).ifPresent(pending::add);
        }
        speed = Math.max(1, tag.getInt("speed"));
        finished = tag.getBoolean("finished");
        status = Status.byId(tag.getInt("status"));
        enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        muted = tag.getBoolean("muted");
        redstoneMode = RedstoneMode.byId(tag.getInt("redstone_mode"));
        if (tag.hasUUID("owner")) {
            UUID id = tag.getUUID("owner");
            owner = new GameProfile(id, tag.getString("owner_name"));
        }
        loadCycle(tag);
    }

    private void saveCycle(CompoundTag tag) {
        tag.putInt("phase", phase.ordinal());
        tag.putLong("phase_start", phaseStart);
        tag.putInt("phase_length", phaseLength);
        tag.putInt("phase_done", phaseDone);
        tag.putBoolean("stalled", stalled);
        tag.putInt("layer", layer);
        tag.putBoolean("deep", deep);
    }

    private void loadCycle(CompoundTag tag) {
        phase = DrillPhase.byId(tag.getInt("phase"));
        phaseStart = tag.getLong("phase_start");
        phaseLength = Math.max(1, tag.getInt("phase_length"));
        phaseDone = tag.getInt("phase_done");
        stalled = tag.getBoolean("stalled");
        layer = tag.contains("layer") ? tag.getInt("layer") : UNSET;
        deep = tag.getBoolean("deep");
    }

    /** Clients get only the cycle: the phase, when it began, how long it lasts and the layer. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveCycle(tag);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadCycle(tag);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }
}
