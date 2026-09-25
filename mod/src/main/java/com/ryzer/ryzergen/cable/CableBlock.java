package com.ryzer.ryzergen.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cable or pipe, in the spirit of Pipez: it joins others of its own kind and any block that
 * offers what it carries. The wrench sets a side to extract (pull out of that block) or disconnects
 * it, and an empty hand opens the panel (stats and fittings) on a cable joined to a machine. Energy cables and
 * item pipes differ only in what they carry, so everything else lives here.
 */
public abstract class CableBlock extends BaseEntityBlock {
    public static final Map<Direction, EnumProperty<CableSide>> SIDES = new EnumMap<>(Direction.class);

    static {
        for (Direction dir : Direction.values()) {
            SIDES.put(dir, EnumProperty.create(dir.getSerializedName(), CableSide.class));
        }
    }

    // 6 x 6 core and arms, the same size as Pipez and Mekanism pipes, and an 8 x 8 extract flange.
    private static final VoxelShape CORE = box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> FLANGES = new EnumMap<>(Direction.class);
    private static final Map<BlockState, VoxelShape> SHAPES = new HashMap<>();

    static {
        ARMS.put(Direction.NORTH, box(5, 5, 0, 11, 11, 5));
        ARMS.put(Direction.SOUTH, box(5, 5, 11, 11, 11, 16));
        ARMS.put(Direction.WEST, box(0, 5, 5, 5, 11, 11));
        ARMS.put(Direction.EAST, box(11, 5, 5, 16, 11, 11));
        ARMS.put(Direction.DOWN, box(5, 0, 5, 11, 5, 11));
        ARMS.put(Direction.UP, box(5, 11, 5, 11, 16, 11));
        FLANGES.put(Direction.NORTH, box(4, 4, 0, 12, 12, 1));
        FLANGES.put(Direction.SOUTH, box(4, 4, 15, 12, 12, 16));
        FLANGES.put(Direction.WEST, box(0, 4, 4, 1, 12, 12));
        FLANGES.put(Direction.EAST, box(15, 4, 4, 16, 12, 12));
        FLANGES.put(Direction.DOWN, box(4, 0, 4, 12, 1, 12));
        FLANGES.put(Direction.UP, box(4, 15, 4, 12, 16, 12));
    }

    protected CableBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (EnumProperty<CableSide> side : SIDES.values()) {
            state = state.setValue(side, CableSide.NONE);
        }
        registerDefaultState(state);
    }

    /** Whether the block at {@code pos} offers what this cable carries on its {@code face}. */
    protected abstract boolean canConnectTo(Level level, BlockPos pos, Direction face);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.computeIfAbsent(state, s -> {
            VoxelShape shape = CORE;
            for (Direction dir : Direction.values()) {
                CableSide side = s.getValue(SIDES.get(dir));
                if (side != CableSide.NONE) {
                    shape = Shapes.or(shape, ARMS.get(dir));
                }
                if (side == CableSide.EXTRACT) {
                    shape = Shapes.or(shape, FLANGES.get(dir));
                }
            }
            return shape;
        });
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return connectSide(state, level, pos, direction);
    }

    /** Works out every side's connection from scratch. */
    public BlockState connect(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            state = connectSide(state, level, pos, dir);
        }
        return state;
    }

    private BlockState connectSide(BlockState state, LevelAccessor level, BlockPos pos, Direction dir) {
        EnumProperty<CableSide> property = SIDES.get(dir);
        CableSide current = state.getValue(property);
        CableSide next;
        if (isDisabled(level, pos, dir)) {
            next = CableSide.NONE;
        } else {
            BlockPos other = pos.relative(dir);
            BlockState neighbour = level.getBlockState(other);
            if (neighbour.is(this)) {
                next = isDisabled(level, other, dir.getOpposite()) ? CableSide.NONE : CableSide.CONNECTED;
            } else if (!(neighbour.getBlock() instanceof CableBlock) && level instanceof Level world
                    && canConnectTo(world, other, dir.getOpposite())) {
                next = current == CableSide.EXTRACT ? CableSide.EXTRACT : CableSide.CONNECTED;
            } else {
                next = CableSide.NONE;
            }
        }
        if (next != current) {
            CableNetwork.changed();
            if (level instanceof Level world && !world.isClientSide) {
                world.invalidateCapabilities(pos);
            }
        }
        return state.setValue(property, next);
    }

    private static boolean isDisabled(LevelAccessor level, BlockPos pos, Direction dir) {
        return level.getBlockEntity(pos) instanceof CableBlockEntity<?> cable && cable.isDisabled(dir);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            CableNetwork.changed();
            if (level.getBlockEntity(pos) instanceof CableBlockEntity<?> cable) {
                cable.dropFittings(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** An empty hand opens the panel for a cable joined to a machine; otherwise, a hint. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            // Holding something (more cable, say): let it place instead of opening the panel.
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof CableBlockEntity<?> cable && !cable.panelSides().isEmpty()) {
                List<Direction> sides = cable.panelSides();
                serverPlayer.openMenu(cable, buffer -> {
                    buffer.writeBlockPos(pos);
                    CableMenu.writeSides(buffer, sides);
                    CableMenu.writeSides(buffer, sides.stream().filter(cable::feeds).toList());
                });
            } else {
                player.displayClientMessage(Component.translatable("message.ryzergen.cable.no_extract"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
