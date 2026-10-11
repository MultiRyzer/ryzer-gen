package com.ryzer.ryzergen.client.progression;

import com.ryzer.ryzergen.client.progression.ProgressionMap.Grid;
import com.ryzer.ryzergen.client.progression.ProgressionMap.Node;
import com.ryzer.ryzergen.client.text.RichText;
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
    private static final int COLUMN_W = 132;
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

    /** A milestone's card: the column's width, and two rows tall. */
    private static final int CARD_W = COLUMN_W - 16;
    private static final int CARD_H = ROW_H + BOX;
    /** The square its icon sits in, at the card's left. */
    private static final int CARD_ICON = 30;

    private static int nodeW(Node node) {
        return node.milestone() ? CARD_W : BOX;
    }

    private static int nodeH(Node node) {
        return node.milestone() ? CARD_H : BOX;
    }

    /** Where arrows leave a step: under its icon's middle. */
    private int iconCentreX(Node node) {
        return nodeX(node) + (node.milestone() ? CARD_ICON / 2 + 4 : BOX / 2);
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
     * An arrow from each input to the step it feeds, routed like a metro map so it never crosses a
     * step or its name: down out of the input into the gap below its row, along that gap, then in
     * the gap just before the step's column up or down to the step, and into it from the left.
     * Lit cyan where the input is reached; the hovered or selected step's inputs stand out.
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
                    int sx = iconCentreX(from);
                    int sy = nodeY(from) + nodeH(from);
                    int lane = sy + (ROW_H - BOX) / 2;
                    int tx = nodeX(node);
                    int ty = nodeY(node) + nodeH(node) / 2;
                    int gap = tx - 5;
                    graphics.fill(sx, sy, sx + 1, lane + 1, colour);
                    graphics.fill(Math.min(sx, gap), lane, Math.max(sx, gap) + 1, lane + 1, colour);
                    graphics.fill(gap, Math.min(lane, ty), gap + 1, Math.max(lane, ty) + 1, colour);
                    graphics.fill(gap, ty, tx, ty + 1, colour);
                    graphics.fill(tx - 3, ty - 1, tx - 1, ty + 2, colour);
                }
            }
        }
    }

    private void renderNode(GuiGraphics graphics, Node node, boolean highlight) {
        if (node.milestone()) {
            renderMilestone(graphics, node, highlight);
            return;
        }
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
        // The step's name beside it, small, cut to the column, on a backing so no line shows through.
        String label = font.plainSubstrByWidth(name(node).getString(), (int) ((COLUMN_W - BOX - 16) / 0.75F));
        int labelW = Math.round(font.width(label) * 0.75F);
        graphics.fill(x + BOX + 2, y + 4, x + BOX + 6 + labelW, y + BOX - 4, 0xFF141820);
        graphics.pose().pushPose();
        graphics.pose().translate(x + BOX + 4, y + 7, 0);
        graphics.pose().scale(0.75F, 0.75F, 1);
        graphics.drawString(font, label, 0, 0, reached ? TEXT : next ? NEXT : DIM, false);
        graphics.pose().popPose();
    }

    /**
     * A tier's milestone, as a card: a soft halo (cyan once reached, pulsing amber when it is next), a
     * bar of the same colour along its top, its icon large in a well on the left, and beside it its
     * name at full size, "Milestone" and the tier, and its headline stat in the mod's markup.
     */
    private void renderMilestone(GuiGraphics graphics, Node node, boolean highlight) {
        int x = nodeX(node);
        int y = nodeY(node);
        boolean reached = reached(node);
        boolean next = next(node);
        int accent = reached ? CYAN : next ? pulse(NEXT) : 0xFF3A4047;
        if (reached || next) {
            int halo = (accent & 0x00FFFFFF) | (reached ? 0x30000000 : (accent >>> 24) / 3 << 24);
            graphics.fill(x - 2, y - 2, x + CARD_W + 2, y + CARD_H + 2, halo);
        }
        graphics.fill(x, y, x + CARD_W, y + CARD_H, reached ? 0xFF22303A : next ? 0xFF2A2620 : NODE_AHEAD);
        graphics.fill(x, y, x + CARD_W, y + 2, accent);
        graphics.renderOutline(x, y, CARD_W, CARD_H, highlight ? 0xFFFFFFFF : accent);
        // The icon, half as big again, in its well.
        int ix = x + 4;
        int iy = y + (CARD_H - CARD_ICON) / 2 + 1;
        graphics.fill(ix, iy, ix + CARD_ICON, iy + CARD_ICON, 0xFF141820);
        graphics.pose().pushPose();
        graphics.pose().translate(ix + 3, iy + 3, 0);
        graphics.pose().scale(1.5F, 1.5F, 1);
        graphics.renderItem(new ItemStack(node.item()), 0, 0);
        graphics.pose().popPose();
        if (!reached) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.fill(ix, iy, ix + CARD_ICON, iy + CARD_ICON, next ? 0x60141820 : 0xA0141820);
            graphics.pose().popPose();
        }
        // Name, caption and headline beside it.
        int tx = ix + CARD_ICON + 5;
        int room = x + CARD_W - 4 - tx;
        String label = font.plainSubstrByWidth(name(node).getString(), room);
        graphics.drawString(font, label, tx, y + 7, reached ? 0xFFFFFFFF : next ? NEXT : DIM, false);
        small(graphics, RichText.tr("progression.ryzergen.milestone"), tx, y + 18, reached ? CYAN : DIM, room);
        String headline = "progression.ryzergen." + node.id() + ".headline";
        if (RichText.has(headline)) {
            small(graphics, RichText.tr(headline), tx, y + 28, reached ? TEXT : DIM, room);
        }
    }

    /** A line at three quarters size, cut to {@code room} pixels. */
    private void small(GuiGraphics graphics, Component text, int x, int y, int colour, int room) {
        List<FormattedCharSequence> lines = font.split(text, (int) (room / 0.75F));
        if (lines.isEmpty()) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(0.75F, 0.75F, 1);
        graphics.drawString(font, lines.get(0), 0, 0, colour, false);
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
            if (mouseX >= x && mouseX < x + nodeW(node) && mouseY >= y && mouseY < y + nodeH(node)) {
                return node;
            }
        }
        return null;
    }

    /** Lang lines {@code base.1}, {@code base.2} and on, as many as exist. */
    private static List<Component> lines(String base) {
        List<Component> out = new ArrayList<>();
        for (int i = 1; I18n.exists(base + "." + i); i++) {
            out.add(RichText.tr(base + "." + i));
        }
        return out;
    }

    private int spoilerButtonY;

    private void renderPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        Node node = selected;
        int left = width - PANEL_W;
        graphics.fill(left, TOP, width, height, HEADER);
        graphics.fill(left, TOP, left + 1, height, LINE);
        int textW = PANEL_W - 30;
        graphics.enableScissor(left + 1, TOP, width, height);
        int y = TOP + 8 - Math.round(panelScroll);
        int top = y;
        graphics.renderItem(new ItemStack(node.item()), left + 8, y);
        graphics.drawString(font, name(node), left + 28, y + 1, TEXT, false);
        graphics.drawString(font, Component.translatable("progression.ryzergen.column." + ProgressionMap.COLUMNS[node.column()]),
                left + 28, y + 11, CYAN, false);
        y += 24;
        // What it takes, and what it is for: the steps that feed it, and the steps it feeds.
        List<String> needs = new ArrayList<>();
        for (String input : node.inputs()) {
            Node from = byId.get(input);
            if (from != null) {
                needs.add("<b>" + name(from).getString() + "</b>");
            }
        }
        if (!needs.isEmpty()) {
            y = paragraph(graphics, RichText.tr("progression.ryzergen.needs", String.join(", ", needs)), left + 8, y, textW, DIM);
        }
        List<String> leads = new ArrayList<>();
        for (Node to : nodes) {
            if (to.inputs().contains(node.id())) {
                leads.add("<b>" + name(to).getString() + "</b>");
            }
        }
        if (!leads.isEmpty()) {
            y = paragraph(graphics, RichText.tr("progression.ryzergen.leads", String.join(", ", leads)), left + 8, y, textW, DIM);
        }
        if (!needs.isEmpty() || !leads.isEmpty()) {
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
                    y = paragraph(graphics, line, left + 8, y, textW, TEXT);
                    y += 3;
                }
                if (!node.grids().isEmpty()) {
                    y = renderGridKey(graphics, node.grids(), left + 8, y + 4, textW);
                }
                for (Grid grid : node.grids()) {
                    y = renderGrid(graphics, grid, left + 8, y, textW);
                }
            }
        } else {
            spoilerButtonY = -1000;
        }
        panelHeight = y - top + 16;
        graphics.disableScissor();
        // The close button, top right, fixed while the panel scrolls.
        boolean overClose = overClose(mouseX, mouseY);
        graphics.fill(width - CLOSE - 6, TOP + 4, width - 6, TOP + 4 + CLOSE, overClose ? 0xFF7A3030 : 0xFF3A4047);
        graphics.renderOutline(width - CLOSE - 6, TOP + 4, CLOSE, CLOSE, overClose ? 0xFFE0503C : 0xFF535A64);
        graphics.drawString(font, "x", width - CLOSE - 6 + (CLOSE - font.width("x")) / 2 + 1, TOP + 6, TEXT, false);
    }

    private static final int CLOSE = 12;

    private boolean overClose(double mouseX, double mouseY) {
        return selected != null && mouseX >= width - CLOSE - 6 && mouseX < width - 6 && mouseY >= TOP + 4 && mouseY < TOP + 4 + CLOSE;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Esc closes a step's panel first, then the map; the map's own key closes it too.
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && selected != null) {
            selected = null;
            return true;
        }
        if (ProgressionKeys.OPEN_MAP.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int paragraph(GuiGraphics graphics, Component text, int x, int y, int w, int colour) {
        for (FormattedCharSequence line : font.split(text, w)) {
            graphics.drawString(font, line, x, y, colour, false);
            y += 10;
        }
        return y;
    }

    /**
     * The best layouts' key, once above them: the kinds of cell they use, in a fixed order, each as
     * its term (icon and word in the cell's colour), then where the front row is.
     */
    private int renderGridKey(GuiGraphics graphics, List<Grid> grids, int x, int y, int w) {
        StringBuilder used = new StringBuilder();
        for (char c : ProgressionMap.CELL_ORDER.toCharArray()) {
            for (Grid grid : grids) {
                if (String.join("", grid.rows()).indexOf(c) >= 0 && used.indexOf(String.valueOf(c)) < 0) {
                    used.append(c);
                }
            }
        }
        int kx = x;
        for (char c : used.toString().toCharArray()) {
            Component label = RichText.parse("{" + ProgressionMap.CELL_WORDS.get(c) + "}");
            int need = font.width(label) + 8;
            if (kx + need > x + w) {
                kx = x;
                y += 10;
            }
            graphics.drawString(font, label, kx, y, TEXT, false);
            kx += need;
        }
        y += 11;
        if (!grids.get(0).hex()) {
            y = paragraph(graphics, RichText.tr("progression.ryzergen.grid.note"), x, y, w, DIM);
        }
        return y + 4;
    }

    /**
     * A best layout as a stat box: the cells on the left (hex rows offset half a cell), and beside
     * them its heading and the numbers that tell it from the others.
     */
    private int renderGrid(GuiGraphics graphics, Grid grid, int x, int y, int w) {
        int cell = 11;
        int widest = 0;
        for (String row : grid.rows()) {
            widest = Math.max(widest, row.length());
        }
        int top = y;
        for (String row : grid.rows()) {
            int offset = grid.hex() ? (widest - row.length()) * cell / 2 : 0;
            for (int i = 0; i < row.length(); i++) {
                int cx = x + offset + i * cell;
                graphics.fill(cx, y, cx + cell - 1, y + cell - 1, ProgressionMap.CELL_COLOURS.getOrDefault(row.charAt(i), 0xFF3A4047));
            }
            y += cell;
        }
        int tx = x + widest * cell + 7;
        int tw = x + w - tx;
        String key = "progression.ryzergen.grid." + grid.title();
        int ty = paragraph(graphics, RichText.tr(key), tx, top, tw, TEXT) + 2;
        for (int n = 1; RichText.has(key + ".stat." + n); n++) {
            ty = paragraph(graphics, RichText.tr(key + ".stat." + n), tx, ty, tw, DIM);
        }
        return Math.max(y, ty) + 8;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (overClose(mouseX, mouseY)) {
                selected = null;
                click();
                return true;
            }
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
