package com.ryzer.ryzergen.storage;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import com.ryzer.ryzergen.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * One pressure tank or fluid tank block (they differ only in holding gases or liquids). In a tower,
 * one block (the lowest north-west one) is the controller and holds all the contents; the others
 * point to it, so pipes on any face reach the same tank. On its own, a block is its own controller
 * with one block's capacity. {@link TankStructure} sets these up.
 */
public class PressureTankBlockEntity extends BlockEntity {
    private final boolean gas;
    private final FluidTank tank = new FluidTank(1, stack -> ModFluids.isGas(stack) == holdsGas()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            dirty = true;
        }
    };
    /** The controller, or null when this block is one. */
    private @Nullable BlockPos controller;
    private int blocks = 1;
    private int height = 1;
    private boolean tower;
    private boolean dirty;

    public PressureTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRESSURE_TANK.get(), pos, state);
        gas = !(state.getBlock() instanceof PressureTankBlock tankBlock) || tankBlock.holdsGas();
        tank.setCapacity(perBlock());
    }

    public boolean holdsGas() {
        return gas;
    }

    /** One block's share: gases pack in tighter, as they are held under pressure. */
    public int perBlock() {
        return Config.get(gas ? Config.TANK_CAPACITY : Config.FLUID_TANK_CAPACITY);
    }

    public boolean isController() {
        return controller == null;
    }

    /** The block holding this tank's gas: itself, or its tower's controller. */
    public PressureTankBlockEntity controllerEntity() {
        if (controller != null && level != null && level.getBlockEntity(controller) instanceof PressureTankBlockEntity main && main.isController()) {
            return main;
        }
        return this;
    }

    /** What pipes see on any face: the controller's tank (gases only, or liquids only). */
    public @Nullable IFluidHandler fluidFor(@Nullable Direction side) {
        return controllerEntity().tank;
    }

    public FluidStack contents() {
        return tank.getFluid();
    }

    public int capacity() {
        return tank.getCapacity();
    }

    public int blocks() {
        return blocks;
    }

    /** Tower height in blocks, for the renderer. */
    public int height() {
        return height;
    }

    /** True for a 2 x 2 tower, false for a block on its own. */
    public boolean isTower() {
        return tower;
    }

    /** Empties this block's tank and hands back what was in it, while the structure is rebuilt. */
    FluidStack takeContents() {
        FluidStack out = tank.getFluid().copy();
        tank.setFluid(FluidStack.EMPTY);
        return out;
    }

    void becomeController(int blocks, int height, boolean tower) {
        this.controller = null;
        this.blocks = blocks;
        this.height = height;
        this.tower = tower;
        tank.setCapacity(blocks * perBlock());
        sync();
    }

    void becomeMember(BlockPos controller) {
        this.controller = controller;
        this.blocks = 1;
        this.height = 1;
        this.tower = true;
        tank.setFluid(FluidStack.EMPTY);
        tank.setCapacity(perBlock());
        sync();
    }

    /** Adds gas during a rebuild. Whatever does not fit escapes. */
    void give(FluidStack stack) {
        if (!stack.isEmpty()) {
            tank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void sync() {
        setChanged();
        dirty = false;
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Sends the fill level to nearby clients now and then, for the sight glass. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, PressureTankBlockEntity tank) {
        if (tank.dirty && level.getGameTime() % 10 == 0) {
            tank.sync();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("capacity", tank.getCapacity());
        if (controller != null) {
            tag.putLong("controller", controller.asLong());
        }
        tag.putInt("blocks", blocks);
        tag.putInt("height", height);
        tag.putBoolean("tower", tower);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.setCapacity(tag.contains("capacity") ? tag.getInt("capacity") : perBlock());
        tank.readFromNBT(registries, tag.getCompound("tank"));
        controller = tag.contains("controller") ? BlockPos.of(tag.getLong("controller")) : null;
        blocks = Math.max(1, tag.getInt("blocks"));
        height = Math.max(1, tag.getInt("height"));
        tower = tag.getBoolean("tower");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
