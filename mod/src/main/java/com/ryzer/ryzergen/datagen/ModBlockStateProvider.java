package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.storage.PressureTankBlock;
import com.ryzer.ryzergen.machine.processing.ProcessingBlock;
import com.ryzer.ryzergen.machine.processing.TallProcessingBlock;
import com.ryzer.ryzergen.machine.pump.IntakePumpBlock;
import com.ryzer.ryzergen.storage.WasteCaskBlock;
import net.minecraft.world.item.ItemDisplayContext;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.battery.BatteryChemistry;
import com.ryzer.ryzergen.battery.HomeBatteryBlock;
import com.ryzer.ryzergen.battery.HomeBatteryBlockEntity;
import com.ryzer.ryzergen.cable.CableBlock;
import com.ryzer.ryzergen.cable.CableSide;
import com.ryzer.ryzergen.cable.CableUpgrade;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterBlock;
import com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterBlock;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorPartBlock;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorSlot;
import com.ryzer.ryzergen.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ModBlockStateProvider extends BlockStateProvider {
    /** Shared by all three part blocks, since a formed quarter looks the same whatever part it is. */
    private final Map<MicroreactorSlot, ModelFile> microreactorFormed = new EnumMap<>(MicroreactorSlot.class);
    private final Map<MicroreactorSlot, ModelFile> microreactorRunning = new EnumMap<>(MicroreactorSlot.class);

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFiles) {
        super(output, RyzerGen.MOD_ID, existingFiles);
    }

    @Override
    protected void registerStatesAndModels() {
        ModBlocks.STONE_ORES.values().forEach(ore -> simpleBlockWithItem(ore.get(), cubeAll(ore.get())));
        ModBlocks.DEEPSLATE_ORES.values().forEach(ore -> simpleBlockWithItem(ore.get(), cubeAll(ore.get())));
        alloySmelter();
        electricAlloySmelter();
        cable(ModBlocks.ENERGY_CABLE.get(), "energy_cable");
        cable(ModBlocks.ITEM_PIPE.get(), "item_pipe");
        cable(ModBlocks.FLUID_PIPE.get(), "fluid_pipe");
        cable(ModBlocks.GAS_PIPE.get(), "gas_pipe");
        cableFittings();
        intakePump();
        coreCracker();
        reprocessor();
        fuelFabricator();
        wasteCask();
        stationParts();
        simpleBlockWithItem(ModBlocks.CREATIVE_BATTERY.get(), models().cubeBottomTop("creative_battery",
                modLoc("block/creative_battery_side"), modLoc("block/creative_top"), modLoc("block/creative_top")));
        simpleBlockWithItem(ModBlocks.CREATIVE_WATER_TANK.get(), models().cubeBottomTop("creative_water_tank",
                modLoc("block/creative_water_tank_side"), modLoc("block/creative_top"), modLoc("block/creative_top")));
        simpleBlockWithItem(ModBlocks.GRAPHITE_BLOCK.get(), cubeAll(ModBlocks.GRAPHITE_BLOCK.get()));
        tank(ModBlocks.PRESSURE_TANK.get(), "pressure_tank", "steel");
        tank(ModBlocks.FLUID_TANK.get(), "fluid_tank", "copper");
        homeBattery();
        microreactorPart(ModBlocks.REACTOR_HEART.get(), "reactor_heart");
        microreactorPart(ModBlocks.REACTOR_MACHINE_UNIT.get(), "reactor_machine_unit");
        microreactorPart(ModBlocks.COOLANT_JACKET.get(), "coolant_jacket");
    }

    /** Each part shows its own faces alone, and its quarter of the combined machine once formed. */
    private void microreactorPart(Block block, String name) {
        ModelFile single = models().orientable(name,
                modLoc("block/" + name + "_side"), modLoc("block/" + name + "_front"), modLoc("block/" + name + "_top"));
        if (microreactorFormed.isEmpty()) {
            for (MicroreactorSlot slot : MicroreactorSlot.FORMED) {
                microreactorFormed.put(slot, formedQuarter(slot, false));
                microreactorRunning.put(slot, formedQuarter(slot, true));
            }
        }
        getVariantBuilder(block).forAllStates(state -> {
            MicroreactorSlot slot = state.getValue(MicroreactorPartBlock.SLOT);
            Map<MicroreactorSlot, ModelFile> formed = state.getValue(MicroreactorPartBlock.RUNNING) ? microreactorRunning : microreactorFormed;
            return ConfiguredModel.builder()
                    .modelFile(slot.isFormed() ? formed.get(slot) : single)
                    .rotationY(((int) state.getValue(MicroreactorPartBlock.FACING).toYRot() + 180) % 360)
                    .build();
        });
        simpleBlockItem(block, single);
    }

    /** One quarter of the assembled 3D microreactor, built facing north. */
    private ModelFile formedQuarter(MicroreactorSlot slot, boolean running) {
        BlockModelBuilder model = models().getBuilder("microreactor_" + slot.getSerializedName() + (running ? "_running" : ""))
                .parent(models().getExistingFile(mcLoc("block/block")));
        MicroreactorModel.build(model, slot, running, name -> modLoc("block/microreactor/" + name));
        return model;
    }

    /**
     * Cable: a multipart of a 6 x 6 core, one arm per connected side, and an 8 x 8 flange on
     * extract sides. Arms and flanges are modelled pointing north and turned into place.
     */
    /** An energy cable or item pipe: a core, an arm per connected side and a flange per extract side. */
    private void cable(Block block, String name) {
        ResourceLocation cable = modLoc("block/" + name);
        ResourceLocation coreTexture = modLoc("block/" + name + "_core");
        ResourceLocation flange = modLoc("block/" + name + "_flange");
        // The core is a plain junction box. Arms map the cable texture's stripe band (rows 5 to 10)
        // along their length with explicit UVs, so one continuous stripe runs the whole way.
        ModelFile core = models().getBuilder(name + "_core").texture("particle", cable).texture("core", coreTexture)
                .element().from(5, 5, 5).to(11, 11, 11).textureAll("#core").end();
        ModelFile arm = models().getBuilder(name + "_arm").texture("particle", cable)
                .texture("cable", cable).texture("core", coreTexture)
                .element().from(5, 5, 0).to(11, 11, 5)
                .face(Direction.NORTH).texture("#core").uvs(5, 5, 11, 11).end()
                .face(Direction.EAST).texture("#cable").uvs(0, 5, 5, 11).end()
                .face(Direction.WEST).texture("#cable").uvs(0, 5, 5, 11).end()
                .face(Direction.UP).texture("#cable").uvs(0, 5, 5, 11).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
                .face(Direction.DOWN).texture("#cable").uvs(0, 5, 5, 11).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
                .end();
        ModelFile plate = models().getBuilder(name + "_flange").texture("particle", cable).texture("flange", flange)
                .element().from(4, 4, 0).to(12, 12, 1).textureAll("#flange").end();
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block).part().modelFile(core).addModel().end();
        for (Direction dir : Direction.values()) {
            int x = dir == Direction.UP ? 270 : dir == Direction.DOWN ? 90 : 0;
            int y = dir.getAxis().isHorizontal() ? ((int) dir.toYRot() + 180) % 360 : 0;
            builder.part().modelFile(arm).rotationX(x).rotationY(y).addModel()
                    .condition(CableBlock.SIDES.get(dir), CableSide.CONNECTED, CableSide.EXTRACT).end();
            builder.part().modelFile(plate).rotationX(x).rotationY(y).addModel()
                    .condition(CableBlock.SIDES.get(dir), CableSide.EXTRACT).end();
        }
        // The item is the pipe itself, as in Pipez: a straight run with the band along it and the core
        // texture on the ends, so every kind is easy to tell apart in the inventory.
        ModelFile straight = models().getBuilder(name + "_inventory").parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", cable).texture("cable", cable).texture("core", coreTexture)
                .element().from(5, 5, 0).to(11, 11, 16)
                .face(Direction.NORTH).texture("#core").uvs(5, 5, 11, 11).end()
                .face(Direction.SOUTH).texture("#core").uvs(5, 5, 11, 11).end()
                .face(Direction.EAST).texture("#cable").uvs(0, 5, 16, 11).end()
                .face(Direction.WEST).texture("#cable").uvs(0, 5, 16, 11).end()
                .face(Direction.UP).texture("#cable").uvs(0, 5, 16, 11).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
                .face(Direction.DOWN).texture("#cable").uvs(0, 5, 16, 11).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
                .end();
        itemModels().getBuilder(name).parent(straight);
    }

    /**
     * Electric alloy smelter: a graphite machine inside a light steel frame, with depth in the
     * Mekanism spirit. Trim base and cap, steel corner pillars, the body set back a pixel between
     * them, a raised window onto the heating coil (glowing when working), vents on the sides and
     * the energy port on the back. Built facing north and turned into place.
     */
    /**
     * Home battery: a two-high cabinet drawn as one 16 x 32 design and cut per block. A trim plinth
     * and cap, a light casing body, graphite posts framing a recessed bay column, the energy port on
     * the back. Each module is its own small model, added by the multipart when its bay is filled;
     * bays are 4 pixels apart, three per block, so none crosses the join.
     */
    private void homeBattery() {
        Block block = ModBlocks.HOME_BATTERY.get();
        BoxModel cabinet = new BoxModel();
        cabinet.add("trim", 1, 0, 1, 15, 2, 15);
        cabinet.add("frame", 2, 2, 3, 14, 30, 14);
        cabinet.add("trim", 1, 2, 2, 3, 30, 4);
        cabinet.add("trim", 13, 2, 2, 15, 30, 4);
        cabinet.add("body", 3, 3, 2.5F, 13, 29, 3);
        cabinet.add("trim", 1, 30, 1, 15, 32, 15);
        cabinet.add("glow_off", 3, 30.5F, 0.9F, 13, 31, 1);
        cabinet.add("trim", 4, 5, 14, 12, 13, 15);
        cabinet.add("trim", 3, 4, 15, 13, 14, 16).decal(Direction.SOUTH, "port_energy");
        Function<String, ResourceLocation> textures = texture -> switch (texture) {
            case "frame" -> modLoc("block/microreactor/steel");
            case "trim" -> modLoc("block/microreactor/steel_dark");
            case "port_energy", "glow", "glow_off" -> modLoc("block/microreactor/" + texture);
            default -> modLoc("block/machine/" + texture);
        };
        ModelFile[] halves = new ModelFile[2];
        for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
            BlockModelBuilder model = models().getBuilder("home_battery_" + half.getSerializedName())
                    .parent(models().getExistingFile(mcLoc("block/block")));
            cabinet.build(model, "frame", textures, half == DoubleBlockHalf.LOWER ? 0 : 16);
            halves[half.ordinal()] = model;
        }
        ModelFile[] modules = new ModelFile[HomeBatteryBlockEntity.MAX_MODULES];
        for (int bay = 0; bay < modules.length; bay++) {
            BoxModel plate = new BoxModel();
            float y = 4 + 4 * bay;
            plate.add("module_case", 3.5F, y, 1.8F, 12.5F, y + 3, 2.5F).decal(Direction.NORTH, "module_lead_acid");
            BlockModelBuilder model = models().getBuilder("home_battery_lead_acid_" + (bay + 1))
                    .parent(models().getExistingFile(mcLoc("block/block")));
            plate.build(model, "module_case", textures, bay < 3 ? 0 : 16);
            modules[bay] = model;
        }
        // Charge bar: 8 LED segments up the front of the right-hand post, 3 pixels apart, 4 per block.
        // Each has a lit (emissive) and an unlit model; the charge property picks which.
        ModelFile[][] leds = new ModelFile[2][HomeBatteryBlock.CHARGE_STEPS];
        for (int step = 0; step < HomeBatteryBlock.CHARGE_STEPS; step++) {
            for (int lit = 0; lit < 2; lit++) {
                BoxModel led = new BoxModel();
                float y = 4 + 3 * step;
                BoxModel.Box box = led.add(lit == 1 ? "glow" : "glow_off", 13.5F, y, 1.8F, 14.5F, y + 2, 2);
                if (lit == 1) {
                    box.glow(Direction.NORTH).glow(Direction.EAST).glow(Direction.WEST).glow(Direction.UP).glow(Direction.DOWN);
                }
                BlockModelBuilder model = models().getBuilder("home_battery_charge_" + (step + 1) + (lit == 1 ? "_lit" : ""))
                        .parent(models().getExistingFile(mcLoc("block/block")));
                led.build(model, lit == 1 ? "glow" : "glow_off", textures, step < 4 ? 0 : 16);
                leds[lit][step] = model;
            }
        }
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int y = ((int) facing.toYRot() + 180) % 360;
            for (int step = 0; step < HomeBatteryBlock.CHARGE_STEPS; step++) {
                DoubleBlockHalf half = step < 4 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER;
                Integer[] litLevels = java.util.stream.IntStream.rangeClosed(step + 1, HomeBatteryBlock.CHARGE_STEPS).boxed().toArray(Integer[]::new);
                Integer[] darkLevels = java.util.stream.IntStream.rangeClosed(0, step).boxed().toArray(Integer[]::new);
                builder.part().modelFile(leds[1][step]).rotationY(y).addModel()
                        .condition(HomeBatteryBlock.FACING, facing).condition(HomeBatteryBlock.HALF, half)
                        .condition(HomeBatteryBlock.CHARGE, litLevels).end();
                builder.part().modelFile(leds[0][step]).rotationY(y).addModel()
                        .condition(HomeBatteryBlock.FACING, facing).condition(HomeBatteryBlock.HALF, half)
                        .condition(HomeBatteryBlock.CHARGE, darkLevels).end();
            }
            for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
                builder.part().modelFile(halves[half.ordinal()]).rotationY(y).addModel()
                        .condition(HomeBatteryBlock.FACING, facing).condition(HomeBatteryBlock.HALF, half).end();
                for (int i = 0; i < 3; i++) {
                    int bay = i + (half == DoubleBlockHalf.LOWER ? 0 : 3);
                    builder.part().modelFile(modules[bay]).rotationY(y).addModel()
                            .condition(HomeBatteryBlock.FACING, facing).condition(HomeBatteryBlock.HALF, half)
                            .condition(HomeBatteryBlock.SEGMENTS.get(i), BatteryChemistry.LEAD_ACID).end();
                }
            }
        }
        itemModels().withExistingParent("home_battery", mcLoc("item/generated")).texture("layer0", modLoc("item/home_battery"));
    }

    private void electricAlloySmelter() {
        Block block = ModBlocks.ELECTRIC_ALLOY_SMELTER.get();
        ModelFile off = electricSmelterModel("electric_alloy_smelter", false);
        ModelFile on = electricSmelterModel("electric_alloy_smelter_on", true);
        horizontalBlock(block, state -> state.getValue(ElectricAlloySmelterBlock.ACTIVE) ? on : off);
        simpleBlockItem(block, off);
    }

    private ModelFile electricSmelterModel(String name, boolean active) {
        BoxModel boxes = new BoxModel();
        boxes.add("trim", 0, 0, 0, 16, 2, 16);
        boxes.add("trim", 0, 14, 0, 16, 16, 16);
        for (float x : new float[] {0, 14}) {
            for (float z : new float[] {0, 14}) {
                boxes.add("frame", x, 2, z, x + 2, 14, z + 2);
            }
        }
        boxes.add("body", 1, 2, 1, 15, 14, 15);
        BoxModel.Box window = boxes.add("frame", 3, 4, 0.5F, 13, 12, 1)
                .decal(Direction.NORTH, active ? "smelter_window_on" : "smelter_window");
        if (active) {
            window.glow(Direction.NORTH);
        }
        boxes.add("frame", 4, 2.5F, 0.5F, 12, 3.5F, 1).decal(Direction.NORTH, active ? "lamp_on" : "lamp_off");
        boxes.add("frame", 15, 5, 4, 15.5F, 11, 12).decal(Direction.EAST, "grille");
        boxes.add("frame", 0.5F, 5, 4, 1, 11, 12).decal(Direction.WEST, "grille");
        boxes.add("trim", 3, 3, 15, 13, 13, 16).decal(Direction.SOUTH, "port_energy");
        BlockModelBuilder model = models().getBuilder(name).parent(models().getExistingFile(mcLoc("block/block")));
        boxes.build(model, "frame", texture -> switch (texture) {
            case "frame" -> modLoc("block/microreactor/steel");
            case "trim" -> modLoc("block/microreactor/steel_dark");
            case "grille", "port_energy" -> modLoc("block/microreactor/" + texture);
            default -> modLoc("block/machine/" + texture);
        });
        return model;
    }

    private void intakePump() {
        Block block = ModBlocks.INTAKE_PUMP.get();
        ModelFile off = intakePumpModel("intake_pump", false);
        ModelFile on = intakePumpModel("intake_pump_on", true);
        horizontalBlock(block, state -> state.getValue(IntakePumpBlock.RUNNING) ? on : off);
        simpleBlockItem(block, off);
    }

    /**
     * Intake pump, facing north: a hazard-striped skid, a strainer with intake grilles low down (where
     * the water comes in), the pump casing at the front with a light strip, the gunmetal motor behind
     * it, the energy port on the back and the blue water port on top.
     */
    private ModelFile intakePumpModel(String name, boolean running) {
        BoxModel boxes = new BoxModel();
        boxes.add("steel_dark", 0, 0, 0, 16, 2, 16).sides("hazard");
        boxes.add("steel", 1, 2, 1, 15, 7, 15);
        boxes.add("steel", 0.5F, 3, 3, 1, 6, 13).decal(Direction.WEST, "grille");
        boxes.add("steel", 15, 3, 3, 15.5F, 6, 13).decal(Direction.EAST, "grille");
        boxes.add("steel", 3, 3, 0.5F, 13, 6, 1).decal(Direction.NORTH, "grille");
        boxes.add("steel_dark", 0.5F, 7, 0.5F, 15.5F, 8, 15.5F);
        // Pump casing at the front, with a light strip round it.
        boxes.add("steel", 3, 8, 1, 13, 14, 8);
        boxes.add(running ? "glow" : "glow_off", 2.9F, 12.25F, 0.9F, 13.1F, 12.75F, 8);
        // Motor behind it: a gunmetal drum, two overlapping boxes for a rounded look.
        boxes.add("lead", 4, 8, 8, 12, 14, 15);
        boxes.add("lead", 3, 9, 8, 13, 13, 15);
        // Energy port on the back.
        boxes.add("steel_dark", 3, 3, 15, 13, 13, 16).decal(Direction.SOUTH, "port_energy");
        // Water out: a copper riser to the blue port on top.
        boxes.add("copper", 5, 14, 5, 11, 15, 11);
        boxes.add("steel_dark", 3, 15, 3, 13, 16, 13).decal(Direction.UP, "port_coolant");
        BlockModelBuilder model = models().getBuilder(name).parent(models().getExistingFile(mcLoc("block/block")));
        boxes.build(model, "steel", texture -> modLoc("block/microreactor/" + texture));
        return model;
    }

    /**
     * Pressure tank (steel walls) and fluid tank (copper walls). On its own: posts at the corners, a sight glass in each side, a hazard-striped
     * base and a cap. In a tower, each block is drawn as the north-west corner (outside faces north
     * and west) and turned into place, with its glass at the inner edge, so the four corners share one
     * sight glass down the middle of each face, running the full height. Base and cap only on the
     * bottom and top layers.
     */
    private void tank(Block block, String name, String wall) {
        BoxModel single = new BoxModel();
        single.add("steel_dark", 0, 0, 0, 16, 2, 16).sides("hazard");
        single.add("steel_dark", 0, 14, 0, 16, 16, 16).face(Direction.UP, "steel");
        for (float x : new float[] {0, 14}) {
            for (float z : new float[] {0, 14}) {
                single.add("steel_dark", x, 2, z, x + 2, 14, z + 2);
            }
        }
        single.add(wall, 2, 2, 0, 6, 14, 1);
        single.add(wall, 10, 2, 0, 14, 14, 1);
        single.add("glass", 6, 2, 0.5F, 10, 14, 1).face(Direction.SOUTH, "liner");
        single.add(wall, 2, 2, 15, 6, 14, 16);
        single.add(wall, 10, 2, 15, 14, 14, 16);
        single.add("glass", 6, 2, 15, 10, 14, 15.5F).face(Direction.NORTH, "liner");
        single.add(wall, 0, 2, 2, 1, 14, 6);
        single.add(wall, 0, 2, 10, 1, 14, 14);
        single.add("glass", 0.5F, 2, 6, 1, 14, 10).face(Direction.EAST, "liner");
        single.add(wall, 15, 2, 2, 16, 14, 6);
        single.add(wall, 15, 2, 10, 16, 14, 14);
        single.add("glass", 15, 2, 6, 15.5F, 14, 10).face(Direction.WEST, "liner");
        ModelFile singleModel = tankModel(name, single);

        Map<PressureTankBlock.Layer, ModelFile> tower = new EnumMap<>(PressureTankBlock.Layer.class);
        for (PressureTankBlock.Layer layer : PressureTankBlock.Layer.values()) {
            boolean base = layer == PressureTankBlock.Layer.BOTTOM || layer == PressureTankBlock.Layer.SINGLE;
            boolean cap = layer == PressureTankBlock.Layer.TOP || layer == PressureTankBlock.Layer.SINGLE;
            float y1 = base ? 2 : 0;
            float y2 = cap ? 14 : 16;
            BoxModel corner = new BoxModel();
            if (base) {
                corner.add("steel_dark", 0, 0, 0, 16, 2, 16).sides("hazard");
            }
            if (cap) {
                corner.add("steel_dark", 0, 14, 0, 16, 16, 16).face(Direction.UP, "steel");
            }
            corner.add("steel_dark", 0, y1, 0, 2, y2, 2);
            corner.add(wall, 2, y1, 0, 12, y2, 1);
            corner.add("glass", 12, y1, 0.5F, 16, y2, 1).face(Direction.SOUTH, "liner");
            corner.add(wall, 0, y1, 2, 1, y2, 12);
            corner.add("glass", 0.5F, y1, 12, 1, y2, 16).face(Direction.EAST, "liner");
            tower.put(layer, tankModel(name + "_" + layer.getSerializedName(), corner));
        }

        getVariantBuilder(block).forAllStates(state -> {
            PressureTankBlock.Corner corner = state.getValue(PressureTankBlock.CORNER);
            if (corner == PressureTankBlock.Corner.NONE) {
                return ConfiguredModel.builder().modelFile(singleModel).build();
            }
            int rotation = switch (corner) {
                case NORTH_EAST -> 90;
                case SOUTH_EAST -> 180;
                case SOUTH_WEST -> 270;
                default -> 0;
            };
            return ConfiguredModel.builder().modelFile(tower.get(state.getValue(PressureTankBlock.LAYER))).rotationY(rotation).build();
        });
        simpleBlockItem(block, singleModel);
    }

    private ModelFile tankModel(String name, BoxModel boxes) {
        BlockModelBuilder model = models().getBuilder(name).parent(models().getExistingFile(mcLoc("block/block")));
        boxes.build(model, "steel", texture -> switch (texture) {
            case "glass" -> modLoc("block/machine/tank_glass");
            case "liner" -> modLoc("block/microreactor/steel_dark");
            default -> modLoc("block/microreactor/" + texture);
        });
        // The sight glass is clear with opaque glare streaks, drawn as cutout. Translucent glass would
        // write depth and hide the translucent steam behind it. Each pane's inner face is a dark liner:
        // from outside it faces away and is culled, so you see through the near pane to the far pane's
        // liner, dark behind the steam.
        model.renderType("cutout");
        return model;
    }

    /**
     * Fission station parts, as they look before the station forms (then the core draws it all). The
     * formed states reuse the same models, which are hidden anyway.
     */
    private void stationParts() {
        simpleBlockWithItem(ModBlocks.STATION_CASING.get(), models().cubeAll("station_casing", modLoc("block/station_casing")));
        simpleBlockWithItem(ModBlocks.STATION_GLASS.get(),
                models().cubeAll("station_glass", modLoc("block/station_glass")).renderType("cutout"));
        simpleBlockWithItem(ModBlocks.TURBINE_ROTOR.get(), models().cubeBottomTop("turbine_rotor",
                modLoc("block/turbine_rotor_side"), modLoc("block/station_casing"), modLoc("block/turbine_rotor_top")));
        ModelFile core = models().orientable("station_core", modLoc("block/station_casing"),
                modLoc("block/station_core_front"), modLoc("block/station_casing"));
        horizontalBlock(ModBlocks.STATION_CORE.get(), core);
        simpleBlockItem(ModBlocks.STATION_CORE.get(), core);
    }

    /** Our shared material and decal textures, by short name. */
    private ResourceLocation machineTexture(String name) {
        return switch (name) {
            case "body", "lamp_on", "lamp_off" -> modLoc("block/machine/" + name);
            default -> modLoc("block/microreactor/" + name);
        };
    }

    private BlockModelBuilder boxModel(String name) {
        return models().getBuilder(name).parent(models().getExistingFile(mcLoc("block/block")));
    }

    /** A thin light strip across a face, lit (emissive) while the machine works. */
    private static void lampStrip(BoxModel boxes, boolean active, float x1, float y1, float z1, float x2, float y2, float z2) {
        BoxModel.Box strip = boxes.add(active ? "glow" : "glow_off", x1, y1, z1, x2, y2, z2);
        if (active) {
            for (Direction dir : Direction.values()) {
                strip.glow(dir);
            }
        }
    }

    /**
     * Core Cracker, facing north: a hazard-striped skid, a light casing with a jaw crusher's
     * flywheels on either side, a grilled access door on the front with a light strip over it, the
     * feed hopper with its item port on top, and the energy port on the back.
     */
    private ModelFile coreCrackerModel(String name, boolean active) {
        BoxModel boxes = new BoxModel();
        boxes.add("steel_dark", 0, 0, 0, 16, 2, 16).sides("hazard");
        boxes.add("steel", 1, 2, 1, 15, 13, 15);
        boxes.add("steel_dark", 0.5F, 13, 0.5F, 15.5F, 14, 15.5F);
        // Flywheels: gunmetal drums on both sides, two overlapping boxes for a rounded look.
        for (float x : new float[] {0, 15}) {
            boxes.add("lead", x, 4, 5, x + 1, 11, 11);
            boxes.add("lead", x, 5, 4, x + 1, 10, 12);
            boxes.add("accent", x + (x == 0 ? -0.25F : 1), 7, 7, x + (x == 0 ? 0 : 1.25F), 8, 9);
        }
        // Access door, with an orange latch.
        boxes.add("steel_dark", 3, 3, 0.5F, 13, 11, 1).decal(Direction.NORTH, "grille");
        boxes.add("accent", 11.5F, 6, 0, 12.5F, 8, 0.5F);
        lampStrip(boxes, active, 2, 12, 0.75F, 14, 12.5F, 1);
        // Feed hopper and item port on top.
        boxes.add("steel", 2.5F, 14, 2.5F, 13.5F, 15, 13.5F);
        boxes.add("steel_dark", 3, 15, 3, 13, 16, 13).decal(Direction.UP, "port_fuel");
        boxes.add("steel_dark", 3, 3, 15, 13, 13, 16).decal(Direction.SOUTH, "port_energy");
        BlockModelBuilder model = boxModel(name);
        boxes.build(model, "steel", this::machineTexture);
        return model;
    }

    private void coreCracker() {
        Block block = ModBlocks.CORE_CRACKER.get();
        ModelFile off = coreCrackerModel("core_cracker", false);
        ModelFile on = coreCrackerModel("core_cracker_on", true);
        horizontalBlock(block, state -> state.getValue(ProcessingBlock.ACTIVE) ? on : off);
        simpleBlockItem(block, off);
    }

    /**
     * Reprocessor, two high, facing north: a light dissolver tank on a hazard-striped skid, with
     * the water port (blue) on its east side and the energy port on the back; above it a gunmetal
     * lead-lined column with a sight glass that glows while it works, a copper line running up
     * from the tank, and the item port on the cap. Drawn as one 32-high design and cut per block.
     */
    private BoxModel reprocessorBoxes(boolean active) {
        BoxModel boxes = new BoxModel();
        boxes.add("steel_dark", 0, 0, 0, 16, 2, 16).sides("hazard");
        boxes.add("steel", 1, 2, 1, 15, 14, 15);
        boxes.add("steel_dark", 3, 4, 0.5F, 13, 10, 1).decal(Direction.NORTH, "grille");
        lampStrip(boxes, active, 2, 11.5F, 0.75F, 14, 12, 1);
        boxes.add("steel_dark", 15, 3, 3, 16, 13, 13).decal(Direction.EAST, "port_coolant");
        boxes.add("steel_dark", 3, 3, 15, 13, 13, 16).decal(Direction.SOUTH, "port_energy");
        boxes.add("steel_dark", 0.5F, 14, 0.5F, 15.5F, 15.5F, 15.5F);
        // The column: gunmetal lead lining, rounded with two overlapping boxes, and trim bands.
        boxes.add("lead", 3, 15.5F, 2, 13, 29, 14);
        boxes.add("lead", 2, 15.5F, 3, 14, 29, 13);
        boxes.add("steel_dark", 1.5F, 21, 1.5F, 14.5F, 22, 14.5F);
        // Sight glass on the front, glowing while it works.
        boxes.add("steel_dark", 6, 16.5F, 1.25F, 10, 28, 2);
        lampStrip(boxes, active, 7, 17.5F, 1, 9, 27, 1.25F);
        // Copper line from the tank up the back corner into the column. It stands clear of the trim
        // band (which reaches 14.5), so no face of it lies on one of the band's and flickers.
        boxes.add("copper", 13, 15.5F, 13, 15, 26, 15);
        // Cap with the item port.
        boxes.add("steel_dark", 1.5F, 29, 1.5F, 14.5F, 31, 14.5F);
        boxes.add("steel_dark", 3, 31, 3, 13, 32, 13).decal(Direction.UP, "port_fuel");
        return boxes;
    }

    private void reprocessor() {
        Block block = ModBlocks.REPROCESSOR.get();
        ModelFile[][] halves = new ModelFile[2][2];
        for (int lit = 0; lit < 2; lit++) {
            for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
                BlockModelBuilder model = boxModel("reprocessor_" + half.getSerializedName() + (lit == 1 ? "_on" : ""));
                reprocessorBoxes(lit == 1).build(model, "steel", this::machineTexture, half == DoubleBlockHalf.LOWER ? 0 : 16);
                halves[lit][half.ordinal()] = model;
            }
        }
        horizontalBlock(block, state -> halves[state.getValue(ProcessingBlock.ACTIVE) ? 1 : 0][state.getValue(TallProcessingBlock.HALF).ordinal()]);
        // The item shows the whole machine, scaled down to fit a slot.
        BlockModelBuilder item = models().getBuilder("reprocessor_item").parent(models().getExistingFile(mcLoc("block/block")));
        reprocessorBoxes(false).buildWhole(item, "steel", this::machineTexture);
        item.transforms()
                .transform(ItemDisplayContext.GUI).rotation(30, 225, 0).translation(0, -3.2F, 0).scale(0.4F).end()
                .transform(ItemDisplayContext.GROUND).translation(0, 1, 0).scale(0.2F).end()
                .transform(ItemDisplayContext.FIXED).translation(0, -4, 0).scale(0.4F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 0, 0).scale(0.3F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).translation(0, -2, 0).scale(0.3F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 225, 0).translation(0, -2, 0).scale(0.3F).end()
                .end();
        simpleBlockItem(block, item);
    }

    /**
     * Fuel Fabricator, facing north: a hazard-striped skid, a light lower cabinet with a grille and a
     * light strip, a graphite worktop, and an open press bay above it (corner posts, a gunmetal ram,
     * a copper rod lying on the tray) under a housing with the item port on top. Energy on the back.
     */
    private ModelFile fuelFabricatorModel(String name, boolean active) {
        BoxModel boxes = new BoxModel();
        boxes.add("steel_dark", 0, 0, 0, 16, 2, 16).sides("hazard");
        boxes.add("steel", 1, 2, 1, 15, 10, 15);
        boxes.add("steel_dark", 3, 3, 0.5F, 13, 7, 1).decal(Direction.NORTH, "grille");
        lampStrip(boxes, active, 2, 8, 0.75F, 14, 8.5F, 1);
        boxes.add("steel_dark", 0.5F, 10, 0.5F, 15.5F, 11, 15.5F);
        for (float x : new float[] {1, 13}) {
            for (float z : new float[] {1, 13}) {
                boxes.add("steel_dark", x, 11, z, x + 2, 14, z + 2);
            }
        }
        // The press: a gunmetal ram over a tray with a copper fuel rod on it.
        boxes.add("lead", 5, 12, 5, 11, 14, 11);
        boxes.add("steel", 3, 11, 6, 13, 11.5F, 10);
        boxes.add("copper", 4.5F, 11.5F, 7.5F, 12, 12, 8.5F);
        boxes.add("accent", 4, 11.5F, 7.5F, 4.5F, 12, 8.5F);
        boxes.add("steel", 1, 14, 1, 15, 15, 15);
        boxes.add("steel_dark", 3, 15, 3, 13, 16, 13).decal(Direction.UP, "port_fuel");
        boxes.add("steel_dark", 3, 3, 15, 13, 13, 16).decal(Direction.SOUTH, "port_energy");
        BlockModelBuilder model = boxModel(name);
        boxes.build(model, "steel", this::machineTexture);
        return model;
    }

    private void fuelFabricator() {
        Block block = ModBlocks.FUEL_FABRICATOR.get();
        ModelFile off = fuelFabricatorModel("fuel_fabricator", false);
        ModelFile on = fuelFabricatorModel("fuel_fabricator_on", true);
        horizontalBlock(block, state -> state.getValue(ProcessingBlock.ACTIVE) ? on : off);
        simpleBlockItem(block, off);
    }

    /**
     * Waste Cask: a light steel drum, rounded with overlapping boxes, with graphite rings (the
     * middle one hazard striped), a lid with the item port on top, and a four-segment gauge down
     * its face that lights a segment per quarter full.
     */
    private ModelFile wasteCaskModel(String name, int fill) {
        BoxModel boxes = new BoxModel();
        boxes.add("steel_dark", 1, 0, 1, 15, 1.5F, 15);
        boxes.add("steel", 2, 1.5F, 2, 14, 14.9F, 14);
        boxes.add("steel", 1.5F, 1.5F, 3, 14.5F, 14.9F, 13);
        boxes.add("steel", 3, 1.5F, 1.5F, 13, 14.9F, 14.5F);
        boxes.add("steel_dark", 1, 7, 1, 15, 9, 15).sides("hazard");
        boxes.add("steel_dark", 1, 13.5F, 1, 15, 15, 15);
        boxes.add("steel", 2.5F, 15, 2.5F, 13.5F, 15.5F, 13.5F);
        boxes.add("steel_dark", 3, 15.5F, 3, 13, 16, 13).decal(Direction.UP, "port_fuel");
        // The gauge: a graphite frame in front of the drum and rings, four segments bottom to top.
        boxes.add("steel_dark", 6.5F, 2.5F, 0.5F, 9.5F, 13, 1);
        for (int i = 0; i < WasteCaskBlock.FILL_STEPS; i++) {
            float y = 3 + 2.5F * i;
            BoxModel.Box segment = boxes.add(i < fill ? "glow" : "glow_off", 7, y, 0.25F, 9, y + 2, 0.5F);
            if (i < fill) {
                segment.glow(Direction.NORTH);
            }
        }
        BlockModelBuilder model = boxModel(name);
        boxes.build(model, "steel", this::machineTexture);
        return model;
    }

    private void wasteCask() {
        Block block = ModBlocks.WASTE_CASK.get();
        ModelFile[] models = new ModelFile[WasteCaskBlock.FILL_STEPS + 1];
        for (int fill = 0; fill <= WasteCaskBlock.FILL_STEPS; fill++) {
            models[fill] = wasteCaskModel(fill == 0 ? "waste_cask" : "waste_cask_" + fill, fill);
        }
        horizontalBlock(block, state -> models[state.getValue(WasteCaskBlock.FILL)]);
        simpleBlockItem(block, models[0]);
    }

    /**
     * Cable fittings (see CableUpgrade), one small model per tier and side, added by the client to
     * cables that have them. Each is drawn on the north side, in the arm next to the machine (the
     * arm runs from z 0 to 5), then turned to its side. Silver: a bright sleeve. Busbar: a heavy
     * copper clamp with bolted lugs. Cryogenic: a thick frosted jacket with a glowing cyan ring.
     */
    private void cableFittings() {
        for (CableUpgrade tier : CableUpgrade.values()) {
            if (tier == CableUpgrade.NONE) {
                continue;
            }
            List<float[]> boxes = new ArrayList<>();
            List<float[]> glowing = new ArrayList<>();
            switch (tier) {
                case SILVER -> boxes.add(new float[] {4.5F, 4.5F, 1, 11.5F, 11.5F, 3.5F});
                case BUSBAR -> {
                    boxes.add(new float[] {4, 4, 1, 12, 12, 4});
                    boxes.add(new float[] {3, 7, 1.5F, 4, 9, 3.5F});
                    boxes.add(new float[] {12, 7, 1.5F, 13, 9, 3.5F});
                }
                case CRYOGENIC -> {
                    boxes.add(new float[] {4, 4, 1, 12, 12, 5});
                    glowing.add(new float[] {3.75F, 3.75F, 2.75F, 12.25F, 12.25F, 3.25F});
                }
                default -> { }
            }
            ResourceLocation texture = modLoc("block/fitting_" + tier.id());
            for (Direction side : Direction.values()) {
                BlockModelBuilder model = models().getBuilder("block/cable_fitting/" + tier.id() + "_" + side.getName())
                        .texture("particle", texture).texture("fitting", texture).texture("glow", modLoc("block/microreactor/glow"));
                for (float[] box : boxes) {
                    fittingBox(model, box, side, "#fitting", false);
                }
                for (float[] box : glowing) {
                    fittingBox(model, box, side, "#glow", true);
                }
            }
        }
    }

    /** One box of a fitting, drawn on the north side and turned to face {@code side}. */
    private static void fittingBox(BlockModelBuilder model, float[] box, Direction side, String texture, boolean glow) {
        float[] a = turn(box[0], box[1], box[2], side);
        float[] b = turn(box[3], box[4], box[5], side);
        model.element()
                .from(Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]))
                .to(Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2]))
                .allFaces((dir, face) -> {
                    face.texture(texture);
                    if (glow) {
                        face.emissivity(15, 15);
                    }
                })
                .end();
    }

    /** A point on the north side (the face at z 0) moved to the same place on {@code side}. */
    private static float[] turn(float x, float y, float z, Direction side) {
        return switch (side) {
            case NORTH -> new float[] {x, y, z};
            case SOUTH -> new float[] {16 - x, y, 16 - z};
            case WEST -> new float[] {z, y, 16 - x};
            case EAST -> new float[] {16 - z, y, x};
            case UP -> new float[] {x, 16 - z, y};
            case DOWN -> new float[] {x, z, 16 - y};
        };
    }

    private void alloySmelter() {
        Block block = ModBlocks.ALLOY_SMELTER.get();
        ModelFile off = models().orientable("alloy_smelter",
                modLoc("block/alloy_smelter_side"), modLoc("block/alloy_smelter_front"), modLoc("block/alloy_smelter_top"));
        ModelFile on = models().orientable("alloy_smelter_on",
                modLoc("block/alloy_smelter_side"), modLoc("block/alloy_smelter_front_on"), modLoc("block/alloy_smelter_top"));
        horizontalBlock(block, state -> state.getValue(AlloySmelterBlock.LIT) ? on : off);
        simpleBlockItem(block, off);
    }
}
