package com.ryzer.ryzergen.battery.container;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * A Container Battery part: frame or thermal unit (the controller extends this). Alone it shows its
 * own faces. Once the container forms, CELL says which block of the combined design it draws; a
 * front block with a rack module in its slot is INSTALLED and draws that face as a lit, glazed door
 * (design section 12). Right-click a slot with an LFP rack to install it; shift-right-click it with
 * an empty hand to take the rack out, with its share of the charge.
 */
public class ContainerPartBlock extends Block {
    public static final MapCodec<ContainerPartBlock> CODEC = simpleCodec(ContainerPartBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty CELL = IntegerProperty.create("cell", 0, ContainerLayout.CELLS);
    public static final BooleanProperty INSTALLED = BooleanProperty.create("installed");

    public ContainerPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CELL, 0).setValue(INSTALLED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CELL, INSTALLED);
    }

    public static boolean isFormed(BlockState state) {
        return state.getValue(CELL) > 0;
    }

    /** Formed, the block draws part of an open design, so it must not hide its neighbours' faces. */
    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return isFormed(state) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return isFormed(state) ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    /** The controller of the formed container this block belongs to, worked out from its cell. */
    public static @Nullable BatteryControllerBlockEntity controller(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ContainerPartBlock) || !isFormed(state)) {
            return null;
        }
        BlockPos controller = ContainerLayout.controllerFrom(pos, state.getValue(FACING), ContainerLayout.cell(state.getValue(CELL)));
        return level.getBlockEntity(controller) instanceof BatteryControllerBlockEntity entity ? entity : null;
    }

    /** The module slot this block holds, or -1. */
    public static int slot(BlockState state) {
        return isFormed(state) ? ContainerLayout.slotAt(ContainerLayout.cell(state.getValue(CELL))) : -1;
    }

    /** A rack into an empty slot, or coolant from a bucket into the fan unit's tank. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        BatteryControllerBlockEntity controller = controller(level, pos, state);
        if (controller == null) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        int slot = slot(state);
        if (stack.getItem() instanceof LfpRackItem && slot >= 0 && !state.getValue(INSTALLED)) {
            if (!level.isClientSide) {
                controller.install(slot, stack);
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 1.3F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getCapability(Capabilities.FluidHandler.ITEM) != null
                && FluidUtil.interactWithFluidHandler(player, hand, controller.coolant())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Shift and an empty hand on an installed slot takes its rack out; otherwise, the controller's screen. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!isFormed(state)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            BatteryControllerBlockEntity controller = controller(level, pos, state);
            int slot = slot(state);
            if (controller != null && player.isShiftKeyDown() && slot >= 0 && state.getValue(INSTALLED)) {
                ItemStack rack = controller.remove(slot);
                if (!player.getInventory().add(rack)) {
                    player.drop(rack, false);
                }
                level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.3F);
            } else if (controller != null) {
                player.openMenu(controller);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this) && !isFormed(state)) {
            ContainerStructure.tryForm(server, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !newState.is(this) && isFormed(state)) {
            ContainerStructure.breakApart(server, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
