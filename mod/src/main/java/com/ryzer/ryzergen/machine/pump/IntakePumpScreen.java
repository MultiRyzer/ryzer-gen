package com.ryzer.ryzergen.machine.pump;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

import static com.ryzer.ryzergen.machine.pump.IntakePumpBlockEntity.*;

/** Intake pump readout. Positions match art/tools/gui_textures.py (intake_pump). */
public class IntakePumpScreen extends AbstractContainerScreen<IntakePumpMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/intake_pump.png");
    private static final int LABEL = 0xFF2B3036;
    private static final int VALUE = 0xFF35C8F5;
    private static final int DIM = 0xFF9AA3AE;
    private static final int GOOD = 0xFF44D65E;
    private static final int WARN = 0xFFF0A030;
    private static final int BAD = 0xFFE0503C;
    private static final int GAUGE_Y = 22;
    private static final int GAUGE_H = 72;
    private static final int ENERGY_X = 11;
    private static final int WATER_X = 25;
    private static final int READOUT_X = 48;
    private static final int KEY_X = 148;
    private static final int KEY_Y = 80;

    public IntakePumpScreen(IntakePumpMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 104;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        GuiGauges.glow(graphics, leftPos + ENERGY_X, topPos + GAUGE_Y, 8, GAUGE_H, menu.energy() / (float) ENERGY_CAPACITY, GuiGauges.ENERGY);
        GuiGauges.glass(graphics, leftPos + ENERGY_X, topPos + GAUGE_Y, 8, GAUGE_H);
        GuiGauges.fluid(graphics, leftPos + WATER_X, topPos + GAUGE_Y, 12, GAUGE_H, menu.water() / (float) WATER_CAPACITY, Fluids.WATER);
        GuiGauges.glass(graphics, leftPos + WATER_X, topPos + GAUGE_Y, 12, GAUGE_H);

        boolean hover = isOver(KEY_X, KEY_Y, mouseX, mouseY);
        graphics.blit(TEXTURE, leftPos + KEY_X, topPos + KEY_Y, hover ? 192 : 176, 0, 16, 16);
        ItemStack icon = new ItemStack(switch (menu.redstoneMode()) {
            case IGNORED -> Items.GUNPOWDER;
            case HIGH -> Items.REDSTONE;
            case LOW -> Items.REDSTONE_TORCH;
        });
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + KEY_X + 2.5F, topPos + KEY_Y + 1.5F, 0);
        graphics.pose().scale(0.7F, 0.7F, 1);
        graphics.renderItem(icon, 0, 0);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        Status status = menu.status();
        int colour = switch (status) {
            case PUMPING -> GOOD;
            case FULL, REDSTONE -> WARN;
            case NO_WATER, NO_POWER -> BAD;
        };
        String key = "gui.ryzergen.intake_pump.status." + status.name().toLowerCase(java.util.Locale.ROOT);
        graphics.drawString(font, Component.translatable(key), READOUT_X + 4, 26, colour, false);
        graphics.drawString(font, Component.translatable(menu.waterBelow() ? "gui.ryzergen.intake_pump.source.found"
                : "gui.ryzergen.intake_pump.source.missing"), READOUT_X + 4, 40, menu.waterBelow() ? VALUE : BAD, false);
        graphics.drawString(font, Component.translatable("gui.ryzergen.intake_pump.rate", status == Status.PUMPING ? Config.get(Config.PUMP_RATE) : 0),
                READOUT_X + 4, 52, VALUE, false);
        graphics.drawString(font, Component.translatable("gui.ryzergen.intake_pump.power", Config.get(Config.PUMP_ENERGY)),
                READOUT_X + 4, 64, DIM, false);
    }

    private boolean isOver(int x, int y, double mouseX, double mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + 16 && mouseY >= topPos + y && mouseY < topPos + y + 16;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (my >= GAUGE_Y - 1 && my < GAUGE_Y + GAUGE_H + 1) {
            if (mx >= ENERGY_X - 1 && mx < ENERGY_X + 9) {
                graphics.renderTooltip(font, Component.translatable("gui.ryzergen.energy",
                        String.format("%,d", menu.energy()), String.format("%,d", ENERGY_CAPACITY)), mouseX, mouseY);
            } else if (mx >= WATER_X - 1 && mx < WATER_X + 13) {
                graphics.renderTooltip(font, Component.translatable("gui.ryzergen.intake_pump.water",
                        String.format("%,d", menu.water()), String.format("%,d", WATER_CAPACITY)), mouseX, mouseY);
            }
        }
        if (isOver(KEY_X, KEY_Y, mouseX, mouseY)) {
            String mode = switch (menu.redstoneMode()) {
                case IGNORED -> "gui.ryzergen.redstone.ignored";
                case HIGH -> "gui.ryzergen.redstone.high";
                case LOW -> "gui.ryzergen.redstone.low";
            };
            graphics.renderComponentTooltip(font, List.of(Component.translatable(mode),
                    Component.translatable("gui.ryzergen.redstone.hint").withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOver(KEY_X, KEY_Y, mouseX, mouseY) && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, IntakePumpMenu.BUTTON_REDSTONE);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
