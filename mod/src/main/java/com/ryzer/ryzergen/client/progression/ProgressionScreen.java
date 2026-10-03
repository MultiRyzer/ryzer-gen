package com.ryzer.ryzergen.client.progression;

import com.ryzer.ryzergen.client.progression.ProgressionMap.Grid;
import com.ryzer.ryzergen.client.progression.ProgressionMap.Node;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The progression map (design pillar 6: roots run deep): the mod's whole production line, from ore
 * in the ground to the sun, in tier columns with an arrow from each step to the steps it feeds.
 * Steps the player has reached are lit; the steps they can make next are outlined amber; the rest
 * wait, dimmed. Drag or scroll to move round it. Click a step for a panel on how it works, with a
 * spoiler button that shows the best setups found (the optimisers' layouts, as the control screens
 * show them). Opened with its key (Y by default).
 */
public class ProgressionScreen extends Screen {
    private static final int BACKGROUND = 0xF0141820;
    private static final int HEADER = 0xFF1D2126;
    private static final int LINE = 0xFF1B5A73;
    private static final int CYAN = 0xFF35C8F5;
    private static final int TEXT = 0xFFE1E6EB;
    private static final int DIM = 0xFF9AA3AE;
    private static final int NEXT = 0xFFF0A030;
    private static final int NODE = 0xFF2B3036;
    private static final int NODE_AHEAD = 0xFF1F2328;
    private static final int TOP = 30;
    private static final int COLUMN_W = 118;
    private static final int ROW_H = 26;
    private static final int BOX = 20;
    private static final int MARGIN = 14;
    private static final int PANEL_W = 210;

    private final List<Node> nodes = ProgressionMap.nodes();
    private final Map<String, Node> byId = new HashMap<>();
    private float panX;
    private float panY;
    private @Nullable Node selected;
    private boolean spoilers;
    private float panelScroll;
    private int panelHeight;

    public ProgressionScreen() {
        super(Component.translatable("progression.ryzergen.title"));
        nodes.forEach(node -> byId.put(node.id(), node));
    }

    @Override
    protected void init() {
        // Ask the server for the player's statistics, so steps crafted or picked up count.
        if (minecraft != null && minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int columns() {
        int last = 0;
        for (Node node : nodes) {
            last = Math.max(last, node.column());
        }
        // With the preview off, one more column says there is more to come.
        return last + 1 + (last < ProgressionMap.COLUMNS.length - 1 ? 1 : 0);
    }

    private int nodeX(Node node) {
        return Math.round(MARGIN + node.column() * COLUMN_W + panX);
    }

    private int nodeY(Node node) {
        return Math.round(TOP + 22 + node.row() * ROW_H + panY);
    }

    private boolean reached(Node node) {
        return ProgressionTracker.reached(Minecraft.getInstance(), node.item());
    }

    /** Not reached yet, but everything that feeds it is: what the player can make next. */
    private boolean next(Node node) {
        if (reached(node)) {
            return false;
        }
        for (String input : node.inputs()) {
            Node from = byId.get(input);
            if (from != null && !reached(from)) {
                return false;
            }
        }
        return true;
    }

    private Component name(Node node) {
        String key = "progression.ryzergen." + node.id();
        return I18n.exists(key) ? Component.translatable(key) : new ItemStack(node.item()).getHoverName();
    }

    private void clampPan() {
        int mapW = MARGIN * 2 + columns() * COLUMN_W;
        int maxRow = 0;
        for (Node node : nodes) {
            maxRow = Math.max(maxRow, node.row());
        }
        int mapH = 22 + (maxRow + 1) * ROW_H + 8;
        int viewW = width - (selected != null ? PANEL_W : 0);
        int viewH = height - TOP;
        panX = Math.max(Math.min(0, viewW - mapW), Math.min(0, panX));
        panY = Math.max(Math.min(0, viewH - mapH), Math.min(0, panY));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);
        clampPan();
        Node hovered = nodeAt(mouseX, mouseY);
        int mapRight = width - (selected != null ? PANEL_W : 0);
        graphics.enableScissor(0, TOP, mapRight, height);
        renderColumns(graphics);
        renderEdges(graphics, hovered != null ? hovered : selected);
        for (Node node : nodes) {
            renderNode(graphics, node, node == hovered || node == selected);
        }
        graphics.disableScissor();
        renderHeader(graphics);
        if (selected != null) {
            renderPanel(graphics, mouseX, mouseY);
        }
        if (hovered != null && (selected == null || mouseX < mapRight)) {
            List<Component> lines = new ArrayList<>();
            lines.add(name(hovered));
            lines.add(Component.translatable(reached(hovered) ? "progression.ryzergen.reached"
                    : next(hovered) ? "progression.ryzergen.next" : "progression.ryzergen.ahead")
                    .withStyle(style -> style.withColor(reached(hovered) ? CYAN : next(hovered) ? NEXT : DIM)));
            lines.add(Component.translatable("progression.ryzergen.click").withStyle(style -> style.withColor(DIM)));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private void renderHeader(GuiGraphics graphics) {
        graphics.fill(0, 0, width, TOP, HEADER);
        graphics.fill(0, TOP - 1, width, TOP, LINE);
        graphics.drawString(font, title, 8, 6, TEXT, false);
        graphics.drawString(font, Component.translatable("progression.ryzergen.hint"), 8, 17, DIM, false);
        // The key: reached, next, ahead.
        int x = width - 8;
        String[] keys = {"progression.ryzergen.ahead", "progression.ryzergen.next", "progression.ryzergen.reached"};
        int[] colours = {DIM, NEXT, CYAN};
        for (int i = 0; i < keys.length; i++) {
            Component label = Component.translatable(keys[i]);
            x -= font.width(label);
            graphics.drawString(font, label, x, 11, colours[i], false);
            x -= 12;
            graphics.fill(x, 11, x + 8, 19, colours[i]);
            x -= 10;
        }
    }

    private void renderColumns(GuiGraphics graphics) {
        boolean previewOff = nodes.stream().noneMatch(Node::preview);
        for (int c = 0; c < columns(); c++) {
            int x = Math.round(MARGIN + c * COLUMN_W + panX);
            int y = Math.round(TOP + 6 + panY);
            if (c > 0) {
                graphics.fill(x - 8, TOP, x - 7, height, 0x30FFFFFF);
            }
            boolean more = previewOff && c == columns() - 1;
            String key = more ? "progression.ryzergen.column.more" : "progression.ryzergen.column." + ProgressionMap.COLUMNS[c];
            graphics.drawString(font, Component.translatable(key), x, y, more ? DIM : CYAN, false);
            if (more) {
                List<FormattedCharSequence> lines = font.split(Component.translatable("progression.ryzergen.column.more_hint"), COLUMN_W - 16);
                for (int i = 0; i < lines.size(); i++) {
                    graphics.drawString(font, lines.get(i), x, y + 16 + i * 10, DIM, false);
                }
            }
        }
    }

    /**
     * An arrow from each input to the step it feeds: right from the input, down or up in the gap
     * just before the step's column, then into it. Lit cyan where the input is reached; the
     * hovered or selected step's inputs stand out.
     */
    private void renderEdges(GuiGraphics graphics, @Nullable Node focus) {
        for (int pass = 0; pass < 2; pass++) {
            for (Node node : nodes) {
                boolean focused = node == focus;
                if ((pass == 1) != focused) {
                    continue;
                }
                for (String input : node.inputs()) {
                    Node from = byId.get(input);
                    if (from == null) {
                        continue;
                    }
                    int colour = focused ? 0xFFFFFFFF : reached(from) ? 0x9035C8F5 : 0x30FFFFFF;
                    int x1 = nodeX(from) + BOX;
                    int y1 = nodeY(from) + BOX / 2;
                    int x2 = nodeX(node);
                    int y2 = nodeY(node) + BOX / 2;
                    int mid = x2 - 5;
                    if (from.column() == node.column()) {
                        // Same column: down the step's left side.
                        mid = x2 - 4;
                        x1 = nodeX(from);
                    }
                    graphics.fill(Math.min(x1, mid), y1, Math.max(x1, mid) + 1, y1 + 1, colour);
                    graphics.fill(mid, Math.min(y1, y2), mid + 1, Math.max(y1, y2) + 1, colour);
                    graphics.fill(mid, y2, x2, y2 + 1, colour);
                    graphics.fill(x2 - 3, y2 - 1, x2 - 1, y2 + 2, colour);
                }
            }
        }
    }

    private void renderNode(GuiGraphics graphics, Node node, boolean highlight) {
        int x = nodeX(node);
        int y = nodeY(node);
        boolean reached = reached(node);
        boolean next = next(node);
        graphics.fill(x, y, x + BOX, y + BOX, reached ? NODE : NODE_AHEAD);
        int border = reached ? CYAN : next ? pulse(NEXT) : 0xFF3A4047;
        graphics.renderOutline(x, y, BOX, BOX, highlight ? 0xFFFFFFFF : border);
        graphics.renderItem(new ItemStack(node.item()), x + 2, y + 2);
        if (!reached) {
            // Not reached yet: the icon shows, dimmed.
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.fill(x + 1, y + 1, x + BOX - 1, y + BOX - 1, next ? 0x60141820 : 0xA0141820);
            graphics.pose().popPose();
        }
        // The step's name beside it, small, cut to the column.
        graphics.pose().pushPose();
        graphics.pose().translate(x + BOX + 4, y + 6, 0);
        graphics.pose().scale(0.75F, 0.75F, 1);
        String label = font.plainSubstrByWidth(name(node).getString(), (int) ((COLUMN_W - BOX - 16) / 0.75F));
        graphics.drawString(font, label, 0, 0, reached ? TEXT : next ? NEXT : DIM, false);
        graphics.pose().popPose();
    }

    private static int pulse(int colour) {
        float t = (float) (Math.sin(System.currentTimeMillis() / 250.0) * 0.5 + 0.5);
        int alpha = (int) (0x70 + 0x8F * t);
        return alpha << 24 | (colour & 0xFFFFFF);
    }

    private @Nullable Node nodeAt(double mouseX, double mouseY) {
        if (mouseY < TOP || (selected != null && mouseX >= width - PANEL_W)) {
            return null;
        }
        for (Node node : nodes) {
            int x = nodeX(node);
            int y = nodeY(node);
            if (mouseX >= x && mouseX < x + BOX && mouseY >= y && mouseY < y + BOX) {
                return node;
            }
        }
        return null;
    }

    /** Lang lines {@code base.1}, {@code base.2} and on, as many as exist. */
    private static List<Component> lines(String base) {
        List<Component> out = new ArrayList<>();
        for (int i = 1; I18n.exists(base + "." + i); i++) {
            out.add(Component.translatable(base + "." + i));
        }
        return out;
    }

    private int spoilerButtonY;

    private void renderPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        Node node = selected;
        int left = width - PANEL_W;
        graphics.fill(left, TOP, width, height, HEADER);
        graphics.fill(left, TOP, left + 1, height, LINE);
        int textW = PANEL_W - 16;
        graphics.enableScissor(left + 1, TOP, width, height);
        int y = TOP + 8 - Math.round(panelScroll);
        int top = y;
        graphics.renderItem(new ItemStack(node.item()), left + 8, y);
        graphics.drawString(font, name(node), left + 28, y + 1, TEXT, false);
        graphics.drawString(font, Component.translatable("progression.ryzergen.column." + ProgressionMap.COLUMNS[node.column()]),
                left + 28, y + 11, CYAN, false);
        y += 24;
        if (!node.inputs().isEmpty()) {
            List<String> needs = new ArrayList<>();
            for (String input : node.inputs()) {
                Node from = byId.get(input);
                if (from != null) {
                    needs.add(name(from).getString());
                }
            }
            y = paragraph(graphics, Component.translatable("progression.ryzergen.needs", String.join(", ", needs)), left + 8, y, textW, DIM);
            y += 4;
        }
        for (Component line : lines("progression.ryzergen." + node.id() + ".info")) {
            y = paragraph(graphics, line, left + 8, y, textW, TEXT);
            y += 3;
        }
        List<Component> spoilerLines = lines("progression.ryzergen." + node.id() + ".spoiler");
        if (!spoilerLines.isEmpty() || !node.grids().isEmpty()) {
            y += 4;
            spoilerButtonY = y;
            Component button = Component.translatable(spoilers ? "progression.ryzergen.spoilers.hide" : "progression.ryzergen.spoilers.show");
            boolean over = mouseX >= left + 8 && mouseX < width - 8 && mouseY >= y && mouseY < y + 14;
            graphics.fill(left + 8, y, width - 8, y + 14, over ? 0xFF4D545D : 0xFF3A4047);
            graphics.renderOutline(left + 8, y, PANEL_W - 16, 14, spoilers ? NEXT : 0xFF535A64);
            graphics.drawString(font, button, left + 8 + (PANEL_W - 16 - font.width(button)) / 2, y + 3, spoilers ? NEXT : TEXT, false);
            y += 20;
            if (spoilers) {
                for (Component line : spoilerLines) {
                    y = paragraph(graphics, line, left + 8, y, textW, NEXT);
                    y += 3;
                }
                for (Grid grid : node.grids()) {
                    y = renderGrid(graphics, grid, left + 8, y + 4, textW);
                }
            }
        } else {
            spoilerButtonY = -1000;
        }
        panelHeight = y - top + 16;
        graphics.disableScissor();
    }

    private int paragraph(GuiGraphics graphics, Component text, int x, int y, int w, int colour) {
        for (FormattedCharSequence line : font.split(text, w)) {
            graphics.drawString(font, line, x, y, colour, false);
            y += 10;
        }
        return y;
    }

    /** A best layout: its name, the cells in their colours (hex rows offset half a cell), and a key. */
    private int renderGrid(GuiGraphics graphics, Grid grid, int x, int y, int w) {
        y = paragraph(graphics, Component.translatable("progression.ryzergen.grid." + grid.title()), x, y, w, CYAN);
        int cell = 11;
        int widest = 0;
        for (String row : grid.rows()) {
            widest = Math.max(widest, row.length());
        }
        StringBuilder used = new StringBuilder();
        for (String row : grid.rows()) {
            int offset = grid.hex() ? (widest - row.length()) * cell / 2 : 0;
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                int cx = x + offset + i * cell;
                graphics.fill(cx, y, cx + cell - 1, y + cell - 1, ProgressionMap.CELL_COLOURS.getOrDefault(c, 0xFF3A4047));
                if (used.indexOf(String.valueOf(c)) < 0) {
                    used.append(c);
                }
            }
            y += cell;
        }
        y += 3;
        // The key: a swatch and a name for each kind of cell in this layout.
        int kx = x;
        for (char c : used.toString().toCharArray()) {
            Component label = Component.translatable("progression.ryzergen.cell." + (Character.isUpperCase(c) ? c : "lower_" + c));
            int need = 9 + font.width(label) + 8;
            if (kx + need > x + w) {
                kx = x;
                y += 10;
            }
            graphics.fill(kx, y + 1, kx + 7, y + 8, ProgressionMap.CELL_COLOURS.getOrDefault(c, 0xFF3A4047));
            graphics.drawString(font, label, kx + 9, y, DIM, false);
            kx += need;
        }
        return y + 16;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (selected != null && mouseX >= width - PANEL_W) {
                if (mouseY >= spoilerButtonY && mouseY < spoilerButtonY + 14 && mouseX >= width - PANEL_W + 8 && mouseX < width - 8) {
                    spoilers = !spoilers;
                    click();
                }
                return true;
            }
            Node node = nodeAt(mouseX, mouseY);
            if (node != null) {
                if (node != selected) {
                    selected = node;
                    spoilers = false;
                    panelScroll = 0;
                } else {
                    selected = null;
                }
                click();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && (selected == null || mouseX < width - PANEL_W)) {
            panX += (float) dragX;
            panY += (float) dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (selected != null && mouseX >= width - PANEL_W) {
            panelScroll = Math.max(0, Math.min(Math.max(0, panelHeight - (height - TOP)), panelScroll - (float) scrollY * 12));
            return true;
        }
        // The wheel moves along the line, left to right; with shift held, up and down.
        if (hasShiftDown()) {
            panY += (float) scrollY * 20;
        } else {
            panX += (float) (scrollY + scrollX) * 30;
        }
        return true;
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
