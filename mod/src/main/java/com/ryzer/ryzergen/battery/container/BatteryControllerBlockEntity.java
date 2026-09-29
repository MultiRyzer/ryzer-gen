package com.ryzer.ryzergen.battery.container;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.battery.BatteryModuleItem;
import com.ryzer.ryzergen.client.ContainerGhostPreview;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Container Battery controller's block entity (design section 12). Before the container forms
 * it builds it from the frame and thermal unit fed into it, as the pool's controller does. Once
 * formed it is the battery: its charge is shared by the LFP racks installed in the front's 20
 * slots, each adding capacity and rate (rule 11). Both energy ports on the back take energy in and
 * give it out, like a home battery, so a cable network treats the container as a battery (a buffer:
 * machines first, surplus into storage) and the cables' own settings decide the flow. Moving energy
 * warms the racks: the fan unit drinks coolant while it works, and without coolant the battery runs
 * at a quarter of its rate.
 */
public class BatteryControllerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int PART_SLOTS = 9;
    public static final int SLOTS = ContainerLayout.SLOTS.size();
    public static final int COOLANT_CAPACITY = 8_000;
    private static final int PER_STEP = 3;
    private static final int STEP_TICKS = 2;

    public static final int DATA_MISSING_FRAME = 0;
    public static final int DATA_MISSING_THERMAL = 1;
    public static final int DATA_BLOCKED = 2;
    public static final int DATA_FORMED = 3;
    public static final int DATA_ENERGY_LOW = 4;
    public static final int DATA_ENERGY_HIGH = 5;
    public static final int DATA_CAPACITY_LOW = 6;
    public static final int DATA_CAPACITY_HIGH = 7;
    public static final int DATA_IN_LOW = 8;
    public static final int DATA_IN_HIGH = 9;
    public static final int DATA_OUT_LOW = 10;
    public static final int DATA_OUT_HIGH = 11;
    public static final int DATA_RACKS = 12;
    public static final int DATA_COOLANT = 13;
    /** Which slots hold racks, a bit each. */
    public static final int DATA_MASK_LOW = 14;
    public static final int DATA_MASK_HIGH = 15;
    public static final int DATA_COUNT = 16;

    private static final List<BlockPos> ORDER = new ArrayList<>();

    static {
        for (int index = 1; index <= ContainerLayout.CELLS; index++) {
            ORDER.add(ContainerLayout.cell(index));
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

    private final FluidTank coolant = new FluidTank(COOLANT_CAPACITY, fluid -> fluid.is(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final boolean[] installed = new boolean[SLOTS];
    private int energy;
    private int inThisTick;
    private int outThisTick;
    private int lastIn;
    private int lastOut;
    /** Whether energy moved lately: the fan turns, and players are told when it starts or stops. */
    private boolean working;
    private final int[] missing = new int[2];
    private int blocked;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_MISSING_FRAME, DATA_MISSING_THERMAL -> missing[index];
                case DATA_BLOCKED -> blocked;
                case DATA_FORMED -> isFormed() ? 1 : 0;
                case DATA_ENERGY_LOW -> energy & 0xFFFF;
                case DATA_ENERGY_HIGH -> energy >>> 16;
                case DATA_CAPACITY_LOW -> capacity() & 0xFFFF;
                case DATA_CAPACITY_HIGH -> capacity() >>> 16;
                case DATA_IN_LOW -> lastIn & 0xFFFF;
                case DATA_IN_HIGH -> lastIn >>> 16;
                case DATA_OUT_LOW -> lastOut & 0xFFFF;
                case DATA_OUT_HIGH -> lastOut >>> 16;
                case DATA_RACKS -> racks();
                case DATA_COOLANT -> coolant.getFluidAmount();
                case DATA_MASK_LOW -> mask() & 0xFFFF;
                case DATA_MASK_HIGH -> mask() >>> 16;
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

    public BatteryControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_CONTROLLER.get(), pos, state);
    }

    public static @Nullable ContainerPart partFor(ItemStack stack) {
        for (ContainerPart part : new ContainerPart[] {ContainerPart.FRAME, ContainerPart.THERMAL}) {
            if (stack.is(part.block().asItem())) {
                return part;
            }
        }
        return null;
    }

    public boolean isFormed() {
        return ContainerPartBlock.isFormed(getBlockState());
    }

    public Direction facing() {
        return getBlockState().getValue(ContainerPartBlock.FACING);
    }

    public boolean raised() {
        return isFormed() || (level != null && ContainerStructure.isRaised(level, worldPosition));
    }

    public BlockPos cellPos(BlockPos cell) {
        return ContainerLayout.toWorld(worldPosition, ContainerLayout.anchor(raised()), facing(), cell);
    }

    public IFluidHandler coolant() {
        return coolant;
    }

    /** Whether the fan should turn. */
    public boolean working() {
        return working;
    }

    // ---------------------------------------------------------------- racks and charge

    public boolean installed(int slot) {
        return installed[slot];
    }

    /** A bit per slot holding a rack. */
    private int mask() {
        int mask = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            mask |= installed[slot] ? 1 << slot : 0;
        }
        return mask;
    }

    public int racks() {
        int count = 0;
        for (boolean rack : installed) {
            count += rack ? 1 : 0;
        }
        return count;
    }

    public int capacity() {
        return racks() * LfpRackItem.capacity();
    }

    /** FE per tick in or out: each rack's, a quarter of it without coolant. */
    public int rate() {
        int rate = racks() * LfpRackItem.rate();
        return coolant.getFluidAmount() > 0 ? rate : rate / 4;
    }

    /** Slots a rack in, bringing its charge with it. */
    public void install(int slot, ItemStack rack) {
        if (installed[slot]) {
            return;
        }
        installed[slot] = true;
        energy = Math.min(capacity(), energy + BatteryModuleItem.energy(rack));
        setChanged();
        if (level instanceof ServerLevel server) {
            ContainerStructure.setInstalled(server, worldPosition, facing(), slot, true);
        }
    }

    /** Takes a rack out, with its fair share of the charge. */
    public ItemStack remove(int slot) {
        if (!installed[slot]) {
            return ItemStack.EMPTY;
        }
        int share = energy / racks();
        installed[slot] = false;
        energy -= share;
        setChanged();
        if (level instanceof ServerLevel server) {
            ContainerStructure.setInstalled(server, worldPosition, facing(), slot, false);
        }
        return BatteryModuleItem.withEnergy(new ItemStack(ModItems.LFP_BATTERY_RACK.get()), share);
    }

    // ---------------------------------------------------------------- ports

    public static @Nullable IEnergyStorage energyAt(Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        ContainerLayout.Port port = portAt(state, side);
        if (port != ContainerLayout.Port.ENERGY_EAST && port != ContainerLayout.Port.ENERGY_WEST) {
            return null;
        }
        BatteryControllerBlockEntity controller = ContainerPartBlock.controller(level, pos, state);
        return controller == null ? null : controller.storage;
    }

    public static @Nullable IFluidHandler fluidAt(Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        BatteryControllerBlockEntity controller = portAt(state, side) == ContainerLayout.Port.COOLANT_IN
                ? ContainerPartBlock.controller(level, pos, state) : null;
        return controller == null ? null : controller.coolantIn;
    }

    private static @Nullable ContainerLayout.Port portAt(BlockState state, @Nullable Direction side) {
        if (side == null || !(state.getBlock() instanceof ContainerPartBlock) || !ContainerPartBlock.isFormed(state)) {
            return null;
        }
        BlockPos cell = ContainerLayout.cell(state.getValue(ContainerPartBlock.CELL));
        for (ContainerLayout.Port port : ContainerLayout.Port.values()) {
            if (port.cell().equals(cell) && port.face(state.getValue(ContainerPartBlock.FACING)) == side) {
                return port;
            }
        }
        return null;
    }

    public @Nullable IItemHandler partsFor(@Nullable Direction side) {
        return isFormed() ? null : parts;
    }

    /** Both energy ports: charge in and out, each up to the racks' rate every tick. */
    private final IEnergyStorage storage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int accepted = Math.max(0, Math.min(amount, Math.min(rate() - inThisTick, capacity() - energy)));
            if (!simulate && accepted > 0) {
                energy += accepted;
                inThisTick += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int given = Math.max(0, Math.min(amount, Math.min(rate() - outThisTick, energy)));
            if (!simulate && given > 0) {
                energy -= given;
                outThisTick += given;
                setChanged();
            }
            return given;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity();
        }

        @Override
        public boolean canExtract() {
            return racks() > 0;
        }

        @Override
        public boolean canReceive() {
            return racks() > 0;
        }
    };

    /** Coolant in: fills the fan unit's tank, never drained from outside. */
    private final IFluidHandler coolantIn = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return coolant.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return COOLANT_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return coolant.isFluidValid(stack);
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

    // ---------------------------------------------------------------- ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, BatteryControllerBlockEntity controller) {
        if (ContainerPartBlock.isFormed(state)) {
            controller.run((ServerLevel) level);
            return;
        }
        if (level.getGameTime() % STEP_TICKS == 0) {
            controller.buildStep((ServerLevel) level);
        }
        if (level.getGameTime() % 10 == 0) {
            controller.survey(level);
        }
    }

    /** The fan's angle as players see it, and how fast it turns (easing up and down). */
    private float fanAngle;
    private float fanAngleBefore;
    private float fanSpeed;

    public float fanAngle(float partialTick) {
        return fanAngleBefore + (fanAngle - fanAngleBefore) * partialTick;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, BatteryControllerBlockEntity controller) {
        ContainerGhostPreview.track(controller);
        controller.fanSpeed += ((controller.working ? 1 : 0) - controller.fanSpeed) * 0.04F;
        controller.fanAngleBefore = controller.fanAngle;
        controller.fanAngle = (controller.fanAngle + controller.fanSpeed * 18) % 360;
    }

    private void run(ServerLevel level) {
        lastIn = inThisTick;
        lastOut = outThisTick;
        inThisTick = 0;
        outThisTick = 0;
        boolean moving = lastIn > 0 || lastOut > 0;
        // Moving energy warms the racks; the fan unit's coolant carries it off.
        if (moving) {
            coolant.drain(Config.get(Config.CONTAINER_COOLANT_USE), IFluidHandler.FluidAction.EXECUTE);
        }
        if (moving != working && (moving || level.getGameTime() % 20 == 0)) {
            working = moving;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---------------------------------------------------------------- building

    private void buildStep(ServerLevel level) {
        int placed = 0;
        boolean raised = raised();
        for (BlockPos cell : ORDER) {
            if (placed >= PER_STEP) {
                return;
            }
            ContainerPart part = ContainerLayout.partAt(cell, raised);
            if (part == ContainerPart.CONTROLLER) {
                continue;
            }
            BlockPos pos = cellPos(cell);
            BlockState there = level.getBlockState(pos);
            if (there.is(part.block()) || !there.canBeReplaced() || !take(part)) {
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

    private boolean take(ContainerPart part) {
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
        boolean raised = raised();
        for (BlockPos cell : ORDER) {
            ContainerPart part = ContainerLayout.partAt(cell, raised);
            if (part == ContainerPart.CONTROLLER) {
                continue;
            }
            BlockState there = level.getBlockState(cellPos(cell));
            if (there.is(part.block())) {
                continue;
            }
            missing[part == ContainerPart.FRAME ? 0 : 1]++;
            if (!there.canBeReplaced()) {
                blocked++;
            }
        }
    }

    // ---------------------------------------------------------------- contents

    public void dropParts(Level level, BlockPos pos) {
        for (int slot = 0; slot < parts.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), parts.getStackInSlot(slot));
            parts.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    /** Everything the controller holds, dropped when it is broken: parts, and each rack with its share of the charge. */
    public void dropAll(Level level, BlockPos pos) {
        dropParts(level, pos);
        for (int slot = 0; slot < SLOTS; slot++) {
            if (installed[slot]) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remove(slot));
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.ryzergen.battery_controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        ContainerLevelAccess access = ContainerLevelAccess.create(level, worldPosition);
        return isFormed() ? new ContainerMenu(containerId, data, access)
                : new ContainerBuildMenu(containerId, inventory, parts, data, access);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("parts", parts.serializeNBT(registries));
        tag.put("coolant", coolant.writeToNBT(registries, new CompoundTag()));
        tag.putInt("racks", mask());
        tag.putInt("energy", energy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        parts.deserializeNBT(registries, tag.getCompound("parts"));
        coolant.readFromNBT(registries, tag.getCompound("coolant"));
        int mask = tag.getInt("racks");
        for (int slot = 0; slot < SLOTS; slot++) {
            installed[slot] = (mask & 1 << slot) != 0;
        }
        energy = Math.min(tag.getInt("energy"), capacity());
    }

    /** Players need only know whether the fan turns. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("working", working);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        working = tag.getBoolean("working");
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }
}
