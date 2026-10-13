package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.text.RichText;
import com.ryzer.ryzergen.machine.SoundSwitch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

/**
 * The sound switch on a machine's screen (SoundSwitch): a tab on the panel's right edge, overlapping
 * it by 3 pixels so the two read as one piece, holding a speaker key: cyan waves while it sounds, a
 * red cross while it is muted. Its sprites (art/tools/gui_textures.py, sound_tab): the tab at 0,0,
 * then the keys from 0,32 (sound, sound hovered, muted, muted hovered).
 */
public final class SoundTab {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/sound_tab.png");
    private static final int SHEET = 64;
    public static final int W = 22;
    public static final int H = 26;
    private static final int OVERLAP = 3;
    private static final int KEY_V = 32;
    /** Where the tab sits on a panel with nothing else on its right edge. */
    public static final int TOP = 16;
    private static final int DIM = 0xFF9EA7B1;

    private SoundTab() {}

    /** A screen with a sound tab, so recipe viewers keep their lists off it. */
    public interface Host {
        /** The tab's top, in the panel's own coordinates. */
        int soundTabY();
    }

    /** The tab's area on screen, for a recipe viewer to keep clear. */
    public static Rect2i area(AbstractContainerScreen<?> screen, int y) {
        return new Rect2i(screen.getGuiLeft() + screen.getXSize() - OVERLAP, screen.getGuiTop() + y, W, H);
    }

    public static void render(GuiGraphics graphics, int left, int top, int width, int y, boolean muted, double mouseX, double mouseY) {
        int x = left + width - OVERLAP;
        graphics.blit(TEXTURE, x, top + y, 0, 0, W, H, SHEET, SHEET);
        int key = (muted ? 2 : 0) + (overKey(left, top, width, y, mouseX, mouseY) ? 1 : 0);
        graphics.blit(TEXTURE, x + 3, top + y + 5, key * 16, KEY_V, 16, 16, SHEET, SHEET);
    }

    public static boolean overKey(int left, int top, int width, int y, double mouseX, double mouseY) {
        int x = left + width - OVERLAP + 3;
        int ky = top + y + 5;
        return mouseX >= x && mouseX < x + 16 && mouseY >= ky && mouseY < ky + 16;
    }

    /** The key's tooltip, if the mouse is over it; true if drawn. */
    public static boolean tooltip(GuiGraphics graphics, Font font, int left, int top, int width, int y, boolean muted, int mouseX, int mouseY) {
        if (!overKey(left, top, width, y, mouseX, mouseY)) {
            return false;
        }
        graphics.renderComponentTooltip(font, List.of(
                RichText.tr(muted ? "gui.ryzergen.sound.off" : "gui.ryzergen.sound.on"),
                RichText.tr("gui.ryzergen.sound.hint").withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
        return true;
    }

    /** A click on the key: presses the menu's sound button, with the keys' click. True if it was the key. */
    public static boolean click(AbstractContainerMenu menu, int left, int top, int width, int y, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!overKey(left, top, width, y, mouseX, mouseY) || minecraft.gameMode == null) {
            return false;
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SoundSwitch.BUTTON);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }
}
