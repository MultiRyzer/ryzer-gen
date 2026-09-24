package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.cable.CableSide;
import com.ryzer.ryzergen.cable.EnergyCableBlock;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterBlock;
import com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterBlock;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorPartBlock;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorSlot;
import com.ryzer.ryzergen.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.EnumMap;
import java.util.Map;

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
        energyCable();
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
    private void energyCable() {
        Block block = ModBlocks.ENERGY_CABLE.get();
        ResourceLocation cable = modLoc("block/energy_cable");
        ResourceLocation coreTexture = modLoc("block/energy_cable_core");
        ResourceLocation flange = modLoc("block/energy_cable_flange");
        // The core is a plain junction box. Arms map the cable texture's stripe band (rows 5 to 10)
        // along their length with explicit UVs, so one continuous stripe runs the whole way.
        ModelFile core = models().getBuilder("energy_cable_core").texture("particle", cable).texture("core", coreTexture)
                .element().from(5, 5, 5).to(11, 11, 11).textureAll("#core").end();
        ModelFile arm = models().getBuilder("energy_cable_arm").texture("particle", cable)
                .texture("cable", cable).texture("core", coreTexture)
                .element().from(5, 5, 0).to(11, 11, 5)
                .face(Direction.NORTH).texture("#core").uvs(5, 5, 11, 11).end()
                .face(Direction.EAST).texture("#cable").uvs(0, 5, 5, 11).end()
                .face(Direction.WEST).texture("#cable").uvs(0, 5, 5, 11).end()
                .face(Direction.UP).texture("#cable").uvs(0, 5, 5, 11).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
                .face(Direction.DOWN).texture("#cable").uvs(0, 5, 5, 11).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
                .end();
        ModelFile plate = models().getBuilder("energy_cable_flange").texture("particle", cable).texture("flange", flange)
                .element().from(4, 4, 0).to(12, 12, 1).textureAll("#flange").end();
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block).part().modelFile(core).addModel().end();
        for (Direction dir : Direction.values()) {
            int x = dir == Direction.UP ? 270 : dir == Direction.DOWN ? 90 : 0;
            int y = dir.getAxis().isHorizontal() ? ((int) dir.toYRot() + 180) % 360 : 0;
            builder.part().modelFile(arm).rotationX(x).rotationY(y).addModel()
                    .condition(EnergyCableBlock.SIDES.get(dir), CableSide.CONNECTED, CableSide.EXTRACT).end();
            builder.part().modelFile(plate).rotationX(x).rotationY(y).addModel()
                    .condition(EnergyCableBlock.SIDES.get(dir), CableSide.EXTRACT).end();
        }
        itemModels().withExistingParent("energy_cable", mcLoc("item/generated")).texture("layer0", modLoc("item/energy_cable"));
    }

    /**
     * Electric alloy smelter: a graphite machine inside a light steel frame, with depth in the
     * Mekanism spirit. Trim base and cap, steel corner pillars, the body set back a pixel between
     * them, a raised window onto the heating coil (glowing when working), vents on the sides and
     * the energy port on the back. Built facing north and turned into place.
     */
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
