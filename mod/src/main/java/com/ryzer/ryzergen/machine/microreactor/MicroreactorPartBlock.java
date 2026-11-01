package com.ryzer.ryzergen.machine.microreactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * One of the four microreactor blocks: heart, machine unit or coolant jacket.
 * Alone it shows its own faces. Once the structure forms, SLOT says which quarter of the machine
 * it is, whatever part it is, and the block itself draws nothing: the heart's renderer draws the
 * whole machine (MicroreactorRenderer), round jar and moving parts included.
 */
public class MicroreactorPartBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<MicroreactorPartBlock> CODEC = simpleCodec(MicroreactorPartBlock::new);
    public static final EnumProperty<MicroreactorSlot> SLOT = EnumProperty.create("slot", MicroreactorSlot.class);
    /** Set on all four blocks by the heart while the reactor runs: glow, light, hum and steam. */
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    public MicroreactorPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SLOT, MicroreactorSlot.NONE).setValue(RUNNING, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SLOT, RUNNING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Formed, the heart's renderer draws the machine; alone, the part shows its own model. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(SLOT).isFormed() ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    /**
     * Once formed the machine is an open 3D shape, so a block must not hide its neighbours' faces
     * or block light from the machine. It keeps a full collision box.
     */
    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(SLOT).isFormed() ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(SLOT).isFormed() ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    /** A bucket or other fluid container on any block of the formed machine fills the coolant tank. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(SLOT).isFormed() && stack.getCapability(Capabilities.FluidHandler.ITEM) != null) {
            ReactorHeartBlockEntity heart = MicroreactorStructure.findHeart(level, pos, state);
            if (heart != null && FluidUtil.interactWithFluidHandler(player, hand, heart.coolantInput())) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Any block of the formed machine opens the control panel. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(SLOT).isFormed()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ReactorHeartBlockEntity heart = MicroreactorStructure.findHeart(level, pos, state);
            if (heart != null) {
                player.openMenu(heart);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Check for a complete structure a tick later, so every way of placing a part is covered. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(state.getBlock()) && !state.getValue(SLOT).isFormed()) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SLOT).isFormed()) {
            MicroreactorStructure.tryForm(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && state.getValue(SLOT).isFormed() && level instanceof ServerLevel server) {
            MicroreactorStructure.breakApart(server, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
