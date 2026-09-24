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
 * Fluid pipe: carries liquids (water, lava, anything that is not a gas) between tanks and machines.
 * The gas pipe ({@link GasPipeBlock}) is the same pipe for gases. See {@link CableBlock}.
 */
public class FluidPipeBlock extends CableBlock {
    public static final MapCodec<FluidPipeBlock> CODEC = simpleCodec(FluidPipeBlock::new);

    public FluidPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** True for the gas pipe. */
    public boolean carriesGas() {
        return false;
    }

    @Override
    protected boolean canConnectTo(Level level, BlockPos pos, Direction face) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return carriesGas() ? FluidPipeBlockEntity.gas(pos, state) : FluidPipeBlockEntity.liquid(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Only pipes that pull need to tick: pushed fluid is passed on as it arrives.
        if (level.isClientSide || !state.getValues().containsValue(CableSide.EXTRACT)) {
            return null;
        }
        return createTickerHelper(type, carriesGas() ? ModBlockEntities.GAS_PIPE.get() : ModBlockEntities.FLUID_PIPE.get(),
                FluidPipeBlockEntity::serverTick);
    }
}
