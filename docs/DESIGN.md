# Ryzer Gen: Design Doc

Status: draft v0.1, 23 Sep 2026

## 0. Identity

- **Name:** Ryzer Gen ("Ryzer" after the author's online name, "Gen" for power generation).
- **Mod ID:** `ryzergen`
- **Target:** Minecraft 1.21.1, NeoForge.

## 1. Pitch

Most reactor mods have one answer to "how do I get more power": build it bigger. This mod makes power come from understanding instead of volume. Every tier teaches a new mechanic, the output of one tier feeds the next, and the physics stays grounded in real science until the very top, where it moves into hypothetical-but-serious territory.

## 2. Design pillars

1. **Progression through knowledge, not volume.** A small, well-designed reactor should beat a big sloppy one.
2. **Somewhat accurate.** Real physics where we can, honest fudges where we must, and never pure fantasy.
3. **No grind.** Cheap structure, meaningful components. Milestones unlock tiers, not piles of ingots. Power is the price of progress (section 5).
4. **Every tier stays useful.** Earlier reactors become stepping stones, not junk.
5. **Pack friendly.** Standard energy, easy recipe tweaking, sensible configs, good performance. Built to earn a spot in packs like ATM10.

## 3. Progression ladder

| Tier | Machine | Real-world basis | New mechanic it teaches |
|---|---|---|---|
| 1 | Microreactor (4-block) | Transportable "nuclear battery" microreactors, TRISO fuel | Heat, fuel life, basic output |
| 2 | Fuel cycle | Reprocessing, MOX fuel (as done in France) | Spent fuel is the next fuel |
| 3 | Modular fission reactor | Conventional fission, moderators and coolant | Layout design |
| 4 | Breeder and thorium | Fast breeder reactors, thorium to U-233 | Making more fuel than you burn, and the tritium that starts fusion |
| 5 | Fusion | Deuterium-tritium tokamaks (ITER), fed by fission and the breeder (see section 10) | Plasma stability, breeding tritium from lithium |
| 6 | Dyson swarm | Dyson swarm concepts, beamed power | Building in the sun dimension |

Later ideas kept on file (not in scope yet): aneutronic fusion, antimatter storage, Kugelblitz black hole power.

## 4. Materials and ores

Two goals that work together:
1. **Standalone:** the mod ships every ore it uses, so it is fully playable with no other mods installed.
2. **Pack friendly:** every ore and material uses the shared common tags (`c:ores/uranium`, `c:raw_materials/uranium`, `c:ingots/uranium`, `c:dusts/uranium` and so on), and every recipe asks for the tag, never our own item. Material from All The Ores, Mekanism or Modern Industrialization works in our machines, and ours works in theirs.

### Ores we ship

| Ore | Real-world role in the mod | Overlaps with |
|---|---|---|
| Uranium | Main fuel | All The Ores, Mekanism, Modern Industrialization. Powah's uraninite is accepted as uranium too |
| Lead | Radiation shielding | All The Ores, Mekanism |
| Silver | Fission control rods | All The Ores |
| Aluminium | Solar sail film | All The Ores |
| Fluorite | Uranium processing | All The Ores, Mekanism |
| Salt | Sodium for breeder coolant, lithium from brine | All The Ores |
| Tungsten | Fusion plasma-facing wall (as in real tokamaks) | Modern Industrialization and others |
| Monazite | Thorium and yttrium source | Modern Industrialization (ours also yields thorium) |

Iron, copper, gold, redstone, coal and sand come from vanilla.

### Pack integration
- Each ore's worldgen can be switched off in the config, so pack authors can keep one source per ore.
- Worldgen is data driven, so vein size, height and rarity can be changed with a datapack.
- Duplicate items merge cleanly with unification mods like Almost Unified, since all tags match.

### Made, not mined

| Material | Role | Made from |
|---|---|---|
| Steel | Structure | Iron ingot and coal or charcoal in the alloy smelter (plus any mod's steel via tag) |
| Graphite | Moderator, fuel coating | Coal or charcoal block in a blast furnace (1 block makes 3 graphite) |
| Silicon carbide | TRISO fuel layer, hardened circuits | Sand and graphite in the alloy smelter |
| Silicon | Circuits and solar cells | Sand in a powered furnace |
| Lithium | Breeds tritium for fusion | Extracted from salt and water in the Lithium Extractor (real lithium mostly comes from brine) |
| Tritium | Fusion fuel | Bred from lithium target rods in the breeder's blanket |
| Heavy water | Fusion fuel (deuterium), better moderator | Separated from water in the Heavy Water Plant |
| Palladium | Hydrogen isotope membranes | Recovered from fission waste in the Waste Refinery |
| Liquid nitrogen | Cools the superconducting magnets | Separated from air in the Cryo Plant |
| Uranium glass | Decoration and light (glows green) | Glass and spare uranium from reprocessing |
| Sodium | Breeder coolant | Split from salt |
| Thorium, yttrium | Thorium fuel, superconductors | Extracted from monazite |

Deliberately skipped: zirconium and spodumene ores, and iridium and platinum. Accurate or useful, but not needed, and fewer ores means less clutter.

## 5. Progression spine

Rule 1: **no compression recipes.** A higher tier never needs a stack of the tier below. It needs different materials made by a new process.

Rule 2: **power is the price.** The machines that make the next tier's key materials run on the current tier's power, and they take a lot of it. Progress is paid for in energy, not in piles of ingots. You cannot make a key material until you can power its machine, and how fast you make it depends on how good your power is.

### Coolant is your friend
- Things run hot, and keeping them cool is a big part of the challenge, as in real power plants. Cooling scales with the machine: a strong core needs a serious water supply, and pushing it harder needs more.
- Water is free and endless, so it is spent generously: the station boils 4 times as much water as its first numbers, and 8 times in overdrive. Intake pumps cost little power (10 FE/t each), so the cost is building the supply, which adds to the scale of the plant. `fission_station.water_use_percent` scales it for packs.
- Better coolants come later (open question 21): each carries more heat per mB, so the same supply cools a stronger core.
- Real basis: a large nuclear plant moves tens of thousands of litres of cooling water a second.

### Power is the price
- **Two numbers per key machine.** The *energy per operation* is the price. The *minimum draw* is the gate: the machine only runs when it gets its full draw every tick. Its buffer holds only two ticks' worth, and a tick short of the full draw makes no progress while the power that did arrive is spent anyway, so a trickle cannot bank up and a big enough pile of the tier below cannot stand in for the tier itself. The screen says UNDERPOWERED and names the draw it needs. Real basis: a process with a threshold (a furnace's heat, a column's cold) gets nowhere below it but still burns what it is given.
- **The budget.** Building the next tier, and its first fuel, costs roughly one to two hours of a well-designed setup of the current tier at full output. A sloppy setup takes three or four times as long. So knowledge still beats volume (pillar 1): the best fission core already makes about four times what a careless one does, and that becomes four times faster progress.
- **Fuel is the brake on volume.** Structure is cheap (rule 3), so more power could mean more generators. What stops that is fuel: rods are finite, so what really wins is energy per rod, which is a layout skill.
- **Reward the clever player:**
  - Efficiency you can design for: staged machines (heavy water cascades), heat recovery, pre-cooled inputs. Each lowers the energy per operation.
  - Heat used directly: some machines can take station steam or heat, which is cheaper than turning it into FE and back. Real basis: cogeneration.
  - Timing: batteries charge while the base is quiet and spend in bursts. Battery discharge caps (3,000 FE/t per cabinet) stop them from beating a minimum draw for long.
  - Speed modules already square the power per tick, so going faster costs more energy per item.
- **No waiting as grind.** Anything long runs unattended, shows its progress and time left, and is fed by pipes, so the cost is building and tuning, not watching a bar.
- **Real basis:** the processes behind our key materials really are power hungry. Heavy water separation, air separation for liquid nitrogen, uranium enrichment and superconductor manufacture all rank among the most energy-hungry industries.
- **Packs:** power from other mods (Mekanism, Powah and the like) makes these costs easy in a big pack. That is fine: FE is FE. Each machine's energy cost and minimum draw scale in the common config, and the mod page says standalone play is the balanced experience.
- **Scope:** these rules apply from the fusion materials (section 10) onwards. Tiers 1 to 3 keep their tested alpha numbers for now and may be rebalanced later.

| Tier | Generator | Key component | New material and process | Circuit |
|---|---|---|---|---|
| 1 | Microreactor | Reactor heart | Steel, lead, graphite (blast furnace) | Basic control board: iron, copper, redstone |
| 2 | Fuel cycle machines (run on microreactor power) | Advanced board, MOX fuel | Silicon from sand and coal in the electric alloy smelter; fluorite for reprocessing | Advanced board: steel, gold, silicon |
| 3 | Fission reactor | Control rod assembly | Silver alloy control rods (real rods use silver, indium and cadmium); MOX fuel from tier 2 | Hardened board: silicon carbide and lead (radiation-hard electronics) |
| 4 | Breeder reactor | Sodium coolant loop, breeder fuel | Spent MOX pyroprocessed in molten salt into transuranic metal, on fission power; sodium split from salt using fission power (real fast breeders are sodium cooled); thorium from monazite | Hardened board |
| 5 | Fusion reactor | Superconducting magnet coil, plasma-facing wall | Tritium bred from lithium in the breeder's blanket; heavy water for deuterium; palladium from fission waste; yttrium from monazite and liquid nitrogen for the magnets; tungsten for the plasma wall (section 10) | Cryogenic board: superconducting wiring |
| 6 | Dyson swarm | Solar sail, receiver dish | Aluminium film sails; silicon solar cells | Photonic board |

Circuits follow their own logic: basic, advanced, radiation-hardened, cryogenic, photonic. Each tier's circuit needs something only the previous tier made possible.

### Steady state (rewarding automation)
A reactor that never stops runs better. Every reactor keeps a **steady-state streak**: how much fuel it has burned without downtime. The longer the streak, the more power it makes from the same fuel, up to a cap that grows with the tier. So a well-automated plant, refuelled by pipes the moment a core runs out, pulls ahead of one refuelled by hand. It rewards exactly the automation rule 3 asks for.

- **The streak** counts full fuel loads burned without downtime: one core for the microreactor, one load of every fuel channel for the station (about 30 minutes of running each, 40 for a microreactor core). It builds up with running time, so topping up or swapping part-used fuel gains nothing.
- **Downtime** is a reactor that should be running but is not, for more than 5 seconds:
  - its fuel runs out and is not replaced in time: the microreactor's core is spent or missing, or a planned fuel channel in the station holds a spent rod or none
  - a SCRAM, or an overheat
  - switched off by hand or by redstone (a pause for maintenance still counts, so refuelling slowly with the reactor off gains nothing)
- **Paused, not broken:** while its energy buffer is full and it stands by (follow load), while its chunk is unloaded, and while the server is off. None of these are the player's fault.
- **Decay, not reset:** each downtime costs a quarter of the streak, so one slow swap or a power cut after a long run is a setback, not a disaster. Repeated misses compound. A meltdown ends it, as it ends the reactor. Breaking the reactor to move it loses the streak.
- **The bonus** raises electrical output only. Fuel burns at the same rate and the core makes the same heat, so water use and cooling do not change: it is more energy per core, which is efficiency. Each tier has its own cap, reached along the same curve:

| Streak (fuel loads) | Share of the tier's bonus |
|---|---|
| 32 | a quarter |
| 128 | half |
| 512 | all of it |

Between milestones it grows smoothly, and it starts climbing from the first load.

| Tier | Cap | At 32 loads | At 128 | At 512 |
|---|---|---|---|---|
| Microreactor | 1.5x | 1.125x | 1.25x | 1.5x |
| Fission station | 2x | 1.25x | 1.5x | 2x |
| Breeder | 2.5x | 1.375x | 1.75x | 2.5x |
| Fusion | 3x | 1.5x | 2x | 3x |

- **Showing it:** the reactor's screen shows the streak, the bonus now and the next milestone. The announcer calls each milestone ("steady state reached", which needs a new recording), and each tier's cap is an advancement.
- **Real basis:** CANDU reactors are refuelled while running, which is much of why they run so much of the time. A reactor that stops loses more than the stop itself: xenon-135 builds up after shutdown and holds a restarted core back for hours (the "iodine pit"), and every stop and start fatigues the vessel. The honest fudge is the size of the bonus: think of it as the core settling into its best equilibrium the longer it runs undisturbed.
- **Balance:** it makes the current tier's power better, so progress is faster (see Power is the price), but each tier's cap stays below what the next tier makes, so a tier is never skipped. The milestones are a first pass: 512 station loads is some 250 hours, a long-term trophy rather than a target, and play-testing will tell whether the curve should be shorter. Config: `steady_state.enabled`, the cap per tier, and a scale for the milestone counts.

## 6. Tier 1: Microreactor

Inspired by the new generation of transportable microreactors. It gets its own original name and look in the mod, not a real product's name. Meant to feel like a basic early machine, in the spirit of Oritech's small early multiblocks: simple to build, no layout rules to learn.

### Structure
- 4 blocks: 1 wide, 2 deep, 2 high. Fits in the corner of a starter base with room for pipes and cables.
- Made of 1 **reactor heart**, 2 **reactor machine units** and 1 **coolant block** (name pending).
- The heart can go in any of the 4 positions; the units and coolant block fill the rest.
- The coolant block is the player's first introduction to coolant blocks, which become a proper building block in later reactor tiers.
- All faces are the outside of the machine, so there is no hidden "core" block to place awkwardly inside.

### Assembly
- When the fourth block is placed, the structure snaps together: short animation, particles, sound, and the 4 blocks render as one machine model.
- Breaking any block drops it back to separate pieces.
- Ghost preview: while holding any part, each unformed heart nearby shows outlines where the other three blocks go, with the suggested part floating in each (machine units above and behind the heart, the coolant jacket at the upper back under the intake). Filled spaces turn green, blocked ones red.

### Input and output
- Like Oritech's multiblocks, the assembled model has fixed, visible connection chutes. Pipes and cables only connect at those points, not on any face.
- The machine faces the way the player was facing when they placed the heart, so the chutes always end up in predictable spots.
- Port layout (machine facing north):
  - **Energy out:** cable socket on the back face of the lower back block. The machine pushes energy out, so it works with cables that do not pull (Mekanism's included).
  - **Coolant in:** intake chute on the top face of the upper back block. Accepts water only, and pipes cannot drain it.
  - **Steam out:** outlet on the right-hand face of the lower back block. The water the reactor boils leaves as steam (10 mB of steam per mB of water, config), pushed out of the port into a gas pipe or pressure tank. With nothing connected it blows out as a visible plume; running dry there is no steam and no plume. Steam will drive turbines at the next tier.
  - **Fuel in:** on the lower front block's flank to your left as you face the front (inputs are on that side for every machine). Fresh cores go in when the slot is free; nothing comes out.
  - **Spent cores out:** on the lower front block's flank to your right as you face the front. The reactor pushes a spent core out into whatever is beside it, the way the energy port pushes power, and pipes can pull it too. Only spent cores ever come out, so automation never pulls a core with fuel left in it.
  - Two ports rather than one hatch, so fresh and spent cores travel on separate pipe lines. On one shared line, a pipe carrying fresh cores to a full reactor would deliver them to the spent-core store instead.
- Every port is a 10x10 flange flush with the block face, centred, around an 8x8 socket. That seats the common 6x6 pipes and cables (Pipez, Mekanism) and their 8x8 end plates. The ring colour says what it carries: red energy, blue coolant, white steam, orange items (fuel).
- Ports use the standard NeoForge energy and fluid capabilities, only on the port face and only while formed.
- Front face carries the display panel. Right-click any block to open the GUI.

### Running it
- The heart holds one sealed fuel core. No topping up. Steady modest power for a long time, then it depletes.
- The core carries its own burn time, so it can be taken out and put back without losing fuel. When spent it turns into a depleted core in the slot.
- Heat model: the core makes a fixed thermal power, and a heat engine turns part of it into FE. Efficiency is 55% of the Carnot limit, 1 - T_cold / T_hot. With water the cold side is near 20°C; dry, the jacket sheds heat to hot air, so the cold side sits near 250°C. Each mode's efficiency is the Carnot figure at its own operating temperature, scaled by warm-up (full from 400°C), so dry is always lowest, water higher, overdrive highest, even while a hot core is cooling down.
  - Dry: settles at 700°C, about 25% efficient, 200 FE/t (the config value).
  - Water: settles at 450°C, about 33% efficient, about 257 FE/t, boiling off 1 mB per tick.
  - The GUI shows efficiency live, with a bar coloured by mode.
- Safety override (overdrive): the interlocks can be switched off (shift-click, on purpose). The control rods come further out for 30% more fission power, and the core settles at 650°C. The steam leaves superheated, so the turbine reaches 66% of the Carnot limit instead of 55%: 45% efficient overall, about 460 FE/t (nearly double the water-cooled output). The cost: double water use and 30% faster fuel burn. Real basis: supercritical power stations reach about 45% with superheated steam.
  - Lose the water in overdrive and the core runs away (2°C per tick). An alarm sounds, the status flashes COOLANT LOSS and smoke pours out. At 1000°C it melts down: the machine and core are destroyed in an explosion (power 5 by default).
  - The player has about 9 seconds to switch off, re-arm the safeties or add water. In follow-load mode a full energy buffer also saves it, since the reactor stands by; in dump mode it does not.
  - Config: meltdowns can be turned off, in which case the interlock re-arms itself on coolant loss. With the safeties on, the microreactor can never melt down.
  - Real basis: a loss-of-coolant accident with the automatic scram disabled. The explosion is an honest fudge for a steam explosion.
  - A meltdown leaves a radiation site that fades over 20 minutes (see section 13).
- Excess power, a toggle in the GUI:
  - Follow load (default): when the energy buffer is full the reactor stands by and saves fuel.
  - Dump: it keeps running on a full buffer and vents the surplus as a steam plume from the steam outlet. It burns fuel nonstop, so it keeps making depleted cores (and later by-products) with no power consumer attached. Real basis: the steam dump (turbine bypass) valves that let a pressurised-water reactor run on when the turbine cannot take its power.
- Running shows: Cherenkov glow in the porthole (emissive, block light 9), a low hum, steam wisps from the coolant intake.
- GUI: fuel left, temperature, output, energy, coolant, on/off. Redstone controllable.
- Balance lives in the common config: output (200 FE/t default), fuel life (48,000 ticks, 40 minutes of running), coolant use.

### Crafting (first pass, being play-tested)

Target: buildable in the early game, after iron and a blast furnace, before diamonds. The structure is cheap; the fuel is where the cost sits.

Machine:
- **Alloy smelter:** the mod's first machine. A furnace with two input slots that burns ordinary furnace fuel, since there is no power yet. Crafted from a furnace, bricks and iron. Pipes connect like a furnace: inputs on top, fuel on the sides, output from the bottom.

Intermediates:
- **Steel:** iron ingot plus coal or charcoal in the alloy smelter.
- **Graphite:** coal or charcoal block in a blast furnace, 3 per block. A fuel core needs 7 graphite, so about 21 coal or charcoal.
- **Basic control board:** 2 redstone, 1 copper and 3 iron (`RCR / III`). Tagged `c:circuits/basic`, so Mekanism's basic control circuit works in its place.
- **Silicon carbide:** sand plus graphite in the alloy smelter (a blast furnace only takes one input).
- **TRISO pellets:** 1 uranium ingot, 2 graphite and 1 silicon carbide make 2 pellets. Based on real TRISO fuel: a uranium kernel coated in carbon and silicon carbide layers.

Fuel note: the real Unity microreactor that inspired this uses standard uranium dioxide fuel with helium coolant and water as the moderator. We use TRISO on purpose, as used by other microreactor designs, because it gives the fuel cycle a real reason for the cracking step.

Blocks:
| Block | Qty in structure | Recipe |
|---|---|---|
| Reactor heart | 1 | 4 lead, 2 steel, 1 graphite, 1 glass, 1 basic control board (`LGL / S#S / LBL`) |
| Reactor machine unit | 2 | 2 steel, 2 copper, 1 lead (`.S. / CLC / .S.`) |
| Coolant jacket | 1 | 8 copper and a water bucket, which comes back empty (`CCC / CWC / CCC`) |

Whole machine: 6 lead, 6 steel, 12 copper, 1 graphite, a board, glass and a bucket of water. Cheap enough before diamonds; the lasting cost is uranium for fuel.

Fuel:
- **Sealed fuel core:** 4 TRISO pellets packed round graphite inside a 4-steel shell (so 2 uranium per core). Goes into the heart. When depleted it is swapped out whole, and the old one becomes the depleted core for the fuel cycle.

Uranium is rare in All The Ores worldgen, so fuel cores should be where most uranium goes, and a single core should last a long time.

### The hook
When the core runs out, the player gets a depleted core they cannot use yet. That curiosity pulls them into the fuel cycle.

## 7. Tier 2: Fuel cycle

Real basis: spent uranium fuel is roughly 95% uranium, about 1% plutonium and about 4% fission products (the actual waste). Microreactor fuel is TRISO (tiny kernels sealed in ceramic and carbon), which is famously hard to reprocess. That gives us a real reason for an extra step. The lesson of the tier: spent fuel is the next fuel.

The loop at a glance: a depleted fuel core goes through the **Core Cracker** (spent kernels out, some graphite and steel back), then the **Reprocessor** (with fluorite and water: uranium, plutonium and waste out), then the **Fuel Fabricator** (TRISO pellets, uranium rods and MOX rods). Spent uranium rods from the fission station skip the cracker and go straight to the reprocessor. Waste goes into a **Waste Cask**.

### Machines
All four follow the electric smelter's pattern: a 3D model in the house style, FE in at a back port (red ring), item pipes in on top and out at the bottom (orange rings), a GUI with power and redstone buttons, JEI and EMI pages, wrench rotation. Recipes are data driven, one recipe type per machine (`ryzergen:cracking`, `ryzergen:reprocessing`, `ryzergen:fabricating`), so packs can change them.

| Machine | Size | Power | Time per job | Notes |
|---|---|---|---|---|
| Core Cracker | 1 block | 20 FE/t | 10 s | A jaw crusher behind a steel door |
| Reprocessor | 2 high | 80 FE/t | 20 s | Water in at the side (blue ring); a tall lead-lined column with a sight glass |
| Fuel Fabricator | 1 block | 40 FE/t | 10 s | Up to three inputs; a press and a rod loading tray |
| Waste Cask | 1 block | none | none | Storage only |

Inputs only take items some recipe uses, and never the same item in two slots, so one pipe cannot fill every slot and jam a machine. When two recipes match, the one using more ingredients wins: a fabricator holding uranium, steel and plutonium makes MOX, not a plain uranium rod. The GUI shows a status line (running, no power, needs water, output full, held by redstone).

Speed modules (upgrade, don't replace: rule 11): each machine has one upgrade slot holding up to 4. Each module adds another full speed, so 4 make a machine five times as fast. Power per tick grows with the square of the speed, so the energy per item grows with the speed: a fully upgraded reprocessor draws 2,000 FE/t, more than a microreactor makes, which gives fission power a job. Recipe: 4 redstone, 2 copper, 2 gold and an advanced board. Real basis: pushing more material through the same kit takes more power.

Power gating: all of them run on microreactor power. Together they draw about 140 FE/t, so one water-cooled microreactor (about 257 FE/t) runs the whole chain alongside an intake pump and the electric smelter.

Throughput: one reprocessor does 180 jobs an hour and one fabricator 360, against the 40 or so rods an hour a uranium station burns, so one of each keeps up with a station.

### Recipes (first pass)
**Core Cracker** (`ryzergen:cracking`)
- Depleted fuel core: spent kernels, 2 graphite and 2 steel (the shell and packing, recycled). Real basis: TRISO has to be crushed open before it can be processed.

**Reprocessor** (`ryzergen:reprocessing`)
- Spent kernels, 1 fluorite and 250 mB water: 1 uranium ingot, 1 plutonium nugget, 1 fission waste. A core took 2 uranium to make, so reprocessing gives half back.
- Spent uranium rod, 1 fluorite and 250 mB water: 1 uranium ingot, 3 plutonium nuggets, 1 fission waste. A station rod holds far more fuel than a microreactor core.
- Real basis: fluoride volatility, where fluorine turns uranium into a gas so it can be separated (a real alternative to the nitric acid PUREX process, and it uses the fluorite we ship). Real plants make hydrogen fluoride from fluorite and acid; fluorite and water stand in for that, an honest fudge.
- Spent MOX rods do not go in here: see tier 4.

**Fuel Fabricator** (`ryzergen:fabricating`)
- 2 uranium ingots and 1 steel: 1 uranium fuel rod. Real basis: fuel pellets stacked in a sealed metal tube. Real cladding is zirconium alloy; we skip zirconium and use steel, as early reactors did.
- 1 plutonium ingot, 1 uranium ingot and 1 steel: 1 MOX fuel rod. Real MOX is about 7% plutonium mixed into uranium.
- 1 uranium ingot, 2 graphite and 1 silicon carbide: 3 TRISO pellets. The crafting grid makes only 2 from the same, so the fabricator pays for itself on microreactor fuel.
- Uranium balance: a uranium rod costs 2 uranium and gives 1 back, so every rod burned costs 1 uranium net. MOX costs 1 more uranium. Three uranium rods and the MOX rod they make come to about 160 million FE for 4 uranium, where a microreactor core gets about 18 million from 2.

**Waste Cask**
- Holds 1,024 fission waste (16 stacks), filled by pipe or hopper from any side. A gauge on the side shows how full it is.
- Keeps its contents when broken, like a shulker box, so a full cask can be carried off and stacked in a store. Nothing is lost and nothing leaks.
- One cask holds about a day of a uranium station's waste (40 an hour). If the reprocessor's output backs up it simply pauses, like any machine.
- Real basis: dry cask storage, where spent fuel sits in sealed steel and concrete casks.

### Spent Fuel Pool (the fuel cycle's first multiblock, being built)
Spent fuel comes out of a reactor too hot to handle: its fission products keep decaying and give off heat for years. Real plants lower it into a deep pool of water, where the water carries the heat away and shields the radiation, and only reprocess it once it has cooled. The pool makes that a real step. Concept: `art/tools/pool_concept.py`.

- **Required.** Reactors mark the depleted cores and spent rods they give out as *hot*. The Core Cracker and the Reprocessor refuse hot items, so everything spent passes through a pool first. Items already in players' worlds carry no mark, so they count as cooled and nobody's store is stranded (a changelog note). Config `fuel_cycle.require_cooling` turns the mark off for packs that want the old loop.
- **Shape:** 5 long, 3 wide and 3 high. Every block face is one framed plate, with a pixel of graphite frame on each block edge, and every feature is centred on its plate. The front has two windows (a pane per block face, lower two rows) onto the racks, the trefoil signs on its end bays and the controller's console in the middle. Inside, two rows of racks on the floor with the stored fuel glowing Cherenkov blue under the water, and a yellow gantry crane on rails inside the rim (a block entity renderer, so it can move later). The back wall is set in, and the cooling loop sits in the recess: a return line, a pump, a heat exchanger and mitred copper pipe.
- **Parts (rule 3):** Pool Liner blocks are the cheap bulk; the cost is in the Pool Controller (front middle, middle row) and the Pool Crane (top middle).
- **Building:** as the fission station: place the controller on the ground, a ghost outline shows the shape, feed the controller the parts and it builds the pool itself round it. When the last part is in, the controller swaps with the liner above it, so the formed pool has its console on the middle row. A pool broken apart and rebuilt keeps its controller there and builds from it.
- **Running it:** 18 rack slots, one item each. An item cools for a set time (first numbers: 5 minutes for a microreactor core, 10 for a station rod, config), then moves to the output slots. The GUI shows each slot's time left, the water level and the output.
- **Water:** a 16,000 mB tank. Decay heat boils water off at about 2 mB/t per hot item (rule 12). Cooling runs only while the water covers the racks (half the tank); below that the timers pause. Nothing melts and nothing radiates: rule 10 keeps radiation to running reactors and meltdowns. The formed model shows the pool dry, full, or full with fuel glowing in the racks.
- **Ports:** inputs on your left as you face the front (hot fuel and water in, on the east end for a pool facing north), cooled fuel out on your right (the west end). Capabilities only on the port faces, only while formed.
- **Marking:** a multiblock has room for an authentic radiation trefoil, so the pool carries two.
- Real basis: every nuclear plant stores spent fuel in a pool for years before it goes to dry casks or reprocessing. Our minutes stand in for years, an honest fudge.

### Machine recipes (first pass)
- **Core Cracker:** 4 steel, 2 pistons (the jaws), 2 iron and 1 basic control board. It is the first tier 2 machine, needed the moment the first core runs out, so it takes a basic board.
- **Reprocessor:** 4 lead (shielding), 2 advanced control boards, 2 steel and a cauldron (the dissolver).
- **Fuel Fabricator:** 4 steel, 2 pistons (the press), 2 copper and 1 advanced control board.
- **Waste Cask:** 4 steel, 4 lead and a barrel.

### Fuel and plutonium (no grind)
- A MOX rod needs one plutonium ingot. MOX makes twice the power of uranium, so it has to cost more than a uranium rod, which is easy to make in bulk in a pack with quarries.
- The ingot is not a compression recipe: the nuggets come from a new process (reprocessing), not from a pile of the tier below. It is also close to real: spent fuel is about 1% plutonium and MOX about 7%, so real MOX takes several spent rods' plutonium per fresh rod.
- The yield is set so two stations pair up: 3 spent uranium rods make 1 MOX rod. A uranium station in its best power layout spends about 40 rods an hour (enough for about 13 MOX rods), and a full MOX station about 10. So one station burns uranium to breed plutonium and a second runs on the MOX it makes, with a little to spare (more with the breeder station in overdrive). Microreactor cores still give 1 nugget each, so early plutonium comes slowly.
- The loop grows as you scale: microreactor cores start it, spent fission rods feed it, and the tier 4 breeder makes plutonium from uranium without burning a station's worth of rods.
- Rod lives and yields are first numbers, tuned in play-testing.

### New materials
- **Silicon:** sand and coal in the electric alloy smelter only (the fuel-burning smelter cannot get hot enough). Real basis: carbothermic reduction, how silicon is made.
- **Advanced control board:** steel, gold and silicon. Tier 2's key component, with MOX.
- **Plutonium** ingot and nugget (`c:ingots/plutonium`, `c:nuggets/plutonium`), **spent kernels**, **fission waste**, **MOX fuel rod**, **uranium fuel rod**.

### Unlocking fission (natural gating)
No hard locks. The fission reactor needs the microreactor's by-products to build and to run: its parts need advanced boards and products of the fuel cycle, and its only fuel comes from the Fuel Fabricator. Advancements mark each step, for packs to hang quests on: cracking the first core, "Closing the Loop" for the first plutonium, and the first MOX rod.

## 8. Tier 3: Fission power station

A fixed-size station, built like the microreactor: place the parts inside a ghost outline and it snaps into one large machine. The dream is that a player builds two or three before moving on, and their base grows a real power plant skyline, each station with its own chimney puffing steam.

### Shape
- A round tower, 12 blocks across and 11 high, on a round base.
- **Base:** the ports, all at ground level: water in, energy out, fuel in and spent fuel out.
- **Reactor chamber:** a glass ring round the core, so the rods glow through it (Cherenkov blue-green) while it runs.
- **The roof is one giant turbine:** a rotor as wide as the station spins above the reactor head, seen through open louvres. Steam from the core drives it, then rises up the station's outer wall, which carries on above the rotor as a hyperbolic steam stack, open at the top and pouring steam. All the steam goes to the turbine; no steam port.
- Block map (facing north; the control core sets the facing): layer 0 a 12-wide ring of casing with the control core in the middle of the front and the ports on the two flat sides; layers 1 to 4 a ring of station glass; layer 5 casing (the reactor head); layers 6 and 7 casing (the turbine band) with the turbine rotor in the middle of layer 6; layers 8 to 10 an 11-wide ring of casing (the stack). About 340 blocks, open inside.
- Formed, the parts stop drawing themselves and the control core draws the whole station from the design in `art/tools/fission_concept.py` (exported to `assets/ryzergen/station/fission_station.json`).

### Building it
- Cheap structure (rule 3): mostly casing and glass, plus a few key parts: a control core (advanced boards), turbine rotor, generator and the port blocks. The ghost outline shows what goes where. A few hundred blocks, only a few of them costly.
- The real cost is what goes in it: fuel, moderators and control rods.

### The core (where the design depth is)
Size is fixed, so the lever is the core layout (rule 1: knowledge, not volume). The core is a small grid of channels (about 5 x 5) filled in the GUI:
- **Fuel rods:** uranium (burns fast; spent rods reprocess into plutonium) or MOX (hotter, lasts much longer). See section 7.
- **Moderators:** slow neutrons so the fuel beside them burns hotter and more efficiently. Graphite first; rarer moderators later give more (as Extreme Reactors rewards better blocks next to its rods). Which materials, and how realistic to be, is decided later. Real options include heavy water and beryllium.
- **Control rods:** silver alloy (tier 3's new material), soak up neutrons to tame a hot layout.
- **Coolant channels:** carry heat away to make steam.
- **Target channels:** hold lithium target rods, which breed tritium for fusion from the fuel beside them, at the cost of 20% of that fuel's heat (section 10). Fully bred rods leave by the output port.
- Channels are planned, then filled: pick a tool in the control screen (fuel, moderator, control rod, coolant) and click channels to plan them. Moderators (graphite blocks) and control rods go in once; fuel channels take uranium or MOX rods, by hand or through the fuel port, and spent rods come back out of the port. The grid is shown as seen from the station's front.
- Channels start empty, so every coolant channel is a choice. Right-click a channel to take its item out, and again to clear its plan.
- First numbers (being play-tested, `fission_station.output_percent` scales them): a uranium rod makes 1,000 thermal FE/t and lasts 30 minutes at a steady burn, MOX 2,000 and 2 hours. MOX also lets each coolant channel carry more heat, up to twice as much in an all-MOX core (scaled by its share of the fuel rods), so a full MOX core plays like the same uranium core at double the power. Real basis: plutonium-bearing fuel runs at a higher power density, and hotter fuel drives more heat into the water; the factor of two is a fudge so closing the fuel cycle pays.
- Neighbours change a rod: each moderator adds 40% heat at no extra burn (better neutron economy, so moderators are how you get more energy per rod); each neighbouring fuel rod adds 20% heat and 20% burn; each control rod takes 35% off both (so control rods trim a layout that runs too hot).
- Coolant is local: a rod's heat goes, shared evenly, to the coolant channels touching it. Each channel carries up to 3,000 thermal FE/t as steam, boiling 1 mB of water per 50 (per 25 in overdrive, where it boils off hotter). Coolant is your friend (section 5): a strong core drinks several intake pumps' worth of water, and overdrive twice that. Heat with no coolant beside it, or more than a channel can carry, stays in the core and heats it until it SCRAMs.
- The core settles at a temperature set by how hard its coolant works: 150°C idle, 600°C at full load. The turbine is 25% efficient at 150°C, up to 40% at 600°C (Carnot). So the most power sits just under the coolant's limit.
- The GUI works out the layout as you plan it: planned power (or "too hot" with the heat left uncooled), the settled temperature and efficiency, how long rods last, and a rating against the best layouts found. Each fuel rod shows a heat bar, each coolant channel a load bar, and uncooled rods or overloaded coolant flash red. No separate planner block is needed. A light puzzle, not a spreadsheet.
- Best layouts found (`art/tools/core_optimiser.py`, a simulated-annealing search, at 100% output): uranium for power 8,006 FE/t (15 rods, 544°C, 38%); uranium for economy 2,713 FE/t from 4 rods (822 FE/t per rod burning); MOX for power 16,012 FE/t (the uranium layout doubled, 544°C); MOX for economy 2,291 FE/t from 2 rods. Rerun it and update `StationReactor.BEST_URANIUM` and `BEST_MOX` whenever these numbers change.
- Rods show in the chamber, glowing, in the layout you chose.
- While it runs, the chamber glass gives off light (15, like a torch), so the station lights up the ground round it, and a green haze drifts up through the chamber (uranium glass glows green). Minecraft light has no colour, so the light itself is white. The haze is only for looks: the first thing to cut if stations cost too much on the client.

### Running it
- Heat from the core boils the water fed in at the base; steam drives the turbine; FE comes out of the base. Real basis: a boiling water reactor, which boils its coolant in the core and sends the steam straight to the turbine.
- Output target: a few thousand FE/t per station, so two or three run a mid-game base. Scaling targets across tiers, set against ATM10 (default configs): microreactor 200 to 460 FE/t, fission 2,000 to 20,000, breeder 20,000 to 100,000, fusion 100,000 to 1,000,000, the swarm above that.
- Too hot with too little water, the core heats up; at 900°C an automatic SCRAM stops the reaction until it cools below 400°C. The SCRAM trips the turbine too: while the core cools, its steam goes round the turbine to the condenser, so it makes no power and the heat it built up is lost. Overloading a core is a real loss, not a free ride. Real basis: a reactor trip trips the turbine, and the decay heat is dumped through the steam dump valves.
- Coolant is local in the running station as on the control screen: heat that no coolant channel can reach (a rod with none beside it, or more than a channel carries) stays in the core, and the other channels do not carry it off, so a core the screen calls too hot climbs until it SCRAMs (or, in overdrive, melts down).
- Ports on the base, on the two flat sides so the front (the core's screen) stays clear of pipes and cables. As you face the front, inputs are on your left: water in and fuel in (the fuel port takes anything a channel is planned for: rods, graphite, control rods). Outputs are on your right: energy out, and the output port (orange ring), which pushes spent rods into whatever is beside it and lets pipes pull from it. Later waste leaves the same way. Inputs and outputs on opposite sides keep the pipe feeding fresh rods apart from the one carrying spent ones away (the microreactor's fuel ports taught the same lesson).
- Breaking the core drops its rods and blocks.

### Overdrive (safeties off)
- The same safety switch as the microreactor, shift-clicked on purpose. The control rods come further out: every rod makes 30% more heat and burns 30% faster, each coolant channel carries 30% more (a hotter core drives more heat through it), and the steam leaves superheated, so the turbine reaches 45% instead of 40%. The same layout gives about half as much power again (best uranium about 11,600 FE/t). No SCRAM.
- The catch: every fuel channel must hold a live rod. A spent or empty fuel channel starts a flux tilt. After 10 seconds' grace (time to swap a rod by hand) the status flashes FLUX TILT with a countdown, and a slow alarm sounds. Refill it within 5 minutes (`fission_station.flux_tilt_seconds`) and all is well. Otherwise the core goes UNSTABLE: it heats by 1°C a tick without limit, smoke pours out and the alarm climbs in pitch. Only re-arming the safeties saves it then (that SCRAMs the core). At 1000°C it melts down: the station and its contents are destroyed. The blast (power 8 by default) is followed by a crater far bigger than a vanilla explosion digs: a scorched bowl 24 blocks in radius (`fission_station.meltdown_crater_radius`, 0 for the blast alone), its floor blackstone, basalt and magma with fires burning, the ground round the rim scorched. It leaves a radiation site of strength 500 that fades over 40 minutes. The crater goes through NeoForge's explosion event, so land claim mods can protect their blocks.
- Overheating in overdrive ends the same way, since nothing SCRAMs, and much sooner: a core with no coolant reaches 1000°C in about a minute. A steady overdrive core never passes 600°C, so above 620°C the status shows MELTDOWN RISK (it outranks the flux tilt countdown) and the alarm climbs with the temperature.
- A reward for good automation: an output port pushing spent rods out and a pipe feeding fresh ones in keeps the channels full.
- Config: `fission_station.meltdowns` off makes the interlock re-arm itself and SCRAM instead of melting down.
- Real basis: power peaking around a gap in the fuel, with too little control rod margin to hold it. The explosion is an honest fudge for a steam explosion.
- Radiation while running is far stronger than the microreactor's, which makes a station a no-go zone for mobs (and a mob farm, if you shield it well).

## 9. Tier 4: Breeder and thorium

Spent MOX is the way in. The lesson of the tier: what one reactor cannot burn, another can.

**Two reactors, two jobs.** The fission station runs the base; the breeder is the utility reactor. It makes fuel for the stations and the tritium that starts fusion, so fusion cannot be reached without it (settled 30 Sep 2026). Its power is a bonus, not its point.

Real basis: every pass through a thermal reactor builds up heavier plutonium isotopes (Pu-240 and up) plus americium and curium. They soak up slow neutrons, so spent MOX cannot be recycled again in a moderated reactor like the fission station. France stores its spent MOX for this reason, waiting for fast reactors, whose unmoderated fast neutrons split those heavy isotopes. So spent MOX is a dead end for the station and the starting fuel for a breeder.

### The chain
1. **Spent MOX rods** come out of the fission station like spent uranium rods, but the tier 2 Reprocessor cannot take them.
2. **Electrorefiner** (the pyroprocessor, named 30 Sep 2026): spent MOX rods, salt and power become transuranic metal (plutonium, americium and curium together, never separated, and kept that way unless a use appears), a uranium ingot and fission waste. Real basis: pyroprocessing, where spent fuel is dissolved in molten salt and the metals are plated out by electrolysis in an electrorefiner. It was designed for fast reactor fuel, keeps the transuranics together (harder to misuse), and uses the salt we ship. It runs on fission power and draws a lot of it (power gating). The same molten salt cell has a second job: salt alone splits into sodium (the chlorine is vented), which is how real sodium is made. One machine for both keeps the road to fusion short. Numbers (first pass): a minimum draw of 1,500 FE/t, beyond any microreactor; a spent MOX rod and 1 salt take 30 s (900,000 FE), 2 salt into a sodium ingot 10 s (300,000 FE). With a rod in it the cell refines the rod; with salt alone it makes sodium. A sodium ingot fed back in melts in the cell's molten salt (sodium melts at 98 C, the cell runs near 500 C) into 250 mB of liquid sodium in its tank, 2 s; a generous fudge on volume, so filling the breeder's loop is no grind (settled 1 Oct 2026). The processing machines gained an output tank for this: a recipe may give a fluid result, drained by pipes on any side or pushed by auto output.
3. **Breeder fuel:** transuranic metal made into fast reactor fuel. It is the only fuel that starts a breeder core, so running MOX is the gate into tier 4. Made in the Fuel Fabricator: transuranic metal, a uranium ingot and 2 steel, 15 s. Real basis: EBR-II's metal fuel, a uranium and plutonium alloy in steel-clad pins (ours leaves out the zirconium).
4. **Breeder reactor:** a sodium-cooled fast reactor. No moderator: fast neutrons burn the transuranic fuel, and a blanket round the core catches the spare ones. It makes more neutrons than the chain reaction needs, and the blanket decides what the spare ones make. Needs more active management than the station (sodium flow, temperatures), with optional redstone or computer control.
5. **The blanket, three choices** (the tier's layout decision, as the station's core is tier 3's):
   - **Uranium:** breeds plutonium, which goes back into MOX rods, so once a breeder runs, MOX no longer needs a uranium station burning rods to feed it (rule 3: no grind). It makes more fissile fuel than it burns.
   - **Thorium** (from monazite): breeds uranium-233 instead. Thorium is not a fuel itself; U-233 is an alternative fuel with its own trade-offs.
   - **Lithium:** lithium target rods breed tritium, the fuel that starts fusion (section 10). The only source of tritium until a fusion reactor's own blanket breeds enough.
   Every blanket slot given to one is a slot not given to the others, so the breeder's layout is a choice between fuel for fission and fuel for fusion.
   The blanket is a ring of slots round the fuel in the breeder's own planning grid, planned on its control screen like the station's channels (settled 30 Sep 2026).
6. **Spent breeder fuel closes the loop:** it goes back through the pyroprocessor into transuranic metal and fission waste, so a breeder can keep itself fuelled once started. Real basis: fast reactor fuel cycles are designed to close this way (settled 30 Sep 2026).

### The core (first pass, 1 Oct 2026; numbers to play-test)
`machine/breeder/BreederReactor` (the sums, shared with the screen) and `BreederRunner` (running it).
- **A hex core of 19 positions**, as fast reactor cores are laid out: a centre, a ring of 6, an outer ring of 12. Each is planned on the control screen as fuel, a control rod, a uranium blanket or a lithium target, or left empty, and filled by hand or through the fuel port (each item goes to a position planned for it). Real basis: the hexagonal assemblies of EBR-II and Dounreay.
- **Heat:** a breeder fuel assembly makes 10,000 thermal FE/t, 10% more for each fuel assembly beside it (fast neutrons shared across the core) and 30% less for each control rod; it burns faster or slower in step and lasts an hour at 100%. No moderator: the neutrons stay fast, which is what lets it breed. A full core of 7 (centre and inner ring) makes about 94,000 thermal FE/t.
- **Blankets breed for free:** each blanket breeds at a rate set by the heat of the fuel beside it (a tenth of it, in the station's target units: about 20 minutes beside two assemblies). It costs the fuel nothing, since those neutrons would otherwise leak out, so the choice is which blanket and where. Uranium blankets (2 uranium and a steel in the Fuel Fabricator) become bred uranium blankets, which the Electrorefiner turns into 3 plutonium nuggets and a uranium ingot (for MOX; a spent uranium rod's worth of plutonium, toned down from a whole ingot on 3 Oct 2026). Lithium target rods become irradiated target rods (tritium for fusion). Thorium (U-233) waits for monazite processing.
- **The sodium loop:** 4,000 mB of liquid sodium, closed (never used up), filled through the sodium port; the reactor will not start until it is full, and a part-filled loop carries less. Cooling is not local, as the station's is: the sodium flows through the whole core and carries up to 100,000 thermal FE/t at 100% flow, set on the screen in 5% steps. The pumps draw 4,000 FE/t at 100%, as the square of the flow. As in the station, the harder the sodium works the hotter the core settles (300 to 550°C) and the more efficient the steam plant (30 to 42%), so the best flow carries the heat with little to spare. Too little flow and the core heats: overheating from 600°C, SCRAM at 650°C until below 450°C. Real basis: a sodium-cooled fast reactor's primary pumps, and its scram.
- **Output:** a full core at the right flow gives about 36,000 FE/t (the low end of the tier's 20,000 to 100,000 target, room for the steady-state streak later). `breeder.output_percent` scales the heat, the loop and the pumps together, so layouts and flows play the same.
- **Ports** (on the side consoles, inputs on your left as you face the front): fuel in (fuel, control rods and blankets, into planned positions), liquid sodium in; out on the right, spent fuel and bred blankets (pushed to whatever is beside the port, or pulled by a pipe), and energy (pushed, or pulled by a cable).
- **Spent breeder fuel** comes out hot (cool it in the pool), and the Electrorefiner turns it and a salt back into transuranic metal, a uranium ingot and fission waste, so a running breeder keeps itself fuelled with only steel and salt added.
- Still to come: the sodium fire (its failure), the steady-state streak, sounds and the announcer, advancements, and JEI and EMI pages.

### Shape (concept 30 Sep 2026, base reworked 1 Oct 2026)
Concept in `art/tools/breeder_concept.py` (textures `breeder_textures.py`, render `art/concepts/breeder.png`). 9 blocks across and about 8 high. In game it is a formed multiblock drawn from this design (`BreederRenderer`), white like the station.
- **A steel sphere on legs.** Real basis: the Dounreay Fast Reactor, a sodium-cooled fast breeder, was housed in a great steel sphere, and the shape is the refinery's Horton sphere: welded plates standing on a ring of legs with cross bracing. The core sits in a pool of liquid sodium inside, sealed from the air.
- **Sealed, not seen through.** Sodium is opaque, so there is no glass: the station is the reactor you watch, the breeder the one you run. Smooth off-white painted plates (its own texture, not the machines' framed panel) in ten latitude bands, 16 round the middle and 8 nearest the poles, joined by thin weld seams running pole to pole as a sphere of tapered plates is built.
- **The girder belt** at the equator, where the 12 legs meet the sphere: a graphite girder (flanges, bolt rows, a stiffener per panel), with a cyan light strip and the radiation trefoil on the front (multiblocks may carry one). The legs are round, as a sphere's are: a grey fireproofing jacket over their lower half (the concrete casing real sphere legs carry near the ground) and off-white painted steel above. They are braced in every bay but the front, which stays open so the console and trefoil read.
- **Getting up it:** a grated walkway round the belt with a yellow handrail, reached by a ladder that climbs beside a leg at the back, bracketed to it. The railed platform on top has a cyan light strip round its edge. (A spiral stair, a ladder cage, a hatch and a crane were tried and set aside.)
- **The lantern:** the bands above the walkway are glass, a hall over the core where the rotating plugs and control rod drives stand.
- **The beacon** on a mast at the platform's front: an amber lamp half covered by its reflector, turned by a block entity renderer while the reactor runs, so a running breeder shows from across a base (as the station's aviation beacons mark it). The breeder's one moving part, drawn in its own group.
- **The compound** (1 Oct 2026, replacing a plinth): no plinth, so the legs stand straight on the ground. A slab-high bund wall round the footprint's edge joins the three consoles, as sodium plants bund their equipment (spilled sodium burns, so it is caught): hazard stripes all the way round its outer face, a short yellow fence on top, and floodlights on its inner face. Under the sphere a square hub, three blocks across, with a thick flanged steel pipe rising into the sphere's foot; from the back of each side console one big pipe runs to the hub, banded in both its ports' colours (it carries both their lines). Two thin conduits run from the front console.
- **Three consoles** at the edge, as the station has its ports, each shallow so the legs behind them reach the ground, with a short top and a louvred back sloping down to a low wall: the front one is the station's front panel (without its hazard band, which the wall carries) with the control core's screen; the side ones hold the ports under the station's hood. As you face the front, the inputs are on your left (fuel in, liquid sodium in) and the outputs on your right (the output port for spent fuel and what the blanket bred, energy out), each port in its own cell, never shared.
- **Lights:** the light strips, the hall's ring and the beacon's lamp are lit while the reactor runs and dark when it stops; the lantern's band and the frame on the ground give off light then, so it lights the ground round it as the station does.
- **Even textures:** the sphere's radius makes each latitude band exactly one plate high; the plate counts only halve from band to band, and where they do each wide plate is drawn in two halves meeting the narrow plates' corners, so the surface closes with no gaps; the belt's panels meet at the legs, and decals are drawn at their exact size.
- Earlier passes (a round tank with its machinery on the roof, then the tank alone) were set aside on 30 Sep 2026; they are in git history.

### How the tiers tie together
- Uranium station: uranium rods become spent rods, which reprocess into plutonium for MOX.
- MOX station: MOX rods become spent MOX, which the pyroprocessor turns into breeder fuel.
- Breeder: burns that fuel and breeds plutonium (back to MOX), U-233 from thorium, or tritium from lithium.
- Every tier stays useful (rule 4): the breeder needs a MOX station to start and keep its fuel coming, and the stations burn what it breeds.
- Fusion needs the breeder: its first tritium comes from the breeder's lithium blanket, and so does any top-up while a fusion blanket breeds less than it burns. Sodium coolant is split from salt on fission power.
- Output target: 20,000 to 100,000 FE/t (section 8's scaling targets).

### New materials
- **Spent MOX rod**, **transuranic metal** (no other mod makes it, so it has no common tag), **breeder fuel**, **uranium-233**.
- **Sodium:** an ingot (`c:ingots/sodium`) split from salt, for recipes, and liquid sodium (`c:sodium`), the breeder's coolant, melted from the ingot in the Electrorefiner. Like steam it is never placed in the world and has no bucket; it lives in fluid pipes and tanks. Its danger is a sodium fire if it meets water, never a steam explosion (settled 30 Sep 2026).
- **Tritium:** a gas, like steam, carried in gas pipes and kept in pressure tanks (settled 30 Sep 2026).

## 10. From fission to fusion

A deuterium-tritium tokamak cannot be built or fuelled without fission. Its fuel, its magnets and its fuel handling all come from the fission station, the breeder and what they leave behind, each made by a new process on fission power (rule 7). None of it is a pile of the tier below (rule 6).

| Fusion needs | What it is | Where it comes from |
|---|---|---|
| Tritium (fuel) | Radioactive hydrogen; almost none exists in nature | Lithium target rods in the breeder's blanket |
| Deuterium (fuel) | Heavy hydrogen, in ordinary water | Heavy water, separated from water |
| Magnets | Superconducting coils that hold the plasma | Yttrium tape from monazite, cooled by liquid nitrogen |
| Fuel handling | Separating hydrogen isotopes | Palladium membranes, the palladium recovered from fission waste |
| First wall | Armour facing the plasma | Tungsten tiles |

### Tritium: lithium in the breeder's blanket
- **Lithium Extractor:** salt and water become lithium (`c:dusts/lithium`). A simple machine on microreactor or fission power. Real basis: direct lithium extraction from brine, where most real lithium comes from.
- **Lithium target rod:** made in the Fuel Fabricator from lithium, aluminium and steel. Real basis: lithium aluminate pellets in a steel tube. The Lithium Extractor makes 2 salt and 500 mB of water into 1 lithium (40 FE/t, 10 s); a target rod takes 2 lithium, 1 aluminium and 1 steel.
- **In the breeder:** target rods go in the breeder's blanket (section 9). The spare fast neutrons slowly turn their lithium into tritium; a finished rod becomes an irradiated target rod and leaves by the output port. Every blanket slot holding lithium is one not breeding plutonium or U-233, so tritium costs fuel, not just time. Numbers are set with the breeder.
- **Not in the station.** The fission station first bred tritium in a target channel of its own (built behind `preview.next_tier`, never released). It moves to the breeder so that fusion needs the breeder; the station's target channel is retired when the breeder lands.
- **Tritium extraction:** irradiated target rods are processed into tritium, which needs palladium membranes (below).
- Real basis: tritium is made by neutrons on lithium-6; the United States makes it in a commercial power reactor with lithium rods. A breeder's whole point is spare neutrons, and using a fast reactor for tritium was studied in the 1990s (the Fast Flux Test Facility was proposed for it). The honest fudge: lithium-6 catches slow neutrons far better than fast ones, so a real moderated reactor is the more efficient tritium maker. We put it in the breeder because the breeder is where the spare neutrons are.
- Once the fusion reactor runs, its lithium blanket breeds its own tritium: the breeder starts fusion, and fusion then sustains itself, as planned for real reactors. The breeder stays useful for topping up while a fusion blanket breeds less than it burns (rule 4).

### Deuterium: heavy water
- **Heavy Water Plant:** water becomes a little heavy water, on fission power. Real basis: heavy water is separated from ordinary water in large plants.
- A multiblock: a row of tall exchange towers in stages (a cascade), so building more stages is the efficiency the design rewards (section 5). Real basis: the Girdler sulfide process runs water and gas past each other in tall towers, stage after stage.
- Two uses: deuterium for fusion, and a better moderator for the fission station (a heavy water channel), as in heavy-water reactors such as CANDU.

### Palladium: the use for fission waste
- **Waste Refinery:** fission waste gives back palladium, and the rest is sealed into glass for storage. Real basis: fission leaves rare metals in spent fuel, palladium, rhodium and ruthenium among them, and high-level waste is vitrified (set in glass) for storage.
- Palladium makes membranes that let hydrogen isotopes through and nothing else, which fusion's fuel system and tritium extraction both need. So the waste the fuel cycle has been storing finally has a use.

### Magnets: yttrium and liquid nitrogen
- **Monazite processing:** monazite gives yttrium (and thorium, for tier 4). Yttrium goes into superconducting tape, wound into the magnet coils. Real basis: this family of superconductors (yttrium barium copper oxide) works at liquid nitrogen temperature, and newer compact tokamaks use it.
- **Cryo Plant:** separates liquid nitrogen from air, on fission power, to keep the coils cold.
- A multiblock: an air intake and compressors feeding a tall insulated cold box (the distillation column), with a frosted storage dewar. Real basis: air separation units are exactly this shape.
- The cryogenic cable fittings use the same yttrium.

### First wall: tungsten
- Tungsten tiles line the inside of the reactor. Real basis: tungsten survives the heat and is what ITER uses.

### Energy costs (first pass, being play-tested)
Power is the price (section 5). The budget is about 1 billion FE to build the reactor and its first fuel: roughly 1.5 hours of 10,000 FE/t, two or three good fission stations or one breeder. Every minimum draw is above any microreactor (460 FE/t overdriven), so fission has to be running.

| Machine | Minimum draw | Energy per operation | Per reactor build | Real basis |
|---|---|---|---|---|
| Waste Refinery | 1,500 FE/t | 600,000 FE (20 s) per waste | Palladium for membranes, small | Rare metals from spent fuel |
| Tritium extraction | 2,000 FE/t | 1.2M FE (30 s) per rod | A few rods for the first charge | Heating rods to drive tritium through palladium |
| Heavy Water Plant | 4,000 FE/t | 20,000 FE per mB | About 20M FE for a 1,000 mB first charge | Isotope separation is famously costly |
| Monazite processing | 2,000 FE/t | 800,000 FE (20 s) per raw monazite | About 80 yttrium: about 64M FE | Acid digestion and solvent extraction |
| Tape machine (name to come) | 6,000 FE/t | 8M FE (about 67 s) per tape | 4 tape per coil, about 90 tape: about 720M FE | Superconducting tape is laid down in vacuum, slowly |
| Tungsten sintering | 3,000 FE/t | 600,000 FE (10 s) per tile | About 64 tiles: about 38M FE | Tungsten is pressed and fired above 2,000 °C |
| Cryo Plant | 2,000 FE/t | Running cost, not build cost | Keeps the coils cold while the reactor runs | Air separation is mostly compressor power |

- Tape is the main cost, as the magnets are in a real tokamak. A player can run two tape machines side by side if they have the power, so more power means faster progress, not a longer queue.
- The Cryo Plant is the fusion reactor's running cost: about 2 to 5% of its output goes back into keeping the coils cold. Real basis: cryogenics are a large part of a fusion plant's own power use.
- Speed modules and staged setups (such as a heavy water cascade) change these numbers; the table is for one plain machine.

### Uranium glass (decoration)
- Glass with a little spare uranium in it, glowing green as the station's chamber haze does. A decorative glass and light block, not part of progression. Real basis: uranium glass is real, and fluoresces green under ultraviolet light.

## 10b. Tier 5: Fusion reactor (tokamak)

A deuterium-tritium tokamak, built and fuelled from section 10. Draft, 25 Sep 2026: the concept is `art/tools/tokamak_concept.py` (render in `art/concepts/fusion_reactor.png`). The station's lesson was layout; this tier's lesson is running a plasma. Hard to run, huge payoff.

### Shape
- A doughnut. The vacuum vessel is a torus 5 blocks thick, 5.5 blocks from the machine's centre to the middle of the tube, on a round base 18 across. About 9 high with the solenoid.
- **The plasma is the doughnut.** It is a glowing ring inside the vessel, pink-violet as real deuterium-tritium plasma looks, seen through a band of viewports round the top of the vessel. Real basis: in a tokamak the plasma fills the ring; nothing burns in the hole in the middle.
- **Central solenoid:** a column up the hole in the middle, capped with a cyan light ring.
- **Toroidal field coils:** 16 D-shaped copper-cased coils round the tube, straight on the inside. Real basis: they make the main field running the long way round the ring; the D shape takes the magnetic load best.
- **Poloidal field coils:** two flat rings, above and below, that hold the plasma's shape.
- **Heating injector:** a duct into the side of the vessel (a neutral beam injector), powered through its end.
- **Base:** hazard-striped plinth with the ports on the front: deuterium in, tritium in, the control core, liquid nitrogen in, energy out. Water in and helium out still to be placed.
- Built like the station: a ghost outline, and the control core builds the reactor itself from parts fed to it. Formed, the core draws the whole machine from the concept design (a GPU mesh, like `client/StationMesh`). The block map is still to draw; aim for about 500 cheap blocks, open inside.

### Parts (rule 3: cheap structure, costly key parts)
- **Reactor casing:** the base and the vessel shell. Steel, cheap, most of the blocks.
- **Viewport:** uranium glass (section 10), a fudge: real viewports are fused silica. Settles open question 17 if kept.
- **Magnet coil:** superconducting tape (yttrium, section 10) in a copper and steel case. 16 of them, plus 2 poloidal rings. The main cost.
- **Central solenoid:** copper and tape, a few blocks tall.
- **Heating injector** and **control core:** advanced boards.
- Inside, filled from the GUI like the station's rods: tungsten first-wall tiles and blanket modules.

### Running it
1. **Cool the magnets.** Liquid nitrogen from the Cryo Plant must flow before the coils can carry current. Real basis: this family of superconductors (section 10) works at liquid nitrogen temperature. Lose the nitrogen and the coils warm and quench: the field collapses and the plasma is lost.
2. **Charge up.** Starting a plasma takes one large pulse of energy for the solenoid and heating, drawn from batteries or fission stations over about a minute (rule 7: fission starts fusion). Numbers to play-test.
3. **Fuel and heat.** Deuterium and tritium come in as gases; heating raises the plasma to its burn temperature (about 150 million °C on the readout). The readout shows the triple product (density times temperature times confinement time) against the ignition line. Real basis: the Lawson criterion.
4. **Burn.** Once the plasma heats itself, heating can be turned down. The panel shows Q (fusion power over heating power): above 1 it gives more than it takes, and the best setups reach ignition (Q without limit). 50:50 fuel gives the most power; an off mix gives less.
5. **Stay inside the limits.** Too much fuel for the field passes the density limit and the plasma disrupts. Too little gives little power. Stronger fields allow more density, and cost more nitrogen. Real basis: the Greenwald density limit.
6. **Helium ash.** Every reaction makes helium, which dilutes the fuel. The divertor at the bottom of the vessel removes it; helium leaves by a port (its use is open question 18).

### The blanket (the layout part)
- Behind the first wall sits a ring of blanket slots, planned in the GUI like the station's channels. Each slot is one of:
  - **Lithium-lead breeder:** breeds tritium from the neutrons, plus some heat. Lead multiplies neutrons, and we ship it. Real basis: lithium-lead breeding blankets.
  - **Heat module:** captures more heat, breeds nothing.
  - **Shield module:** protects the coils, so they need less nitrogen.
- The panel shows the tritium breeding ratio. At 1.0 or more the reactor makes all the tritium it burns and the fission target rods are no longer needed (fission starts fusion, fusion then sustains itself, section 10). Above 1, spare tritium comes out. Below 1, it needs topping up.
- So every slot is a choice between power, fuel and magnet cost (rule 1).

### Wear
- 14 MeV neutrons slowly wear the tungsten tiles, and disruptions take a chunk out of them. Worn tiles come out of a port and new ones go in, so it automates (rule 3). Long lives, never a chore.

### H-mode (the risky option)
- In place of the safeties switch: past a heating threshold the plasma can jump into H-mode, which roughly doubles confinement and so power. The catch is edge bursts (real name: edge-localised modes) that wear the tiles and divertor much faster, with a small chance of a disruption on each one. Real basis: H-mode is how real tokamaks plan to reach high Q, and taming those bursts is an open problem.

### Safety
- Fusion cannot run away: the worst case is a disruption. The plasma dies in an instant and its energy hits the wall: a bang, sparks, tile damage and a restart (with a new charge). No meltdown, no crater. Real basis: a tokamak holds only a few grams of fuel at a time, and any upset stops the reaction.
- Radiation while running is the strongest in the mod (fast neutrons), so a reactor needs distance or shielding. It stops when the plasma stops (rule 10).

### Output
- Target 100,000 to 1,000,000 FE/t (section 8's scaling targets). Heat leaves through the blanket's water and comes out as FE at the base, as the station does; the steam loop and turbines inside the base are the fudge (open question 19).

## 11. Tier 6: Dyson swarm

### The sun dimension
- A bright yellow, glowing superflat world. Framed as a "stabilised photosphere" layer, since the real sun has no surface.
- Entry needs special gear: heat shielding, radiation protection and a visor. Without the visor the screen whites out.
- Performance note: a huge truly transparent floor is expensive to render. Use a glowing block that looks translucent or shimmers instead.

### The sun gate
- The way in is the sun gate: a small sun held above a round platform by three curved emitter arms. Honest fudge: it is a stabilised window onto the photosphere, so the sun you see is the real one, scaled down.
- It doubles as the swarm's orrery. Solar shades circle the little sun on six tilted orbits (24 slots each), and slots fill as the real swarm's coverage rises, evenly all round. Empty slots show as faint outlines, so progress reads at a glance from across the base.
- Somewhat accurate touches: the sun darkens and reddens towards its edge (limb darkening), the surface boils with granulation, inner orbits turn faster (Kepler's third law), and the corona dims as coverage rises because the shades catch the light.
- Concept design in `art/tools/sun_gate_concept.py`, 11 blocks across and about 10 high. The platform is static geometry; the sun, corona, prominences and shades are drawn every frame (`SunGateSun`). A creative Sun Gate Preview block shows it in game (right-click steps the coverage shown).

### Anchor sails
- The first batch of sails is launched by hand in the sun dimension.
- Right-click a sail: it becomes a real entity, drifts upward, then vanishes and joins the swarm.
- These anchor sails are the reference array the automation machine needs to calibrate. The count is still open (see OPEN-QUESTIONS.md).
- The launch trip is a one-time adventure, not a chore.

### Automation
- Sail launcher machine linked to the sun gate. Once calibrated, it teleports sails into the dimension automatically.
- GUI shows calibration progress, e.g. 14/20 anchors detected.
- Better sail tiers cover more sky per launch, so progress comes from tech, not volume.

### Visuals
- Custom sky renderer draws the swarm filling in around the sun as coverage rises.
- The swarm is also visible faintly from the overworld sun, so progress shows in your base's sky.

### Getting power home
- Receiver dish at base, output scaled by swarm coverage. Based on real beamed power concepts (microwave or laser).

### Optional extras (config)
- Solar flare events that damage anchors and drop calibration. Off by default.
- A full swarm dims the overworld sun. Off by default, since it affects crops and mob spawning.
- Shared server-wide swarm for multiplayer.

## 11b. Water, steam and pipes (tier 1)

- **Intake pump:** a one-block machine that sits one block above the water and draws from the water source block directly below it, without ever draining it. 100 mB/t for 10 FE/t (config), energy in on any side, and it pushes water out of every side but the bottom (its intake), so a pipe on any side takes it without an extract setting. Pipes come first, so a pipe's panel shows the pump's full output; a tank or machine touching the pump gets only what the pipes did not take. A fluid pipe set to extract from the pump takes over: the pump pushes nowhere and leaves its water for that pipe. The microreactor runs dry without water, so its own dry power can start the pump that cools it. Real basis: an intake pump on a lake or river.
- **Steam:** a gas, stored as a fluid that is lighter than air, tagged `c:steam` and `c:gaseous` so other mods' steam users and tanks take it. No bucket, never placed in the world.
- **Pipes:** four kinds share one base (auto-connect, wrench to extract or disconnect, a stats panel): energy cables, item pipes, fluid pipes (liquids only, 250 mB/t) and gas pipes (gases only, 1,000 mB/t). Liquids and gases never share a pipe, as in real plants. Band colours match the ports: red energy, orange items, blue liquids, white gases.
- **Fittings (upgrade, don't replace):** one rule for everything entering a network: whatever comes in on a side is limited by that side's fitting, whether it is pulled by an extract side, pushed in by a generator, or drawn from a battery; the source's own rate limits it too. So a full home battery (3,000 FE/t) discharges at 1,000 FE/t on a plain cable and at its own rate once fitted. A cable's panel (empty hand on any cable joined to a machine) lists each machine side above the player's inventory; sides that feed the network get a fitting slot, and sides that only take from it say "receives only", since a fitting there would do nothing. One fitting per side; each tier is crafted from the one before, so a better fitting replaces a worse one, and the same three serve every kind of cable and pipe:
  - Silver Fittings (4 silver, 4 copper, redstone): 8 times the base. Silver is the best common conductor.
  - Busbar Fittings (silver fittings, 4 copper blocks, 2 steel, 2 advanced boards): 32 times. Heavy busbars carry a power station's current.
  - Cryogenic Fittings (busbar fittings, 4 yttrium, 4 blue ice): 1,024 times, effectively unlimited. Superconductors carry current with no loss. The recipe loads once yttrium exists (tier 5).
  - Energy goes 1,000, 8,000, 32,000, about 1 million FE/t; fluids and gases scale the same way; item pipes grow their batch up to a stack, then pull more often, down to every tick.
  - The fitting shows on the cable where it is fitted: a bright silver sleeve, a bolted copper clamp, or a frosted jacket with a glowing cyan ring. The cable's model adds them from the block entity (fittings in the block state would multiply each cable's states by 4096).
  - Without fittings a plain cable carries 1,000 FE/t per input, which is why a fission station needs at least silver fittings on its energy port's cable.
- **Pressure tank:** a steel tank with sight glasses. One block on its own is a small tank (32,000 mB, config). Built as a 2 x 2 footprint and stacked up to 16 high, the blocks join into one tower that pools its gas. A gas fills its whole container instead of sitting at a level, so the steam shows through the glass as a haze that thickens with the pressure. Pipes connect to any outside face. Breaking a block vents that block's share. Real basis: a gas receiver with sight glasses.
- **Fluid tank:** the pressure tank's twin for liquids (water, lava), with copper walls and 16,000 mB per block (config). Same 2 x 2 towers; the liquid shows through the glass at its level, settled at the bottom, and lava glows. Pressure tanks and fluid tanks never join each other.

## 12. Energy storage

Goal: batteries that look and grow like real ones, instead of one block recoloured per tier. Progress comes from adding modules and switching to better chemistries, not crafting a new block.

Simple rule: each module adds both capacity and charge/discharge rate. The chemistry decides how much. No separate inverter block.

### Tier 1: Modular Battery Rack
- A 2-block-tall cabinet of battery modules, like a stackable home battery (internal ID `home_battery`).
- Right-click a battery module into it and one more segment appears on the model, up to 6.
- Shift right-click removes the top module, so modules can be moved or swapped.
- Chemistries, swapped into the same stack:
  - **Lead-acid modules:** available early (lead is a tier 1 material). Older off-grid homes really used these.
  - **LFP (lithium iron phosphate) modules:** unlocked once the electrolyser makes lithium. What most modern home batteries use. Much higher capacity and rate.
- Built (first pass):
  - Lead-acid module: 200,000 FE and 500 FE/t each (config), so a full cabinet holds 1.2M FE at 3,000 FE/t. Recipe `.C. / LRL / LLL` (5 lead, a copper terminal, redstone standing in for the acid).
  - Cabinet: `III / C.C / IBI` (iron, copper, a basic board). Cheap; the cost is in the modules.
  - A module carries its share of the charge when taken out (shown as a bar on the item), so energy moves with it and swapping chemistries loses nothing.
  - On a cable network the battery is a buffer, as in Mekanism: machines are served first and the battery takes the surplus, and when the sources fall short the network tops machines up from the battery. One plain cable does both, no wrench needed. Any block that both accepts and gives energy counts as a buffer, so other mods' storage works the same way. A cable side set to extract still pulls from it. Both directions are limited by the combined module rate. Right-click with an empty hand opens a readout; there is no inventory to fill.
  - Placed and broken like a door; the lower half holds the contents.
  - A charge bar of 8 cyan LED segments runs up the front post, lit (emissive) in eighths of the charge, so you can read how full it is from across the room.

### Tier 2: Container battery (grid scale)
- **Next to build**, after the Spent Fuel Pool. Concept: `art/tools/container_concept.py`. Original design, based on the idea of real container batteries.
- **Shape:** 8 long, 3 wide and 3 high: a white container the size of a 20 foot shipping container (seven blocks) with orange corner castings, and a gunmetal fan unit on its east end. Laid out on the pool's grid: one framed plate per block face, every feature centred.
- **Modular:** the front has 20 module slots, one per block face round the controller's console in the middle bay. An empty slot is a hatch in a white plate, so a new container is a clean white box. Right-click an LFP rack module into a slot and that face turns into a glazed door with the rack set back behind the glass, lit, and the block gives light; shift right-click takes it out. Capacity and rate grow with each rack (rule 11).
- **Racks:** LFP (lithium iron phosphate), made with lithium from the Lithium Extractor, so the container is a real step up from the home battery and unlocks around fission. Sodium-ion racks, a real, cheaper grid chemistry, come later in the same slots.
- **Thermal management:** the fan unit fills the east end: one big fan (a block entity renderer, spinning while the battery works) with coolant in at its hub. Without coolant the battery charges and discharges at a quarter of its rate. Real grid batteries need active cooling.
- **Ports:** coolant in at the fan's hub (east end), and an energy port on the back at each end. Each energy port both takes energy in and gives it out, as the home battery does, so a cable network treats the container as a battery (machines first, surplus into storage) and the cables' own settings decide the flow. Three vents between them along the back's middle row. The west end carries two electrical hazard signs.
- **Parts and building:** 70 Container Frame blocks are the bulk; the cost is in the Battery Controller (the console) and the Thermal Unit (the fan's hub), and above all the racks. Built like the pool: place the controller on the ground, feed it the parts and it builds the container round it, then swaps up into its console.
- **Numbers (first pass, config):** each LFP rack holds 2M FE and moves 2,000 FE/t, so a full container holds 40M FE at 40,000 FE/t. The fan unit uses 2 mB of coolant per tick while energy flows, from an 8,000 mB tank.

### Endgame: Superconducting storage
- **SMES (superconducting magnetic energy storage):** a real technology that stores energy in the magnetic field of a superconducting coil and charges or discharges almost instantly.
- Uses the yttrium superconductors and cryogenic cooling from the fusion tier. Where Dyson swarm power gets stored.

## 13. Radiation

Radiation is on by default (config toggle to turn it off). It exists to make the machines feel dangerous and the protective gear feel special, not to punish players.

### Sources
- **Only running reactors**, within a short radius (low to high by tier).
- **Meltdowns**, which leave a severe, lingering area. This is still the reactor, just after it has gone wrong.
- Ores, raw materials, fuel cores, depleted cores and waste are all safe to carry and mine. A player 10 minutes in who finds uranium is never affected.
- Real basis: natural uranium ore is only weakly radioactive, so safe mining is close to reality. Treating spent fuel as safe to handle is a deliberate simplification.

### Effects
- Players build up a dose while exposed. Dose fades slowly over time away from sources.
- Rising dose applies escalating effects: weakness, then nausea, then damage. Never an instant kill outside a meltdown.
- Mobs (animals, monsters, villagers, golems) take a dose too, the same way: weakness, then slowness, then slow damage. Strong reactors make no-go zones for mobs, and a shielded reactor room can run a mob farm. Mobs recover away from sources. Config `radiation.affect_mobs` (on by default) turns it off, for packs that want villagers safe near reactors.

### Numbers (first pass, being play-tested)
- Dose rate (mSv/s) = source strength / distance squared, out to 16 blocks. Checked once a second per player.
- Microreactor strength: 20 running, 50 in overdrive, 120 during a coolant loss. A meltdown leaves a site of strength 250 that fades to nothing over 20 minutes (saved with the world). Right next to a running reactor that is about 9 mSv/s: weakness in about 10 seconds, damage within a minute. Distance and shielding are the answer.
- Shielding along the line from source to head: each solid block halves the rate, water takes 20%, lead blocks (`c:storage_blocks/lead`) take 90%. The reactor's own blocks count as the source, not a shield. Real basis: inverse-square falloff and attenuation; lead is the best common shield.
- Dose thresholds: 100 mSv weakness; 250 adds hunger and bouts of nausea; 500 adds radiation damage, half a heart every 4 seconds, ignoring armour. Dose caps at 1000.
- Recovery: 0.5 mSv/s once the rate drops below 0.05. Dose resets on death. Creative and spectator players are exempt.
- Config: `radiation.enabled`, `radiation.strength` (a multiplier) and `radiation.affect_mobs`.

### The dosimeter ring
- A craftable ring worn in the **Accessories** ring slot (tagged `accessories:ring`, and `curios:ring` for packs that use Curios). Recipe: a fluorite chip in an iron band (`.F. / I.I / .I.`); fluorite (calcium fluoride) is a real thermoluminescent dosimeter material.
- Without an accessory mod installed, carrying it anywhere in the inventory counts, so the mod stays fully playable standalone.
- The plain ring blocks all of the dose for now, since there is no other radiation gear yet (and may never be). Its gauge and clicks still read the radiation around you, so it warns you even while it protects you. Lower `DosimeterRingItem.PROTECTION` if linings ever arrive.
- Real basis: nuclear workers really do wear ring and badge dosimeters. The one fudge: real dosimeters only measure dose, while ours also protects.
- It records your total dose quietly (shown in its tooltip) but shows no gauge and makes no sound.

### The Geiger counter
- A handheld counter (recipe: glass pane over a basic board, iron and a note block). While you carry it (anywhere in the inventory, or clipped to the Accessories belt slot, `accessories:belt` and `curios:belt`), a small HUD gauge shows your dose and the dose rate around you, and it clicks faster the stronger the field. Shift-right-click it to mute the clicks and keep the gauge (a plain right-click is Accessories' quick-equip). Without one, radiation is silent.
- Real basis: a Geiger-Muller tube, a gas-filled tube that gives a pulse, heard as a click, each time radiation passes through it. It measures; it does not protect.
- Upgraded, not replaced: new tier materials are added to the same ring (lead lining, then silicon carbide, then tungsten) to protect against stronger sources. No crafting a new ring each tier.
- Multiplayer: a friend without a ring is at risk around your reactor, which makes the ring feel earned.

## 14. Anti-grind rules

- Structure blocks are cheap. Cost lives in the interesting parts.
- Tiers unlock through milestones, not resource piles.
- Power is the price (section 5): key materials cost energy, and a better power setup gets there faster.
- A long process runs unattended, shows its time left, and is fed by pipes.
- Test designs in the planner before building.
- Any repeated manual action should be replaced by automation soon after the player understands it.

## 15. Compatibility

- Standard NeoForge energy (FE).
- Works with Mekanism steam and fluids where it makes sense.
- EMI and JEI recipe display.
- All recipes data driven so pack makers can change them.
- Config for radiation, meltdowns, flares, sun dimming and balance values.
- Accessories integration for the dosimeter ring (optional dependency; the ring also works carried).

## 16. Art style

- **Direction:** "Oritech-lite". Chunky industrial machines with visible pipes, panels and chutes, but built from standard block models instead of heavy animated models.
- Standard JSON block models (made of boxes, editable in Blockbench) and 16x16 textures.
- Multiblocks swap to one combined model when assembled, which delivers most of the Oritech feel.
- Animation kept light: particles, glowing textures, a few moving parts where they matter (fans, pumps).
- Key showpiece models (fusion reactor, sail launcher) can be upgraded to animated models later, or handed to an artist, without changing any code design.

## 17. Naming and IP

- All names, models and textures original.
- Real-world inspirations can be credited on the mod page, not used as item names.
