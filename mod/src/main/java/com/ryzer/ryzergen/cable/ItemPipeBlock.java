package com.ryzer.ryzergen.cable;

import com.mojang.serialization.MapCodec;
import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

/**
 * Item pipe, the energy cable's twin: it joins other item pipes and any block with an inventory.
 * An extract side pulls items out of its block and the network delivers them to the others, taking
 * turns. Items pushed in (by a hopper, say) are passed along the same way. See {@link CableBlock}.
 */
public class ItemPipeBlock extends CableBlock {
    public static final MapCodec<ItemPipeBlock> CODEC = simpleCodec(ItemPipeBlock::new);

    public ItemPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canConnectTo(Level level, BlockPos pos, Direction face) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Only pipes that pull need to tick: pushed items are passed on as they arrive.
        if (level.isClientSide || !state.getValues().containsValue(CableSide.EXTRACT)) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.ITEM_PIPE.get(), ItemPipeBlockEntity::serverTick);
    }
}
