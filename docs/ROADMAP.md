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

## Polish
- [x] Cable and pipe fittings: silver, busbar and cryogenic, one slot per machine side in the panel, each tier crafted from the last, shown on the cable
- [x] Fitting item icons: redraw them as cards like the Speed Module (a graphite card with a clear symbol per tier), so they are easy to spot in a chest
- [x] Geiger counter: a new item that gives the clicks and the HUD gauge, so they only run when you carry one (inventory, or an Accessories slot such as the belt). The dosimeter ring keeps its protection but goes quiet. Shift-right-click to mute the clicks but keep the gauge
- [x] Dosimeter ring worn on the hand: dropped. Nothing shows on the hand, as with other mods' rings, and that is fine
- [ ] Steady state (design section 5): a streak of fuel loads burned without downtime raises a reactor's output, up to 1.5x for the microreactor and 2x for the fission station (later tiers higher). Downtime past 5 seconds costs a quarter of the streak; standby, unloaded chunks and a stopped server only pause it. Shown on the reactor's screen, milestones called by the announcer (a new line to record) and an advancement per cap. Config: on/off, cap per tier, milestone scale
- [ ] Meltdown waste: a meltdown (microreactor and up) leaves radioactive debris in and around its crater, such as corium (melted core) and contaminated rubble. It keeps the site radioactive and is hard to clear: it needs proper tools or a process to remove it, and it goes into Waste Casks rather than being broken like stone. Radiation from meltdowns is already allowed by design rule 10; the debris makes the site a lasting problem to deal with, not just a hole
- [x] Reactor voice announcer (0.1.1): short spoken lines from the reactor's control system when something happens, made with Voicebox (MIT app; VoiceStudio does not run on AMD GPUs on Windows). First list: safeties off (overdrive on), safeties back on, SCRAM, overheat warning, flux tilt, coolant loss, meltdown imminent, out of fuel (only once the reactor has actually stopped, never on a routine swap by pipe), reactor and station online. No fuel-low warning: automated fuel is always running low. Each line its own sound event (packs can replace or silence them), subtitles on, played on the client near the machine and not repeated on a fast loop, and a config toggle to turn the voice off. Licences checked 28 Sep 2026: Qwen CustomVoice and Qwen3-TTS (Apache 2.0), Kokoro (Apache 2.0) and Chatterbox (MIT) are fine for a published mod; LuxTTS and HumeAI TADA are unchecked. Plan: a Qwen CustomVoice preset voice (Kokoro as the fallback), no cloning. OmniVoice (VoiceStudio's default) is CC-BY-NC, so never use it. Never clone a real person's voice, and record the engine, model and licence in `art/sounds/CREDITS.md`

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
- [x] Unfinished next-tier content (Lithium Extractor, target rods) behind the `preview.next_tier` config, off by default: no recipes, hidden from the creative tab, JEI and EMI, no target tool in the station grid
- [x] MIT licence (sounds keep their Pixabay licence), version `0.1.0-alpha`, GitHub links in the mod metadata
- [x] Mod page description with a player-facing roadmap
- [x] Mod icon (`art/tools/mod_icon.py`, rendered from the formed microreactor), also the in-game logo
- [x] Test checklist for the run below (`docs/ALPHA-TEST.md`)
- [x] Dosimeter ring in the Accessories slot checked: it protects, and nothing shows on the hand (fine, see Polish)
- [x] Playtest fixes (26 Sep 2026): pipes pull only results from every machine, never ingredients; JEI text placed properly on the fuel, alloying and machine pages, with tidy result columns; moderator plan colour (amber); microreactor fuel in and out on separate ports; station ports moved to the sides (inputs left, outputs right, as you face the front); louvre ribs under the station's stack aligned; station casing 16 per craft
- [ ] Steam in JEI and EMI: a page showing the microreactor and the station turning water into steam (mB per mB)
- [ ] Visual audit of the multiblocks (microreactor, station, tanks, battery) before screenshots
- [x] Commit and push the current work
- [ ] Survival run on a fresh world with only the release jar (no dev mods), then again with JEI, and once inside ATM10
- [ ] Screenshots and a short clip: the microreactor snapping together, the station building itself and running
- [ ] Make the GitHub repo public; turn on issues
- [ ] Create the Modrinth and CurseForge projects, upload the jar, credit the sounds

## Milestone 4: Fission to fusion (design section 10)
- [x] Lithium Extractor (salt and water to lithium)
- [x] Lithium target rods (Fuel Fabricator) and a target channel in the station core: breeds tritium from neighbouring fuel, costs 20% of their heat, irradiated rods leave by the output port
- [x] Power is the price (design section 5): gated machines in `machine/processing` (two-tick buffer, full draw every tick or no progress, UNDERPOWERED status), draw and time scaled per machine in the common config (`machines.<id>`), time left and energy to finish on the arrow's tooltip
- [ ] Waste Refinery: palladium from fission waste, the rest vitrified
- [ ] Tritium extraction from irradiated target rods (palladium membranes)
- [ ] Heavy Water Plant, and a heavy water moderator channel in the station
- [ ] Monazite processing (yttrium, and thorium for tier 4)
- [ ] Cryo Plant (liquid nitrogen)
- [ ] Superconducting tape and magnet coils; tungsten tiles
- [ ] Uranium glass (decorative light block)
- [ ] A better coolant for the fission station (open question 21): a coolant channel filled with it carries more heat, so a MOX core can be pushed harder and target rods breed tritium faster. Made by a later process, and used again by later tiers

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

## Later
- Tier 4: pyroprocessor (spent MOX to transuranic metal), sodium from salt, breeder reactor with a uranium or thorium blanket (design section 9)
- Fusion
- Sail launcher automation and receiver dish
- Overworld sky ring, flare events, multiplayer shared swarm
- Mod integrations (Mekanism; EMI and JEI are done)
- Beta once tier 4 (the breeder) lands; stable once the energy scale is settled
