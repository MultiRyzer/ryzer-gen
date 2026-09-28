"""Builds the reactor announcer's voice lines from Ryzer's recording (art/sounds/VOICE-SCRIPT.md).

Reads one recording of every take and the label file Audacity exported with it, both in
art/sounds/source/ (ignored by git). Each label marks where a line starts; the line runs to the next
label. For every line it:
  1. trims the silence round it, keeping a short breath of room at each end,
  2. gently lowers the room noise between words (a soft expander, not a gate that chops),
  3. gives it a control-room speaker character: less low end, a little more presence, the top
     rolled off above the speaker's range, light compression and a short, quiet room echo,
  4. levels it to a common loudness with a peak ceiling, so every line sits together in game.
Then it writes mono Ogg Vorbis to mod/src/main/resources/assets/ryzergen/sounds/voice/ and WAV
previews (before and after) to art/sounds/voice_preview/ (ignored by git) to listen to.

Needs numpy and soundfile. Run from the repo root:
    python art/tools/voice_lines.py
"""
import glob
import os
import re

import numpy as np
import soundfile as sf

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SOURCES = os.path.join(ROOT, 'art', 'sounds', 'source')
RECORDING = os.path.join(SOURCES, 'Voice Takes.wav')
OUT = os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'sounds', 'voice')
PREVIEW = os.path.join(ROOT, 'art', 'sounds', 'voice_preview')

# The label text, lower case with punctuation dropped, to the sound's name.
LINES = {
    'reactor online': 'reactor_online',
    'station online all systems nominal': 'station_online',
    'safeties engaged': 'safeties_on',
    'reactor offline no fuel': 'no_fuel',
    'warning safeties disengaged overdrive active': 'safeties_off',
    'scram emergency shutdown': 'scram',
    'warning core temperature rising': 'overheat',
    'alert coolant loss': 'coolant_loss',
    'danger meltdown imminent evacuate': 'meltdown_risk',
    'warning flux tilt detected': 'flux_tilt',
}

TARGET_RMS_DB = -18.0      # loudness of the spoken part
PEAK_CEILING_DB = -1.0     # never louder than this
HEAD_PAD, TAIL_PAD = 0.06, 0.18


def db(x):
    return 10 ** (x / 20)


def labels():
    """(start seconds, text) for each label, from the newest label file in the source folder."""
    files = sorted(glob.glob(os.path.join(SOURCES, '*abels*.txt')), key=os.path.getmtime)
    if not files:
        raise SystemExit('No Audacity label file in ' + SOURCES)
    out = []
    with open(files[-1], encoding='utf-8-sig') as fh:
        for row in fh:
            parts = row.rstrip('\r\n').split('\t')
            if len(parts) >= 3 and parts[0].strip():
                out.append((float(parts[0]), parts[2].strip()))
    return sorted(out)


def key(text):
    return ' '.join(re.sub(r'[^a-z ]', ' ', text.lower()).split())


def envelope(x, rate, window=0.01):
    n = max(1, int(window * rate))
    return np.sqrt(np.convolve(x * x, np.ones(n) / n, mode='same'))


def trim(x, rate, threshold_db=-40.0):
    """Cuts to the speech, with a little room either side and short fades."""
    env = envelope(x, rate)
    loud = np.where(env > db(threshold_db))[0]
    if len(loud) == 0:
        return x
    a = max(0, loud[0] - int(HEAD_PAD * rate))
    b = min(len(x), loud[-1] + int(TAIL_PAD * rate))
    y = x[a:b].copy()
    fi, fo = int(0.01 * rate), int(0.08 * rate)
    y[:fi] *= np.linspace(0, 1, fi)
    y[-fo:] *= np.linspace(1, 0, fo)
    return y


def expand(x, rate, floor_db, ratio=2.5):
    """Softly turns down what is near the room's noise floor, so pauses sound clean."""
    env = envelope(x, rate, 0.02)
    threshold = db(floor_db + 12)
    gain = np.where(env < threshold, (np.maximum(env, 1e-9) / threshold) ** (ratio - 1), 1.0)
    gain = np.convolve(gain, np.ones(int(0.01 * rate)) / int(0.01 * rate), mode='same')
    return x * gain


def speaker_eq(x, rate):
    """A zero-phase EQ drawn as a gain curve over frequency: bass cut below about 170 Hz, a small
    presence lift near 2.5 kHz, and the top rolled off above about 7 kHz."""
    n = 1 << int(np.ceil(np.log2(len(x) + rate)))
    spectrum = np.fft.rfft(x, n)
    f = np.fft.rfftfreq(n, 1 / rate)
    high_pass = 1 / np.sqrt(1 + (170 / np.maximum(f, 1)) ** 4)
    low_pass = 1 / np.sqrt(1 + (f / 7000) ** 4)
    presence = 1 + (db(3) - 1) * np.exp(-0.5 * (np.log2(np.maximum(f, 1) / 2500) / 0.6) ** 2)
    return np.fft.irfft(spectrum * high_pass * low_pass * presence, n)[:len(x)]


def compress(x, rate, threshold_db=-24.0, ratio=3.0):
    env = envelope(x, rate, 0.015)
    over = np.maximum(20 * np.log10(np.maximum(env, 1e-9)) - threshold_db, 0)
    gain = db(-over * (1 - 1 / ratio))
    gain = np.convolve(gain, np.ones(int(0.005 * rate)) / int(0.005 * rate), mode='same')
    return x * gain


def room(x, rate):
    """A few quiet early reflections, as off the hard walls of a small control room."""
    y = np.concatenate([x, np.zeros(int(0.3 * rate))])
    for delay, level in ((0.019, -16), (0.031, -19), (0.047, -22), (0.071, -26), (0.103, -30)):
        d = int(delay * rate)
        tap = np.zeros_like(y)
        tap[d:d + len(x)] = x
        y += tap * db(level)
    return y


def level(x):
    loud = envelope(x, 44100)
    speech = x[loud > db(-40)] if np.any(loud > db(-40)) else x
    x = x * db(TARGET_RMS_DB) / max(np.sqrt(np.mean(speech ** 2)), 1e-9)
    peak = np.max(np.abs(x))
    if peak > db(PEAK_CEILING_DB):
        x = np.tanh(x / peak * 1.2) / np.tanh(1.2) * db(PEAK_CEILING_DB)
    return x


def main():
    signal, rate = sf.read(RECORDING)
    if signal.ndim > 1:
        signal = signal.mean(axis=1)
    marks = labels()
    windows = 0.2
    rms = [np.sqrt(np.mean(signal[i:i + int(windows * rate)] ** 2)) for i in range(0, len(signal) - int(windows * rate), int(windows * rate))]
    floor_db = 20 * np.log10(max(min(rms), 1e-9))
    print(f'room noise about {floor_db:.0f} dBFS')
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(PREVIEW, exist_ok=True)
    made = set()
    for i, (start, text) in enumerate(marks):
        end = marks[i + 1][0] if i + 1 < len(marks) else len(signal) / rate
        name = LINES.get(key(text))
        if name is None:
            print(f'skipped "{text}" (not in the script)')
            continue
        raw = trim(signal[int(start * rate):int(end * rate)], rate)
        # The room can change during a session (a fan starting), so each line uses its own
        # noise floor when that is louder than the recording's quietest stretch.
        env = envelope(raw, rate, 0.05)
        local_db = 20 * np.log10(max(np.percentile(env, 10), 1e-9))
        clean = expand(raw, rate, max(floor_db, local_db))
        done = level(room(compress(speaker_eq(clean, rate), rate), rate))
        done = trim(done, rate, -50)
        sf.write(os.path.join(OUT, name + '.ogg'), done, rate, format='OGG', subtype='VORBIS')
        sf.write(os.path.join(PREVIEW, name + '_dry.wav'), level(clean), rate)
        sf.write(os.path.join(PREVIEW, name + '.wav'), done, rate)
        made.add(name)
        print(f'{name:16} {len(done) / rate:4.1f} s  from "{text}"')
    missing = sorted(set(LINES.values()) - made)
    if missing:
        print('still to record:', ', '.join(missing))


if __name__ == '__main__':
    main()
