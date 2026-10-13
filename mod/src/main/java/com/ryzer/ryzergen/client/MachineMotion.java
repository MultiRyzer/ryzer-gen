package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.AnimatedMachine;
import com.ryzer.ryzergen.machine.MachineClock;
import com.ryzer.ryzergen.machine.MachineClock.Look;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the single-block machines do while they run, as data: the moving parts their renderer
 * (MachinePartsRenderer) turns and slides, the lit faces it fills and dims (glows, and the status
 * lamp by each readout), and the wisps their client tick gives off. Bringing a machine to life is a
 * few lines here and its part models in datagen, not a renderer of its own.
 *
 * <p>Everything follows the machine's clock (MachineClock): its spin and heat ease in and out, so
 * parts spin up and wind down and hot things warm and cool rather than switch; its beat runs faster
 * with speed modules; and its look (off, standby, working, starved) lights the lamp. Positions are in
 * pixels on the machine facing north (from the foot of a two-high one); the renderer turns them with
 * the block.
 */
public final class MachineMotion {
    private MachineMotion() {}

    /** One frame of a machine's clock. {@code time} runs on whether or not it works, for lamps. */
    public record Clock(float spin, double beat, float heat, Look look, float time) {
        static Clock of(AnimatedMachine machine, float partialTick, Level level) {
            MachineClock clock = machine.clock();
            float time = level == null ? 0 : (level.getGameTime() % 24_000) + partialTick;
            return new Clock(clock.spin(), clock.beat(partialTick), clock.heat(), clock.look(), time);
        }

        /** How far through a cycle of {@code length} beats, 0 to 1, started {@code phase} of a cycle on. */
        public float cycle(float length, float phase) {
            double at = (beat / length + phase) % 1;
            return (float) (at < 0 ? at + 1 : at);
        }
    }

    public sealed interface Element permits Part, Glow {}

    /** A model of its own (built in datagen), moved by {@code move}. Warm parts are lit by the machine's heat. */
    public record Part(ModelResourceLocation model, Move move, boolean warm) implements Element {}

    /**
     * Lit faces over the block model: the box's {@code faces}, a hair proud of them, filled from its
     * foot to {@code fill} of its height and tinted {@code shine} (ARGB; no alpha, not drawn). Seen
     * at full brightness; the texture maps by position, as the block model's does.
     */
    public record Glow(ResourceLocation texture, float x1, float y1, float z1, float x2, float y2, float z2,
                       Set<Direction> faces, Fill fill, Shine shine) implements Element {}

    /**
     * Wisps a working machine gives off about once in {@code every} ticks, at a point, or anywhere
     * within {@code spread} pixels of it across; drifting up at {@code rise} blocks a tick.
     */
    public record Puff(ParticleOptions particle, float x, float y, float z, float spread, float rise, int every) {}

    @FunctionalInterface
    public interface Move {
        void apply(PoseStack pose, Clock clock);
    }

    @FunctionalInterface
    public interface Fill {
        float to(Clock clock);
    }

    @FunctionalInterface
    public interface Shine {
        int argb(Clock clock);
    }

    private static final Map<String, List<Element>> ELEMENTS = new HashMap<>();
    private static final Map<String, List<Puff>> PUFFS = new HashMap<>();

    public static final ModelResourceLocation CORE_CRACKER_FLYWHEEL = part("core_cracker_flywheel");
    public static final ModelResourceLocation FUEL_FABRICATOR_RAM = part("fuel_fabricator_ram");
    public static final ModelResourceLocation ELECTROREFINER_BASKET = part("electrorefiner_basket");
    public static final ModelResourceLocation ELECTROREFINER_CATHODE = part("electrorefiner_cathode");
    public static final ModelResourceLocation INTAKE_PUMP_COUPLING = part("intake_pump_coupling");
    public static final ModelResourceLocation REPROCESSOR_FAN = part("reprocessor_fan");

    static {
        // Core Cracker (a jaw crusher): the flywheels turn about their axle, which runs east to
        // west, and crushed dust falls from the chute while it works.
        ELEMENTS.put("core_cracker", List.of(
                new Part(CORE_CRACKER_FLYWHEEL, spin(Axis.XP, 0, 7.5F, 8, 14), false),
                lamp(8.5F, 2.25F)));
        PUFFS.put("core_cracker", List.of(
                new Puff(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GRAY_CONCRETE_POWDER.defaultBlockState()),
                        8, 2.5F, 1.2F, 2.5F, 0, 4)));

        // Fuel Fabricator (a pellet press): the ram and platen (modelled on the bed) rest 1.5
        // pixels up and press down onto the bed in a steady stroke, puffing a little powder.
        ELEMENTS.put("fuel_fabricator", List.of(
                new Part(FUEL_FABRICATOR_RAM, slide(0, 1.5F, 0,
                        clock -> 1 - clock.spin() * (1 - Mth.cos((float) (clock.beat() * 14 % 360) * Mth.DEG_TO_RAD)) / 2), false),
                lamp(6.5F, 1.25F)));
        PUFFS.put("fuel_fabricator", List.of(new Puff(ParticleTypes.WHITE_ASH, 8, 9.75F, 8, 2, 0.005F, 6)));

        // Lithium Extractor: brine pumped up through the sorbent beds and pressed back out, the lit
        // bed rising up each column (two boxes: flanks on the wide one, front and back on the
        // narrow), and bubbles breaking on the brine.
        List<Element> lithium = new ArrayList<>();
        ResourceLocation bed = texture("lithium_extractor/column_on");
        Fill stroke = clock -> Math.max(0.08F, clock.spin() * pumpStroke(clock.cycle(80, 0)));
        for (float x : new float[] {5, 11}) {
            lithium.add(new Glow(bed, x - 2, 5, 7, x + 2, 14, 11, EnumSet.of(Direction.WEST, Direction.EAST), stroke, MachineMotion::whileWorking));
            lithium.add(new Glow(bed, x - 1.5F, 5, 6.5F, x + 1.5F, 14, 11.5F, EnumSet.of(Direction.NORTH, Direction.SOUTH), stroke, MachineMotion::whileWorking));
        }
        lithium.add(lamp(8.5F, 1));
        ELEMENTS.put("lithium_extractor", List.copyOf(lithium));
        PUFFS.put("lithium_extractor", List.of(new Puff(ParticleTypes.BUBBLE_POP, 8, 5.1F, 9.5F, 5, 0, 3)));

        // Monazite Processor: pulsed extraction columns, the liquid driven up and down to mix the
        // two phases, the two columns half a beat apart; fumes off the hot acid in the digester.
        List<Element> monazite = new ArrayList<>();
        ResourceLocation solvent = texture("monazite_processor/column_on");
        float[] edges = {1.5F, 12.5F};
        for (int c = 0; c < edges.length; c++) {
            float phase = c * 0.5F;
            monazite.add(new Glow(solvent, edges[c], 9, 2, edges[c] + 2, 15, 4,
                    EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST),
                    clock -> clock.spin() * (0.3F + 0.7F * pulse(clock.cycle(30, phase))), MachineMotion::whileWorking));
        }
        monazite.add(lamp(6.5F, 1.25F));
        ELEMENTS.put("monazite_processor", List.copyOf(monazite));
        PUFFS.put("monazite_processor", List.of(new Puff(ParticleTypes.WHITE_SMOKE, 8, 15.4F, 4.3F, 3, 0.01F, 14)));

        // Reprocessor: the dissolver's agitator motor, its cooling fan turning on its west end, and
        // off-gas from the top of the column while it dissolves.
        ELEMENTS.put("reprocessor", List.of(new Part(REPROCESSOR_FAN, spin(Axis.XP, 0, 8, 8, 24), false)));
        PUFFS.put("reprocessor", List.of(new Puff(ParticleTypes.WHITE_SMOKE, 4.5F, 31.2F, 4.5F, 0.5F, 0.012F, 12)));

        // Electrorefiner (Argonne's Mark-IV, which refined EBR-II's fuel): the four anode baskets of
        // chopped fuel turn slowly in the molten salt; now and then the cathode lifts its plated
        // uranium clear of the salt for harvest and dips back. The salt and the heater in the well
        // round the pot glow with the cell's heat (dull red while it waits, bright while it works).
        // Off-gas wisps from the stack while it works.
        ELEMENTS.put("electrorefiner", List.of(
                new Part(ELECTROREFINER_BASKET, spin(Axis.YP, 8, 0, 6.5F, 3), true),
                new Part(ELECTROREFINER_CATHODE, slide(0, 1.25F, 0, clock -> clock.spin() * harvest(clock.cycle(200, 0))), true),
                new Glow(texture("electrorefiner/salt_molten"), 2.75F, 9.25F, 2.75F, 13.25F, 9.25F, 10.25F,
                        EnumSet.of(Direction.UP), clock -> 1, clock -> hot(clock, 1)),
                new Glow(texture("electrorefiner/heater"), 1.5F, 7.8F, 1.5F, 14.5F, 7.8F, 11.5F,
                        EnumSet.of(Direction.UP), clock -> 1, clock -> hot(clock, 0.85F)),
                lamp(5.5F, 1.25F)));
        PUFFS.put("electrorefiner", List.of(new Puff(ParticleTypes.WHITE_SMOKE, 4, 15.75F, 3.25F, 0, 0.02F, 6)));

        // Electric Alloy Smelter (an induction furnace): the readout's lamp, and now and then an
        // ember spat from the molten charge in the crucible.
        ELEMENTS.put("electric_alloy_smelter", List.of(lamp(12.5F, 8.5F)));
        PUFFS.put("electric_alloy_smelter", List.of(new Puff(ParticleTypes.LAVA, 8, 11.1F, 6, 2, 0, 30)));

        // Intake Pump (a vertical pump set): the coupling between motor and pump turns in the motor
        // stool's open frame, and the discharge nozzle drips.
        ELEMENTS.put("intake_pump", List.of(new Part(INTAKE_PUMP_COUPLING, spin(Axis.YP, 8, 0, 8, 20), false)));
        PUFFS.put("intake_pump", List.of(new Puff(ParticleTypes.DRIPPING_WATER, 8, 2.9F, 0.4F, 1, 0, 20)));

        // Alloy Smelter (tier 1, fuel-fired): smoke from the flue while it burns.
        PUFFS.put("alloy_smelter", List.of(new Puff(ParticleTypes.SMOKE, 8, 16.2F, 8, 1.5F, 0.02F, 4)));
    }

    /** A machine's parts and glows, in drawing order (none for a machine with no moving parts). */
    public static List<Element> of(String machine) {
        return ELEMENTS.getOrDefault(machine, List.of());
    }

    /** Every part model, to load alongside the block models. */
    public static List<ModelResourceLocation> models() {
        List<ModelResourceLocation> models = new ArrayList<>();
        for (List<Element> elements : ELEMENTS.values()) {
            for (Element element : elements) {
                if (element instanceof Part part) {
                    models.add(part.model());
                }
            }
        }
        return models;
    }

    /**
     * Client tick: moves the machine's clock on (working, as its block shows it) and gives off its
     * wisps while it works, from points turned as the block faces.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AnimatedMachine machine, boolean working) {
        machine.clock().animate(working);
        List<Puff> puffs = PUFFS.get(machine.motionId());
        if (puffs == null || !working) {
            return;
        }
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        for (Puff puff : puffs) {
            if (level.random.nextInt(puff.every()) != 0) {
                continue;
            }
            float dx = (level.random.nextFloat() * 2 - 1) * puff.spread();
            float dz = (level.random.nextFloat() * 2 - 1) * puff.spread();
            Vec3 at = place(pos, facing, puff.x() + dx, puff.y(), puff.z() + dz);
            level.addParticle(puff.particle(), at.x, at.y, at.z,
                    (level.random.nextFloat() - 0.5F) * 0.01F, puff.rise(), (level.random.nextFloat() - 0.5F) * 0.01F);
        }
    }

    /** A point on the machine (pixels, facing north) in the world, turned as the blockstate turns its model. */
    private static Vec3 place(BlockPos pos, Direction facing, float x, float y, float z) {
        double dx = x / 16 - 0.5;
        double dz = z / 16 - 0.5;
        double turn = Math.toRadians(-(((int) facing.toYRot() + 180) % 360));
        double cos = Math.cos(turn);
        double sin = Math.sin(turn);
        return new Vec3(pos.getX() + 0.5 + dx * cos + dz * sin, pos.getY() + y / 16, pos.getZ() + 0.5 - dx * sin + dz * cos);
    }

    // ---------------------------------------------------------------- moves

    /** Turns about an axis through ({@code x}, {@code y}, {@code z}), {@code degrees} a beat. */
    public static Move spin(Axis axis, float x, float y, float z, float degrees) {
        return (pose, clock) -> {
            pose.translate(x / 16, y / 16, z / 16);
            pose.mulPose(axis.rotationDegrees((float) (clock.beat() * degrees % 360)));
            pose.translate(-x / 16, -y / 16, -z / 16);
        };
    }

    /** Slides by ({@code dx}, {@code dy}, {@code dz}) pixels, times {@code amount}. */
    public static Move slide(float dx, float dy, float dz, Fill amount) {
        return (pose, clock) -> {
            float f = amount.to(clock);
            pose.translate(dx * f / 16, dy * f / 16, dz * f / 16);
        };
    }

    // ---------------------------------------------------------------- cycles, 0 to 1 through one

    /**
     * A pump's stroke: a slow rise (half the cycle), a hold at the top, a squeeze back down (quicker,
     * starting slow as if against pressure), and a rest. It ends where it starts.
     */
    static float pumpStroke(float p) {
        if (p < 0.5F) {
            return smooth(p / 0.5F);
        }
        if (p < 0.65F) {
            return 1;
        }
        if (p < 0.85F) {
            float f = (p - 0.65F) / 0.2F;
            return 1 - f * f;
        }
        return 0;
    }

    /** A pulse: a quick surge up, a short hold, a slower fall back. */
    static float pulse(float p) {
        if (p < 0.25F) {
            float f = p / 0.25F;
            return f * (2 - f);
        }
        if (p < 0.4F) {
            return 1;
        }
        return 1 - smooth((p - 0.4F) / 0.6F);
    }

    /** A cathode harvest: down in the salt most of the time, then a lift, a pause clear of it, and a dip back. */
    static float harvest(float p) {
        if (p < 0.6F) {
            return 0;
        }
        if (p < 0.7F) {
            return smooth((p - 0.6F) / 0.1F);
        }
        if (p < 0.8F) {
            return 1;
        }
        if (p < 0.92F) {
            return 1 - smooth((p - 0.8F) / 0.12F);
        }
        return 0;
    }

    private static float smooth(float f) {
        return f * f * (3 - 2 * f);
    }

    // ---------------------------------------------------------------- glows

    /**
     * The status lamp in a readout's top right (the family screen's dark spot): the readout's top
     * edge at {@code top}, its face at {@code z}, its east edge at x 12.5 as every readout's is.
     */
    private static Glow lamp(float top, float z) {
        return new Glow(texture("family/lamp"), 4.5F, top - 1, z, 5.5F, top, z, EnumSet.of(Direction.NORTH), clock -> 1,
                MachineMotion::lampColour);
    }

    private static int whileWorking(Clock clock) {
        return clock.spin() < 0.02F ? 0 : 0xFFFFFFFF;
    }

    /**
     * Something glowing with the machine's heat, at {@code strength}: dull red while it is warm,
     * the texture's own colours when hot, fading out as it cools so the cold surface beneath shows
     * through. A starved machine flickers.
     */
    private static int hot(Clock clock, float strength) {
        float heat = clock.heat();
        if (heat < 0.02F) {
            return 0;
        }
        if (clock.look() == Look.STARVED) {
            heat *= 0.8F + 0.2F * Mth.sin(clock.time() * 0.9F) * Mth.sin(clock.time() * 2.3F);
        }
        float b = heat * strength;
        int alpha = Math.round(Math.min(1, heat * 5) * 255);
        return argb(alpha, b, b * (float) Math.sqrt(b), b * b);
    }

    /** The status lamp: steady green working, breathing amber waiting, blinking red starved, dark off. */
    private static int lampColour(Clock clock) {
        return switch (clock.look()) {
            case OFF -> 0;
            case WORKING -> 0xFF5BEA74;
            case STANDBY -> {
                float b = 0.35F + 0.65F * (0.5F + 0.5F * Mth.cos(clock.time() * Mth.TWO_PI / 50));
                yield argb(255, b * 0.94F, b * 0.63F, b * 0.19F);
            }
            case STARVED -> (int) (clock.time() / 5) % 2 == 0 ? 0xFFFF4A3C : 0;
        };
    }

    private static int argb(int alpha, float r, float g, float b) {
        return alpha << 24 | channel(r) << 16 | channel(g) << 8 | channel(b);
    }

    private static int channel(float f) {
        return Mth.clamp(Math.round(f * 255), 0, 255);
    }

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/" + name));
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/machine/" + path);
    }
}
