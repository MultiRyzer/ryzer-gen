package com.ryzer.ryzergen.storage;

import com.mojang.serialization.MapCodec;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * Pressure tank: a steel gas tank with sight glasses. One block on its own is a small tank. Built
 * into a 2 x 2 footprint and stacked up to {@link TankStructure#MAX_HEIGHT} high, the blocks join
 * into one tower that pools its gas, and the steam inside shows through the glass, thicker as the
 * pressure rises. Pipes connect to any outside face. Real basis: a gas receiver with sight glasses.
 */
public class PressureTankBlock extends BaseEntityBlock {
    public static final MapCodec<PressureTankBlock> CODEC = simpleCodec(PressureTankBlock::new);
    public static final EnumProperty<Corner> CORNER = EnumProperty.create("corner", Corner.class);
    public static final EnumProperty<Layer> LAYER = EnumProperty.create("layer", Layer.class);

    /** Where a block sits in the 2 x 2 footprint, or NONE on its own. */
    public enum Corner implements StringRepresentable {
        NONE, NORTH_WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Where a block sits in the tower's height. */
    public enum Layer implements StringRepresentable {
        SINGLE, BOTTOM, MIDDLE, TOP;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** True: holds gases (pressure tank). False: holds liquids ({@link FluidTankBlock}). */
    public boolean holdsGas() {
        return true;
    }

    public PressureTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CORNER, Corner.NONE).setValue(LAYER, Layer.SINGLE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CORNER, LAYER);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            TankStructure.placed(server, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !newState.is(this) && level.getBlockEntity(pos) instanceof PressureTankBlockEntity tank) {
            TankStructure.removed(server, pos, tank);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Right-click shows what the tank (or the whole tower) holds. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PressureTankBlockEntity tank) {
            PressureTankBlockEntity controller = tank.controllerEntity();
            FluidStack contents = controller.contents();
            int capacity = controller.capacity();
            int percent = capacity == 0 ? 0 : (int) ((long) contents.getAmount() * 100 / capacity);
            Component name = contents.isEmpty() ? Component.translatable("message.ryzergen.pressure_tank.empty") : contents.getHoverName();
            player.displayClientMessage(Component.translatable("message.ryzergen.pressure_tank.contents", name,
                    String.format("%,d", contents.getAmount()), String.format("%,d", capacity), percent, controller.blocks()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PressureTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.PRESSURE_TANK.get(), PressureTankBlockEntity::serverTick);
    }
}
