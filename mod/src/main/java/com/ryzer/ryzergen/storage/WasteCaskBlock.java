package com.ryzer.ryzergen.storage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Waste Cask (design section 7): a thick steel drum for fission waste. Real basis: dry cask
 * storage. Waste gives off no radiation in the mod (rule 10), so the cask is about tidiness, never
 * survival. Waste is a solid item (real high-level waste is set in glass). Right-click with waste to put
 * it in, or with an empty hand to see how full it is; a gauge on its face shows the same at a glance.
 */
public class WasteCaskBlock extends BaseEntityBlock {
    public static final MapCodec<WasteCaskBlock> CODEC = simpleCodec(WasteCaskBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final int FILL_STEPS = 4;
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, FILL_STEPS);

    public WasteCaskBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILL, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FILL);
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

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WasteCaskBlockEntity(pos, state);
    }

    /** Right-click with waste in hand to put it in (the whole stack, as far as it fits). */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.FISSION_WASTE.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof WasteCaskBlockEntity cask) {
            ItemStack left = ItemHandlerHelper.insertItem(cask.items(), stack.copy(), false);
            int moved = stack.getCount() - left.getCount();
            if (moved > 0) {
                stack.shrink(moved);
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 0.8F);
            }
            player.displayClientMessage(Component.translatable(moved > 0 ? "message.ryzergen.waste_cask.stored"
                            : "message.ryzergen.waste_cask.full",
                    String.format("%,d", cask.stored()), String.format("%,d", WasteCaskBlockEntity.CAPACITY)), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof WasteCaskBlockEntity cask) {
            player.displayClientMessage(Component.translatable("message.ryzergen.waste_cask.stored",
                    String.format("%,d", cask.stored()), String.format("%,d", WasteCaskBlockEntity.CAPACITY)), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** In creative nothing drops, so a cask with waste in it drops itself, contents and all, as a shulker box does. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof WasteCaskBlockEntity cask
                && cask.stored() > 0) {
            ItemStack stack = new ItemStack(this);
            stack.applyComponents(cask.collectComponents());
            ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int stored = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().mapToInt(ItemStack::getCount).sum();
        tooltip.add(Component.translatable("tooltip.ryzergen.waste_cask.stored",
                String.format("%,d", stored), String.format("%,d", WasteCaskBlockEntity.CAPACITY)).withStyle(style -> style.withColor(0xFF9AA3AE)));
    }
}
