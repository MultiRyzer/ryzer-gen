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
- station/front_panel: the station's front panel, 32 x 16, drawn once across its two blocks (rows
  1 to 15 show on the 15-pixel face): a graphite plate with a lit bevel, a cyan light line, status
  lamps, a bezel round the screen (the screen itself is a separate glowing box), vents, and a
  hazard band along the foot.
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


def front_panel(hazard=True):
    """With hazard False, the plain foot the breeder's console uses (its compound wall carries the stripes)."""
    W, H = 32, 16
    k = [['T'] * W for _ in range(H)]
    for x in range(W):
        k[1][x] = 'b'                        # lit top bevel
        k[2][x] = 'S'
        k[3][x] = 'U'                        # groove holding the light line
        k[4][x] = 'S'
        k[15][x] = 'U'                       # shadowed foot
    for y in range(1, 16):
        k[y][0] = 'b' if y < 13 or not hazard else k[y][0]  # lit left edge
        k[y][W - 1] = 'U'                     # shadowed right edge
    for x in range(2, W - 2):
        k[3][x] = 'i'                        # cyan light line, inset from the ends
    # Bezel round the screen (the screen box covers x 11 to 20, rows 5 to 10 of the face).
    for y in range(5, 12):
        for x in range(9, 23):
            k[y][x] = 'u' if (y in (5, 11) or x in (9, 22)) else 'z'
    for x in range(9, 23):
        k[5][x] = 's'                        # lit top of the bezel
    # Status lamps on the left: green, amber, red, on a dark strip.
    for y in range(6, 11):
        for x in range(3, 7):
            k[y][x] = 'U'
    for (y, c) in ((7, 'n'), (8, 'a'), (9, 'x')):
        k[y][4] = c
        k[y][5] = c
    # Vents on the right: three slots.
    for y in (6, 8, 10):
        for x in range(25, 29):
            k[y][x] = 'U'
        for x in range(25, 29):
            k[y + 1][x] = 'b' if y < 10 else k[y + 1][x]
    # Hazard band along the foot: yellow on graphite, 45 degrees.
    for y in (12, 13, 14) if hazard else ():
        for x in range(1, W - 1):
            k[y][x] = 'Y' if (x + y) % 6 < 3 else 'B'
    return [[solid(c) for c in row] for row in k]


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


def swarm_cell():
    """A sky swarm panel's sun side, seen from far off: a thin bright frame round a 3 x 3 grid of
    deep blue photovoltaic cells split by fine silver bus lines, with a soft diagonal sheen. Finer
    and quieter than the sun gate's shades, which are a close-up simulation."""
    tex = []
    for y in range(16):
        row = []
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                k = 'H'
            elif x in (5, 10) or y in (5, 10):
                k = 'I'
            else:
                sheen = abs((x - y) - 2) < 2
                k = 'v' if sheen else ('w' if (x // 5 + y // 5) % 2 else 'g')
            row.append(solid(k))
        tex.append(row)
    return tex


def swarm_back():
    """A sky swarm panel's back, the side seen from the ground: a graphite radiator with fine fins,
    lit along its top and left edges and shadowed along the others. Closed into a shell, the edges
    draw the fine grid of seams across it."""
    tex = []
    for y in range(16):
        row = []
        for x in range(16):
            if y == 0 or x == 0:
                k = 'J'
            elif y == 15 or x == 15:
                k = 'U'
            else:
                k = 'b' if y % 3 == 1 else 'S'
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


# ---------------------------------------------------------------- the fission station's own set
# Every part of the station has its own texture, so each reads as what it is: a dark foundation,
# a light body, a gunmetal vessel head, and colour only where it means something.

def letters(rows):
    return [[solid(c) for c in row] for row in rows]


def fill(k, w=16, h=16):
    return [[k] * w for _ in range(h)]


def skirt():
    # The foundation band (6 pixels, rows 10 to 15): the darkest part of the station, with a vent
    # slot per step.
    k = fill('T')
    k[10] = ['b'] * 16
    k[15] = ['K'] * 16
    for x in range(5, 11):
        k[12][x] = 'K'
        k[13][x] = 'U'
    return letters(k)


def deck():
    # The chamber floor round the core: dark floor plates with rows of grating slots.
    k = fill('T')
    for y in range(16):
        for x in range(16):
            if y % 4 == 1 and 1 <= x % 8 <= 5:
                k[y][x] = 'U'
    k[15] = ['S'] * 16
    for y in range(16):
        k[y][15] = 'S'
    return letters(k)


def core_plate():
    # The pedestal the rods stand in: a light machined plate, seamed once per step.
    k = fill('E')
    k[0] = ['A'] * 16
    k[15] = ['I'] * 16
    for y in range(16):
        k[y][15] = 'J'
    return letters(k)


def pillar():
    # A chamber rib's outer flange (8 across, repeated): lit edge, clean face, a shallow channel.
    cols = ['A', 'E', 'E', 'I', 'I', 'E', 'F', 'J']
    k = [[cols[x % 8] for x in range(16)] for _ in range(16)]
    k[0] = ['J'] * 16                        # a joint per block of height
    return letters(k)


def rib_web():
    # The web behind the flange, and the inner flange seen through the glass: graphite.
    cols = ['b', 'S', 'S', 'S', 'S', 'S', 'T', 'U']
    return letters([[cols[x % 8] for x in range(16)] for _ in range(16)])


def clamp():
    # Collars and shoes that hold the ribs: gunmetal, lit on top, an orange lug.
    k = fill('u')
    k[0] = ['H'] * 16
    k[1] = ['s'] * 16
    for y in range(16):
        k[y][0] = 's'
        k[y][15] = 'z'
    for y in (2, 3):
        for x in (4, 5):
            k[y][x] = 'X'
        k[y][6] = 'Z'
    return letters(k)


def copper_pipe():
    # A coolant downcomer (3 across): lit, face, shadow, along its length.
    cols = ['e', 'R', 'r']
    return letters([[cols[min(x, 2)] for x in range(16)] for _ in range(16)])


def head():
    # The reactor head, a pressure vessel flange (rows top down, y 96 to 80): a lit lip, gunmetal
    # plates, the groove for the cyan light line in the middle, a shadowed foot. A seam per step.
    rows = ['H', 's', 'u', 'u', 'u', 'u', 'z', 'T', 'T', 'z', 's', 'u', 'u', 'u', 'z', 'K']
    k = [[rows[y]] * 16 for y in range(16)]
    for y in range(1, 15):
        if rows[y] != 'T':
            k[y][15] = 'z'
            if rows[y] == 'u':
                k[y][0] = 's'
    return letters(k)


def head_top():
    k = fill('z')
    k[0] = ['u'] * 16
    for y in range(16):
        k[y][15] = 'T'
    return letters(k)


def rim():
    k = fill('b')
    k[0] = ['s'] * 16
    k[15] = ['T'] * 16
    return letters(k)


def column():
    # The stack's columns (an I-beam flange, 7 across): graphite with a lit edge.
    cols = ['s', 'b', 'b', 'S', 'b', 'b', 'T']
    k = [[cols[x % 7] for x in range(16)] for _ in range(16)]
    k[0] = ['T'] * 16
    return letters(k)


def vane():
    # A louvre vane (10 across): a light plate with a lit leading edge.
    cols = ['A', 'E', 'E', 'E', 'F', 'F', 'F', 'I', 'I', 'J']
    return letters([[cols[min(x, 9)] for x in range(16)] for _ in range(16)])


def stack_panel(half):
    # The stack's cladding: big light panels two steps wide (half 0 and 1), staggered band to band
    # like real cladding. Lit along the top, a shadow at the foot, one seam per panel.
    k = fill('E')
    k[0] = ['A'] * 16
    k[1] = ['A'] * 16
    k[15] = ['I'] * 16
    for y in range(2, 15):
        if half == 0:
            k[y][0] = 'A'
        else:
            k[y][15] = 'J'
    return letters(k)


def stack_base():
    # The stack's dark foot band.
    k = fill('z')
    k[6] = ['s'] * 16
    k[15] = ['T'] * 16
    for y in range(6, 15):
        k[y][15] = 'T'
    return letters(k)


def stack_inner():
    k = fill('T')
    for y in range(16):
        k[y][0] = 'S'
        k[y][1] = 'S'
        k[y][15] = 'U'
    return letters(k)


def warning():
    # Aviation marking round the top of the stack (12 pixels, rows 4 to 15): red, white, red.
    rows = ['x'] * 8 + ['A'] * 4 + ['x'] * 4
    k = [[rows[y]] * 16 for y in range(16)]
    k[4] = ['e'] * 16
    k[8] = ['H'] * 16
    k[12] = ['e'] * 16
    k[15] = ['8'] * 16
    return letters(k)


def beacon():
    k = fill('x')
    for y in (1, 2):
        for x in (1, 2):
            k[y][x] = 'e'
    return letters(k)


GLYPHS = {
    'R': ['110', '101', '110', '101', '101'],
    'G': ['011', '100', '101', '101', '011'],
    '-': ['000', '000', '111', '000', '000'],
    '1': ['010', '110', '010', '010', '111'],
}


def wide_panel():
    a, b = stack_panel(0), stack_panel(1)
    return [[(a if x < 16 else b)[y][x % 16] for x in range(32)] for y in range(16)]


def stack_stencil():
    # The station's number stencilled across two cladding panels (32 x 16), read from the front.
    k = wide_panel()
    text = 'RG-1'
    x0 = (32 - (len(text) * 4 - 1)) // 2
    for i, ch in enumerate(text):
        for gy, line in enumerate(GLYPHS[ch]):
            for gx, bit in enumerate(line):
                if bit == '1':
                    k[5 + gy][x0 + i * 4 + gx] = solid('T')
    return k


def stack_hatch():
    # An access hatch across two cladding panels (32 x 16): a gunmetal door with an orange handle.
    k = wide_panel()
    for y in range(3, 15):
        for x in range(11, 21):
            edge = y == 3 or x == 11
            shade = y == 14 or x == 20
            k[y][x] = solid('s' if edge else 'z' if shade else 'u')
    for y in (8, 9):
        k[y][18] = solid('X')
    return k


def halves(tex):
    """Splits a 32-wide texture into the two 16-wide panels it is drawn on, mirrored so it reads
    the right way round from outside (u runs to the viewer's left round the station)."""
    flipped = [row[::-1] for row in tex]
    return [row[:16] for row in flipped], [row[16:] for row in flipped]


def fuel_pin(lit):
    # A fuel pin (3 across): zirconium-clad pellets glowing green, a darker joint every 6 pixels
    # where one pellet meets the next.
    cols = ['7', 'N', 'n'] if lit else ['5', '6', '5']
    k = [[cols[x % 3] for x in range(16)] for _ in range(16)]
    for y in (5, 11):
        k[y] = [('6' if lit else '5')] * 16
    return letters(k)


def spacer():
    # The spacer grids and end fittings that hold a fuel bundle together: bright steel.
    k = fill('H')
    k[0] = ['A'] * 16
    k[15] = ['I'] * 16
    return letters(k)


def nozzle():
    k = fill('u')
    k[0] = ['s'] * 16
    k[15] = ['z'] * 16
    for y in range(16):
        k[y][15] = 'z'
    return letters(k)


def cherenkov():
    # A coolant channel's water column, glowing Cherenkov blue (the real glow of a reactor's water).
    cols = ['G', 'c', 'C', 'c', 'G', 'c', 'C', 'c']
    return letters([[cols[x % 8] for x in range(16)] for _ in range(16)])


def moderator():
    # A graphite moderator column with its bore holes.
    k = fill('S')
    for y in range(16):
        k[y][0] = 'b'
        k[y][15] = 'T'
    for y in (3, 11):
        for x in (3, 4, 11, 12):
            k[y][x] = 'U'
    return letters(k)


def control():
    # A control rod: polished steel with depth marks every 4 pixels, so its travel reads.
    k = fill('E')
    for y in range(16):
        k[y][0] = 'A'
        k[y][15] = 'J'
    for y in (0, 4, 8, 12):
        for x in range(2, 7):
            k[y][x] = 'T'
    return letters(k)


def housing():
    # The port housing's face: a light plate, lit along the top, with a dark vent strip at the foot.
    k = fill('F')
    k[0] = ['A'] * 16
    k[1] = ['E'] * 16
    for x in range(16):
        k[14][x] = 'T' if x % 3 else 'b'
        k[15][x] = 'U'
    return letters(k)


def housing_slope():
    # The sloped hood over the ports: graphite, a lit front lip, a cyan light line near the top.
    k = fill('S')
    k[0] = ['b'] * 16
    k[3] = ['i'] * 16
    k[4] = ['T'] * 16
    k[15] = ['E'] * 16
    return letters(k)


def housing_cheek():
    # The cheeks either end of the housing: graphite with a hazard stripe down the front.
    k = fill('S')
    for y in range(16):
        k[y][0] = 'b'
        k[y][15] = 'U'
    return letters(k)


def hazard_upright():
    return letters([['Y' if (x + y) % 6 < 3 else 'B' for x in range(16)] for y in range(16)])


def tag(colour):
    # A slim light bar above a port, in the port's ring colour.
    return letters(fill(colour))


STEAM_FRAMES = 16


def steam():
    """Steam rising through the chamber, animated: soft, faint white wisps over a thin haze,
    drifting up the texture (v) and swaying a little. Wide and low so the rods read through it,
    and wrapping at the sides so the bands round the chamber show no seams. Drawn translucent."""
    out = []
    for f in range(STEAM_FRAMES):
        phase = 2 * math.pi * f / STEAM_FRAMES
        for y in range(16):
            v = 2 * math.pi * y / 16
            row = []
            for x in range(16):
                a = 0.08
                for base, freq, width in ((3.5, 1, 4.5), (9.0, 2, 3.5), (13.0, 1, 3.0)):
                    centre = base + 1.5 * math.sin(v * freq + phase + base)
                    density = 0.5 + 0.5 * math.sin(v * 2 + phase * 2 + base)
                    dx = abs(x - centre) % 16
                    dx = min(dx, 16 - dx)
                    a += 0.45 * density * math.exp(-(dx ** 2) / width)
                row.append((245, 250, 252, round(70 * min(1.0, a))))
            out.append(row)
    return out


def publish_station():
    for name, tex in (('skirt', skirt()), ('deck', deck()), ('core_plate', core_plate()), ('pillar', pillar()),
                      ('rib_web', rib_web()), ('clamp', clamp()), ('copper_pipe', copper_pipe()), ('head', head()),
                      ('head_top', head_top()), ('rim', rim()), ('column', column()), ('vane', vane()),
                      ('stack_a', stack_panel(0)), ('stack_b', stack_panel(1)), ('stack_base', stack_base()),
                      ('stack_inner', stack_inner()), ('warning', warning()), ('beacon', beacon())):
        write_texture('block/station/' + name, tex)
    for name, tex in (('fuel_pin', fuel_pin(True)), ('fuel_pin_off', fuel_pin(False)), ('spacer', spacer()),
                      ('nozzle', nozzle()), ('cherenkov', cherenkov()), ('moderator', moderator()),
                      ('control', control()), ('steam', steam()), ('housing', housing()),
                      ('housing_slope', housing_slope()), ('housing_cheek', housing_cheek()),
                      ('hazard_upright', hazard_upright()), ('tag_coolant', tag('G')), ('tag_fuel', tag('X')),
                      ('tag_energy', tag('x'))):
        write_texture('block/station/' + name, tex)
    meta = os.path.join(mp.TEXTURES, 'block', 'station', 'steam.png.mcmeta')
    with open(meta, 'w') as fh:
        json.dump({'animation': {'frametime': 3, 'interpolate': True}}, fh, indent=2)
        fh.write('\n')
    for name, tex in (('stencil', stack_stencil()), ('hatch', stack_hatch())):
        right, left = halves(tex)
        write_texture('block/station/' + name + '_a', right)
        write_texture('block/station/' + name + '_b', left)


def publish_all():
    write_texture('block/machine/smooth_glass', smooth_glass())
    write_texture('block/machine/console', console())
    write_texture('block/machine/console_top', console_top())
    write_texture('block/station/rib', rib())
    write_texture('block/station/plinth', plinth())
    write_texture('block/station/front_panel', front_panel())
    write_texture('block/breeder/front_panel', front_panel(hazard=False))
    publish_station()
    write_texture('block/fusion/plasma_wisp', plasma_wisp())
    write_texture('block/fusion/arc', arc())
    write_texture('block/sun/photosphere', photosphere())
    write_texture('block/sun/shade', shade())
    write_texture('block/sun/swarm_cell', swarm_cell())
    write_texture('block/sun/swarm_back', swarm_back())
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
