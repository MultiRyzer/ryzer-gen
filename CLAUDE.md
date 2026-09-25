# Ryzer Gen

NeoForge mod for Minecraft 1.21.1 (the version ATM10 runs on). Mod ID `ryzergen`, package `com.ryzer.ryzergen`. Power progression through physics: portable microreactor, fuel cycle, fission, breeder, fusion, then a Dyson swarm around a walkable sun.

## Where things live

- `docs/DESIGN.md`: the full design. Source of truth for how anything should work.
- `docs/ROADMAP.md`: milestones in build order. Tick boxes as work lands.
- `docs/OPEN-QUESTIONS.md`: undecided items. Once settled, move them into DESIGN.md.
- `mod/`: the Gradle project (NeoForge MDK). Run Gradle commands from here.
- `art/textures/`: finished 16x16 textures, copied into `mod/src/main/resources/assets/ryzergen/textures/`.
- `art/concepts/`, `reference/`: sketches and research, not shipped.

## Build and run

From `mod/` (Java 21 required):

- `./gradlew runClient` launches a test client with the mod loaded.
- `./gradlew build` compiles and packages the jar.
- `./gradlew runData` runs datagen into `src/generated/resources/`. Commit the generated files. Rerun it after changing anything under `datagen/`.

The dev client also loads a recipe viewer and Accessories (ring slot for the dosimeter) through `localRuntime` in `build.gradle`, versions in `gradle.properties`. The viewer is JEI (what ATM10 ships) or EMI, picked by `dev_recipe_viewer`; we have plugins for both, and running both would show recipes twice. None of these is ever a dependency of the published mod: compat code lives in `compat/<mod>` and only touches the other mod after checking it is loaded (or, for EMI and JEI, through their own plugin annotations).

Shared machine pieces: `machine/MachineEnergyStorage` (receive-only FE buffer), `machine/RedstoneMode`, `client/GuiGauges` (lit and fluid gauges), and in the art tools `part_frame` for block faces. Simple processing machines (items and an optional fluid in, results out, FE per tick) share `machine/processing`: add an entry to `ProcessingMachine` and a `MachineRecipe` process rather than writing a new block entity, menu and screen; its GUI comes from `processing()` in `gui_textures.py`. Cables and pipes live in `cable/`: `CableBlock` and `CableBlockEntity` hold everything shared (connections, wrench, network scan, stats panel), and `EnergyCable*` and `ItemPipe*` add what they carry. Each side can hold a fitting (`CableUpgrade`) that multiplies its input limit; `client/FittedCableModel` adds the fitting models from the block entity's model data, so never put fittings in the block state. Radiation lives in `radiation/`.

On this machine Gradle fails with "Unable to establish loopback connection" because Java cannot create its internal socket in the default temp folder. Prefix Gradle commands with `JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:\jtmp'` (the folder `C:\jtmp` must exist), and run them outside the sandbox.

Registration uses `DeferredRegister` in `com.ryzer.ryzergen.registry`, wired up in `RyzerGen`. Every new item goes in the Ryzer Gen creative tab automatically. Lang entries are hand-written in `src/main/resources/assets/ryzergen/lang/en_us.json`. Models, blockstates, loot tables, tags, recipes and worldgen come from datagen (`com.ryzer.ryzergen.datagen`), not hand-written JSON.

Machines live in `com.ryzer.ryzergen.machine.<name>` (block, block entity, menu, screen). The alloy smelter is the template: `ItemStackHandler` inventory exposed through the item handler capability, `ContainerData` for progress, a custom recipe type in `com.ryzer.ryzergen.recipe`. GUI textures go in `textures/gui/`. Tier 1 machines burn furnace fuel, because nothing is powered before the microreactor.

Ores are driven by the `OreType` enum: adding an entry there gives stone and deepslate ore blocks, drops, ingots, tags, loot and smelting. Vein size and rarity live in `ModWorldGenProvider`. Each ore has a worldgen toggle in the common config (`ryzergen-common.toml`), applied by the `ryzergen:configurable_ore` biome modifier. IDs and names use British "aluminium", but common tags use "aluminum" because that is what other mods use.

## Writing rules

- **Never use em dashes**, in docs, code comments, lang strings, commit messages or chat. Use a colon, comma, full stop or brackets instead.
- Plain, short sentences. British spelling where it already appears (aluminium, stabilised).
- All names and models are original, and so are machine textures. Material textures (ores, raw drops, ingots, gems) follow the common modding practice of building on vanilla: ores draw our own mineral clusters over vanilla stone and deepslate, and items gradient-map vanilla shapes onto our colours (`material_textures.py`, which reads vanilla from the Minecraft jar at run time; never copy vanilla files into the repo). Real-world products and projects can inspire things and be credited on the mod page, but never become item names.

## Design rules

These come from `docs/DESIGN.md`. Check any new feature against them.

1. **Progression through knowledge, not volume.** A small, well-designed machine should beat a big sloppy one.
2. **Somewhat accurate.** Real physics where we can, honest fudges where we must, never pure fantasy. Note the real-world basis for new mechanics.
3. **No grind.** Structure blocks are cheap; cost lives in the interesting parts. Tiers unlock through milestones, not resource piles. Any repeated manual action gets automation soon after the player understands it.
4. **Every tier stays useful.** Earlier machines are stepping stones, not junk.
5. **Pack friendly.** FE energy, data-driven recipes and worldgen, config toggles for balance and anything disruptive, good performance. Built to earn a place in packs like ATM10.
6. **No compression recipes.** A higher tier never needs a stack of the tier below. It needs different materials made by a new process.
7. **Power gating.** The machine that makes the next tier's key material runs on the current tier's power.
8. **Common tags everywhere.** Every ore and material uses `c:` tags (`c:ingots/uranium`, `c:dusts/lead`, ...) and every recipe asks for the tag, never our own item. Each ore's worldgen has a config toggle. The mod ships every ore it uses so it plays standalone.
9. **Optional dependencies stay optional.** Curios, Mekanism, EMI/JEI integrations must never be required to load or play.
10. **Radiation comes only from running reactors and meltdowns.** Ores, fuel, depleted cores and waste are safe to carry. Effects escalate slowly and never instantly kill outside a meltdown. On by default, config toggle to turn off.
11. **Upgrade, don't replace.** Batteries grow by adding modules and better chemistries; the dosimeter ring gains new linings. Avoid one block recoloured per tier.

## Art style

"Oritech-lite": chunky industrial machines with visible pipes, panels and chutes, built from standard JSON block models (Blockbench boxes) and 16x16 textures. Multiblocks swap to one combined model when assembled. Keep animation light: particles, glowing textures, the odd moving part.

No GeckoLib (it would be a hard dependency). A formed multiblock is designed as one list of boxes in datagen (see `MicroreactorModel`), which is cut into one model per block so lighting and culling stay normal. Boxes use small tiling material textures (mapped by position) plus decals at one texel per pixel. Future moving parts go on a block entity renderer. A big static body drawn by a renderer (like the fission station's) goes in a GPU mesh built once (`client/StationMesh`), with the plain draw as a fallback while a shader pack is on.

Textures are drawn in Python under `art/tools/` (no PIL needed). `pixelart.py` holds the shared palette, canvas and house-style helpers; each machine or GUI gets its own script that imports it and calls `publish()`. After `runData`, `python art/tools/model_preview.py --scale 14 OUT.png MODEL@dx,dy,dz ...` renders the generated models isometrically, which is the quickest way to check a model before launching the game.

House style, set by the microreactor (carry it to every new machine):
- Clean and modern (Applied Energistics or Oritech, not Tekkit): flat, noise-free panels with crisp one-pixel bevels. No rivets, grime or scuffs. Use the modern ramp in `pixelart.py` (casing greys A to J, graphite b to U, gunmetal s to z, cyan f/i/j, orange X/Z).
- Materials: light grey casing with a shallow inset, graphite trim for skids and rings, mid gunmetal for reactor vessels, clean copper for pipes and fins, orange accent for small hardware (clips, lugs), yellow on graphite hazard stripes on bases. Each tile is one panel, so every block face reads as a neat plate.
- Light strips: thin cyan strips set into rings and roof lines, emissive while the machine runs and dim when it is off. Screens are always emissive; portholes glow only while running.
- Ports: a 10x10 flange flush with the block face around an 8x8 socket, centred. Ring colour: red energy, blue coolant/fluid, white steam, orange items. Capabilities only on the port face, only while formed.
- GUI text must fit: after changing any screen or its lang strings, run `python art/tools/ui_text_check.py` (it measures each line with the game's own font against the space it has, using worst-case values) and add checks for new screens. Do this before every push.
- Machines with a facing turn with the wrench. `WrenchItem` turns any of our blocks with a horizontal `FACING` (both halves of two-high ones), so give new machines that property rather than special-casing them. Multiblock parts are the exception.
- Pipe items show the pipe itself (a straight 3D run), not a flat icon; pipe art comes from the `PIPES` family in `machine_textures.py`.
- GUIs (`gui_textures.py`): light casing panel with cut corners and a crisp bevel, a graphite machine bay, near-black gauge wells, a dark readout screen with cyan text and row icons, graphite buttons, a cyan light line above the player inventory. Gauge fills are drawn in code (lit gradients; fluids use their own animated texture), never pixel art. Power and redstone buttons use the shared `RedstoneMode`. Labels in dark graphite, values in cyan, status in green, amber, orange or red.
- Buttons: 3D keycaps (lit top edge, shaded face, dark front lip). Power glows green with a soft halo when on, dim red when off. Redstone buttons render vanilla items (gunpowder ignored, redstone high, redstone torch low), as Mekanism does.
- Audio: our own sound events, so packs can replace them. The user sources recordings (Pixabay and similar) into `art/sounds/source/`; `machine_sounds.py` (needs numpy and soundfile) cuts each into a seamless loop, levels it and ships it as mono Ogg Vorbis in `assets/ryzergen/sounds/`. Claude cannot hear audio, so the user judges sounds by ear in game. Avoid short sounds on a fast server-side repeat; loop a clip on the client instead and bend its pitch or volume to show what the machine is doing. Record every source in `art/sounds/CREDITS.md` for the mod page.
