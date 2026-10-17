package com.ryzer.ryzergen.showcase;

import com.ryzer.ryzergen.battery.container.ContainerLayout;
import com.ryzer.ryzergen.machine.assembler.AssemblerLayout;
import com.ryzer.ryzergen.machine.breeder.BreederLayout;
import com.ryzer.ryzergen.machine.breeder.BreederPart;
import com.ryzer.ryzergen.machine.drill.MegaDrillLayout;
import com.ryzer.ryzergen.machine.fission.StationLayout;
import com.ryzer.ryzergen.machine.fission.StationPart;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorSlot;
import com.ryzer.ryzergen.machine.pool.PoolLayout;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The press kit's showcase site: every machine built and formed in one row, in the order a player
 * meets them, fronts facing the player, each with a sign. A second fission station stands apart as
 * the meltdown range. Machines are placed part by part in the order the game tests use, so each one
 * forms by its own rules (nothing here sets a formed state). Dev only, through {@link ShowcaseCommand}.
 */
public final class Showcase {
    /** Every machine faces north, towards the player, who stands north of the row. */
    static final Direction FACING = Direction.NORTH;
    /** Blocks between machines, and from the player to the fronts. */
    private static final int GAP = 4;
    private static final int SETBACK = 8;
    /** Clear air above the site, enough for the Mega Drill and the station's roof. */
    private static final int CLEAR = 18;
    /** Stone under the Mega Drill, for it to bore into. */
    private static final int BEDROCK_DEPTH = 24;
    /** How far east of the row the meltdown range's station stands. */
    private static final int RANGE = 48;

    private Showcase() {}

    /** One machine: its sign's two lines and how to lay out its blocks round a local origin. */
    public record Machine(String name, String note, Supplier<Map<BlockPos, BlockState>> blocks) {
        public Map<BlockPos, BlockState> layout() {
            return blocks.get();
        }
    }

    /** The row, in progression order. */
    public static final List<Machine> ROW = List.of(
            single("Alloy Smelter", "Tier 1: steel", ModBlocks.ALLOY_SMELTER.get()),
            new Machine("Microreactor", "Tier 1: first power", Showcase::microreactor),
            single("Intake Pump", "Water in", ModBlocks.INTAKE_PUMP.get()),
            single("Core Cracker", "Fuel cycle 1", ModBlocks.CORE_CRACKER.get()),
            single("Reprocessor", "Fuel cycle 2", ModBlocks.REPROCESSOR.get()),
            single("Fuel Fabricator", "Fuel cycle 3", ModBlocks.FUEL_FABRICATOR.get()),
            single("Waste Cask", "Fuel cycle: waste", ModBlocks.WASTE_CASK.get()),
            single("Electric Alloy", "Smelter", ModBlocks.ELECTRIC_ALLOY_SMELTER.get()),
            single("Lithium", "Extractor", ModBlocks.LITHIUM_EXTRACTOR.get()),
            single("Electrorefiner", "Bridge to fusion", ModBlocks.ELECTROREFINER.get()),
            single("Monazite", "Processor", ModBlocks.MONAZITE_PROCESSOR.get()),
            new Machine("Container Battery", "Storage", Showcase::container),
            new Machine("Cooling Pool", "Hot fuel", Showcase::pool),
            new Machine("Assembler", "Auto crafting", Showcase::assembler),
            new Machine("Fission Station", "Tier 3, 5 bay reserve", () -> station(true)),
            new Machine("Breeder Reactor", "Tier 4", Showcase::breeder),
            new Machine("Mega Drill", "Bores a layer at a time", Showcase::drill));

    /** The meltdown range: a station with no reserve, on its own pad. */
    public static final Machine MELTDOWN = new Machine("Meltdown Range", "Safeties off at your risk", () -> station(false));

    /** Builds the whole site with the player standing at {@code player}; returns a line per machine. */
    public static List<String> build(ServerLevel level, BlockPos player) {
        List<String> report = new ArrayList<>();
        int x = player.getX();
        int front = player.getZ() + SETBACK;
        int floor = player.getY() - 1;
        List<Placed> placed = new ArrayList<>();
        for (Machine machine : ROW) {
            Placed p = Placed.at(machine, x, floor + 1, front);
            placed.add(p);
            x = p.maxX + 1 + GAP;
        }
        Placed range = Placed.at(MELTDOWN, x + RANGE, floor + 1, front);

        int minX = player.getX() - GAP;
        int maxX = x;
        int maxZ = placed.stream().mapToInt(p -> p.maxZ).max().orElse(front) + GAP;
        pad(level, minX, maxX, player.getZ() - GAP, maxZ, floor);
        pad(level, range.minX - GAP * 3, range.maxX + GAP * 3, player.getZ() - GAP, range.maxZ + GAP * 3, floor);
        for (Placed p : placed) {
            if (p.machine.name().equals("Mega Drill")) {
                fill(level, p.minX - 1, p.maxX + 1, floor - BEDROCK_DEPTH, floor - 1, p.minZ - 1, p.maxZ + 1, Blocks.STONE.defaultBlockState());
            }
        }
        for (Placed p : placed) {
            report.add(p.place(level));
        }
        report.add(range.place(level));
        supplies(level, player.offset(0, 0, 2));
        return report;
    }

    /** Places one machine with its local origin at {@code at}, for the game tests. */
    public static void placeAlone(ServerLevel level, Machine machine, BlockPos at) {
        for (Map.Entry<BlockPos, BlockState> e : machine.layout().entrySet()) {
            set(level, at.offset(e.getKey()), e.getValue());
        }
    }

    /** A machine's place in the site: its layout shifted so its west edge and its front land where asked. */
    private record Placed(Machine machine, BlockPos offset, int minX, int maxX, int minZ, int maxZ) {
        static Placed at(Machine machine, int west, int y, int front) {
            Map<BlockPos, BlockState> layout = machine.layout();
            int lx = Integer.MAX_VALUE, hx = Integer.MIN_VALUE, lz = Integer.MAX_VALUE, hz = Integer.MIN_VALUE;
            for (BlockPos pos : layout.keySet()) {
                lx = Math.min(lx, pos.getX());
                hx = Math.max(hx, pos.getX());
                lz = Math.min(lz, pos.getZ());
                hz = Math.max(hz, pos.getZ());
            }
            BlockPos offset = new BlockPos(west - lx, y, front - lz);
            return new Placed(machine, offset, west, west + hx - lx, front, front + hz - lz);
        }

        String place(ServerLevel level) {
            placeAlone(level, machine, offset);
            int centre = (minX + maxX) / 2;
            sign(level, new BlockPos(centre, offset.getY(), minZ - 2), machine.name(), machine.note());
            return machine.name() + " at " + centre + ", " + offset.getY() + ", " + minZ;
        }
    }

    // ---- The machines' layouts, round a local origin at the controller or core. ----

    private static Machine single(String name, String note, Block block) {
        return new Machine(name, note, () -> {
            Map<BlockPos, BlockState> out = new LinkedHashMap<>();
            out.put(BlockPos.ZERO, facing(block.defaultBlockState()));
            return out;
        });
    }

    private static Map<BlockPos, BlockState> microreactor() {
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        BlockPos origin = BlockPos.ZERO;
        // The heart in the lower front, the coolant jacket above it, the machine units behind:
        // the arrangement the build preview shows.
        out.put(MicroreactorSlot.LOWER_FRONT.fromOrigin(origin, FACING), facing(ModBlocks.REACTOR_HEART.get().defaultBlockState()));
        out.put(MicroreactorSlot.UPPER_FRONT.fromOrigin(origin, FACING), facing(ModBlocks.COOLANT_JACKET.get().defaultBlockState()));
        out.put(MicroreactorSlot.LOWER_BACK.fromOrigin(origin, FACING), facing(ModBlocks.REACTOR_MACHINE_UNIT.get().defaultBlockState()));
        out.put(MicroreactorSlot.UPPER_BACK.fromOrigin(origin, FACING), facing(ModBlocks.REACTOR_MACHINE_UNIT.get().defaultBlockState()));
        return out;
    }

    private static Map<BlockPos, BlockState> station(boolean reserve) {
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        BlockPos core = BlockPos.ZERO;
        out.put(core, facing(ModBlocks.STATION_CORE.get().defaultBlockState()));
        for (Map.Entry<BlockPos, StationPart> e : StationLayout.PARTS.entrySet()) {
            if (e.getValue() != StationPart.CORE) {
                out.put(StationLayout.toWorld(core, FACING, e.getKey()), e.getValue().block().defaultBlockState());
            }
        }
        if (reserve) {
            for (var ring : StationLayout.RESERVE) {
                for (BlockPos cell : ring) {
                    out.put(StationLayout.toWorld(core, FACING, cell), ModBlocks.COOLANT_RESERVE.get().defaultBlockState());
                }
            }
        }
        StationLayout.Port water = StationLayout.Port.WATER;
        out.put(StationLayout.portPos(core, FACING, water).relative(StationLayout.portFace(FACING, water)),
                ModBlocks.CREATIVE_WATER_TANK.get().defaultBlockState());
        return out;
    }

    private static Map<BlockPos, BlockState> breeder() {
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        BlockPos core = BlockPos.ZERO;
        out.put(core, facing(ModBlocks.BREEDER_CORE.get().defaultBlockState()));
        for (Map.Entry<BlockPos, BreederPart> e : BreederLayout.PARTS.entrySet()) {
            if (e.getValue() != BreederPart.CORE) {
                out.put(BreederLayout.toWorld(core, FACING, e.getKey()), e.getValue().block().defaultBlockState());
            }
        }
        BreederLayout.Port sodium = BreederLayout.Port.SODIUM;
        out.put(BreederLayout.portPos(core, FACING, sodium).relative(BreederLayout.portFace(FACING, sodium)),
                ModBlocks.CREATIVE_SODIUM_TANK.get().defaultBlockState());
        return out;
    }

    private static Map<BlockPos, BlockState> drill() {
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        BlockPos controller = BlockPos.ZERO;
        for (MegaDrillLayout.Cell cell : MegaDrillLayout.CELL_LIST) {
            if (!cell.pos().equals(MegaDrillLayout.CONTROLLER)) {
                out.put(MegaDrillLayout.toWorld(controller, FACING, cell.pos()), ModBlocks.DRILL_FRAME.get().defaultBlockState());
            }
        }
        // The controller last: placing it completes the drill.
        out.put(controller, facing(ModBlocks.MEGA_DRILL_CONTROLLER.get().defaultBlockState()));
        port(out, MegaDrillLayout.Port.ENERGY, ModBlocks.CREATIVE_BATTERY.get());
        port(out, MegaDrillLayout.Port.WATER, ModBlocks.CREATIVE_WATER_TANK.get());
        return out;
    }

    private static void port(Map<BlockPos, BlockState> out, MegaDrillLayout.Port port, Block supply) {
        out.put(MegaDrillLayout.toWorld(BlockPos.ZERO, FACING, port.cell()).relative(port.face(FACING)), supply.defaultBlockState());
    }

    private static Map<BlockPos, BlockState> assembler() {
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        BlockPos controller = BlockPos.ZERO;
        BlockState controllerState = null;
        for (int index = 1; index <= AssemblerLayout.CELLS; index++) {
            BlockPos cell = AssemblerLayout.cell(index);
            BlockState state = facing(AssemblerLayout.partAt(cell).block().defaultBlockState());
            if (cell.equals(AssemblerLayout.CONTROLLER)) {
                controllerState = state;
            } else {
                out.put(AssemblerLayout.toWorld(controller, FACING, cell), state);
            }
        }
        out.put(controller, controllerState);
        return out;
    }

    /** The pool, laid out as a player builds it: the controller on the ground, a liner above it. */
    private static Map<BlockPos, BlockState> pool() {
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        BlockPos controller = BlockPos.ZERO;
        BlockPos anchor = PoolLayout.anchor(false);
        BlockState controllerState = null;
        for (int index = 1; index <= PoolLayout.CELLS; index++) {
            BlockPos cell = PoolLayout.cell(index);
            BlockState state = facing(PoolLayout.partAt(cell, false).block().defaultBlockState());
            if (cell.equals(anchor)) {
                controllerState = state;
            } else {
                out.put(PoolLayout.toWorld(controller, anchor, FACING, cell), state);
            }
        }
        out.put(controller, controllerState);
        return out;
    }

    /** The container battery, built like the pool: the controller on the ground. */
    private static Map<BlockPos, BlockState> container() {
        Map<BlockPos, BlockState> out = new LinkedHashMap<>();
        BlockPos controller = BlockPos.ZERO;
        BlockPos anchor = ContainerLayout.anchor(false);
        BlockState controllerState = null;
        for (int index = 1; index <= ContainerLayout.CELLS; index++) {
            BlockPos cell = ContainerLayout.cell(index);
            BlockState state = facing(ContainerLayout.partAt(cell, false).block().defaultBlockState());
            if (cell.equals(anchor)) {
                controllerState = state;
            } else {
                out.put(ContainerLayout.toWorld(controller, anchor, FACING, cell), state);
            }
        }
        out.put(controller, controllerState);
        return out;
    }

    // ---- Placing. ----

    private static BlockState facing(BlockState state) {
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.setValue(BlockStateProperties.HORIZONTAL_FACING, FACING) : state;
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            level.setBlock(pos.above(), state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    /** A smooth stone floor with clear air above it. */
    private static void pad(ServerLevel level, int x0, int x1, int z0, int z1, int floor) {
        fill(level, x0, x1, floor, floor, z0, z1, Blocks.SMOOTH_STONE.defaultBlockState());
        fill(level, x0, x1, floor + 1, floor + CLEAR, z0, z1, Blocks.AIR.defaultBlockState());
    }

    private static void fill(ServerLevel level, int x0, int x1, int y0, int y1, int z0, int z1, BlockState state) {
        for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    private static void sign(ServerLevel level, BlockPos pos, String name, String note) {
        // Rotation 8 faces the text north, towards the player.
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = new SignText().setMessage(1, Component.literal(name)).setMessage(2, Component.literal(note));
            sign.setText(text, true);
            sign.setWaxed(true);
        }
    }

    /** A chest of what the machines burn, so a visitor can start any of them. */
    private static void supplies(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof Container chest) {
            int slot = 0;
            for (ItemStack stack : List.of(
                    stack(ModItems.SEALED_FUEL_CORE.get(), 4),
                    stack(ModItems.URANIUM_FUEL_ROD.get(), 64),
                    stack(ModItems.MOX_FUEL_ROD.get(), 32),
                    stack(ModItems.BREEDER_FUEL.get(), 32),
                    stack(ModItems.TUNGSTEN_TIP.get(), 16),
                    stack(ModItems.LFP_BATTERY_RACK.get(), 16),
                    stack(ModItems.SWARM_CONTROLLER.get(), 1))) {
                chest.setItem(slot++, stack);
            }
        }
    }

    private static ItemStack stack(ItemLike item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.setCount(Math.min(count, stack.getMaxStackSize()));
        return stack;
    }
}
