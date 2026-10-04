# Open Questions

Decisions still to make. Move them into DESIGN.md once settled.

1. **Anchor sail count.** Currently 20 as a placeholder. Depends on the maximum swarm size and how expensive one sail is to make.
2. **Where the Dyson swarm sits.** Final endgame after fusion, or a parallel path you can take instead of the nuclear route?
3. **Meltdown default.** Which behaviour ships as the default: safe shutdown, or something with consequences?
4. **Energy scale.** How much FE per tick for each tier, so it sits well alongside Mekanism and Powah in ATM10.
5. **Beyond the swarm.** Do aneutronic fusion, antimatter or Kugelblitz ever come in, or does the mod end at the sun?
6. **Microreactor fuel.** Does a new reactor come with its first fuel core, or does the player craft one separately?
7. **Coolant block name.** Registered as "Coolant Jacket" (`coolant_jacket`) as a working name. Confirm or rename before release.
8. **Middle battery tier.** A fridge-sized commercial cabinet between the home stack and the container, or go straight from house to container?
9. Settled 3 Oct 2026: our own miner, the Melt Drill, a rock-melting drill that goes straight down a chunk quarter, with speed modules, a filter and a wearing tungsten tip. See DESIGN.md section 11c. (Powered tools and armour are still open.)
10. Settled 30 Sep 2026: the Electrorefiner, one molten salt cell that also splits salt into sodium. See DESIGN.md section 9.
11. Settled 30 Sep 2026: a ring of blanket slots in the breeder's planning grid. See DESIGN.md section 9.
12. Settled 30 Sep 2026: a closed loop, back through the pyroprocessor. See DESIGN.md section 9.
13. Settled 30 Sep 2026: an ingot for recipes and liquid sodium for the coolant; a sodium fire if it meets water. See DESIGN.md section 9.
14. Settled 30 Sep 2026: kept folded into transuranic metal; split out only if a use appears. See DESIGN.md section 9.
15. Settled 30 Sep 2026: the breeder sits in line, and fusion needs it (its lithium blanket is the only source of tritium). See DESIGN.md section 9.
16. Settled 30 Sep 2026: a gas in gas pipes and pressure tanks. See DESIGN.md section 9.
17. **Uranium glass.** Decoration only, or also the fusion reactor's viewing windows (a fudge)?
18. **Fusion helium.** What is the helium from the divertor for: vented, a coolant for a later tier, or decoration?
19. **Fusion heat out.** FE straight out of the base (as the station does), or steam out to a turbine hall the player builds?
20. **Stepping through the sun gate.** How does the player go in: walk up the dais into the sun (it swells round them and the screen whites out), or use the console? And what does the gate cost to run: FE per trip, or a steady draw to hold the sun?
21. **The better station coolant.** Which fluid, and where it comes from? Candidates: sodium from tier 4 (the breeder's coolant, so a tier 4 product upgrades tier 3; real sodium carries far more heat than water but never boils, so it would need a heat exchanger, a fudge), or pressurised water (boils hotter, so each channel carries more; cheap, but only a small step). Leaning to sodium: it fits "every tier stays useful".
22. **The mega drill's deep bore.** Once it reaches bedrock it bores on into the deep crust for better ore (DESIGN.md section 11c). How to balance it: which ores each drill head reaches, its draw and tip wear, ore per cycle, and whether it needs a better coolant. Set against ATM10's void miners.
