package com.ryzer.ryzergen.client.handbook;

import com.ryzer.ryzergen.client.progression.ProgressionKeys;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * "Press Y for the map", in the style of vanilla's tutorial toasts (the ones that teach WASD), with
 * the handbook as its icon and a bar running down while it shows. The key is whatever the map is
 * bound to.
 */
public class HandbookToast implements Toast {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/tutorial");
    private static final long SHOWN = 12_000;
    private boolean hidden;

    public void hide() {
        hidden = true;
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toasts, long time) {
        graphics.blitSprite(BACKGROUND, 0, 0, width(), height());
        graphics.renderFakeItem(new ItemStack(ModItems.OPERATORS_HANDBOOK.get()), 6, 8);
        var font = toasts.getMinecraft().font;
        graphics.drawString(font, Component.translatable("handbook.ryzergen.toast.title"), 30, 7, 0xFF500050, false);
        Component key = ProgressionKeys.OPEN_MAP.getTranslatedKeyMessage().copy().withStyle(style -> style.withBold(true));
        graphics.drawString(font, Component.translatable("handbook.ryzergen.toast.line", key), 30, 18, 0xFF000000, false);
        float left = Math.max(0, 1 - (float) time / SHOWN);
        graphics.fill(3, 28, 157, 29, 0xFFFFFFFF);
        graphics.fill(3, 28, 3 + (int) (154 * left), 29, 0xFF500050);
        return hidden || time >= SHOWN ? Visibility.HIDE : Visibility.SHOW;
    }
}
