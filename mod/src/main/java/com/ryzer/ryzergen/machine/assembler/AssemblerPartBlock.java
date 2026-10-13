package com.ryzer.ryzergen.machine.assembler;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * An Assembler part: frame or arm (the controller extends this). Alone it shows its own faces.
 * Once the cell forms, CELL says which block of the combined design it draws and WORKING which of
 * the design's two states (lights and belts running), whatever part it is, as the pool's parts do.
 * Placing the last part forms the cell; breaking any part takes it apart.
 */
public class AssemblerPartBlock extends Block {
    public static final MapCodec<AssemblerPartBlock> CODEC = simpleCodec(AssemblerPartBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** 0 for a part standing alone; 1 to 27 for its cell in a formed Assembler (see AssemblerLayout.index). */
    public static final IntegerProperty CELL = IntegerProperty.create("cell", 0, AssemblerLayout.CELLS);
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    public AssemblerPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CELL, 0).setValue(WORKING, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CELL, WORKING);
    }

    public static boolean isFormed(BlockState state) {
        return state.getValue(CELL) > 0;
    }

    /** Formed, a part collides only where the design is drawn: the bay, posts, rim and rack. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isFormed(state) ? AssemblerLayout.shape(state.getValue(CELL), state.getValue(FACING)) : Shapes.block();
    }

    /** Once formed the block draws part of an open model, so it must not hide its neighbours' faces. */
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

    /** The controller of the formed Assembler this block belongs to, worked out from its cell. */
    public static @Nullable AssemblerBlockEntity controller(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof AssemblerPartBlock) || !isFormed(state)) {
            return null;
        }
        BlockPos controller = AssemblerLayout.controllerFrom(pos, state.getValue(FACING), AssemblerLayout.cell(state.getValue(CELL)));
        return level.getBlockEntity(controller) instanceof AssemblerBlockEntity entity ? entity : null;
    }

    /** Any block of the formed Assembler opens the controller's screen. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!isFormed(state)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            AssemblerBlockEntity controller = controller(level, pos, state);
            if (controller != null) {
                player.openMenu(controller, controller.getBlockPos());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this) && !isFormed(state)) {
            AssemblerStructure.tryForm(server, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !newState.is(this) && isFormed(state)) {
            AssemblerStructure.breakApart(server, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
