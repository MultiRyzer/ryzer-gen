"""Builds the reactor announcer's voice lines from Ryzer's recording (art/sounds/VOICE-SCRIPT.md).

Reads one recording of every take and the label file Audacity exported with it, both in
art/sounds/source/ (ignored by git). Each label marks where a line starts; the line runs to the next
label. For every line it:
  1. trims the silence round it, keeping a short breath of room at each end,
  2. gently lowers the room noise between words (a soft expander, not a gate that chops),
  3. gives it an emergency public-address character: a horn speaker's narrow range (little bass,
     the top rolled off), a touch of grit, light compression, then the echo of a large hard room
     (a reverb tail and a faint slapback off a far wall),
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
    """A zero-phase EQ drawn as a gain curve over frequency, shaped like a horn loudspeaker: bass
    cut below about 300 Hz, a presence lift near 2 kHz, and the top rolled off above about 5 kHz."""
    n = 1 << int(np.ceil(np.log2(len(x) + rate)))
    spectrum = np.fft.rfft(x, n)
    f = np.fft.rfftfreq(n, 1 / rate)
    high_pass = 1 / np.sqrt(1 + (300 / np.maximum(f, 1)) ** 4)
    low_pass = 1 / np.sqrt(1 + (f / 5000) ** 6)
    presence = 1 + (db(5) - 1) * np.exp(-0.5 * (np.log2(np.maximum(f, 1) / 2000) / 0.7) ** 2)
    return np.fft.irfft(spectrum * high_pass * low_pass * presence, n)[:len(x)]


def compress(x, rate, threshold_db=-24.0, ratio=3.0):
    env = envelope(x, rate, 0.015)
    over = np.maximum(20 * np.log10(np.maximum(env, 1e-9)) - threshold_db, 0)
    gain = db(-over * (1 - 1 / ratio))
    gain = np.convolve(gain, np.ones(int(0.005 * rate)) / int(0.005 * rate), mode='same')
    return x * gain


def grit(x, drive=2.0):
    """The slight break-up of a loudspeaker pushed hard."""
    peak = max(np.max(np.abs(x)), 1e-9)
    return np.tanh(x / peak * drive) / np.tanh(drive) * peak


REVERB_SECONDS = 1.1       # the time the tail takes to fade by 60 dB
REVERB_LEVEL_DB = -9       # the tail against the direct voice
SLAPBACK = (0.16, -17)     # a single echo off a far wall: delay in s, level in dB


def room(x, rate):
    """The echo of a large hard room: early reflections, a smooth decaying tail and a faint
    slapback, made as an impulse response (fixed noise, so every line gets the same room) and
    applied by convolution."""
    length = int((REVERB_SECONDS + 0.2) * rate)
    t = np.arange(length) / rate
    noise = np.random.default_rng(7).standard_normal(length)
    tail = noise * np.exp(-6.91 * t / REVERB_SECONDS) * np.minimum(t / 0.02, 1)
    # Darker as it decays, as air and soft surfaces take the top out of a real echo.
    n = 1 << int(np.ceil(np.log2(length)))
    f = np.fft.rfftfreq(n, 1 / rate)
    tail = np.fft.irfft(np.fft.rfft(tail, n) / np.sqrt(1 + (f / 3500) ** 2), n)[:length]
    impulse = tail / np.sqrt(np.sum(tail ** 2)) * db(REVERB_LEVEL_DB)
    for delay, level in ((0.023, -12), (0.041, -14), (0.067, -17)):
        impulse[int(delay * rate)] += db(level)
    impulse[int(SLAPBACK[0] * rate)] += db(SLAPBACK[1])
    impulse[0] = 1.0
    size = len(x) + length
    n = 1 << int(np.ceil(np.log2(size)))
    return np.fft.irfft(np.fft.rfft(x, n) * np.fft.rfft(impulse, n), n)[:size]


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
        done = level(room(compress(grit(speaker_eq(clean, rate)), rate), rate))
        done = trim(done, rate, -55)
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
