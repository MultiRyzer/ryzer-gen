# Reactor voice announcer: script

Recorded by Ryzer, then enhanced (Voicebox, Qwen models, Apache 2.0). Raw takes go in `art/sounds/source/` (ignored by git). Each file name is the sound event it becomes. See the roadmap item "Reactor voice announcer".

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
| `voice_flux_tilt.wav` | "Warning. Flux tilt detected." | The station's overdrive countdown starts |

## Danger (urgent, no shouting)
| File | Line | Plays when |
|---|---|---|
| `voice_coolant_loss.wav` | "Alert. Coolant loss." | The microreactor loses water in overdrive |
| `voice_meltdown_risk.wav` | "Danger. Meltdown imminent. Evacuate." | The last stage before a meltdown |

## Recording
- Two or three takes per line, about a second of silence before and after.
- Same room, spot and mic distance for every line. No hum nearby.
- WAV, 44.1 or 48 kHz, mono is fine. One file per line, or one file of takes to be cut.
