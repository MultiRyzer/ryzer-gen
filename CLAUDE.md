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

On this machine Gradle fails with "Unable to establish loopback connection" because Java cannot create its internal socket in the default temp folder. Prefix Gradle commands with `JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:\jtmp'` (the folder `C:\jtmp` must exist), and run them outside the sandbox.

- `./gradlew runData` runs datagen into `src/generated/resources/`. Commit the generated files. Rerun it after changing anything under `datagen/`.

Registration uses `DeferredRegister` in `com.ryzer.ryzergen.registry`, wired up in `RyzerGen`. Every new item goes in the Ryzer Gen creative tab automatically. Lang entries are hand-written in `src/main/resources/assets/ryzergen/lang/en_us.json`. Models, blockstates, loot tables, tags, recipes and worldgen come from datagen (`com.ryzer.ryzergen.datagen`), not hand-written JSON.

Ores are driven by the `OreType` enum: adding an entry there gives stone and deepslate ore blocks, drops, ingots, tags, loot and smelting. Vein size and rarity live in `ModWorldGenProvider`. Each ore has a worldgen toggle in the common config (`ryzergen-common.toml`), applied by the `ryzergen:configurable_ore` biome modifier. IDs and names use British "aluminium", but common tags use "aluminum" because that is what other mods use.

## Writing rules

- **Never use em dashes**, in docs, code comments, lang strings, commit messages or chat. Use a colon, comma, full stop or brackets instead.
- Plain, short sentences. British spelling where it already appears (aluminium, stabilised).
- All names, models and textures are original. Real-world products and projects can inspire things and be credited on the mod page, but never become item names.

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
