package com.ryzer.ryzergen.machine.processing;

import com.ryzer.ryzergen.machine.SoundSwitch;
import com.ryzer.ryzergen.machine.AnimatedMachine;
import com.ryzer.ryzergen.machine.MachineClock;
import com.ryzer.ryzergen.machine.MachineClock.Look;
import com.ryzer.ryzergen.machine.MachineEnergyStorage;
import com.ryzer.ryzergen.machine.MachineItemPort;
import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.machine.pool.HotFuel;
import com.ryzer.ryzergen.recipe.MachineRecipe;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A fuel cycle machine at work (see {@link ProcessingMachine}): input slots, output slots, an energy
 * buffer, for the reprocessor a water tank, and for the Electrorefiner an output tank for fluid
 * results. Each tick it finds the recipe its inputs make, and
 * while there is room for the results and power to spend, it works towards it.
 *
 * <p>Pipes connect on any side, and every side works the same way: items in (only items some recipe
 * uses, and never the same item in two input slots, so one pipe cannot fill every slot and jam it),
 * results out (only results, so a pipe set to extract never pulls ingredients),
 * energy and water in and fluid results out on any side. When two recipes match, the one using more ingredients wins, so a
 * fabricator holding uranium, steel and plutonium makes MOX rather than a plain uranium rod.
 */
public class ProcessingBlockEntity extends BlockEntity implements MenuProvider, AnimatedMachine, SoundSwitch.Holder {
    /** Its sound switch (the speaker key on its screen): muted, its running sounds stop. */
    private final SoundSwitch sound = new SoundSwitch(this);

    @Override
    public SoundSwitch sound() {
        return sound;
    }

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_TOTAL = 1;
    public static final int DATA_ENERGY_LOW = 2;
    public static final int DATA_ENERGY_HIGH = 3;
    public static final int DATA_WATER = 4;
    public static final int DATA_STATUS = 5;
    public static final int DATA_REDSTONE = 6;
    public static final int DATA_ENABLED = 7;
    public static final int DATA_MODULES = 8;
    public static final int DATA_AUTO_OUTPUT = 9;
    public static final int DATA_TANK = 10;
    /** The output tank's fluid, as its registry id (-1 empty), so the screen can draw it. */
    public static final int DATA_TANK_FLUID = 11;
    public static final int DATA_COUNT = 12;
    /** Auto output tries every this many ticks. */
    private static final int AUTO_OUTPUT_INTERVAL = 10;

    /** UNDERPOWERED: a gated machine getting some power, but less than its full draw. */
    public enum Status { IDLE, RUNNING, NO_POWER, NO_WATER, OUTPUT_FULL, REDSTONE, OFF, UNDERPOWERED }

    private final ProcessingMachine machine;
    private final ItemStackHandler items;
    private final IItemHandler inputs;
    private final IItemHandler outputs;
    private final MachineEnergyStorage energy;
    private final FluidTank water;
    /** Fluid results, drained by pipes on any side. */
    private final FluidTank tank;
    private final IFluidHandler fluidPort;

    private int progress;
    private int total;
    private Status status = Status.IDLE;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    private boolean enabled = true;
    /**
     * Pushes finished results into the blocks beside it (a chest, another machine, a pipe). Off by
     * default, so machines that merely touch never hand each other things unless the player asks.
     */
    private boolean autoOutput;
    private int autoOutputFrom;
    /** Set when the inputs or water change, so the recipe is looked up again. */
    private boolean dirty = true;
    private @Nullable MachineRecipe recipe;
    /** The client's working sound, typed loosely so this class never loads client code on a server. */
    public @Nullable Object clientSound;
    /** No recipe yet, but the inputs would make one with a full tank: waiting on water. */
    private boolean thirsty;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_TOTAL -> total;
                case DATA_ENERGY_LOW -> energy.getEnergyStored() & 0xFFFF;
                case DATA_ENERGY_HIGH -> energy.getEnergyStored() >>> 16;
                case DATA_WATER -> water.getFluidAmount();
                case DATA_STATUS -> status.ordinal();
                case DATA_REDSTONE -> redstoneMode.ordinal();
                case DATA_ENABLED -> enabled ? 1 : 0;
                case DATA_MODULES -> modules();
                case DATA_AUTO_OUTPUT -> autoOutput ? 1 : 0;
                case DATA_TANK -> tank.getFluidAmount();
                case DATA_TANK_FLUID -> tank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Read-only on the server; the client copy lives in the menu.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ProcessingBlockEntity(ProcessingMachine machine, BlockPos pos, BlockState state) {
        super(machine.blockEntityType(), pos, state);
        this.machine = machine;
        energy = new MachineEnergyStorage(capacity(machine, 0), maxInput(machine, 0), this::setChanged);
        items = createItems(machine, this::usable, () -> {
            dirty = true;
            setChanged();
        });
        inputs = MachineItemPort.of(items, 0, machine.inputs(), machine.inputs(), machine.slots());
        outputs = inputs;
        water = new FluidTank(machine.waterCapacity(), stack -> stack.is(machine.inputTag())) {
            @Override
            protected void onContentsChanged() {
                dirty = true;
                setChanged();
            }
        };
        tank = new FluidTank(Math.max(1, machine.tankCapacity())) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        // Water in (tank 0, if it takes water), fluid results out (the last tank, if it has one).
        fluidPort = new IFluidHandler() {
            private @Nullable FluidTank at(int index) {
                if (machine.usesWater() && index == 0) {
                    return water;
                }
                return machine.hasTank() && index == getTanks() - 1 ? tank : null;
            }

            @Override
            public int getTanks() {
                return (machine.usesWater() ? 1 : 0) + (machine.hasTank() ? 1 : 0);
            }

            @Override
            public FluidStack getFluidInTank(int index) {
                FluidTank held = at(index);
                return held == null ? FluidStack.EMPTY : held.getFluid();
            }

            @Override
            public int getTankCapacity(int index) {
                FluidTank held = at(index);
                return held == null ? 0 : held.getCapacity();
            }

            @Override
            public boolean isFluidValid(int index, FluidStack stack) {
                return at(index) == water && water.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return machine.usesWater() ? water.fill(resource, action) : 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return machine.hasTank() ? tank.drain(resource, action) : FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return machine.hasTank() ? tank.drain(maxDrain, action) : FluidStack.EMPTY;
            }
        };
    }

    /**
     * Shared with the client-side menu so slot rules match on both sides. {@code usable} says whether
     * any recipe takes an item (the client, with no recipes to hand, lets everything through).
     */
    public static ItemStackHandler createItems(ProcessingMachine machine, Predicate<ItemStack> usable, Runnable onChanged) {
        return new ItemStackHandler(machine.slots() + 1) {
            @Override
            protected void onContentsChanged(int slot) {
                onChanged.run();
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == machine.upgradeSlot() ? ProcessingMachine.MAX_MODULES : super.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == machine.upgradeSlot()) {
                    return stack.is(ModItems.SPEED_MODULE.get());
                }
                if (slot >= machine.inputs() || !usable.test(stack) || HotFuel.isHot(stack)) {
                    return false;
                }
                for (int other = 0; other < machine.inputs(); other++) {
                    if (other != slot && ItemStack.isSameItemSameComponents(getStackInSlot(other), stack)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    public ProcessingMachine machine() {
        return machine;
    }

    /** What it shows from outside, and the client's clock its moving parts follow (MachineMotion). */
    private final MachineClock clock = new MachineClock();

    @Override
    public MachineClock clock() {
        return clock;
    }

    @Override
    public String motionId() {
        return machine.id();
    }

    private Look lookFor(Status status) {
        return switch (status) {
            case RUNNING -> Look.WORKING;
            case UNDERPOWERED -> Look.STARVED;
            case IDLE, NO_WATER, OUTPUT_FULL -> energy.getEnergyStored() > 0 ? Look.STANDBY : Look.OFF;
            case NO_POWER, REDSTONE, OFF -> Look.OFF;
        };
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = clock.write();
        sound.save(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        sound.load(tag);
        clock.read(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    /** Work done on the current job, in ticks of work. */
    public int progress() {
        return progress;
    }

    /** Speed modules fitted, 0 to {@link ProcessingMachine#MAX_MODULES}. */
    public int modules() {
        return Math.min(ProcessingMachine.MAX_MODULES, items.getStackInSlot(machine.upgradeSlot()).getCount());
    }

    /** Work done per tick: one, plus one per speed module. */
    public static int speed(int modules) {
        return 1 + modules;
    }

    /** Power drawn per tick while working: the base, times the speed squared. */
    public static int energyPerTick(ProcessingMachine machine, int modules) {
        return (int) Math.min(Integer.MAX_VALUE, (long) machine.energyPerTick() * speed(modules) * speed(modules));
    }

    /**
     * The energy buffer: a gated machine's holds {@link ProcessingMachine#GATED_BUFFER_TICKS} ticks
     * of its draw, the others a fixed store (larger if the config raises their draw).
     */
    public static int capacity(ProcessingMachine machine, int modules) {
        int draw = energyPerTick(machine, modules);
        if (machine.gated()) {
            return (int) Math.min(Integer.MAX_VALUE, (long) draw * ProcessingMachine.GATED_BUFFER_TICKS);
        }
        return (int) Math.max(ProcessingMachine.ENERGY_CAPACITY, Math.min(Integer.MAX_VALUE, 8L * draw));
    }

    /** FE per tick the buffer takes in. */
    public static int maxInput(ProcessingMachine machine, int modules) {
        return machine.gated() ? capacity(machine, modules) : Math.max(ProcessingMachine.MAX_INPUT, energyPerTick(machine, modules));
    }

    private void updateLimits() {
        int modules = modules();
        energy.setLimits(capacity(machine, modules), maxInput(machine, modules));
    }

    private List<MachineRecipe> recipes() {
        if (level == null) {
            return List.of();
        }
        return level.getRecipeManager().getAllRecipesFor(machine.process().type()).stream().map(RecipeHolder::value).toList();
    }

    private boolean usable(ItemStack stack) {
        return level == null || recipes().stream().anyMatch(recipe -> recipe.uses(stack));
    }

    private MachineRecipe.Input input(FluidStack fluid) {
        List<ItemStack> held = new ArrayList<>(machine.inputs());
        for (int slot = 0; slot < machine.inputs(); slot++) {
            held.add(items.getStackInSlot(slot));
        }
        return new MachineRecipe.Input(held, fluid);
    }

    /** The matching recipe that uses the most ingredients, or null. */
    private @Nullable MachineRecipe findRecipe(MachineRecipe.Input input) {
        MachineRecipe best = null;
        for (MachineRecipe candidate : recipes()) {
            if (candidate.slotsFor(input) != null && (best == null || candidate.inputs().size() > best.inputs().size())) {
                best = candidate;
            }
        }
        return best;
    }

    /** Whether all of a recipe's results fit: its items in the output slots together, its fluid in the tank. */
    private boolean hasRoomFor(MachineRecipe recipe) {
        if (recipe.fluidResult().isPresent()) {
            FluidStack fluid = recipe.fluidResult().get();
            if (!machine.hasTank() || tank.fill(fluid.copy(), IFluidHandler.FluidAction.SIMULATE) < fluid.getAmount()) {
                return false;
            }
        }
        List<ItemStack> slots = new ArrayList<>();
        for (int slot = machine.inputs(); slot < machine.slots(); slot++) {
            slots.add(items.getStackInSlot(slot).copy());
        }
        for (ItemStack result : recipe.results()) {
            int left = result.getCount();
            for (int i = 0; i < slots.size() && left > 0; i++) {
                ItemStack held = slots.get(i);
                if (held.isEmpty()) {
                    int moved = Math.min(left, result.getMaxStackSize());
                    slots.set(i, result.copyWithCount(moved));
                    left -= moved;
                } else if (ItemStack.isSameItemSameComponents(held, result)) {
                    int moved = Math.min(left, held.getMaxStackSize() - held.getCount());
                    held.grow(moved);
                    left -= moved;
                }
            }
            if (left > 0) {
                return false;
            }
        }
        return true;
    }

    private void craft(MachineRecipe recipe) {
        int[] slots = recipe.slotsFor(input(water.getFluid()));
        if (slots == null) {
            return;
        }
        for (int i = 0; i < slots.length; i++) {
            items.extractItem(slots[i], recipe.inputs().get(i).count(), false);
        }
        recipe.fluid().ifPresent(fluid -> water.drain(fluid.amount(), IFluidHandler.FluidAction.EXECUTE));
        recipe.fluidResult().ifPresent(fluid -> tank.fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE));
        for (ItemStack result : recipe.results()) {
            ItemStack left = result.copy();
            for (int slot = machine.inputs(); slot < machine.slots() && !left.isEmpty(); slot++) {
                ItemStack held = items.getStackInSlot(slot);
                if (held.isEmpty()) {
                    int moved = Math.min(left.getCount(), left.getMaxStackSize());
                    items.setStackInSlot(slot, left.copyWithCount(moved));
                    left.shrink(moved);
                } else if (ItemStack.isSameItemSameComponents(held, left)) {
                    int moved = Math.min(left.getCount(), held.getMaxStackSize() - held.getCount());
                    items.setStackInSlot(slot, held.copyWithCount(held.getCount() + moved));
                    left.shrink(moved);
                }
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ProcessingBlockEntity machine) {
        machine.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        if (autoOutput && level.getGameTime() % AUTO_OUTPUT_INTERVAL == 0) {
            pushResults(level, pos);
        }
        updateLimits();
        if (dirty) {
            recipe = findRecipe(input(water.getFluid()));
            thirsty = recipe == null && machine.usesWater()
                    && findRecipe(input(new FluidStack(machine.inputFluid(), machine.waterCapacity()))) != null;
            dirty = false;
        }
        boolean powered = level.hasNeighborSignal(pos) || (machine.tall() && level.hasNeighborSignal(pos.above()));
        Status before = status;
        boolean working = false;
        // Taken every tick, so it always covers the last two, whatever the machine is doing.
        int intake = energy.takeIntake();
        int draw = energyPerTick(machine, modules());
        if (!enabled) {
            status = Status.OFF;
        } else if (!redstoneMode.allows(powered)) {
            status = Status.REDSTONE;
        } else if (recipe == null) {
            status = thirsty ? Status.NO_WATER : Status.IDLE;
        } else if (!hasRoomFor(recipe)) {
            status = Status.OUTPUT_FULL;
        } else if ((machine.gated() && intake < ProcessingMachine.GATED_BUFFER_TICKS * draw) || !energy.consume(draw)) {
            status = machine.gated() && (energy.getEnergyStored() > 0 || intake > 0) ? Status.UNDERPOWERED : Status.NO_POWER;
            if (machine.gated()) {
                // Short of the full draw: no progress, and what did arrive is spent anyway.
                energy.drain();
            }
        } else {
            status = Status.RUNNING;
            working = true;
            total = machine.time(recipe.time());
            progress += speed(modules());
            if (progress >= total) {
                craft(recipe);
                progress = 0;
            }
        }
        if (recipe == null && progress > 0) {
            progress = 0;
        }
        if (working || status != before) {
            setChanged();
        }
        clock.show(level, pos, state, lookFor(status), modules());
        if (working != state.getValue(ProcessingBlock.ACTIVE)) {
            level.setBlock(pos, state.setValue(ProcessingBlock.ACTIVE, working), Block.UPDATE_ALL);
            if (machine.tall()) {
                BlockState upper = level.getBlockState(pos.above());
                if (upper.is(state.getBlock())) {
                    level.setBlock(pos.above(), upper.setValue(ProcessingBlock.ACTIVE, working), Block.UPDATE_ALL);
                }
            }
        }
    }

    public void toggleAutoOutput() {
        autoOutput = !autoOutput;
        setChanged();
    }

    /**
     * Offers each result to the blocks beside it, starting one side further round each time. A
     * two-high machine skips its own upper half.
     */
    private void pushResults(Level level, BlockPos pos) {
        Direction[] sides = Direction.values();
        for (int i = 0; i < sides.length; i++) {
            Direction dir = sides[(autoOutputFrom + i) % sides.length];
            if (machine.tall() && dir == Direction.UP) {
                continue;
            }
            if (!tank.isEmpty()) {
                IFluidHandler fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(dir), dir.getOpposite());
                if (fluids != null) {
                    tank.drain(fluids.fill(tank.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                }
            }
            IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target == null) {
                continue;
            }
            for (int slot = machine.inputs(); slot < machine.slots(); slot++) {
                ItemStack held = items.getStackInSlot(slot);
                if (held.isEmpty()) {
                    continue;
                }
                ItemStack left = ItemHandlerHelper.insertItemStacked(target, held.copy(), false);
                if (left.getCount() != held.getCount()) {
                    items.setStackInSlot(slot, left);
                }
            }
        }
        autoOutputFrom = (autoOutputFrom + 1) % sides.length;
    }

    public void togglePower() {
        enabled = !enabled;
        setChanged();
    }

    public void cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        setChanged();
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == null) {
            return items;
        }
        return side == Direction.DOWN ? outputs : inputs;
    }

    public IEnergyStorage getEnergy(@Nullable Direction side) {
        return energy;
    }

    /** Water in and fluid results out, for machines with either tank. */
    public @Nullable IFluidHandler getFluids(@Nullable Direction side) {
        return machine.usesWater() || machine.hasTank() ? fluidPort : null;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(slot));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ryzergen." + machine.id());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ProcessingMenu(machine, containerId, inventory, items, data, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        sound.save(tag);
        tag.put("inventory", items.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        if (machine.usesWater()) {
            tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        }
        if (machine.hasTank()) {
            tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        }
        tag.putInt("progress", progress);
        tag.putInt("total", total);
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("auto_output", autoOutput);
        tag.putInt("redstone_mode", redstoneMode.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sound.load(tag);
        // Copied slot by slot, so machines saved before the upgrade slot existed keep their items.
        ItemStackHandler saved = new ItemStackHandler();
        saved.deserializeNBT(registries, tag.getCompound("inventory"));
        for (int slot = 0; slot < items.getSlots(); slot++) {
            items.setStackInSlot(slot, slot < saved.getSlots() ? saved.getStackInSlot(slot) : ItemStack.EMPTY);
        }
        updateLimits();
        energy.setStored(tag.getInt("energy"));
        if (machine.usesWater()) {
            water.readFromNBT(registries, tag.getCompound("water"));
        }
        if (machine.hasTank()) {
            tank.readFromNBT(registries, tag.getCompound("tank"));
        }
        progress = tag.getInt("progress");
        total = tag.getInt("total");
        enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        autoOutput = tag.getBoolean("auto_output");
        redstoneMode = RedstoneMode.byId(tag.getInt("redstone_mode"));
        dirty = true;
    }
}
