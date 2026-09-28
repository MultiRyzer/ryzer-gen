package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.cable.CableKind;
import com.ryzer.ryzergen.scanner.FlowScanClientState;
import com.ryzer.ryzergen.scanner.FlowScanPayload;
import com.ryzer.ryzergen.scanner.FlowScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;

/**
 * The Flow Scanner's labels: while the player holds one, each pipe and cable input nearby shows
 * what comes in on it, floating at that face like a name tag. The colour says how close the input
 * is to its limit, not how big the number is: 50,000 FE/t on a 100,000 limit is yellow-orange,
 * 100 mB/t on a 100 limit is deep red. Faint through walls, so pipes behind machines can be found.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class FlowScannerLabels {
    private static final float SCALE = 0.02F;
    /** How full, to colour: green idle, yellow half, orange three quarters, red nearly full, dark red at the limit. */
    private static final float[] STOPS = {0, 0.5F, 0.75F, 0.9F, 1};
    private static final int[] COLOURS = {0x44D65E, 0xF0D030, 0xF08A1E, 0xE0503C, 0xA81818};
    private static final int IDLE = 0x9AA3AE;

    private FlowScannerLabels() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !FlowScanner.holding(player)) {
            return;
        }
        List<FlowScanPayload.Entry> entries = FlowScanClientState.entries();
        if (entries.isEmpty()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Font font = minecraft.font;
        CableKind[] kinds = CableKind.values();
        for (FlowScanPayload.Entry entry : entries) {
            BlockPos pos = BlockPos.of(entry.pos());
            Direction side = Direction.from3DDataValue(entry.side());
            // At the input face, a little above it so it clears the pipe.
            Vec3 at = Vec3.atCenterOf(pos).add(side.getStepX() * 0.36, 0.34 + side.getStepY() * 0.2, side.getStepZ() * 0.36);
            if (at.distanceToSqr(camera) > FlowScanner.RADIUS * FlowScanner.RADIUS) {
                continue;
            }
            CableKind kind = entry.kind() >= 0 && entry.kind() < kinds.length ? kinds[entry.kind()] : CableKind.ENERGY;
            String text = format(entry.rate()) + " " + unit(kind);
            int colour = entry.rate() < 0.5F ? IDLE : colour(entry.limit() > 0 ? entry.rate() / entry.limit() : 1);

            pose.pushPose();
            pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
            pose.mulPose(event.getCamera().rotation());
            pose.scale(SCALE, -SCALE, SCALE);
            Matrix4f matrix = pose.last().pose();
            float x = -font.width(text) / 2F;
            // Like a name tag: faint through walls, then solid where it can be seen.
            font.drawInBatch(text, x, 0, 0x40000000 | colour, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH,
                    0x50000000, LightTexture.FULL_BRIGHT);
            font.drawInBatch(text, x, 0, 0xFF000000 | colour, false, matrix, buffers, Font.DisplayMode.NORMAL,
                    0, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        buffers.endBatch();
    }

    /** Interpolates the colour stops at {@code fill} (0 idle to 1 at the limit). */
    static int colour(float fill) {
        fill = Math.max(0, Math.min(1, fill));
        for (int i = 1; i < STOPS.length; i++) {
            if (fill <= STOPS[i]) {
                float t = (fill - STOPS[i - 1]) / (STOPS[i] - STOPS[i - 1]);
                return mix(COLOURS[i - 1], COLOURS[i], t);
            }
        }
        return COLOURS[COLOURS.length - 1];
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return r << 16 | g << 8 | bl;
    }

    /** Short numbers: 850, 12.5k, 1.2M. */
    static String format(float value) {
        if (value >= 999_500) {
            return trim(value / 1_000_000F) + "M";
        }
        if (value >= 9_995) {
            return trim(value / 1_000F) + "k";
        }
        return value >= 10 ? Integer.toString(Math.round(value)) : trim(value);
    }

    private static String trim(float value) {
        String text = String.format(java.util.Locale.ROOT, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    private static String unit(CableKind kind) {
        return switch (kind) {
            case ENERGY -> "FE/t";
            case ITEMS -> "items/s";
            case FLUID, GAS -> "mB/t";
        };
    }
}
