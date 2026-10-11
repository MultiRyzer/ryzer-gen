package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.client.text.RichText;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The steady-state streak as a reactor screen shows it, over its live power readout: the streak,
 * the bonus it gives now, and the next milestone (design section 5).
 */
public final class SteadyStateTooltip {
    private static final int HINT = 0xFF9AA3AE;

    private SteadyStateTooltip() {}

    /** From the synced values: the streak and next milestone in tenths of a load, the bonus in thousandths. */
    public static List<Component> lines(int streakTenths, int bonusPermille, int nextTenths) {
        return List.of(
                RichText.tr("gui.ryzergen.steady.streak", String.format("%.1f", streakTenths / 10F)),
                RichText.tr("gui.ryzergen.steady.bonus", String.format("%.2f", bonusPermille / 1000F)),
                nextTenths > 0
                        ? RichText.tr("gui.ryzergen.steady.next", Math.round(nextTenths / 10F))
                        : RichText.tr("gui.ryzergen.steady.max"),
                RichText.tr("gui.ryzergen.steady.hint").withStyle(style -> style.withColor(HINT)));
    }
}
