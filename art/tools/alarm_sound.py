"""Builds the microreactor's coolant-loss alarm loop from art/sounds/source/nuclear_alarm.mp3.

Source: "Nuclear Alarm" from the freesound_community account on Pixabay (Pixabay Content License).
Credit it on the mod page.

The source is two identical tone pulses 1.09 s apart. We keep exactly one cycle (pulse plus gap),
mono, so it loops seamlessly. The game then plays it looping and raises the pitch as the core
heats towards meltdown, which speeds the beeping up and bends the tone upwards together
(see MicroreactorAlarmSound).

Needs numpy and soundfile (pip install soundfile numpy). Run from the repo root:
    python art/tools/alarm_sound.py
"""
import os

import numpy as np
import soundfile as sf

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SOURCE = os.path.join(ROOT, 'art', 'sounds', 'source', 'nuclear_alarm.mp3')
OUT = os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'sounds', 'microreactor_alarm.ogg')

# One cycle, measured from the source: pulses start at 0.07 s and 1.16 s. Both cut points sit in
# silence, so the loop joins without a click.
START = 0.05
PERIOD = 1.09


def main():
    data, rate = sf.read(SOURCE)
    mono = data.mean(axis=1) if data.ndim > 1 else data
    a = int(START * rate)
    loop = mono[a:a + int(PERIOD * rate)].copy()
    fade = int(0.004 * rate)
    loop[:fade] *= np.linspace(0, 1, fade)
    loop[-fade:] *= np.linspace(1, 0, fade)
    loop *= 0.9 / np.max(np.abs(loop))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    sf.write(OUT, loop.astype(np.float32), rate, format='OGG', subtype='VORBIS')
    print('wrote', OUT, f'{len(loop) / rate:.2f} s, {os.path.getsize(OUT) // 1024} KB')


if __name__ == '__main__':
    main()
