package com.ryzer.ryzergen.client.terminal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * Text in the terminal font (ryzergen:terminal, published by art/tools/terminal_font.py): a 5 x 7
 * monospace in capitals, 6 units a character, so columns of numbers line up. Drawn at any whole or
 * half scale; the control screens draw small text at half a GUI pixel per font pixel.
 */
public final class TerminalText {
    public static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "terminal");
    private static final Style STYLE = Style.EMPTY.withFont(FONT);
    public static final int ADVANCE = 6;
    public static final int HEIGHT = 7;

    private TerminalText() {}

    /** Width in font pixels: 6 a character, less the last one's spacing. */
    public static int width(String text) {
        return text.isEmpty() ? 0 : text.length() * ADVANCE - 1;
    }

    /** Draws {@code text} with its top left at (x, y), {@code scale} units a font pixel; returns the x after it. */
    public static float draw(GuiGraphics graphics, String text, float x, float y, int colour, float scale) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1);
        graphics.drawString(Minecraft.getInstance().font, Component.literal(text.toUpperCase(Locale.ROOT)).withStyle(STYLE), 0, 0,
                colour, false);
        pose.popPose();
        return x + width(text) * scale;
    }

    public static float right(GuiGraphics graphics, String text, float x1, float y, int colour, float scale) {
        return draw(graphics, text, x1 - width(text) * scale, y, colour, scale);
    }

    public static float centre(GuiGraphics graphics, String text, float cx, float y, int colour, float scale) {
        return draw(graphics, text, cx - width(text) * scale / 2, y, colour, scale);
    }
}
