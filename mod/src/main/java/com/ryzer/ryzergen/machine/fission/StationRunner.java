package com.ryzer.ryzergen.machine.fission;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.advancement.Milestone;
import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.machine.fission.StationReactor.Channel;
import com.ryzer.ryzergen.radiation.RadiationSources;
import com.ryzer.ryzergen.registry.ModDataComponents;
import com.ryzer.ryzergen.registry.ModTriggers;
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
 */
public class StationRunner {
    public static final int WATER_CAPACITY = 16_000;
    public static final int ENERGY_CAPACITY = 2_000_000;
    /** Thermal FE carried away per mB of water boiled. */
    public static final float HEAT_PER_WATER = 200;
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
    /** Dose rate in mSv/s one block from the core: a meltdown site, which fades over 40 minutes. */
    private static final float MELTDOWN_RADIATION = 500;
    private static final long MELTDOWN_FADE_TICKS = 48_000;

    public enum Status {
        // New statuses go at the end: the status is saved by its position in this list.
        OFFLINE, NO_FUEL, WARMING, ONLINE, NO_WATER, OVERHEAT, SCRAM, OVERDRIVE, FLUX_TILT, UNSTABLE, RUNAWAY;

        /** Whether the alarm sounds. */
        public boolean alarm() {
            return this == FLUX_TILT || this == UNSTABLE || this == RUNAWAY;
        }

        /** Heading for meltdown, so the alarm climbs with the core's temperature. */
        public boolean critical() {
            return this == UNSTABLE || this == RUNAWAY;
        }
    }

    private final Runnable changed;
    final Channel[] types = new Channel[StationReactor.CHANNELS];
    private final float[] burnt = new float[StationReactor.CHANNELS];

    final ItemStackHandler channels = new ItemStackHandler(StationReactor.CHANNELS) {
        @Override
        protected void onContentsChanged(int slot) {
            changed.run();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return types[slot].accepts(stack);
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
            return types[slot].accepts(stack) ? channels.insertItem(slot, stack, simulate) : stack;
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
            return types[slot].accepts(stack);
        }
    };

    /**
     * The output port: spent rods come out (pushed into whatever is beside it, or pulled by a pipe);
     * nothing goes in. Waste from later machines will leave the same way.
     */
    final IItemHandler outputPort = new IItemHandler() {
        @Override
        public int getSlots() {
            return StationReactor.CHANNELS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemStack held = channels.getStackInSlot(slot);
            return StationReactor.isSpent(held) ? held : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return StationReactor.isSpent(channels.getStackInSlot(slot)) ? channels.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
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
    int output;
    int efficiencyPermille;
    /** How hard the turbine is turning, 0 to 1, for the renderer. */
    float turbine;

    StationRunner(Runnable changed) {
        java.util.Arrays.fill(types, Channel.EMPTY);
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

    /**
     * The safety switch. Disarming is the player's choice; re-arming SCRAMs the core at once, which
     * is also the only way to save one gone unstable.
     */
    void toggleSafeties() {
        safeties = !safeties;
        if (safeties) {
            if (unstable || temperature >= RESET_TEMPERATURE) {
                scrammed = true;
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
     * Right-click on a channel: takes out whatever is in it, or, if it is empty, clears its plan.
     * Returns what came out, for the player.
     */
    ItemStack clear(int channel) {
        ItemStack held = channels.getStackInSlot(channel);
        if (!held.isEmpty()) {
            channels.setStackInSlot(channel, ItemStack.EMPTY);
        } else {
            types[channel] = Channel.EMPTY;
        }
        changed.run();
        return held;
    }

    /** Sets a channel's plan. Whatever no longer fits comes back out, for the player to take. */
    ItemStack setType(int channel, Channel type) {
        types[channel] = type;
        ItemStack held = channels.getStackInSlot(channel);
        if (!held.isEmpty() && !type.accepts(held) && !(type == Channel.FUEL && StationReactor.isSpent(held))) {
            channels.setStackInSlot(channel, ItemStack.EMPTY);
            changed.run();
            return held;
        }
        changed.run();
        return ItemStack.EMPTY;
    }

    /** One tick. {@code centre} is the middle of the station, for radiation. */
    void tick(ServerLevel level, BlockPos core, Direction facing, Vec3 centre) {
        boolean overdrive = !safeties;
        StationReactor.Analysis analysis = StationReactor.analyse(types, rods(), overdrive);
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
                    toggleSafeties();
                    scrammed = true;
                    overdrive = false;
                }
            }
        } else if (!unstable) {
            tiltTicks = 0;
        }

        // Coolant carries heat away as steam, as far as its channels, the core's temperature and the
        // water allow. Heat no coolant can reach (see StationReactor) stays and warms the core. With
        // the reaction stopped, every coolant channel works on cooling the core down.
        float stored = (temperature - AMBIENT) * HEAT_CAPACITY;
        float byWater = water.getFluidAmount() * HEAT_PER_WATER;
        float carryable = generation > 0 ? Math.max(0, generation - analysis.stranded()) : 0;
        int working = generation > 0 && analysis.working() > 0 ? analysis.working() : analysis.coolants();
        float potential = working * analysis.capacity() * StationReactor.coolingRamp(temperature);
        float removed = Math.max(0, Math.min(potential, Math.min(carryable + stored, byWater)));
        if (removed > 0) {
            water.drain((int) Math.ceil(removed / HEAT_PER_WATER), IFluidHandler.FluidAction.EXECUTE);
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
        if (temperature >= SCRAM_TEMPERATURE && !overdrive) {
            scrammed = true;
        }
        if (overdrive && temperature >= MELTDOWN_TEMPERATURE) {
            if (Config.STATION_MELTDOWNS.get()) {
                meltdown(level, core, facing, centre);
                return;
            }
            toggleSafeties();
            scrammed = true;
        }

        float efficiency = StationReactor.efficiency(temperature, overdrive);
        efficiencyPermille = removed > 0 ? Math.round(efficiency * 1000) : 0;
        output = Math.round(removed * efficiency);
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
            pushSpent(level, core, facing);
        }
        if (unstable && level.getGameTime() % 3 == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, centre.x, centre.y + 2, centre.z, 6, 3, 1.5, 3, 0.03);
        }

        Status before = status;
        // An overheating core outranks a flux tilt: with no coolant it melts down in about a minute,
        // long before the tilt's countdown runs out.
        status = unstable ? Status.UNSTABLE
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
        // Nearby clients follow the alarm closely, so it can speed up as the core heats.
        if (status != before || (status.critical() && level.getGameTime() % 5 == 0)) {
            changed.run();
        }
        // A layout as good as the best known, running: worth an advancement for whoever is near.
        if ((status == Status.ONLINE || status == Status.OVERDRIVE) && output > 0 && level.getGameTime() % 100 == 0
                && analysis.rating() >= 0.999F) {
            ModTriggers.MILESTONE.get().triggerNearby(level, centre, Milestone.STATION_PERFECT);
        }
    }

    /**
     * The core has run away. The station and everything in it is destroyed in a steam explosion,
     * which leaves a strong radiation site. Only reachable with the safeties off and meltdowns on.
     */
    private void meltdown(ServerLevel level, BlockPos core, Direction facing, Vec3 centre) {
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

    /** Pushes spent rods out of the output port into whatever is beside it. */
    private void pushSpent(ServerLevel level, BlockPos core, Direction facing) {
        BlockPos port = StationLayout.portPos(core, facing, StationLayout.Port.OUTPUT);
        Direction face = StationLayout.portFace(facing, StationLayout.Port.OUTPUT);
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, port.relative(face), face.getOpposite());
        if (target == null) {
            return;
        }
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            ItemStack spent = channels.getStackInSlot(i);
            if (!StationReactor.isSpent(spent)) {
                continue;
            }
            ItemStack left = net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(target, spent.copy(), false);
            if (left.isEmpty()) {
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

    /** Pushes power out of the energy port into whatever is in front of it. */
    private void pushEnergy(ServerLevel level, BlockPos core, Direction facing) {
        if (energy.getEnergyStored() <= 0) {
            return;
        }
        BlockPos port = StationLayout.portPos(core, facing, StationLayout.Port.ENERGY);
        IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, port.relative(facing), facing.getOpposite());
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
        int[] plan = new int[types.length];
        for (int i = 0; i < types.length; i++) {
            plan[i] = types[i].ordinal();
        }
        tag.putIntArray("plan", plan);
        tag.put("channels", channels.serializeNBT(registries));
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putFloat("temperature", temperature);
        tag.putBoolean("scrammed", scrammed);
        tag.putBoolean("enabled", enabled);
        tag.putInt("redstone_mode", redstoneMode.ordinal());
        tag.putBoolean("safeties", safeties);
        tag.putInt("tilt_ticks", tiltTicks);
        tag.putBoolean("unstable", unstable);
        tag.putInt("status", status.ordinal());
    }

    void load(CompoundTag tag, HolderLookup.Provider registries) {
        int[] plan = tag.getIntArray("plan");
        for (int i = 0; i < types.length; i++) {
            types[i] = i < plan.length ? Channel.byId(plan[i]) : Channel.EMPTY;
        }
        channels.deserializeNBT(registries, tag.getCompound("channels"));
        water.readFromNBT(registries, tag.getCompound("water"));
        energy.set(tag.getInt("energy"));
        temperature = tag.contains("temperature") ? tag.getFloat("temperature") : AMBIENT;
        scrammed = tag.getBoolean("scrammed");
        enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        redstoneMode = RedstoneMode.byId(tag.getInt("redstone_mode"));
        safeties = !tag.contains("safeties") || tag.getBoolean("safeties");
        tiltTicks = tag.getInt("tilt_ticks");
        unstable = tag.getBoolean("unstable");
        int id = tag.getInt("status");
        status = id >= 0 && id < Status.values().length ? Status.values()[id] : Status.OFFLINE;
    }
}
