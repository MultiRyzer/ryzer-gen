"""The control screen's sounds, synthesised, so there is nothing to license.

Ported from the Web Audio recipes in the core puzzle's reference page
(art/concepts/station_screen/core-puzzle.html): oscillators with exponential attack and decay
envelopes and pitch sweeps, band-passed noise, and a low-passed pad. Each is rendered to mono Ogg
Vorbis in assets/ryzergen/sounds as console_<name>.ogg, for the sound events in ModSounds (the
screen plays them in client/terminal/ConsoleSounds). Claude cannot hear these: the user judges them
by ear in game.

Needs numpy and soundfile (pip install soundfile numpy). Run from the repo root:
    python art/tools/console_sounds.py
"""
import math
import os

import numpy as np
import soundfile as sf

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'sounds')
RATE = 44100
MASTER = 0.55
SCALE = [0, 2, 4, 7, 9]
RNG = np.random.default_rng(1)


def pitch(k):
    """Step k of the pentatonic scale from C4, five steps an octave, as the page's notes climb it."""
    return 261.63 * 2 ** ((k // 5 * 12 + SCALE[k % 5]) / 12)


class Mix:
    """A buffer the parts are added into, long enough for the last of them."""

    def __init__(self, seconds):
        self.buf = np.zeros(int(seconds * RATE) + 1)

    def add(self, signal, when):
        start = int(when * RATE)
        end = min(len(self.buf), start + len(signal))
        self.buf[start:end] += signal[:end - start]


def envelope(n, attack, decay, peak):
    """The page's env(): exponential from silence to the peak over the attack, then back down."""
    t = np.arange(n) / RATE
    floor = 1e-4
    rise = floor * (peak / floor) ** np.clip(t / attack, 0, 1)
    fall = peak * (floor / peak) ** np.clip((t - attack) / decay, 0, 1)
    return np.where(t < attack, rise, fall)


def oscillator(kind, freq, n, sweep=None, length=None):
    t = np.arange(n) / RATE
    if sweep:
        # Exponential glide from freq to sweep over the note, as exponentialRampToValueAtTime.
        f = freq * (sweep / freq) ** np.clip(t / length, 0, 1)
    else:
        f = np.full(n, float(freq))
    phase = np.cumsum(f) / RATE % 1.0
    if kind == 'sine':
        return np.sin(2 * math.pi * phase)
    if kind == 'square':
        return np.where(phase < 0.5, 1.0, -1.0)
    if kind == 'triangle':
        return 1 - 4 * np.abs(phase - 0.5)
    return 2 * phase - 1  # sawtooth


def tone(mix, freq, kind, attack, decay, peak, when=0.0, sweep=None):
    n = int((attack + decay + 0.05) * RATE)
    mix.add(oscillator(kind, freq, n, sweep, attack + decay) * envelope(n, attack, decay, peak) * MASTER, when)


def biquad(signal, kind, freq, q):
    """An RBJ cookbook biquad, as the Web Audio BiquadFilterNode."""
    w = 2 * math.pi * freq / RATE
    alpha = math.sin(w) / (2 * q)
    cos = math.cos(w)
    if kind == 'bandpass':
        b = [alpha, 0, -alpha]
    else:  # lowpass
        b = [(1 - cos) / 2, 1 - cos, (1 - cos) / 2]
    a = [1 + alpha, -2 * cos, 1 - alpha]
    b = [v / a[0] for v in b]
    a = [v / a[0] for v in a]
    out = np.zeros_like(signal)
    x1 = x2 = y1 = y2 = 0.0
    for i, x in enumerate(signal):
        y = b[0] * x + b[1] * x1 + b[2] * x2 - a[1] * y1 - a[2] * y2
        x2, x1, y2, y1 = x1, x, y1, y
        out[i] = y
    return out


def noise(mix, duration, freq, peak, when=0.0):
    n = int((duration + 0.05) * RATE)
    white = RNG.uniform(-1, 1, n)
    mix.add(biquad(white, 'bandpass', freq, 1.2) * envelope(n, 0.003, duration, peak) * MASTER, when)


def note(mix, freq, when=0.0, peak=0.22):
    """The page's note(): a sine with a soft octave over it."""
    tone(mix, freq, 'sine', 0.004, 0.55, peak, when)
    tone(mix, freq * 2, 'sine', 0.004, 0.25, peak * 0.3, when)


def pad(mix, when, freqs, cutoff=900, rise=0.4, length=2.2, peak=0.05):
    """A soft sawtooth chord through a low-pass, swelling in and out."""
    n = int((length + 0.1) * RATE)
    t = np.arange(n) / RATE
    gain = np.where(t < rise, 1e-4 * (peak / 1e-4) ** np.clip(t / rise, 0, 1),
                    peak * (1e-4 / peak) ** np.clip((t - rise) / (length - rise), 0, 1))
    chord = sum(oscillator('sawtooth', f, n) for f in freqs)
    mix.add(biquad(chord, 'lowpass', cutoff, 0.707) * gain * MASTER, when)


def clunk(mix, when, low=False):
    """A part seating in its channel: a dull thud and a short metal tick."""
    tone(mix, 110 if low else 130, 'sine', 0.003, 0.12, 0.45, when, 55)
    noise(mix, 0.03, 1400, 0.18, when)


def whirr(mix, when, start, end, length):
    """The fuel handling machine's servo: a filtered sawtooth gliding between two pitches, with a hiss."""
    n = int(length * RATE)
    t = np.arange(n) / RATE
    body = biquad(oscillator('sawtooth', start, n, end, length), 'lowpass', 700, 0.9)
    gain = 0.12 * np.sin(math.pi * np.clip(t / length, 0, 1)) ** 0.6
    mix.add(body * gain * MASTER, when)
    noise(mix, length * 0.8, 2600, 0.04, when)


def sounds():
    out = {}
    # A tick as the mouse crosses a channel.
    m = Mix(0.08)
    noise(m, 0.012, 3800, 0.04)
    out['tick'] = (m, -14)
    # A part placed: fuel a soft drop, graphite a knock, water a rising blip.
    m = Mix(0.3)
    tone(m, 150, 'sine', 0.004, 0.16, 0.5, 0, 70)
    noise(m, 0.02, 2400, 0.08)
    out['place_fuel'] = (m, -6)
    m = Mix(0.2)
    tone(m, 320, 'triangle', 0.002, 0.08, 0.35)
    noise(m, 0.03, 900, 0.12)
    out['place_graphite'] = (m, -7)
    m = Mix(0.25)
    tone(m, 420, 'sine', 0.01, 0.14, 0.3, 0, 980)
    out['place_water'] = (m, -8)
    m = Mix(0.2)
    noise(m, 0.09, 600, 0.12)
    out['clear'] = (m, -9)
    # The climbing note, one sample an octave from the next: C5 and C6 (the game pitches each an octave either way).
    for name, freq in (('note', pitch(5)), ('note_high', pitch(10))):
        m = Mix(0.7)
        note(m, freq)
        out[name] = (m, -6)
    # Heat lost: a falling blip. A rod losing its water: a buzz.
    m = Mix(0.25)
    tone(m, 330, 'square', 0.003, 0.07, 0.06)
    tone(m, 311, 'square', 0.003, 0.09, 0.06, 0.08)
    out['down'] = (m, -12)
    m = Mix(0.3)
    tone(m, 110, 'square', 0.003, 0.08, 0.07)
    tone(m, 110, 'square', 0.003, 0.08, 0.07, 0.12)
    out['buzz'] = (m, -10)
    # Three stars: the core locking (a low thunk), an arpeggio up the scale, and a soft pad under it.
    m = Mix(3.5)
    tone(m, 55, 'sine', 0.005, 0.5, 0.7, 0, 38)
    noise(m, 0.18, 160, 0.4)
    for i, k in enumerate((0, 2, 4, 5, 7, 10)):
        note(m, pitch(10 + k), 1.15 + i * 0.07, 0.2)
    pad(m, 1.1, (261.63, 329.63, 392.0, 523.25))
    out['fanfare'] = (m, -3)
    # Inject: the servo runs up, three parts seat, and two notes climb. Eject runs it the other way.
    m = Mix(1.6)
    whirr(m, 0, 90, 180, 0.5)
    for i in range(3):
        clunk(m, 0.32 + i * 0.18)
    note(m, pitch(10), 0.95, 0.18)
    note(m, pitch(12), 1.07, 0.18)
    out['inject'] = (m, -5)
    m = Mix(1.6)
    whirr(m, 0, 180, 90, 0.5)
    for i in range(3):
        clunk(m, 0.32 + i * 0.18, low=True)
    note(m, pitch(7), 0.95, 0.18)
    note(m, pitch(5), 1.07, 0.18)
    out['eject'] = (m, -5)
    return out


def finish(signal, peak_db):
    """Trims the silent tail, fades its last 10 ms and sets its peak."""
    loud = np.nonzero(np.abs(signal) > 1e-4)[0]
    signal = signal[:loud[-1] + 1] if len(loud) else signal
    fade = min(len(signal), int(0.01 * RATE))
    signal[-fade:] *= np.linspace(1, 0, fade)
    return signal * (10 ** (peak_db / 20) / max(1e-9, np.abs(signal).max()))


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, (mix, peak) in sounds().items():
        out = finish(mix.buf, peak)
        path = os.path.join(OUT, 'console_' + name + '.ogg')
        sf.write(path, out.astype(np.float32), RATE, format='OGG', subtype='VORBIS')
        print('%-16s %.2f s  peak %d dBFS' % (name, len(out) / RATE, peak))


if __name__ == '__main__':
    main()
