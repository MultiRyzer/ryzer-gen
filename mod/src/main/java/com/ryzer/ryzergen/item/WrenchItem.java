package com.ryzer.ryzergen.item;

import com.ryzer.ryzergen.cable.CableNetwork;
import com.ryzer.ryzergen.cable.CableSide;
import com.ryzer.ryzergen.cable.EnergyCableBlock;
import com.ryzer.ryzergen.cable.EnergyCableBlockEntity;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterBlock;
import com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Configures cables the way Pipez does: right-click a side to switch it between delivering and
 * extracting, shift-right-click to disconnect or reconnect it. Also turns the alloy smelters.
 */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (state.getBlock() instanceof EnergyCableBlock cable) {
            if (!level.isClientSide) {
                Direction side = clickedSide(context);
                boolean sneaking = player != null && player.isShiftKeyDown();
                Component message = sneaking ? toggleConnection(cable, level, pos, side) : toggleExtract(state, level, pos, side);
                if (message != null && player != null) {
                    player.displayClientMessage(message, true);
                }
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.4F, 1.6F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getBlock() instanceof AlloySmelterBlock || state.getBlock() instanceof ElectricAlloySmelterBlock) {
            if (!level.isClientSide) {
                Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
                level.setBlock(pos, state.setValue(HorizontalDirectionalBlock.FACING, facing.getClockWise()), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.4F, 1.6F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /**
     * The side the player meant: the arm they clicked, found from where they hit relative to the
     * centre, or the face of the core if they clicked the middle.
     */
    private static Direction clickedSide(UseOnContext context) {
        Vec3 hit = context.getClickLocation().subtract(Vec3.atCenterOf(context.getClickedPos()));
        double ax = Math.abs(hit.x);
        double ay = Math.abs(hit.y);
        double az = Math.abs(hit.z);
        double max = Math.max(ax, Math.max(ay, az));
        if (max <= 3.5 / 16) {
            return context.getClickedFace();
        }
        if (max == ax) {
            return hit.x > 0 ? Direction.EAST : Direction.WEST;
        }
        if (max == ay) {
            return hit.y > 0 ? Direction.UP : Direction.DOWN;
        }
        return hit.z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static Component toggleConnection(EnergyCableBlock cable, Level level, BlockPos pos, Direction side) {
        if (!(level.getBlockEntity(pos) instanceof EnergyCableBlockEntity entity)) {
            return null;
        }
        boolean disconnect = !entity.isDisabled(side);
        entity.setDisabled(side, disconnect);
        // Disconnecting from another cable disconnects both ends, so they stay apart.
        BlockPos other = pos.relative(side);
        if (level.getBlockEntity(other) instanceof EnergyCableBlockEntity neighbour) {
            neighbour.setDisabled(side.getOpposite(), disconnect);
            BlockState otherState = level.getBlockState(other);
            level.setBlock(other, cable.connect(otherState, level, other), Block.UPDATE_ALL);
        }
        level.setBlock(pos, cable.connect(level.getBlockState(pos), level, pos), Block.UPDATE_ALL);
        return Component.translatable(disconnect ? "message.ryzergen.cable.disconnected" : "message.ryzergen.cable.connected");
    }

    private static Component toggleExtract(BlockState state, Level level, BlockPos pos, Direction side) {
        CableSide current = state.getValue(EnergyCableBlock.SIDES.get(side));
        if (current == CableSide.NONE || level.getBlockState(pos.relative(side)).getBlock() instanceof EnergyCableBlock) {
            return Component.translatable("message.ryzergen.cable.no_machine");
        }
        CableSide next = current == CableSide.EXTRACT ? CableSide.CONNECTED : CableSide.EXTRACT;
        level.setBlock(pos, state.setValue(EnergyCableBlock.SIDES.get(side), next), Block.UPDATE_ALL);
        CableNetwork.changed();
        level.invalidateCapabilities(pos);
        return Component.translatable(next == CableSide.EXTRACT ? "message.ryzergen.cable.extract" : "message.ryzergen.cable.insert");
    }
}
