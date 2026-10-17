# Changelog

Player-facing changes, newest first. Paste each release's section into the Modrinth and CurseForge changelog and the GitHub release notes.

## 0.2.2-alpha (18 October 2026)

### New
- **The Assembler.** An open robot cell that crafts: a 3 x 3 x 3 multiblock where an orange six-axis arm carries the real ingredients from the in-feed belt to a fixture bed, lays them out as the recipe needs, and takes the result to the out-feed. It holds nine patterns of any crafting-table recipe, taking turns, and a pattern takes any item its recipe takes (another mod's lead works where ours was set). 40 FE/t; Speed Modules craft more per cycle. Its stack light and readout show what it is waiting for.
- **Pipe filters.** Each side of an item pipe can take a filter of up to 7 items (only these, or all but these) and a priority, set in the pipe's panel.
- **The coolant reserve**, emergency core cooling for the fission station. Build Coolant Reserve blocks onto the station's back in bays (1, 3 or 5, growing out from the back's centre). It fills from the station's spare water, and in overdrive it holds a core that has lost its cooling at 620°C while its water lasts, 2 minutes a bay, with INJECTING and a countdown on the control screen. Dry, the core heats on as before. It does nothing for a flux tilt.
- **The Operator's Handbook.** A ring-bound manual you get the first time you join a world (or craft from 3 paper and a uranium), with a chapter for each of Start, Basics, Power, Heat, Radiation and Help. A "Press Y for the map" hint shows after joining until you have opened the progression map once, and the map has Handbook, Discord and Report a bug links in its corner.
- **A sound switch** on every machine that makes a running sound. Muted, it is quiet, but meltdown alarms and spoken warnings still sound.
- **Creative Sodium Tank**: endless liquid sodium, for trying out the breeder.

### Changed
- **Machines come alive.** Every single-block machine shows what it is doing: moving parts that ease in and out and speed up with Speed Modules, glows that brighten with its heat, a status lamp (green working, amber on standby, red short of power), and dust, fumes, embers, drips or smoke while it works.
- **The Electrorefiner is rebuilt** as a real pyroprocessing cell: a pot of molten salt behind a sight glass, four anode baskets turning in it, and a cathode lifting its plated metal out.
- **The Spent Fuel Pool** has a new look, with a railing round its rim, a radiation monitor, a level gauge and a thermometer that climbs with the decay heat of the rods cooling in it.
- **The Mega Drill** has service lines from its ports up to the deck, roof fans that turn, beacons that sweep while it runs, an aviation light on its crown and floodlights into the pit.
- **The Alloy Smelter is a plain block** now, like vanilla's furnaces: banded firebrick in a steel frame with a copper flue on top, and its fire glowing in the door while it burns.

### Fixed
- **Power-hungry machines can no longer be run on a weak supply through a redstone clock.** A gated machine now works only when its full draw has arrived.
- **The round sun is white hot without shader packs.** Its light was drawn under its disc, so it looked dull.
- The Electrorefiner's top takes a pipe again, and the pool's radiation signs show whole.

## 0.2.1-alpha (11 October 2026)

### New
- **One switch for all ore generation.** `generate_ores` in `ryzergen-common.toml` turns off every Ryzer Gen ore at once, for packs where other mods already supply them. Each ore still has its own switch too. Every recipe asks for common tags (`c:ingots/uranium`, `c:raw_materials/lead` and so on), so uranium, lead, silver and aluminium from other mods, such as Immersive Engineering, work in their place.

## 0.2.0-alpha (11 October 2026)

### New
- **Tier 4: the breeder reactor.** A sodium-cooled fast reactor in a steel sphere, built round its Breeder Control Core. It burns the transuranics the new Electrorefiner recovers from spent MOX, and its fuel makes more neutrons than it needs: blankets placed round the core catch the spare ones and breed new fuel. A uranium blanket breeds plutonium for MOX rods; a thorium blanket breeds U-233. Plan its 19-position core on the control screen, fill its sodium loop and set the pumps' flow to carry the heat, for up to 38,000 FE/t. Keep the pumps running when it stops: with no flow, a hot core's decay heat boils the sodium into a fire inside its bund wall (a config option turns this off). It has its own announcer lines, advancements and JEI and EMI pages.
- **The thorium line.** The Monazite Processor cracks raw monazite with liquid sodium into thorium and yttrium, at 2,000 FE/t. Thorium blankets breed U-233 in the breeder, and U-233 rods are the fission station's hottest fuel: 2,500 FE/t of heat for an hour. Yttrium makes Cryogenic Fittings.
- **The Electrorefiner** recovers transuranic metal from spent MOX and makes sodium from salt, on fission power.
- **The progression map.** Press Y for a map of the whole mod, from ore in the ground to the breeder: every step in tier columns, with arrows from each step to what it feeds. The steps you have reached light up and the ones you can make next pulse, so you can see how far you have come. Click a step to read how it works, and open its spoilers for the best setups we have found (the station's best layouts among them). The key can be changed under Controls.
- **Milestones on the progression map.** Each tier's big machine (the microreactor, the Mega Drill, the Fission Station, the breeder) stands out as a large card with its headline stat.
- **JEI and EMI pages** for the Spent Fuel Pool, the Container Battery and steam. The pool's page gives its cooling times and the battery's its capacity and rate, from your config. Look up steam to see where it comes from: the microreactor boils its coolant into steam you can pipe out, while the fission station boils its water into its own turbine.
- **Two advancements:** Cooling Off (build a Spent Fuel Pool) and Grid Scale (build a Container Battery).
- **Floodlights round the fission station's base.** They glow while it runs, and the base gives off light, so the ground round the station is lit at night.
- **The mega drill.** The mod's quarry, at tier 2. Place a Mega Drill Controller on the ground, feed it 234 Drill Frame and it builds a 16-high tower over an open pit (a ghost outline shows where). Fit a Drill Head and a Tungsten Tip, and feed it power and plenty of water: it charges, fires its beam, and the layer under its middle glows red hot and bursts, a 7 x 7 shaft down to the bottom of the world. There it bores on into the deep crust, bringing up ore every cycle at several times the power: the common ores with a Mk I head, uranium, lead, silver and gems with a Mk II, tungsten and monazite with a Mk III. Ores come up into its output (with anything you put in its filter); the rest melts away, and water and lava in the pit's walls set into obsidian. Drill heads Mk I to III and Speed Modules make it faster, at the square of the power. Its deck can be walked on and its ladder climbed. It has its own sounds (the beam's shot quickens with its speed), and a switch on its screen mutes it.

- **Steady state.** A reactor that keeps running builds a streak of fuel loads burned without stopping, which raises its power output: up to 1.25x for the microreactor, 1.5x for the fission station and 1.75x for the breeder, reached over 8, 32 and 128 loads. A stop of more than 5 seconds costs a quarter of the streak; standing by on a full buffer only pauses it. Hover a reactor's power readout to see its streak, and the announcer calls each milestone. Two new challenges: Never Off and Baseload.
- **A new announcer voice** for every reactor line, with new lines for steady state and the breeder.

### Changed
- **Clearer text everywhere.** Tooltips, the progression map, the JEI and EMI pages and the machine screens now read at a glance. Key terms (Power, Heat, Coolant, Fuel, Radiation and more) are always one colour and carry a small icon, and numbers come first ("2,000 FE/t Heat"). Tooltips show what an item is and its numbers straight away, with how it works behind Shift, and no longer repeat what the progression map explains. The station's best layouts on the map show their power and rod count beside them, so you can tell them apart.
- **Text says what things are for, not what goes into them** (JEI and EMI show recipes). The progression map's panels show what each step needs and what it leads to, and the core planners' tools say what each part does.
- **New ore textures.** Each ore now shows its real mineral: black pitchblende crusted with lime autunite (uranium), stepped galena cubes (lead), wire silver, round bauxite pebbles (aluminium), purple fluorite cubes with a cyan glint, pink halite cubes (salt), black wolframite blades flecked with cream scheelite (tungsten) and honey-brown monazite prisms. The crystals catch the light and shade the rock beside them, and every block's edge is plain stone, so a vein blends into the cave wall as vanilla's ores do. Raw uranium, aluminium, tungsten and monazite are recoloured to match.
- **The Alloy Smelter has new textures**: fired bricks in varied tones, steel guards on its corners and a grated fire door.
- **Cables and pipes look like a short length of pipe in your inventory**, with the icon of what each carries in the corner of its slot: energy, items, liquids or gases.
- **Control rods are violet** in the core grids and text (red stays for power), the breeder's fuel is green like the station's, and blankets are pink.
- **The fission station's graphite moderator columns** are built like its other rods: three graphite bricks banded with steel, on an end fitting with a cap.

### Fixed
- **Water the mega drill frees now behaves.** Flowing water held up by a block or source the drill removes recedes or falls instead of hanging in the shaft, and each water source the beam deletes boils off in a puff of steam.
- **The Lithium Extractor's recipes show in JEI and EMI** again. It left the preview in 0.1.2, but the recipe viewers still hid it unless the preview was on.
- **The wormhole no longer prints the outline of builds in front of it** into the bent sky, and the dark band of bent ground round it fills in solid.

## 0.1.2-alpha (29 September 2026)

### New
- **Moving machines.** Machines now show what they are doing while they work:
  - The Core Cracker's flywheels spin on their axle.
  - The Fuel Fabricator's press strokes down onto its bed.
  - The Lithium Extractor's columns fill with light as brine is pumped up, hold at the top, squeeze back down and rest.
  - The fission station's rods rise to the reactor head while it runs and sink halfway when it stops. A newly loaded rod comes in halfway and rises (it works at once).
  - The Spent Fuel Pool's crane runs to the racks when fuel finishes cooling, lifts a rod and carries it to the output end (at most once every ten seconds). Hot fuel bubbles under the water.
  - The Container Battery's fan spins while energy flows.
- **Spent Fuel Pool.** Spent fuel now comes out of reactors *hot*, still giving off decay heat, and must cool before the Core Cracker or Reprocessor will take it. Place a Pool Controller on the ground, feed it 43 Pool Liner and a Pool Crane, and it builds a 5 by 3 by 3 pool itself (a ghost outline shows where); the controller then moves up to sit in the middle of the front. Fill it with water, then load the hot fuel into its 18 racks, where it glows blue under the water: a core cools in 5 minutes, a fission rod in 10. Decay heat boils the water off, so keep it topped up. Hot fuel and water go in on its left end as you face it, and cooled fuel comes out on the right. **Before you update:** fuel you already have counts as cooled, so nothing is stranded. Packs that want the old loop can turn the step off with `fuel_cycle.require_cooling` in the common config.
- **Container Battery.** Grid-scale storage: a white shipping container with a gunmetal fan unit on one end. Place a Battery Controller on the ground, feed it 70 Container Frame and a Thermal Unit, and it builds the 8 by 3 by 3 container itself. Its front has 20 hatches: right-click an LFP Battery Rack into one and that hatch becomes a lit, glazed door, adding 2M FE and 2,000 FE/t. Shift-right-click with an empty hand takes a rack out with its charge. Its two energy ports on the back each take energy in or give it out, so your cables decide the flow and a network treats it as a battery. Coolant goes in at the fan's hub; without coolant it runs at a quarter of its rate. The fan spins while energy flows.
- **The Lithium Extractor is out of preview.** LFP racks need lithium, so the extractor and lithium dust now load in every game. Target rods and tritium stay behind `preview.next_tier`.
- **Every machine redesigned.** The alloy smelter, electric alloy smelter, Core Cracker, Reprocessor, Fuel Fabricator, Lithium Extractor and Intake Pump are now proper 3D machines that share one family look:
  - The alloy smelter is a brick hearth with a fire door that glows while it burns, under a hood and flue.
  - The electric alloy smelter shows its induction coil and crucible, which glow while it works.
  - The Core Cracker has a jaw crusher, a flywheel and a hopper mouth.
  - The Reprocessor has a dissolver vessel below and a lead-lined column above, with a sight glass where the solution bubbles teal while it works, a hazard band and a label plate.
  - The Fuel Fabricator has a press cylinder over a rod loading bed.
  - The Lithium Extractor has a brine basin and a column that lights up while it works.
  - The Intake Pump is a blue motor on a pump casing, with a copper discharge nozzle.
- **Pipes meet something.** Every machine's top reaches the top of the block (the Core Cracker's hopper, the smelter's flue, a flanged connector on the rest) and its back carries a connector, so pipes and cables join cleanly. The electric smelter, Fuel Fabricator and Lithium Extractor carry theirs on a service frame.
- **New surfaces.** No texture is flat colour any more: a subtle finish on every surface, and designed panels with screws on the big faces.
- **The microreactor, rebuilt to the new standard:** staved vessel with bolted flanges and a flat closure head with a lit control drive, radiators on the coolant housing, plated panels, round pipes. The ports have not moved.
- **New logo:** the fission station, running.
- **Hold Shift for details.** Every machine, tool, fuel and part has a short summary under its name, and more while you hold Shift, colour coded: values in cyan, benefits in green, cautions in gold, dangers in red and controls in yellow.
- **The creative tab is sorted by type:** tools, machines, pipes, storage, upgrades, fuel, ore blocks, raw ores, ingots, materials and boards, with the creative and preview items last.

### Fixed
- **Batteries no longer drain into each other.** Energy a cable pulls out of a battery now only goes to machines, never into another battery, so a Container Battery on the same network as your modular batteries no longer empties them.
- **Fission station coolant is local, as the control screen says.** Heat that no coolant channel can reach (a rod with no coolant beside it, or more than one channel can carry) now stays in the core. Before, the other coolant channels quietly carried it off, so a core the control screen called too hot ran fine. **Before you update:** if a station's control screen says *too hot*, fix its layout, or it will climb until it SCRAMs. In overdrive it will melt down.
- **A SCRAM trips the turbine.** While a SCRAMmed core cools, its steam now goes round the turbine and it makes no power, as in a real plant. Before, the turbine kept turning on the stored heat, so overloading a core and letting it SCRAM cost almost nothing.
- Thanks to u/MushroomMan234, whose [Fission Station Calculator](https://moddecoded.com/tools/ryzer-gen/fission-station-calculator/) on Mod Decoded simulates the station tick by tick and found both.

## 0.1.1-alpha (28 September 2026)

### New
- **Reactor announcer.** The microreactor and the fission station now speak when something worth hearing happens: safeties off and on, SCRAM, overheating, flux tilt, coolant loss, meltdown imminent, and coming online after a shutdown. Re-arming the safeties while the core is hot is announced as a controlled shutdown, not a SCRAM alarm. It sounds like an emergency PA. Turn it down with the vanilla Voice/Speech volume slider, or off for everyone with `announcer.enabled` in the common config.

- **Auto output** on the Core Cracker, Reprocessor, Fuel Fabricator and Lithium Extractor: a tab on the right edge of the machine's screen, with a hopper key and a light that glows green while it is on. When on, the machine pushes its results into the blocks beside it: a chest, another machine or a pipe. Off by default, so machines that only touch never hand each other things.

- **The fission station's new look.**
  - The chamber's plain ribs are now I-beams in gunmetal shoes, with copper coolant pipes running down every other one.
  - Every part of the station has its own texture now.
  - The stack is clad in big panels and carries the station's number, an access hatch, and red and white aviation marking with red beacons.
  - The port housings have a hood with a lit tag in each port's colour.
  - Each kind of rod looks different through the glass: fuel bundles glow green, coolant channels glow Cherenkov blue, and steam fills the chamber while the station runs.

- **A round sun.** The overworld's sun is a glowing round disc instead of vanilla's square. Turn it off with `sky.round_sun` in the common config.

- **A first look at the Dyson swarm.**
  - The creative-only Swarm Controller launches a swarm of 640 solar panels round the sun, and you can watch it from the ground.
  - Use it again and the panels fly in and lock together into a shell. The far side closes first and the last gap shuts like an iris, and the daylight goes out.
  - While the sun is enclosed, the sky stays a dark ember orange and the world stays dim. The moon goes dark too, since it has no sunlight to reflect.
  - Use it once more and the enclosed sun collapses to a white flash. A wormhole opens where it was, bending the sky round it into a ring, and through it you can see a new star somewhere else. With a shader pack on, it shows as a simpler picture instead.
  - Sneak and use it to light a new sun.
  - Only operators can use it. The tier that builds the swarm for real comes later.

- **Flow Scanner.** Hold it and every pipe and cable input nearby shows what comes in on it, as a floating label coloured by how close that input is to its limit (green idle, yellow half, red at the limit). The quickest way to find a bottleneck in a big network.

### Changed
- **Coolant is your friend.** The fission station boils 4 times as much water, and 8 times in overdrive. A strong core now needs several intake pumps (about 5 for the best uranium layout, 9 for MOX, twice that in overdrive). `fission_station.water_use_percent` scales it.

### Performance
- Pipes and cables share one network per group of joined pipes, scanned once, and a change only rebuilds the networks it touches. Before, placing or wrenching any pipe anywhere made every cable in the world rescan its whole network, which could stutter in big bases.
- Energy cables only tick when they have an extract side, like item and fluid pipes already did.
- Networks no longer stop at 4,096 blocks, and scans no longer load chunks.

### Fixed
- Intake pumps only pushed water out of their top, so a pipe joined to a pump's side got no water unless it was set to extract. Pumps now push out of every side but the bottom, into pipes first (so a pipe's panel shows the pump's full output) and into a tank or machine beside them only with what is left. A pipe set to extract from a pump takes all of it.
- The fluid pipe panel counted intake pumps as tanks.
- A pipe side that something pushes into stayed listed as an input only while things fitted: with a full tank, the microreactor's steam side dropped off the pipe's panel and the Flow Scanner. It now shows 0 instead.
- With a shader pack on, the ghost outlines of an unbuilt microreactor or station cast shadows. They no longer do.
