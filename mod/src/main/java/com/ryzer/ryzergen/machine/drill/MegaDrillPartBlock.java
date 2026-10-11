package com.ryzer.ryzergen.machine.drill;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * A mega drill part: the drill frame (the controller extends this). Alone it is a plain block. Once
 * the drill forms, CELL says which cell of the design it fills (see MegaDrillLayout) and FACING
 * which way the drill faces; it stops drawing itself (the controller draws the whole drill) and
 * collides as the design is drawn there, the ladder's cells climbable. Placing the last part forms
 * the drill; breaking any part takes it apart.
 */
public class MegaDrillPartBlock extends Block {
    public static final MapCodec<MegaDrillPartBlock> CODEC = simpleCodec(MegaDrillPartBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** 0 for a part standing alone; 1 and up for its cell in a formed drill (see MegaDrillLayout.cell). */
    public static final IntegerProperty CELL = IntegerProperty.create("cell", 0, MegaDrillLayout.CELLS);
    /** The base's parts while the drill runs: they light the ground round it. */
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public MegaDrillPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CELL, 0).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CELL, LIT);
    }

    public static boolean isFormed(BlockState state) {
        return state.getValue(CELL) > 0;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return isFormed(state) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isFormed(state) ? MegaDrillLayout.shape(state.getValue(CELL), state.getValue(FACING)) : Shapes.block();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return isFormed(state) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    /**
     * Formed, a part is only a place in the structure (the controller draws what is seen), so it lets
     * light straight through: the ground under the drill keeps its daylight, and the light the
     * renderer reads is the open sky's.
     */
    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return isFormed(state);
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return isFormed(state) ? 0 : super.getLightBlock(state, level, pos);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return isFormed(state) ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    /** The ladder up the front of a formed drill climbs like any ladder. */
    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return isFormed(state) && MegaDrillLayout.cell(state.getValue(CELL)).shape().ladder();
    }

    /** The controller of the formed drill this block belongs to, worked out from its cell. */
    public static @Nullable MegaDrillBlockEntity controller(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof MegaDrillPartBlock) || !isFormed(state)) {
            return null;
        }
        BlockPos controller = MegaDrillLayout.controllerFrom(pos, state.getValue(FACING),
                MegaDrillLayout.cell(state.getValue(CELL)).pos());
        return level.getBlockEntity(controller) instanceof MegaDrillBlockEntity entity ? entity : null;
    }

    /** A bucket or other fluid container on any block of the formed drill fills its water. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (isFormed(state) && stack.getCapability(Capabilities.FluidHandler.ITEM) != null) {
            MegaDrillBlockEntity controller = controller(level, pos, state);
            if (controller != null && FluidUtil.interactWithFluidHandler(player, hand, controller.waterPort())) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Any block of the formed drill opens the controller's screen. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!isFormed(state)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            MegaDrillBlockEntity controller = controller(level, pos, state);
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
            MegaDrillStructure.tryForm(server, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !newState.is(this) && isFormed(state)) {
            MegaDrillStructure.breakApart(server, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
