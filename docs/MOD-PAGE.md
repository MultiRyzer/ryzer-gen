# Mod page (Modrinth and CurseForge)

The text below is the project description. Paste it into both sites (both take Markdown). Summary line for the short description field:

> Nuclear power progression grounded in real physics: a portable microreactor, a real fuel cycle and a fission station you design channel by channel.

---

# Ryzer Gen

**Power from understanding, not volume.** Most reactor mods answer "how do I get more power?" with "build it bigger". In Ryzer Gen a small, well-designed machine beats a big sloppy one. Every tier teaches a new idea, feeds the next one, and stays grounded in real science.

> **Alpha.** Tiers 1 to 3 are playable. Balance is a first pass and may change between versions. Back up your world before updating.

NeoForge 1.21.1. Standalone: it ships every ore it uses. Optional support for JEI, EMI and Accessories.

## What's in the alpha

### Tier 1: the microreactor
- A four-block portable reactor based on real transportable "nuclear battery" designs and TRISO fuel. It snaps together into one machine when the parts are in place, with a ghost preview to show you where they go.
- Heat, fuel life and coolant: it runs dry, but water makes it run better and turns into steam.
- Overdrive: take the safeties off for more power. Lose coolant while they are off and it melts down.
- Home battery (a cabinet of lead-acid modules), energy cables, item pipes, fluid and gas pipes, an intake pump, and tall pressure and fluid tanks.

### Tier 2: the fuel cycle
- Spent fuel is the next fuel. Crack depleted cores, reprocess them into uranium and plutonium, and fabricate uranium and MOX fuel rods, much as France recycles its fuel.
- Fission waste goes into a Waste Cask that keeps its contents when broken.
- Speed Modules, and cable fittings (silver, busbar, cryogenic) to raise throughput.

### Tier 3: the fission power station
- A large round station with a glass chamber and a giant roof turbine. Feed the control core its parts and it builds itself.
- The core is where the depth is: you plan every channel as fuel, moderator, control rod or coolant. Heat, fuel economy and output all come from the layout, and the planner rates your design against the best possible layouts.
- MOX fuel makes double the power of uranium. SCRAM at 900°C, or turn the safeties off and risk a flux tilt and a meltdown that leaves a crater.

### Radiation
- Radiation comes **only** from running reactors and meltdown sites. Ores, fuel and waste are safe to carry.
- Dose falls off with distance and is blocked by shielding. Effects build up slowly and never kill instantly outside a meltdown. Mobs are affected too.
- The Geiger counter clicks and shows a HUD gauge. The dosimeter ring gives 25% protection (ring slot with Accessories).
- Turn it all off in the config if you prefer.

## Roadmap

| Tier | What | Status |
|---|---|---|
| 1 | Microreactor, battery, pipes and steam | In the alpha |
| 2 | Fuel cycle: reprocessing and MOX | In the alpha |
| 3 | Fission power station | In the alpha |
| Bridge | Fission to fusion: lithium target rods (tritium), heavy water, palladium from waste, superconducting magnets, liquid nitrogen, tungsten | In progress |
| 4 | Breeder reactor: spent MOX is pyroprocessed into fuel for a sodium-cooled fast reactor, with a uranium or thorium blanket | Planned |
| 5 | Fusion: a deuterium-tritium tokamak built and fuelled by fission | Planned |
| 6 | Dyson swarm: launch sails round a walkable sun in its own dimension, then beam the power home | Planned |

Want an early look at the next tier? Set `preview.next_tier = true` in `ryzergen-common.toml`. It is unfinished and its products have no use yet.

Full roadmap and issue tracker: https://github.com/MultiRyzer/ryzer-gen

## For pack makers
- FE energy throughout. Data-driven recipes and worldgen.
- Every recipe asks for common tags (`c:ingots/uranium`, `c:dusts/lead`, ...), so other mods' materials just work.
- Every ore's worldgen has a config toggle, so you can turn off ores another mod already supplies.
- Toggles for radiation, mob radiation, meltdowns, explosion power and crater size, plus output and capacity settings.
- No required dependencies besides NeoForge. MIT licensed: include it in any pack.

## How it was made
Ryzer Gen is designed by Ryzer. The code was written with help from Claude, an AI assistant. The textures are drawn in code by the mod's own scripts; material textures build on vanilla shapes. Sounds are credited below.

## Credits
Sounds from Pixabay (Pixabay Content License):
- Microreactor alarm: "Automatic Depressurization System, Nuclear Reactor" by u_whvpuvkdwz
- Fission station alarm: "Alarm Klaxon" by SoundFX for Free
- Turbine: "Underground Alien Reactor" by freesound_community
- Core Cracker: "Factory Grinding" by freesound_community
- Reprocessor: "Basement Water Pump" by freesound_community
- Fuel Fabricator: "Lowering Ramp" by freesound_community
