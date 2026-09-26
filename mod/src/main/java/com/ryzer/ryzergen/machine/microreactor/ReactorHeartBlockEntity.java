package com.ryzer.ryzergen.machine.microreactor;

import com.ryzer.ryzergen.cable.CableBlock;
import com.ryzer.ryzergen.cable.CableSide;
import com.ryzer.ryzergen.cable.GasPipeBlock;
import com.ryzer.ryzergen.advancement.Milestone;
import com.ryzer.ryzergen.registry.ModTriggers;
import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.radiation.RadiationSources;
import com.ryzer.ryzergen.material.ModTags;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import com.ryzer.ryzergen.registry.ModFluids;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The microreactor's brain: fuel core slot, energy buffer, coolant tank, heat and controls. The heart
 * can sit in any slot, so the ports and the GUI on the other blocks find it through
 * {@link MicroreactorStructure#findHeart}.
 *
 * <p>Heat model: the core makes a fixed thermal power, and a heat engine turns part of it into FE.
 * Its efficiency is a fraction of the Carnot limit, 1 - T_cold / T_hot (in kelvin). Water in the
 * coolant tank gives a cold side near ambient; dry, the jacket can only shed heat to hot air, so the
 * cold side sits at about 250°C and efficiency drops. A cold core converts almost nothing, so output
 * ramps up as it heats.
 *
 * <p>When the buffer is full the reactor either follows the load (stands by and saves fuel) or, in
 * dump mode, keeps running and vents the surplus as steam through the steam outlet, so it keeps
 * burning fuel and making depleted cores and by-products. Real basis: the steam dump (turbine bypass)
 * valves that let a pressurised-water reactor run on when the turbine cannot take its power.
 *
 * <p>Safety override (overdrive): with the interlocks off the control rods come further out, for 30%
 * more fission power and a hotter core. The steam leaves superheated, so the turbine gets much closer
 * to the Carnot limit (45% overall, like a supercritical power station), at the cost of double water
 * use and faster fuel burn. Lose the water in overdrive and nothing stops the heat: the core runs away and, if the
 * config allows meltdowns, explodes. Real basis: a loss-of-coolant accident with the automatic scram
 * disabled.
 */
public class ReactorHeartBlockEntity extends BlockEntity implements MenuProvider {
    // Placeholder balance until the energy scale is settled (see OPEN-QUESTIONS.md).
    public static final int ENERGY_CAPACITY = 100_000;
    public static final int MAX_OUTPUT = 1_000;
    public static final int COOLANT_CAPACITY = 4_000;
    public static final int STEAM_CAPACITY = 8_000;
    public static final int AMBIENT_TEMPERATURE = 20;
    public static final int MAX_TEMPERATURE = 1_000;
    public static final int COOLED_TEMPERATURE = 450;
    public static final int UNCOOLED_TEMPERATURE = 700;
    public static final int OVERDRIVE_TEMPERATURE = 650;
    /** Output reaches full strength from this core temperature up. */
    private static final int WARM_TEMPERATURE = 400;
    /** Where a dry core in overdrive is heading. It melts down on the way, at MAX_TEMPERATURE. */
    private static final int RUNAWAY_TEMPERATURE = 1_200;
    /** Cold side of the heat engine: water near ambient, or hot air through a dry jacket. */
    private static final double WATER_SINK = 20;
    private static final double DRY_SINK = 250;
    /** Share of the Carnot limit a real turbine and generator reach, and with superheated steam. */
    private static final double ENGINE_FRACTION = 0.55;
    private static final double SUPERHEATED_ENGINE_FRACTION = 0.66;
    /** Dry efficiency at operating temperature: the config output is set against this. */
    private static final double REFERENCE_EFFICIENCY = efficiency(UNCOOLED_TEMPERATURE, false, false);
    private static final double OVERDRIVE_POWER = 1.3;
    /** Fuel burn per tick in tenths, normal and in overdrive. */
    private static final int BURN = 10;
    private static final int OVERDRIVE_BURN = 13;
    /** The core's burn time is written back to the item this often, so the GUI and item stay in step. */
    private static final int FUEL_SYNC_TICKS = 20;

    /** Dose rate in mSv/s one block from the core (see Radiation): running, overdrive, runaway, meltdown site. */
    public static final float RUNNING_RADIATION = 20;
    public static final float OVERDRIVE_RADIATION = 50;
    public static final float COOLANT_LOSS_RADIATION = 120;
    public static final float MELTDOWN_RADIATION = 250;
    /** A meltdown site fades over 20 minutes. */
    public static final long MELTDOWN_FADE_TICKS = 24_000;

    /** What the readout screen says. */
    public enum Status {
        ONLINE, OFFLINE, HALTED, NO_CORE, DEPLETED, STANDBY, OVERDRIVE, COOLANT_LOSS, DUMPING;

        public boolean running() {
            return this == ONLINE || this == OVERDRIVE || this == COOLANT_LOSS || this == DUMPING;
        }

        public boolean overdrive() {
            return this == OVERDRIVE || this == COOLANT_LOSS;
        }
    }

    // Synced to the open GUI. Values travel as shorts, so energy is split into two halves.
    public static final int DATA_ENERGY_LOW = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_COOLANT = 2;
    public static final int DATA_TEMPERATURE = 3;
    public static final int DATA_OUTPUT = 4;
    public static final int DATA_ENABLED = 5;
    public static final int DATA_REDSTONE = 6;
    public static final int DATA_FUEL = 7;
    public static final int DATA_STATUS = 8;
    /** Heat-to-power efficiency in tenths of a percent. */
    public static final int DATA_EFFICIENCY = 9;
    public static final int DATA_SAFETIES = 10;
    public static final int DATA_DUMP = 11;
    public static final int DATA_COUNT = 12;

    /** Extract only from outside; the reactor fills it through {@link Buffer#generate}. */
    private final class Buffer extends EnergyStorage {
        Buffer() {
            super(ENERGY_CAPACITY, 0, MAX_OUTPUT);
        }

        void generate(int amount) {
            energy = Math.min(capacity, energy + amount);
            setChanged();
        }

        boolean isFull() {
            return energy >= capacity;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int extracted = super.extractEnergy(toExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
            }
            return extracted;
        }
    }

    private final Buffer energy = new Buffer();

    private final FluidTank coolant = new FluidTank(COOLANT_CAPACITY, stack -> stack.is(Tags.Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    /**
     * The steam the boiling coolant makes, waiting at the steam outlet. The port pushes it into
     * whatever is connected (a gas pipe, a pressure tank); with nothing there, or the buffer full, it
     * simply escapes, as it always has.
     */
    private final FluidTank steam = new FluidTank(STEAM_CAPACITY, stack -> stack.is(ModFluids.STEAM.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    /** What pipes see at the steam outlet: they can drain it but never fill it. */
    private final IFluidHandler steamOutput = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return steam.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return steam.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return steam.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return steam.drain(maxDrain, action);
        }
    };

    private final ItemStackHandler fuel = createFuelSlot(this::setChanged);
    /** What pipes see at the fuel inlet: fresh cores go in when the slot is free; nothing comes out. */
    private final IItemHandler fuelInput = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return fuel.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack.is(ModTags.MICROREACTOR_FUEL) ? fuel.insertItem(slot, stack, simulate) : stack;
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
            return stack.is(ModTags.MICROREACTOR_FUEL);
        }
    };

    /**
     * What pipes see at the spent-core outlet: only a spent core, and only coming out, so automation
     * never pulls a core with fuel left in it, and nothing can be put in.
     */
    private final IItemHandler fuelOutput = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemStack core = fuel.getStackInSlot(slot);
            return core.is(ModItems.DEPLETED_FUEL_CORE.get()) ? core : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return fuel.getStackInSlot(slot).is(ModItems.DEPLETED_FUEL_CORE.get()) ? fuel.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
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

    /** What pipes see at the coolant port: they can fill it but never drain it. */
    private final IFluidHandler coolantInput = new IFluidHandler() {
        @Override
        public int getTanks() {
            return coolant.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return coolant.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return coolant.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return coolant.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return coolant.fill(resource, action);
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

    private boolean enabled = true;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    private Status status = Status.NO_CORE;
    private int temperature = AMBIENT_TEMPERATURE;
    private int generation;
    private int efficiency;
    /** Interlocks armed. Off means overdrive. */
    private boolean safeties = true;
    /** Keep running on a full buffer and vent the surplus, rather than standing by. */
    private boolean dumpExcess;
    private int fuelTicks;
    private int burnTenths;
    /** The client's looping hum and alarm, typed loosely so this class never loads client code on a server. */
    @Nullable
    public Object clientHum;
    @Nullable
    public Object clientAlarm;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY_LOW -> energy.getEnergyStored() & 0xFFFF;
                case DATA_ENERGY_HIGH -> energy.getEnergyStored() >>> 16;
                case DATA_COOLANT -> coolant.getFluidAmount();
                case DATA_TEMPERATURE -> temperature;
                case DATA_OUTPUT -> generation;
                case DATA_ENABLED -> enabled ? 1 : 0;
                case DATA_REDSTONE -> redstoneMode.ordinal();
                case DATA_FUEL -> fuelPermille();
                case DATA_STATUS -> status.ordinal();
                case DATA_EFFICIENCY -> efficiency;
                case DATA_SAFETIES -> safeties ? 1 : 0;
                case DATA_DUMP -> dumpExcess ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-side values are read-only; the client copy lives in the menu.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    @Nullable
    private BlockCapabilityCache<IEnergyStorage, @Nullable Direction> energyTarget;

    public ReactorHeartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_HEART.get(), pos, state);
    }

    /** One slot holding one sealed fuel core. Shared with the client-side menu so the rules match. */
    public static ItemStackHandler createFuelSlot(Runnable onChanged) {
        return new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                onChanged.run();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.is(ModTags.MICROREACTOR_FUEL);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    /** Extract only: the energy port never accepts power. */
    public IEnergyStorage energyOutput() {
        return energy;
    }

    public IFluidHandler coolantInput() {
        return coolantInput;
    }

    public IFluidHandler steamOutput() {
        return steamOutput;
    }

    public void togglePower() {
        enabled = !enabled;
        setChanged();
    }

    public boolean safetiesArmed() {
        return safeties;
    }

    /** The fuel inlet (on your left as you face the front): fresh cores in. */
    public IItemHandler fuelInput() {
        return fuelInput;
    }

    /** The spent-core outlet (on your right as you face the front): spent cores out. */
    public IItemHandler fuelOutput() {
        return fuelOutput;
    }

    public void toggleSafeties() {
        safeties = !safeties;
        setChanged();
    }

    public void toggleDump() {
        dumpExcess = !dumpExcess;
        setChanged();
    }

    public void cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        setChanged();
    }

    public void dropContents(Level level, BlockPos pos) {
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), fuel.getStackInSlot(0));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ReactorHeartBlockEntity heart) {
        MicroreactorSlot slot = state.getValue(MicroreactorPartBlock.SLOT);
        heart.generation = 0;
        heart.efficiency = 0;
        boolean cooled = heart.coolant.getFluidAmount() > 0;
        if (!slot.isFormed()) {
            heart.status = Status.OFFLINE;
            heart.updateTemperature(Status.OFFLINE, cooled);
            return;
        }
        Direction facing = state.getValue(MicroreactorPartBlock.FACING);
        BlockPos origin = slot.toOrigin(pos, facing);
        Status before = heart.status;
        heart.status = heart.currentStatus(level, origin, facing, cooled);
        if (heart.status == Status.COOLANT_LOSS && !Config.MICROREACTOR_MELTDOWNS.get()) {
            // Meltdowns are off: the interlock refuses to stay disarmed and drops back to a safe dry run.
            heart.safeties = true;
            heart.status = Status.ONLINE;
        }
        boolean running = heart.status.running();
        heart.updateTemperature(heart.status, cooled);
        if (heart.temperature >= MAX_TEMPERATURE) {
            heart.meltdown((ServerLevel) level, origin, facing);
            return;
        }
        if (running) {
            heart.run(cooled);
            RadiationSources.emit((ServerLevel) level, MicroreactorStructure.centre(origin, facing), switch (heart.status) {
                case COOLANT_LOSS -> COOLANT_LOSS_RADIATION;
                case OVERDRIVE -> OVERDRIVE_RADIATION;
                default -> RUNNING_RADIATION;
            });
        }
        if (heart.status == Status.COOLANT_LOSS) {
            heart.alarm((ServerLevel) level, origin, facing);
        }
        // Nearby clients follow a coolant loss closely, so the alarm can speed up with the heat.
        boolean alarmChanged = (before == Status.COOLANT_LOSS) != (heart.status == Status.COOLANT_LOSS);
        if (alarmChanged || (heart.status == Status.COOLANT_LOSS && level.getGameTime() % 5 == 0)) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        // Steam only exists while water is boiling. With nothing on the steam outlet to take it, it
        // blows out as a plume; piped away, it stays in the pipe.
        boolean piped = heart.pushSteam(level, origin, facing);
        if (running && cooled && !piped) {
            heart.vent((ServerLevel) level, origin, facing);
        }
        if (running != state.getValue(MicroreactorPartBlock.RUNNING)) {
            MicroreactorStructure.setRunning(level, origin, facing, running);
        }
        heart.pushEnergy((ServerLevel) level, origin, facing);
        if (level.getGameTime() % 20 == 0) {
            heart.ejectSpentCore(level, origin, facing);
        }
    }

    private void updateTemperature(Status status, boolean cooled) {
        int target = switch (status) {
            case ONLINE, DUMPING -> cooled ? COOLED_TEMPERATURE : UNCOOLED_TEMPERATURE;
            case OVERDRIVE -> OVERDRIVE_TEMPERATURE;
            case COOLANT_LOSS -> RUNAWAY_TEMPERATURE;
            default -> AMBIENT_TEMPERATURE;
        };
        int step = status == Status.COOLANT_LOSS || (temperature > target && cooled) ? 2 : 1;
        if (temperature != target) {
            temperature += Mth.clamp(target - temperature, -step, step);
            setChanged();
        }
    }

    /** Heat-to-power efficiency at a core temperature, as a fraction of the Carnot limit. */
    public static double efficiency(double temperature, boolean cooled, boolean superheated) {
        double hot = temperature + 273.15;
        double cold = (cooled ? WATER_SINK : DRY_SINK) + 273.15;
        return (superheated ? SUPERHEATED_ENGINE_FRACTION : ENGINE_FRACTION) * Math.max(0, 1 - cold / hot);
    }

    /** One tick of fission: make power from the heat, burn fuel, boil off coolant. */
    private void run(boolean cooled) {
        boolean overdrive = !safeties;
        double thermalPower = Config.get(Config.MICROREACTOR_OUTPUT) / REFERENCE_EFFICIENCY * (overdrive ? OVERDRIVE_POWER : 1);
        // Each mode's efficiency is the Carnot figure at its own operating temperature, scaled only by
        // warm-up. Using the live temperature instead made efficiency fall while water cooled a hot
        // core down, which read backwards: dry is always lowest, water higher, overdrive highest.
        int operating = !cooled ? UNCOOLED_TEMPERATURE : overdrive ? OVERDRIVE_TEMPERATURE : COOLED_TEMPERATURE;
        double warmup = Mth.clamp((temperature - AMBIENT_TEMPERATURE) / (double) (WARM_TEMPERATURE - AMBIENT_TEMPERATURE), 0, 1);
        double eff = efficiency(operating, cooled, overdrive && cooled) * warmup;
        efficiency = (int) Math.round(eff * 1000);
        generation = (int) Math.round(thermalPower * eff);
        energy.generate(generation);
        if (cooled) {
            // Boiled water leaves as steam: a fixed amount per mB (real water expands far more, but
            // this steam is under pressure). What the buffer cannot hold escapes.
            int boiled = coolant.drain(Config.get(Config.MICROREACTOR_COOLANT_USE) * (overdrive ? 2 : 1),
                    IFluidHandler.FluidAction.EXECUTE).getAmount();
            steam.fill(new FluidStack(ModFluids.STEAM.get(), boiled * Config.get(Config.STEAM_PER_WATER)),
                    IFluidHandler.FluidAction.EXECUTE);
        }
        burnTenths += overdrive ? OVERDRIVE_BURN : BURN;
        if (++fuelTicks >= FUEL_SYNC_TICKS) {
            ItemStack core = fuel.getStackInSlot(0);
            int left = FuelCoreItem.fuelLeft(core) - burnTenths / 10;
            burnTenths %= 10;
            fuelTicks = 0;
            if (left > 0) {
                ItemStack burnt = core.copy();
                FuelCoreItem.setFuelLeft(burnt, left);
                fuel.setStackInSlot(0, burnt);
            } else {
                // Spent: swapped out whole for the fuel cycle, as the design says.
                fuel.setStackInSlot(0, new ItemStack(ModItems.DEPLETED_FUEL_CORE.get()));
            }
        }
    }

    /** Steam blown out of the steam outlet when nothing is connected to take it. */
    private void vent(ServerLevel level, BlockPos origin, Direction facing) {
        Direction face = MicroreactorPort.STEAM_OUT.face(facing);
        Vec3 port = Vec3.atCenterOf(MicroreactorPort.STEAM_OUT.blockPos(origin, facing)).relative(face, 0.55);
        if (level.getGameTime() % 2 == 0) {
            // A count of 0 makes the offsets a velocity: a jet straight out of the port.
            level.sendParticles(ParticleTypes.CLOUD, port.x, port.y, port.z, 0, face.getStepX(), 0.15, face.getStepZ(), 0.12);
        }
    }

    /** Smoke while a dry core runs away. The alarm itself plays on the client, see MicroreactorAlarmSound. */
    private void alarm(ServerLevel level, BlockPos origin, Direction facing) {
        Vec3 centre = MicroreactorStructure.centre(origin, facing);
        if (level.getGameTime() % 4 == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, centre.x, centre.y + 1.0, centre.z, 3, 0.3, 0.2, 0.6, 0.02);
        }
    }

    /**
     * The core has run away. The machine and its fuel are destroyed in a steam explosion.
     * Only reachable with the safeties off, no water, and meltdowns allowed in the config.
     */
    private void meltdown(ServerLevel level, BlockPos origin, Direction facing) {
        Vec3 centre = MicroreactorStructure.centre(origin, facing);
        fuel.setStackInSlot(0, ItemStack.EMPTY);
        for (MicroreactorSlot part : MicroreactorSlot.FORMED) {
            level.removeBlock(part.fromOrigin(origin, facing), false);
        }
        ModTriggers.MILESTONE.get().triggerNearby(level, centre, Milestone.MELTDOWN);
        level.explode(null, centre.x, centre.y, centre.z, Config.MICROREACTOR_MELTDOWN_POWER.get().floatValue(),
                true, Level.ExplosionInteraction.BLOCK);
        RadiationSources.contaminate(level, centre, MELTDOWN_RADIATION, MELTDOWN_FADE_TICKS);
    }

    private int fuelPermille() {
        ItemStack core = fuel.getStackInSlot(0);
        return core.is(ModTags.MICROREACTOR_FUEL) ? Math.round(FuelCoreItem.fraction(core) * 1000) : 0;
    }

    private Status currentStatus(Level level, BlockPos origin, Direction facing, boolean cooled) {
        if (!enabled) {
            return Status.OFFLINE;
        }
        boolean powered = false;
        for (MicroreactorSlot part : MicroreactorSlot.FORMED) {
            powered |= level.hasNeighborSignal(part.fromOrigin(origin, facing));
        }
        if (!redstoneMode.allows(powered)) {
            return Status.HALTED;
        }
        ItemStack core = fuel.getStackInSlot(0);
        if (core.isEmpty()) {
            return Status.NO_CORE;
        }
        if (!core.is(ModTags.MICROREACTOR_FUEL)) {
            return Status.DEPLETED;
        }
        boolean full = energy.isFull();
        if (full && !dumpExcess) {
            return Status.STANDBY;
        }
        if (!safeties) {
            return cooled ? Status.OVERDRIVE : Status.COOLANT_LOSS;
        }
        return full ? Status.DUMPING : Status.ONLINE;
    }

    /**
     * Pushes steam out of the steam outlet, the same way the energy port pushes power. Returns
     * whether something is connected there to take it.
     */
    private boolean pushSteam(Level level, BlockPos origin, Direction facing) {
        Direction face = MicroreactorPort.STEAM_OUT.face(facing);
        BlockPos target = MicroreactorPort.STEAM_OUT.blockPos(origin, facing).relative(face);
        IFluidHandler receiver = level.getCapability(Capabilities.FluidHandler.BLOCK, target, face.getOpposite());
        if (receiver != null && !steam.isEmpty()) {
            steam.drain(receiver.fill(steam.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        }
        // A gas pipe set to extract offers nothing to push into (it pulls instead), but it is still
        // taking the steam away.
        BlockState pipe = level.getBlockState(target);
        boolean extracting = pipe.getBlock() instanceof GasPipeBlock
                && pipe.getValue(CableBlock.SIDES.get(face.getOpposite())) != CableSide.NONE;
        return receiver != null || extracting;
    }

    /**
     * Pushes a spent core out of the spent-core outlet into whatever is beside it (an item pipe, a
     * hopper, a chest), the same way the energy port pushes power, so the pipe there needs no
     * extract side.
     */
    private void ejectSpentCore(Level level, BlockPos origin, Direction facing) {
        ItemStack core = fuel.getStackInSlot(0);
        if (!core.is(ModItems.DEPLETED_FUEL_CORE.get())) {
            return;
        }
        Direction face = MicroreactorPort.FUEL_OUT.face(facing);
        BlockPos target = MicroreactorPort.FUEL_OUT.blockPos(origin, facing).relative(face);
        IItemHandler receiver = level.getCapability(Capabilities.ItemHandler.BLOCK, target, face.getOpposite());
        if (receiver != null) {
            fuel.setStackInSlot(0, ItemHandlerHelper.insertItemStacked(receiver, core.copy(), false));
        }
    }

    /**
     * Pushes energy out of the energy port. Many cables (Mekanism's included) do not pull from
     * other mods' generators, so a generator that pushes works with all of them.
     */
    private void pushEnergy(ServerLevel level, BlockPos origin, Direction facing) {
        if (energy.getEnergyStored() <= 0) {
            return;
        }
        Direction face = MicroreactorPort.ENERGY_OUT.face(facing);
        BlockPos target = MicroreactorPort.ENERGY_OUT.blockPos(origin, facing).relative(face);
        if (energyTarget == null || !energyTarget.pos().equals(target) || energyTarget.context() != face.getOpposite()) {
            energyTarget = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, target, face.getOpposite());
        }
        IEnergyStorage receiver = energyTarget.getCapability();
        if (receiver != null && receiver.canReceive()) {
            int offered = energy.extractEnergy(MAX_OUTPUT, true);
            energy.extractEnergy(receiver.receiveEnergy(offered, false), false);
        }
    }

    public Status status() {
        return status;
    }

    public int temperature() {
        return temperature;
    }

    /** Only what the client needs for sound: the status and temperature. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("status", status.ordinal());
        tag.putInt("temperature", temperature);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        int id = tag.getInt("status");
        status = id >= 0 && id < Status.values().length ? Status.values()[id] : Status.OFFLINE;
        temperature = tag.getInt("temperature");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.ryzergen.microreactor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MicroreactorMenu(containerId, inventory, this, fuel, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energy.serializeNBT(registries));
        tag.put("coolant", coolant.writeToNBT(registries, new CompoundTag()));
        tag.put("steam", steam.writeToNBT(registries, new CompoundTag()));
        tag.put("fuel", fuel.serializeNBT(registries));
        tag.putBoolean("enabled", enabled);
        tag.putInt("redstone_mode", redstoneMode.ordinal());
        tag.putInt("temperature", temperature);
        tag.putBoolean("safeties", safeties);
        tag.putBoolean("dump_excess", dumpExcess);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energy")) {
            energy.deserializeNBT(registries, tag.get("energy"));
        }
        coolant.readFromNBT(registries, tag.getCompound("coolant"));
        steam.readFromNBT(registries, tag.getCompound("steam"));
        fuel.deserializeNBT(registries, tag.getCompound("fuel"));
        enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        redstoneMode = RedstoneMode.byId(tag.getInt("redstone_mode"));
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : AMBIENT_TEMPERATURE;
        safeties = !tag.contains("safeties") || tag.getBoolean("safeties");
        dumpExcess = tag.getBoolean("dump_excess");
    }
}
