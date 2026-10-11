# Reactor voice announcer: script

Voiced with an AI voice generator (11 Oct 2026; engine, model and licence in `art/sounds/CREDITS.md`), one file per line named after its words, in `art/sounds/source/voice/` (ignored by git). `art/tools/voice_lines.py` gives them the PA-speaker character. Each file name below is the sound event it becomes. See the roadmap item "Reactor voice announcer".

Only things worth hearing get a line: no "fuel low" warning, since with automation fuel is always running down and being replaced.

Delivery: a control system stating facts. Calm for routine lines, firmer for cautions, urgent but clear for danger. Plain speech; the speaker character is added afterwards.

## Routine (calm, even, a little clipped)
| File | Line | Plays when |
|---|---|---|
| `voice_reactor_online.wav` | "Reactor online." | The microreactor starts from a real shutdown: switched on, or refuelled after running out. Not on a routine core swap by pipe |
| `voice_station_online.wav` | "Station online. All systems nominal." | The fission station finishes building or comes online |
| `voice_safeties_on.wav` | "Safeties engaged." | Safeties are switched back on |
| `voice_no_fuel.wav` | "Reactor offline. No fuel." | The reactor has stopped for lack of fuel: its core is spent and no fresh one has arrived (never on a routine swap by pipe) |

## Caution (firmer, slightly slower, still controlled)
| File | Line | Plays when |
|---|---|---|
| `voice_safeties_off.wav` | "Warning. Safeties disengaged. Overdrive active." | Safeties are switched off |
| `voice_overheat.wav` | "Warning. Core temperature rising." | The core climbs towards its limit |
| `voice_scram.wav` | "SCRAM. Emergency shutdown." | The station SCRAMs ("scram" as one word) |
| `voice_controlled_shutdown.wav` | "Safeties engaged. Controlled shutdown." | The player re-arms the safeties and that shuts the station down |
| `voice_flux_tilt.wav` | "Warning. Flux tilt detected." | The station's overdrive countdown starts |

## Danger (urgent, no shouting)
| File | Line | Plays when |
|---|---|---|
| `voice_coolant_loss.wav` | "Alert. Coolant loss." | The microreactor loses water in overdrive |
| `voice_meltdown_risk.wav` | "Danger. Meltdown imminent. Evacuate." | The last stage before a meltdown |

## For features still to come (voiced 11 Oct 2026)
Voiced and registered, but nothing plays them yet: they wait for the features below.

| File | Line | Tone | Plays when |
|---|---|---|---|
| `voice_no_fuel.wav` | "Reactor offline. No fuel." | Routine | As above |
| `voice_steady_state_1.wav` | "Steady state reached." | Routine | A reactor's streak reaches 32 loads (design section 5) |
| `voice_steady_state_2.wav` | "Steady state holding. Output rising." | Routine | 128 loads |
| `voice_steady_state_3.wav` | "Peak steady state. Maximum output." | Routine | 512 loads, the tier's cap |
| `voice_breeder_online.wav` | "Breeder online. Sodium loop nominal." | Routine | The breeder finishes building or comes online |
| `voice_sodium_low.wav` | "Warning. Sodium loop low." | Caution | The breeder's sodium loop drops below what it needs |
| `voice_hot_spot.wav` | "Warning. Hot spot detected." | Caution | An assembly passes the hot spot limit |
| `voice_sodium_fire.wav` | "Alert. Sodium fire." | Danger | Sodium meets water: the breeder's failure (design section 9) |

## Recording
- Two or three takes per line, about a second of silence before and after.
- Same room, spot and mic distance for every line. No hum nearby.
- WAV, 44.1 or 48 kHz, mono is fine. One file per line, or one file of takes to be cut.
