package com.ryzer.ryzergen.machine.fission;

import net.minecraft.world.inventory.DataSlot;
import com.ryzer.ryzergen.machine.SoundSwitch;
import com.ryzer.ryzergen.advancement.Milestone;
import com.ryzer.ryzergen.registry.ModItems;
import com.ryzer.ryzergen.registry.ModTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.ryzer.ryzergen.machine.RedstoneMode;
import com.ryzer.ryzergen.machine.fission.StationReactor.Channel;
import com.ryzer.ryzergen.registry.ModMenus;
import com.ryzer.ryzergen.machine.SteadyState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The formed station's controls (design section 8, "The core puzzle"): the core's plan, set channel
 * by channel; inject and eject; power, redstone and the safeties; and the live readings. No item
 * slots: parts go in through the input port. Two cores (uranium and MOX), each with its own plan;
 * switching empties the core first. What is in each channel syncs as a small code
 * ({@link #DATA_ITEMS}), enough for the screen to draw it and work out the running core.
 */
public class StationControlMenu extends AbstractContainerMenu {
    public static final int BUTTON_POWER = 0;
    public static final int BUTTON_REDSTONE = 1;
    /** The safety interlock switch: off is overdrive. */
    public static final int BUTTON_SAFETY = 2;
    public static final int BUTTON_INJECT = 3;
    public static final int BUTTON_EJECT = 4;
    /** Pick the other core (after ejecting this one's parts; the screen warns first). */
    public static final int BUTTON_SWITCH_CORE = 5;
    /** Plus channel * 8 + channel type: set that channel's plan. */
    public static final int BUTTON_PLAN = 100;

    public static final int DATA_TEMPERATURE = 0;
    public static final int DATA_STATUS = 1;
    public static final int DATA_OUTPUT_LOW = 2;
    public static final int DATA_OUTPUT_HIGH = 3;
    public static final int DATA_EFFICIENCY = 4;
    public static final int DATA_WATER = 5;
    public static final int DATA_ENERGY_LOW = 6;
    public static final int DATA_ENERGY_HIGH = 7;
    public static final int DATA_ENABLED = 8;
    public static final int DATA_REDSTONE = 9;
    public static final int DATA_SAFETIES = 10;
    /** Seconds left before a flux tilt makes the core unstable. */
    public static final int DATA_TILT = 11;
    /** The steady-state streak (tenths of a load), its bonus (thousandths) and the next milestone (tenths). */
    public static final int DATA_STREAK = 12;
    public static final int DATA_BONUS = 13;
    public static final int DATA_NEXT = 14;
    /** The coolant reserve: seconds of cover its water gives, and its bays (0 for none). */
    public static final int DATA_RESERVE = 15;
    public static final int DATA_RESERVE_BAYS = 16;
    /** What is beside the input port (0 nothing, 1 an inventory, 2 a pipe), and the rods and graphite in it. */
    public static final int DATA_INPUT = 17;
    public static final int DATA_INPUT_FUEL = 18;
    public static final int DATA_INPUT_GRAPHITE = 19;
    /** What is beside the output port, and its free slots. */
    public static final int DATA_OUTPUT_KIND = 20;
    public static final int DATA_OUTPUT_FREE = 21;
    /** Whether the fuel port fills the core (off after an eject until the next inject). */
    public static final int DATA_FEEDING = 22;
    /** The core picked (StationReactor.Core), and the control rods beside the input port. */
    public static final int DATA_CORE = 23;
    public static final int DATA_INPUT_CONTROL = 24;
    /** The picked core's plan, then the other core's (for its tab's score). */
    public static final int DATA_PLAN = 25;
    public static final int DATA_OTHER_PLAN = DATA_PLAN + StationReactor.CHANNELS;
    /** Per channel, what is in it: see {@link #code}. */
    public static final int DATA_ITEMS = DATA_OTHER_PLAN + StationReactor.CHANNELS;
    public static final int DATA_COUNT = DATA_ITEMS + StationReactor.CHANNELS;

    /** A channel's item codes (the low 3 bits); the fuel left, in percent, sits above them. */
    public static final int ITEM_NONE = 0;
    public static final int ITEM_GRAPHITE = 1;
    public static final int ITEM_URANIUM = 2;
    public static final int ITEM_MOX = 3;
    public static final int ITEM_U233 = 4;
    public static final int ITEM_SPENT = 5;
    /** Something the plan no longer takes, waiting for the output port. */
    public static final int ITEM_OTHER = 6;
    public static final int ITEM_CONTROL = 7;

    private final @Nullable StationCoreBlockEntity core;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    /** The machine's sound switch, for the screen's speaker key. */
    private final DataSlot muted;
    /**
     * The last command this player gave and what it did: a count that steps with each command (so
     * the screen sees a repeat), the outcome, parts moved and parts still missing.
     */
    private final DataSlot commandCount = DataSlot.standalone();
    private final DataSlot commandOutcome = DataSlot.standalone();
    private final DataSlot commandMoved = DataSlot.standalone();
    private final DataSlot commandMissing = DataSlot.standalone();

    /** Client side: the readings arrive as synced data. */
    public StationControlMenu(int containerId, Inventory inventory) {
        this(containerId, null, new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public StationControlMenu(int containerId, Inventory inventory, StationCoreBlockEntity core, ContainerLevelAccess access) {
        this(containerId, core, liveData(core.runner()), access);
    }

    private StationControlMenu(int containerId, @Nullable StationCoreBlockEntity core, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.STATION_CONTROL.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.core = core;
        this.data = data;
        this.access = access;
        addDataSlots(data);
        muted = addDataSlot(SoundSwitch.slot(access));
        addDataSlot(commandCount);
        addDataSlot(commandOutcome);
        addDataSlot(commandMoved);
        addDataSlot(commandMissing);
    }

    /** A channel's item as a code for the screen: what it is, and for a fuel rod the fuel left in percent. */
    public static int code(Channel type, ItemStack held, StationReactor.Core core) {
        if (held.isEmpty()) {
            return ITEM_NONE;
        }
        if (StationReactor.leaving(type, held, core)) {
            return StationReactor.isSpent(held) ? ITEM_SPENT : ITEM_OTHER;
        }
        StationReactor.Fuel fuel = StationReactor.Fuel.of(held);
        if (fuel == null) {
            return type == Channel.CONTROL ? ITEM_CONTROL : ITEM_GRAPHITE;
        }
        int left = Math.round(100F * StationReactor.fuelLeft(held) / fuel.life);
        return (ITEM_URANIUM + fuel.ordinal()) | Math.max(0, Math.min(100, left)) << 3;
    }

    private static ContainerData liveData(StationRunner runner) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (index >= DATA_ITEMS) {
                    int channel = index - DATA_ITEMS;
                    ItemStack held = runner.channels.getStackInSlot(channel);
                    if (runner.draining && !held.isEmpty()) {
                        return StationReactor.isSpent(held) ? ITEM_SPENT : ITEM_OTHER;
                    }
                    return code(runner.types[channel], held, runner.core);
                }
                if (index >= DATA_OTHER_PLAN) {
                    return runner.plans[1 - runner.core.ordinal()][index - DATA_OTHER_PLAN].ordinal();
                }
                if (index >= DATA_PLAN) {
                    return runner.types[index - DATA_PLAN].ordinal();
                }
                return switch (index) {
                    case DATA_TEMPERATURE -> Math.round(runner.temperature);
                    case DATA_STATUS -> runner.status.ordinal();
                    case DATA_OUTPUT_LOW -> runner.output & 0xFFFF;
                    case DATA_OUTPUT_HIGH -> runner.output >>> 16;
                    case DATA_EFFICIENCY -> runner.efficiencyPermille;
                    case DATA_WATER -> runner.water.getFluidAmount();
                    case DATA_ENERGY_LOW -> runner.energy().getEnergyStored() & 0xFFFF;
                    case DATA_ENERGY_HIGH -> runner.energy().getEnergyStored() >>> 16;
                    case DATA_ENABLED -> runner.enabled ? 1 : 0;
                    case DATA_REDSTONE -> runner.redstoneMode.ordinal();
                    case DATA_STREAK -> runner.steady.loadsTenths();
                    case DATA_BONUS -> runner.steady.bonusPermille(SteadyState.Tier.STATION);
                    case DATA_NEXT -> runner.steady.nextTenths();
                    case DATA_SAFETIES -> runner.safeties ? 1 : 0;
                    case DATA_TILT -> (runner.tiltTicksLeft() + 19) / 20;
                    case DATA_RESERVE -> runner.reserveSeconds();
                    case DATA_RESERVE_BAYS -> runner.reserveBays;
                    case DATA_INPUT -> runner.inputKind;
                    case DATA_INPUT_FUEL -> Math.min(runner.inputFuel, 9999);
                    case DATA_INPUT_GRAPHITE -> Math.min(runner.inputGraphite, 9999);
                    case DATA_OUTPUT_KIND -> runner.outputKind;
                    case DATA_OUTPUT_FREE -> Math.min(runner.outputFree, 9999);
                    case DATA_FEEDING -> runner.feeding ? 1 : 0;
                    case DATA_CORE -> runner.core.ordinal();
                    case DATA_INPUT_CONTROL -> Math.min(runner.inputControl, 9999);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Read-only on the server.
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    public Channel plan(int channel) {
        return Channel.byId(data.get(DATA_PLAN + channel));
    }

    /** The other core's plan, for its tab. */
    public Channel[] otherPlan() {
        Channel[] plan = new Channel[StationReactor.CHANNELS];
        for (int i = 0; i < plan.length; i++) {
            plan[i] = Channel.byId(data.get(DATA_OTHER_PLAN + i));
        }
        return plan;
    }

    public StationReactor.Core core() {
        return StationReactor.Core.byId(data.get(DATA_CORE));
    }

    public int inputControl() {
        return data.get(DATA_INPUT_CONTROL);
    }

    public Channel[] plan() {
        Channel[] plan = new Channel[StationReactor.CHANNELS];
        for (int i = 0; i < plan.length; i++) {
            plan[i] = plan(i);
        }
        return plan;
    }

    /** What is in channel {@code channel}: one of the ITEM codes. */
    public int item(int channel) {
        return data.get(DATA_ITEMS + channel) & 7;
    }

    /** Fuel left in channel {@code channel}'s rod, 0 to 1 (0 with no live rod). */
    public float fuelLeft(int channel) {
        int code = data.get(DATA_ITEMS + channel);
        return (code & 7) >= ITEM_URANIUM && (code & 7) <= ITEM_U233 ? (code >> 3 & 127) / 100F : 0;
    }

    /** Stand-ins for the core's items, so the screen can work out the running core with the shared sums. */
    public ItemStack[] rods() {
        ItemStack[] rods = new ItemStack[StationReactor.CHANNELS];
        for (int i = 0; i < rods.length; i++) {
            rods[i] = switch (item(i)) {
                case ITEM_GRAPHITE -> new ItemStack(ModItems.GRAPHITE_BLOCK.get());
                case ITEM_URANIUM -> new ItemStack(ModItems.URANIUM_FUEL_ROD.get());
                case ITEM_MOX -> new ItemStack(ModItems.MOX_FUEL_ROD.get());
                case ITEM_U233 -> new ItemStack(ModItems.U233_FUEL_ROD.get());
                case ITEM_SPENT -> new ItemStack(ModItems.SPENT_URANIUM_ROD.get());
                case ITEM_CONTROL -> new ItemStack(ModItems.CONTROL_ROD.get());
                default -> ItemStack.EMPTY;
            };
        }
        return rods;
    }

    private int wide(int low, int high) {
        return (data.get(high) & 0xFFFF) << 16 | (data.get(low) & 0xFFFF);
    }

    public int temperature() {
        return data.get(DATA_TEMPERATURE);
    }

    public StationRunner.Status status() {
        int id = data.get(DATA_STATUS);
        return id >= 0 && id < StationRunner.Status.values().length ? StationRunner.Status.values()[id] : StationRunner.Status.OFFLINE;
    }

    public int streak() {
        return data.get(DATA_STREAK);
    }

    public int bonus() {
        return data.get(DATA_BONUS);
    }

    public int nextMilestone() {
        return data.get(DATA_NEXT);
    }

    public int output() {
        return wide(DATA_OUTPUT_LOW, DATA_OUTPUT_HIGH);
    }

    public int efficiencyPermille() {
        return data.get(DATA_EFFICIENCY);
    }

    public int water() {
        return data.get(DATA_WATER);
    }

    public int energy() {
        return wide(DATA_ENERGY_LOW, DATA_ENERGY_HIGH);
    }

    public boolean enabled() {
        return data.get(DATA_ENABLED) != 0;
    }

    public RedstoneMode redstoneMode() {
        return RedstoneMode.byId(data.get(DATA_REDSTONE));
    }

    public boolean safeties() {
        return data.get(DATA_SAFETIES) != 0;
    }

    public int tiltSeconds() {
        return data.get(DATA_TILT);
    }

    public int reserveSeconds() {
        return data.get(DATA_RESERVE);
    }

    public int reserveBays() {
        return data.get(DATA_RESERVE_BAYS);
    }

    public int inputKind() {
        return data.get(DATA_INPUT);
    }

    public int inputFuel() {
        return data.get(DATA_INPUT_FUEL);
    }

    public int inputGraphite() {
        return data.get(DATA_INPUT_GRAPHITE);
    }

    public int outputKind() {
        return data.get(DATA_OUTPUT_KIND);
    }

    public int outputFree() {
        return data.get(DATA_OUTPUT_FREE);
    }

    public boolean feeding() {
        return data.get(DATA_FEEDING) != 0;
    }

    /** Steps by one with each command this player gives. */
    public int commandCount() {
        return commandCount.get();
    }

    public StationRunner.Command commandOutcome() {
        return StationRunner.Command.byId(commandOutcome.get());
    }

    public int commandMoved() {
        return commandMoved.get();
    }

    public int commandMissing() {
        return commandMissing.get();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (SoundSwitch.click(access, id)) {
            return true;
        }
        if (core == null) {
            return false;
        }
        StationRunner runner = core.runner();
        if (id == BUTTON_POWER) {
            runner.enabled = !runner.enabled;
            core.setChanged();
            return true;
        }
        if (id == BUTTON_SAFETY) {
            runner.toggleSafeties(true);
            if (!runner.safeties && player instanceof ServerPlayer serverPlayer) {
                ModTriggers.MILESTONE.get().trigger(serverPlayer, Milestone.STATION_OVERDRIVE);
            }
            return true;
        }
        if (id == BUTTON_REDSTONE) {
            runner.redstoneMode = runner.redstoneMode.next();
            core.setChanged();
            return true;
        }
        if ((id == BUTTON_INJECT || id == BUTTON_EJECT || id == BUTTON_SWITCH_CORE) && core.getLevel() instanceof ServerLevel level) {
            StationRunner.Result result = switch (id) {
                case BUTTON_INJECT -> runner.inject(level, core.getBlockPos(), core.facing());
                case BUTTON_EJECT -> runner.eject(level, core.getBlockPos(), core.facing());
                default -> runner.switchCore(level, core.getBlockPos(), core.facing(),
                        StationReactor.Core.byId(1 - runner.core.ordinal()));
            };
            commandCount.set((commandCount.get() + 1) & 0x7FFF);
            commandOutcome.set(result.command().ordinal());
            commandMoved.set(result.moved());
            commandMissing.set(result.missing());
            core.setChanged();
            return true;
        }
        int plan = id - BUTTON_PLAN;
        if (plan >= 0 && plan < StationReactor.CHANNELS * 8) {
            ItemStack returned = runner.setType(plan / 8, Channel.byId(plan % 8));
            if (!returned.isEmpty()) {
                player.getInventory().placeItemBackInInventory(returned);
            }
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof StationCoreBlock
                && level.getBlockState(pos).getValue(StationPartBlock.FORMED) && player.distanceToSqr(pos.getCenter()) < 20 * 20, true);
    }

    /** Whether the machine's sound is muted (its screen's speaker key). */
    public boolean muted() {
        return muted.get() != 0;
    }
}
