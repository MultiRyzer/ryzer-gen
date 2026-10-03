package com.ryzer.ryzergen.machine.breeder;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import com.ryzer.ryzergen.machine.breeder.BreederReactor.Position;
import com.ryzer.ryzergen.machine.fission.TargetRodItem;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The breeder's control screen, laid out as the station's. Left, the core's 19 positions in hex
 * rows: pick a plan tool (fuel, control rod, uranium blanket, lithium target) and click positions to
 * plan them, then put assemblies in. Right-click a position to take its item out, and again to
 * clear its plan. Right, the readout: live output above the line, the plan's forecast below it
 * (power at this flow, settled temperature and efficiency, fuel life, breeding). Then the heat,
 * sodium and energy bars, the pumps' flow, and the power and redstone keys. Positions match
 * art/tools/gui_textures.py (breeder_control).
 */
public class BreederControlScreen extends AbstractContainerScreen<BreederControlMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/breeder_control.png");
    private static final int LABEL = 0xFF2B3036;
    private static final int VALUE = 0xFF35C8F5;
    private static final int DIM = 0xFF9AA3AE;
    private static final int GOOD = 0xFF44D65E;
    private static final int WARN = 0xFFF0A030;
    private static final int BAD = 0xFFE0503C;
    private static final int TEXT_X = 104;
    private static final int BAR_X = 104;
    private static final int BAR_W = 104;
    private static final int HEAT_Y = 84;
    private static final int SODIUM_Y = 92;
    private static final int ENERGY_Y = 100;
    private static final int TOOL_Y = 110;
    private static final int KEYS_Y = 108;
    private static final int FLOW_DOWN_X = 104;
    private static final int FLOW_TEXT_X = 121;
    private static final int FLOW_TEXT_W = 30;
    private static final int FLOW_UP_X = 152;
    private static final int POWER_X = 172;
    private static final int REDSTONE_X = 190;
    /** Sprites, below the panel in the texture. */
    private static final int KEY_U = 0;
    private static final int KEY_HOVER_U = 16;
    private static final int POWER_U = 32;
    private static final int SPRITE_V = 224;
    private static final Position[] TOOLS = {Position.FUEL, Position.CONTROL, Position.URANIUM, Position.LITHIUM};

    /** The plan tool picked, or null to handle items normally. */
    private @Nullable Position tool;

    public BreederControlScreen(BreederControlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // The machine bay starts at y 13, so the title sits higher than the default.
        titleLabelY = 4;
        imageWidth = BreederControlMenu.WIDTH;
        imageHeight = 222;
        inventoryLabelX = BreederControlMenu.INVENTORY_X;
        inventoryLabelY = BreederControlMenu.INVENTORY_Y - 11;
    }

    private static int toolX(int index) {
        return 8 + index * 18;
    }

    private static ItemStack toolIcon(Position position) {
        return new ItemStack(switch (position) {
            case FUEL, EMPTY -> ModItems.BREEDER_FUEL.get();
            case CONTROL -> ModItems.CONTROL_ROD.get();
            case URANIUM -> ModItems.URANIUM_BLANKET.get();
            case LITHIUM -> ModItems.LITHIUM_TARGET_ROD.get();
        });
    }

    /** Each plan's colour, used for its positions' frames and under its tool. */
    private static int colour(Position position) {
        return switch (position) {
            case FUEL -> 0xFF35C8F5;
            case CONTROL -> 0xFFE0503C;
            case URANIUM -> 0xFF44D65E;
            case LITHIUM -> 0xFFB27CF0;
            case EMPTY -> 0;
        };
    }

    /** A soft tint of each plan's colour, so the layout reads at a glance with items in place. */
    private static int fill(Position position) {
        return colour(position) == 0 ? 0 : 0x40000000 | (colour(position) & 0xFFFFFF);
    }

    private BreederReactor.Analysis analysis() {
        return BreederReactor.analyse(menu.plan(), menu.items());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        Position[] plan = menu.plan();
        for (int i = 0; i < BreederReactor.POSITIONS; i++) {
            int x = leftPos + BreederControlMenu.slotX(i);
            int y = topPos + BreederControlMenu.slotY(i);
            graphics.fill(x, y, x + 16, y + 16, fill(plan[i]));
        }
        for (int k = 0; k < TOOLS.length; k++) {
            Position position = TOOLS[k];
            int x = leftPos + toolX(k);
            int y = topPos + TOOL_Y;
            graphics.blit(TEXTURE, x, y, isOver(toolX(k), TOOL_Y, mouseX, mouseY) ? KEY_HOVER_U : KEY_U, SPRITE_V, 16, 16);
            graphics.fill(x + 3, y + 12, x + 13, y + 13, colour(position));
            if (tool == position) {
                graphics.renderOutline(x - 1, y - 1, 18, 18, colour(position));
            }
            smallItem(graphics, toolIcon(position), x + 2.5F, y + 0.5F);
        }
        for (int x : new int[] {FLOW_DOWN_X, FLOW_UP_X}) {
            graphics.blit(TEXTURE, leftPos + x, topPos + KEYS_Y, isOver(x, KEYS_Y, mouseX, mouseY) ? KEY_HOVER_U : KEY_U, SPRITE_V, 16, 16);
        }
        int powerKey = (menu.enabled() ? 0 : 2) + (isOver(POWER_X, KEYS_Y, mouseX, mouseY) ? 1 : 0);
        graphics.blit(TEXTURE, leftPos + POWER_X, topPos + KEYS_Y, POWER_U + powerKey * 16, SPRITE_V, 16, 16);
        graphics.blit(TEXTURE, leftPos + REDSTONE_X, topPos + KEYS_Y, isOver(REDSTONE_X, KEYS_Y, mouseX, mouseY) ? KEY_HOVER_U : KEY_U, SPRITE_V, 16, 16);
        smallItem(graphics, new ItemStack(switch (menu.redstoneMode()) {
            case IGNORED -> Items.GUNPOWDER;
            case HIGH -> Items.REDSTONE;
            case LOW -> Items.REDSTONE_TORCH;
        }), leftPos + REDSTONE_X + 2.5F, topPos + KEYS_Y + 1.5F);

        bar(graphics, HEAT_Y, menu.temperature() / BreederRunner.SCRAM_TEMPERATURE,
                menu.temperature() >= BreederRunner.HOT_TEMPERATURE ? BAD : WARN);
        bar(graphics, SODIUM_Y, menu.sodium() / (float) BreederReactor.LOOP, 0xFFC8C4BA);
        bar(graphics, ENERGY_Y, menu.energy() / (float) BreederRunner.ENERGY_CAPACITY, GuiGauges.ENERGY);
    }

    /**
     * Over the items: a frame in the plan's colour where a position still waits for its item, a
     * flashing red one on an assembly over the hot spot limit, and the positions beside the one under
     * the mouse outlined, as those are the ones it affects.
     */
    private void renderPositions(GuiGraphics graphics) {
        Position[] plan = menu.plan();
        ItemStack[] items = menu.items();
        BreederReactor.Analysis analysis = analysis();
        boolean flash = (System.currentTimeMillis() / 400) % 2 == 0;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 250);
        for (int i = 0; i < BreederReactor.POSITIONS; i++) {
            if (analysis.hotSpot(i)) {
                if (flash) {
                    graphics.renderOutline(BreederControlMenu.slotX(i), BreederControlMenu.slotY(i), 16, 16, BAD);
                }
            } else if (plan[i] != Position.EMPTY && items[i].isEmpty()) {
                graphics.renderOutline(BreederControlMenu.slotX(i), BreederControlMenu.slotY(i), 16, 16, colour(plan[i]));
            }
        }
        Slot hovered = getSlotUnderMouse();
        if (hovered != null && hovered.index < BreederReactor.POSITIONS) {
            for (int n : BreederReactor.neighbours(hovered.getSlotIndex())) {
                graphics.renderOutline(BreederControlMenu.slotX(n) - 1, BreederControlMenu.slotY(n) - 1, 18, 18, 0xFFE8ECF0);
            }
        }
        graphics.pose().popPose();
    }

    private void bar(GuiGraphics graphics, int y, float fraction, int colour) {
        GuiGauges.glowHorizontal(graphics, leftPos + BAR_X, topPos + y, BAR_W, 4, fraction, colour);
    }

    private void smallItem(GuiGraphics graphics, ItemStack stack, float x, float y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(0.7F, 0.7F, 1);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
        renderPositions(graphics);
        // The flow keys' signs, and the flow between them.
        graphics.drawString(font, "-", FLOW_DOWN_X + 6, KEYS_Y + 4, LABEL, false);
        graphics.drawString(font, "+", FLOW_UP_X + 5, KEYS_Y + 4, LABEL, false);
        String flow = menu.flow() + "%";
        graphics.drawString(font, flow, FLOW_TEXT_X + (FLOW_TEXT_W - font.width(flow)) / 2, KEYS_Y + 4, VALUE, false);
        // Now: status, then output and core temperature.
        BreederRunner.Status status = menu.status();
        int colour = switch (status) {
            case ONLINE -> GOOD;
            case WARMING, NO_FUEL, OFFLINE -> WARN;
            case NO_SODIUM, OVERHEAT, SCRAM -> BAD;
        };
        text(graphics, Component.translatable("gui.ryzergen.breeder.status." + status.name().toLowerCase(Locale.ROOT)), 20, colour);
        text(graphics, Component.translatable("gui.ryzergen.breeder.live", compact(menu.output()), menu.temperature()), 29,
                menu.temperature() >= BreederRunner.HOT_TEMPERATURE ? BAD : VALUE);
        // The plan: what this layout gives once settled, at this flow.
        BreederReactor.Analysis plan = analysis();
        if (plan.fuel() == 0) {
            text(graphics, Component.translatable("gui.ryzergen.breeder.plan_empty"), 42, DIM);
            return;
        }
        if (!plan.holdsSteady()) {
            int spots = 0;
            for (int i = 0; i < BreederReactor.POSITIONS; i++) {
                spots += plan.hotSpot(i) ? 1 : 0;
            }
            text(graphics, Component.translatable("gui.ryzergen.breeder.hot_spots", spots), 42, BAD);
            text(graphics, Component.translatable("gui.ryzergen.breeder.hot_spot_hint"), 51, DIM);
            return;
        }
        float flowFraction = menu.flow() / 100F;
        float capacity = BreederReactor.capacity(flowFraction, BreederReactor.LOOP);
        if (plan.generation() > capacity + 0.5F) {
            text(graphics, Component.translatable("gui.ryzergen.breeder.too_hot"), 42, BAD);
            text(graphics, Component.translatable("gui.ryzergen.breeder.more_flow",
                    (int) Math.ceil(plan.generation() / BreederReactor.capacity(1, BreederReactor.LOOP) * 100)), 51, DIM);
            return;
        }
        float output = BreederReactor.plannedOutput(plan.generation(), flowFraction, BreederReactor.LOOP);
        text(graphics, Component.translatable("gui.ryzergen.breeder.plan", compact(Math.round(output))), 42, VALUE);
        float temperature = BreederReactor.settledTemperature(plan.generation(), capacity);
        text(graphics, Component.translatable("gui.ryzergen.breeder.plan_temperature",
                Math.round(BreederReactor.efficiency(temperature) * 100), Math.round(temperature)), 51, VALUE);
        text(graphics, Component.translatable("gui.ryzergen.breeder.fuel_life", Math.round(plan.fuelMinutes())), 60, VALUE);
        text(graphics, Component.translatable("gui.ryzergen.breeder.blankets", plan.blankets()), 69, plan.blankets() > 0 ? VALUE : DIM);
    }

    private void text(GuiGraphics graphics, Component text, int y, int colour) {
        graphics.drawString(font, text, TEXT_X, y, colour, false);
    }

    /** 950, 12.5k, 1.20M; negative values keep their sign. */
    private static String compact(int value) {
        int abs = Math.abs(value);
        String sign = value < 0 ? "-" : "";
        if (abs < 10_000) {
            return String.format("%,d", value);
        }
        if (abs < 1_000_000) {
            return sign + String.format("%.1fk", abs / 1_000.0);
        }
        return sign + String.format("%.2fM", abs / 1_000_000.0);
    }

    private boolean isOver(int x, int y, double mouseX, double mouseY) {
        return isOver(x, y, 16, 16, mouseX, mouseY);
    }

    private boolean isOver(int x, int y, int w, int h, double mouseX, double mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    private static Component tip(String key, int colour, Object... args) {
        return Component.translatable(key, args).withStyle(style -> style.withColor(colour));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Slot hovered = getSlotUnderMouse();
        if (hovered != null && hovered.index < BreederReactor.POSITIONS && menu.getCarried().isEmpty()) {
            graphics.renderComponentTooltip(font, positionTooltip(hovered.getSlotIndex()), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
        for (int k = 0; k < TOOLS.length; k++) {
            if (isOver(toolX(k), TOOL_Y, mouseX, mouseY)) {
                graphics.renderComponentTooltip(font, List.of(
                        Component.translatable("gui.ryzergen.breeder.tool." + TOOLS[k].name().toLowerCase(Locale.ROOT)),
                        tip("gui.ryzergen.station.tool.hint", DIM),
                        tip("gui.ryzergen.station.tool.clear", DIM)), mouseX, mouseY);
            }
        }
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (isOver(POWER_X, KEYS_Y, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(menu.enabled() ? "gui.ryzergen.station.power.on" : "gui.ryzergen.station.power.off"), mouseX, mouseY);
        } else if (isOver(REDSTONE_X, KEYS_Y, mouseX, mouseY)) {
            String mode = switch (menu.redstoneMode()) {
                case IGNORED -> "gui.ryzergen.redstone.ignored";
                case HIGH -> "gui.ryzergen.redstone.high";
                case LOW -> "gui.ryzergen.redstone.low";
            };
            graphics.renderTooltip(font, Component.translatable(mode), mouseX, mouseY);
        } else if (isOver(FLOW_DOWN_X, KEYS_Y, FLOW_UP_X + 16 - FLOW_DOWN_X, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.ryzergen.breeder.flow", menu.flow()),
                    tip("gui.ryzergen.breeder.flow.pumps", DIM, compact(menu.pumping())),
                    tip("gui.ryzergen.breeder.flow.hint", DIM)), mouseX, mouseY);
        } else if (mx >= BAR_X && mx < BAR_X + BAR_W) {
            Component tip = my >= HEAT_Y - 1 && my < HEAT_Y + 5
                    ? Component.translatable("gui.ryzergen.station.heat", menu.temperature(), (int) BreederRunner.SCRAM_TEMPERATURE)
                    : my >= SODIUM_Y - 1 && my < SODIUM_Y + 5
                    ? Component.translatable("gui.ryzergen.breeder.sodium", String.format("%,d", menu.sodium()), String.format("%,d", BreederReactor.LOOP))
                    : my >= ENERGY_Y - 1 && my < ENERGY_Y + 5
                    ? Component.translatable("gui.ryzergen.energy", String.format("%,d", menu.energy()), String.format("%,d", BreederRunner.ENERGY_CAPACITY))
                    : null;
            if (tip != null) {
                graphics.renderTooltip(font, tip, mouseX, mouseY);
            }
        }
    }

    /** What a position does in this layout, so the player can see why a plan scores as it does. */
    private List<Component> positionTooltip(int position) {
        Position[] plan = menu.plan();
        ItemStack[] items = menu.items();
        BreederReactor.Analysis analysis = analysis();
        List<Component> lines = new java.util.ArrayList<>();
        int fuel = 0;
        int control = 0;
        for (int n : BreederReactor.neighbours(position)) {
            if (plan[n].accepts(items[n])) {
                fuel += plan[n] == Position.FUEL ? 1 : 0;
                control += plan[n] == Position.CONTROL ? 1 : 0;
            }
        }
        String key = "gui.ryzergen.breeder.position.";
        int colour = colour(plan[position]);
        switch (plan[position]) {
            case EMPTY -> lines.add(Component.translatable(key + "empty"));
            case FUEL -> {
                if (analysis.heat()[position] <= 0) {
                    lines.add(tip(key + "fuel.waiting", colour));
                    break;
                }
                lines.add(items[position].getHoverName().copy().withStyle(style -> style.withColor(colour)));
                lines.add(tip(key + "fuel.heat", analysis.hotSpot(position) ? BAD : VALUE, compact(Math.round(analysis.heat()[position]))));
                if (analysis.hotSpot(position)) {
                    lines.add(tip(key + "fuel.hot_spot", BAD, compact(Math.round(BreederReactor.hotSpot()))));
                }
                if (fuel > 0) {
                    lines.add(Component.translatable(key + "fuel.fuel", fuel));
                }
                if (control > 0) {
                    lines.add(Component.translatable(key + "fuel.control", control));
                }
                lines.add(Component.translatable(key + "fuel.burn", Math.round(analysis.burn()[position] * 100)));
            }
            case CONTROL -> {
                if (items[position].isEmpty()) {
                    lines.add(tip(key + "control.waiting", colour));
                } else {
                    lines.add(items[position].getHoverName().copy().withStyle(style -> style.withColor(colour)));
                    lines.add(tip(key + "control", fuel > 0 ? VALUE : DIM, fuel));
                }
            }
            case URANIUM, LITHIUM -> {
                String name = plan[position].name().toLowerCase(Locale.ROOT);
                if (items[position].isEmpty()) {
                    lines.add(tip(key + name + ".waiting", colour));
                } else if (!plan[position].accepts(items[position])) {
                    lines.add(items[position].getHoverName().copy().withStyle(style -> style.withColor(colour)));
                    lines.add(tip(key + "blanket.done", GOOD));
                } else {
                    lines.add(items[position].getHoverName().copy().withStyle(style -> style.withColor(colour)));
                    float minutes = analysis.breedMinutes(position);
                    lines.add(minutes > 0
                            ? tip(key + "blanket.breeding", VALUE, Math.max(1, Math.round(minutes * (1 - TargetRodItem.fraction(items[position])))))
                            : tip(key + "blanket.idle", DIM));
                }
            }
        }
        lines.add(tip("gui.ryzergen.station.tool.clear", DIM));
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Slot slot = getSlotUnderMouse();
        boolean position = slot != null && slot.index < BreederReactor.POSITIONS;
        if (button == 1 && position && menu.getCarried().isEmpty()) {
            // Right-click: take the item out, or clear the plan if there is none.
            if (slot.hasItem() || menu.plan(slot.getSlotIndex()) != Position.EMPTY) {
                press(BreederControlMenu.BUTTON_CLEAR + slot.getSlotIndex());
            }
            return true;
        }
        if (button == 0) {
            for (int k = 0; k < TOOLS.length; k++) {
                if (isOver(toolX(k), TOOL_Y, mouseX, mouseY)) {
                    tool = tool == TOOLS[k] ? null : TOOLS[k];
                    click();
                    return true;
                }
            }
            if (isOver(POWER_X, KEYS_Y, mouseX, mouseY)) {
                return press(BreederControlMenu.BUTTON_POWER);
            }
            if (isOver(REDSTONE_X, KEYS_Y, mouseX, mouseY)) {
                return press(BreederControlMenu.BUTTON_REDSTONE);
            }
            if (isOver(FLOW_DOWN_X, KEYS_Y, mouseX, mouseY)) {
                return press(BreederControlMenu.BUTTON_FLOW_DOWN);
            }
            if (isOver(FLOW_UP_X, KEYS_Y, mouseX, mouseY)) {
                return press(BreederControlMenu.BUTTON_FLOW_UP);
            }
            // With a plan tool picked and nothing held, clicking a position plans it.
            if (tool != null && position && menu.getCarried().isEmpty() && menu.plan(slot.getSlotIndex()) != tool) {
                return press(BreederControlMenu.BUTTON_PLAN + slot.getSlotIndex() * 8 + tool.ordinal());
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private boolean press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            click();
        }
        return true;
    }
}
