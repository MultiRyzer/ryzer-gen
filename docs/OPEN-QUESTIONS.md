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
9. **Power-spending machines.** Add things like an automated miner or powered tools and armour, or keep the mod to power and processing? Leaning towards our own quarry: a microreactor runs it slowly, and more power runs it faster. It gives bigger reactors a job, supplies uranium and fluorite for the fuel cycle in standalone play, and feeds later builds. To settle: its speed per FE, whether it upgrades by modules (rule 11) rather than a new block per tier, and a config toggle so packs that already have quarries can turn it off.
10. **Pyroprocessor name.** A working name for the machine that turns spent MOX into transuranic metal. Pick an original name before it is built.
11. **Breeder blanket.** Is breeding part of the breeder's core layout (blanket channels round the fuel, like the station's grid), or a separate blanket block or machine?
12. **Spent breeder fuel.** What comes out of spent breeder fuel: more transuranic metal (a closed loop), waste only, or a feed for fusion?
13. **Sodium form.** Sodium as a dust, an ingot, or only a liquid coolant fluid. Real sodium is a soft metal kept away from water, which could be a hazard mechanic.
14. **Americium and curium.** Keep them folded into transuranic metal, or split them out later (americium-241 for a radioisotope battery, say)?
15. **Where the breeder sits.** With the fission station breeding tritium, fusion no longer needs the breeder. Keep fission, breeder, fusion in line, or make the breeder an optional branch (fuel supply and thorium) with fusion straight after fission? Leaning towards the branch.
16. **How tritium is stored.** A gas in pressure tanks and gas pipes (we have both), or canisters (real tritium is often stored bound in metal, as a hydride)?
17. **Uranium glass.** Decoration only, or also the fusion reactor's viewing windows (a fudge)?
