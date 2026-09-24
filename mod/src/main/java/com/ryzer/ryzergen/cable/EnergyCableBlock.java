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
 * Energy cable, in the spirit of Pipez: it joins other energy cables and any block that holds
 * energy on its own. Pushed energy is passed along to every connected machine. See {@link CableBlock}.
 */
public class EnergyCableBlock extends CableBlock {
    public static final MapCodec<EnergyCableBlock> CODEC = simpleCodec(EnergyCableBlock::new);

    public EnergyCableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canConnectTo(Level level, BlockPos pos, Direction face) {
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, face) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Every cable ticks, not just extractors: one per network (its leader) draws on the batteries.
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.ENERGY_CABLE.get(), EnergyCableBlockEntity::serverTick);
    }
}
