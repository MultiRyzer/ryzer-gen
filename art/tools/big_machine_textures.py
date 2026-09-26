"""Textures made for the big renderer-drawn machines (the fission station, the fusion reactor).

The ordinary casing panels are one bevelled plate per block, which reads well on a block face but
not on a long console, a tall rib or a thin band: tiled, they show a frame every 16 pixels and cut
plates in half. These are drawn for the shapes they go on instead. Called by the concept scripts
(fission_concept.py, tokamak_concept.py), or run on its own:
    python art/tools/big_machine_textures.py

- machine/console, console_top: a console's front, one clean plate however wide. Every column is
  the same, so it tiles along without a seam; lit along the top edge, shadowed at the foot.
- station/rib: an upright rib (8 pixels across, along the texture's height): a lit edge, a recessed
  channel, a joint line every block.
- station/plinth: the station's 10-pixel plinth band: a lit lip, a groove, a panel joint per step.
- fusion/plasma_wisp: the plasma's glow shells, animated: soft filaments flowing round the ring,
  white on black (drawn additively and tinted in code, see FusionPlasma).
- fusion/arc: a soft white streak for the lightning arcs (tinted in code), clear at the edges.
- sun/photosphere: the sun gate's surface, animated: bright granules with orange lanes between
  them, slowly boiling (SunGateSun darkens the limb in code).
- sun/shade, shade_back: a solar shade, the side facing the sun (dark blue cells in a light frame)
  and the back (gold foil). sun/shade_ghost: an empty slot's outline, white on black (additive).
- sun/white: plain white, for glows shaded by vertex colour (the corona).
"""
import json
import math
import os

import model_preview as mp
from pixelart import PAL, rgb
from quad_design import smooth_glass, write_texture


def solid(k, a=255):
    return (*rgb(PAL[k]), a)


def console():
    # Rows as they land on a 15-pixel-tall console (its face covers v 1 to 16).
    rows = {1: 'b'}
    tex = []
    for y in range(16):
        k = rows.get(y, 'U' if y == 15 else 'S')
        tex.append([solid(k)] * 16)
    return tex


def console_top():
    return [[solid('S')] * 16 for _ in range(16)]


def rib():
    # Across (columns 0 to 7, repeated): lit edge, face, a recessed channel, face, shadowed edge.
    cols = ['E', 'F', 'F', 'I', 'b', 'J', 'I', 'J']
    tex = []
    for y in range(16):
        row = []
        for x in range(16):
            k = cols[x % 8]
            if y == 0:
                k = 'b'                      # the joint between one block's length and the next
            elif y == 1 and cols[x % 8] not in ('b',):
                k = 'A'                      # lit just below the joint
            row.append(solid(k))
        tex.append(row)
    return tex


def plinth():
    # Rows 0 to 9 are the band (the cylinder maps them top down); columns seam once per step.
    band = ['h', 'E', 'F', 'F', 'F', 'J', 'b', 'F', 'F', 'I']
    tex = []
    for y in range(16):
        row = []
        for x in range(16):
            k = band[y] if y < len(band) else 'I'
            if x == 15 and 1 < y < len(band):
                k = 'J'                      # panel joint
            elif x == 0 and 1 < y < len(band) and k == 'F':
                k = 'E'
            row.append(solid(k))
        tex.append(row)
    return tex


FRAMES = 16


def plasma_wisp():
    """Wispy filaments for the plasma's glow shells, animated. Drawn additively (light added, so
    black is clear) and tinted in code, so this is white on black: two soft filaments running along
    the ring (the texture's v), each wandering across and breathing, over a faint haze. 16 frames,
    looped with interpolation; the shells also turn, so the filaments flow."""
    out = []
    for f in range(FRAMES):
        phase = 2 * math.pi * f / FRAMES
        for y in range(16):
            v = 2 * math.pi * y / 16
            row = []
            for x in range(16):
                glow = 0.0
                for base, amp, freq, sign, width in ((4.5, 2.5, 1, 1, 1.4), (11.5, 2.0, 2, -1, 0.9)):
                    centre = base + amp * math.sin(v * freq + sign * phase)
                    strength = 0.75 + 0.25 * math.sin(phase * 2 + base)
                    glow = max(glow, strength * math.exp(-((x - centre) ** 2) / width))
                level = min(1.0, 0.03 + glow)
                c = round(255 * level)
                row.append((c, c, c, 255))
            out.append(row)
    return out


def arc():
    # Across the streak (v): opaque white in the middle, fading to clear at both edges.
    tex = []
    for y in range(16):
        a = max(0.0, 1 - abs(y - 7.5) / 7.5) ** 1.6
        tex.append([(255, 255, 255, round(255 * a))] * 16)
    return tex


def photosphere():
    """Solar granulation: convection cells, hot rising middles (pale yellow) and cooler sinking
    lanes (orange) between them. Tileable (distances wrap round the tile), 16 frames looped with
    interpolation: each cell's centre drifts in a small circle and its brightness breathes, so the
    surface boils. Real granules are about 1,000 km across and live about ten minutes."""
    cells = [(2.5, 3.0), (9.0, 1.5), (13.5, 6.5), (5.5, 8.5), (1.0, 13.0), (10.5, 11.5), (6.5, 14.5), (14.5, 14.0)]
    lane, mid, hot = (214, 92, 20), (250, 170, 50), (255, 244, 196)
    out = []
    for f in range(FRAMES):
        phase = 2 * math.pi * f / FRAMES
        pts = [(cx + 0.9 * math.cos(phase + k * 1.7), cy + 0.9 * math.sin(phase + k * 2.3)) for k, (cx, cy) in enumerate(cells)]
        glow = [0.8 + 0.2 * math.sin(phase * (1 + k % 2) + k) for k in range(len(cells))]
        for y in range(16):
            row = []
            for x in range(16):
                d = []
                for k, (px, py) in enumerate(pts):
                    dx = min(abs(x + 0.5 - px), 16 - abs(x + 0.5 - px))
                    dy = min(abs(y + 0.5 - py), 16 - abs(y + 0.5 - py))
                    d.append((math.hypot(dx, dy), k))
                d.sort()
                edge = min(1.0, (d[1][0] - d[0][0]) / 2.6)
                t = edge * glow[d[0][1]]
                a, b, u = (lane, mid, t / 0.5) if t < 0.5 else (mid, hot, (t - 0.5) / 0.5)
                row.append(tuple(round(a[i] + (b[i] - a[i]) * u) for i in range(3)) + (255,))
            out.append(row)
    return out


def shade():
    """The side facing the sun: a light frame round dark blue cells, with a thin cyan catch light
    on the top edge. 16 pixels drawn onto a panel about 7 across, so the cell grid is coarse."""
    tex = []
    for y in range(16):
        row = []
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                k = 'E'
            elif x in (1, 14) or y in (1, 14):
                k = 'J'
            elif x in (5, 10) or y in (5, 10):
                k = 'U'
            else:
                k = 'w' if (x + y) % 5 else 'v'
            row.append(solid(k))
        tex.append(row)
    return tex


def shade_back():
    """The back: gold foil (multi-layer insulation, as on spacecraft) in a graphite frame, with
    soft creases so it catches the light."""
    foil = [(150, 108, 30), (196, 150, 52), (226, 186, 84), (246, 214, 128)]
    tex = []
    for y in range(16):
        row = []
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                row.append(solid('S'))
                continue
            crease = math.sin((x * 0.9 + y * 0.5)) + 0.6 * math.sin(y * 1.3 - x * 0.4)
            row.append((*foil[min(3, max(0, int((crease + 1.6) / 3.2 * 4)))], 255))
        tex.append(row)
    return tex


def shade_ghost():
    # An empty slot: a thin frame with corner marks, white on black (added, tinted cyan in code).
    tex = []
    for y in range(16):
        row = []
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            corner = (x < 4 or x > 11) and (y < 4 or y > 11) and (x in (0, 1, 14, 15) or y in (0, 1, 14, 15))
            c = 255 if corner else 110 if edge else 0
            row.append((c, c, c, 255))
        tex.append(row)
    return tex


def publish_all():
    write_texture('block/machine/smooth_glass', smooth_glass())
    write_texture('block/machine/console', console())
    write_texture('block/machine/console_top', console_top())
    write_texture('block/station/rib', rib())
    write_texture('block/station/plinth', plinth())
    write_texture('block/fusion/plasma_wisp', plasma_wisp())
    write_texture('block/fusion/arc', arc())
    write_texture('block/sun/photosphere', photosphere())
    write_texture('block/sun/shade', shade())
    write_texture('block/sun/shade_back', shade_back())
    write_texture('block/sun/shade_ghost', shade_ghost())
    write_texture('block/sun/white', [[(255, 255, 255, 255)] * 16 for _ in range(16)])
    for name, frametime in (('fusion/plasma_wisp', 2), ('sun/photosphere', 4)):
        meta = os.path.join(mp.TEXTURES, 'block', *name.split('/')) + '.png.mcmeta'
        with open(meta, 'w') as fh:
            json.dump({'animation': {'frametime': frametime, 'interpolate': True}}, fh, indent=2)
            fh.write('\n')


if __name__ == '__main__':
    publish_all()
    print('textures written')
