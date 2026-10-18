# Sound credits

Every recording below is from Pixabay, under the Pixabay Content License. Credit them on the mod page. `art/tools/machine_sounds.py` cuts them into the loops and one-shots the mod ships.

The original recordings are not in this repository: the licence allows using them inside the mod, not redistributing them as they are. To rebuild the sounds, download each one from Pixabay into `art/sounds/source/` (ignored by git).

| Sound in the mod | Recording | Author |
|---|---|---|
| Microreactor coolant-loss alarm | Automatic Depressurization System, Nuclear Reactor | u_whvpuvkdwz |
| Fission station alarm | Alarm Klaxon | SoundFX for Free (soundfxforfree) |
| Fission station turbine | Underground Alien Reactor | freesound_community |
| Core Cracker | Factory Grinding | freesound_community |
| Reprocessor | Basement Water Pump | freesound_community |
| Fuel Fabricator | Lowering Ramp | freesound_community |
| Mega drill beam firing | Short Energy Beam Shot | yodguard |
| Mega drill layer bursting | Boulder Impact | Dragon Studio |

The reactor announcer's voice lines (`sounds/voice/`) are generated with ElevenLabs text to speech in one of its preset voices (no cloned voice, 11 Oct 2026), then given their PA-speaker character by `art/tools/voice_lines.py`. They were made on the free plan, which allows non-commercial use with attribution: the mod is a free hobby that earns nothing. Credit on the mod page: "Announcer voice generated with ElevenLabs (elevenlabs.io)". If the mod ever earns money (CurseForge rewards, donations, a paid pack), regenerate every line on a paid plan first, since free-plan audio is not licensed for that and upgrading later does not cover it.

The microreactor's running hum is vanilla's beacon hum, pitched down, the mega drill's charging hum is vanilla's beacon powering up, slowed, and the dosimeter click is vanilla's note block hi-hat, pitched up. None of these files is shipped.

The fission station control screen's sounds (`sounds/console_*.ogg`: ticks, place sounds, the climbing note, the three-star fanfare, inject and eject) are synthesised by `art/tools/console_sounds.py` from oscillators and filtered noise, after the core puzzle reference page's Web Audio recipes. Nothing is recorded, so nothing needs a licence or credit.
