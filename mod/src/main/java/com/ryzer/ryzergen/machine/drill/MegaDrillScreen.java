package com.ryzer.ryzergen.machine.drill;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import com.ryzer.ryzergen.machine.drill.MegaDrillBlockEntity.Status;
import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.client.text.RichText;
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
import java.util.Locale;

/**
 * The formed drill's controls: the head, tip and speed module slots with the power and redstone
 * keys down the left, the readout (status, the layer it is on, its speed; the energy gauge's tooltip
 * gives the draw), the output and the
 * filter row under it, and the energy and water gauges on the right. Positions match
 * art/tools/gui_textures.py (mega_drill).
 */
public class MegaDrillScreen extends AbstractContainerScreen<MegaDrillMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/mega_drill.png");
    private static final int LABEL = 0xFF2B3036;
    private static final int VALUE = 0xFF35C8F5;
    private static final int DIM = 0xFF9AA3AE;
    private static final int GOOD = 0xFF44D65E;
    private static final int WARN = 0xFFF0A030;
    private static final int BAD = 0xFFE0503C;
    private static final int KEY_X = 8;
    private static final int POWER_Y = 72;
    private static final int REDSTONE_Y = 90;
    private static final int TEXT_X = 31;
    private static final int LINE_Y = 20;
    private static final int LINE_STEP = 10;
    private static final int WELL_Y = 17;
    private static final int WELL_H = 90;
    private static final int ENERGY_X = 139;
    private static final int WATER_X = 152;
    private static final int ENERGY_W = 8;
    private static final int WATER_W = 12;
    // Sprites, right of the panel: the keycap (plain and hovered), then the power keys (on, on
    // hovered, off, off hovered).
    private static final int SPRITE_KEY_V = 0;
    private static final int SPRITE_POWER_V = 16;
    /**
     * The sound switch, in a tab on the panel's right edge (its sprite at 176,40), overlapping the edge
     * by 3 pixels so the two read as one piece; its keys (sound on, on hovered, muted, muted hovered)
     * from 176,72.
     */
    private static final int TAB_X = 173;
    private static final int TAB_Y = 16;
    private static final int TAB_W = 22;
    private static final int TAB_H = 26;
    private static final int SPRITE_TAB_V = 40;
    private static final int SPRITE_SOUND_V = 72;
    private static final int MUTE_X = TAB_X + 3;
    private static final int MUTE_Y = TAB_Y + 5;

    public MegaDrillScreen(MegaDrillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 202;
        titleLabelY = 4;
        inventoryLabelY = MegaDrillMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        GuiGauges.glow(graphics, leftPos + ENERGY_X, topPos + WELL_Y, ENERGY_W, WELL_H,
                menu.energy() / (float) MegaDrillBlockEntity.ENERGY_CAPACITY, GuiGauges.ENERGY);
        GuiGauges.glass(graphics, leftPos + ENERGY_X, topPos + WELL_Y, ENERGY_W, WELL_H);
        GuiGauges.fluid(graphics, leftPos + WATER_X, topPos + WELL_Y, WATER_W, WELL_H,
                menu.water() / (float) MegaDrillBlockEntity.WATER_CAPACITY, Fluids.WATER);
        GuiGauges.glass(graphics, leftPos + WATER_X, topPos + WELL_Y, WATER_W, WELL_H);

        int power = (menu.enabled() ? 0 : 2) + (isOver(KEY_X, POWER_Y, mouseX, mouseY) ? 1 : 0);
        graphics.blit(TEXTURE, leftPos + KEY_X, topPos + POWER_Y, 176 + power * 16, SPRITE_POWER_V, 16, 16);
        boolean hover = isOver(KEY_X, REDSTONE_Y, mouseX, mouseY);
        graphics.blit(TEXTURE, leftPos + KEY_X, topPos + REDSTONE_Y, hover ? 192 : 176, SPRITE_KEY_V, 16, 16);
        ItemStack icon = new ItemStack(switch (menu.redstoneMode()) {
            case IGNORED -> Items.GUNPOWDER;
            case HIGH -> Items.REDSTONE;
            case LOW -> Items.REDSTONE_TORCH;
        });
        graphics.blit(TEXTURE, leftPos + TAB_X, topPos + TAB_Y, 176, SPRITE_TAB_V, TAB_W, TAB_H);
        int sound = (menu.muted() ? 2 : 0) + (isOver(MUTE_X, MUTE_Y, mouseX, mouseY) ? 1 : 0);
        graphics.blit(TEXTURE, leftPos + MUTE_X, topPos + MUTE_Y, 176 + sound * 16, SPRITE_SOUND_V, 16, 16);
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + KEY_X + 2.5F, topPos + REDSTONE_Y + 1.5F, 0);
        graphics.pose().scale(0.7F, 0.7F, 1);
        graphics.renderItem(icon, 0, 0);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
        Status status = menu.status();
        int colour = switch (status) {
            case CHARGING, FIRING, SCANNING, BORING -> GOOD;
            case OFF, FINISHED -> DIM;
            case OUTPUT_FULL, REDSTONE, UNLOADED -> WARN;
            case NO_HEAD, NO_TIP, NO_POWER, NO_WATER -> BAD;
        };
        graphics.drawString(font, RichText.tr("gui.ryzergen.mega_drill.status." + status.name().toLowerCase(Locale.ROOT)),
                TEXT_X, LINE_Y, colour, false);
        graphics.drawString(font, menu.deep() ? RichText.tr("gui.ryzergen.mega_drill.deep", menu.layer())
                        : RichText.tr("gui.ryzergen.mega_drill.layer", menu.layer()),
                TEXT_X, LINE_Y + LINE_STEP, VALUE, false);
        Component speed = menu.speed() == 0 ? RichText.tr("gui.ryzergen.mega_drill.speed_none")
                : RichText.tr("gui.ryzergen.mega_drill.speed", menu.speed());
        graphics.drawString(font, speed, TEXT_X, LINE_Y + 2 * LINE_STEP, menu.speed() == 0 ? DIM : VALUE, false);
    }

    private boolean isOver(int x, int y, double mouseX, double mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + 16 && mouseY >= topPos + y && mouseY < topPos + y + 16;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        boolean wellRow = my >= WELL_Y - 1 && my < WELL_Y + WELL_H + 1;
        if (wellRow && mx >= ENERGY_X - 1 && mx < ENERGY_X + ENERGY_W + 1) {
            graphics.renderComponentTooltip(font, List.of(
                    RichText.tr("gui.ryzergen.energy", String.format("%,d", menu.energy()),
                            String.format("%,d", MegaDrillBlockEntity.ENERGY_CAPACITY)),
                    RichText.tr("gui.ryzergen.mega_drill.uses", String.format("%,d", menu.draw()))
                            .withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
        } else if (wellRow && mx >= WATER_X - 1 && mx < WATER_X + WATER_W + 1) {
            graphics.renderComponentTooltip(font, List.of(
                    RichText.tr("gui.ryzergen.mega_drill.water", String.format("%,d", menu.water()),
                            String.format("%,d", MegaDrillBlockEntity.WATER_CAPACITY)),
                    RichText.tr("gui.ryzergen.mega_drill.water_use", String.format("%,d", Config.get(Config.DRILL_WATER)))
                            .withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < MegaDrillMenu.MACHINE_SLOTS) {
            slotHint(graphics, hoveredSlot.index, mouseX, mouseY);
        } else if (isOver(KEY_X, POWER_Y, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(
                    RichText.tr(menu.enabled() ? "gui.ryzergen.processing.power.on" : "gui.ryzergen.processing.power.off"),
                    RichText.tr("gui.ryzergen.power.hint").withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
        } else if (isOver(MUTE_X, MUTE_Y, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(
                    RichText.tr(menu.muted() ? "gui.ryzergen.mega_drill.sound.off" : "gui.ryzergen.mega_drill.sound.on"),
                    RichText.tr("gui.ryzergen.power.hint").withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
        } else if (isOver(KEY_X, REDSTONE_Y, mouseX, mouseY)) {
            String mode = switch (menu.redstoneMode()) {
                case IGNORED -> "gui.ryzergen.redstone.ignored";
                case HIGH -> "gui.ryzergen.redstone.high";
                case LOW -> "gui.ryzergen.redstone.low";
            };
            graphics.renderComponentTooltip(font, List.of(RichText.tr(mode),
                    RichText.tr("gui.ryzergen.redstone.hint").withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
        }
    }

    /** What an empty slot is for. */
    private void slotHint(GuiGraphics graphics, int index, int mouseX, int mouseY) {
        Component name;
        Component hint;
        if (index == MegaDrillBlockEntity.SLOT_HEAD) {
            name = RichText.tr("gui.ryzergen.mega_drill.head_slot");
            hint = RichText.tr("gui.ryzergen.mega_drill.head_hint");
        } else if (index == MegaDrillBlockEntity.SLOT_TIP) {
            name = RichText.tr("gui.ryzergen.mega_drill.tip_slot");
            hint = RichText.tr("gui.ryzergen.mega_drill.tip_hint");
        } else if (index == MegaDrillBlockEntity.SLOT_MODULES) {
            name = RichText.tr("gui.ryzergen.processing.upgrade_slot");
            hint = RichText.tr("gui.ryzergen.processing.upgrade_hint", ProcessingMachine.MAX_MODULES);
        } else if (index >= MegaDrillMenu.FIRST_FILTER) {
            name = RichText.tr("gui.ryzergen.mega_drill.filter_slot");
            hint = RichText.tr("gui.ryzergen.mega_drill.filter_hint");
        } else {
            return;
        }
        graphics.renderComponentTooltip(font, List.of(name, hint.copy().withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            int id = isOver(KEY_X, POWER_Y, mouseX, mouseY) ? MegaDrillMenu.BUTTON_POWER
                    : isOver(KEY_X, REDSTONE_Y, mouseX, mouseY) ? MegaDrillMenu.BUTTON_REDSTONE
                    : isOver(MUTE_X, MUTE_Y, mouseX, mouseY) ? MegaDrillMenu.BUTTON_MUTE : -1;
            if (id >= 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
