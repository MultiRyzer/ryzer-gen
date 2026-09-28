# Changelog

Player-facing changes, newest first. Paste each release's section into the Modrinth and CurseForge changelog and the GitHub release notes.

## 0.1.1 (unreleased)

### New
- **Reactor announcer.** The microreactor and the fission station now speak when something worth hearing happens: safeties off and on, SCRAM, overheating, flux tilt, coolant loss, meltdown imminent, and coming online after a shutdown. Re-arming the safeties while the core is hot is announced as a controlled shutdown, not a SCRAM alarm. It sounds like an emergency PA. Turn it down with the vanilla Voice/Speech volume slider, or off for everyone with `announcer.enabled` in the common config.

- **Auto output** on the Core Cracker, Reprocessor, Fuel Fabricator and Lithium Extractor: a tab on the right edge of the machine's screen, with a hopper key and a light that glows green while it is on. When on, the machine pushes its results into the blocks beside it: a chest, another machine or a pipe. Off by default, so machines that only touch never hand each other things.

- **The fission station's new look.**
  - The chamber's plain ribs are now I-beams in gunmetal shoes, with copper coolant pipes running down every other one.
  - Every part of the station has its own texture now.
  - The stack is clad in big panels and carries the station's number, an access hatch, and red and white aviation marking with red beacons.
  - The port housings have a hood with a lit tag in each port's colour.
  - Each kind of rod looks different through the glass: fuel bundles glow green, coolant channels glow Cherenkov blue, and steam fills the chamber while the station runs.

- **A round sun.** The overworld's sun is a glowing round disc instead of vanilla's square. Turn it off with `sky.round_sun` in the common config.

- **A first look at the Dyson swarm.**
  - The creative-only Swarm Controller launches a swarm of 640 solar panels round the sun, and you can watch it from the ground.
  - Use it again and the panels fly in and lock together into a shell. The far side closes first and the last gap shuts like an iris, and the daylight goes out.
  - While the sun is enclosed, the sky stays a dark ember orange and the world stays dim. The moon goes dark too, since it has no sunlight to reflect.
  - Use it once more and the enclosed sun collapses to a white flash. A wormhole opens where it was, bending the sky round it into a ring, and through it you can see a new star somewhere else. With a shader pack on, it shows as a simpler picture instead.
  - Sneak and use it to light a new sun.
  - Only operators can use it. The tier that builds the swarm for real comes later.

- **Flow Scanner.** Hold it and every pipe and cable input nearby shows what comes in on it, as a floating label coloured by how close that input is to its limit (green idle, yellow half, red at the limit). The quickest way to find a bottleneck in a big network.

### Changed
- **Coolant is your friend.** The fission station boils 4 times as much water, and 8 times in overdrive. A strong core now needs several intake pumps (about 5 for the best uranium layout, 9 for MOX, twice that in overdrive). `fission_station.water_use_percent` scales it.

### Performance
- Pipes and cables share one network per group of joined pipes, scanned once, and a change only rebuilds the networks it touches. Before, placing or wrenching any pipe anywhere made every cable in the world rescan its whole network, which could stutter in big bases.
- Energy cables only tick when they have an extract side, like item and fluid pipes already did.
- Networks no longer stop at 4,096 blocks, and scans no longer load chunks.

### Fixed
- Intake pumps only pushed water out of their top, so a pipe joined to a pump's side got no water unless it was set to extract. Pumps now push out of every side but the bottom, into pipes first (so a pipe's panel shows the pump's full output) and into a tank or machine beside them only with what is left. A pipe set to extract from a pump takes all of it.
- The fluid pipe panel counted intake pumps as tanks.
- With a shader pack on, the ghost outlines of an unbuilt microreactor or station cast shadows. They no longer do.
