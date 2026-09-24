package com.ryzer.ryzergen.machine.fission;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import com.ryzer.ryzergen.machine.fission.StationReactor.Channel;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The fission station's control screen. Left, the core grid: pick a plan tool (fuel, moderator,
 * control rod, coolant) and click channels to plan them, then put rods and blocks in. Right-click
 * a channel to take its item out, and again to clear its plan. Each plan has its own colour, as a
 * soft tint; uncooled rods and overloaded coolant flash red.
 * Right, the readout: live output above the line, the plan's forecast below it (power, settled
 * temperature and efficiency, rod life, and a rating against the best layouts found). Then the
 * heat, water and energy bars, the safety interlock switch and the power and redstone keys. With
 * the safeties off (shift-click) the plan and readout show overdrive. Positions match
 * art/tools/gui_textures.py (station_control).
 */
public class StationControlScreen extends AbstractContainerScreen<StationControlMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/station_control.png");
    private static final int LABEL = 0xFF2B3036;
    private static final int VALUE = 0xFF35C8F5;
    private static final int DIM = 0xFF9AA3AE;
    private static final int GOOD = 0xFF44D65E;
    private static final int WARN = 0xFFF0A030;
    private static final int BAD = 0xFFE0503C;
    private static final int TEXT_X = 104;
    private static final int BAR_X = 104;
    private static final int BAR_W = 104;
    private static final int HEAT_Y = 84;
    private static final int WATER_Y = 92;
    private static final int ENERGY_Y = 100;
    private static final int TOOL_Y = 110;
    private static final int POWER_X = 172;
    private static final int REDSTONE_X = 190;
    private static final int KEYS_Y = 108;
    private static final int SAFETY_X = 104;
    private static final int SAFETY_Y = 109;
    private static final int SAFETY_W = 34;
    private static final int SAFETY_H = 14;
    private static final int SAFETY_ARMED_U = 0;
    private static final int SAFETY_DISARMED_U = 34;
    private static final int SAFETY_V = 240;
    /** Sprites, below the panel in the texture. */
    private static final int KEY_U = 0;
    private static final int KEY_HOVER_U = 16;
    private static final int POWER_U = 32;
    private static final int SPRITE_V = 224;
    private static final Channel[] TOOLS = {Channel.FUEL, Channel.MODERATOR, Channel.CONTROL, Channel.COOLANT};

    /** The plan tool picked, or null to handle items normally. */
    private @Nullable Channel tool;

    public StationControlScreen(StationControlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = StationControlMenu.WIDTH;
        imageHeight = 222;
        inventoryLabelX = StationControlMenu.INVENTORY_X;
        inventoryLabelY = StationControlMenu.INVENTORY_Y - 11;
    }

    private static int toolX(int index) {
        return 8 + index * 18;
    }

    private static ItemStack toolIcon(Channel channel) {
        return new ItemStack(switch (channel) {
            case FUEL, EMPTY -> ModItems.URANIUM_FUEL_ROD.get();
            case MODERATOR -> ModItems.GRAPHITE_BLOCK.get();
            case CONTROL -> ModItems.CONTROL_ROD.get();
            case COOLANT -> Items.WATER_BUCKET;
        });
    }

    /** Each plan's colour, used for its channels' frames and under its tool. */
    private static int colour(Channel channel) {
        return switch (channel) {
            case FUEL -> 0xFF44D65E;
            case MODERATOR -> 0xFF8A929C;
            case CONTROL -> 0xFFE0503C;
            case COOLANT -> 0xFF3A8CF0;
            case EMPTY -> 0;
        };
    }

    /** A soft tint of each plan's colour, so the layout reads at a glance with items in place. */
    private static int fill(Channel channel) {
        return switch (channel) {
            case FUEL -> 0x4044D65E;
            case MODERATOR -> 0x508A929C;
            case CONTROL -> 0x40E0503C;
            case COOLANT -> 0x603A8CF0;
            case EMPTY -> 0;
        };
    }

    private StationReactor.Analysis analysis() {
        return StationReactor.analyse(menu.plan(), menu.rods(), !menu.safeties());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        Channel[] plan = menu.plan();
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            int x = leftPos + StationControlMenu.slotX(i);
            int y = topPos + StationControlMenu.slotY(i);
            graphics.fill(x, y, x + 16, y + 16, fill(plan[i]));
        }
        for (int k = 0; k < TOOLS.length; k++) {
            Channel channel = TOOLS[k];
            int x = leftPos + toolX(k);
            int y = topPos + TOOL_Y;
            graphics.blit(TEXTURE, x, y, isOver(toolX(k), TOOL_Y, mouseX, mouseY) ? KEY_HOVER_U : KEY_U, SPRITE_V, 16, 16);
            graphics.fill(x + 3, y + 12, x + 13, y + 13, colour(channel));
            if (tool == channel) {
                graphics.renderOutline(x - 1, y - 1, 18, 18, colour(channel));
            }
            smallItem(graphics, toolIcon(channel), x + 2.5F, y + 0.5F);
        }
        graphics.blit(TEXTURE, leftPos + SAFETY_X, topPos + SAFETY_Y, menu.safeties() ? SAFETY_ARMED_U : SAFETY_DISARMED_U,
                SAFETY_V, SAFETY_W, SAFETY_H);
        if (isOver(SAFETY_X, SAFETY_Y, SAFETY_W, SAFETY_H, mouseX, mouseY)) {
            graphics.fill(leftPos + SAFETY_X, topPos + SAFETY_Y, leftPos + SAFETY_X + SAFETY_W, topPos + SAFETY_Y + SAFETY_H, 0x20FFFFFF);
        }
        int powerKey = (menu.enabled() ? 0 : 2) + (isOver(POWER_X, KEYS_Y, mouseX, mouseY) ? 1 : 0);
        graphics.blit(TEXTURE, leftPos + POWER_X, topPos + KEYS_Y, POWER_U + powerKey * 16, SPRITE_V, 16, 16);
        graphics.blit(TEXTURE, leftPos + REDSTONE_X, topPos + KEYS_Y, isOver(REDSTONE_X, KEYS_Y, mouseX, mouseY) ? KEY_HOVER_U : KEY_U, SPRITE_V, 16, 16);
        smallItem(graphics, new ItemStack(switch (menu.redstoneMode()) {
            case IGNORED -> Items.GUNPOWDER;
            case HIGH -> Items.REDSTONE;
            case LOW -> Items.REDSTONE_TORCH;
        }), leftPos + REDSTONE_X + 2.5F, topPos + KEYS_Y + 1.5F);

        bar(graphics, HEAT_Y, menu.temperature() / StationRunner.SCRAM_TEMPERATURE, menu.temperature() >= StationRunner.HOT_TEMPERATURE ? BAD : WARN);
        bar(graphics, WATER_Y, menu.water() / (float) StationRunner.WATER_CAPACITY, 0xFF3571C0);
        bar(graphics, ENERGY_Y, menu.energy() / (float) StationRunner.ENERGY_CAPACITY, GuiGauges.ENERGY);
    }

    /**
     * Over the items, kept quiet so the layout reads at a glance: a frame in the plan's colour only
     * where a channel still waits for its item (the rods' own bars show their fuel; heat and coolant
     * load are in each channel's tooltip). A rod with no
     * coolant beside it flashes red (its heat stays in the core), as does overloaded coolant. The
     * channels beside the one under the mouse are outlined, as those are the ones it affects.
     */
    private void renderChannels(GuiGraphics graphics) {
        Channel[] plan = menu.plan();
        ItemStack[] rods = menu.rods();
        StationReactor.Analysis analysis = analysis();
        boolean flash = (System.currentTimeMillis() / 400) % 2 == 0;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 250);
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            if (plan[i] == Channel.EMPTY) {
                continue;
            }
            int x = StationControlMenu.slotX(i);
            int y = StationControlMenu.slotY(i);
            boolean alarm = false;
            if (analysis.heat()[i] > 0) {
                alarm = !cooled(plan, i);
            } else if (plan[i] == Channel.COOLANT && analysis.load()[i] > 0) {
                alarm = analysis.load()[i] > StationReactor.capacity(analysis.overdrive()) + 1;
            }
            if (alarm && flash) {
                frame(graphics, x, y, BAD);
            } else if (plan[i] != Channel.COOLANT && rods[i].isEmpty()) {
                frame(graphics, x, y, colour(plan[i]));
            }
        }
        Slot hovered = getSlotUnderMouse();
        if (hovered != null && hovered.index < StationReactor.CHANNELS) {
            for (int n : StationReactor.neighbours(hovered.getSlotIndex())) {
                graphics.renderOutline(StationControlMenu.slotX(n) - 1, StationControlMenu.slotY(n) - 1, 18, 18, 0xFFE8ECF0);
            }
        }
        graphics.pose().popPose();
    }

    /** Whether a channel has a coolant channel beside it. */
    private static boolean cooled(Channel[] plan, int channel) {
        for (int n : StationReactor.neighbours(channel)) {
            if (plan[n] == Channel.COOLANT) {
                return true;
            }
        }
        return false;
    }

    /** A 1 px frame just inside a slot. */
    private static void frame(GuiGraphics graphics, int x, int y, int colour) {
        graphics.renderOutline(x, y, 16, 16, colour);
    }

    private void bar(GuiGraphics graphics, int y, float fraction, int colour) {
        GuiGauges.glowHorizontal(graphics, leftPos + BAR_X, topPos + y, BAR_W, 4, fraction, colour);
    }

    private void smallItem(GuiGraphics graphics, ItemStack stack, float x, float y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(0.7F, 0.7F, 1);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
        renderChannels(graphics);
        // Now: status, then output and core temperature.
        StationRunner.Status status = menu.status();
        boolean flash = (System.currentTimeMillis() / 400) % 2 == 0;
        int colour = switch (status) {
            case ONLINE -> GOOD;
            case WARMING, NO_FUEL, OFFLINE, OVERDRIVE -> WARN;
            case NO_WATER, OVERHEAT, SCRAM -> BAD;
            case FLUX_TILT, UNSTABLE -> flash ? BAD : WARN;
        };
        int seconds = menu.tiltSeconds();
        text(graphics, Component.translatable("gui.ryzergen.station.status." + status.name().toLowerCase(Locale.ROOT),
                String.format("%d:%02d", seconds / 60, seconds % 60)), 20, colour);
        text(graphics, Component.translatable("gui.ryzergen.station.live", compact(menu.output()), menu.temperature()), 29,
                menu.temperature() >= StationRunner.HOT_TEMPERATURE ? BAD : VALUE);
        // The plan: what this layout gives once settled.
        StationReactor.Analysis plan = analysis();
        if (plan.fuelRods() == 0) {
            text(graphics, Component.translatable("gui.ryzergen.station.plan_empty"), 42, DIM);
            return;
        }
        if (!plan.holdsSteady()) {
            text(graphics, Component.translatable("gui.ryzergen.station.too_hot"), 42, BAD);
            text(graphics, Component.translatable("gui.ryzergen.station.stranded", compact(Math.round(plan.stranded()))), 51, DIM);
            return;
        }
        text(graphics, Component.translatable("gui.ryzergen.station.plan", compact(Math.round(plan.plannedOutput()))), 42, VALUE);
        float temperature = plan.settledTemperature();
        text(graphics, Component.translatable("gui.ryzergen.station.plan_temperature",
                Math.round(StationReactor.efficiency(temperature, plan.overdrive()) * 100), Math.round(temperature)), 51, VALUE);
        text(graphics, Component.translatable("gui.ryzergen.station.rod_life", Math.round(plan.rodMinutes())), 60, VALUE);
        int rating = Math.round(plan.rating() * 100);
        text(graphics, Component.translatable("gui.ryzergen.station.rating", rating), 69, rating >= 90 ? GOOD : rating >= 60 ? WARN : DIM);
    }

    private void text(GuiGraphics graphics, Component text, int y, int colour) {
        graphics.drawString(font, text, TEXT_X, y, colour, false);
    }

    /** 950, 12.5k, 1.20M. */
    private static String compact(int value) {
        if (value < 10_000) {
            return String.format("%,d", value);
        }
        if (value < 1_000_000) {
            return String.format("%.1fk", value / 1_000.0);
        }
        return String.format("%.2fM", value / 1_000_000.0);
    }

    private boolean isOver(int x, int y, double mouseX, double mouseY) {
        return isOver(x, y, 16, 16, mouseX, mouseY);
    }

    private boolean isOver(int x, int y, int w, int h, double mouseX, double mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    /** Tooltip lines wrapped to a readable width. */
    private List<net.minecraft.util.FormattedCharSequence> wrap(List<Component> lines) {
        List<net.minecraft.util.FormattedCharSequence> out = new java.util.ArrayList<>();
        for (Component line : lines) {
            out.addAll(font.split(line, 200));
        }
        return out;
    }

    private static Component tip(String key, int colour) {
        return Component.translatable(key).withStyle(style -> style.withColor(colour));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Slot hovered = getSlotUnderMouse();
        if (hovered != null && hovered.index < StationReactor.CHANNELS && menu.getCarried().isEmpty()) {
            graphics.renderComponentTooltip(font, channelTooltip(hovered.getSlotIndex()), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
        for (int k = 0; k < TOOLS.length; k++) {
            if (isOver(toolX(k), TOOL_Y, mouseX, mouseY)) {
                graphics.renderComponentTooltip(font, List.of(
                        Component.translatable("gui.ryzergen.station.tool." + TOOLS[k].name().toLowerCase(Locale.ROOT)),
                        Component.translatable("gui.ryzergen.station.tool.hint").withStyle(style -> style.withColor(DIM)),
                        Component.translatable("gui.ryzergen.station.tool.clear").withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
            }
        }
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (isOver(POWER_X, KEYS_Y, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(menu.enabled() ? "gui.ryzergen.station.power.on" : "gui.ryzergen.station.power.off"), mouseX, mouseY);
        } else if (isOver(REDSTONE_X, KEYS_Y, mouseX, mouseY)) {
            String mode = switch (menu.redstoneMode()) {
                case IGNORED -> "gui.ryzergen.redstone.ignored";
                case HIGH -> "gui.ryzergen.redstone.high";
                case LOW -> "gui.ryzergen.redstone.low";
            };
            graphics.renderTooltip(font, Component.translatable(mode), mouseX, mouseY);
        } else if (isOver(SAFETY_X, SAFETY_Y, SAFETY_W, SAFETY_H, mouseX, mouseY)) {
            List<Component> lines = menu.safeties()
                    ? List.of(tip("gui.ryzergen.station.safeties.armed", GOOD),
                    tip("gui.ryzergen.station.safeties.overdrive", DIM),
                    tip("gui.ryzergen.station.safeties.warning", BAD),
                    tip("gui.ryzergen.station.safeties.disarm_hint", DIM))
                    : List.of(tip("gui.ryzergen.station.safeties.disarmed", BAD),
                    tip("gui.ryzergen.station.safeties.warning", BAD),
                    tip("gui.ryzergen.station.safeties.arm_hint", DIM));
            graphics.renderTooltip(font, wrap(lines), mouseX, mouseY);
        } else if (mx >= TEXT_X && mx < TEXT_X + BAR_W && my >= 67 && my < 77) {
            graphics.renderTooltip(font, Component.translatable("gui.ryzergen.station.rating.tip"), mouseX, mouseY);
        } else if (mx >= BAR_X && mx < BAR_X + BAR_W) {
            Component tip = my >= HEAT_Y - 1 && my < HEAT_Y + 5
                    ? Component.translatable("gui.ryzergen.station.heat", menu.temperature(), (int) StationRunner.SCRAM_TEMPERATURE)
                    : my >= WATER_Y - 1 && my < WATER_Y + 5
                    ? Component.translatable("gui.ryzergen.intake_pump.water", String.format("%,d", menu.water()), String.format("%,d", StationRunner.WATER_CAPACITY))
                    : my >= ENERGY_Y - 1 && my < ENERGY_Y + 5
                    ? Component.translatable("gui.ryzergen.energy", String.format("%,d", menu.energy()), String.format("%,d", StationRunner.ENERGY_CAPACITY))
                    : null;
            if (tip != null) {
                graphics.renderTooltip(font, tip, mouseX, mouseY);
            }
        }
    }

    /** What a channel does in this layout, worked out so the player can see why a plan scores as it does. */
    private List<Component> channelTooltip(int channel) {
        Channel[] plan = menu.plan();
        ItemStack[] rods = menu.rods();
        StationReactor.Analysis analysis = analysis();
        List<Component> lines = new java.util.ArrayList<>();
        int moderators = 0;
        int fuel = 0;
        int control = 0;
        int coolant = 0;
        for (int n : StationReactor.neighbours(channel)) {
            if (plan[n] == Channel.COOLANT) {
                coolant++;
            } else if (plan[n].accepts(rods[n])) {
                switch (plan[n]) {
                    case MODERATOR -> moderators++;
                    case FUEL -> fuel++;
                    case CONTROL -> control++;
                    default -> { }
                }
            }
        }
        String key = "gui.ryzergen.station.channel.";
        switch (plan[channel]) {
            case EMPTY -> lines.add(Component.translatable(key + "empty"));
            case FUEL -> {
                if (analysis.heat()[channel] <= 0) {
                    lines.add(Component.translatable(key + "fuel.waiting").withStyle(style -> style.withColor(colour(Channel.FUEL))));
                    break;
                }
                lines.add(rods[channel].getHoverName().copy().withStyle(style -> style.withColor(colour(Channel.FUEL))));
                lines.add(Component.translatable(key + "fuel.heat", compact(Math.round(analysis.heat()[channel]))).withStyle(style -> style.withColor(VALUE)));
                if (moderators > 0) {
                    lines.add(Component.translatable(key + "fuel.moderators", moderators));
                }
                if (fuel > 0) {
                    lines.add(Component.translatable(key + "fuel.fuel", fuel));
                }
                if (control > 0) {
                    lines.add(Component.translatable(key + "fuel.control", control));
                }
                lines.add(Component.translatable(key + "fuel.burn", Math.round(analysis.burn()[channel] * 100)));
                lines.add(coolant > 0
                        ? Component.translatable(key + "fuel.cooled", coolant).withStyle(style -> style.withColor(GOOD))
                        : Component.translatable(key + "fuel.uncooled").withStyle(style -> style.withColor(BAD)));
            }
            case COOLANT -> {
                lines.add(Component.translatable(key + "coolant").withStyle(style -> style.withColor(colour(Channel.COOLANT))));
                float load = analysis.load()[channel];
                int capacity = Math.round(StationReactor.capacity(analysis.overdrive()));
                if (load <= 0) {
                    lines.add(Component.translatable(key + "coolant.idle").withStyle(style -> style.withColor(DIM)));
                } else if (load > capacity + 1) {
                    lines.add(Component.translatable(key + "coolant.load", compact(Math.round(load)), compact(capacity)).withStyle(style -> style.withColor(BAD)));
                    lines.add(Component.translatable(key + "coolant.over").withStyle(style -> style.withColor(BAD)));
                } else {
                    lines.add(Component.translatable(key + "coolant.load", compact(Math.round(load)), compact(capacity)).withStyle(style -> style.withColor(VALUE)));
                }
            }
            case MODERATOR, CONTROL -> {
                String name = plan[channel].name().toLowerCase(Locale.ROOT);
                int colour = colour(plan[channel]);
                int touched = fuel;
                if (rods[channel].isEmpty()) {
                    lines.add(Component.translatable(key + name + ".waiting").withStyle(style -> style.withColor(colour)));
                } else {
                    lines.add(rods[channel].getHoverName().copy().withStyle(style -> style.withColor(colour)));
                    lines.add(Component.translatable(key + name, touched).withStyle(style -> style.withColor(touched > 0 ? VALUE : DIM)));
                }
            }
        }
        lines.add(Component.translatable("gui.ryzergen.station.tool.clear").withStyle(style -> style.withColor(DIM)));
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Slot slot = getSlotUnderMouse();
        boolean channel = slot != null && slot.index < StationReactor.CHANNELS;
        if (button == 1 && channel && menu.getCarried().isEmpty()) {
            // Right-click: take the item out, or clear the plan if there is none.
            if (slot.hasItem() || menu.plan(slot.getSlotIndex()) != Channel.EMPTY) {
                press(StationControlMenu.BUTTON_CLEAR + slot.getSlotIndex());
            }
            return true;
        }
        if (button == 0) {
            for (int k = 0; k < TOOLS.length; k++) {
                if (isOver(toolX(k), TOOL_Y, mouseX, mouseY)) {
                    tool = tool == TOOLS[k] ? null : TOOLS[k];
                    click();
                    return true;
                }
            }
            if (isOver(POWER_X, KEYS_Y, mouseX, mouseY)) {
                return press(StationControlMenu.BUTTON_POWER);
            }
            // Disarming the safeties takes a shift-click, so it is never an accident.
            if (isOver(SAFETY_X, SAFETY_Y, SAFETY_W, SAFETY_H, mouseX, mouseY)) {
                return !menu.safeties() || hasShiftDown() ? press(StationControlMenu.BUTTON_SAFETY) : true;
            }
            if (isOver(REDSTONE_X, KEYS_Y, mouseX, mouseY)) {
                return press(StationControlMenu.BUTTON_REDSTONE);
            }
            // With a plan tool picked and nothing held, clicking a channel plans it.
            if (tool != null && channel && menu.getCarried().isEmpty() && menu.plan(slot.getSlotIndex()) != tool) {
                return press(StationControlMenu.BUTTON_PLAN + slot.getSlotIndex() * 8 + tool.ordinal());
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private boolean press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            click();
        }
        return true;
    }
}
