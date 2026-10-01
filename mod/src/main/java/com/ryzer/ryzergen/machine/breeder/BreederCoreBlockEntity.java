package com.ryzer.ryzergen.machine.breeder;

import com.ryzer.ryzergen.client.BreederGhostPreview;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The breeder's control core. Before the reactor forms it builds it: feed its store the frame and
 * shell (by hand or by pipe) and it places them itself, bottom layer first, a few at a time, until
 * the last one snaps the reactor together. Once formed it anchors the renderer, which draws the
 * whole reactor and turns its beacon. The reactor itself (the core grid, the blanket, the sodium
 * loop) comes next; for now a formed breeder is the building, ready for it, and a redstone signal
 * on the control core stands in for it running: its lights come on, the lantern lights the ground
 * round it and the beacon turns.
 */
public class BreederCoreBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOTS = 9;
    /** Blocks placed per build step, and ticks between steps. */
    private static final int PER_STEP = 3;
    private static final int STEP_TICKS = 2;

    public static final int DATA_MISSING_FRAME = 0;
    public static final int DATA_MISSING_SHELL = 1;
    public static final int DATA_BLOCKED = 2;
    public static final int DATA_FORMED = 3;
    public static final int DATA_COUNT = 4;

    /** Build order: the design's spaces bottom layer first, each layer front to back. */
    private static final List<Map.Entry<BlockPos, BreederPart>> ORDER = new ArrayList<>(BreederLayout.PARTS.entrySet());

    static {
        ORDER.removeIf(entry -> entry.getValue() == BreederPart.CORE);
        ORDER.sort(Comparator.<Map.Entry<BlockPos, BreederPart>>comparingInt(entry -> entry.getKey().getY())
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

    private final int[] missing = new int[2];
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

    public BreederCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BREEDER_CORE.get(), pos, state);
    }

    /** Which buildable part an item is, or null. The core itself is never stocked. */
    public static @Nullable BreederPart partFor(ItemStack stack) {
        for (BreederPart part : new BreederPart[] {BreederPart.FRAME, BreederPart.SHELL}) {
            if (stack.is(part.block().asItem())) {
                return part;
            }
        }
        return null;
    }

    public boolean isFormed() {
        return getBlockState().getValue(BreederPartBlock.FORMED);
    }

    public Direction facing() {
        return getBlockState().getValue(BreederCoreBlock.FACING);
    }

    /** What pipes see: the parts store, only while the reactor is still to be built. */
    public @Nullable IItemHandler itemsFor(@Nullable Direction side) {
        return isFormed() ? null : parts;
    }

    /** Whether the reactor runs. Until its reactor logic lands, a redstone signal on the core. */
    public boolean isRunning() {
        return isFormed() && level != null && level.hasNeighborSignal(worldPosition);
    }

    /** What the lantern's parts last showed; null after loading, so they are set once. */
    private @Nullable Boolean lit;

    /**
     * Lights (or darkens) the shell parts round the lantern and the frame on the ground (the consoles
     * and the legs' feet, where the floodlights are), so the reactor lights the ground round it.
     */
    private void lightLantern(Level level, boolean shine) {
        for (Map.Entry<BlockPos, BreederPart> entry : BreederLayout.PARTS.entrySet()) {
            boolean lantern = entry.getValue() == BreederPart.SHELL && entry.getKey().getY() >= BreederLayout.LANTERN_Y;
            boolean ground = entry.getValue() == BreederPart.FRAME && entry.getKey().getY() == 0;
            if (!lantern && !ground) {
                continue;
            }
            BlockPos at = BreederLayout.toWorld(worldPosition, facing(), entry.getKey());
            BlockState part = level.getBlockState(at);
            if (part.is(entry.getValue().block()) && part.getValue(BreederPartBlock.FORMED)
                    && part.getValue(BreederPartBlock.LIT) != shine) {
                level.setBlock(at, part.setValue(BreederPartBlock.LIT, shine), Block.UPDATE_CLIENTS);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BreederCoreBlockEntity core) {
        if (state.getValue(BreederPartBlock.FORMED)) {
            boolean shine = core.isRunning();
            if (core.lit == null || core.lit != shine) {
                core.lit = shine;
                core.lightLantern(level, shine);
            }
            return;
        }
        core.lit = null;
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
        for (Map.Entry<BlockPos, BreederPart> entry : ORDER) {
            if (placed >= PER_STEP) {
                return;
            }
            BlockPos pos = BreederLayout.toWorld(worldPosition, facing(), entry.getKey());
            BlockState there = level.getBlockState(pos);
            if (there.is(entry.getValue().block()) || !there.canBeReplaced() || !take(entry.getValue())) {
                continue;
            }
            level.setBlock(pos, entry.getValue().block().defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.5F, 0.9F + level.random.nextFloat() * 0.2F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
            placed++;
            // The last part forms the reactor, which ends the build.
            if (isRemoved() || isFormed()) {
                return;
            }
        }
    }

    /** Takes one of a part from the store. */
    private boolean take(BreederPart part) {
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
        missing[0] = missing[1] = 0;
        blocked = 0;
        for (Map.Entry<BlockPos, BreederPart> entry : ORDER) {
            BlockState there = level.getBlockState(BreederLayout.toWorld(worldPosition, facing(), entry.getKey()));
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

    // ---------------------------------------------------------------- client

    /** The beacon's turn, degrees per tick: about one sweep every three seconds. */
    private static final float BEACON_SPEED = 6;
    private float beacon;
    private float beaconBefore;
    /** The client's GPU mesh of the reactor's body (see StationMesh), closed when the core goes. */
    public Object clientMesh;

    public static void clientTick(Level level, BlockPos pos, BlockState state, BreederCoreBlockEntity core) {
        BreederGhostPreview.track(core);
        core.beaconBefore = core.beacon;
        if (core.isRunning()) {
            core.beacon = (core.beacon + BEACON_SPEED) % 360;
            if (core.beacon < core.beaconBefore) {
                core.beaconBefore -= 360;
            }
        }
    }

    /** The beacon's angle this frame, in degrees. */
    public float beaconAngle(float partialTick) {
        return beaconBefore + (beacon - beaconBefore) * partialTick;
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.ryzergen.breeder_core");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BreederCoreMenu(containerId, inventory, parts, data, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("parts", parts.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        parts.deserializeNBT(registries, tag.getCompound("parts"));
    }
}
