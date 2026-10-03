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
- [x] Fuel automation: fresh cores in on the left of the vessel, spent cores pushed out on the right (two ports, so the two pipe lines never mix)

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
- [x] Shared machine recipe type (items and an optional fluid in, several outputs), JEI and EMI categories
- [x] Core Cracker (20 FE/t, 10 s: depleted core to spent kernels, 2 graphite, 2 steel)
- [x] Reprocessor, two high (80 FE/t, 20 s, 250 mB water: spent kernels or a spent uranium rod plus fluorite to uranium, plutonium nuggets and waste)
- [x] Fuel Fabricator (40 FE/t, 10 s: uranium rods, MOX rods from a plutonium ingot, 3 TRISO pellets per uranium)
- [x] Waste Cask (1,024 waste, keeps its contents when broken, fill gauge)
- [x] Models, textures and GUIs in the house style; GUI text checked
- [x] Advancements (first cracked core, "Closing the Loop", first MOX rod)
- [x] A test pass through the whole loop in game
- [x] Speed upgrades for the machines: a Speed Module, up to 4 per machine, each adding a full speed at the square of the power
- [x] Machine sounds: our own sound events for each machine working, looped on the client while it runs (sources in `art/sounds/CREDITS.md`)
- [x] Machine redesign (0.1.2): all seven single-block machines rebuilt as box models on shared family textures (`art/tools/machine_family.py`, one texture script per machine)
- [x] Pipe connections on every single-block machine: the top reaches the block's top over the pipe's centre and the back carries a neutral connector (the intake pump's discharge flange on top)
- [x] Surface finish (`pixelart.finish`) on every machine texture, and designed panels on big flat faces (Core Cracker frame, electric smelter and fuel fabricator cabinets)
- [x] Core Cracker flywheels turn while it works (a model of their own on a block entity renderer)
- [x] Machine animations: Fuel Fabricator press stroke, Lithium Extractor column glow pulse, station rods rise while running and sink halfway when stopped (new rods come in halfway), Spent Fuel Pool crane run when fuel cools (at most every 10 seconds)
- [x] Microreactor on the design pipeline (`microreactor_concept.py`): staves, bolted flanges, closure head, radiators, plated housing; new logo of the fission station (`mod_icon.py`)
- [ ] Electric alloy smelter sound: a working loop like the fuel cycle machines (an induction hum), from a recording the user sources into `art/sounds/source/`

## Polish
- [x] Cable and pipe fittings: silver, busbar and cryogenic, one slot per machine side in the panel, each tier crafted from the last, shown on the cable
- [x] Fitting item icons: redraw them as cards like the Speed Module (a graphite card with a clear symbol per tier), so they are easy to spot in a chest
- [x] Geiger counter: a new item that gives the clicks and the HUD gauge, so they only run when you carry one (inventory, or an Accessories slot such as the belt). The dosimeter ring keeps its protection but goes quiet. Shift-right-click to mute the clicks but keep the gauge
- [x] Dosimeter ring worn on the hand: dropped. Nothing shows on the hand, as with other mods' rings, and that is fine
- [ ] Steady state (design section 5): a streak of fuel loads burned without downtime raises a reactor's output, up to 1.5x for the microreactor and 2x for the fission station (later tiers higher). Downtime past 5 seconds costs a quarter of the streak; standby, unloaded chunks and a stopped server only pause it. Shown on the reactor's screen, milestones called by the announcer (a new line to record) and an advancement per cap. Config: on/off, cap per tier, milestone scale
- [ ] Meltdown waste: a meltdown (microreactor and up) leaves radioactive debris in and around its crater, such as corium (melted core) and contaminated rubble. It keeps the site radioactive and is hard to clear: it needs proper tools or a process to remove it, and it goes into Waste Casks rather than being broken like stone. Radiation from meltdowns is already allowed by design rule 10; the debris makes the site a lasting problem to deal with, not just a hole
- [x] Reactor voice announcer (0.1.1): short spoken lines from the reactor's control system when something happens, made with Voicebox (MIT app; VoiceStudio does not run on AMD GPUs on Windows). First list: safeties off (overdrive on), safeties back on, SCRAM, overheat warning, flux tilt, coolant loss, meltdown imminent, out of fuel (only once the reactor has actually stopped, never on a routine swap by pipe), reactor and station online. No fuel-low warning: automated fuel is always running low. Each line its own sound event (packs can replace or silence them), subtitles on, played on the client near the machine and not repeated on a fast loop, and a config toggle to turn the voice off. Licences checked 28 Sep 2026: Qwen CustomVoice and Qwen3-TTS (Apache 2.0), Kokoro (Apache 2.0) and Chatterbox (MIT) are fine for a published mod; LuxTTS and HumeAI TADA are unchecked. Plan: a Qwen CustomVoice preset voice (Kokoro as the fallback), no cloning. OmniVoice (VoiceStudio's default) is CC-BY-NC, so never use it. Never clone a real person's voice, and record the engine, model and licence in `art/sounds/CREDITS.md`

## Milestone 2b: First multiblocks after the station
- [x] Spent Fuel Pool concept (`art/tools/pool_concept.py`): the framed-plate grid, windows, trefoils, crane inside the rim, cooling loop with mitred pipes on the back
- [x] Spent Fuel Pool build (design section 7): liner, controller and crane parts; auto-build with a ghost outline; formed model cut by datagen from the exported design (`art/designs/spent_fuel_pool.json`, three looks: dry, full, cooling); hot mark on spent fuel from reactors, refused by the cracker and reprocessor; 18 cooling slots, water tank and boil-off; GUI; config `fuel_cycle.require_cooling` and cooling times; recipes, tooltips; changelog note
- [x] Spent Fuel Pool finished (0.1.2): tested in game; the crane on a block entity renderer, running to the racks when fuel has cooled; bubbles over hot fuel; JEI and EMI info page; an advancement (Cooling Off)
- [x] Container Battery concept (`art/tools/container_concept.py`): white container, 20 hatch slots on the front, gunmetal fan unit on the east end, power on the back
- [x] Container Battery build (design section 12): frame, controller and thermal unit parts; auto-build from the ground with the controller swapping up to its console; LFP racks installed per slot (model and light per block); fan on a block entity renderer; quarter rate without coolant; readout with a slot map; the Lithium Extractor out of preview for the racks' lithium
- [x] Container Battery finished (0.1.2): tested in game; energy pulled from a battery goes only to machines, so batteries no longer drain into each other; JEI and EMI info page; an advancement (Grid Scale)
- [ ] Sodium-ion racks for the Container Battery: a second chemistry (design rule 11)
- [ ] Heavy Water Plant and Cryo Plant are designed as multiblocks too (milestone 4). No cooling tower for now

## Milestone 3: Fission power station
- [x] Block map drawn and previewed (`art/tools/fission_concept.py`): a 12-wide round tower, 11 high, glass chamber, the roof one giant turbine inside an open steam stack
- [x] Parts (casing, glass, turbine rotor, control core), recipes, ghost outline and snap-together forming
- [x] Formed look: the core draws the station from the exported design, rotor spinning, steam from the stack
- [x] Draw the static body from a cached GPU buffer (it is about 22,000 faces): `client/StationMesh`, rebuilt only when the light, facing or resources change; drawn the old way while a shader pack is on
- [x] Auto-build: feed the control core the parts and it builds the station itself
- [x] Core grid GUI (plan tools, heat tints, live readout, plan preview)
- [x] Layout rules reworked: local coolant, moderators for fuel economy, load-driven temperature; planning stats, per-channel tooltips and a rating against the best layouts (`art/tools/core_optimiser.py`)
- [x] Heat, fuel burn, spent rods, turbine and FE out; water, fuel and energy ports at the base; SCRAM at 900°C
- [x] Silver control rods and graphite block moderators
- [x] Output port for spent rods (later waste); fuel rods keep their burn when taken out
- [x] Overdrive: safeties off for about 50% more power, flux tilt countdown, unstable core, meltdown with config toggle
- [ ] Better moderators than graphite (decided later)
- [x] Fuel rod recipes (the Fuel Fabricator, milestone 2)
- [x] MOX rebalanced: twice uranium's heat and more coolant capacity, so a full MOX core makes double the power
- [x] Spent MOX rods come out of the station (they wait for the tier 4 pyroprocessor)
- [x] Its own alarm (a klaxon) and a turbine hum that follows the rotor
- [x] Advancements (Rod Loaded, Power Station, By the Book, No Brakes, Crater Maker), a Reactor Fuel page and info pages in JEI and EMI

## Public alpha (0.1.0-alpha): tiers 1 to 3
The first public build: the microreactor, home battery, pipes and steam, the fuel cycle and the fission station. Page text in `docs/MOD-PAGE.md`.
- [x] Unfinished next-tier content (target rods; the Lithium Extractor left the preview in 0.1.2 for the Container Battery) behind the `preview.next_tier` config, off by default: no recipes, hidden from the creative tab, JEI and EMI, no target tool in the station grid
- [x] MIT licence (sounds keep their Pixabay licence), version `0.1.0-alpha`, GitHub links in the mod metadata
- [x] Mod page description with a player-facing roadmap
- [x] Mod icon (`art/tools/mod_icon.py`, rendered from the formed microreactor), also the in-game logo
- [x] Test checklist for the run below (`docs/ALPHA-TEST.md`)
- [x] Dosimeter ring in the Accessories slot checked: it protects, and nothing shows on the hand (fine, see Polish)
- [x] Playtest fixes (26 Sep 2026): pipes pull only results from every machine, never ingredients; JEI text placed properly on the fuel, alloying and machine pages, with tidy result columns; moderator plan colour (amber); microreactor fuel in and out on separate ports; station ports moved to the sides (inputs left, outputs right, as you face the front); louvre ribs under the station's stack aligned; station casing 16 per craft
- [x] Steam in JEI and EMI: an info page on steam and the reactors, with the microreactor's water to steam rate from the config (the station boils its water into its own turbine and gives out no steam)
- [ ] Visual audit of the multiblocks (microreactor, station, tanks, battery) before screenshots
- [x] Commit and push the current work
- [ ] Survival run on a fresh world with only the release jar (no dev mods), then again with JEI, and once inside ATM10
- [x] Screenshots and a short clip: the microreactor snapping together, the station building itself and running
- [x] Make the GitHub repo public; turn on issues
- [x] Create the Modrinth and CurseForge projects, upload the jar, credit the sounds (Modrinth still in review)

## Milestone 3b: Breeder reactor (tier 4, design section 9)
The utility reactor: fuel for the stations and the tritium that starts fusion. Fusion needs it.
- [x] Settle the breeder's open questions (30 Sep 2026): the blanket is a ring in the planning grid; spent breeder fuel closes the loop; sodium is an ingot and a liquid coolant; tritium is a gas; the pyroprocessor is the Electrorefiner; americium and curium stay in transuranic metal
- [x] Concept (`art/tools/breeder_concept.py`, textures `breeder_textures.py`): a steel sphere on 12 braced legs, as Dounreay's fast reactor was housed, with a girder belt at the equator, a railed platform on top, the console proud of the plinth; even textures
- [x] Concept, fleshed out: a walkway round the belt, a caged ladder, the platform's hatch and light strip, footings, sodium lines, and the beacon (its moving part)
- [x] The beacon on a block entity renderer: turning while the reactor runs (a redstone signal on the core stands in for running until the core logic)
- [x] Electrorefiner (behind `preview.next_tier`): one molten salt cell, gated at 1,500 FE/t. Spent MOX and salt into transuranic metal, a uranium ingot and fission waste (30 s); salt alone into a sodium ingot (10 s). Sodium ingot and transuranic metal items
- [x] Liquid sodium (`c:sodium`), melted from the ingot in the Electrorefiner into its new output tank (processing machines can now give a fluid result)
- [ ] Electrorefiner sound: an electrical hum from a recording (it borrows the turbine's drone for now)
- [x] Breeder fuel from transuranic metal (the Fuel Fabricator: transuranic metal, uranium and 2 steel)
- [x] Breeder multiblock: parts, ghost outline, auto-build, formed look (a GPU mesh lit from the open sky for shader packs); the base reworked into a compound (bund wall, shallow consoles, one shared pipe a side to a hub), white to match the station, lights and floodlights that run with it
- [x] Working breeder ports (fuel and sodium in on the east, spent fuel, bred blankets and energy out on the west)
- [x] Core and blanket, first pass: a 19-position hex core planned on a control screen, fast fuel, a closed sodium loop with pump flow and temperatures, SCRAM; uranium and lithium blankets; spent fuel and bred blankets out by port and refined in the Electrorefiner. Thorium to U-233 waits for monazite
- [ ] Play-test the breeder's numbers (heat, breeding rates, loop, pumps)
- [ ] Safety and failure (sodium fire, not a steam explosion), steady state, sounds and announcer, advancements, JEI and EMI pages
- [ ] Retire the station's target channel; target rods go in the breeder's blanket
- [ ] Monazite processing brought forward from milestone 4 for thorium (yttrium can wait)

## Milestone 4: Fission to fusion (design section 10)
- [x] Lithium Extractor (salt and water to lithium)
- [x] Lithium target rods (Fuel Fabricator) and a target channel in the station core: breeds tritium from neighbouring fuel, costs 20% of their heat, irradiated rods leave by the output port. Moving to the breeder's blanket (design section 9), so the station's target channel is retired when the breeder lands
- [x] Power is the price (design section 5): gated machines in `machine/processing` (two-tick buffer, full draw every tick or no progress, UNDERPOWERED status), draw and time scaled per machine in the common config (`machines.<id>`), time left and energy to finish on the arrow's tooltip
- [ ] Waste Refinery: palladium from fission waste, the rest vitrified
- [ ] Tritium extraction from irradiated target rods (palladium membranes)
- [ ] Heavy Water Plant (a multiblock cascade of exchange towers), and a heavy water moderator channel in the station
- [ ] Monazite processing (yttrium, and thorium for tier 4)
- [ ] Cryo Plant (a multiblock: compressors and a cold box; liquid nitrogen)
- [ ] Superconducting tape and magnet coils; tungsten tiles
- [ ] Uranium glass (decorative light block)
- [ ] A better coolant for the fission station (open question 21): a coolant channel filled with it carries more heat, so a MOX core can be pushed harder. Made by a later process, and used again by later tiers

## Milestone 5: Fusion reactor
- [x] Design draft: shape, parts, running it, blanket, wear, H-mode, safety (design section 10b)
- [x] Concept render (`art/tools/tokamak_concept.py`): the doughnut vessel with the plasma ring behind viewports, 16 D coils, solenoid, poloidal rings, heating injector
- [x] Creative Fusion Reactor Preview block: draws the concept design round itself in game (next-tier preview only)
- [ ] Block map, part blocks, ghost outline, snap-together forming and auto-build (behind `preview.next_tier` until tier 5 is complete)
- [ ] Formed look: the core draws the reactor from the concept design (GPU mesh), the plasma glowing and pulsing while it burns
- [ ] Control core GUI: magnets, charge, fuel mix, heating, triple product, Q, blanket plan
- [ ] Plasma model: temperature, density limit, confinement, Q and ignition, disruptions, H-mode
- [ ] Blanket modules and tritium breeding ratio; tungsten tile wear and replacement
- [ ] Sounds, advancements, JEI and EMI pages

## Milestone 6: Sun dimension prototype
- [x] Sun gate concept design (`art/tools/sun_gate_concept.py`) and creative preview block: a mini sun over a platform, with the shade swarm as an orrery showing coverage
- [ ] Superflat glowing dimension, entered through the sun gate
- [ ] Protective gear and visor whiteout
- [ ] Hand-launched anchor sails and swarm counter
- [ ] Sky renderer showing swarm coverage

## Melt Drill (design section 11c)
- [x] Concept (`art/tools/melt_drill_concept.py`): the quarry block outside a chunk, drills (Mk I to III) standing up in its four quarters, each a tower on legs with a glass beam chamber (after a derrick, a rocket and a single-machine version)
- [x] Concept refined with the user: drills as tall eight-sided towers (`art/concepts/melt_drill_mk3.png`) that sink layer by layer; a static mega drill as the fallback
- [ ] Settle: what a tower does at bedrock and when its drill is pulled
- [ ] The quarry block and drills: four quarters to bedrock, fused rock walls, filter, drill marks, speed modules, tungsten tips, chunk loading, config
- [ ] Benchmark its speed against ATM10's quarries

## Progression map
- [x] An in-game map of the whole production line (key Y): tier columns, arrows, steps lit as you reach them and the next ones pulsing, a panel per step with how it works and spoilers for the best setups (3 Oct 2026)
- [ ] Open a step's recipes in JEI or EMI from its panel

## Later
- Fusion
- Sail launcher automation and receiver dish
- Overworld sky ring, flare events, multiplayer shared swarm
- Mod integrations (Mekanism; EMI and JEI are done)
- Beta once tier 4 (the breeder) lands; stable once the energy scale is settled
