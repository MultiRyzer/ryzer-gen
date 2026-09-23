# Roadmap

Build in thin, playable slices. Each milestone should load in game and be tested before the next starts.

## Milestone 0: Setup
- [ ] Install Java 21 JDK and IntelliJ IDEA Community (if not already)
- [ ] Create the mod project from the NeoForge 1.21.1 template into `mod/`
- [ ] Pick a mod name and mod ID
- [ ] Confirm `runClient` launches a test world with the mod loaded
- [ ] First commit to Git (optional: push to a private GitHub repo)

## Milestone 1: Microreactor (first playable)
- [ ] Ores needed for tier 1 (uranium, lead) with worldgen, common tags and config toggles
- [ ] Steel and graphite recipes
- [ ] Register reactor heart, reactor machine unit, coolant block and packed microreactor
- [ ] Multiblock detection for the 4-block layout (heart in any position)
- [ ] Snap assembly: combined model, particles, sound
- [ ] Break any block to disassemble
- [ ] Energy generation, fuel life, depletion into a depleted core
- [ ] Simple heat model with coolant bonus
- [ ] Control unit GUI (charge, temperature, output, on/off, redstone)
- [ ] Fixed connection chutes on the assembled model
- [ ] Wrench pack-up and unfold, keeping charge
- [ ] Ghost placement preview
- [ ] Placeholder textures, then proper 16x16 art
- [ ] Recipes and config values
- [ ] Basic radiation (dose, effects, config toggle)
- [ ] Dosimeter ring with Curios slot support and inventory fallback

## Milestone 1b: Home battery
- [ ] Home battery stack cabinet with 6 module slots and segment model
- [ ] Lead-acid module
- [ ] Basic cables

## Milestone 2: Fuel cycle
- [ ] Fluorite ore
- [ ] Core cracker, reprocessor, fuel fabricator
- [ ] Recovered uranium, plutonium, waste, MOX fuel items
- [ ] Waste storage block
- [ ] Unlock trigger for the fission reactor

## Milestone 3: Modular fission reactor
- [ ] Shared machine framework (energy, fluids, GUI, upgrades) reused from here on
- [ ] Multiblock with layout-based heat and efficiency
- [ ] Planner block
- [ ] Fuel types with different behaviour
- [ ] Configurable meltdowns

## Milestone 4: Sun dimension prototype
- [ ] Superflat glowing dimension and portal
- [ ] Protective gear and visor whiteout
- [ ] Hand-launched anchor sails and swarm counter
- [ ] Sky renderer showing swarm coverage

## Later
- Breeder reactor and thorium path
- Fusion
- Sail launcher automation and receiver dish
- Overworld sky ring, flare events, multiplayer shared swarm
- Mod integrations (Mekanism, EMI/JEI)
- Public release on CurseForge and Modrinth
