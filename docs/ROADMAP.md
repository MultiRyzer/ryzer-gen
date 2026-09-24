# Roadmap

Build in thin, playable slices. Each milestone should load in game and be tested before the next starts.

## Milestone 0: Setup
- [x] Install Java 21 JDK (IntelliJ IDEA optional)
- [x] Create the mod project from the NeoForge 1.21.1 template into `mod/`
- [x] Pick a mod name and mod ID (Ryzer Gen, `ryzergen`)
- [x] Confirm `runClient` launches a test world with the mod loaded
- [x] First commit to Git (optional: push to a private GitHub repo)

## Milestone 1: Microreactor (first playable)
- [x] Ores needed for tier 1 (uranium, lead) with worldgen, common tags and config toggles (all 8 ores done)
- [x] Alloy smelter (fuel burning, two inputs)
- [x] Steel and graphite recipes
- [x] Register reactor heart, reactor machine unit and coolant block
- [x] Multiblock detection for the 4-block layout (heart in any position)
- [x] Snap assembly: combined model, particles, sound (no motion animation yet)
- [x] Break any block to disassemble
- [x] Energy generation, fuel life, depletion into a depleted core
- [x] Simple heat model with coolant bonus
- [x] Control unit GUI (fuel, temperature, output, efficiency, energy, coolant, on/off, redstone, dump and safety controls)
- [x] Fixed connection chutes on the assembled model (energy and coolant connected, steam modelled only)
- [x] Ghost placement preview (outlines and suggested parts while holding a part)
- [x] Placeholder textures, then proper 16x16 art
  - [x] Ores, raw drops, fluorite, salt, ingots and steel rebuilt on vanilla bases (`art/tools/material_textures.py`)
  - [x] Microreactor parts, formed machine, GUI and fuel items in the modern house style
  - [x] Alloy smelter GUI in the house style
  - [x] Alloy smelter block faces (kept as they are)
  - [x] Dosimeter ring (tilted green band, fluorite chip) and wrench (steel head, orange collar, graphite grip) redrawn
- [x] Recipes and config values (first pass: board, heart, units, jacket, graphite 3 per block; play-testing)
- [x] Overdrive (safeties off, 45% efficiency) with coolant-loss meltdown, alarm and config toggle
- [x] Dump mode to keep burning fuel on a full buffer
- [x] EMI support: alloying recipes and tag names (optional, loads only with EMI)
- [x] JEI support: alloying recipes, smelter catalysts, click the arrow to see recipes (ATM10 ships JEI)
- [x] Basic radiation (dose, inverse-square falloff, shielding, effects, meltdown sites, config toggle)
- [x] Dosimeter ring with Accessories slot support and inventory fallback (HUD gauge, Geiger clicks, 25% protection)
- [x] Electric alloy smelter (FE, twice as fast, same recipes)
- [x] Advancements: a Ryzer Gen tab walking from the first ore to a running microreactor, ending on the depleted core
- [x] Fuel automation: one fuel hatch on top; fresh cores go in, the reactor pushes spent ones back out

## Milestone 1b: Home battery
- [x] Home battery stack cabinet with 6 module slots and segment model
- [x] Lead-acid module
- [x] Basic cables (Pipez style: auto-connect, wrench to extract or disconnect) and the wrench
  - [x] Cable panel: right-click a cable with an extract side (empty hand) to see each source, its current rate against the max, and how many machines it feeds
  - [x] Batteries act as network buffers: machines first, surplus into storage, shortfalls drawn back out
- [x] Item pipes (the cable's twin: auto-connect, wrench to extract or disconnect, stats panel, round-robin delivery, 8 items/s per extract side)

## Milestone 1c: Water, steam and pipes
- [x] Intake pump (one water source block below, 10 FE/t, 100 mB/t out of the top port)
- [x] Steam (a gaseous fluid, `c:steam`) from the microreactor's steam port, 10 mB per mB of water boiled
- [x] Fluid pipe (liquids, 250 mB/t) and gas pipe (gases, 1,000 mB/t) on the shared cable base
- [x] Pressure tank tower: 2x2 footprint up to 16 high, 32,000 mB per block, sight glass showing the steam
- [x] Fluid tank tower for liquids (copper, 16,000 mB per block), the liquid shown at its level
- [x] Home battery panel text fits (checked by `art/tools/ui_text_check.py`)
- [x] Radiation affects mobs too (weakness, slowness, slow damage; config `affect_mobs`), so strong reactors make no-go zones and mob farms

## Milestone 2: Fuel cycle
- [x] Fluorite ore (shipped with the other ores in milestone 1)
- [x] Silicon (electric alloy smelter only), advanced control board, plutonium, spent kernels, fission waste, MOX and uranium fuel rod items
- [ ] Core Cracker
- [ ] Reprocessor (spent kernels + fluorite + water: 1 uranium ingot, 1 plutonium nugget, 1 waste)
- [ ] Fuel Fabricator (MOX rods, uranium rods, TRISO pellets from uranium)
- [ ] Waste Cask
- [ ] Advancements ("Closing the Loop"), JEI and EMI pages, a test pass through the whole loop

## Milestone 3: Fission power station
- [x] Block map drawn and previewed (`art/tools/fission_concept.py`): a 12-wide round tower, 11 high, glass chamber, the roof one giant turbine inside an open steam stack
- [x] Parts (casing, glass, turbine rotor, control core), recipes, ghost outline and snap-together forming
- [x] Formed look: the core draws the station from the exported design, rotor spinning, steam from the stack
- [ ] Draw the static body from a cached GPU buffer (it is about 22,000 faces)
- [x] Auto-build: feed the control core the parts and it builds the station itself
- [x] Core grid GUI (plan tools, heat tints, live readout, plan preview)
- [x] Layout rules reworked: local coolant, moderators for fuel economy, load-driven temperature; planning stats, per-channel tooltips and a rating against the best layouts (`art/tools/core_optimiser.py`)
- [x] Heat, fuel burn, spent rods, turbine and FE out; water, fuel and energy ports at the base; SCRAM at 900°C
- [x] Silver control rods and graphite block moderators
- [x] Output port for spent rods (later waste); fuel rods keep their burn when taken out
- [x] Overdrive: safeties off for about 50% more power, flux tilt countdown, unstable core, meltdown with config toggle
- [ ] Better moderators than graphite (decided later)
- [ ] Fuel rod recipes (the Fuel Fabricator, milestone 2)
- [ ] Advancements, JEI and EMI pages, its own alarm sound

## Milestone 4: Sun dimension prototype
- [ ] Superflat glowing dimension and portal
- [ ] Protective gear and visor whiteout
- [ ] Hand-launched anchor sails and swarm counter
- [ ] Sky renderer showing swarm coverage

## Later
- Breeder reactor and thorium path
- Fusion
- Sail launcher automation and receiver dish
- Overworld sky ring, flare events, multiplayer shared swarm
- Mod integrations (Mekanism; EMI and JEI are done)
- Public release on CurseForge and Modrinth
