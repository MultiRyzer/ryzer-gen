# Changelog

Player-facing changes, newest first. Paste each release's section into the Modrinth and CurseForge changelog and the GitHub release notes.

## 0.1.1 (unreleased)

### New
- **Reactor announcer.** The microreactor and the fission station now speak when something worth hearing happens: safeties off and on, SCRAM, overheating, flux tilt, coolant loss, meltdown imminent, and coming online after a shutdown. It sounds like an emergency PA. Turn it down with the vanilla Voice/Speech volume slider, or off for everyone with `announcer.enabled` in the common config.

- **Auto output** button on the Core Cracker, Reprocessor, Fuel Fabricator and Lithium Extractor (top left of the machine area, a hopper key). When on, the machine pushes its results into the blocks beside it: a chest, another machine or a pipe. Off by default, so machines that only touch never hand each other things.

### Changed
- **Coolant is your friend.** The fission station boils 4 times as much water, and 8 times in overdrive. A strong core now needs several intake pumps (about 5 for the best uranium layout, 9 for MOX, twice that in overdrive). `fission_station.water_use_percent` scales it.

### Performance
- Pipes and cables share one network per group of joined pipes, scanned once, and a change only rebuilds the networks it touches. Before, placing or wrenching any pipe anywhere made every cable in the world rescan its whole network, which could stutter in big bases.
- Energy cables only tick when they have an extract side, like item and fluid pipes already did.
- Networks no longer stop at 4,096 blocks, and scans no longer load chunks.

### Fixed
- Intake pumps only pushed water out of their top, so a pipe joined to a pump's side got no water unless it was set to extract. Pumps now push out of every side but the bottom, into pipes first (so a pipe's panel shows the pump's full output) and into a tank or machine beside them only with what is left. A pipe set to extract from a pump takes all of it.
- The fluid pipe panel counted intake pumps as tanks.
