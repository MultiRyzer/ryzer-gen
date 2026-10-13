package com.ryzer.ryzergen.cable;

import org.jetbrains.annotations.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import com.ryzer.ryzergen.client.text.RichText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import static com.ryzer.ryzergen.cable.CableMenu.*;

/**
 * Cable and pipe panel: one row per side joined to a machine, with the block there, a bar of what
 * came in on that side against its limit, and the side's fitting slot; a footer with the base limit
 * and what the network reaches; then the player's inventory. The panel grows with the rows: the
 * texture (art/tools/gui_textures.py, cable) is drawn for six rows, and the screen shows its top
 * part, then its inventory section.
 *
 * <p>On an item pipe, clicking a row opens that side's filter editor in place of the rows: a line
 * with the way back, the block, and the side's priority (down, the number, up), and a line with the
 * filter's entries and its mode key (only these, or all but these). An entry is set by clicking it
 * holding an item and cleared empty-handed. A row whose side has a filter or a priority carries a
 * cyan mark on its block.
 */
public class CableScreen extends AbstractContainerScreen<CableMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/cable.png");
    private static final int TEXTURE_W = 256;
    private static final int TEXTURE_H = 320;
    /** Where the texture's inventory section starts (after six rows), and its height. */
    private static final int INVENTORY_V = CableMenu.inventoryTop(6);
    private static final int INVENTORY_H = 100;
    private static final int LABEL = 0xFF2B3036;
    private static final int NAME = 0xFFB3BBC4;
    private static final int VALUE = 0xFF35C8F5;
    private static final int DIM = 0xFF4D545D;
    private static final int HINT = 0xFF9AA3AE;
    private static final int WELL = 0xFF1D2126;
    private static final int WELL_LIGHT = 0xFF4D545D;
    private static final int ROW_H = 22;
    /** Rows stop short of the fitting slot at their right end. */
    private static final int ROW_WELL_W = ROW_W - 22;
    private static final int BAR_X = 22;
    private static final int BAR_Y = 13;
    private static final int BAR_H = 5;
    /**
     * A second of samples, so round-robin delivery does not make the numbers flicker. Energy shows
     * their average per tick; items move in bursts, so they show the total per second.
     */
    private static final int SAMPLES = 20;

    private final int[][] samples = new int[6][SAMPLES];
    private int sample;
    /** The side whose filter editor is open (item pipes), or none. */
    private @Nullable Direction editing;
    // The editor, in the panel's coordinates: its first line on the first row, its second on the next.
    private static final int KEY = 12;
    private static final int BACK_X = ROW_X + 3;
    private static final int PLUS_X = ROW_X + ROW_W - 3 - KEY;
    private static final int MINUS_X = PLUS_X - 26;
    private static final int ENTRY_X = ROW_X + 3;
    private static final int MODE_X = ROW_X + ROW_W - 3 - 16;
    private static final int GOOD = 0xFF44D65E;
    private static final int BAD = 0xFFD13C3C;

    public CableScreen(CableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = CableMenu.inventoryTop(menu.sides().size()) + INVENTORY_H;
        inventoryLabelY = CableMenu.inventoryTop(menu.sides().size()) + 7;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (Direction dir : Direction.values()) {
            samples[dir.get3DDataValue()][sample] = menu.moved(dir);
        }
        sample = (sample + 1) % SAMPLES;
    }

    @Override
    protected void init() {
        super.init();
        for (Direction dir : Direction.values()) {
            java.util.Arrays.fill(samples[dir.get3DDataValue()], menu.moved(dir));
        }
    }

    private int average(Direction dir) {
        long total = 0;
        for (int value : samples[dir.get3DDataValue()]) {
            total += value;
        }
        return menu.kind().perSecond() ? (int) total : (int) Math.round(total / (double) SAMPLES);
    }

    private String key(String name) {
        return "gui.ryzergen." + menu.kind().langKey() + "." + name;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int top = CableMenu.inventoryTop(menu.sides().size());
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, top, TEXTURE_W, TEXTURE_H);
        graphics.blit(TEXTURE, leftPos, topPos + top, 0, INVENTORY_V, imageWidth, INVENTORY_H, TEXTURE_W, TEXTURE_H);

        List<Direction> sides = menu.sides();
        if (editing != null) {
            editorBg(graphics, editing, mouseX, mouseY);
            return;
        }
        ItemPipeBlockEntity pipe = pipe();
        for (int row = 0; row < sides.size(); row++) {
            Direction dir = sides.get(row);
            int x = leftPos + ROW_X;
            int y = topPos + rowY(row);
            boolean feeds = menu.feeds(dir);
            well(graphics, x, y, feeds ? ROW_WELL_W : ROW_W, ROW_H);
            ItemStack icon = sourceIcon(dir);
            if (!icon.isEmpty()) {
                graphics.renderItem(icon, x + 3, y + 3);
            }
            if (pipe != null && (pipe.filtered(dir) || pipe.priority(dir) != 0)) {
                // The mark: a small cyan funnel on the block's corner.
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                graphics.fill(x + 13, y + 14, x + 19, y + 15, VALUE);
                graphics.fill(x + 14, y + 15, x + 18, y + 16, VALUE);
                graphics.fill(x + 15, y + 16, x + 17, y + 19, VALUE);
                graphics.pose().popPose();
            }
            if (!feeds) {
                continue;
            }
            slot(graphics, leftPos + FITTING_X - 1, y + FITTING_DY - 1);
            int barX = x + BAR_X;
            int barW = ROW_WELL_W - BAR_X - 4;
            graphics.fill(barX, y + BAR_Y, barX + barW, y + BAR_Y + BAR_H, 0xFF2B3036);
            int max = menu.maxRate(dir);
            float fraction = max == 0 ? 0 : Math.min(1, average(dir) / (float) max);
            GuiGauges.glowHorizontal(graphics, barX, y + BAR_Y, barW, BAR_H, fraction, menu.kind().colour());
            for (int tick = 1; tick < 4; tick++) {
                graphics.fill(barX + barW * tick / 4, y + BAR_Y, barX + barW * tick / 4 + 1, y + BAR_Y + BAR_H, 0x50000000);
            }
        }
        well(graphics, leftPos + ROW_X, topPos + rowY(Math.max(sides.size(), 1)), ROW_W, ROW_H);
    }

    /** A row's filter and priority, and that a click opens its editor (item pipes). */
    private void filterLines(List<Component> lines, @Nullable ItemPipeBlockEntity pipe, Direction dir) {
        if (pipe == null) {
            return;
        }
        if (pipe.filtered(dir)) {
            int listed = 0;
            for (int i = 0; i < ItemPipeBlockEntity.FILTER_SLOTS; i++) {
                listed += pipe.filter(dir, i).isEmpty() ? 0 : 1;
            }
            lines.add(RichText.tr(pipe.onlyListed(dir) ? "gui.ryzergen.item_pipe.filter.only_count" : "gui.ryzergen.item_pipe.filter.except_count",
                    listed).withStyle(style -> style.withColor(VALUE)));
        }
        if (pipe.priority(dir) != 0) {
            lines.add(RichText.tr("gui.ryzergen.item_pipe.priority", pipe.priority(dir)).withStyle(style -> style.withColor(VALUE)));
        }
        lines.add(RichText.tr("gui.ryzergen.item_pipe.filter.open").withStyle(style -> style.withColor(HINT)));
    }

    private void editorTooltip(GuiGraphics graphics, Direction dir, int mouseX, int mouseY) {
        ItemPipeBlockEntity pipe = pipe();
        if (pipe == null) {
            return;
        }
        Component hint = null;
        Component head = null;
        if (over(BACK_X, rowY(0) + 5, KEY, KEY, mouseX, mouseY)) {
            head = RichText.tr("gui.ryzergen.item_pipe.filter.back");
        } else if (over(MINUS_X, rowY(0) + 5, PLUS_X + KEY - MINUS_X, KEY, mouseX, mouseY)) {
            head = RichText.tr("gui.ryzergen.item_pipe.priority", pipe.priority(dir));
            hint = RichText.tr("gui.ryzergen.item_pipe.priority.hint");
        } else if (over(MODE_X, rowY(1) + 3, 16, 16, mouseX, mouseY)) {
            head = RichText.tr(pipe.onlyListed(dir) ? "gui.ryzergen.item_pipe.filter.only" : "gui.ryzergen.item_pipe.filter.except");
            hint = RichText.tr(menu.mode(dir) == CableSide.EXTRACT ? "gui.ryzergen.item_pipe.filter.on_extract"
                    : "gui.ryzergen.item_pipe.filter.on_joined");
        } else {
            for (int i = 0; i < ItemPipeBlockEntity.FILTER_SLOTS; i++) {
                if (over(ENTRY_X + i * 18, rowY(1) + 2, 18, 18, mouseX, mouseY)) {
                    ItemStack entry = pipe.filter(dir, i);
                    head = entry.isEmpty() ? RichText.tr("gui.ryzergen.item_pipe.filter.slot") : entry.getHoverName();
                    hint = RichText.tr(entry.isEmpty() ? "gui.ryzergen.item_pipe.filter.slot_hint" : "gui.ryzergen.item_pipe.filter.slot_clear");
                }
            }
        }
        if (head != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(head);
            if (hint != null) {
                Component shown = hint;
                lines.add(shown.copy().withStyle(style -> style.withColor(HINT)));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    /** The open pipe, on the client, for its filters and priorities (an item pipe's panel only). */
    private @Nullable ItemPipeBlockEntity pipe() {
        if (menu.kind() != CableKind.ITEMS || minecraft == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getBlockEntity(menu.pos()) instanceof ItemPipeBlockEntity pipe ? pipe : null;
    }

    private void editorBg(GuiGraphics graphics, Direction dir, int mouseX, int mouseY) {
        ItemPipeBlockEntity pipe = pipe();
        int x = leftPos + ROW_X;
        int top = topPos + rowY(0);
        int line = topPos + rowY(1);
        well(graphics, x, top, ROW_W, ROW_H);
        well(graphics, x, line, ROW_W, ROW_H);
        key(graphics, leftPos + BACK_X, top + 5, KEY, KEY, over(BACK_X, rowY(0) + 5, KEY, KEY, mouseX, mouseY));
        key(graphics, leftPos + MINUS_X, top + 5, KEY, KEY, over(MINUS_X, rowY(0) + 5, KEY, KEY, mouseX, mouseY));
        key(graphics, leftPos + PLUS_X, top + 5, KEY, KEY, over(PLUS_X, rowY(0) + 5, KEY, KEY, mouseX, mouseY));
        for (int i = 0; i < ItemPipeBlockEntity.FILTER_SLOTS; i++) {
            int sx = leftPos + ENTRY_X + i * 18;
            slot(graphics, sx, line + 2);
            ItemStack entry = pipe == null ? ItemStack.EMPTY : pipe.filter(dir, i);
            if (!entry.isEmpty()) {
                graphics.renderItem(entry, sx + 1, line + 3);
            }
        }
        boolean only = pipe != null && pipe.onlyListed(dir);
        int mx = leftPos + MODE_X;
        int my = line + 3;
        key(graphics, mx, my, 16, 16, over(MODE_X, rowY(1) + 3, 16, 16, mouseX, mouseY));
        if (only) {
            // Only these: a green tick.
            graphics.fill(mx + 4, my + 8, mx + 6, my + 10, GOOD);
            graphics.fill(mx + 6, my + 10, mx + 8, my + 12, GOOD);
            graphics.fill(mx + 8, my + 8, mx + 10, my + 10, GOOD);
            graphics.fill(mx + 10, my + 6, mx + 12, my + 8, GOOD);
        } else {
            // All but these: a red cross.
            for (int k = 0; k < 7; k++) {
                graphics.fill(mx + 4 + k, my + 4 + k, mx + 6 + k, my + 6 + k, BAD);
                graphics.fill(mx + 10 - k, my + 4 + k, mx + 12 - k, my + 6 + k, BAD);
            }
        }
    }

    /** A small key: a graphite keycap, lit along its top, a dark lip at its foot. */
    private static void key(GuiGraphics graphics, int x, int y, int w, int h, boolean hover) {
        graphics.fill(x, y, x + w, y + h, 0xFF1D2126);
        graphics.fill(x, y, x + w, y + h - 1, hover ? 0xFF4D545D : 0xFF3A4047);
        graphics.fill(x, y, x + w, y + 1, hover ? 0xFF646C77 : 0xFF4D545D);
    }

    /** Whether the mouse is over a box in the panel's coordinates. */
    private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
        double mx = mouseX - leftPos;
        double my = mouseY - topPos;
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void edit(@Nullable Direction dir) {
        editing = dir;
        menu.editing(dir != null);
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.kind() == CableKind.ITEMS) {
            if (editing != null) {
                if (over(BACK_X, rowY(0) + 5, KEY, KEY, mouseX, mouseY)) {
                    edit(null);
                    return true;
                }
                if (over(MINUS_X, rowY(0) + 5, KEY, KEY, mouseX, mouseY)) {
                    press(CableMenu.BUTTON_PRIORITY_DOWN + editing.get3DDataValue());
                    return true;
                }
                if (over(PLUS_X, rowY(0) + 5, KEY, KEY, mouseX, mouseY)) {
                    press(CableMenu.BUTTON_PRIORITY_UP + editing.get3DDataValue());
                    return true;
                }
                if (over(MODE_X, rowY(1) + 3, 16, 16, mouseX, mouseY)) {
                    press(CableMenu.BUTTON_ONLY + editing.get3DDataValue());
                    return true;
                }
                for (int i = 0; i < ItemPipeBlockEntity.FILTER_SLOTS; i++) {
                    if (over(ENTRY_X + i * 18, rowY(1) + 2, 18, 18, mouseX, mouseY)) {
                        press(CableMenu.filterButton(editing, i));
                        return true;
                    }
                }
            } else {
                List<Direction> sides = menu.sides();
                for (int row = 0; row < sides.size(); row++) {
                    Direction dir = sides.get(row);
                    if (over(ROW_X, rowY(row), menu.feeds(dir) ? ROW_WELL_W : ROW_W, ROW_H, mouseX, mouseY)) {
                        edit(dir);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Escape (or the inventory key) shuts the editor first, then the panel. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing != null && (keyCode == 256 || minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            edit(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** A sunken readout, the same as the texture's screens: dark glass, a lit lower and right edge. */
    private static void well(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, WELL);
        graphics.fill(x, y + h - 1, x + w, y + h, WELL_LIGHT);
        graphics.fill(x + w - 1, y, x + w, y + h, WELL_LIGHT);
    }

    /** An 18 x 18 item slot like the texture's: a mid grey well, shaded top and left, lit bottom and right. */
    private static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF868F9A);
        graphics.fill(x, y, x + 18, y + 1, 0xFF4D545D);
        graphics.fill(x, y, x + 1, y + 18, 0xFF4D545D);
        graphics.fill(x, y + 17, x + 18, y + 18, 0xFFE1E6EB);
        graphics.fill(x + 17, y, x + 18, y + 18, 0xFFE1E6EB);
        graphics.fill(x + 1, y + 1, x + 17, y + 2, 0xFF9EA7B1);
        graphics.fill(x + 1, y + 1, x + 2, y + 17, 0xFF9EA7B1);
    }

    private BlockState source(Direction dir) {
        Level level = minecraft == null ? null : minecraft.level;
        return level == null ? null : level.getBlockState(menu.pos().relative(dir));
    }

    private ItemStack sourceIcon(Direction dir) {
        BlockState state = source(dir);
        return state == null ? ItemStack.EMPTY : new ItemStack(state.getBlock());
    }

    private Component sourceName(Direction dir) {
        BlockState state = source(dir);
        return state == null ? Component.empty() : state.getBlock().getName();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
        List<Direction> sides = menu.sides();
        if (editing != null) {
            ItemPipeBlockEntity pipe = pipe();
            int y = rowY(0);
            graphics.drawString(font, "<", BACK_X + 3, y + 7, NAME, false);
            graphics.drawString(font, "-", MINUS_X + 4, y + 7, NAME, false);
            graphics.drawString(font, "+", PLUS_X + 3, y + 7, NAME, false);
            String number = String.valueOf(pipe == null ? 0 : pipe.priority(editing));
            graphics.drawString(font, number, MINUS_X + KEY + (PLUS_X - MINUS_X - KEY - font.width(number)) / 2, y + 7, VALUE, false);
            int nameX = BACK_X + KEY + 4;
            graphics.drawString(font, font.plainSubstrByWidth(sourceName(editing).getString(), MINUS_X - 4 - nameX), nameX, y + 7, NAME, false);
            return;
        }
        for (int row = 0; row < sides.size(); row++) {
            Direction dir = sides.get(row);
            int y = rowY(row);
            if (!menu.feeds(dir)) {
                // A machine that only takes from the network: nothing comes in here, so no fitting.
                String note = RichText.tr("gui.ryzergen.cable.receives_only").getString();
                int noteX = ROW_X + ROW_W - 4 - font.width(note);
                graphics.drawString(font, note, noteX, y + 7, DIM, false);
                graphics.drawString(font, font.plainSubstrByWidth(sourceName(dir).getString(), noteX - ROW_X - BAR_X - 4),
                        ROW_X + BAR_X, y + 7, NAME, false);
                continue;
            }
            int current = average(dir);
            // What came in: the limit is in the tooltip and the bar shows how close it is.
            String value = RichText.tr(key("rate"), String.format("%,d", current)).getString();
            int valueX = ROW_X + ROW_WELL_W - 4 - font.width(value);
            graphics.drawString(font, value, valueX, y + 3, current > 0 ? VALUE : DIM, false);
            String name = font.plainSubstrByWidth(sourceName(dir).getString(), valueX - ROW_X - BAR_X - 4);
            graphics.drawString(font, name, ROW_X + BAR_X, y + 3, NAME, false);
        }
        int footer = rowY(Math.max(sides.size(), 1));
        graphics.drawString(font, RichText.tr(key("max"), String.format("%,d", menu.baseRate())),
                ROW_X + 4, footer + 3, VALUE, false);
        // What the network reaches, amber when it reaches nothing. Batteries only exist on energy networks.
        int receivers = menu.receivers();
        int buffers = menu.buffers();
        Component network = buffers > 0
                ? RichText.tr(key("network_buffers"), receivers, buffers)
                : RichText.tr(key("network"), receivers);
        graphics.drawString(font, network, ROW_X + 4, footer + 12, receivers + buffers > 0 ? NAME : 0xFFF0A030, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        List<Direction> sides = menu.sides();
        if (editing != null) {
            editorTooltip(graphics, editing, mouseX, mouseY);
            return;
        }
        ItemPipeBlockEntity filtering = pipe();
        for (int row = 0; row < sides.size(); row++) {
            int y = rowY(row);
            Direction dir = sides.get(row);
            String sideName = RichText.tr("gui.ryzergen.direction." + dir.getName()).getString();
            if (!menu.feeds(dir)) {
                if (mx >= ROW_X && mx < ROW_X + ROW_W && my >= y && my < y + ROW_H) {
                    List<Component> lines = new ArrayList<>(List.of(sourceName(dir),
                            RichText.tr("gui.ryzergen.cable.receives_only_hint").withStyle(style -> style.withColor(HINT))));
                    filterLines(lines, filtering, dir);
                    graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
                }
                continue;
            }
            if (mx >= ROW_X && mx < ROW_X + ROW_WELL_W && my >= y && my < y + ROW_H) {
                List<Component> lines = new ArrayList<>();
                lines.add(sourceName(dir));
                lines.add(RichText.tr(menu.mode(dir) == CableSide.EXTRACT ? "gui.ryzergen.cable.side"
                        : "gui.ryzergen.cable.side_joined", sideName).withStyle(style -> style.withColor(HINT)));
                lines.add(RichText.tr("gui.ryzergen.cable.limit",
                        RichText.tr(key("rate"), String.format("%,d", menu.maxRate(dir))))
                        .withStyle(style -> style.withColor(VALUE)));
                lines.add(RichText.tr(key("average")).withStyle(style -> style.withColor(HINT)));
                filterLines(lines, filtering, dir);
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            } else if (mx >= FITTING_X - 1 && mx < FITTING_X + 17 && my >= y + FITTING_DY - 1 && my < y + FITTING_DY + 17
                    && menu.upgrade(dir) == CableUpgrade.NONE && menu.getCarried().isEmpty()) {
                graphics.renderComponentTooltip(font, List.of(
                        RichText.tr("gui.ryzergen.cable.fitting_slot", sideName),
                        RichText.tr("gui.ryzergen.cable.fitting_hint").withStyle(style -> style.withColor(HINT))),
                        mouseX, mouseY);
            }
        }
    }
}
