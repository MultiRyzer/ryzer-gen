package com.ryzer.ryzergen.machine.pool;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * A Spent Fuel Pool part: liner or crane (the controller extends this). Alone it shows its own
 * faces. Once the pool forms, CELL says which block of the combined design it draws and LOOK which
 * of the design's states, whatever part it is (as the microreactor's parts do). Placing the last
 * part forms the pool; breaking any part takes it apart.
 */
public class PoolPartBlock extends Block {
    public static final MapCodec<PoolPartBlock> CODEC = simpleCodec(PoolPartBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** 0 for a part standing alone; 1 to 45 for its cell in a formed pool (see PoolLayout.index). */
    public static final IntegerProperty CELL = IntegerProperty.create("cell", 0, PoolLayout.CELLS);
    public static final EnumProperty<PoolLook> LOOK = EnumProperty.create("look", PoolLook.class);

    public PoolPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CELL, 0)
                .setValue(LOOK, PoolLook.DRY));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CELL, LOOK);
    }

    public static boolean isFormed(BlockState state) {
        return state.getValue(CELL) > 0;
    }

    /**
     * Once formed the block draws part of an open 3D model (water, windows), so it must not hide its
     * neighbours' faces. It keeps a full collision box.
     */
    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return isFormed(state) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return isFormed(state) ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return isFormed(state);
    }

    /** The controller of the formed pool this block belongs to, worked out from its cell. */
    public static @Nullable PoolControllerBlockEntity controller(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof PoolPartBlock) || !isFormed(state)) {
            return null;
        }
        BlockPos controller = PoolLayout.controllerFrom(pos, state.getValue(FACING), PoolLayout.cell(state.getValue(CELL)));
        return level.getBlockEntity(controller) instanceof PoolControllerBlockEntity entity ? entity : null;
    }

    /** A bucket or other fluid container on any block of the formed pool fills its water. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (isFormed(state) && stack.getCapability(Capabilities.FluidHandler.ITEM) != null) {
            PoolControllerBlockEntity controller = controller(level, pos, state);
            if (controller != null && FluidUtil.interactWithFluidHandler(player, hand, controller.water())) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Any block of the formed pool opens the controller's screen. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!isFormed(state)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            PoolControllerBlockEntity controller = controller(level, pos, state);
            if (controller != null) {
                player.openMenu(controller);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this) && !isFormed(state)) {
            PoolStructure.tryForm(server, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !newState.is(this) && isFormed(state)) {
            PoolStructure.breakApart(server, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
