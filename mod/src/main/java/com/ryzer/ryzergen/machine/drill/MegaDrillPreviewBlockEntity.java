package com.ryzer.ryzergen.machine.drill;

import com.ryzer.ryzergen.registry.ModBlockEntities;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The mega drill preview's cycle, run on the server: with a redstone signal it charges (the light runs
 * down the drive shaft), fires (the beam heats the layer it strikes), and the whole layer bursts at
 * once; then it steps down a layer and charges again. It mines the 7 x 7 under the base's opening from the
 * ground it stands on down to bedrock, leaving a 2 x 2 footing under each of its four feet, and throws
 * the drops away (a creative test; the real drill, MegaDrillBlockEntity, keeps them). Clients get only
 * the phase, when it began and how long it lasts, and draw everything else from those (MegaDrillRenderer).
 */
public class MegaDrillPreviewBlockEntity extends BlockEntity implements MegaDrillView {
    /** Ticks to charge, and to heat a layer until it bursts, at 1x; the speed setting divides both. */
    public static final int CHARGE_TICKS = 100;
    public static final int HEAT_TICKS = 80;
    /** The area mined, from the block: 7 blocks one way and 8 the other on each axis, 16 across. */
    public static final int LOW = MegaDrillLayout.LOW;
    public static final int HIGH = MegaDrillLayout.HIGH;
    private static final int UNSET = Integer.MIN_VALUE;

    private DrillPhase phase = DrillPhase.IDLE;
    private long phaseStart;
    private int phaseLength = 1;
    private int layer = UNSET;
    private int speed = 1;
    /** The client's GPU mesh of the design's body (a StationMesh), closed when the block goes. */
    private Object clientMesh;

    public MegaDrillPreviewBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEGA_DRILL_PREVIEW.get(), pos, state);
    }

    /** The legs' footings: a 2 x 2 under each foot pad, at the 9 x 9 base's corners, never mined. */
    public static boolean footing(int dx, int dz) {
        return MegaDrillLayout.footing(dx, dz);
    }

    public DrillPhase phase() {
        return phase;
    }

    /** The layer being mined (or next to be), or the block's own level before it first runs. */
    public int layer() {
        return layer == UNSET ? worldPosition.getY() : layer;
    }

    /** How far through its phase it is, 0 to 1. */
    public float progress(long gameTime, float partialTick) {
        return Mth.clamp((gameTime - phaseStart + partialTick) / phaseLength, 0, 1);
    }

    /** Ticks since its phase began. */
    public float elapsed(long gameTime, float partialTick) {
        return gameTime - phaseStart + partialTick;
    }

    /** Steps the preview's speed through 1x, 2x and 4x (as speed upgrades will); returns the new one. */
    public int cycleSpeed() {
        speed = speed >= 4 ? 1 : speed * 2;
        setChanged();
        return speed;
    }

    /** Whether the position (in the area, on the layer) holds a block the beam bursts. */
    private boolean bursts(BlockGetter level, BlockPos pos, int dx, int dz) {
        if (footing(dx, dz) || (dx == 0 && dz == 0 && pos.getY() == worldPosition.getY())) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !(state.getBlock() instanceof MegaDrillPartBlock) && state.getDestroySpeed(level, pos) >= 0;
    }

    /** Moves down past layers with nothing to burst; false once it is below the world. */
    private boolean findWork(Level level) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (; layer >= level.getMinBuildHeight(); layer--) {
            for (int dx = LOW; dx <= HIGH; dx++) {
                for (int dz = LOW; dz <= HIGH; dz++) {
                    pos.set(worldPosition.getX() + dx, layer, worldPosition.getZ() + dz);
                    if (bursts(level, pos, dx, dz)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void enter(Level level, DrillPhase next, int length) {
        phase = next;
        phaseStart = level.getGameTime();
        phaseLength = Math.max(1, length);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void charge(Level level) {
        if (!findWork(level)) {
            enter(level, DrillPhase.DONE, 1);
            return;
        }
        enter(level, DrillPhase.CHARGING, CHARGE_TICKS / speed);
        level.playSound(null, worldPosition, ModSounds.MEGA_DRILL_CHARGE.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MegaDrillPreviewBlockEntity drill) {
        if (!state.getValue(MegaDrillPreviewBlock.POWERED)) {
            if (drill.phase != DrillPhase.IDLE) {
                drill.enter(level, DrillPhase.IDLE, 1);
            }
            return;
        }
        long now = level.getGameTime();
        switch (drill.phase) {
            case IDLE -> {
                if (drill.layer == UNSET) {
                    drill.layer = pos.getY();
                }
                drill.charge(level);
            }
            case CHARGING -> {
                if (now - drill.phaseStart >= drill.phaseLength) {
                    drill.enter(level, DrillPhase.FIRING, HEAT_TICKS / drill.speed);
                    MegaDrillBlockEntity.playFire(level, pos, drill.speed);
                }
            }
            case FIRING -> {
                if (now - drill.phaseStart >= drill.phaseLength) {
                    drill.burst((ServerLevel) level);
                    drill.layer--;
                    drill.charge(level);
                }
            }
            case DONE -> {
            }
        }
    }

    /** The whole layer bursts at once: every block the beam can break goes, with its own break dust. */
    private void burst(ServerLevel level) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int count = 0;
        for (int dx = LOW; dx <= HIGH; dx++) {
            for (int dz = LOW; dz <= HIGH; dz++) {
                pos.set(worldPosition.getX() + dx, layer, worldPosition.getZ() + dz);
                if (!bursts(level, pos, dx, dz)) {
                    continue;
                }
                // Break dust (and a break sound) from every eighth block keeps the burst affordable.
                if (count++ % 8 == 0) {
                    level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(level.getBlockState(pos)));
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        double x = worldPosition.getX() + 1.0;
        double z = worldPosition.getZ() + 1.0;
        level.sendParticles(ParticleTypes.LAVA, x, layer + 0.6, z, 30, 2.2, 0.2, 2.2, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, layer + 0.6, z, 20, 2.2, 0.4, 2.2, 0.02);
        level.playSound(null, x, layer + 0.5, z, ModSounds.MEGA_DRILL_BURST.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
    }

    /** Sparks at the lens as the charge arrives; once it fires, lava spits and smoke rises off the layer. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, MegaDrillPreviewBlockEntity drill) {
        if (state.getValue(MegaDrillPreviewBlock.POWERED)) {
            MegaDrillBlockEntity.effects(level, drill, pos, drill.layer());
        }
    }

    @Override
    public boolean drillShown() {
        return true;
    }

    @Override
    public Direction drillFacing() {
        return getBlockState().getValue(MegaDrillPreviewBlock.FACING);
    }

    @Override
    public boolean drillRunning() {
        return getBlockState().getValue(MegaDrillPreviewBlock.POWERED);
    }

    @Override
    public DrillPhase drillPhase() {
        return phase;
    }

    @Override
    public float drillProgress(long gameTime, float partialTick) {
        return progress(gameTime, partialTick);
    }

    @Override
    public float drillElapsed(long gameTime, float partialTick) {
        return elapsed(gameTime, partialTick);
    }

    @Override
    public int drillLayer() {
        return layer();
    }

    @Override
    public BlockPos drillCentre() {
        return worldPosition;
    }

    @Override
    public boolean drillHeats(BlockGetter level, BlockPos pos, int dx, int dz) {
        return !pos.equals(worldPosition) && bursts(level, pos, dx, dz);
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        load(tag);
    }

    private void save(CompoundTag tag) {
        tag.putInt("phase", phase.ordinal());
        tag.putLong("phaseStart", phaseStart);
        tag.putInt("phaseLength", phaseLength);
        tag.putInt("layer", layer);
        tag.putInt("speed", speed);
    }

    private void load(CompoundTag tag) {
        phase = DrillPhase.byId(tag.getInt("phase"));
        phaseStart = tag.getLong("phaseStart");
        phaseLength = Math.max(1, tag.getInt("phaseLength"));
        layer = tag.contains("layer") ? tag.getInt("layer") : UNSET;
        speed = Math.max(1, tag.getInt("speed"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        save(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        load(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
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
}
