package com.ryzer.ryzergen.machine.microreactor;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.ARGB;
import com.ryzer.ryzergen.client.GuiGauges;
import com.ryzer.ryzergen.machine.RedstoneMode;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

import static com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlockEntity.*;

/**
 * The microreactor control panel. Positions match art/tools/gui_textures.py, which draws the
 * panel and the button sprites to the right of it. Gauge fills are drawn here, not from pixel art.
 */
public class MicroreactorScreen extends AbstractContainerScreen<MicroreactorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/microreactor.png");

    // Panel layout: gauge insides are 8 x 60, the fuel bar inside is 16 x 4.
    private static final int GAUGE_H = 60;
    private static final int TEMP_X = 45;
    private static final int ENERGY_X = 143;
    private static final int COOLANT_X = 157;
    private static final int GAUGE_Y = 21;
    private static final int FUEL_X = 8;
    private static final int FUEL_Y = 41;
    private static final int POWER_X = 8;
    private static final int REDSTONE_X = 26;
    private static final int DUMP_X = 27;
    private static final int DUMP_Y = 21;
    private static final int BUTTON_Y = 49;
    private static final int SAFETY_X = 8;
    private static final int SAFETY_Y = 67;
    private static final int SAFETY_W = 34;
    private static final int SAFETY_H = 14;
    /** The efficiency bar's full width stands for this much efficiency. */
    private static final float EFFICIENCY_SCALE = 0.50F;
    private static final int SCREEN_X = 58;
    private static final int SCREEN_W = 80;
    private static final int READOUT_X = 71;

    // Gauge colours, lit versions of the palette.
    private static final int ENERGY_COLOUR = 0xFFFF4D3D;
    private static final int FUEL_COLOUR = 0xFF52E06A;
    private static final int TEMP_COOL = 0xFF44D65E;
    private static final int TEMP_WARM = 0xFFF0A030;
    private static final int TEMP_HOT = 0xFFE8342A;

    // Sprite sheet.
    private static final int SPRITE_BUTTON_U = 176;
    private static final int SPRITE_BUTTON_HOVER_U = 192;
    private static final int SPRITE_BUTTON_V = 64;
    private static final int SPRITE_ICON_V = 80;
    /** Whole power keys in a row: on, on hovered, off, off hovered. */
    private static final int SPRITE_POWER_KEYS_U = 176;
    private static final int SPRITE_POWER_KEYS_V = 128;
    private static final int SPRITE_FOLLOW_LOAD_U = 224;
    private static final int SPRITE_DUMP_U = 240;
    private static final int SPRITE_SAFETY_U = 176;
    private static final int SPRITE_SAFETY_ARMED_V = 96;
    private static final int SPRITE_SAFETY_DISARMED_V = 110;

    // Readout colours, from the texture palette.
    /** Dark graphite, for labels on the light casing. */
    private static final int LABEL = 0xFF2B3036;
    private static final int VALUE = 0xFF35C8F5;
    private static final int GOOD = 0xFF44D65E;
    private static final int WARN = 0xFFF0A030;
    private static final int BAD = 0xFFD13C3C;
    private static final int HOT = 0xFFFF8A1E;
    private static final int DIM = 0xFF2A7CD6;
    private static final int HINT = 0xFF9AA3AE;

    public MicroreactorScreen(MicroreactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 190;
        inventoryLabelY = 95;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        float heat = Mth.clamp((menu.temperature() - AMBIENT_TEMPERATURE) / (float) (MAX_TEMPERATURE - AMBIENT_TEMPERATURE), 0, 1);
        int heatColour = heat < 0.5F
                ? ARGB.lerp(heat * 2, TEMP_COOL, TEMP_WARM)
                : ARGB.lerp((heat - 0.5F) * 2, TEMP_WARM, TEMP_HOT);
        glowGauge(graphics, TEMP_X, heat, heatColour);
        glowGauge(graphics, ENERGY_X, menu.energy() / (float) ENERGY_CAPACITY, ENERGY_COLOUR);
        fluidGauge(graphics, COOLANT_X, menu.coolant() / (float) COOLANT_CAPACITY, Fluids.WATER);
        for (int x : new int[] {TEMP_X, ENERGY_X, COOLANT_X}) {
            glass(graphics, x);
        }
        int fuel = Math.round(menu.fuel() * 16);
        if (fuel > 0) {
            graphics.fillGradient(leftPos + FUEL_X, topPos + FUEL_Y, leftPos + FUEL_X + fuel, topPos + FUEL_Y + 4,
                    FUEL_COLOUR, ARGB.darken(FUEL_COLOUR, 0.55F));
            graphics.fill(leftPos + FUEL_X, topPos + FUEL_Y, leftPos + FUEL_X + fuel, topPos + FUEL_Y + 1, ARGB.lighten(FUEL_COLOUR, 0.5F));
        }

        int powerKey = (menu.enabled() ? 0 : 2) + (isOver(POWER_X, BUTTON_Y, 16, 16, mouseX, mouseY) ? 1 : 0);
        graphics.blit(TEXTURE, leftPos + POWER_X, topPos + BUTTON_Y, SPRITE_POWER_KEYS_U + powerKey * 16, SPRITE_POWER_KEYS_V, 16, 16);
        redstoneButton(graphics, mouseX, mouseY);
        button(graphics, DUMP_X, DUMP_Y, mouseX, mouseY, menu.dumpExcess() ? SPRITE_DUMP_U : SPRITE_FOLLOW_LOAD_U, SPRITE_ICON_V);
        graphics.blit(TEXTURE, leftPos + SAFETY_X, topPos + SAFETY_Y, SPRITE_SAFETY_U,
                menu.safeties() ? SPRITE_SAFETY_ARMED_V : SPRITE_SAFETY_DISARMED_V, SAFETY_W, SAFETY_H);
        if (isOver(SAFETY_X, SAFETY_Y, SAFETY_W, SAFETY_H, mouseX, mouseY)) {
            graphics.fill(leftPos + SAFETY_X, topPos + SAFETY_Y, leftPos + SAFETY_X + SAFETY_W, topPos + SAFETY_Y + SAFETY_H, 0x20FFFFFF);
        }
    }

    private void glowGauge(GuiGraphics graphics, int x, float fraction, int colour) {
        GuiGauges.glow(graphics, leftPos + x, topPos + GAUGE_Y, 8, GAUGE_H, fraction, colour);
    }

    private void fluidGauge(GuiGraphics graphics, int x, float fraction, Fluid fluid) {
        GuiGauges.fluid(graphics, leftPos + x, topPos + GAUGE_Y, 8, GAUGE_H, fraction, fluid);
    }

    private void glass(GuiGraphics graphics, int x) {
        GuiGauges.glass(graphics, leftPos + x, topPos + GAUGE_Y, 8, GAUGE_H);
    }

    /**
     * The redstone key shows the game's own items, as Mekanism does: gunpowder for ignored, redstone
     * dust for "runs with a signal", and a redstone torch (which inverts a signal) for "runs without".
     */
    private void redstoneButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int base = isOver(REDSTONE_X, BUTTON_Y, 16, 16, mouseX, mouseY) ? SPRITE_BUTTON_HOVER_U : SPRITE_BUTTON_U;
        graphics.blit(TEXTURE, leftPos + REDSTONE_X, topPos + BUTTON_Y, base, SPRITE_BUTTON_V, 16, 16);
        ItemStack icon = new ItemStack(switch (menu.redstoneMode()) {
            case IGNORED -> Items.GUNPOWDER;
            case HIGH -> Items.REDSTONE;
            case LOW -> Items.REDSTONE_TORCH;
        });
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + REDSTONE_X + 2.5F, topPos + BUTTON_Y + 1.5F, 0);
        graphics.pose().scale(0.7F, 0.7F, 1);
        graphics.renderItem(icon, 0, 0);
        graphics.pose().popPose();
    }

    private void button(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, int iconU, int iconV) {
        int base = isOver(x, y, 16, 16, mouseX, mouseY) ? SPRITE_BUTTON_HOVER_U : SPRITE_BUTTON_U;
        graphics.blit(TEXTURE, leftPos + x, topPos + y, base, SPRITE_BUTTON_V, 16, 16);
        graphics.blit(TEXTURE, leftPos + x, topPos + y, iconU, iconV, 16, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);

        Status status = menu.status();
        Component statusText = Component.translatable("gui.ryzergen.microreactor.status." + status.name().toLowerCase());
        int statusColour = switch (status) {
            case ONLINE -> GOOD;
            case OFFLINE -> BAD;
            case STANDBY, DUMPING -> VALUE;
            case OVERDRIVE -> HOT;
            case COOLANT_LOSS -> Util.getMillis() / 250 % 2 == 0 ? BAD : 0xFFFFFFFF;
            case HALTED, NO_CORE, DEPLETED -> WARN;
        };
        graphics.drawString(font, statusText, SCREEN_X + (SCREEN_W - font.width(statusText)) / 2, 24, statusColour, false);

        graphics.drawString(font, Component.translatable("gui.ryzergen.fe_per_tick", String.format("%,d", menu.output())),
                READOUT_X, 37, VALUE, false);
        graphics.drawString(font, Component.translatable("gui.ryzergen.celsius", menu.temperature()), READOUT_X, 49, VALUE, false);
        Component fuel = status == Status.NO_CORE || status == Status.DEPLETED
                ? Component.translatable("gui.ryzergen.microreactor.no_core")
                : Component.literal(Math.round(menu.fuel() * 100) + "%");
        graphics.drawString(font, fuel, READOUT_X, 61, VALUE, false);

        // Efficiency: how much of the core's heat becomes power, with a bar coloured by cooling mode.
        if (status.running()) {
            float efficiency = menu.efficiency();
            graphics.drawString(font, String.format("%.1f%%", efficiency * 100), READOUT_X, 73, VALUE, false);
            int barColour = status.overdrive() ? HOT : menu.coolant() > 0 ? VALUE : WARN;
            int barX = SCREEN_X + SCREEN_W - 30;
            graphics.fill(barX, 74, barX + 26, 79, 0xFF15171B);
            int w = Math.round(Mth.clamp(efficiency / EFFICIENCY_SCALE, 0, 1) * 24);
            if (w > 0) {
                graphics.fillGradient(barX + 1, 75, barX + 1 + w, 78, barColour, ARGB.darken(barColour, 0.4F));
            }
        } else {
            graphics.drawString(font, "--", READOUT_X, 73, DIM, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (isOver(TEMP_X - 1, GAUGE_Y - 1, 10, 62, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.ryzergen.microreactor.temperature",
                    menu.temperature(), MAX_TEMPERATURE), mouseX, mouseY);
        } else if (isOver(ENERGY_X - 1, GAUGE_Y - 1, 10, 62, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.ryzergen.energy",
                    String.format("%,d", menu.energy()), String.format("%,d", ENERGY_CAPACITY)), mouseX, mouseY);
        } else if (isOver(COOLANT_X - 1, GAUGE_Y - 1, 10, 62, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.ryzergen.microreactor.coolant",
                    String.format("%,d", menu.coolant()), String.format("%,d", COOLANT_CAPACITY)), mouseX, mouseY);
        } else if (isOver(FUEL_X - 1, FUEL_Y - 1, 18, 6, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.ryzergen.microreactor.fuel",
                    Math.round(menu.fuel() * 100)), mouseX, mouseY);
        } else if (isOver(POWER_X, BUTTON_Y, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable(menu.enabled() ? "gui.ryzergen.power.on" : "gui.ryzergen.power.off"),
                    Component.translatable("gui.ryzergen.power.hint").withStyle(style -> style.withColor(0xFF9AA3AE))),
                    mouseX, mouseY);
        } else if (isOver(SCREEN_X, 71, SCREEN_W, 10, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.ryzergen.microreactor.efficiency"),
                    hint("gui.ryzergen.microreactor.efficiency.hint", HINT)), mouseX, mouseY);
        } else if (isOver(DUMP_X, DUMP_Y, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, menu.dumpExcess()
                    ? List.of(Component.translatable("gui.ryzergen.microreactor.dump.on"),
                    hint("gui.ryzergen.microreactor.dump.on_hint", HINT), hint("gui.ryzergen.redstone.hint", HINT))
                    : List.of(Component.translatable("gui.ryzergen.microreactor.dump.off"),
                    hint("gui.ryzergen.microreactor.dump.off_hint", HINT), hint("gui.ryzergen.redstone.hint", HINT)),
                    mouseX, mouseY);
        } else if (isOver(SAFETY_X, SAFETY_Y, SAFETY_W, SAFETY_H, mouseX, mouseY)) {
            List<Component> lines = menu.safeties()
                    ? List.of(hint("gui.ryzergen.microreactor.safeties.armed", GOOD),
                    hint("gui.ryzergen.microreactor.safeties.overdrive", HINT),
                    hint("gui.ryzergen.microreactor.safeties.warning", BAD),
                    hint("gui.ryzergen.microreactor.safeties.disarm_hint", HINT))
                    : List.of(hint("gui.ryzergen.microreactor.safeties.disarmed", BAD),
                    hint("gui.ryzergen.microreactor.safeties.warning", BAD),
                    hint("gui.ryzergen.microreactor.safeties.arm_hint", HINT));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (isOver(REDSTONE_X, BUTTON_Y, 16, 16, mouseX, mouseY)) {
            RedstoneMode mode = menu.redstoneMode();
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.ryzergen.redstone." + mode.name().toLowerCase()),
                    Component.translatable("gui.ryzergen.redstone.hint").withStyle(style -> style.withColor(0xFF9AA3AE))),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isOver(POWER_X, BUTTON_Y, 16, 16, mouseX, mouseY)) {
                return press(MicroreactorMenu.BUTTON_POWER);
            }
            if (isOver(REDSTONE_X, BUTTON_Y, 16, 16, mouseX, mouseY)) {
                return press(MicroreactorMenu.BUTTON_REDSTONE);
            }
            if (isOver(DUMP_X, DUMP_Y, 16, 16, mouseX, mouseY)) {
                return press(MicroreactorMenu.BUTTON_DUMP);
            }
            // Disarming the interlocks takes a deliberate shift-click; re-arming takes any click.
            if (isOver(SAFETY_X, SAFETY_Y, SAFETY_W, SAFETY_H, mouseX, mouseY) && (!menu.safeties() || hasShiftDown())) {
                return press(MicroreactorMenu.BUTTON_SAFETIES);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static Component hint(String key, int colour) {
        return Component.translatable(key).withStyle(style -> style.withColor(colour));
    }

    private boolean press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        return true;
    }

    private boolean isOver(int x, int y, int w, int h, double mouseX, double mouseY) {
        double mx = mouseX - leftPos;
        double my = mouseY - topPos;
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
