package com.ryzer.ryzergen.machine.fission;

import com.ryzer.ryzergen.machine.Announcer;
import org.jetbrains.annotations.Nullable;
import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.advancement.Milestone;
import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.machine.fission.StationReactor.Channel;
import com.ryzer.ryzergen.radiation.RadiationSources;
import com.ryzer.ryzergen.registry.ModDataComponents;
import com.ryzer.ryzergen.registry.ModTriggers;
import com.ryzer.ryzergen.machine.SteadyState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A formed fission station at work: the 25 core channels (their plan and what is in them), the
 * water tank, the energy buffer and the core's temperature. Each tick the fuel makes heat
 * ({@link StationReactor}), the coolant carries it off as steam (drinking water) to the turbine,
 * the turbine makes power (more efficiently the hotter the core), and the rods burn down.
 *
 * <p>If the coolant cannot keep up, or the water runs out, the core heats up; at 900°C the
 * automatic SCRAM drops the control rods and the reaction stops until the core is below 400°C.
 * Real basis: a boiling water reactor and its scram.
 *
 * <p>Overdrive (safeties off): more power (see {@link StationReactor}), but no SCRAM, and every
 * fuel channel must hold a live rod. A gap in the fuel skews the neutron flux (a flux tilt); with
 * the control rods pulled out nothing evens it out. After 10 seconds' grace (time to swap a rod)
 * the alarm starts, and after five more minutes the core goes unstable
 * and heats without limit. Only re-arming the safeties (which SCRAMs) saves it then; at 1000°C it
 * melts down. So overdrive rewards automation that swaps spent rods promptly. Real basis: power
 * peaking around a gap in the fuel, with too little control rod margin to hold it.
 *
 * <p>The coolant reserve (design section 8), built onto the station's back in bays, fills from the
 * station's spare water. In overdrive, when the core has lost its cooling and passes 620°C, it floods
 * the core and holds it there while its water lasts (two minutes a bay by default); dry, the core
 * heats on as before. It never touches an unstable core: a flux tilt is a reactivity fault, not a
 * loss of cooling. Real basis: emergency core cooling accumulators, which hold only their own water.
 */
public class StationRunner {
    public static final int WATER_CAPACITY = 16_000;
    public static final int ENERGY_CAPACITY = 2_000_000;
    /**
     * Thermal FE one mB of water carries away before the water-use factor: water is cheap, so a
     * station drinks a lot of it (coolant is your friend, design section 5). Running it takes 4
     * times this much water, and overdrive 8 times, scaled by {@code fission_station.water_use_percent}.
     */
    public static final float HEAT_PER_WATER = 200;
    public static final float WATER_USE = 4;
    public static final float OVERDRIVE_WATER_USE = 8;
    /** Thermal FE to warm the core by one degree. */
    private static final float HEAT_CAPACITY = 20_000;
    public static final float AMBIENT = 20;
    public static final float SCRAM_TEMPERATURE = 900;
    private static final float RESET_TEMPERATURE = 400;
    public static final float HOT_TEMPERATURE = 700;
    /**
     * In overdrive a core that holds steady settles at 600°C at most (full coolant load), so one
     * past this is overheating with nothing to stop it: a runaway, heading for meltdown.
     */
    public static final float RUNAWAY_TEMPERATURE = 620;
    private static final float WARM_TEMPERATURE = 150;
    /** In overdrive nothing stops the heat: at this temperature the core melts down. */
    public static final float MELTDOWN_TEMPERATURE = 1000;
    /** Degrees per tick an unstable core climbs. */
    private static final float RUNAWAY_RATE = 1;
    /** Water each bay of the coolant reserve holds. */
    public static final int RESERVE_PER_BAY = 16_000;
    /** The most water a tick the reserve takes, and only while the station's tank is over half full. */
    private static final int RESERVE_FILL = 100;
    /** Dose rate in mSv/s one block from the core: a meltdown site, which fades over 40 minutes. */
    private static final float MELTDOWN_RADIATION = 500;
    private static final long MELTDOWN_FADE_TICKS = 48_000;

    public enum Status {
        // New statuses go at the end: the status is saved by its position in this list.
        OFFLINE, NO_FUEL, WARMING, ONLINE, NO_WATER, OVERHEAT, SCRAM, OVERDRIVE, FLUX_TILT, UNSTABLE, RUNAWAY, INJECTING;

        /** Whether the alarm sounds. */
        public boolean alarm() {
            return this == FLUX_TILT || this == UNSTABLE || this == RUNAWAY || this == INJECTING;
        }

        /** Heading for meltdown, so the alarm climbs with the core's temperature. */
        public boolean critical() {
            return this == UNSTABLE || this == RUNAWAY;
        }
    }

    private final Runnable changed;
    /** Each core's plan (uranium, MOX); the station runs the one picked on its screen's tabs. */
    final Channel[][] plans = {new Channel[StationReactor.CHANNELS], new Channel[StationReactor.CHANNELS]};
    /** The core picked, and its plan. */
    StationReactor.Core core = StationReactor.Core.URANIUM;
    Channel[] types = plans[0];
    private final float[] burnt = new float[StationReactor.CHANNELS];

    final ItemStackHandler channels = new ItemStackHandler(StationReactor.CHANNELS) {
        @Override
        protected void onContentsChanged(int slot) {
            changed.run();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return types[slot].accepts(stack, core);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    final FluidTank water = new FluidTank(WATER_CAPACITY, stack -> stack.is(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            changed.run();
        }
    };

    /** The energy buffer: outside callers can only take from it; the turbine fills it. */
    private final class Buffer extends EnergyStorage {
        Buffer() {
            super(ENERGY_CAPACITY, 0, ENERGY_CAPACITY);
        }

        void generate(int amount) {
            energy = Math.min(capacity, energy + amount);
            changed.run();
        }

        void set(int amount) {
            energy = Math.max(0, Math.min(capacity, amount));
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int out = super.extractEnergy(toExtract, simulate);
            if (out > 0 && !simulate) {
                changed.run();
            }
            return out;
        }
    }

    private final Buffer energy = new Buffer();

    /** The fuel port: anything a channel is planned for goes in (fresh rods, graphite, control rods). Nothing comes out. */
    final IItemHandler fuelPort = new IItemHandler() {
        @Override
        public int getSlots() {
            return StationReactor.CHANNELS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return channels.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // After an eject the core takes nothing until the next inject, so a pipe does not refill it.
            return feeding && types[slot].accepts(stack, core) ? channels.insertItem(slot, stack, simulate) : stack;
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
            return types[slot].accepts(stack, core);
        }
    };

    /**
     * The output port: spent rods, and anything a channel's plan no longer takes, come out (pushed
     * into whatever is beside it, or pulled by a pipe); nothing goes in. Waste from later machines
     * will leave the same way.
     */
    final IItemHandler outputPort = new IItemHandler() {
        @Override
        public int getSlots() {
            return StationReactor.CHANNELS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return leavingAt(slot) ? channels.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return leavingAt(slot) ? channels.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    boolean enabled = true;
    /**
     * Whether the fuel port fills the core's empty channels. An eject stops it (so a pipe does not
     * put the parts straight back), and the next inject starts it again.
     */
    boolean feeding = true;
    /**
     * An eject still under way: every part in the core counts as leaving, so a pipe pulling from the
     * output port takes them all, and anything that accepts items beside it is pushed them. Ends
     * once the core is empty, or with the next inject.
     */
    boolean draining;

    /** Whether channel {@code i}'s item leaves by the output port. */
    boolean leavingAt(int i) {
        ItemStack held = channels.getStackInSlot(i);
        return !held.isEmpty() && (draining || StationReactor.leaving(types[i], held, core));
    }
    RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    /** Interlocks armed. Off means overdrive. */
    boolean safeties = true;
    /** Ticks a fuel channel has gone without a live rod in overdrive. */
    int tiltTicks;
    /** Past the flux tilt limit: the core heats without limit until the safeties are re-armed. */
    boolean unstable;
    float temperature = AMBIENT;
    private boolean scrammed;
    Status status = Status.OFFLINE;
    /** The control system's voice, and what it last knew, to speak only on a change. Not saved. */
    private final Announcer announcer = new Announcer();
    final SteadyState steady = new SteadyState();
    /** One load of every fuel channel: about 30 minutes of running. */
    private static final float LOAD_TICKS = 36_000;
    private @Nullable Boolean announcedSafeties;
    private @Nullable Boolean announcedScram;
    /** The current SCRAM came from the player re-arming the safeties: a controlled shutdown. Not saved. */
    private boolean plannedScram;
    /** Ticks since the station last ran: it only says it is online after being down a while. */
    private int downTicks = RESTART_ANNOUNCE_TICKS;
    private static final int RESTART_ANNOUNCE_TICKS = 200;
    /** Bays of coolant reserve built onto the formed station (0, 1, 3 or 5), set by {@link StationReserve}. */
    int reserveBays;
    /** Water in the reserve, in mB. */
    float reserve;
    int output;
    int efficiencyPermille;
    /** How hard the turbine is turning, 0 to 1, for the renderer. */
    float turbine;

    StationRunner(Runnable changed) {
        for (Channel[] plan : plans) {
            java.util.Arrays.fill(plan, Channel.EMPTY);
        }
        this.changed = changed;
    }

    public IEnergyStorage energy() {
        return energy;
    }

    /** A channel's plan and what is in it, for the renderer. */
    public Channel type(int channel) {
        return types[channel];
    }

    public ItemStack item(int channel) {
        return channels.getStackInSlot(channel);
    }

    public IFluidHandler waterPort() {
        return waterPort;
    }

    public IItemHandler fuelPort() {
        return fuelPort;
    }

    public IItemHandler outputPort() {
        return outputPort;
    }

    public Status status() {
        return status;
    }

    public float temperature() {
        return temperature;
    }

    /** The core's plan, a copy. */
    public Channel[] types() {
        return types.clone();
    }

    /** FE/t the turbine made last tick. */
    public int output() {
        return output;
    }

    public int reserveBays() {
        return reserveBays;
    }

    /** A bay was built or lost: the reserve's water over its new capacity is gone. */
    void setReserveBays(int bays) {
        if (bays != reserveBays) {
            reserveBays = bays;
            reserve = Math.min(reserve, reserveCapacity());
            changed.run();
        }
    }

    public int reserveCapacity() {
        return reserveBays * RESERVE_PER_BAY;
    }

    /** Water in the coolant reserve, in mB. */
    public float reserve() {
        return reserve;
    }

    /** mB the reserve drains each tick while it holds the core: a full bay lasts the configured time. */
    private static float reserveDrain() {
        return RESERVE_PER_BAY / (Config.get(Config.STATION_RESERVE_SECONDS) * 20F);
    }

    /** Seconds the reserve's water would hold the core for, from now. */
    public int reserveSeconds() {
        return reserve <= 0 ? 0 : (int) Math.ceil(reserve / reserveDrain() / 20);
    }

    /**
     * Ticks a fuel channel may sit without a live rod before the flux tilt counts: time to swap a
     * spent rod by hand without setting off the alarm.
     */
    private static final int TILT_GRACE = 200;

    /** Whether a gap has outlasted the grace period, so the countdown and alarm run. */
    boolean tilting() {
        return tiltTicks > TILT_GRACE;
    }

    /** Ticks left before a flux tilt makes the core unstable. */
    public int tiltTicksLeft() {
        return Math.max(0, Math.min(tiltLimit(), TILT_GRACE + tiltLimit() - tiltTicks));
    }

    private static int tiltLimit() {
        return Config.get(Config.STATION_TILT_SECONDS) * 20;
    }

    /** What a forecast ends in, if anything stops it. */
    public enum Outcome { NONE, SCRAM, MELTDOWN }

    /**
     * The core's temperature run forward from now with this layout, for the projection's forecast:
     * the same heat sums as {@link #tick} (keep the two in step), assuming the water holds out.
     * {@code samples[i]} is the temperature after {@code i * every} ticks. A core that is not running
     * only cools. A flux tilt makes it unstable after {@code unstableIn} ticks (0 now, below 0 never),
     * and in overdrive the coolant reserve holds it at 620°C for {@code reserveTicks}.
     */
    public static Forecast forecast(StationReactor.Analysis analysis, float temperature, boolean running, boolean overdrive,
                                    int unstableIn, int reserveTicks, int ticks, int every) {
        float[] samples = new float[ticks / every + 1];
        samples[0] = temperature;
        float t = temperature;
        boolean on = running;
        Outcome outcome = Outcome.NONE;
        int at = -1;
        for (int tick = 1; tick <= ticks; tick++) {
            float generation = on ? analysis.generation() : 0;
            float stored = (t - AMBIENT) * HEAT_CAPACITY;
            float carryable = generation > 0 ? Math.max(0, generation - analysis.stranded()) : 0;
            boolean routed = analysis.stranded() <= 0.5F;
            float reachable = generation > 0 && !routed ? carryable : carryable + stored;
            int working = generation > 0 && analysis.working() > 0 ? analysis.working() : analysis.coolants();
            float potential = working * analysis.capacity() * StationReactor.coolingRamp(t);
            t += (generation - Math.max(0, Math.min(potential, reachable))) / HEAT_CAPACITY;
            if (generation <= 0) {
                t -= (t - AMBIENT) * 0.0005F;
            }
            t = Math.max(AMBIENT, t);
            boolean unstable = on && unstableIn >= 0 && tick >= unstableIn;
            if (unstable) {
                t += RUNAWAY_RATE;
            }
            if (overdrive && !unstable && reserveTicks > 0 && t > RUNAWAY_TEMPERATURE) {
                t = RUNAWAY_TEMPERATURE;
                reserveTicks--;
            }
            if (outcome == Outcome.NONE && t >= MELTDOWN_TEMPERATURE) {
                outcome = Outcome.MELTDOWN;
                at = tick;
                t = MELTDOWN_TEMPERATURE;
                on = false;
            } else if (outcome == Outcome.NONE && on && !overdrive && t >= SCRAM_TEMPERATURE) {
                outcome = Outcome.SCRAM;
                at = tick;
                on = false;
            }
            if (tick % every == 0) {
                samples[tick / every] = t;
            }
        }
        return new Forecast(samples, every, outcome, at);
    }

    /** A forecast: temperatures every {@code every} ticks, and what stopped it and when ({@code at}, -1 if nothing). */
    public record Forecast(float[] samples, int every, Outcome outcome, int at) {}

    /**
     * The safety switch. Disarming is the player's choice; re-arming SCRAMs the core at once, which
     * is also the only way to save one gone unstable. {@code byPlayer} is false when the interlock
     * does it: a SCRAM the player chose is a controlled shutdown, so the announcer does not warn.
     */
    public void toggleSafeties(boolean byPlayer) {
        safeties = !safeties;
        if (safeties) {
            if (unstable || temperature >= RESET_TEMPERATURE) {
                scrammed = true;
                plannedScram = byPlayer;
            }
            unstable = false;
            tiltTicks = 0;
        }
        changed.run();
    }

    /** Whether a fuel channel lacks a live rod (empty, or holding a spent one). */
    private boolean fuelGap() {
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            if (types[i] == Channel.FUEL && !StationReactor.isFreshFuel(channels.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    public ItemStack[] rods() {
        ItemStack[] rods = new ItemStack[StationReactor.CHANNELS];
        for (int i = 0; i < rods.length; i++) {
            rods[i] = channels.getStackInSlot(i);
        }
        return rods;
    }

    /**
     * Sets a channel's plan (only the types a plan may hold). Whatever no longer fits comes back out,
     * for the player to take; a spent rod stays in a fuel channel until the output port takes it.
     */
    public ItemStack setType(int channel, Channel type) {
        if (!type.plannable(core)) {
            return ItemStack.EMPTY;
        }
        types[channel] = type;
        ItemStack held = channels.getStackInSlot(channel);
        boolean spentHere = type == Channel.FUEL && StationReactor.isSpent(held);
        if (!held.isEmpty() && !type.accepts(held, core) && !spentHere) {
            channels.setStackInSlot(channel, ItemStack.EMPTY);
            changed.run();
            return held;
        }
        changed.run();
        return ItemStack.EMPTY;
    }

    /** What an inject or eject did, for the screen. */
    public record Result(Command command, int moved, int missing) {}

    /** The outcomes of the screen's two commands. New ones go at the end: the screen gets them by position. */
    public enum Command {
        NONE,
        /** Every planned part is in. */
        INJECTED,
        /** Some parts are still missing; they go in as they reach the input port. */
        INJECTED_PART,
        /** Nothing on the input port to take parts from, and parts were needed. */
        NO_INPUT,
        /** The core already holds everything its plan needs. */
        NOTHING_NEEDED,
        EJECTED,
        /** What is on the output port is full; some parts are still in the core. */
        EJECTED_PART,
        /** Nothing on the output port to send parts to. */
        NO_OUTPUT,
        NOTHING_TO_EJECT,
        /** The other core was picked (after ejecting this one's parts, if there were any). */
        SWITCHED,
        /** A pipe pulling from the output port is taking the core's parts out. */
        DRAINING;

        public static Command byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : NONE;
        }

        public boolean error() {
            return this == NO_INPUT || this == NO_OUTPUT || this == EJECTED_PART;
        }
    }

    /** The inventory beside a port's face (a chest, or a pipe), or null. */
    private static @Nullable IItemHandler beside(ServerLevel level, BlockPos core, Direction facing, StationLayout.Port port) {
        BlockPos at = StationLayout.portPos(core, facing, port);
        Direction face = StationLayout.portFace(facing, port);
        return level.getCapability(Capabilities.ItemHandler.BLOCK, at.relative(face), face.getOpposite());
    }

    /** What sits beside a port: 0 nothing, 1 an inventory, 2 a pipe. */
    private static int kindBeside(ServerLevel level, BlockPos core, Direction facing, StationLayout.Port port) {
        BlockPos at = StationLayout.portPos(core, facing, port);
        Direction face = StationLayout.portFace(facing, port);
        if (level.getBlockEntity(at.relative(face)) instanceof com.ryzer.ryzergen.cable.ItemPipeBlockEntity) {
            return 2;
        }
        return beside(level, core, facing, port) != null ? 1 : 0;
    }

    /**
     * Inject components: fills every empty channel planned for fuel or graphite from what is beside
     * the input port (a chest; a pipe delivers on its own), and turns feeding back on, so fresh rods
     * keep coming in as rods are spent.
     */
    public Result inject(ServerLevel level, BlockPos core, Direction facing) {
        feeding = true;
        draining = false;
        IItemHandler source = beside(level, core, facing, StationLayout.Port.FUEL);
        int needed = 0;
        int moved = 0;
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            if ((types[i] != Channel.FUEL && types[i] != Channel.MODERATOR && types[i] != Channel.CONTROL)
                    || types[i].accepts(channels.getStackInSlot(i), this.core)) {
                continue;
            }
            needed++;
            if (source == null || !channels.getStackInSlot(i).isEmpty()) {
                continue;
            }
            for (int slot = 0; slot < source.getSlots(); slot++) {
                if (types[i].accepts(source.getStackInSlot(slot), this.core) && types[i].accepts(source.extractItem(slot, 1, true), this.core)) {
                    channels.setStackInSlot(i, source.extractItem(slot, 1, false));
                    moved++;
                    break;
                }
            }
        }
        changed.run();
        if (needed == 0) {
            return new Result(Command.NOTHING_NEEDED, 0, 0);
        }
        if (source == null) {
            return new Result(Command.NO_INPUT, 0, needed);
        }
        return new Result(moved == needed ? Command.INJECTED : Command.INJECTED_PART, moved, needed - moved);
    }

    /**
     * Eject components: sends every part in the core (rods, graphite, control rods, spent rods) out
     * of the output port, and stops feeding until the next inject. The plan stays. What is beside
     * the port and takes items (a chest, a pipe set to carry items away) is pushed them at once; a
     * pipe pulling from the port takes them as it runs, since the whole core counts as leaving until
     * it is empty. A full chest keeps the rest until it has room.
     */
    public Result eject(ServerLevel level, BlockPos core, Direction facing) {
        int held = 0;
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            held += channels.getStackInSlot(i).isEmpty() ? 0 : 1;
        }
        if (held == 0) {
            return new Result(Command.NOTHING_TO_EJECT, 0, 0);
        }
        int kind = kindBeside(level, core, facing, StationLayout.Port.OUTPUT);
        if (kind == 0) {
            return new Result(Command.NO_OUTPUT, 0, held);
        }
        feeding = false;
        draining = true;
        int moved = 0;
        IItemHandler target = beside(level, core, facing, StationLayout.Port.OUTPUT);
        if (target != null) {
            for (int i = 0; i < StationReactor.CHANNELS; i++) {
                ItemStack part = channels.getStackInSlot(i);
                if (!part.isEmpty() && ItemHandlerHelper.insertItem(target, part.copy(), false).isEmpty()) {
                    channels.setStackInSlot(i, ItemStack.EMPTY);
                    moved++;
                }
            }
        }
        changed.run();
        if (moved == held) {
            draining = false;
            return new Result(Command.EJECTED, moved, 0);
        }
        return new Result(kind == 2 ? Command.DRAINING : Command.EJECTED_PART, moved, held - moved);
    }

    /**
     * Picks the other core. A core holding parts is emptied first (as an eject would: the screen
     * warns before it asks); if they cannot all leave, nothing switches and the eject's error says why.
     */
    public Result switchCore(ServerLevel level, BlockPos at, Direction facing, StationReactor.Core to) {
        if (to == core) {
            return new Result(Command.NONE, 0, 0);
        }
        // A pipe draining the core may still be taking its parts out: they leave whatever the new plan is.
        Result ejected = eject(level, at, facing);
        if (ejected.command() == Command.NO_OUTPUT || ejected.command() == Command.EJECTED_PART) {
            return ejected;
        }
        core = to;
        types = plans[to.ordinal()];
        tiltTicks = 0;
        changed.run();
        return new Result(Command.SWITCHED, ejected.moved() + ejected.missing(), 0);
    }

    public StationReactor.Core core() {
        return core;
    }

    /** A core's plan, a copy. */
    public Channel[] plan(StationReactor.Core of) {
        return plans[of.ordinal()].clone();
    }

    /** What the ports see, for the screen's components box: refreshed about once a second. */
    int inputKind;
    int inputFuel;
    int inputGraphite;
    int inputControl;
    int outputKind;
    int outputFree;

    private void scanPorts(ServerLevel level, BlockPos core, Direction facing) {
        inputKind = kindBeside(level, core, facing, StationLayout.Port.FUEL);
        outputKind = kindBeside(level, core, facing, StationLayout.Port.OUTPUT);
        inputFuel = 0;
        inputGraphite = 0;
        inputControl = 0;
        outputFree = 0;
        IItemHandler source = inputKind == 1 ? beside(level, core, facing, StationLayout.Port.FUEL) : null;
        if (source != null) {
            // A big storage network can have thousands of slots: count only the first few hundred.
            for (int slot = 0; slot < Math.min(source.getSlots(), 512); slot++) {
                ItemStack stack = source.getStackInSlot(slot);
                if (Channel.FUEL.accepts(stack, this.core)) {
                    inputFuel += stack.getCount();
                } else if (Channel.MODERATOR.accepts(stack, this.core)) {
                    inputGraphite += stack.getCount();
                } else if (Channel.CONTROL.accepts(stack, StationReactor.Core.MOX)) {
                    inputControl += stack.getCount();
                }
            }
        }
        IItemHandler target = outputKind == 1 ? beside(level, core, facing, StationLayout.Port.OUTPUT) : null;
        if (target != null) {
            for (int slot = 0; slot < Math.min(target.getSlots(), 512); slot++) {
                outputFree += target.getStackInSlot(slot).isEmpty() ? 1 : 0;
            }
        }
    }

    /** One tick. {@code centre} is the middle of the station, for radiation. */
    /** Set each tick by the core from its sound switch: muted, the announcer speaks only of meltdown. */
    boolean quiet;

    void tick(ServerLevel level, BlockPos core, Direction facing, Vec3 centre) {
        announcer.mute(quiet);
        boolean overdrive = !safeties;
        StationReactor.Analysis analysis = StationReactor.analyse(types, rods(), overdrive, this.core);
        boolean allowed = enabled && redstoneMode.allows(level.hasNeighborSignal(core));
        if (scrammed && temperature < RESET_TEMPERATURE) {
            scrammed = false;
        }
        float generation = allowed && !scrammed ? analysis.generation() : 0;

        // Overdrive: a fuel channel without a live rod starts the flux tilt clock.
        if (overdrive && generation > 0 && fuelGap()) {
            tiltTicks++;
            if (tiltTicks >= TILT_GRACE + tiltLimit() && !unstable) {
                if (Config.STATION_MELTDOWNS.get()) {
                    unstable = true;
                } else {
                    // Meltdowns are off: the interlock refuses to stay disarmed.
                    toggleSafeties(false);
                    scrammed = true;
                    overdrive = false;
                }
            }
        } else if (!unstable) {
            tiltTicks = 0;
        }

        // Coolant carries heat away as steam, as far as its channels, the core's temperature and the
        // water allow. Coolant is local (see StationReactor): heat no channel can reach, from a rod
        // with no coolant beside it or more than a channel carries, stays and warms the core, and
        // while a core makes such heat its coolant carries only what the layout routes to it, so the
        // core climbs until it SCRAMs, as the control screen warns. (Before, the spare channels also
        // carried off that stored heat, so a core the screen called too hot ran fine.) Heat stored any
        // other way, from warming up or with the reaction stopped, every coolant channel carries off.
        // The projection's forecast runs these same sums ahead (forecast()): keep the two in step.
        float stored = (temperature - AMBIENT) * HEAT_CAPACITY;
        float heatPerWater = heatPerWater(overdrive);
        float byWater = water.getFluidAmount() * heatPerWater;
        float carryable = generation > 0 ? Math.max(0, generation - analysis.stranded()) : 0;
        boolean routed = analysis.stranded() <= 0.5F;
        float reachable = generation > 0 && !routed ? carryable : carryable + stored;
        int working = generation > 0 && analysis.working() > 0 ? analysis.working() : analysis.coolants();
        float potential = working * analysis.capacity() * StationReactor.coolingRamp(temperature);
        float removed = Math.max(0, Math.min(potential, Math.min(reachable, byWater)));
        if (removed > 0) {
            water.drain((int) Math.ceil(removed / heatPerWater), IFluidHandler.FluidAction.EXECUTE);
        }
        // The coolant reserve fills from what the core does not need: only while the station's tank
        // is over half full, so it never starves the core, and never while it is holding the core.
        if (reserveBays > 0 && status != Status.INJECTING) {
            int take = (int) Math.min(Math.min(water.getFluidAmount() - WATER_CAPACITY / 2, RESERVE_FILL), reserveCapacity() - reserve);
            if (take > 0) {
                water.drain(take, IFluidHandler.FluidAction.EXECUTE);
                reserve += take;
            }
        }
        temperature += (generation - removed) / HEAT_CAPACITY;
        // A stopped core slowly loses its heat to the air. Not while it runs: the plan (and the
        // rating) assume every bit of heat reaches the turbine, and the leak cost about a fifth of it.
        if (generation <= 0) {
            temperature -= (temperature - AMBIENT) * 0.0005F;
        }
        temperature = Math.max(AMBIENT, temperature);
        if (unstable) {
            temperature += RUNAWAY_RATE;
        }
        // The coolant reserve: in overdrive, a core that has lost its cooling is flooded and held at
        // 620°C while the reserve's water lasts, draining at a steady rate whatever the heat. Never an
        // unstable core (a flux tilt is a reactivity fault, which only re-arming the safeties stops).
        boolean injecting = false;
        if (overdrive && !unstable && reserve > 0 && temperature > RUNAWAY_TEMPERATURE) {
            temperature = RUNAWAY_TEMPERATURE;
            reserve = Math.max(0, reserve - reserveDrain());
            injecting = true;
            changed.run();
        }
        if (temperature >= SCRAM_TEMPERATURE && !overdrive) {
            scrammed = true;
        }
        if (overdrive && temperature >= MELTDOWN_TEMPERATURE) {
            if (Config.STATION_MELTDOWNS.get()) {
                meltdown(level, core, facing, centre);
                return;
            }
            toggleSafeties(false);
            scrammed = true;
        }

        // A SCRAM trips the turbine too: while the core cools, its steam goes round the turbine to the
        // condenser (a steam dump), so it makes no power and the heat it had built up is lost. Real
        // basis: a reactor trip trips the turbine, and the decay heat is dumped. (Before, the cooling
        // core kept the turbine turning, so overloading a core and letting it SCRAM cost almost
        // nothing.)
        float efficiency = StationReactor.efficiency(temperature, overdrive);
        efficiencyPermille = removed > 0 && !scrammed ? Math.round(efficiency * 1000) : 0;
        output = scrammed ? 0 : Math.round(removed * efficiency * steady.bonus(SteadyState.Tier.STATION));
        if (output > 0) {
            energy.generate(output);
        }
        turbine = generation > 0 ? Math.min(1, removed / Math.max(1, generation)) : Math.max(0, turbine - 0.01F);

        if (generation > 0) {
            burn(analysis);
            RadiationSources.emit(level, centre, (60 + generation / 300) * (unstable ? 3 : 1));
        }
        pushEnergy(level, core, facing);
        if (level.getGameTime() % 10 == 0) {
            pushLeaving(level, core, facing);
        }
        if (level.getGameTime() % 20 == 0) {
            scanPorts(level, core, facing);
        }
        if (unstable && level.getGameTime() % 3 == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, centre.x, centre.y + 2, centre.z, 6, 3, 1.5, 3, 0.03);
        }

        Status before = status;
        // An overheating core outranks a flux tilt: with no coolant it melts down in about a minute,
        // long before the tilt's countdown runs out.
        status = unstable ? Status.UNSTABLE
                : injecting ? Status.INJECTING
                : overdrive && temperature >= RUNAWAY_TEMPERATURE ? Status.RUNAWAY
                : tilting() ? Status.FLUX_TILT
                : !allowed ? Status.OFFLINE
                : scrammed ? Status.SCRAM
                : analysis.fuelRods() == 0 ? Status.NO_FUEL
                : temperature >= HOT_TEMPERATURE ? Status.OVERHEAT
                : water.isEmpty() ? Status.NO_WATER
                : temperature < WARM_TEMPERATURE ? Status.WARMING
                : overdrive ? Status.OVERDRIVE
                : Status.ONLINE;
        announce(level, centre, before);
        // The steady-state streak: running with every planned fuel channel live builds it; a stop,
        // or a channel left spent or empty, is downtime.
        boolean steadyRunning = generation > 0 && !scrammed && !fuelGap();
        int milestone = steady.tick(steadyRunning ? SteadyState.State.RUNNING : SteadyState.State.DOWN, steadyRunning ? 1 / LOAD_TICKS : 0);
        SteadyState.reached(level, centre, announcer, milestone, Milestone.STATION_STEADY);
        // Nearby clients follow the alarm closely, so it can speed up as the core heats.
        if (status != before || (status.critical() && level.getGameTime() % 5 == 0)) {
            changed.run();
        }
        // The best layout, running with every part in place: worth an advancement for whoever is near.
        if ((status == Status.ONLINE || status == Status.OVERDRIVE) && output > 0 && level.getGameTime() % 100 == 0
                && analysis.stars() == 3) {
            ModTriggers.MILESTONE.get().triggerNearby(level, centre, Milestone.STATION_PERFECT);
        }
    }

    /** Thermal FE each mB of water carries away: less in overdrive, where it boils off hotter and faster. */
    public static float heatPerWater(boolean overdrive) {
        float use = (overdrive ? OVERDRIVE_WATER_USE : WATER_USE) * Config.get(Config.STATION_WATER_USE) / 100F;
        return HEAT_PER_WATER / Math.max(0.01F, use);
    }

    /** Speaks when something worth hearing has changed since the last tick. */
    private void announce(ServerLevel level, Vec3 at, Status before) {
        boolean running = status == Status.ONLINE || status == Status.OVERDRIVE;
        boolean wasRunning = before == Status.ONLINE || before == Status.OVERDRIVE;
        // Most urgent first: a line only plays if nothing more urgent is already being said.
        if ((status == Status.RUNAWAY || status == Status.UNSTABLE) && before != Status.RUNAWAY && before != Status.UNSTABLE) {
            announcer.say(level, at, Announcer.Line.MELTDOWN_RISK);
        }
        // The reserve fires on a loss of cooling, so the control system calls it.
        if (status == Status.INJECTING && before != Status.INJECTING) {
            announcer.say(level, at, Announcer.Line.COOLANT_LOSS);
        }
        // Not on the first tick after loading a station that was already scrammed. When the player
        // caused it by re-arming the safeties, it is a controlled shutdown, not an alarm.
        boolean controlledShutdown = false;
        if (scrammed && announcedScram != null && !announcedScram) {
            controlledShutdown = plannedScram;
            announcer.say(level, at, plannedScram ? Announcer.Line.CONTROLLED_SHUTDOWN : Announcer.Line.SCRAM);
        }
        announcedScram = scrammed;
        if (!scrammed) {
            plannedScram = false;
        }
        if (status == Status.FLUX_TILT && before != Status.FLUX_TILT) {
            announcer.say(level, at, Announcer.Line.FLUX_TILT);
        }
        if (status == Status.OVERHEAT && before != Status.OVERHEAT) {
            announcer.say(level, at, Announcer.Line.OVERHEAT);
        }
        // The controlled shutdown line already says the safeties are engaged.
        if (announcedSafeties != null && announcedSafeties != safeties && !controlledShutdown) {
            announcer.say(level, at, safeties ? Announcer.Line.SAFETIES_ON : Announcer.Line.SAFETIES_OFF);
        }
        announcedSafeties = safeties;
        if (running && !wasRunning && downTicks >= RESTART_ANNOUNCE_TICKS) {
            announcer.say(level, at, Announcer.Line.STATION_ONLINE);
        }
        downTicks = running ? 0 : Math.min(downTicks + 1, RESTART_ANNOUNCE_TICKS);
    }

    /**
     * The core has run away. The station and everything in it is destroyed in a steam explosion,
     * which leaves a strong radiation site. Only reachable with the safeties off and meltdowns on.
     */
    private void meltdown(ServerLevel level, BlockPos core, Direction facing, Vec3 centre) {
        steady.reset();
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            channels.setStackInSlot(i, ItemStack.EMPTY);
        }
        // Taken apart first, so removing the parts does not search for the station again.
        StationStructure.breakApart(level, core, level.getBlockState(core));
        for (BlockPos cell : StationLayout.PARTS.keySet()) {
            BlockPos pos = StationLayout.toWorld(core, facing, cell);
            if (!pos.equals(core)) {
                level.removeBlock(pos, false);
            }
        }
        level.removeBlock(core, false);
        ModTriggers.MILESTONE.get().triggerNearby(level, centre, Milestone.STATION_MELTDOWN);
        MeltdownCrater.carve(level, BlockPos.containing(centre.x, core.getY(), centre.z), Config.get(Config.STATION_MELTDOWN_RADIUS));
        level.explode(null, centre.x, centre.y, centre.z, Config.STATION_MELTDOWN_POWER.get().floatValue(),
                true, Level.ExplosionInteraction.BLOCK);
        RadiationSources.contaminate(level, centre, MELTDOWN_RADIATION, MELTDOWN_FADE_TICKS);
    }

    /** Pushes spent rods, and anything the plan no longer takes (or all of it, while draining), out of the output port into whatever is beside it. */
    private void pushLeaving(ServerLevel level, BlockPos core, Direction facing) {
        if (draining) {
            boolean empty = true;
            for (int i = 0; i < StationReactor.CHANNELS; i++) {
                empty &= channels.getStackInSlot(i).isEmpty();
            }
            if (empty) {
                draining = false;
                changed.run();
            }
        }
        IItemHandler target = beside(level, core, facing, StationLayout.Port.OUTPUT);
        if (target == null) {
            return;
        }
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            ItemStack held = channels.getStackInSlot(i);
            if (leavingAt(i) && ItemHandlerHelper.insertItem(target, held.copy(), false).isEmpty()) {
                channels.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    /** Burns each fuel rod by how hard it runs; a rod that is used up becomes a spent rod. */
    private void burn(StationReactor.Analysis analysis) {
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            if (analysis.burn()[i] <= 0) {
                continue;
            }
            burnt[i] += analysis.burn()[i];
            int whole = (int) burnt[i];
            if (whole <= 0) {
                continue;
            }
            burnt[i] -= whole;
            ItemStack rod = channels.getStackInSlot(i);
            int left = StationReactor.fuelLeft(rod) - whole;
            if (left > 0) {
                ItemStack burning = rod.copy();
                burning.set(ModDataComponents.FUEL_LEFT.get(), left);
                channels.setStackInSlot(i, burning);
            } else {
                channels.setStackInSlot(i, StationReactor.spentFor(rod));
            }
        }
    }

    /** Pushes power out of the energy port into whatever is beside it. */
    private void pushEnergy(ServerLevel level, BlockPos core, Direction facing) {
        if (energy.getEnergyStored() <= 0) {
            return;
        }
        BlockPos port = StationLayout.portPos(core, facing, StationLayout.Port.ENERGY);
        Direction face = StationLayout.portFace(facing, StationLayout.Port.ENERGY);
        IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, port.relative(face), face.getOpposite());
        if (receiver != null && receiver.canReceive()) {
            int sent = receiver.receiveEnergy(energy.getEnergyStored(), false);
            energy.extractEnergy(sent, false);
        }
    }

    /** What the water port offers pipes: water in, nothing out. */
    final IFluidHandler waterPort = new IFluidHandler() {
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
            return stack.is(Fluids.WATER);
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

    void save(CompoundTag tag, HolderLookup.Provider registries) {
        // The uranium plan keeps the key older worlds used.
        for (StationReactor.Core of : StationReactor.Core.values()) {
            Channel[] plan = plans[of.ordinal()];
            int[] ids = new int[plan.length];
            for (int i = 0; i < plan.length; i++) {
                ids[i] = plan[i].ordinal();
            }
            tag.putIntArray(of == StationReactor.Core.URANIUM ? "plan" : "plan_mox", ids);
        }
        tag.putInt("core", core.ordinal());
        tag.put("channels", channels.serializeNBT(registries));
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putFloat("temperature", temperature);
        tag.putBoolean("scrammed", scrammed);
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("feeding", feeding);
        tag.putBoolean("draining", draining);
        tag.putInt("redstone_mode", redstoneMode.ordinal());
        tag.putBoolean("safeties", safeties);
        tag.putInt("tilt_ticks", tiltTicks);
        tag.putBoolean("unstable", unstable);
        tag.putInt("status", status.ordinal());
        tag.putInt("reserve_bays", reserveBays);
        tag.putFloat("reserve", reserve);
        steady.save(tag);
    }

    void load(CompoundTag tag, HolderLookup.Provider registries) {
        for (StationReactor.Core of : StationReactor.Core.values()) {
            int[] ids = tag.getIntArray(of == StationReactor.Core.URANIUM ? "plan" : "plan_mox");
            Channel[] plan = plans[of.ordinal()];
            for (int i = 0; i < plan.length; i++) {
                plan[i] = i < ids.length ? Channel.byId(ids[i]) : Channel.EMPTY;
                // A target rod channel, or an older world's control rod in the uranium plan: the plan
                // clears, and its rod leaves by the output port like a spent one.
                if (!plan[i].plannable(of)) {
                    plan[i] = Channel.EMPTY;
                }
            }
        }
        core = StationReactor.Core.byId(tag.getInt("core"));
        types = plans[core.ordinal()];
        channels.deserializeNBT(registries, tag.getCompound("channels"));
        water.readFromNBT(registries, tag.getCompound("water"));
        energy.set(tag.getInt("energy"));
        temperature = tag.contains("temperature") ? tag.getFloat("temperature") : AMBIENT;
        scrammed = tag.getBoolean("scrammed");
        enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        feeding = !tag.contains("feeding") || tag.getBoolean("feeding");
        draining = tag.getBoolean("draining");
        redstoneMode = RedstoneMode.byId(tag.getInt("redstone_mode"));
        safeties = !tag.contains("safeties") || tag.getBoolean("safeties");
        tiltTicks = tag.getInt("tilt_ticks");
        unstable = tag.getBoolean("unstable");
        int id = tag.getInt("status");
        status = id >= 0 && id < Status.values().length ? Status.values()[id] : Status.OFFLINE;
        reserveBays = tag.getInt("reserve_bays");
        reserve = tag.getFloat("reserve");
        steady.load(tag);
    }
}
