# Mod page (Modrinth and CurseForge)

The text below is the project description. Paste it into both sites (both take Markdown; on CurseForge pick the Markdown editor first). Upload the gallery first, then replace each `IMAGE_URL` with that picture's address (open it in the gallery and copy the image link). On CurseForge use the copies in `art/gallery/curseforge/`, which are under its 850 px limit.

| Placeholder | Gallery file |
|---|---|
| `HERO_IMAGE_URL` | `01_hero_station_sunset.jpg` |
| `PRODUCTION_LINE_IMAGE_URL` | `03_production_line.jpg` |
| `CORE_PLANNER_IMAGE_URL` | `02_core_planner.png` |
| `MELTDOWN_BEFORE_IMAGE_URL` | `04_meltdown_before.jpg` |
| `MELTDOWN_CRATER_IMAGE_URL` | `04_meltdown_crater.jpg` |
| `SWARM_IMAGE_URL` | `fission_station_swarm.webp` (CurseForge: `fission_station_swarm.jpg`) |

Summary line for the short description field:

> Nuclear power progression grounded in real physics: a portable microreactor, a real fuel cycle and a fission station you design channel by channel.

---

<center>

![The fission power station at sunset](HERO_IMAGE_URL)

[![Source on GitHub](https://img.shields.io/badge/source-GitHub-181717?style=for-the-badge&logo=github)](https://github.com/MultiRyzer/ryzer-gen)
[![Issues](https://img.shields.io/github/issues/MultiRyzer/ryzer-gen?style=for-the-badge&label=issues)](https://github.com/MultiRyzer/ryzer-gen/issues)
[![MIT licence](https://img.shields.io/badge/licence-MIT-2E7D32?style=for-the-badge)](https://github.com/MultiRyzer/ryzer-gen/blob/main/LICENSE)
![NeoForge 1.21.1](https://img.shields.io/badge/NeoForge-1.21.1-D9731F?style=for-the-badge)
![Alpha](https://img.shields.io/badge/status-alpha-C62828?style=for-the-badge)

</center>

**Power from understanding, not volume.** Most reactor mods answer "how do I get more power?" with "build it bigger". In Ryzer Gen a small, well-designed machine beats a big sloppy one. Start with a portable microreactor, recycle your spent fuel into MOX, and design a fission station channel by channel, with fusion and a Dyson swarm on the way.

> **Alpha.** Tiers 1 to 3 are playable. Balance is a first pass and may change between versions. Back up your world before updating.

NeoForge 1.21.1 (21.1.249 or newer, so it runs in ATM10). Standalone: it ships every ore it uses. Optional support for JEI, EMI and Accessories.

## What's in the alpha

### Tier 1: the microreactor
- A four-block portable reactor based on real transportable "nuclear battery" designs and TRISO fuel. It snaps together into one machine when the parts are in place, with a ghost preview to show you where they go.
- Heat, fuel life and coolant: it runs dry, but water makes it run better and turns into steam.
- Overdrive: take the safeties off for more power. Lose coolant while they are off and it melts down.
- Modular Battery Rack (a cabinet of lead-acid modules), energy cables, item pipes, fluid and gas pipes, an intake pump, and tall pressure and fluid tanks.
- The Flow Scanner: hold it and every pipe and cable input nearby shows what comes in on it, coloured by how close it is to its limit. The quickest way to find a bottleneck.
- The reactors talk: an emergency-PA announcer calls out safeties, SCRAM, coolant loss, flux tilt and meltdowns as they happen.

### Tier 2: the fuel cycle
- Spent fuel is the next fuel. Crack depleted cores, reprocess them into uranium and plutonium, and fabricate uranium and MOX fuel rods, much as France recycles its fuel.
- Fission waste goes into a Waste Cask that keeps its contents when broken.
- Speed Modules, and cable fittings (silver, busbar, cryogenic) to raise throughput.
- Auto output: the processing machines can push their results straight into a chest, a pipe or the next machine.

![A fuel cycle production line feeding the fission station](PRODUCTION_LINE_IMAGE_URL)

### Tier 3: the fission power station
- A large round station with a glass chamber and a giant roof turbine. Feed the control core its parts and it builds itself.
- Watch your core through the glass: fuel bundles glow green, coolant channels glow Cherenkov blue, and steam fills the chamber while it runs.
- The core is where the depth is: you plan every channel as fuel, moderator, control rod or coolant. Heat, fuel economy and output all come from the layout, and the planner rates your design against the best possible layouts.
- Coolant is your friend: a strong core boils a lot of water, so it needs a real water supply of several intake pumps.
- MOX fuel makes double the power of uranium. SCRAM at 900°C, or turn the safeties off and risk a flux tilt and a meltdown that leaves a crater.

![The core planner rating a layout at 100%](CORE_PLANNER_IMAGE_URL)

![A fission station in the woods](MELTDOWN_BEFORE_IMAGE_URL)
![The same spot after a meltdown](MELTDOWN_CRATER_IMAGE_URL)

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

![A glimpse of the endgame: a Dyson swarm round the sun](SWARM_IMAGE_URL)

**Sneak peek at the endgame.** The creative-only Swarm Controller lets you watch it happen from the ground: a swarm of solar panels launches round the sun, closes into a shell that shuts out the daylight, then collapses the sun into a wormhole that bends the sky round it. It is a preview for creative mode and operators only; the tier that builds it in survival comes later.

Want an early look at the next tier? Set `preview.next_tier = true` in `ryzergen-common.toml`. It is unfinished and its products have no use yet.

Full roadmap and issue tracker: https://github.com/MultiRyzer/ryzer-gen

## For pack makers
- FE energy throughout. Data-driven recipes and worldgen.
- Automation friendly: single-block machines take pipes and cables on any side, and pipes only ever pull results, never ingredients, so AE2 and other storage mods can drive them directly. Multiblocks keep inputs and outputs on separate ports.
- Every recipe asks for common tags (`c:ingots/uranium`, `c:dusts/lead`, ...), so other mods' materials just work.
- Every ore's worldgen has a config toggle, so you can turn off ores another mod already supplies.
- Toggles for radiation, mob radiation, meltdowns, explosion power and crater size, the announcer, the station's water use and the round sun, plus output and capacity settings.
- Built to stay light in big bases: joined pipes and cables share one network, rebuilt only where something changes, and only pipes that pull from something tick.
- Two small client-side mixins, for the sky only (the round sun and the Dyson swarm's darkening). Nothing touches world generation or gameplay code.
- No required dependencies besides NeoForge. MIT licensed: include it in any pack.

## FAQ
- **Fabric or Forge?** No: NeoForge 1.21.1 only.
- **Older versions?** No plans. The mod is built for 1.21.1, the version ATM10 runs on.
- **Can I use it in my modpack?** Yes, any pack. It is MIT licensed.
- **Is radiation dangerous to carry around?** No. Only running reactors and meltdown sites emit it, and it can be turned off.
- **Shader packs?** Yes, it works with Iris and shader packs. The wormhole's light-bending only shows without one; with a shader pack on it shows as a simpler picture.
- **Found a bug?** Please open an issue on GitHub with your log and the steps to reproduce it.

## How it was made
Ryzer Gen is designed by Ryzer. The code was written with help from Claude, an AI assistant. The textures are drawn in code by the mod's own scripts; material textures build on vanilla shapes. Sounds are credited below.

## Credits
The reactor announcer's voice is Ryzer's own, recorded for the mod.

Sounds from Pixabay (Pixabay Content License):
- Microreactor alarm: "Automatic Depressurization System, Nuclear Reactor" by u_whvpuvkdwz
- Fission station alarm: "Alarm Klaxon" by SoundFX for Free
- Turbine: "Underground Alien Reactor" by freesound_community
- Core Cracker: "Factory Grinding" by freesound_community
- Reprocessor: "Basement Water Pump" by freesound_community
- Fuel Fabricator: "Lowering Ramp" by freesound_community
