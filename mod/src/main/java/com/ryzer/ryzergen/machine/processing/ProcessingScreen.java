package com.ryzer.ryzergen.machine.processing;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import com.ryzer.ryzergen.machine.processing.ProcessingBlockEntity.Status;
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
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The fuel cycle machines' screen. Positions match art/tools/gui_textures.py (processing()), which
 * draws one texture per machine at textures/gui/&lt;machine&gt;.png.
 */
public class ProcessingScreen extends AbstractContainerScreen<ProcessingMenu> {
    private static final int LABEL = 0xFF2B3036;
    private static final int DIM = 0xFF9AA3AE;
    private static final int GOOD = 0xFF44D65E;
    private static final int WARN = 0xFFF0A030;
    private static final int BAD = 0xFFE0503C;
    public static final int ARROW_X = 84;
    public static final int ARROW_Y = 34;
    private static final int WELL_Y = 18;
    private static final int WELL_H = 50;
    private static final int ENERGY_X = 153;
    private static final int WATER_X = 9;
    private static final int KEY_X = 133;
    private static final int POWER_Y = 17;
    private static final int REDSTONE_Y = 35;
    private static final int STATUS_X = 28;
    private static final int STATUS_Y = 58;
    // Sprites, right of the panel: the lit arrow, the redstone keycap (plain and hovered), then the
    // power keys (on, on hovered, off, off hovered).
    private static final int SPRITE_ARROW_V = 0;
    private static final int SPRITE_KEY_V = 20;
    private static final int SPRITE_POWER_V = 36;

    private final ResourceLocation texture;

    public ProcessingScreen(ProcessingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // The machine bay starts at y 13, so the title sits higher than the default, and the
        // inventory label clears the bay's bottom edge (y 71).
        titleLabelY = 4;
        inventoryLabelY = 73;
        texture = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/" + menu.machine().id() + ".png");
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int arrow = Mth.ceil(menu.progress() * 24);
        graphics.blit(texture, leftPos + ARROW_X, topPos + ARROW_Y, 176, SPRITE_ARROW_V, arrow, 17);
        GuiGauges.glow(graphics, leftPos + ENERGY_X, topPos + WELL_Y, 8, WELL_H,
                menu.energy() / (float) ProcessingBlockEntity.capacity(menu.machine(), menu.modules()), GuiGauges.ENERGY);
        GuiGauges.glass(graphics, leftPos + ENERGY_X, topPos + WELL_Y, 8, WELL_H);
        if (menu.machine().usesWater()) {
            GuiGauges.fluid(graphics, leftPos + WATER_X, topPos + WELL_Y, 10, WELL_H,
                    menu.water() / (float) menu.machine().waterCapacity(), Fluids.WATER);
            GuiGauges.glass(graphics, leftPos + WATER_X, topPos + WELL_Y, 10, WELL_H);
        }

        int power = (menu.enabled() ? 0 : 2) + (isOver(KEY_X, POWER_Y, mouseX, mouseY) ? 1 : 0);
        graphics.blit(texture, leftPos + KEY_X, topPos + POWER_Y, 176 + power * 16, SPRITE_POWER_V, 16, 16);
        boolean hover = isOver(KEY_X, REDSTONE_Y, mouseX, mouseY);
        graphics.blit(texture, leftPos + KEY_X, topPos + REDSTONE_Y, hover ? 192 : 176, SPRITE_KEY_V, 16, 16);
        ItemStack icon = new ItemStack(switch (menu.redstoneMode()) {
            case IGNORED -> Items.GUNPOWDER;
            case HIGH -> Items.REDSTONE;
            case LOW -> Items.REDSTONE_TORCH;
        });
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
            case RUNNING -> GOOD;
            case IDLE, OFF -> DIM;
            case OUTPUT_FULL, REDSTONE -> WARN;
            case NO_POWER, NO_WATER, UNDERPOWERED -> BAD;
        };
        // Running, the line also shows the time left (the arrow's own tooltip is the recipe viewer's).
        Component line = status == Status.RUNNING && menu.ticksLeft() > 0
                ? Component.translatable("gui.ryzergen.processing.status.running_time", clock(menu.ticksLeft()))
                : Component.translatable("gui.ryzergen.processing.status." + status.name().toLowerCase(Locale.ROOT));
        graphics.drawString(font, line, STATUS_X, STATUS_Y, colour, false);
    }

    /** Ticks as a short clock, "0:45" or "3:05", for the status line. */
    private static String clock(int ticks) {
        int seconds = (ticks + 19) / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /** Ticks as "45s" or "3m 05s". */
    private static String duration(int ticks) {
        int seconds = (ticks + 19) / 20;
        return seconds < 60 ? seconds + "s" : String.format("%dm %02ds", seconds / 60, seconds % 60);
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
        if (wellRow && mx >= ENERGY_X - 1 && mx < ENERGY_X + 9) {
            String draw = String.format("%,d", ProcessingBlockEntity.energyPerTick(menu.machine(), menu.modules()));
            List<Component> lines = new ArrayList<>(List.of(
                    Component.translatable("gui.ryzergen.energy", String.format("%,d", menu.energy()),
                            String.format("%,d", ProcessingBlockEntity.capacity(menu.machine(), menu.modules()))),
                    Component.translatable("gui.ryzergen.processing.uses", draw).withStyle(style -> style.withColor(DIM)),
                    Component.translatable("gui.ryzergen.processing.speed", ProcessingBlockEntity.speed(menu.modules()))
                            .withStyle(style -> style.withColor(DIM))));
            if (menu.machine().gated()) {
                lines.add(Component.translatable("gui.ryzergen.processing.gated", draw).withStyle(style -> style.withColor(WARN)));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (mx >= STATUS_X && mx < STATUS_X + 78 && my >= STATUS_Y - 1 && my < STATUS_Y + 9 && menu.ticksLeft() > 0) {
            long cost = (long) menu.ticksLeft() * ProcessingBlockEntity.energyPerTick(menu.machine(), menu.modules());
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.ryzergen.processing.time_left", duration(menu.ticksLeft())),
                    Component.translatable("gui.ryzergen.processing.energy_left", String.format("%,d", cost))
                            .withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
        } else if (wellRow && menu.machine().usesWater() && mx >= WATER_X - 1 && mx < WATER_X + 11) {
            graphics.renderTooltip(font, Component.translatable("gui.ryzergen.intake_pump.water",
                    String.format("%,d", menu.water()), String.format("%,d", menu.machine().waterCapacity())), mouseX, mouseY);
        } else if (hoveredSlot != null && hoveredSlot.getSlotIndex() == menu.machine().upgradeSlot() && !hoveredSlot.hasItem()) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.ryzergen.processing.upgrade_slot"),
                    Component.translatable("gui.ryzergen.processing.upgrade_hint", ProcessingMachine.MAX_MODULES)
                            .withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
        } else if (isOver(KEY_X, POWER_Y, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable(menu.enabled() ? "gui.ryzergen.processing.power.on" : "gui.ryzergen.processing.power.off"),
                    Component.translatable("gui.ryzergen.power.hint").withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
        } else if (isOver(KEY_X, REDSTONE_Y, mouseX, mouseY)) {
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
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            int id = isOver(KEY_X, POWER_Y, mouseX, mouseY) ? ProcessingMenu.BUTTON_POWER
                    : isOver(KEY_X, REDSTONE_Y, mouseX, mouseY) ? ProcessingMenu.BUTTON_REDSTONE : -1;
            if (id >= 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
