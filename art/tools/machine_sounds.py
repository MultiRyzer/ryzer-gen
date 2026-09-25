"""Builds the mod's sound loops from the recordings in art/sounds/source/ into
mod/src/main/resources/assets/ryzergen/sounds/ (mono Ogg Vorbis).

Each loop is cut from a steady stretch of its recording. Where the sound repeats (an alarm's
beep), the loop is a whole number of cycles, its length tuned to the sample so the end runs
straight back into the start; the rest are joined with a crossfade, the loop's tail blended into
its head, so there is no click or gap. Each is then levelled to a common loudness so the machines
sit together in game (sounds.json sets the final volumes).

Sources, all from Pixabay (Pixabay Content License). Credit them on the mod page:
    microreactor_alarm  "Automatic Depressurization System, Nuclear Reactor" by u_whvpuvkdwz
    station_alarm       "Alarm Klaxon" by SoundFX for Free (soundfxforfree)
    station_hum         "Underground Alien Reactor" by freesound_community
    core_cracker        "Factory Grinding" by freesound_community
    reprocessor         "Basement Water Pump" by freesound_community
    fuel_fabricator     "Lowering Ramp" by freesound_community

Needs numpy and soundfile (pip install soundfile numpy). Run from the repo root:
    python art/tools/machine_sounds.py
"""
import os

import numpy as np
import soundfile as sf

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SOURCES = os.path.join(ROOT, 'art', 'sounds', 'source')
OUT = os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'sounds')

# name: (source file, start s, length s, crossfade s, (repeat period s, cycles) or None, loudness dBFS RMS)
LOOPS = {
    # Sixteen beeps from the steady middle; the client pitches it up towards meltdown.
    'microreactor_alarm': ('microreactor_alarm_depressurization.mp3', 1.0, None, 0.02, (0.458, 16), -18),
    # Two blasts of the klaxon (it repeats exactly every 0.944 s), after its start.
    'station_alarm': ('station_alarm_klaxon.mp3', 1.5, None, 0.02, (0.944, 2), -18),
    # A long stretch of the windy reactor drone, well after it fades in.
    'station_hum': ('station_hum_underground_reactor.mp3', 9.0, 14.0, 1.5, None, -22),
    'core_cracker': ('core_cracker_factory_grinding.mp3', 5.0, 5.0, 0.6, None, -21),
    'reprocessor': ('reprocessor_basement_water_pump.mp3', 1.5, 13.0, 0.6, None, -21),
    'fuel_fabricator': ('fuel_fabricator_lowering_ramp.mp3', 0.25, 9.5, 0.4, None, -21),
}
def mono(path):
    data, rate = sf.read(path)
    return (data.mean(axis=1) if data.ndim > 1 else data), rate


def tune_length(signal, rate, start, length, search=0.02, window=0.2):
    """The loop length near `length` whose end best matches its start, to the sample."""
    s, w = int(start * rate), int(window * rate)
    head = signal[s:s + w]
    best, best_score = int(length * rate), -2.0
    for n in range(int((length - search) * rate), int((length + search) * rate)):
        tail = signal[s + n:s + n + w]
        score = np.dot(head, tail) / (np.linalg.norm(head) * np.linalg.norm(tail) + 1e-9)
        if score > best_score:
            best, best_score = n, score
    return best, best_score


def loop(signal, rate, start, n, fade):
    """n samples from `start`, with the next `fade` seconds blended into the head (equal power),
    so the last sample runs on into the first as the recording itself did."""
    s, f = int(start * rate), int(fade * rate)
    out = signal[s:s + n].copy()
    t = np.linspace(0, np.pi / 2, f)
    out[:f] = out[:f] * np.sin(t) + signal[s + n:s + n + f] * np.cos(t)
    return out


def level(signal, target_db):
    rms = np.sqrt(np.mean(signal ** 2))
    out = signal * (10 ** (target_db / 20) / rms)
    peak = np.abs(out).max()
    return out * (0.95 / peak) if peak > 0.95 else out


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, (source, start, length, fade, period, loudness) in LOOPS.items():
        signal, rate = mono(os.path.join(SOURCES, source))
        if period:
            length = period[0] * period[1]
        n, match = tune_length(signal, rate, start, length)
        out = level(loop(signal, rate, start, n, fade), loudness)
        sf.write(os.path.join(OUT, name + '.ogg'), out, rate, format='OGG', subtype='VORBIS')
        print(f'{name}: {n / rate:.3f} s at {rate} Hz, seam match {match:.2f}')


if __name__ == '__main__':
    main()
