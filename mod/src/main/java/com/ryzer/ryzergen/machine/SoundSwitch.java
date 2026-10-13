package com.ryzer.ryzergen.machine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A machine's sound switch, the speaker key in a tab on its screen's right edge (client.SoundTab).
 * Muted, it goes quiet (a working loop, a hum, a fire's roar, its announcer) but for meltdown
 * alarms: the microreactor's coolant-loss alarm, the station's alarm once it heads for meltdown,
 * and the spoken meltdown warning, so a muted reactor still warns before it melts down. Kept with
 * the block entity and saved with it, and sent to clients in its update tag, as the loops play there.
 */
public final class SoundSwitch {
    /** The menu button that toggles it, the same on every machine's menu and clear of their own. */
    public static final int BUTTON = 90;

    private final BlockEntity owner;
    private boolean muted;

    public SoundSwitch(BlockEntity owner) {
        this.owner = owner;
    }

    public boolean muted() {
        return muted;
    }

    /** Server: mutes or unmutes it, saved and sent on to clients. */
    public void toggle() {
        muted = !muted;
        owner.setChanged();
        Level level = owner.getLevel();
        if (level != null) {
            level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Into the block entity's saved data, and its update tag. */
    public void save(CompoundTag tag) {
        tag.putBoolean("muted", muted);
    }

    public void load(CompoundTag tag) {
        muted = tag.getBoolean("muted");
    }

    /**
     * The menu's slot for it, so the screen's key shows it: on the server it reads the machine at
     * the menu's position, on the client (no position) it holds what the server sends.
     */
    public static DataSlot slot(ContainerLevelAccess access) {
        if (access == ContainerLevelAccess.NULL) {
            return DataSlot.standalone();
        }
        return new DataSlot() {
            @Override
            public int get() {
                return access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof Holder holder && holder.sound().muted())
                        .orElse(false) ? 1 : 0;
            }

            @Override
            public void set(int value) {
            }
        };
    }

    /** A menu's button press: toggles the machine's switch if it is this button. True if it was. */
    public static boolean click(ContainerLevelAccess access, int id) {
        if (id != BUTTON) {
            return false;
        }
        access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof Holder holder) {
                holder.sound().toggle();
            }
        });
        return true;
    }

    /** A machine block entity with a sound switch. */
    public interface Holder {
        SoundSwitch sound();
    }
}
