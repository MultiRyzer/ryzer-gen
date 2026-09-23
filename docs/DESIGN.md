# Design Doc

Status: draft v0.1, 23 Sep 2026

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

Compatibility rule: every recipe asks for common tags (`c:ingots/uranium`, `c:ingots/lead` and so on), never a specific mod's item. That way material from All The Ores, Mekanism or Modern Industrialization all works.

| Material | Real-world role | Source |
|---|---|---|
| Uranium | Main fuel | Shared tag. We also ship our own ore so the mod works standalone; packs merge it automatically. Powah's uraninite is accepted as uranium too |
| Lead | Radiation shielding | Shared tag |
| Steel | Structure | Shared tag |
| Graphite | Moderator, fuel coating | Made from coal or charcoal, no ore |
| Silicon carbide | Hard ceramic layer in TRISO fuel | Made from sand and graphite, no ore |
| Lithium | Breeds tritium for fusion | Shared tag plus our own recipe |
| Fluorite | Uranium processing | Shared tag |
| Iridium, platinum | Late game fusion and sun gear | Shared tag |
| Thorium | Thorium fuel path | Our one new ore: thorium-bearing monazite, tagged to merge with Modern Industrialization's monazite. Our machines extract thorium from anyone's monazite |

Deliberately skipped: zirconium and spodumene ores. Accurate, but they add ore clutter. Handled as processing steps instead.

## 5. Tier 1: Microreactor

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
- Ghost preview: holding a heart or unit shows a faint outline of where the other blocks go.

### Input and output
- Like Oritech's multiblocks, the assembled model has fixed, visible connection chutes. Pipes and cables only connect at those points, not on any face.
- The machine faces the way the player was facing when they placed the heart, so the chutes always end up in predictable spots.
- Draft chute layout (to be settled when we model it):
  - **Energy out:** a cable socket low on the back.
  - **Coolant in:** an intake chute on top.
  - **Heat/steam out:** an outlet on one side, used later when steam matters.
- Front face carries the display panel. Right-click any block to open the GUI.

### Running it
- The heart holds one sealed fuel core. No topping up. Steady modest power for a long time, then it depletes.
- Heat matters a little: water or coolant nearby raises output, boxing it in stone throttles it.
- GUI: fuel left, temperature, output, on/off. Redstone controllable.

### Crafting (draft, balance TBD)

Target: buildable in the early game, after iron and a blast furnace, before diamonds. The structure is cheap; the fuel is where the cost sits.

Intermediates:
- **Graphite:** coal or charcoal block in a blast furnace.
- **Silicon carbide:** sand plus graphite in a blast furnace.
- **TRISO pellets:** uranium ingot, graphite and silicon carbide. Based on real TRISO fuel: a uranium kernel coated in carbon and silicon carbide layers.

Fuel note: the real Unity microreactor that inspired this uses standard uranium dioxide fuel with helium coolant and water as the moderator. We use TRISO on purpose, as used by other microreactor designs, because it gives the fuel cycle a real reason for the cracking step.

Blocks:
| Block | Qty in structure | Draft recipe |
|---|---|---|
| Reactor heart | 1 | Steel, lead, graphite, redstone and glass |
| Reactor machine unit | 2 | Steel, lead and copper |
| Coolant block (name pending) | 1 | Copper and a water bucket (draft) |

Fuel and tools:
- **Sealed fuel core:** TRISO pellets packed in graphite inside a steel shell. Goes into the heart. When depleted it is swapped out whole, and the old one becomes the depleted core for the fuel cycle.
- **Wrench:** iron and steel. Used for pack-up and disassembly.

Uranium is rare in All The Ores worldgen, so fuel cores should be where most uranium goes, and a single core should last a long time.

### Portability
- Shift right-click with a wrench to pack it into a single "packed microreactor" item that keeps its fuel.
- Place it anywhere to unfold it again. Good for outposts, other dimensions and moving contraptions.

### The hook
When the core runs out, the player gets a depleted core they cannot use yet. That curiosity pulls them into the fuel cycle.

## 6. Tier 2: Fuel cycle

Real basis: spent uranium fuel is roughly 95% uranium, about 1% plutonium and about 4% fission products (the actual waste). Microreactor fuel is TRISO (tiny kernels sealed in ceramic and carbon), which is famously hard to reprocess. That gives us a real reason for an extra step.

1. **Core cracker:** breaks the TRISO casing on depleted cores.
2. **Reprocessor:** splits the result into recovered uranium, plutonium and waste.
3. **Fuel fabricator:** combines plutonium and uranium into MOX fuel for the fission reactor.
4. **Waste storage:** a simple, low-effort cask or vault. Enough to feel real, never a chore.

Processing the first depleted core unlocks the fission reactor.

## 7. Tier 3: Modular fission reactor

- Multiblock with a flexible size, but size is not the main lever. Layout is.
- Fuel rods next to moderators run hotter and more efficiently. Coolant channels remove heat. Bad designs overheat.
- A light design puzzle, not a spreadsheet.
- **Planner block:** previews heat and output for a design before you spend resources building it.
- Different fuels behave differently (hot and fast, slow and stable, breeding).
- Meltdowns are configurable, from "shuts down and loses fuel" to full crater.

## 8. Tier 4: Breeder and thorium

- Breeder reactor: produces more fissile fuel than it consumes. Needs more active management (flow rates, temperatures), with optional redstone or computer control.
- Thorium path: thorium is not a fuel itself but breeds U-233. An alternative route with its own trade-offs.

## 9. Tier 5: Fusion

- Deuterium-tritium tokamak.
- Tritium is bred from lithium in your fission reactors, tying the tiers together.
- Gameplay: keep the plasma stable, manage wear on components from neutron damage.
- Hard to run, huge payoff.

## 10. Tier 6: Dyson swarm

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

## 11. Anti-grind rules

- Structure blocks are cheap. Cost lives in the interesting parts.
- Tiers unlock through milestones, not resource piles.
- Test designs in the planner before building.
- Any repeated manual action should be replaced by automation soon after the player understands it.

## 12. Compatibility

- Standard NeoForge energy (FE).
- Works with Mekanism steam and fluids where it makes sense.
- EMI and JEI recipe display.
- All recipes data driven so pack makers can change them.
- Config for meltdowns, flares, sun dimming and balance values.

## 13. Naming and IP

- All names, models and textures original.
- Real-world inspirations can be credited on the mod page, not used as item names.
