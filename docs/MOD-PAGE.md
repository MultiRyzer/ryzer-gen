# Mod page (Modrinth and CurseForge)

The text below is the project description. Paste it into Modrinth as it is. CurseForge's normal editor does not read Markdown: either switch its description to the Markdown editor before pasting, or paste `docs/MOD-PAGE.html` (the same page as HTML, written by `python art/tools/mod_page_html.py`; rerun it after editing this file) into the editor's source view.

The page's look comes from pictures, since neither site allows fonts or styles: banners in the design system's web style (Chakra Petch headings, graphite, cyan), drawn in `art/modpage/banners.html` and rendered to `art/gallery/page/` by `python art/tools/mod_page_banners.py` (needs Edge or Chrome, run from PowerShell). Every image links straight to its file on GitHub's `main` branch, so nothing needs uploading by hand: push first, then paste. After changing a banner, render it, push, and the page picks it up. Pictures are at most 840 px wide, under CurseForge's limit.

Summary line for the short description field:

> Nuclear power progression grounded in real physics: a portable microreactor, a real fuel cycle and a fission station you design channel by channel.

---

<center>

![Ryzer Gen: power from understanding, not volume](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/hero.png)

[![Source on GitHub](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/key_github.png)](https://github.com/MultiRyzer/ryzer-gen) [![Join the Discord](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/key_discord.png)](https://discord.gg/cmUZcRtqfg) [![Report a bug](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/key_issues.png)](https://github.com/MultiRyzer/ryzer-gen/issues)

</center>

Most reactor mods answer "how do I get more power?" with "build it bigger". In Ryzer Gen a small, well-designed machine beats a big sloppy one. Start with a portable microreactor, recycle your spent fuel into MOX, and design a fission station channel by channel, with fusion and a Dyson swarm on the way.

> **Alpha.** Tiers 1 to 3 are playable. Balance is a first pass and may change between versions. Back up your world before updating.

NeoForge 1.21.1 (21.1.249 or newer, so it runs in ATM10). Standalone: it ships every ore it uses. Optional support for JEI, EMI and Accessories.

![What's in the alpha: tiers 1 to 3 are playable](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/section_alpha.png)

![Tier 1: the microreactor](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/tier_1.png)

- A four-block portable reactor based on real transportable "nuclear battery" designs and TRISO fuel. It snaps together into one machine when the parts are in place, with a ghost preview to show you where they go.
- Heat, fuel life and coolant: it runs dry, but water makes it run better and turns into steam.
- Overdrive: take the safeties off for more power. Lose coolant while they are off and it melts down.
- Modular Battery Rack (a cabinet of lead-acid modules), energy cables, item pipes, fluid and gas pipes, an intake pump, and tall pressure and fluid tanks.
- The Flow Scanner: hold it and every pipe and cable input nearby shows what comes in on it, coloured by how close it is to its limit. The quickest way to find a bottleneck.
- The reactors talk: an emergency-PA announcer calls out safeties, SCRAM, coolant loss, flux tilt and meltdowns as they happen.

![Tier 2: the fuel cycle](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/tier_2.png)

- Spent fuel is the next fuel. Crack depleted cores, reprocess them into uranium and plutonium, and fabricate uranium and MOX fuel rods, much as France recycles its fuel.
- Spent fuel comes out of a reactor hot. Cool it in the Spent Fuel Pool, a multiblock that builds itself: the rods glow blue under the water while they cool, and a crane lifts them out when they are done.
- The Container Battery: grid-scale storage in a shipping container. Slot LFP battery racks into its 20 hatches, and keep it cooled.
- Every machine moves while it works: flywheels spin, the press strokes, the lithium columns pulse with light.
- Fission waste goes into a Waste Cask that keeps its contents when broken.
- Speed Modules, and cable fittings (silver, busbar, cryogenic) to raise throughput. The processing machines can push their results straight into a chest, a pipe or the next machine.

![A fuel cycle production line feeding the fission station](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/curseforge/03_production_line.jpg)

![Tier 3: the fission power station](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/tier_3.png)

- A large round station with a glass chamber and a giant roof turbine. Feed the control core its parts and it builds itself.
- Watch your core through the glass: fuel bundles glow green, coolant channels glow Cherenkov blue, steam fills the chamber, and the rods rise into the core when it starts.
- The core is where the depth is: you plan every channel as fuel, moderator, control rod or coolant. Heat, fuel economy and output all come from the layout, and the planner rates your design against the best possible layouts.
- Coolant is your friend: a strong core boils a lot of water, so it needs a real water supply of several intake pumps.
- MOX fuel makes double the power of uranium. SCRAM at 900°C, or turn the safeties off and risk a flux tilt and a meltdown that leaves a crater.

![The core planner rating a layout at 100%](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/02_core_planner.png)

![Radiation: only running reactors and meltdown sites](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/section_radiation.png)

- Radiation comes **only** from running reactors and meltdown sites. Ores, fuel and waste are safe to carry.
- Dose falls off with distance and is blocked by shielding. Effects build up slowly and never kill instantly outside a meltdown. Mobs are affected too.
- The Geiger counter clicks and shows a HUD gauge. The dosimeter ring gives 25% protection (ring slot with Accessories).
- Turn it all off in the config if you prefer.

![A fission station in the woods](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/curseforge/04_meltdown_before.jpg)
![The same spot after a meltdown](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/curseforge/04_meltdown_crater.jpg)

![The endgame: harness the sun](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/section_endgame.png)

![A Dyson swarm round the sun](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/curseforge/fission_station_swarm.jpg)

A swarm of solar panels launches round the sun, closes into a shell that shuts out the daylight, then collapses the sun into a wormhole that bends the sky round it. For now it is a preview through the creative-only Swarm Controller, for creative mode and operators; the tier that builds it in survival comes later.

![Roadmap: from a portable reactor to a star](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/section_roadmap.png)

![Roadmap: tiers 1 to 3 in the alpha, the fission to fusion bridge in progress, then the breeder reactor, fusion and the Dyson swarm](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/roadmap.png)

Want an early look at the next tier? Set `preview.next_tier = true` in `ryzergen-common.toml`. It is unfinished and its products have no use yet.

Full roadmap and issue tracker on [GitHub](https://github.com/MultiRyzer/ryzer-gen). Join the [Discord](https://discord.gg/cmUZcRtqfg) for help, core designs, work in progress and a say in what comes next.

![For pack makers](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/section_packs.png)

- FE energy throughout. Data-driven recipes and worldgen.
- Automation friendly: single-block machines take pipes and cables on any side, and pipes only ever pull results, never ingredients, so AE2 and other storage mods can drive them directly. Multiblocks keep inputs and outputs on separate ports.
- Every recipe asks for common tags (`c:ingots/uranium`, `c:dusts/lead`, ...), so other mods' materials just work.
- Every ore's worldgen has a config toggle, so you can turn off ores another mod already supplies.
- Toggles for radiation, mob radiation, meltdowns, explosion power and crater size, the announcer, the station's water use, fuel cooling and the round sun, plus output and capacity settings.
- Built to stay light in big bases: joined pipes and cables share one network, rebuilt only where something changes, and only pipes that pull from something tick. Moving parts are drawn with plain block models, no animation library.
- Two small client-side mixins, for the sky only (the round sun and the Dyson swarm's darkening). Nothing touches world generation or gameplay code.
- No required dependencies besides NeoForge. MIT licensed: include it in any pack.

![FAQ](https://raw.githubusercontent.com/MultiRyzer/ryzer-gen/main/art/gallery/page/section_faq.png)

- **Fabric or Forge?** No: NeoForge 1.21.1 only.
- **Older versions?** No plans. The mod is built for 1.21.1, the version ATM10 runs on.
- **Can I use it in my modpack?** Yes, any pack. It is MIT licensed.
- **Is radiation dangerous to carry around?** No. Only running reactors and meltdown sites emit it, and it can be turned off.
- **Shader packs?** Yes, it works with Iris and shader packs. The wormhole's light-bending only shows without one; with a shader pack on it shows as a simpler picture.
- **Found a bug?** Please open an issue on GitHub with your log and the steps to reproduce it. For questions, ask in `#help` on the [Discord](https://discord.gg/cmUZcRtqfg).

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
