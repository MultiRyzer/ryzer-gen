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
3. **No grind.** Cheap structure, meaningful components. Milestones unlock tiers, not piles of ingots.
4. **Every tier stays useful.** Earlier reactors become stepping stones, not junk.
5. **Pack friendly.** Standard energy, easy recipe tweaking, sensible configs, good performance. Built to earn a spot in packs like ATM10.

## 3. Progression ladder

| Tier | Machine | Real-world basis | New mechanic it teaches |
|---|---|---|---|
| 1 | Microreactor (4-block) | Transportable "nuclear battery" microreactors, TRISO fuel | Heat, fuel life, basic output |
| 2 | Fuel cycle | Reprocessing, MOX fuel (as done in France) | Spent fuel is the next fuel |
| 3 | Modular fission reactor | Conventional fission, moderators and coolant | Layout design |
| 4 | Breeder and thorium | Fast breeder reactors, thorium to U-233 | Making more fuel than you burn |
| 5 | Fusion | Deuterium-tritium tokamaks (ITER) | Plasma stability, breeding tritium from lithium |
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
| Graphite | Moderator, fuel coating | Coal or charcoal block in a blast furnace (1 block makes 1 graphite) |
| Silicon carbide | TRISO fuel layer, hardened circuits | Sand and graphite in the alloy smelter |
| Silicon | Circuits and solar cells | Sand in a powered furnace |
| Lithium | Breeds tritium for fusion | Extracted from salt brine (real lithium mostly comes from brine) |
| Sodium | Breeder coolant | Split from salt |
| Thorium, yttrium | Thorium fuel, superconductors | Extracted from monazite |

Deliberately skipped: zirconium and spodumene ores, and iridium and platinum. Accurate or useful, but not needed, and fewer ores means less clutter.

## 5. Progression spine

Rule 1: **no compression recipes.** A higher tier never needs a stack of the tier below. It needs different materials made by a new process.

Rule 2: **power gating.** The machine that makes the next tier's key material runs on the current tier's power. You cannot make it until you can power it.

| Tier | Generator | Key component | New material and process | Circuit |
|---|---|---|---|---|
| 1 | Microreactor | Reactor heart | Steel, lead, graphite (blast furnace) | Basic control board: iron, copper, redstone |
| 2 | Fuel cycle machines (run on microreactor power) | Precision parts | Silicon wafers from sand in a powered furnace; fluorite for uranium processing | Advanced board: steel, gold, silicon |
| 3 | Fission reactor | Control rod assembly | Silver alloy control rods (real rods use silver, indium and cadmium); MOX fuel from tier 2 | Hardened board: silicon carbide and lead (radiation-hard electronics) |
| 4 | Breeder reactor | Sodium coolant loop | Sodium split from salt using fission power (real fast breeders are sodium cooled); thorium from monazite | Hardened board |
| 5 | Fusion reactor | Superconducting magnet coil, plasma-facing wall | Yttrium from monazite for superconductors; tritium bred from lithium in the breeder; tungsten for the plasma wall | Cryogenic board: superconducting wiring |
| 6 | Dyson swarm | Solar sail, receiver dish | Aluminium film sails; silicon solar cells | Photonic board |

Circuits follow their own logic: basic, advanced, radiation-hardened, cryogenic, photonic. Each tier's circuit needs something only the previous tier made possible.

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
- Ghost preview: while holding any part, each unformed heart nearby shows outlines where the other three blocks go, with the suggested part floating in each (machine units above and behind the heart, the coolant jacket at the upper back under the intake), and an arrow on the ground for the way the machine will face. Filled spaces turn green, blocked ones red.

### Input and output
- Like Oritech's multiblocks, the assembled model has fixed, visible connection chutes. Pipes and cables only connect at those points, not on any face.
- The machine faces the way the player was facing when they placed the heart, so the chutes always end up in predictable spots.
- Port layout (machine facing north):
  - **Energy out:** cable socket on the back face of the lower back block. The machine pushes energy out, so it works with cables that do not pull (Mekanism's included).
  - **Coolant in:** intake chute on the top face of the upper back block. Accepts water only, and pipes cannot drain it.
  - **Heat/steam out:** outlet on the right-hand face of the lower back block. Modelled, connected later when steam matters.
- Every port is a 10x10 flange flush with the block face, centred, around an 8x8 socket. That seats the common 6x6 pipes and cables (Pipez, Mekanism) and their 8x8 end plates. The ring colour says what it carries: red energy, blue coolant, white steam.
- Ports use the standard NeoForge energy and fluid capabilities, only on the port face and only while formed.
- Front face carries the display panel. Right-click any block to open the GUI.

### Running it
- The heart holds one sealed fuel core. No topping up. Steady modest power for a long time, then it depletes.
- The core carries its own burn time, so it can be taken out and put back without losing fuel. When spent it turns into a depleted core in the slot.
- Heat model: the core makes a fixed thermal power, and a heat engine turns part of it into FE. Efficiency is 55% of the Carnot limit, 1 - T_cold / T_hot. With water the cold side is near 20°C; dry, the jacket sheds heat to hot air, so the cold side sits near 250°C. A cold core converts almost nothing, so output ramps as it heats.
  - Dry: settles at 700°C, about 25% efficient, 200 FE/t (the config value).
  - Water: settles at 450°C, about 33% efficient, about 257 FE/t, boiling off 1 mB per tick.
  - The GUI shows efficiency live, with a bar coloured by mode.
- Safety override (overdrive): the interlocks can be switched off (shift-click, on purpose). The control rods come further out for 30% more fission power, and the core settles at 650°C. The steam leaves superheated, so the turbine reaches 66% of the Carnot limit instead of 55%: 45% efficient overall, about 460 FE/t (nearly double the water-cooled output). The cost: double water use and 30% faster fuel burn. Real basis: supercritical power stations reach about 45% with superheated steam.
  - Lose the water in overdrive and the core runs away (2°C per tick). An alarm sounds, the status flashes COOLANT LOSS and smoke pours out. At 1000°C it melts down: the machine and core are destroyed in an explosion (power 5 by default).
  - The player has about 9 seconds to switch off, re-arm the safeties or add water. In follow-load mode a full energy buffer also saves it, since the reactor stands by; in dump mode it does not.
  - Config: meltdowns can be turned off, in which case the interlock re-arms itself on coolant loss. With the safeties on, the microreactor can never melt down.
  - Real basis: a loss-of-coolant accident with the automatic scram disabled. The explosion is an honest fudge for a steam explosion.
  - Radiation from meltdowns waits for the radiation milestone.
- Excess power, a toggle in the GUI:
  - Follow load (default): when the energy buffer is full the reactor stands by and saves fuel.
  - Dump: it keeps running on a full buffer and vents the surplus as a steam plume from the steam outlet. It burns fuel nonstop, so it keeps making depleted cores (and later by-products) with no power consumer attached. Real basis: the steam dump (turbine bypass) valves that let a pressurised-water reactor run on when the turbine cannot take its power.
- Still to do: boxing it in stone throttles it.
- Running shows: Cherenkov glow in the porthole (emissive, block light 9), a low hum, steam wisps from the coolant intake.
- GUI: fuel left, temperature, output, energy, coolant, on/off. Redstone controllable.
- Balance lives in the common config: output (200 FE/t default), fuel life (72,000 ticks, one hour of running), coolant use.

### Crafting (draft, balance TBD)

Target: buildable in the early game, after iron and a blast furnace, before diamonds. The structure is cheap; the fuel is where the cost sits.

Machine:
- **Alloy smelter:** the mod's first machine. A furnace with two input slots that burns ordinary furnace fuel, since there is no power yet. Crafted from a furnace, bricks and iron. Pipes connect like a furnace: inputs on top, fuel on the sides, output from the bottom.

Intermediates:
- **Steel:** iron ingot plus coal or charcoal in the alloy smelter.
- **Graphite:** coal or charcoal block in a blast furnace.
- **Silicon carbide:** sand plus graphite in the alloy smelter (a blast furnace only takes one input).
- **TRISO pellets:** 1 uranium ingot, 2 graphite and 1 silicon carbide make 2 pellets. Based on real TRISO fuel: a uranium kernel coated in carbon and silicon carbide layers.

Fuel note: the real Unity microreactor that inspired this uses standard uranium dioxide fuel with helium coolant and water as the moderator. We use TRISO on purpose, as used by other microreactor designs, because it gives the fuel cycle a real reason for the cracking step.

Blocks:
| Block | Qty in structure | Draft recipe |
|---|---|---|
| Reactor heart | 1 | Steel, lead, graphite, redstone and glass |
| Reactor machine unit | 2 | Steel, lead and copper |
| Coolant block (name pending) | 1 | Copper and a water bucket (draft) |

Fuel:
- **Sealed fuel core:** 4 TRISO pellets packed round graphite inside a 4-steel shell (so 2 uranium per core). Goes into the heart. When depleted it is swapped out whole, and the old one becomes the depleted core for the fuel cycle.

Uranium is rare in All The Ores worldgen, so fuel cores should be where most uranium goes, and a single core should last a long time.

### The hook
When the core runs out, the player gets a depleted core they cannot use yet. That curiosity pulls them into the fuel cycle.

## 7. Tier 2: Fuel cycle

Real basis: spent uranium fuel is roughly 95% uranium, about 1% plutonium and about 4% fission products (the actual waste). Microreactor fuel is TRISO (tiny kernels sealed in ceramic and carbon), which is famously hard to reprocess. That gives us a real reason for an extra step.

1. **Core cracker:** breaks the TRISO casing on depleted cores.
2. **Reprocessor:** splits the result into recovered uranium, plutonium and waste.
3. **Fuel fabricator:** combines plutonium and uranium into MOX fuel for the fission reactor.
4. **Waste storage:** a simple, low-effort cask or vault. Enough to feel real, never a chore. Waste does not emit radiation, so storage is about tidiness and realism, not survival.

Processing the first depleted core unlocks the fission reactor.

## 8. Tier 3: Modular fission reactor

- Multiblock with a flexible size, but size is not the main lever. Layout is.
- Fuel rods next to moderators run hotter and more efficiently. Coolant channels remove heat. Bad designs overheat.
- A light design puzzle, not a spreadsheet.
- **Planner block:** previews heat and output for a design before you spend resources building it.
- Different fuels behave differently (hot and fast, slow and stable, breeding).
- Meltdowns are configurable, from "shuts down and loses fuel" to full crater.

## 9. Tier 4: Breeder and thorium

- Breeder reactor: produces more fissile fuel than it consumes. Needs more active management (flow rates, temperatures), with optional redstone or computer control.
- Thorium path: thorium is not a fuel itself but breeds U-233. An alternative route with its own trade-offs.

## 10. Tier 5: Fusion

- Deuterium-tritium tokamak.
- Tritium is bred from lithium in your fission reactors, tying the tiers together.
- Gameplay: keep the plasma stable, manage wear on components from neutron damage.
- Hard to run, huge payoff.

## 11. Tier 6: Dyson swarm

### The sun dimension
- A bright yellow, glowing superflat world. Framed as a "stabilised photosphere" layer, since the real sun has no surface.
- Entry needs special gear: heat shielding, radiation protection and a visor. Without the visor the screen whites out.
- Performance note: a huge truly transparent floor is expensive to render. Use a glowing block that looks translucent or shimmers instead.

### Anchor sails
- The first batch of sails is launched by hand in the sun dimension.
- Right-click a sail: it becomes a real entity, drifts upward, then vanishes and joins the swarm.
- These anchor sails are the reference array the automation machine needs to calibrate. The count is still open (see OPEN-QUESTIONS.md).
- The launch trip is a one-time adventure, not a chore.

### Automation
- Sail launcher machine linked to the portal. Once calibrated, it teleports sails into the dimension automatically.
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

## 12. Energy storage

Goal: batteries that look and grow like real ones, instead of one block recoloured per tier. Progress comes from adding modules and switching to better chemistries, not crafting a new block.

Simple rule: each module adds both capacity and charge/discharge rate. The chemistry decides how much. No separate inverter block.

### Tier 1: Home battery stack
- A 2-block-tall cabinet, like a stackable home battery.
- Right-click a battery module into it and one more segment appears on the model, up to 6.
- Shift right-click removes the top module, so modules can be moved or swapped.
- Chemistries, swapped into the same stack:
  - **Lead-acid modules:** available early (lead is a tier 1 material). Older off-grid homes really used these.
  - **LFP (lithium iron phosphate) modules:** unlocked once the electrolyser makes lithium. What most modern home batteries use. Much higher capacity and rate.

### Tier 2: Container battery (grid scale)
- A shipping-container-sized multiblock. Original design, based on the idea of real container batteries.
- Place the container frame, then fill the rack slots along the walls with battery racks. Racks appear visibly as they are added.
- A **thermal management block** is required, part of the coolant block family. Real grid batteries need active cooling.
- Chemistries: LFP racks, then **sodium-ion racks**, a real, cheaper grid chemistry. Sodium comes from salt electrolysis.

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

### The dosimeter ring
- A craftable accessory worn in a ring or necklace slot via **Curios** (the accessory API ATM10 uses). Mods like Accessories also support Curios items through their compatibility layer.
- Without an accessory mod installed, carrying it in the inventory counts, so the mod stays fully playable standalone.
- Real basis: nuclear workers really do wear ring and badge dosimeters. The one fudge: real dosimeters only measure dose, while ours also protects.
- Shows a small HUD gauge with current exposure, and clicks like a Geiger counter near sources.
- Upgraded, not replaced: new tier materials are added to the same ring (lead lining, then silicon carbide, then tungsten) to protect against stronger sources. No crafting a new ring each tier.
- Multiplayer: a friend without a ring is at risk around your reactor, which makes the ring feel earned.

## 14. Anti-grind rules

- Structure blocks are cheap. Cost lives in the interesting parts.
- Tiers unlock through milestones, not resource piles.
- Test designs in the planner before building.
- Any repeated manual action should be replaced by automation soon after the player understands it.

## 15. Compatibility

- Standard NeoForge energy (FE).
- Works with Mekanism steam and fluids where it makes sense.
- EMI and JEI recipe display.
- All recipes data driven so pack makers can change them.
- Config for radiation, meltdowns, flares, sun dimming and balance values.
- Curios integration for the dosimeter ring (optional dependency).

## 16. Art style

- **Direction:** "Oritech-lite". Chunky industrial machines with visible pipes, panels and chutes, but built from standard block models instead of heavy animated models.
- Standard JSON block models (made of boxes, editable in Blockbench) and 16x16 textures.
- Multiblocks swap to one combined model when assembled, which delivers most of the Oritech feel.
- Animation kept light: particles, glowing textures, a few moving parts where they matter (fans, pumps).
- Key showpiece models (fusion reactor, sail launcher) can be upgraded to animated models later, or handed to an artist, without changing any code design.

## 17. Naming and IP

- All names, models and textures original.
- Real-world inspirations can be credited on the mod page, not used as item names.
