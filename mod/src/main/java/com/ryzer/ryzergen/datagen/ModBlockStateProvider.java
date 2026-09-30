package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.battery.container.ContainerPartBlock;
import com.ryzer.ryzergen.battery.container.ContainerLayout;
import com.ryzer.ryzergen.machine.pool.PoolPartBlock;
import com.ryzer.ryzergen.machine.pool.PoolLayout;
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
import net.minecraft.core.BlockPos;
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
import java.util.HashMap;
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
        lithiumExtractor();
        electrorefiner();
        wasteCask();
        stationParts();
        spentFuelPool();
        containerBattery();
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

    /**
     * The Spent Fuel Pool's parts. Alone each shows its own faces (the controller's front carries the
     * console); formed, each draws its cell of the pool's design (art/tools/pool_concept.py) in the
     * pool's current look, turned to face the way the controller does.
     */
    private void spentFuelPool() {
        DesignModel design = DesignModel.load("spent_fuel_pool");
        Map<String, ModelFile> formed = new HashMap<>();
        for (String look : design.stateNames()) {
            if (look.startsWith("crane_")) {
                // The crane's moving parts: whole models its renderer puts in place (PoolRenderer),
                // with the park position (the design's CRANE_ORIGIN) as their origin.
                design.buildPart(models().getBuilder("block/pool/" + look).parent(models().getExistingFile(mcLoc("block/block"))),
                        look, 40, 40, 19, "paint");
                continue;
            }
            for (int index = 1; index <= PoolLayout.CELLS; index++) {
                BlockPos cell = PoolLayout.cell(index);
                BlockModelBuilder model = models().getBuilder("block/pool/" + look + "_" + index)
                        .parent(models().getExistingFile(mcLoc("block/block")));
                design.build(model, look, cell.getX(), cell.getY(), cell.getZ(), "liner", models()::nested);
                formed.put(look + "_" + index, model);
            }
        }
        ModelFile liner = models().cubeAll("pool_liner", modLoc("block/pool/part_liner"));
        ModelFile crane = models().cubeAll("pool_crane", modLoc("block/pool/part_crane"));
        ModelFile controller = models().orientable("pool_controller", modLoc("block/pool/part_liner"),
                modLoc("block/pool/part_controller"), modLoc("block/pool/part_liner"));
        Map<Block, ModelFile> alone = Map.of(ModBlocks.POOL_LINER.get(), liner, ModBlocks.POOL_CRANE.get(), crane,
                ModBlocks.POOL_CONTROLLER.get(), controller);
        alone.forEach((block, single) -> {
            getVariantBuilder(block).forAllStates(state -> {
                int cell = state.getValue(PoolPartBlock.CELL);
                boolean turns = cell > 0 || block == ModBlocks.POOL_CONTROLLER.get();
                return ConfiguredModel.builder()
                        .modelFile(cell > 0 ? formed.get(state.getValue(PoolPartBlock.LOOK).getSerializedName() + "_" + cell) : single)
                        .rotationY(turns ? ((int) state.getValue(PoolPartBlock.FACING).toYRot() + 180) % 360 : 0)
                        .build();
            });
            simpleBlockItem(block, single);
        });
    }

    /**
     * The Container Battery's parts. Alone each shows its own faces; formed, each draws its cell of
     * the container's design (art/tools/container_concept.py), a front block showing its slot with
     * a rack in it when INSTALLED, turned to face the way the controller does.
     */
    private void containerBattery() {
        DesignModel design = DesignModel.load("container_battery");
        Map<String, ModelFile> formed = new HashMap<>();
        for (String look : design.stateNames()) {
            for (int index = 1; index <= ContainerLayout.CELLS; index++) {
                BlockPos cell = ContainerLayout.cell(index);
                BlockModelBuilder model = models().getBuilder("block/container/" + look + "_" + index)
                        .parent(models().getExistingFile(mcLoc("block/block")));
                design.build(model, look, cell.getX(), cell.getY(), cell.getZ(), "liner", models()::nested);
                formed.put(look + "_" + index, model);
            }
        }
        ModelFile frame = models().cubeAll("container_frame", modLoc("block/container/part_frame"));
        ModelFile thermal = models().cubeAll("thermal_unit", modLoc("block/container/part_thermal"));
        ModelFile controller = models().orientable("battery_controller", modLoc("block/container/part_frame"),
                modLoc("block/container/part_controller"), modLoc("block/container/part_frame"));
        Map<Block, ModelFile> alone = Map.of(ModBlocks.CONTAINER_FRAME.get(), frame, ModBlocks.THERMAL_UNIT.get(), thermal,
                ModBlocks.BATTERY_CONTROLLER.get(), controller);
        alone.forEach((block, single) -> {
            getVariantBuilder(block).forAllStates(state -> {
                int cell = state.getValue(ContainerPartBlock.CELL);
                boolean turns = cell > 0 || block == ModBlocks.BATTERY_CONTROLLER.get();
                String look = state.getValue(ContainerPartBlock.INSTALLED) ? "full" : "empty";
                return ConfiguredModel.builder()
                        .modelFile(cell > 0 ? formed.get(look + "_" + cell) : single)
                        .rotationY(turns ? ((int) state.getValue(ContainerPartBlock.FACING).toYRot() + 180) % 360 : 0)
                        .build();
            });
            simpleBlockItem(block, single);
        });
    }

    /** The formed microreactor's design (art/tools/microreactor_concept.py), read once. */
    private DesignModel microreactorDesign;

    /** One quarter of the assembled 3D microreactor, built facing north, cut from its design. */
    private ModelFile formedQuarter(MicroreactorSlot slot, boolean running) {
        if (microreactorDesign == null) {
            microreactorDesign = DesignModel.load("microreactor");
        }
        BlockModelBuilder model = models().getBuilder("microreactor_" + slot.getSerializedName() + (running ? "_running" : ""))
                .parent(models().getExistingFile(mcLoc("block/block")));
        boolean back = slot == MicroreactorSlot.LOWER_BACK || slot == MicroreactorSlot.UPPER_BACK;
        boolean upper = slot == MicroreactorSlot.UPPER_FRONT || slot == MicroreactorSlot.UPPER_BACK;
        microreactorDesign.build(model, running ? "running" : "idle", 0, upper ? 1 : 0, back ? 1 : 0, "steel", models()::nested);
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
     * them, a raised window onto the heating coil (glowing when working), vents on the sides.
     * No ports drawn: like every single-block machine, it takes pipes and cables on any side. Built
     * facing north and turned into place.
     */
    /**
     * Home battery: a two-high cabinet drawn as one 16 x 32 design and cut per block. A trim plinth
     * and cap, a light casing body, graphite posts framing a recessed bay column (cables connect on
     * any side, so no port is drawn). Each module is its own small model, added by the multipart when its bay is filled;
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
        Function<String, ResourceLocation> textures = texture -> switch (texture) {
            case "frame" -> modLoc("block/microreactor/steel");
            case "trim" -> modLoc("block/microreactor/steel_dark");
            case "glow", "glow_off" -> modLoc("block/microreactor/" + texture);
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

    /**
     * Electric Alloy Smelter, facing north: an induction furnace, as real ones are built, in the
     * service frame: the power cabinet across the back (the lit screen and a light strip on its
     * front, vents on its sides), and in the bay under the roof the crucible in its coil, the coil's
     * clamp bars holding its turns apart, the crucible's rim with its pour spout, and copper busbars
     * from the coil back into the cabinet. The coil and the crucible's mouth glow while it works.
     * Its own textures: art/tools/electric_smelter_textures.py.
     */
    private ModelFile electricSmelterModel(String name, boolean active) {
        BoxModel boxes = new BoxModel();
        boxes.add("skid", 0, 0, 0, 16, 2, 16);
        serviceFrame(boxes, 9, 2, "lpanel_13x12", 2);
        boxes.add("cabinet", 3.5F, 10.5F, 8.5F, 12.5F, 12.5F, 9).decal(Direction.NORTH, "screen").glow(Direction.NORTH);
        lampStrip(boxes, active, 2, 13.25F, 8.75F, 14, 13.75F, 9);
        boxes.add("cabinet", 14.5F, 5, 10, 15, 11, 14).decal(Direction.EAST, "vent");
        boxes.add("cabinet", 1, 5, 10, 1.5F, 11, 14).decal(Direction.WEST, "vent");
        // The crucible in its coil, in the bay.
        boxes.add("metal", 3, 2, 3, 13, 3, 8.5F);
        boxes.add(active ? "coil_on" : "coil", 3.5F, 3, 3.5F, 12.5F, 10, 8.5F);
        // The coil's clamp bars, which hold its turns apart, as on a real induction coil.
        for (float x : new float[] {5, 10.25F}) {
            boxes.add("metal", x, 3, 3, x + 0.75F, 10, 3.5F);
        }
        for (float z : new float[] {4.5F, 7}) {
            boxes.add("metal", 3, 3, z, 3.5F, 10, z + 0.75F);
            boxes.add("metal", 12.5F, 3, z, 13, 10, z + 0.75F);
        }
        BoxModel.Box rim = boxes.add("metal", 4, 10, 4, 12, 11, 8).face(Direction.UP, active ? "mouth_on" : "mouth");
        if (active) {
            rim.glow(Direction.UP);
        }
        boxes.add("metal", 7, 10, 3.25F, 9, 10.5F, 4);
        // Busbars from the coil's sides back into the cabinet.
        for (float x : new float[] {2.75F, 12.5F}) {
            boxes.add("copper", x, 5, 5.5F, x + 0.75F, 5.75F, 9);
            boxes.add("copper", x, 7.5F, 5.5F, x + 0.75F, 8.25F, 9);
        }
        BlockModelBuilder model = models().getBuilder(name).parent(models().getExistingFile(mcLoc("block/block")));
        boxes.build(model, "cabinet", machineParts("electric_smelter"));
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
     * Intake pump, facing north: a vertical pump set, as on a real intake. A rounded gunmetal pump
     * casing (the volute) on the family skid, a bright flange ring, the electric motor standing on it
     * in water-blue with cooling fins, its fan cover and shaft cap on top, and a copper discharge
     * nozzle with a flange at the front. A light strip round the casing while it runs. Pipes and
     * cables connect on any side, so no ports are drawn. Its own textures:
     * art/tools/intake_pump_textures.py.
     */
    private ModelFile intakePumpModel(String name, boolean running) {
        BoxModel boxes = new BoxModel();
        boxes.add("skid", 0, 0, 0, 16, 2, 16);
        // The volute, rounded with two boxes of different heights (no shared faces to flicker).
        boxes.add("metal", 2.5F, 2, 2.5F, 13.5F, 7, 13.5F);
        boxes.add("metal", 2, 2, 3.5F, 14, 6.5F, 12.5F);
        lampStrip(boxes, running, 3, 5, 2.25F, 13, 5.5F, 2.5F);
        boxes.add("bright", 3.5F, 7, 3.5F, 12.5F, 7.5F, 12.5F);
        // The motor.
        boxes.add("motor", 4, 7.5F, 4, 12, 14, 12);
        boxes.add("motor", 3.5F, 7.5F, 4.75F, 12.5F, 13.75F, 11.25F);
        boxes.add("fan_cover", 4.5F, 14, 4.5F, 11.5F, 15, 11.5F);
        // Water leaves by the top: the discharge flange a pipe meets, on a neck through the fan cover.
        topConnector(boxes, 15, 16);
        backConnector(boxes, 12);
        // Discharge nozzle.
        boxes.add("copper", 6.5F, 3, 0.5F, 9.5F, 6, 2.5F);
        boxes.add("bright", 6, 2.5F, 1, 10, 6.5F, 1.5F);
        BlockModelBuilder model = boxModel(name);
        boxes.build(model, "metal", machineParts("intake_pump"));
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
        // The fusion preview: a station core face on a creative top, so it reads as a test block.
        ModelFile fusionPreview = models().orientableWithBottom("fusion_preview", modLoc("block/station_casing"),
                modLoc("block/station_core_front"), modLoc("block/station_casing"), modLoc("block/creative_top"));
        horizontalBlock(ModBlocks.FUSION_PREVIEW.get(), fusionPreview);
        simpleBlockItem(ModBlocks.FUSION_PREVIEW.get(), fusionPreview);
        // The sun gate preview uses the same block (its coverage setting changes only the drawing).
        horizontalBlock(ModBlocks.SUN_GATE_PREVIEW.get(), fusionPreview);
        simpleBlockItem(ModBlocks.SUN_GATE_PREVIEW.get(), fusionPreview);
    }

    /** The machine family's shared textures (art/tools/machine_family.py). */
    private static final java.util.Set<String> FAMILY = java.util.Set.of(
            "skid", "cabinet", "lid", "screen", "vent", "metal", "bright", "copper", "accent", "hazard", "connector");

    /*
     * Where pipes meet a single-block machine. Pipes and cables connect on any side (MachineItemPort),
     * but a pipe should meet something: the machine's top reaches the top of the block over the
     * pipe's centre (its own feature where it has one: a hopper, a flue), and the back carries a
     * connector, a neutral graphite flange round a socket, centred on the face where a pipe meets it,
     * on a neck from the body. Neutral, not a port's colour: any side takes anything. The front stays
     * clean for the screen; flanks are left to the machine's own features.
     */

    /**
     * The service frame some machines are built in, so their pipes meet a flat back and a flat top:
     * a rear cabinet across the back (its front at z {@code cabinetFront}, from {@code cabinetBottom}
     * up, a designed panel {@code backPanel} on its back), a flat roof over the whole machine, two
     * corner posts at the front holding it up (from {@code postBottom}), and the connectors on the
     * roof and the cabinet's back. The machine's working parts sit in the open bay under the roof.
     */
    private static void serviceFrame(BoxModel boxes, float cabinetFront, float cabinetBottom, String backPanel, float postBottom) {
        boxes.add("cabinet", 1.5F, cabinetBottom, cabinetFront, 14.5F, 14, 15.5F).decal(Direction.SOUTH, backPanel);
        boxes.add("metal", 1.5F, 14, 1.5F, 14.5F, 15.5F, 15.5F).decal(Direction.UP, "lpanel_13x14");
        for (float x : new float[] {1.5F, 13}) {
            boxes.add("metal", x, postBottom, 1.5F, x + 1.5F, 14, 3);
        }
        topConnector(boxes, 15.5F, 16);
        backConnector(boxes, 15.5F);
    }

    /** The back's connector: a 6 x 6 neck from the body's back (at z {@code body}) to an 8 x 8 flange flush with the block's back. */
    private static void backConnector(BoxModel boxes, float body) {
        backConnector(boxes, body, 8);
    }

    /** The back's connector, centred at height {@code y} (8 on a one-block machine). */
    private static void backConnector(BoxModel boxes, float body, float y) {
        if (body < 15.5F) {
            boxes.add("metal", 5, y - 3, body, 11, y + 3, 15.5F);
        }
        boxes.add("metal", 4, y - 4, 15.5F, 12, y + 4, 16).decal(Direction.SOUTH, "connector");
    }

    /** The top's connector: a 4 x 4 neck from the body's top (at y {@code body}) to an 8 x 8 flange flush with {@code top}. */
    private static void topConnector(BoxModel boxes, float body, float top) {
        if (body < top - 0.5F) {
            boxes.add("metal", 6, body, 6, 10, top - 0.5F, 10);
        }
        boxes.add("metal", 4, top - 0.5F, 4, 12, top, 12).decal(Direction.UP, "connector");
    }

    /**
     * Textures for one of the redesigned machines: the family's shared ones, the microreactor's
     * light strip, and the rest from the machine's own folder (its art/tools script).
     */
    private java.util.function.Function<String, ResourceLocation> machineParts(String machine) {
        return name -> switch (name) {
            case "glow", "glow_off" -> modLoc("block/microreactor/" + name);
            // lpanel_WxH: the family's designed panels, drawn at the size of the face they cover.
            default -> FAMILY.contains(name) || name.startsWith("lpanel_") ? modLoc("block/machine/family/" + name)
                    : modLoc("block/machine/" + machine + "/" + name);
        };
    }

    /** Our shared material and decal textures, by short name. */
    private ResourceLocation machineTexture(String name) {
        return switch (name) {
            case "body" -> modLoc("block/machine/" + name);
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
     * Core Cracker, facing north: a jaw crusher, as used to crack hard ceramics. A heavy ribbed
     * frame on the family skid, a stepped feed hopper on top, a spoked flywheel either side with an
     * orange hub (the flywheel stores the energy that drives the jaws through each stroke), a panel
     * with the lit screen and a light strip on the front, and a discharge chute below it where the
     * cracked kernels drop out. Each flywheel is three boxes a little apart in depth, so it reads
     * round and no two faces share a plane. Its own textures: art/tools/core_cracker_textures.py.
     */
    private ModelFile coreCrackerModel(String name, boolean active) {
        BoxModel boxes = new BoxModel();
        boxes.add("skid", 0, 0, 0, 16, 2, 16);
        // The cast frame: each side a designed panel drawn at its size (10 x 9), not a tile.
        boxes.add("crusher", 3, 2, 3, 13, 11, 13).decal(Direction.NORTH, "frame_side").decal(Direction.SOUTH, "frame_side")
                .decal(Direction.WEST, "frame_side").decal(Direction.EAST, "frame_side");
        // The flywheels on both flanks are a model of their own (coreCrackerFlywheel), which the
        // machine's renderer turns while it works; their axle caps stay here.
        boxes.add("accent", 0.6F, 6.75F, 7.25F, 1, 8.25F, 8.75F);
        boxes.add("accent", 15, 6.75F, 7.25F, 15.4F, 8.25F, 8.75F);
        // Front: the panel with its screen and light strip, and the chute below.
        boxes.add("cabinet", 3.25F, 5.5F, 2.5F, 12.75F, 9, 3);
        boxes.add("cabinet", 3.5F, 6.5F, 2.25F, 12.5F, 8.5F, 2.5F).decal(Direction.NORTH, "screen").glow(Direction.NORTH);
        lampStrip(boxes, active, 3.5F, 9.5F, 2.75F, 12.5F, 10, 3);
        boxes.add("metal", 5, 2, 1.5F, 11, 5, 3).decal(Direction.NORTH, "chute");
        boxes.add("accent", 5, 4.75F, 1.25F, 11, 5.25F, 1.5F);
        // The feed hopper, widening upwards.
        boxes.add("bright", 4, 11, 4, 12, 12.5F, 12);
        boxes.add("bright", 3, 12.5F, 3, 13, 14, 13);
        // The hopper's mouth rises to the top of the block, where a pipe from above meets it.
        boxes.add("bright", 2.5F, 14, 2.5F, 13.5F, 16, 13.5F).face(Direction.UP, "hopper_mouth");
        backConnector(boxes, 13);
        BlockModelBuilder model = boxModel(name);
        boxes.build(model, "crusher", machineParts("core_cracker"));
        return model;
    }

    /**
     * The Core Cracker's two flywheels, where they sit on its flanks, as a model of their own: the
     * machine's renderer turns it about their axle (y 7.5, z 8) while it works. Each wheel is three
     * overlapping boxes, rounded, each face a shade inset from the next so none share a plane.
     */
    private void coreCrackerFlywheel() {
        BoxModel boxes = new BoxModel();
        float[][] wheel = {{5, 11, 2.5F, 12.5F}, {4, 12, 3.5F, 11.5F}, {3, 13, 4.5F, 10.5F}};
        for (int i = 0; i < wheel.length; i++) {
            float inset = i * 0.1F;
            boxes.add("flywheel", 1 + inset, wheel[i][2], wheel[i][0], 2.5F + inset, wheel[i][3], wheel[i][1]);
            boxes.add("flywheel", 13.5F - inset, wheel[i][2], wheel[i][0], 15 - inset, wheel[i][3], wheel[i][1]);
        }
        boxes.build(boxModel("core_cracker_flywheel"), "flywheel", machineParts("core_cracker"));
    }

    private void coreCracker() {
        coreCrackerFlywheel();
        Block block = ModBlocks.CORE_CRACKER.get();
        ModelFile off = coreCrackerModel("core_cracker", false);
        ModelFile on = coreCrackerModel("core_cracker_on", true);
        horizontalBlock(block, state -> state.getValue(ProcessingBlock.ACTIVE) ? on : off);
        simpleBlockItem(block, off);
    }

    /**
     * Reprocessor, two high, facing north: a rounded welded dissolver vessel on a hazard-striped
     * skid (it handles hot, radioactive fuel, so it earns the stripes), with a sight glass onto the
     * solution that glows teal with rising bubbles while it works, and a light strip on its cap.
     * Above it the lead-lined separation column with bright rings, a hazard band and label plate, a copper
     * line up the east flank from the vessel, a lid, and connectors on the cap and the back. Rounded shapes are two boxes of different
     * heights, so no faces share a plane. No ports drawn: pipes and cables connect on any side. One
     * 32-high design, cut per block. Its own textures: art/tools/reprocessor_textures.py.
     */
    private BoxModel reprocessorBoxes(boolean active) {
        BoxModel boxes = new BoxModel();
        boxes.add("skid", 0, 0, 0, 16, 2, 16).sides("hazard");
        // The dissolver vessel.
        boxes.add("vessel", 2, 2, 3, 14, 13, 13);
        boxes.add("vessel", 3, 2, 2, 13, 12.5F, 14);
        boxes.add("metal", 6.5F, 2.25F, 1.9F, 9.5F, 12.75F, 2);
        BoxModel.Box sight = boxes.add("metal", 7, 2.5F, 1.75F, 9, 12.5F, 2).decal(Direction.NORTH, active ? "sight_on" : "sight");
        if (active) {
            sight.glow(Direction.NORTH);
        }
        boxes.add("metal", 2.5F, 13, 2.5F, 13.5F, 14, 13.5F);
        lampStrip(boxes, active, 3, 13.25F, 2.25F, 13, 13.75F, 2.5F);
        // The column.
        boxes.add("column", 4, 14, 3, 12, 30, 13);
        boxes.add("column", 3, 14, 4, 13, 29.5F, 12);
        for (float y : new float[] {17, 28}) {
            boxes.add("bright", 2.75F, y, 2.75F, 13.25F, y + 0.75F, 13.25F);
        }
        // Marked as process plants mark their vessels: a hazard band round the column and a label
        // plate below it, by shape and colour. A radiation trefoil cannot be drawn well this small;
        // multiblocks, with room for an authentic one, may carry it.
        boxes.add("hazard", 2.9F, 22, 2.9F, 13.1F, 24, 13.1F);
        boxes.add("column", 5, 18.5F, 2.85F, 11, 21.5F, 3).decal(Direction.NORTH, "label");
        // Copper line up the east flank (the back is the connector's), from the vessel into the
        // column: a flange on the vessel, a riser standing clear of the rings and the band, and a
        // flanged stub into the column.
        boxes.add("bright", 14, 9.25F, 7, 14.5F, 11.25F, 9);
        boxes.add("copper", 14.25F, 9.5F, 7.25F, 15.75F, 26.5F, 8.75F);
        boxes.add("copper", 13, 25, 7.25F, 14.25F, 26.5F, 8.75F);
        boxes.add("bright", 12.9F, 24.75F, 7, 13.4F, 26.75F, 9);
        boxes.add("lid", 3.5F, 30, 3.5F, 12.5F, 31, 12.5F);
        topConnector(boxes, 31, 32);
        backConnector(boxes, 14);
        return boxes;
    }

    private void reprocessor() {
        Block block = ModBlocks.REPROCESSOR.get();
        ModelFile[][] halves = new ModelFile[2][2];
        for (int lit = 0; lit < 2; lit++) {
            for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
                BlockModelBuilder model = boxModel("reprocessor_" + half.getSerializedName() + (lit == 1 ? "_on" : ""));
                reprocessorBoxes(lit == 1).build(model, "vessel", machineParts("reprocessor"), half == DoubleBlockHalf.LOWER ? 0 : 16);
                halves[lit][half.ordinal()] = model;
            }
        }
        horizontalBlock(block, state -> halves[state.getValue(ProcessingBlock.ACTIVE) ? 1 : 0][state.getValue(TallProcessingBlock.HALF).ordinal()]);
        // The item shows the whole machine, scaled down to fit a slot.
        BlockModelBuilder item = models().getBuilder("reprocessor_item").parent(models().getExistingFile(mcLoc("block/block")));
        reprocessorBoxes(false).buildWhole(item, "vessel", machineParts("reprocessor"));
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
     * Fuel Fabricator, facing north: a pellet press and rod loading station, as fuel plants use. A
     * family cabinet with the lit screen and a light strip and a gunmetal worktop, and on it the
     * service frame (a cabinet across the back, the roof on posts): under the roof a hydraulic
     * press, two uprights with an orange hydraulic cylinder hanging from the roof between them and a
     * bright ram and platen over the bed with its pellet die, and at the front a loading tray with
     * two finished fuel rods and their orange end caps. Its own textures:
     * art/tools/fuel_fabricator_textures.py.
     */
    private ModelFile fuelFabricatorModel(String name, boolean active) {
        BoxModel boxes = new BoxModel();
        boxes.add("skid", 0, 0, 0, 16, 2, 16);
        boxes.add("cabinet", 1.5F, 2, 1.5F, 14.5F, 8, 15.5F).decal(Direction.NORTH, "lpanel_13x6")
                .decal(Direction.SOUTH, "lpanel_13x6").decal(Direction.WEST, "lpanel_14x6").decal(Direction.EAST, "lpanel_14x6");
        boxes.add("cabinet", 3.5F, 4.5F, 1.25F, 12.5F, 6.5F, 1.5F).decal(Direction.NORTH, "screen").glow(Direction.NORTH);
        lampStrip(boxes, active, 2, 7, 1.25F, 14, 7.5F, 1.5F);
        boxes.add("metal", 1, 8, 1, 15, 9, 15);
        serviceFrame(boxes, 11, 9, "lpanel_13x5", 9);
        // The press: its uprights carry the roof, the cylinder hangs from it.
        boxes.add("metal", 2.5F, 9, 6, 4.5F, 14, 10);
        boxes.add("metal", 11.5F, 9, 6, 13.5F, 14, 10);
        boxes.add("cylinder", 6, 11.5F, 6.5F, 10, 14, 9.5F);
        // The ram and platen press up and down while it works: a model of their own
        // (fuelFabricatorRam), moved by its renderer.
        boxes.add("metal", 5, 9, 5.5F, 11, 9.5F, 10.5F).face(Direction.UP, "bed");
        // The loading tray with two finished rods.
        boxes.add("metal", 3, 9, 1.5F, 13, 9.5F, 5);
        for (float z : new float[] {2, 3.25F}) {
            boxes.add("rod", 3.5F, 9.5F, z, 12.25F, 10.25F, z + 0.75F);
            boxes.add("accent", 12.25F, 9.5F, z, 12.75F, 10.25F, z + 0.75F);
        }
        BlockModelBuilder model = boxModel(name);
        boxes.build(model, "cabinet", machineParts("fuel_fabricator"));
        return model;
    }

    /**
     * Lithium Extractor, facing north: direct lithium extraction from brine, as real plants do it. A
     * brine basin on the family skid, white salt crusting its edge, in the service frame (a cabinet
     * across the back, the roof on posts standing on the basin's rim): under the roof two frosted
     * ion-exchange columns standing in the brine, whose sorbent beds glow cyan while brine runs
     * through them, and a small control box at the front with the lit screen and a light strip.
     * Rounded columns are two boxes of different heights, so no faces share a plane. Its own
     * textures: art/tools/lithium_extractor_textures.py.
     */
    private ModelFile lithiumExtractorModel(String name, boolean active) {
        BoxModel boxes = new BoxModel();
        boxes.add("skid", 0, 0, 0, 16, 2, 16);
        boxes.add("basin", 1.5F, 2, 1.5F, 14.5F, 5, 14.5F).face(Direction.UP, "brine");
        serviceFrame(boxes, 12, 2, "lpanel_13x12", 5);
        // Unlit here: while it works, its renderer lights the sorbent beds in a glow that rises up
        // the columns and falls back.
        String column = "column";
        for (float x : new float[] {5, 11}) {
            boxes.add(column, x - 2, 5, 7, x + 2, 14, 11);
            boxes.add(column, x - 1.5F, 5, 6.5F, x + 1.5F, 13.75F, 11.5F);
        }
        // Control box.
        boxes.add("cabinet", 3.5F, 5, 1.25F, 12.5F, 9, 4.5F);
        boxes.add("cabinet", 3.5F, 6.5F, 1, 12.5F, 8.5F, 1.25F).decal(Direction.NORTH, "screen").glow(Direction.NORTH);
        lampStrip(boxes, active, 4, 5.5F, 1, 12, 6, 1.25F);
        BlockModelBuilder model = boxModel(name);
        boxes.build(model, "basin", machineParts("lithium_extractor"));
        return model;
    }

    private void lithiumExtractor() {
        Block block = ModBlocks.LITHIUM_EXTRACTOR.get();
        ModelFile off = lithiumExtractorModel("lithium_extractor", false);
        ModelFile on = lithiumExtractorModel("lithium_extractor_on", true);
        horizontalBlock(block, state -> state.getValue(ProcessingBlock.ACTIVE) ? on : off);
        simpleBlockItem(block, off);
    }

    /**
     * Electrorefiner, facing north: pyroprocessing, a sealed molten salt cell as fast reactor fuel
     * plants plan it. The family cabinet with the lit screen and a light strip and a gunmetal
     * worktop, and on it the cell: a gunmetal vessel (two boxes of different heights, so it reads
     * as rounded), a hazard band round it, since it handles spent fuel, and at the front a small
     * sight window onto the salt, glowing orange while the current runs. Its lid carries four
     * electrodes in orange clamps at the corners and the top connector in the middle; the back
     * connector sits on the cabinet. Its own textures: art/tools/electrorefiner_textures.py.
     */
    private ModelFile electrorefinerModel(String name, boolean active) {
        BoxModel boxes = new BoxModel();
        boxes.add("skid", 0, 0, 0, 16, 2, 16);
        boxes.add("cabinet", 1.5F, 2, 1.5F, 14.5F, 8, 15.5F).decal(Direction.NORTH, "lpanel_13x6")
                .decal(Direction.SOUTH, "lpanel_13x6").decal(Direction.WEST, "lpanel_14x6").decal(Direction.EAST, "lpanel_14x6");
        boxes.add("cabinet", 3.5F, 4.5F, 1.25F, 12.5F, 6.5F, 1.5F).decal(Direction.NORTH, "screen").glow(Direction.NORTH);
        lampStrip(boxes, active, 2, 7, 1.25F, 14, 7.5F, 1.5F);
        boxes.add("metal", 1, 8, 1, 15, 9, 15);
        // The cell.
        boxes.add("cell", 3.5F, 9, 3.5F, 12.5F, 14.5F, 12.5F);
        boxes.add("cell", 3, 9, 4, 13, 14.25F, 12);
        boxes.add("hazard", 2.9F, 12.5F, 3.4F, 13.1F, 13.5F, 12.6F);
        BoxModel.Box window = boxes.add("cell", 5, 10, 3.35F, 11, 12, 3.5F).decal(Direction.NORTH, active ? "window_on" : "window");
        if (active) {
            window.glow(Direction.NORTH);
        }
        boxes.add("lid", 3, 14.5F, 3, 13, 15.25F, 13);
        // Electrodes down through the lid, clamped at the corners, clear of the connector's flange.
        for (float x : new float[] {3.25F, 12}) {
            for (float z : new float[] {3.25F, 12}) {
                boxes.add("bright", x, 15.25F, z, x + 0.75F, 16, z + 0.75F);
                boxes.add("accent", x - 0.1F, 15.25F, z - 0.1F, x + 0.85F, 15.6F, z + 0.85F);
            }
        }
        topConnector(boxes, 15.25F, 16);
        backConnector(boxes, 15.5F, 6);
        BlockModelBuilder model = boxModel(name);
        boxes.build(model, "cell", machineParts("electrorefiner"));
        return model;
    }

    private void electrorefiner() {
        Block block = ModBlocks.ELECTROREFINER.get();
        ModelFile off = electrorefinerModel("electrorefiner", false);
        ModelFile on = electrorefinerModel("electrorefiner_on", true);
        horizontalBlock(block, state -> state.getValue(ProcessingBlock.ACTIVE) ? on : off);
        simpleBlockItem(block, off);
    }

    /**
     * The Fuel Fabricator's ram and platen, at the bottom of their stroke (the platen on the bed):
     * its renderer lifts them 1.5 pixels to rest and presses them down in turn while it works. The
     * ram runs up into the cylinder, so it never shows a gap.
     */
    private void fuelFabricatorRam() {
        BoxModel boxes = new BoxModel();
        boxes.add("bright", 7, 10, 7, 9, 13.5F, 9);
        boxes.add("bright", 6.5F, 9.5F, 7, 9.5F, 10, 9);
        boxes.build(boxModel("fuel_fabricator_ram"), "bright", machineParts("fuel_fabricator"));
    }

    private void fuelFabricator() {
        fuelFabricatorRam();
        Block block = ModBlocks.FUEL_FABRICATOR.get();
        ModelFile off = fuelFabricatorModel("fuel_fabricator", false);
        ModelFile on = fuelFabricatorModel("fuel_fabricator_on", true);
        horizontalBlock(block, state -> state.getValue(ProcessingBlock.ACTIVE) ? on : off);
        simpleBlockItem(block, off);
    }

    /**
     * Waste Cask: a light steel drum, rounded with overlapping boxes, with graphite rings (the
     * middle one hazard striped), a lid on top, and a four-segment gauge down
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
        ModelFile off = alloySmelterModel("alloy_smelter", false);
        ModelFile on = alloySmelterModel("alloy_smelter_on", true);
        horizontalBlock(block, state -> state.getValue(AlloySmelterBlock.LIT) ? on : off);
        simpleBlockItem(block, off);
    }

    /**
     * Alloy Smelter, facing north: tier 1, before any power, so a small fuel-fired furnace as early
     * blast furnaces were. A firebrick hearth on the family skid, bound with two steel bands, a fire
     * door that glows while it burns with an orange latch above it, and a stepped gunmetal hood rising
     * to a copper flue with a ring round it. The only brick machine, so it reads as the earliest.
     * Its own textures: art/tools/alloy_smelter_textures.py.
     */
    private ModelFile alloySmelterModel(String name, boolean lit) {
        BoxModel boxes = new BoxModel();
        boxes.add("skid", 0, 0, 0, 16, 2, 16);
        boxes.add("firebrick", 2, 2, 2, 14, 11, 14);
        for (float y : new float[] {4, 8}) {
            boxes.add("metal", 1.75F, y, 1.75F, 14.25F, y + 1, 14.25F);
        }
        BoxModel.Box door = boxes.add("metal", 4.5F, 3.5F, 1.5F, 11.5F, 7.5F, 2).decal(Direction.NORTH, lit ? "fire_door_on" : "fire_door");
        if (lit) {
            door.glow(Direction.NORTH);
        }
        boxes.add("accent", 7, 7.75F, 1.25F, 9, 8.25F, 1.75F);
        boxes.add("metal", 2.5F, 11, 2.5F, 13.5F, 12, 13.5F);
        boxes.add("metal", 4, 12, 4, 12, 13, 12);
        boxes.add("copper", 6, 13, 6, 10, 16, 10);
        boxes.add("metal", 5.5F, 14.5F, 5.5F, 10.5F, 15, 10.5F);
        backConnector(boxes, 14.25F);
        BlockModelBuilder model = boxModel(name);
        boxes.build(model, "firebrick", machineParts("alloy_smelter"));
        return model;
    }
}
