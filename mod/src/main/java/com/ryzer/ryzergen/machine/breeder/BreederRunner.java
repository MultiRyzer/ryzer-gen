package com.ryzer.ryzergen.machine.breeder;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.machine.Announcer;
import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.machine.breeder.BreederReactor.Position;
import com.ryzer.ryzergen.machine.fission.StationReactor;
import com.ryzer.ryzergen.radiation.RadiationSources;
import com.ryzer.ryzergen.registry.ModDataComponents;
import com.ryzer.ryzergen.registry.ModFluids;
import com.ryzer.ryzergen.machine.SteadyState;
import com.ryzer.ryzergen.advancement.Milestone;
import com.ryzer.ryzergen.registry.ModTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
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
 * A formed breeder at work: the 19 core positions (their plan and what is in them), the sodium
 * loop, the pumps' flow, the energy buffer and the core's temperature. Each tick the fuel makes heat
 * ({@link BreederReactor}), the sodium carries it to the steam plant, which makes power (more
 * efficiently the hotter the core), the pumps take their share, the fuel burns down and the
 * blankets breed.
 *
 * <p>The loop is closed: its sodium is never used up, but it must be filled before the reactor will
 * start, and a loop less than full carries less heat in step. If the sodium cannot keep up (too
 * little flow, a part-filled loop, or an assembly over the hot spot limit) the core heats; at 650°C the automatic SCRAM drops the control
 * rods until it is below 450°C. Real basis: a sodium-cooled fast reactor and its scram.
 *
 * <p>A core that stops still makes decay heat, which only the flow carries away, and the pumps need
 * power: the reactor's own, or its buffer's, half of which is kept back from the energy port for
 * them. With the flow stopped, a strong core's decay heat drives the sodium past its boiling point
 * (883°C): it bursts a seal and burns inside the bund wall for 30 seconds, and the loop is lost
 * (design section 9, the sodium fire).
 */
public class BreederRunner {
    public static final int ENERGY_CAPACITY = 4_000_000;
    /** Thermal FE to warm the core by one degree: a pool of sodium holds a lot of heat. */
    private static final float HEAT_CAPACITY = 40_000;
    public static final float AMBIENT = 20;
    public static final float HOT_TEMPERATURE = 600;
    public static final float SCRAM_TEMPERATURE = 650;
    private static final float RESET_TEMPERATURE = 450;
    /** Sodium boils here: a stopped loop past it bursts a seal, and the sodium fire starts. */
    public static final float SODIUM_BOIL = 883;
    /** Decay heat at shutdown, as a share of what the core was making. */
    private static final float DECAY_SHARE = 0.07F;
    /** Decay heat halves every 2 minutes. */
    private static final float DECAY_PER_TICK = (float) Math.pow(0.5, 1 / 2400.0);
    /** The core's slow loss of heat to the building round it, a share of its heat over ambient a tick. */
    private static final float PASSIVE_LOSS = 0.00002F;
    /** How long the sodium burns, in ticks. */
    private static final int FIRE_TICKS = 600;
    /** A just-burnt core: its boiling sodium carried the heat off as it burst. */
    private static final float AFTER_FIRE_TEMPERATURE = 400;
    /** Ticks a breeder must have been down before starting again is announced. */
    private static final int RESTART_ANNOUNCE_TICKS = 100;
    /** Flow steps on the control screen, in percent. */
    public static final int FLOW_STEP = 5;

    public enum Status {
        // New statuses go at the end: the status is saved by its position in this list.
        OFFLINE, NO_FUEL, NO_SODIUM, WARMING, ONLINE, OVERHEAT, SCRAM, SODIUM_FIRE;

        /** Whether the reactor is making heat. */
        public boolean running() {
            return this == WARMING || this == ONLINE || this == OVERHEAT;
        }
    }

    private final Runnable changed;
    final Position[] types = new Position[BreederReactor.POSITIONS];
    private final float[] burnt = new float[BreederReactor.POSITIONS];
    /** Breeding work done on each blanket so far, below the thousand that make one unit on the item. */
    private final float[] bredPart = new float[BreederReactor.POSITIONS];

    final ItemStackHandler positions = new ItemStackHandler(BreederReactor.POSITIONS) {
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

    final FluidTank sodium = new FluidTank(BreederReactor.LOOP, stack -> stack.is(ModFluids.SODIUM_TAG)) {
        @Override
        protected void onContentsChanged() {
            changed.run();
        }
    };

    /** The energy buffer: outside callers can only take from it; the steam plant fills it. */
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

    /** The fuel port: anything a position is planned for goes in (fuel, control rods, blankets). Nothing comes out. */
    final IItemHandler fuelPort = new IItemHandler() {
        @Override
        public int getSlots() {
            return BreederReactor.POSITIONS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return positions.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return types[slot].accepts(stack) ? positions.insertItem(slot, stack, simulate) : stack;
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

    /** The output port: spent fuel and bred blankets come out (pushed beside it, or pulled by a pipe); nothing goes in. */
    final IItemHandler outputPort = new IItemHandler() {
        @Override
        public int getSlots() {
            return BreederReactor.POSITIONS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemStack held = positions.getStackInSlot(slot);
            return BreederReactor.isFinished(held) ? held : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return BreederReactor.isFinished(positions.getStackInSlot(slot)) ? positions.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
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

    /** The sodium port: liquid sodium in, to fill the loop; nothing out. */
    final IFluidHandler sodiumPort = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return sodium.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return BreederReactor.LOOP;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.is(ModFluids.SODIUM_TAG);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return sodium.fill(resource, action);
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

    private final Announcer announcer = new Announcer();
    final SteadyState steady = new SteadyState();
    /** One load: an assembly's life, an hour at full output. */
    private static final float LOAD_TICKS = 72_000;
    private Boolean announcedScram;
    private boolean announcedHotSpot;
    private int downTicks = RESTART_ANNOUNCE_TICKS;
    /** Decay heat still coming off a core that has stopped, in thermal FE a tick. */
    private float decayHeat;
    /** What the core was making last tick, to start its decay heat from when it stops. */
    private float lastGeneration;
    /** Ticks the sodium fire still has to burn. */
    int fireTicks;
    boolean enabled = true;
    RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    /** The pumps' flow, in percent. */
    int flow = 80;
    float temperature = AMBIENT;
    private boolean scrammed;
    Status status = Status.OFFLINE;
    int output;
    int pumping;
    int efficiencyPermille;

    /** Set each tick by the core from its sound switch: muted, the fire's roar is not played and the announcer keeps quiet. */
    boolean quiet;

    BreederRunner(Runnable changed) {
        java.util.Arrays.fill(types, Position.EMPTY);
        this.changed = changed;
    }

    public IEnergyStorage energy() {
        return energy;
    }

    public IItemHandler fuelPort() {
        return fuelPort;
    }

    public IItemHandler outputPort() {
        return outputPort;
    }

    public IFluidHandler sodiumPort() {
        return sodiumPort;
    }

    public Status status() {
        return status;
    }

    public float flow() {
        return flow / 100F;
    }

    public ItemStack[] items() {
        ItemStack[] items = new ItemStack[BreederReactor.POSITIONS];
        for (int i = 0; i < items.length; i++) {
            items[i] = positions.getStackInSlot(i);
        }
        return items;
    }

    /** Right-click on a position: takes out whatever is in it, or, if it is empty, clears its plan. */
    ItemStack clear(int position) {
        ItemStack held = positions.getStackInSlot(position);
        if (!held.isEmpty()) {
            positions.setStackInSlot(position, ItemStack.EMPTY);
        } else {
            types[position] = Position.EMPTY;
        }
        changed.run();
        return held;
    }

    /** Sets a position's plan. Whatever no longer fits comes back out, for the player to take. */
    ItemStack setType(int position, Position type) {
        types[position] = type;
        ItemStack held = positions.getStackInSlot(position);
        if (!held.isEmpty() && !type.accepts(held) && !BreederReactor.isFinished(held)) {
            positions.setStackInSlot(position, ItemStack.EMPTY);
            changed.run();
            return held;
        }
        changed.run();
        return ItemStack.EMPTY;
    }

    void setFlow(int percent) {
        flow = Math.max(0, Math.min(100, percent));
        changed.run();
    }

    /** One tick. {@code centre} is the middle of the reactor, for radiation and the sodium fire. */
    void tick(ServerLevel level, BlockPos core, Direction facing, Vec3 centre) {
        announcer.mute(quiet);
        BreederReactor.Analysis analysis = BreederReactor.analyse(types, items());
        boolean allowed = enabled && redstoneMode.allows(level.hasNeighborSignal(core));
        boolean filled = sodium.getFluidAmount() >= BreederReactor.LOOP;
        if (scrammed && temperature < RESET_TEMPERATURE && fireTicks <= 0) {
            scrammed = false;
        }
        // The reactor will not start on a loop that is not full; once running, a loop that loses
        // sodium carries less, and the core heats.
        float generation = allowed && !scrammed && (filled || status.running()) ? analysis.generation() : 0;
        // A core that stops keeps making decay heat, which fades over minutes.
        if (generation > 0) {
            decayHeat = 0;
        } else if (lastGeneration > 0) {
            decayHeat = lastGeneration * DECAY_SHARE;
        }
        lastGeneration = generation;
        decayHeat *= DECAY_PER_TICK;

        // A core making heat always drives its pumps (the steam plant, or its startup supply while it
        // warms). A stopped one runs them from its buffer, and with that empty they stall.
        float wanted = flow > 0 && sodium.getFluidAmount() > 0 ? BreederReactor.pumpPower(flow()) : 0;
        boolean pumped = wanted <= 0 || generation > 0 || energy.getEnergyStored() >= wanted;
        float flowing = pumped ? flow() : 0;

        // The sodium carries away what the loop and the core's temperature allow: the heat made, less
        // any over a hot spot's limit (which stays in the core), and with the reaction stopped,
        // whatever heat the core still holds; decay heat on top.
        float stored = (temperature - AMBIENT) * HEAT_CAPACITY;
        float capacity = BreederReactor.capacity(flowing, sodium.getFluidAmount());
        float potential = capacity * BreederReactor.coolingRamp(temperature);
        float carryable = (generation > 0 ? Math.max(0, generation - analysis.stranded()) : stored) + decayHeat;
        float removed = Math.max(0, Math.min(potential, carryable));
        temperature += (generation + decayHeat - removed) / HEAT_CAPACITY;
        temperature -= (temperature - AMBIENT) * PASSIVE_LOSS;
        temperature = Math.max(AMBIENT, temperature);
        if (temperature >= SCRAM_TEMPERATURE) {
            scrammed = true;
        }
        // Past sodium's boiling point the loop bursts: the sodium fire.
        if (temperature >= SODIUM_BOIL && sodium.getFluidAmount() > 0) {
            if (Config.get(Config.BREEDER_SODIUM_FIRE)) {
                ignite(level, centre);
            } else {
                temperature = SODIUM_BOIL;
            }
        }
        if (fireTicks > 0) {
            burnFire(level, core, centre);
        }

        // The steam plant makes power from what the sodium carries; the pumps take theirs first. A
        // SCRAM trips the steam plant too, as the station's turbine trips.
        float efficiency = BreederReactor.efficiency(temperature);
        pumping = pumped ? Math.round(wanted) : 0;
        int made = scrammed ? 0 : Math.round(removed * efficiency * steady.bonus(SteadyState.Tier.BREEDER));
        output = made - pumping;
        efficiencyPermille = removed > 0 && !scrammed ? Math.round(efficiency * 1000) : 0;
        if (output > 0) {
            energy.generate(output);
        } else if (output < 0) {
            // The pumps run from the buffer when the reactor is not making enough to drive them.
            energy.extractEnergy(-output, false);
        }

        if (generation > 0) {
            burn(analysis);
            breed(analysis);
            RadiationSources.emit(level, centre, 80 + generation / 1500);
        }
        pushEnergy(level, core, facing);
        if (level.getGameTime() % 10 == 0) {
            pushFinished(level, core, facing);
        }

        Status before = status;
        status = fireTicks > 0 ? Status.SODIUM_FIRE
                : !allowed ? Status.OFFLINE
                : scrammed ? Status.SCRAM
                : analysis.fuel() == 0 ? Status.NO_FUEL
                : generation <= 0 ? Status.NO_SODIUM
                : temperature >= HOT_TEMPERATURE ? Status.OVERHEAT
                : temperature < BreederReactor.IDLE_TEMPERATURE ? Status.WARMING
                : Status.ONLINE;
        announce(level, centre, before, generation > 0 && analysis.stranded() > 0);
        int milestone = steady.tick(generation > 0 ? SteadyState.State.RUNNING : SteadyState.State.DOWN, generation > 0 ? 1 / LOAD_TICKS : 0);
        SteadyState.reached(level, centre, announcer, milestone, Milestone.BREEDER_STEADY);
        if (status != before) {
            changed.run();
        }
    }

    /** The loop bursts: the sodium is lost, the core SCRAMs, and the fire starts. */
    private void ignite(ServerLevel level, Vec3 centre) {
        sodium.setFluid(FluidStack.EMPTY);
        scrammed = true;
        decayHeat = 0;
        temperature = AFTER_FIRE_TEMPERATURE;
        fireTicks = FIRE_TICKS;
        steady.reset();
        ModTriggers.MILESTONE.get().triggerNearby(level, centre, Milestone.SODIUM_FIRE);
        changed.run();
    }

    /**
     * The sodium burning inside the bund wall: dense white smoke (sodium oxide) and flames over the
     * compound, and anyone inside it set alight. No blocks burn: the bund holds it.
     */
    private void burnFire(ServerLevel level, BlockPos core, Vec3 centre) {
        fireTicks--;
        double half = BreederLayout.SIZE / 2.0;
        AABB compound = new AABB(centre.x - half, core.getY(), centre.z - half, centre.x + half, core.getY() + 3, centre.z + half);
        if (level.getGameTime() % 10 == 0) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, compound)) {
                entity.igniteForSeconds(4);
            }
        }
        RandomSource random = level.random;
        for (int i = 0; i < 4; i++) {
            double x = centre.x + (random.nextDouble() - 0.5) * (BreederLayout.SIZE - 2);
            double z = centre.z + (random.nextDouble() - 0.5) * (BreederLayout.SIZE - 2);
            level.sendParticles(ParticleTypes.FLAME, x, core.getY() + 0.2, z, 2, 0.2, 0.1, 0.2, 0.02);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, core.getY() + 0.5, z, 1, 0.3, 0.2, 0.3, 0.01);
        }
        if (level.getGameTime() % 5 == 0) {
            level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, centre.x, core.getY() + 1, centre.z, 2, 2.5, 0.5, 2.5, 0.01);
        }
        if (!quiet && level.getGameTime() % 30 == 0) {
            level.playSound(null, centre.x, core.getY() + 1, centre.z, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 3.0F, 0.8F);
        }
    }

    /** Speaks when something worth hearing has changed since the last tick, most urgent first. */
    private void announce(ServerLevel level, Vec3 at, Status before, boolean hotSpot) {
        if (status == Status.SODIUM_FIRE && before != Status.SODIUM_FIRE) {
            announcer.say(level, at, Announcer.Line.SODIUM_FIRE);
        }
        // Not on the first tick after loading a breeder that was already scrammed.
        if (scrammed && announcedScram != null && !announcedScram && status != Status.SODIUM_FIRE) {
            announcer.say(level, at, Announcer.Line.SCRAM);
        }
        announcedScram = scrammed;
        if (status == Status.OVERHEAT && before != Status.OVERHEAT) {
            announcer.say(level, at, Announcer.Line.OVERHEAT);
        }
        if (hotSpot && !announcedHotSpot) {
            announcer.say(level, at, Announcer.Line.HOT_SPOT);
        }
        announcedHotSpot = hotSpot;
        if (status == Status.NO_SODIUM && before != Status.NO_SODIUM) {
            announcer.say(level, at, Announcer.Line.SODIUM_LOW);
        }
        if (status.running() && !before.running() && downTicks >= RESTART_ANNOUNCE_TICKS) {
            announcer.say(level, at, Announcer.Line.BREEDER_ONLINE);
        }
        downTicks = status.running() ? 0 : Math.min(downTicks + 1, RESTART_ANNOUNCE_TICKS);
    }

    /** Burns each assembly by how hard it runs; one that is used up becomes spent breeder fuel. */
    private void burn(BreederReactor.Analysis analysis) {
        for (int i = 0; i < BreederReactor.POSITIONS; i++) {
            if (analysis.burn()[i] <= 0) {
                continue;
            }
            burnt[i] += analysis.burn()[i];
            int whole = (int) burnt[i];
            if (whole <= 0) {
                continue;
            }
            burnt[i] -= whole;
            ItemStack fuel = positions.getStackInSlot(i);
            int left = BreederReactor.fuelLeft(fuel) - whole;
            if (left > 0) {
                ItemStack burning = fuel.copy();
                burning.set(ModDataComponents.FUEL_LEFT.get(), left);
                positions.setStackInSlot(i, burning);
            } else {
                positions.setStackInSlot(i, BreederReactor.spent());
            }
        }
    }

    /** Breeds each blanket by the fuel beside it; one that is done becomes its bred product. */
    private void breed(BreederReactor.Analysis analysis) {
        for (int i = 0; i < BreederReactor.POSITIONS; i++) {
            if (analysis.breed()[i] <= 0) {
                continue;
            }
            bredPart[i] += analysis.breed()[i];
            int whole = (int) (bredPart[i] / 1000);
            if (whole <= 0) {
                continue;
            }
            bredPart[i] -= whole * 1000F;
            ItemStack blanket = positions.getStackInSlot(i);
            int bred = blanket.getOrDefault(ModDataComponents.BRED.get(), 0) + whole;
            if (bred < StationReactor.TARGET_WORK) {
                ItemStack breeding = blanket.copy();
                breeding.set(ModDataComponents.BRED.get(), bred);
                positions.setStackInSlot(i, breeding);
            } else {
                positions.setStackInSlot(i, BreederReactor.bred(types[i]));
            }
        }
    }

    /** Pushes spent fuel and bred blankets out of the output port into whatever is beside it. */
    private void pushFinished(ServerLevel level, BlockPos core, Direction facing) {
        BlockPos port = BreederLayout.portPos(core, facing, BreederLayout.Port.OUTPUT);
        Direction face = BreederLayout.portFace(facing, BreederLayout.Port.OUTPUT);
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, port.relative(face), face.getOpposite());
        if (target == null) {
            return;
        }
        for (int i = 0; i < BreederReactor.POSITIONS; i++) {
            ItemStack done = positions.getStackInSlot(i);
            if (!BreederReactor.isFinished(done)) {
                continue;
            }
            if (ItemHandlerHelper.insertItem(target, done.copy(), false).isEmpty()) {
                positions.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    /** Pushes power out of the energy port into whatever is beside it, keeping half the buffer back for the pumps. */
    private void pushEnergy(ServerLevel level, BlockPos core, Direction facing) {
        int spare = energy.getEnergyStored() - ENERGY_CAPACITY / 2;
        if (spare <= 0) {
            return;
        }
        BlockPos port = BreederLayout.portPos(core, facing, BreederLayout.Port.ENERGY);
        Direction face = BreederLayout.portFace(facing, BreederLayout.Port.ENERGY);
        IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, port.relative(face), face.getOpposite());
        if (receiver != null && receiver.canReceive()) {
            int sent = receiver.receiveEnergy(spare, false);
            energy.extractEnergy(sent, false);
        }
    }

    void save(CompoundTag tag, HolderLookup.Provider registries) {
        int[] plan = new int[types.length];
        for (int i = 0; i < types.length; i++) {
            plan[i] = types[i].ordinal();
        }
        tag.putIntArray("plan", plan);
        tag.put("positions", positions.serializeNBT(registries));
        tag.put("sodium", sodium.writeToNBT(registries, new CompoundTag()));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putFloat("temperature", temperature);
        tag.putBoolean("scrammed", scrammed);
        tag.putBoolean("enabled", enabled);
        tag.putInt("redstone_mode", redstoneMode.ordinal());
        tag.putInt("flow", flow);
        tag.putInt("status", status.ordinal());
        tag.putFloat("decay_heat", decayHeat);
        tag.putFloat("last_generation", lastGeneration);
        tag.putInt("fire_ticks", fireTicks);
        steady.save(tag);
    }

    void load(CompoundTag tag, HolderLookup.Provider registries) {
        int[] plan = tag.getIntArray("plan");
        for (int i = 0; i < types.length; i++) {
            types[i] = i < plan.length ? Position.byId(plan[i]) : Position.EMPTY;
        }
        positions.deserializeNBT(registries, tag.getCompound("positions"));
        sodium.readFromNBT(registries, tag.getCompound("sodium"));
        energy.set(tag.getInt("energy"));
        temperature = tag.contains("temperature") ? tag.getFloat("temperature") : AMBIENT;
        scrammed = tag.getBoolean("scrammed");
        enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        redstoneMode = RedstoneMode.byId(tag.getInt("redstone_mode"));
        flow = tag.contains("flow") ? tag.getInt("flow") : 80;
        int id = tag.getInt("status");
        status = id >= 0 && id < Status.values().length ? Status.values()[id] : Status.OFFLINE;
        decayHeat = tag.getFloat("decay_heat");
        lastGeneration = tag.getFloat("last_generation");
        fireTicks = tag.getInt("fire_ticks");
        steady.load(tag);
    }
}
