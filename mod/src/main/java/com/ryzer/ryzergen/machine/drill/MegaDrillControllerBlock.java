package com.ryzer.ryzergen.machine.drill;

import com.mojang.serialization.MapCodec;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The mega drill's controller: placed first, on the ground, its face towards the player. The drill
 * is built behind it, the controller front and centre of its base at the foot of the ladder, the pit
 * in the middle. Before the drill forms it builds it from the frame fed into it; once formed it runs
 * the drill and draws it.
 */
public class MegaDrillControllerBlock extends MegaDrillPartBlock implements EntityBlock {
    public static final MapCodec<MegaDrillControllerBlock> CODEC = simpleCodec(MegaDrillControllerBlock::new);

    public MegaDrillControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /** Faces the player, so the drill's front, with its ladder, is the side they see. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** The drill breaks blocks as its owner would, so land claims treat it as theirs. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof MegaDrillBlockEntity controller) {
            controller.setOwner(player.getGameProfile());
        }
    }

    /** Unformed, the controller's own screen is its parts store. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (isFormed(state)) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MegaDrillBlockEntity controller) {
            player.openMenu(controller);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MegaDrillBlockEntity controller) {
            controller.dropAll(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MegaDrillBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.MEGA_DRILL.get()) {
            return null;
        }
        return level.isClientSide
                ? (lvl, pos, st, be) -> MegaDrillBlockEntity.clientTick(lvl, pos, st, (MegaDrillBlockEntity) be)
                : (lvl, pos, st, be) -> MegaDrillBlockEntity.serverTick(lvl, pos, st, (MegaDrillBlockEntity) be);
    }
}
