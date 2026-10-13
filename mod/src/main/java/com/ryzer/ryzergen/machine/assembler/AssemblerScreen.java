package com.ryzer.ryzergen.machine.assembler;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import com.ryzer.ryzergen.client.SoundTab;
import com.ryzer.ryzergen.client.text.RichText;
import com.ryzer.ryzergen.machine.assembler.AssemblerBlockEntity.Status;
import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.ryzer.ryzergen.machine.assembler.AssemblerBlockEntity.GRID;
import static com.ryzer.ryzergen.machine.assembler.AssemblerBlockEntity.PATTERNS;

/**
 * The formed Assembler's controls. On the left the pattern rack: nine patterns, each showing what it
 * makes; click one to edit it in the ghost grid beside it, where clicking a cell with an item sets
 * it and clicking empty-handed clears it, the result showing at the arrow's end. Under them the power
 * and redstone keys, the speed module slot and the readout (what it is making, or what it waits on),
 * then the buffer and the output. Positions match art/tools/gui_textures.py (assembler).
 */
public class AssemblerScreen extends AbstractContainerScreen<AssemblerMenu> implements SoundTab.Host {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/assembler.png");
    private static final int LABEL = 0xFF2B3036;
    private static final int VALUE = 0xFF35C8F5;
    private static final int DIM = 0xFF9AA3AE;
    private static final int GOOD = 0xFF44D65E;
    private static final int WARN = 0xFFF0A030;
    private static final int BAD = 0xFFE0503C;
    private static final int BAYS_X = 8;
    private static final int GRID_X = 67;
    private static final int GRID_Y = 17;
    private static final int RESULT_X = 137;
    private static final int RESULT_Y = 35;
    private static final int CLEAR_X = 137;
    private static final int CLEAR_Y = 54;
    private static final int KEY_Y = 73;
    private static final int POWER_X = 8;
    private static final int REDSTONE_X = 26;
    private static final int READOUT_X = 69;
    private static final int READOUT_W = 96;
    private static final int READOUT_Y = 76;
    private static final int ENERGY_X = 159;
    private static final int ENERGY_Y = 17;
    private static final int ENERGY_W = 8;
    private static final int ENERGY_H = 52;
    /** The pattern being edited (client only). */
    private int selected;

    public AssemblerScreen(AssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 226;
        titleLabelY = 4;
        inventoryLabelY = AssemblerMenu.INVENTORY_Y - 11;
    }

    private AssemblerBlockEntity cell() {
        return minecraft == null || minecraft.level == null ? null : menu.machine(minecraft.level);
    }

    private static boolean over(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** The pattern bay or grid cell under the mouse (relative to the panel), or -1. */
    private static int bayAt(int mx, int my) {
        for (int i = 0; i < PATTERNS; i++) {
            if (over(BAYS_X + (i % 3) * 18, GRID_Y + (i / 3) * 18, 16, 16, mx, my)) {
                return i;
            }
        }
        return -1;
    }

    private static int gridAt(int mx, int my) {
        for (int i = 0; i < GRID; i++) {
            if (over(GRID_X + (i % 3) * 18, GRID_Y + (i / 3) * 18, 16, 16, mx, my)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        int mx = mouseX - x0;
        int my = mouseY - y0;
        graphics.blit(TEXTURE, x0, y0, 0, 0, imageWidth, imageHeight);
        SoundTab.render(graphics, x0, y0, imageWidth, soundTabY(), menu.muted(), mouseX, mouseY);
        GuiGauges.glow(graphics, x0 + ENERGY_X, y0 + ENERGY_Y, ENERGY_W, ENERGY_H,
                menu.energy() / (float) AssemblerBlockEntity.ENERGY_CAPACITY, GuiGauges.ENERGY);
        GuiGauges.glass(graphics, x0 + ENERGY_X, y0 + ENERGY_Y, ENERGY_W, ENERGY_H);
        int power = (menu.enabled() ? 0 : 2) + (over(POWER_X, KEY_Y, 16, 16, mx, my) ? 1 : 0);
        graphics.blit(TEXTURE, x0 + POWER_X, y0 + KEY_Y, 176 + power * 16, 16, 16, 16);
        graphics.blit(TEXTURE, x0 + REDSTONE_X, y0 + KEY_Y, over(REDSTONE_X, KEY_Y, 16, 16, mx, my) ? 192 : 176, 0, 16, 16);
        graphics.blit(TEXTURE, x0 + CLEAR_X, y0 + CLEAR_Y, over(CLEAR_X, CLEAR_Y, 16, 16, mx, my) ? 224 : 208, 0, 16, 16);
        ItemStack icon = new ItemStack(switch (menu.redstoneMode()) {
            case IGNORED -> Items.GUNPOWDER;
            case HIGH -> Items.REDSTONE;
            case LOW -> Items.REDSTONE_TORCH;
        });
        graphics.pose().pushPose();
        graphics.pose().translate(x0 + REDSTONE_X + 2.5F, y0 + KEY_Y + 1.5F, 0);
        graphics.pose().scale(0.7F, 0.7F, 1);
        graphics.renderItem(icon, 0, 0);
        graphics.pose().popPose();

        AssemblerBlockEntity cell = cell();
        if (cell == null) {
            return;
        }
        // The rack: each pattern's result, the chosen one framed in cyan, the one at work marked green
        // and the one waiting for ingredients amber.
        for (int p = 0; p < PATTERNS; p++) {
            int bx = x0 + BAYS_X + (p % 3) * 18;
            int by = y0 + GRID_Y + (p / 3) * 18;
            if (p == selected) {
                frame(graphics, bx - 1, by - 1, 18, 18, VALUE);
            }
            ItemStack result = cell.result(p);
            if (!result.isEmpty()) {
                graphics.renderItem(result, bx, by);
            } else if (!cell.patternEmpty(p)) {
                // Set, but no recipe makes it: a red mark.
                graphics.fill(bx + 5, by + 5, bx + 11, by + 11, 0x80E0503C);
            }
            int lamp = p == menu.active() ? GOOD : p == menu.waiting() ? WARN : 0;
            if (lamp != 0) {
                graphics.fill(bx + 13, by, bx + 16, by + 3, lamp);
            }
        }
        // The chosen pattern's grid, its items drawn as ghosts, cells it lacks tinted red.
        for (int c = 0; c < GRID; c++) {
            int gx = x0 + GRID_X + (c % 3) * 18;
            int gy = y0 + GRID_Y + (c / 3) * 18;
            ItemStack ghost = cell.ghost(selected, c);
            if (!ghost.isEmpty()) {
                graphics.renderItem(ghost, gx, gy);
                graphics.fill(gx, gy, gx + 16, gy + 16, 300, 0x408B949E);
            }
            if (selected == menu.waiting() && (menu.missingMask() & (1 << c)) != 0) {
                graphics.fill(gx, gy, gx + 16, gy + 16, 300, 0x60E0503C);
            }
        }
        ItemStack result = cell.result(selected);
        if (!result.isEmpty()) {
            graphics.renderItem(result, x0 + RESULT_X, y0 + RESULT_Y);
            graphics.renderItemDecorations(font, result, x0 + RESULT_X, y0 + RESULT_Y);
        }
        // The cycle's progress, under the readout.
        if (menu.active() >= 0) {
            GuiGauges.glowHorizontal(graphics, x0 + READOUT_X, y0 + READOUT_Y + 10, READOUT_W, 2, menu.progress(), VALUE);
        }
    }

    private static void frame(GuiGraphics graphics, int x, int y, int w, int h, int colour) {
        graphics.fill(x, y, x + w, y + 1, colour);
        graphics.fill(x, y + h - 1, x + w, y + h, colour);
        graphics.fill(x, y, x + 1, y + h, colour);
        graphics.fill(x + w - 1, y, x + w, y + h, colour);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
        Status status = menu.status();
        int colour = switch (status) {
            case WORKING -> GOOD;
            case OFF, NO_PATTERN -> DIM;
            case REDSTONE, STARVED, OUTPUT_FULL -> WARN;
            case NO_POWER -> BAD;
        };
        Component word = RichText.tr("gui.ryzergen.assembler.status." + status.name().toLowerCase(Locale.ROOT));
        graphics.drawString(font, word, 169 - font.width(word), titleLabelY, colour, false);
        // The readout: the item it is making, or the one the pattern it waits on lacks, with a word.
        AssemblerBlockEntity cell = cell();
        ItemStack shown = readoutItem(cell);
        Component line = null;
        int lineColour = VALUE;
        if (cell != null && menu.active() >= 0) {
            line = RichText.tr("gui.ryzergen.assembler.making", cell.batch());
        } else if (!shown.isEmpty()) {
            line = RichText.tr("gui.ryzergen.assembler.needs");
            lineColour = WARN;
        } else if (status == Status.NO_PATTERN) {
            line = RichText.tr("gui.ryzergen.assembler.no_pattern");
            lineColour = DIM;
        }
        int textX = READOUT_X;
        if (!shown.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(READOUT_X, READOUT_Y - 2.5F, 0);
            graphics.pose().scale(0.75F, 0.75F, 1);
            graphics.renderItem(shown, 0, 0);
            graphics.pose().popPose();
            textX += 15;
        }
        if (line != null) {
            graphics.drawString(font, line, textX, READOUT_Y, lineColour, false);
        }
    }

    /** The item the readout shows: what is being made, or what the cell waits for. */
    private ItemStack readoutItem(AssemblerBlockEntity cell) {
        if (cell == null) {
            return ItemStack.EMPTY;
        }
        if (menu.active() >= 0) {
            return cell.result(menu.active());
        }
        if (menu.status() == Status.STARVED && menu.waiting() >= 0) {
            for (int c = 0; c < GRID; c++) {
                if ((menu.missingMask() & (1 << c)) != 0) {
                    return cell.ghost(menu.waiting(), c);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (SoundTab.tooltip(graphics, font, leftPos, topPos, imageWidth, soundTabY(), menu.muted(), mouseX, mouseY)) {
            return;
        }
        renderTooltip(graphics, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        AssemblerBlockEntity cell = cell();
        List<Component> lines = new ArrayList<>();
        int bay = bayAt(mx, my);
        int grid = gridAt(mx, my);
        if (bay >= 0 && cell != null) {
            ItemStack result = cell.result(bay);
            lines.add(result.isEmpty() ? RichText.tr(cell.patternEmpty(bay) ? "gui.ryzergen.assembler.pattern.empty"
                    : "gui.ryzergen.assembler.pattern.none", bay + 1) : RichText.tr("gui.ryzergen.assembler.pattern", bay + 1, result.getHoverName()));
            lines.add(RichText.tr("gui.ryzergen.assembler.pattern.hint").withStyle(style -> style.withColor(DIM)));
        } else if (grid >= 0 && cell != null) {
            ItemStack ghost = cell.ghost(selected, grid);
            if (!ghost.isEmpty()) {
                lines.add(ghost.getHoverName());
                lines.add(RichText.tr("gui.ryzergen.assembler.grid.any").withStyle(style -> style.withColor(DIM)));
            }
            lines.add(RichText.tr("gui.ryzergen.assembler.grid.hint").withStyle(style -> style.withColor(DIM)));
        } else if (over(RESULT_X, RESULT_Y, 16, 16, mx, my) && cell != null && !cell.result(selected).isEmpty()) {
            graphics.renderTooltip(font, cell.result(selected), mouseX, mouseY);
            return;
        } else if (over(READOUT_X - 2, READOUT_Y - 3, READOUT_W + 4, 14, mx, my) && !readoutItem(cell).isEmpty()) {
            lines.add(readoutItem(cell).getHoverName());
        } else if (over(CLEAR_X, CLEAR_Y, 16, 16, mx, my)) {
            lines.add(RichText.tr("gui.ryzergen.assembler.clear"));
            lines.add(RichText.tr("gui.ryzergen.assembler.clear.hint").withStyle(style -> style.withColor(DIM)));
        } else if (over(ENERGY_X - 1, ENERGY_Y - 1, ENERGY_W + 2, ENERGY_H + 2, mx, my)) {
            lines.add(RichText.tr("gui.ryzergen.energy", String.format("%,d", menu.energy()),
                    String.format("%,d", AssemblerBlockEntity.ENERGY_CAPACITY)));
            lines.add(RichText.tr("gui.ryzergen.assembler.uses", String.format("%,d", menu.draw())).withStyle(style -> style.withColor(DIM)));
        } else if (over(POWER_X, KEY_Y, 16, 16, mx, my)) {
            lines.add(RichText.tr(menu.enabled() ? "gui.ryzergen.processing.power.on" : "gui.ryzergen.processing.power.off"));
            lines.add(RichText.tr("gui.ryzergen.power.hint").withStyle(style -> style.withColor(DIM)));
        } else if (over(REDSTONE_X, KEY_Y, 16, 16, mx, my)) {
            String mode = switch (menu.redstoneMode()) {
                case IGNORED -> "gui.ryzergen.redstone.ignored";
                case HIGH -> "gui.ryzergen.redstone.high";
                case LOW -> "gui.ryzergen.redstone.low";
            };
            lines.add(RichText.tr(mode));
            lines.add(RichText.tr("gui.ryzergen.redstone.hint").withStyle(style -> style.withColor(DIM)));
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < AssemblerMenu.MACHINE_SLOTS) {
            if (hoveredSlot.index == 0) {
                lines.add(RichText.tr("gui.ryzergen.processing.upgrade_slot"));
                lines.add(RichText.tr("gui.ryzergen.processing.upgrade_hint", ProcessingMachine.MAX_MODULES).withStyle(style -> style.withColor(DIM)));
            } else if (hoveredSlot.index < AssemblerMenu.FIRST_OUTPUT) {
                lines.add(RichText.tr("gui.ryzergen.assembler.buffer"));
                lines.add(RichText.tr("gui.ryzergen.assembler.buffer.hint").withStyle(style -> style.withColor(DIM)));
            } else {
                lines.add(RichText.tr("gui.ryzergen.assembler.output"));
            }
        }
        if (!lines.isEmpty()) {
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && SoundTab.click(menu, leftPos, topPos, imageWidth, soundTabY(), mouseX, mouseY)) {
            return true;
        }
        int mx = (int) mouseX - leftPos;
        int my = (int) mouseY - topPos;
        int bay = bayAt(mx, my);
        if (bay >= 0 && (button == 0 || button == 1)) {
            selected = bay;
            click(-1);
            return true;
        }
        int grid = gridAt(mx, my);
        if (grid >= 0 && (button == 0 || button == 1)) {
            // A left click sets the cell from the held item (empty-handed, clears it); a right click clears it.
            click((button == 1 ? AssemblerMenu.BUTTON_UNSET : AssemblerMenu.BUTTON_GHOST) + selected * GRID + grid);
            return true;
        }
        if (button == 0) {
            int id = over(POWER_X, KEY_Y, 16, 16, mx, my) ? AssemblerMenu.BUTTON_POWER
                    : over(REDSTONE_X, KEY_Y, 16, 16, mx, my) ? AssemblerMenu.BUTTON_REDSTONE
                    : over(CLEAR_X, CLEAR_Y, 16, 16, mx, my) ? AssemblerMenu.BUTTON_CLEAR + selected : -2;
            if (id != -2) {
                click(id);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Presses a menu button ({@code -1}: none, only the click), with the keys' click. */
    private void click(int id) {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }
        if (id >= 0) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public int soundTabY() {
        return SoundTab.TOP;
    }
}
