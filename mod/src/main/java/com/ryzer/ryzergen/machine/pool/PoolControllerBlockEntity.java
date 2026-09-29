package com.ryzer.ryzergen.machine.pool;

import com.ryzer.ryzergen.registry.ModParticles;
import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.client.PoolGhostPreview;
import com.ryzer.ryzergen.registry.ModBlockEntities;
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
import net.neoforged.neoforge.capabilities.Capabilities;
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

/**
 * The pool controller's block entity. Before the pool forms it builds it: feed its store liner and
 * the crane (by hand or by pipe) and it places them itself, bottom layer first, until the last one
 * forms the pool. Once formed it runs it (design section 7): hot spent fuel comes in at the fuel
 * port into the racks, one item per slot, and cools for a set time while the water covers the
 * racks; decay heat boils the water off, more the more it holds; cooled fuel moves to the output
 * slots, which the output port pushes out and pipes can pull from.
 */
public class PoolControllerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int PART_SLOTS = 9;
    public static final int RACKS = 18;
    public static final int OUTPUTS = 6;
    public static final int CAPACITY = 16_000;
    /** The racks are covered, and fuel cools, from half full. */
    public static final int COVERED = CAPACITY / 2;
    /** Blocks placed per build step, and ticks between steps. */
    private static final int PER_STEP = 3;
    private static final int STEP_TICKS = 2;

    public static final int DATA_MISSING_LINER = 0;
    public static final int DATA_MISSING_CRANE = 1;
    public static final int DATA_BLOCKED = 2;
    public static final int DATA_FORMED = 3;
    public static final int DATA_WATER = 4;
    /** Each rack's cooling, 0 to 1000, from here on. */
    public static final int DATA_PROGRESS = 5;
    public static final int DATA_COUNT = DATA_PROGRESS + RACKS;

    /** Build order: the design's cells bottom layer first, each layer front to back. */
    private static final List<BlockPos> ORDER = new ArrayList<>();

    static {
        for (int index = 1; index <= PoolLayout.CELLS; index++) {
            ORDER.add(PoolLayout.cell(index));
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

    private final ItemStackHandler racks = new ItemStackHandler(RACKS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (getStackInSlot(slot).isEmpty()) {
                cooling[slot] = 0;
            }
            rodsChanged = true;
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return HotFuel.isSpentFuel(stack);
        }
    };

    private final ItemStackHandler output = new ItemStackHandler(OUTPUTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final FluidTank water = new FluidTank(CAPACITY, fluid -> fluid.is(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    /** Ticks each rack's item has cooled. */
    private final int[] cooling = new int[RACKS];
    /** The block event that starts the crane's run; its parameter is the rack slot it serves. */
    public static final int CRANE_EVENT = 1;
    /** The crane runs at most this often, so a rack of fuel cooling at once does not queue up runs. */
    private static final int CRANE_EVERY = 200;
    /** When the crane last ran (server), and when its run started and for which slot (client). */
    private long craneLast = Long.MIN_VALUE / 2;
    private long craneStart = Long.MIN_VALUE / 2;
    private int craneSlot;
    /**
     * What nearby players see of the racks: each slot's cooling, 0 to 1000, or -1 when empty. Kept
     * on both sides: the server sends it when it changes, and the renderer draws the rods from it.
     */
    private final int[] rods = new int[RACKS];
    private boolean rodsChanged = true;
    /** Water still owed to the boil-off, in thousandths of a mB, so small rates add up. */
    private int boilOwed;
    private final int[] missing = new int[2];
    private int blocked;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_MISSING_LINER, DATA_MISSING_CRANE -> missing[index];
                case DATA_BLOCKED -> blocked;
                case DATA_FORMED -> isFormed() ? 1 : 0;
                case DATA_WATER -> water.getFluidAmount();
                default -> progress(index - DATA_PROGRESS);
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

    public PoolControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POOL_CONTROLLER.get(), pos, state);
    }

    /** Which buildable part an item is, or null. The controller itself is never stocked. */
    public static @Nullable PoolPart partFor(ItemStack stack) {
        for (PoolPart part : new PoolPart[] {PoolPart.LINER, PoolPart.CRANE}) {
            if (stack.is(part.block().asItem())) {
                return part;
            }
        }
        return null;
    }

    public boolean isFormed() {
        return PoolPartBlock.isFormed(getBlockState());
    }

    public Direction facing() {
        return getBlockState().getValue(PoolPartBlock.FACING);
    }

    /**
     * Whether the controller sits in its formed place: always once formed, and while rebuilding a
     * pool broken apart (liner below it). Otherwise it sits on the ground below its place.
     */
    public boolean raised() {
        return isFormed() || (level != null && PoolStructure.isRaised(level, worldPosition));
    }

    /** Where a cell of the pool goes, wherever the controller sits now. */
    public BlockPos cellPos(BlockPos cell) {
        return PoolLayout.toWorld(worldPosition, PoolLayout.anchor(raised()), facing(), cell);
    }

    public IFluidHandler water() {
        return water;
    }

    /** How far a rack's item has cooled, 0 to 1000. */
    private int progress(int rack) {
        ItemStack stack = racks.getStackInSlot(rack);
        return stack.isEmpty() ? 0 : Math.min(1000, cooling[rack] * 1000 / HotFuel.coolingTicks(stack));
    }

    /** How the pool should look now: dry, full, or cooling fuel. */
    public PoolLook look() {
        if (water.getFluidAmount() < COVERED) {
            return PoolLook.DRY;
        }
        for (int slot = 0; slot < RACKS; slot++) {
            if (!racks.getStackInSlot(slot).isEmpty()) {
                return PoolLook.ACTIVE;
            }
        }
        return PoolLook.WET;
    }

    // ---------------------------------------------------------------- ports

    /** What pipes see on a port face of a formed pool, or on the controller while it is building. */
    public static @Nullable IItemHandler itemsAt(Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        PoolLayout.Port port = portAt(state, side);
        PoolControllerBlockEntity controller = port == null ? null : PoolPartBlock.controller(level, pos, state);
        if (controller == null) {
            return null;
        }
        return switch (port) {
            case FUEL_IN -> controller.fuelIn;
            case OUTPUT -> controller.fuelOut;
            default -> null;
        };
    }

    public static @Nullable IFluidHandler fluidAt(Level level, BlockPos pos, BlockState state, @Nullable Direction side) {
        PoolControllerBlockEntity controller = portAt(state, side) == PoolLayout.Port.WATER_IN
                ? PoolPartBlock.controller(level, pos, state) : null;
        return controller == null ? null : controller.waterIn;
    }

    /** The port on {@code side} of this block, if it is one on a formed pool. */
    private static @Nullable PoolLayout.Port portAt(BlockState state, @Nullable Direction side) {
        if (side == null || !(state.getBlock() instanceof PoolPartBlock) || !PoolPartBlock.isFormed(state)) {
            return null;
        }
        BlockPos cell = PoolLayout.cell(state.getValue(PoolPartBlock.CELL));
        for (PoolLayout.Port port : PoolLayout.Port.values()) {
            if (port.cell().equals(cell) && port.face(state.getValue(PoolPartBlock.FACING)) == side) {
                return port;
            }
        }
        return null;
    }

    /** The parts store, for pipes, while the pool is still to be built. */
    public @Nullable IItemHandler partsFor(@Nullable Direction side) {
        return isFormed() ? null : parts;
    }

    /** Hot fuel in: into the racks only, nothing out. */
    private final IItemHandler fuelIn = new IItemHandler() {
        @Override
        public int getSlots() {
            return RACKS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return racks.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return racks.insertItem(slot, stack, simulate);
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
            return racks.isItemValid(slot, stack);
        }
    };

    /** Cooled fuel out: from the output slots only, nothing in. */
    private final IItemHandler fuelOut = new IItemHandler() {
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
            return CAPACITY;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, PoolControllerBlockEntity controller) {
        if (PoolPartBlock.isFormed(state)) {
            controller.cool((ServerLevel) level);
            return;
        }
        if (level.getGameTime() % STEP_TICKS == 0) {
            controller.buildStep((ServerLevel) level);
        }
        if (level.getGameTime() % 10 == 0) {
            controller.survey(level);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, PoolControllerBlockEntity controller) {
        PoolGhostPreview.track(controller);
        if (PoolPartBlock.isFormed(state) && state.getValue(PoolPartBlock.LOOK) != PoolLook.DRY) {
            controller.bubble(level);
        }
        // Steam rising off warm water while fuel cools.
        if (state.getValue(PoolPartBlock.LOOK) == PoolLook.ACTIVE && level.random.nextInt(4) == 0) {
            BlockPos crane = PoolLayout.toWorld(pos, controller.facing(), PoolLayout.CRANE);
            double x = crane.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 3.5;
            double z = crane.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 1.8;
            level.addParticle(ParticleTypes.WHITE_SMOKE, x, crane.getY() + 0.4, z, 0, 0.02, 0);
        }
    }

    /**
     * Decay heat boils the water round a hot rod: bubbles rise off its top to the surface, more
     * the hotter it is, and stop once it has cooled.
     */
    private void bubble(Level level) {
        Direction facing = facing();
        double surface = PoolLayout.designToWorld(worldPosition, facing, 0, PoolLayout.WATER_TOP, 0).y;
        for (int slot = 0; slot < RACKS; slot++) {
            int cooled = rods[slot];
            if (cooled < 0 || cooled >= 1000) {
                continue;
            }
            float heat = 1 - cooled / 1000F;
            if (level.random.nextFloat() > 0.35F * heat) {
                continue;
            }
            double x = PoolLayout.RACK_X + (slot % PoolLayout.COLUMNS) * PoolLayout.CELL + 3 + level.random.nextDouble() * 4;
            double z = PoolLayout.RACK_Z + (slot / PoolLayout.COLUMNS) * PoolLayout.CELL + 3 + level.random.nextDouble() * 4;
            net.minecraft.world.phys.Vec3 at = PoolLayout.designToWorld(worldPosition, facing, x, PoolLayout.ROD_TOP, z);
            // The rise to the surface rides in the particle's y speed; it makes its own.
            level.addParticle(ModParticles.POOL_BUBBLE.get(), at.x, at.y, at.z, 0, surface - at.y, 0);
        }
    }

    /** One tick of the running pool. */
    private void cool(ServerLevel level) {
        int hot = 0;
        boolean covered = water.getFluidAmount() >= COVERED;
        for (int slot = 0; slot < RACKS; slot++) {
            ItemStack stack = racks.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            hot++;
            if (!covered) {
                continue;
            }
            if (cooling[slot] < HotFuel.coolingTicks(stack)) {
                cooling[slot]++;
                continue;
            }
            // Cooled: the crane lifts it out into the output slots, if there is room.
            ItemStack cooled = stack.copy();
            HotFuel.cool(cooled);
            if (ItemHandlerHelper.insertItemStacked(output, cooled, true).isEmpty()) {
                ItemHandlerHelper.insertItemStacked(output, cooled, false);
                racks.setStackInSlot(slot, ItemStack.EMPTY);
                level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.4F, 1.2F);
                // The crane lifts it out: players see it run to the rack and carry a rod to the output.
                if (level.getGameTime() - craneLast >= CRANE_EVERY) {
                    craneLast = level.getGameTime();
                    level.blockEvent(worldPosition, getBlockState().getBlock(), CRANE_EVENT, slot);
                }
            }
        }
        boil(hot);
        if (level.getGameTime() % 10 == 0) {
            pushOutput(level);
        }
        // Players see the rods go in and out at once, and their glow fade every second as they cool.
        if (rodsChanged || (hot > 0 && level.getGameTime() % 20 == 0)) {
            rodsChanged = false;
            for (int slot = 0; slot < RACKS; slot++) {
                rods[slot] = racks.getStackInSlot(slot).isEmpty() ? -1 : progress(slot);
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        if (level.getGameTime() % 20 == 0) {
            PoolLook look = look();
            if (getBlockState().getValue(PoolPartBlock.LOOK) != look) {
                PoolStructure.setLook(level, worldPosition, facing(), look);
            }
        }
    }

    /** Decay heat boils the water off: so much per cooling item per tick. */
    private void boil(int hot) {
        boilOwed += hot * Config.get(Config.POOL_WATER_USE) * 1000;
        int mb = boilOwed / 1000;
        if (mb > 0) {
            water.drain(mb, IFluidHandler.FluidAction.EXECUTE);
            boilOwed -= mb * 1000;
        }
    }

    /** Pushes cooled fuel out of the output port into whatever is beside it. */
    private void pushOutput(ServerLevel level) {
        Direction face = PoolLayout.Port.OUTPUT.face(facing());
        BlockPos target = PoolLayout.toWorld(worldPosition, facing(), PoolLayout.Port.OUTPUT.cell()).relative(face);
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, face.getOpposite());
        if (handler == null) {
            return;
        }
        for (int slot = 0; slot < OUTPUTS; slot++) {
            ItemStack stack = output.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack left = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
            output.setStackInSlot(slot, left);
        }
    }

    // ---------------------------------------------------------------- building

    /** Places up to a few parts from the store into their cells, lowest first. */
    private void buildStep(ServerLevel level) {
        int placed = 0;
        boolean raised = raised();
        for (BlockPos cell : ORDER) {
            if (placed >= PER_STEP) {
                return;
            }
            PoolPart part = PoolLayout.partAt(cell, raised);
            if (part == PoolPart.CONTROLLER) {
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
            // The last part forms the pool, which ends the build.
            if (isRemoved() || isFormed()) {
                return;
            }
        }
    }

    private boolean take(PoolPart part) {
        for (int slot = 0; slot < parts.getSlots(); slot++) {
            if (partFor(parts.getStackInSlot(slot)) == part) {
                parts.extractItem(slot, 1, false);
                return true;
            }
        }
        return false;
    }

    /** Counts what is still to place and what is in the way, for the screen. */
    private void survey(Level level) {
        missing[0] = missing[1] = 0;
        blocked = 0;
        boolean raised = raised();
        for (BlockPos cell : ORDER) {
            PoolPart part = PoolLayout.partAt(cell, raised);
            if (part == PoolPart.CONTROLLER) {
                continue;
            }
            BlockState there = level.getBlockState(cellPos(cell));
            if (there.is(part.block())) {
                continue;
            }
            missing[part == PoolPart.LINER ? 0 : 1]++;
            if (!there.canBeReplaced()) {
                blocked++;
            }
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
        drop(level, pos, racks);
        drop(level, pos, output);
    }

    private static void drop(Level level, BlockPos pos, ItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(slot));
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == CRANE_EVENT) {
            if (level != null && level.isClientSide) {
                craneStart = level.getGameTime();
                craneSlot = param;
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    /** Ticks since the crane's run started (client), for its renderer. */
    public float craneTime(float partialTick) {
        return level == null ? Float.MAX_VALUE : level.getGameTime() - craneStart + partialTick;
    }

    /** The rack slot the crane's current run serves. */
    public int craneSlot() {
        return craneSlot;
    }

    /** Each rack slot's cooling as players see it, 0 to 1000, or -1 when the slot is empty. */
    public int rod(int slot) {
        return rods[slot];
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("rods", rods);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        int[] sent = tag.getIntArray("rods");
        System.arraycopy(sent, 0, rods, 0, Math.min(sent.length, RACKS));
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.ryzergen.pool_controller");
    }

    /** Unbuilt, the screen is the parts store; formed, it is the pool's racks. */
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        ContainerLevelAccess access = ContainerLevelAccess.create(level, worldPosition);
        return isFormed() ? new PoolMenu(containerId, inventory, racks, output, data, access)
                : new PoolBuildMenu(containerId, inventory, parts, data, access);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("parts", parts.serializeNBT(registries));
        tag.put("racks", racks.serializeNBT(registries));
        tag.put("output", output.serializeNBT(registries));
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.putIntArray("cooling", cooling);
        tag.putInt("boil_owed", boilOwed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        parts.deserializeNBT(registries, tag.getCompound("parts"));
        racks.deserializeNBT(registries, tag.getCompound("racks"));
        output.deserializeNBT(registries, tag.getCompound("output"));
        water.readFromNBT(registries, tag.getCompound("water"));
        int[] saved = tag.getIntArray("cooling");
        System.arraycopy(saved, 0, cooling, 0, Math.min(saved.length, RACKS));
        boilOwed = tag.getInt("boil_owed");
    }
}
