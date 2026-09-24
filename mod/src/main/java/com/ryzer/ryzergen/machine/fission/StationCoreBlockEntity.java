package com.ryzer.ryzergen.machine.fission;

import com.ryzer.ryzergen.client.StationAlarmSound;
import com.ryzer.ryzergen.client.StationGhostPreview;
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
import net.minecraft.util.RandomSource;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The control core's block entity. Before the station forms it builds it: feed its store the casing,
 * glass and rotor (by hand or by pipe) and it places them itself, bottom layer first, a few at a
 * time, until the last one snaps the station together. Once formed it runs the station
 * ({@link StationRunner}): its panel becomes the station's controls and core grid, its ports take
 * water and fuel and give power, and it anchors the renderer, which shows the rods as loaded.
 */
public class StationCoreBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOTS = 9;
    /** Blocks placed per build step, and ticks between steps. */
    private static final int PER_STEP = 3;
    private static final int STEP_TICKS = 2;

    public static final int DATA_MISSING_CASING = 0;
    public static final int DATA_MISSING_GLASS = 1;
    public static final int DATA_MISSING_ROTOR = 2;
    public static final int DATA_BLOCKED = 3;
    public static final int DATA_FORMED = 4;
    public static final int DATA_COUNT = 5;

    /** The stack's open top, in blocks above the base, and roughly its inside radius. */
    private static final double STACK_TOP = 11;
    private static final double STACK_RADIUS = 4.5;

    /** Build order: the design's spaces bottom layer first, each layer front to back. */
    private static final List<Map.Entry<BlockPos, StationPart>> ORDER = new ArrayList<>(StationLayout.PARTS.entrySet());

    static {
        ORDER.removeIf(entry -> entry.getValue() == StationPart.CORE);
        ORDER.sort(Comparator.<Map.Entry<BlockPos, StationPart>>comparingInt(entry -> entry.getKey().getY())
                .thenComparingInt(entry -> entry.getKey().getZ()).thenComparingInt(entry -> entry.getKey().getX()));
    }

    private final ItemStackHandler parts = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return partFor(stack) != null;
        }
    };

    private final StationRunner runner = new StationRunner(this::runnerChanged);
    private boolean dirty;
    private final int[] missing = new int[3];
    private int blocked;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_FORMED) {
                return isFormed() ? 1 : 0;
            }
            return index == DATA_BLOCKED ? blocked : index < missing.length ? missing[index] : 0;
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

    public StationCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATION_CORE.get(), pos, state);
    }

    /** Which buildable part an item is, or null. The core itself is never stocked. */
    public static @Nullable StationPart partFor(ItemStack stack) {
        for (StationPart part : new StationPart[] {StationPart.CASING, StationPart.GLASS, StationPart.ROTOR}) {
            if (stack.is(part.block().asItem())) {
                return part;
            }
        }
        return null;
    }

    public boolean isFormed() {
        return getBlockState().getValue(StationPartBlock.FORMED);
    }

    public Direction facing() {
        return getBlockState().getValue(StationCoreBlock.FACING);
    }

    public ItemStackHandler parts() {
        return parts;
    }

    /** What pipes see: the parts store, only while the station is still to be built. */
    public @Nullable IItemHandler itemsFor(@Nullable Direction side) {
        return isFormed() ? null : parts;
    }

    /** The centre of the footprint, in world coordinates (the corner between four cells). */
    public double[] centre() {
        BlockPos origin = StationLayout.toWorld(worldPosition, facing(), BlockPos.ZERO);
        BlockPos far = StationLayout.toWorld(worldPosition, facing(), new BlockPos(StationLayout.SIZE - 1, 0, StationLayout.SIZE - 1));
        return new double[] {(Math.min(origin.getX(), far.getX()) + Math.max(origin.getX(), far.getX()) + 1) / 2.0,
                (Math.min(origin.getZ(), far.getZ()) + Math.max(origin.getZ(), far.getZ()) + 1) / 2.0};
    }

    public StationRunner runner() {
        return runner;
    }

    private void runnerChanged() {
        setChanged();
        dirty = true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StationCoreBlockEntity core) {
        if (state.getValue(StationPartBlock.FORMED)) {
            double[] centre = core.centre();
            core.runner.tick((ServerLevel) level, pos, core.facing(), new net.minecraft.world.phys.Vec3(centre[0], pos.getY() + 3, centre[1]));
            // Nearby players see the rods as loaded and the turbine turning.
            if (core.dirty && level.getGameTime() % 10 == 0) {
                core.dirty = false;
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
            return;
        }
        if (level.getGameTime() % STEP_TICKS == 0) {
            core.buildStep((ServerLevel) level);
        }
        if (level.getGameTime() % 10 == 0) {
            core.survey(level);
        }
    }

    /** Places up to a few parts from the store into their spaces, lowest first. */
    private void buildStep(ServerLevel level) {
        int placed = 0;
        for (Map.Entry<BlockPos, StationPart> entry : ORDER) {
            if (placed >= PER_STEP) {
                return;
            }
            BlockPos pos = StationLayout.toWorld(worldPosition, facing(), entry.getKey());
            BlockState there = level.getBlockState(pos);
            if (there.is(entry.getValue().block()) || !there.canBeReplaced() || !take(entry.getValue())) {
                continue;
            }
            level.setBlock(pos, entry.getValue().block().defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.5F, 0.9F + level.random.nextFloat() * 0.2F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
            placed++;
            // The last part forms the station, which ends the build.
            if (isRemoved() || isFormed()) {
                return;
            }
        }
    }

    /** Takes one of a part from the store. */
    private boolean take(StationPart part) {
        for (int slot = 0; slot < parts.getSlots(); slot++) {
            if (partFor(parts.getStackInSlot(slot)) == part) {
                parts.extractItem(slot, 1, false);
                return true;
            }
        }
        return false;
    }

    /** Counts what is still to place and what is in the way, for the panel. */
    private void survey(Level level) {
        missing[0] = missing[1] = missing[2] = 0;
        blocked = 0;
        for (Map.Entry<BlockPos, StationPart> entry : ORDER) {
            BlockState there = level.getBlockState(StationLayout.toWorld(worldPosition, facing(), entry.getKey()));
            if (there.is(entry.getValue().block())) {
                continue;
            }
            missing[entry.getValue().ordinal()]++;
            if (!there.canBeReplaced()) {
                blocked++;
            }
        }
    }

    /** Empties the parts store onto the ground at {@code pos}. */
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < parts.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), parts.getStackInSlot(slot));
            parts.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    /** Everything the core holds, dropped when it is broken: the parts store and the core's rods and blocks. */
    public void dropAll(Level level, BlockPos pos) {
        dropContents(level, pos);
        for (int slot = 0; slot < runner.channels.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), runner.channels.getStackInSlot(slot));
            runner.channels.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    /** The client's alarm, typed loosely so this class never loads client code on a server. */
    public Object clientAlarm;

    /** Degrees the rotor turns each tick at full steam. */
    private static final float ROTOR_SPEED = 8;
    private float rotor;
    private float rotorBefore;
    /** The turbine's speed as the player sees it: eases towards the real one. */
    private float spin;

    public boolean isRunning() {
        return runner.turbine > 0.05F;
    }

    /** The rotor's angle for this frame. */
    public float rotorAngle(float partialTick) {
        return rotorBefore + (rotor - rotorBefore) * partialTick;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, StationCoreBlockEntity core) {
        StationGhostPreview.track(core);
        if (!core.isFormed()) {
            return;
        }
        StationAlarmSound.update(core);
        core.spin += (core.runner.turbine - core.spin) * 0.02F;
        core.rotorBefore = core.rotor;
        core.rotor = (core.rotor + core.spin * ROTOR_SPEED) % 360;
        if (!core.isRunning()) {
            return;
        }
        // Steam pouring up out of the stack.
        RandomSource random = level.random;
        double[] centre = core.centre();
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(random.nextDouble()) * STACK_RADIUS;
            level.addParticle(ParticleTypes.CLOUD, centre[0] + Math.cos(angle) * r, pos.getY() + STACK_TOP - 0.5,
                    centre[1] + Math.sin(angle) * r, (random.nextDouble() - 0.5) * 0.02, 0.12 + random.nextDouble() * 0.08,
                    (random.nextDouble() - 0.5) * 0.02);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.ryzergen.station_core");
    }

    /** Unbuilt, the panel is the parts store; formed, it is the station's controls. */
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        ContainerLevelAccess access = ContainerLevelAccess.create(level, worldPosition);
        return isFormed() ? new StationControlMenu(containerId, inventory, this, access)
                : new StationCoreMenu(containerId, inventory, parts, data, access);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("parts", parts.serializeNBT(registries));
        runner.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        parts.deserializeNBT(registries, tag.getCompound("parts"));
        runner.load(tag, registries);
    }

    /** Clients get the core layout and rods (for the chamber) and whether the turbine is turning. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        runner.save(tag, registries);
        tag.putFloat("turbine", runner.turbine);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        runner.load(tag, registries);
        runner.turbine = tag.getFloat("turbine");
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }
}
