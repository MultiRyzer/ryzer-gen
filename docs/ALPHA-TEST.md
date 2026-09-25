# Alpha test run

One pass through everything the 0.1.0 alpha ships, before it goes public. Tick things off as you go, and write down anything that is broken, confusing, slow or grindy in the notes at the bottom. "Confusing" counts: if you had to open JEI or the code to work something out, a new player will be stuck there too.

## Setup
- [ ] Build the jar (`./gradlew build`) and copy `mod/build/libs/ryzergen-0.1.0-alpha.jar` into a clean NeoForge 1.21.1 instance (the normal launcher, not `runClient`)
- [ ] First run with **only** Ryzer Gen installed. Then add JEI for a second pass, and Accessories for the ring
- [ ] New survival world, normal difficulty
- [ ] The mods list shows the icon, the description, MIT and the GitHub link

## 1. Getting started
- [ ] Find each ore without cheating: uranium, lead, silver, aluminium, fluorite, salt, tungsten, monazite. Note roughly how long each took
- [ ] Ores drop the right raw item, and smelt or blast into ingots
- [ ] Alloy smelter: burns furnace fuel, makes steel. Graphite from blasting
- [ ] The advancement tab starts on the first ore and leads you on

## 2. The microreactor
- [ ] Holding a part shows the ghost outline and suggested parts
- [ ] It snaps together (model, particles, sound) with the heart in any of the four positions
- [ ] Breaking any block takes it apart and gives the parts back
- [ ] Runs dry at about 200 FE/t. With water from an intake pump it runs better and steam comes out
- [ ] The control unit GUI: every value makes sense, power and redstone buttons work
- [ ] Fuel hatch: fresh cores go in by pipe, spent cores come back out
- [ ] Overdrive: safeties off, cut the water, the alarm sounds, and it melts down (then again with `meltdowns = false`: it should re-arm instead)

## 3. Power network
- [ ] Energy cables auto-connect; the wrench sets extract and disconnect; the cable panel shows sensible numbers
- [ ] Home battery: add modules, it fills from surplus and covers shortfalls. Fill it for the advancement
- [ ] Item, fluid and gas pipes move what they should, at the rates their panels say
- [ ] Pressure tank and fluid tank towers form at any height up to 16 and show their contents
- [ ] Fittings (silver, busbar, cryogenic) slot in and raise the limit

## 4. Fuel cycle
- [ ] Deplete a fuel core, crack it, reprocess it, get uranium, plutonium nuggets and waste
- [ ] Fuel Fabricator: uranium rods, MOX rods, TRISO pellets
- [ ] Waste Cask fills and keeps its contents when broken
- [ ] Speed Modules make machines faster and hungrier
- [ ] Each machine's sound loops cleanly while it runs and stops when it stops

## 5. Fission station
- [ ] Craft the parts; the ghost outline shows the shape
- [ ] Feed the core its parts (by hand and by pipe) and it builds itself
- [ ] Plan a core in the grid. The rating and tooltips help you improve it
- [ ] Water, fuel and power ports work; spent rods leave by the output port
- [ ] The turbine spins and hums, steam rises from the stack
- [ ] SCRAM at 900°C. Overdrive: flux tilt countdown, then a meltdown and crater (and with meltdowns off)
- [ ] A full MOX core makes about double the power
- [ ] FPS stays reasonable looking at a running station, with and without a shader pack

## 6. Radiation
- [ ] Nothing radiates except running reactors and meltdown sites
- [ ] Geiger counter clicks and shows the gauge; shift-right-click mutes it
- [ ] Dosimeter ring protects; worn in the Accessories ring slot it shows as a band on the hand (check in third person, F5)
- [ ] Effects build slowly; mobs near a reactor are affected
- [ ] `radiation.enabled = false` turns it all off

## 7. Hidden preview
- [ ] With the default config there is no Lithium Extractor, lithium or target rod in the creative tab, JEI, or any recipe, and the station grid has no target tool
- [ ] Set `preview.next_tier = true`, restart: all of it comes back

## 8. Multiplayer
- [x] Starts on a dedicated server without crashing (`./gradlew runServer`, 25 Sep 2026: loads, generates a world, no errors)
- [ ] Place and run a microreactor, a fuel cycle machine and the station on a server, with a client joined
- [ ] Two players can see the same reactor running, animations and sounds included

## 9. Inside ATM10
- [ ] Add the jar to an ATM10 instance. It loads, recipes show in JEI, and other mods' ores work in our recipes (common tags)
- [ ] Nothing clashes with ores or items from Mekanism and friends

## Notes
Write anything odd here, with what you were doing when it happened.

- 
