# Changelog

Player-facing changes, newest first. Paste each release's section into the Modrinth and CurseForge changelog and the GitHub release notes.

## 0.1.1 (unreleased)

### New
- **Reactor announcer.** The microreactor and the fission station now speak when something worth hearing happens: safeties off and on, SCRAM, overheating, flux tilt, coolant loss, meltdown imminent, and coming online after a shutdown. It sounds like an emergency PA. Turn it down with the vanilla Voice/Speech volume slider, or off for everyone with `announcer.enabled` in the common config.

### Changed
- **Coolant is your friend.** The fission station boils 4 times as much water, and 8 times in overdrive. A strong core now needs several intake pumps (about 5 for the best uranium layout, 9 for MOX, twice that in overdrive). `fission_station.water_use_percent` scales it.

### Fixed
- Intake pumps only pushed water out of their top, so a pipe joined to a pump's side got no water unless it was set to extract. Pumps now push out of every side but the bottom.
- The fluid pipe panel counted intake pumps as tanks.
