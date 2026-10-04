"""The Melt Drill's own textures (block/drill/), for its concepts: the mega drill
(mega_drill_concept.py) and the sinking towers (melt_drill_concept.py). Drawn to the house style
and even, as the station's and the breeder's are: every texture is drawn for the part it
goes on, at one texel per pixel, so each surface shows it whole, never a plate cut at an edge or
squeezed out of shape.
- Plates a block each, cut from each box's own corner: panel (the housing, the pods and the
  console), and two panels drawn with what is on them (the station's way, not a decal stuck over
  a panel): panel_hatch, the inspection hatch round its window, and panel_stencil, the drill's
  number D-1. lid is the same plate in graphite for roofs; shaft is the drive shaft's ribbed
  casing, a flange at the foot of each block; rotary is the rotary table the drill string passes
  down through; fan is a cooling fan under its guard; sheave and sheave_end the crown's sheave
  block.
- Narrow parts drawn at their width: post (5 wide: a steel leg with a bolted splice plate over
  each block's joint), brace (4 wide angle steel, riveted), leg (the buttress legs, 14 wide, a
  groove down the middle and a bolted plate joint every block), ram and rod (3 and 4 wide),
  rail and rail_post, stack (8 wide, sooted under its lip) and stack_top, pulley, duct (the
  copper coolant duct, a joint each block).
- Bands drawn at their height (rows 16 - h to 15) with patterns that repeat every 4 or 8 pixels,
  so they run on unbroken round any length: band (4 high), band6, band8 (with the slot the light
  line sits in), base (6: a lit lip over hazard stripes), pit_wall (5: firebrick lining the pit,
  glowing where the melt laps it), collar (12: vents, the light line's slot, a bolt line), desk
  (8: cabinet doors). Every band texture is filled to the top, so one used on a taller face shows
  plain graphite rather than nothing.
- Patterns that repeat every 8 pixels, laid from the world grid so any cut shows them evenly:
  grating (bar grating round the pit), tread (diamond plate for decks and roofs), underside
  (joists), hazard (45 degree stripes, the same read either way round).
- Small hardware: accent (orange clamps, bands and hubs, lit along the top), trim (graphite for
  edges a pixel thin).
- Decals at their exact size, drawn in the top left corner of their sheet: window (6 x 10),
  screen (16 x 9), spot (14 x 14, where the beam meets the melt).
- Lit: window, screen, amber (the lamps and the shaft's light band), lamps (the console's status
  lights), spot. Those that go dark when the drill stands idle have an _off picture, as do
  pit_wall and emitter (cold).
- The sinking towers' (melt_drill_concept.py): casing, pot, vent, frame, skirt, ring, warning.
- melt and beam (animated, lit): the molten pit, dark crust drifting slowly over glowing melt,
  and the laser beam, a bright core flickering along its length. crust is the pit while idle.
  hot_obsidian (animated, lit): the mega drill's pit floor while it runs, vanilla obsidian mapped
  onto a heat ramp, glowing red hot; idle, its pit is plain vanilla obsidian.
Run from the repo root:
    python art/tools/drill_textures.py
"""
import json
import math
import os

from pixelart import *  # noqa: F401,F403
from pixelart import ART_TEXTURES, MOD_TEXTURES, write_png
from fluid_textures import smooth_noise

FOLDER = 'block/drill/'
LIT_YELLOW = 'f2d062'      # the handrail's lit top: safety yellow catching the light


def put(t, x, y, colour):
    """Sets a pixel to a colour outside the palette (hex, no #)."""
    if 0 <= x < t.w and 0 <= y < t.h:
        t.px[y][x] = colour


def rows(t, colours, x0=0, x1=15, y0=0):
    """Fills whole rows, one colour each, from row y0 down."""
    for i, c in enumerate(colours):
        t.rect(x0, y0 + i, x1, y0 + i, c)


# ---------------------------------------------------------------- plates a block each
def panel():
    t = Tex()
    t.rect(0, 0, 15, 15, 'F')
    t.rect(0, 0, 15, 0, 'A')
    t.rect(0, 0, 0, 15, 'A')
    t.rect(15, 0, 15, 15, 'J')
    t.rect(0, 15, 15, 15, 'J')
    t.rect(2, 2, 13, 13, 'E')
    t.rect(2, 2, 13, 2, 'I')
    t.rect(2, 2, 2, 13, 'I')
    t.rect(3, 13, 13, 13, 'A')
    t.rect(13, 3, 13, 13, 'A')
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        t.set(x, y, 'J')
    return t


def panel_hatch():
    """The housing's panel with its inspection hatch: a graphite door filling the inset, a dark
    gap round it, two orange hinges on its left and a handle on its right. The window (lit, its
    own plate) sits in the door's middle, over columns 5 to 10 and rows 3 to 12."""
    t = panel()
    t.rect(2, 2, 13, 13, 'U')
    t.rect(3, 3, 12, 12, 'S')
    t.rect(3, 3, 12, 3, 'b')
    t.rect(3, 3, 3, 12, 'b')
    t.rect(4, 12, 12, 12, 'T')
    t.rect(12, 4, 12, 12, 'T')
    for y in (4, 10):
        t.set(2, y, 'X')
        t.set(3, y, 'X')
        t.set(2, y + 1, 'Z')
        t.set(3, y + 1, 'Z')
    t.set(11, 7, 'l')
    t.set(11, 8, 'l')
    t.set(12, 7, 'M')
    t.set(12, 8, 'T')
    return t


GLYPHS = {
    'D': ['110', '101', '101', '101', '110'],
    '-': ['00', '00', '11', '00', '00'],
    '1': ['010', '110', '010', '010', '111'],
}


def panel_stencil():
    """The housing's panel with the drill's number stencilled on it in graphite, D-1 (as the
    station's RG-1), centred in its inset."""
    t = panel()
    x0 = 3
    for ch in 'D-1':
        for y, row in enumerate(GLYPHS[ch]):
            for x, bit in enumerate(row):
                if bit == '1':
                    t.set(x0 + x, 6 + y, 'S')
        x0 += len(GLYPHS[ch][0]) + 1
    return t


def lid():
    """Graphite roofs (the pods, the crown's beam): one plate a block, a lit bevel along the top
    and left, a shadowed foot and right edge, a screw in each corner."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(0, 0, 15, 0, 'M')
    t.rect(0, 0, 0, 15, 'M')
    t.rect(1, 1, 14, 1, 'b')
    t.rect(1, 1, 1, 14, 'b')
    t.rect(0, 15, 15, 15, 'U')
    t.rect(15, 0, 15, 15, 'U')
    t.rect(1, 14, 14, 14, 'T')
    t.rect(14, 1, 14, 14, 'T')
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        t.set(x, y, 'U')
        t.set(x - 1, y - 1, 'M')
    return t


def shaft():
    """The drive shaft's casing, a block each: gunmetal, lit down its left edge, two cooling ribs
    down its face, and a bolted flange at the foot of each block with its shadow below it."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    t.rect(0, 0, 0, 15, 's')
    t.rect(15, 0, 15, 15, 'z')
    for x in (4, 10):
        t.rect(x, 1, x, 12, 's')
        t.rect(x + 1, 1, x + 1, 12, 'z')
    t.rect(0, 0, 15, 0, 'z')
    t.rect(0, 13, 15, 13, 's')
    t.rect(0, 14, 15, 14, 'u')
    for x in (2, 6, 9, 13):
        t.set(x, 14, 'K')
    t.rect(0, 15, 15, 15, 'z')
    return t


def rotary():
    """The rotary table on the housing's roof, where the drill string goes down: a graphite plate
    (as lid) round a gunmetal bushing, lit on its upper left, four bolts round it, the bore dark."""
    t = lid()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if r <= 2.2:
                t.set(x, y, 'K')
            elif r <= 3.2:
                t.set(x, y, 'U')
            elif r <= 5.7:
                edge = r > 4.8
                t.set(x, y, ('s' if x + y < 15 else 'z') if edge else 'u')
    for x, y in ((5, 5), (10, 5), (5, 10), (10, 10)):
        t.set(x, y, 'K')
    return t


def fan():
    """A cooling fan from above, a block: a graphite rim round a dark well, four swept blades
    under a guard (a ring and a cross), the hub left dark for the orange hub cap over it."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'U')
    t.rect(0, 0, 15, 0, 'M')
    t.rect(0, 0, 0, 15, 'M')
    t.rect(1, 1, 14, 1, 'b')
    t.rect(1, 1, 1, 14, 'b')
    t.rect(0, 15, 15, 15, 'T')
    t.rect(15, 0, 15, 15, 'T')
    t.rect(1, 14, 14, 14, 'S')
    t.rect(14, 1, 14, 14, 'S')
    for y in range(2, 14):
        for x in range(2, 14):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            if r < 1.6:
                continue
            if r <= 5.9 and (a - r * 0.22) % (math.pi / 2) < 0.62:
                t.set(x, y, 'S')
            if abs(r - 5.7) < 0.55 or (r <= 5.7 and (x in (7, 8) or y in (7, 8))):
                t.set(x, y, 'l')
    return t


def sheave():
    """The crown's sheave block, its long sides (16 x 8, rows 8 to 15): a gunmetal plate, lit
    along its top, a screw in each corner."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    t.rect(0, 8, 15, 8, 's')
    t.rect(0, 8, 0, 15, 's')
    t.rect(15, 8, 15, 15, 'z')
    t.rect(0, 15, 15, 15, 'z')
    for x, y in ((2, 10), (13, 10), (2, 13), (13, 13)):
        t.set(x, y, 'K')
    return t


def sheave_end():
    """The sheave block's ends (8 x 8, columns 0 to 7 and rows 8 to 15): gunmetal round the
    sheaves' axle boss."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    t.rect(0, 8, 7, 8, 's')
    t.rect(0, 8, 0, 15, 's')
    t.rect(7, 8, 7, 15, 'z')
    t.rect(0, 15, 7, 15, 'z')
    for y in range(9, 15):
        for x in range(1, 7):
            r = math.hypot(x + 0.5 - 4, y + 0.5 - 12)
            if r <= 1.1:
                t.set(x, y, 'K')
            elif r <= 2.4:
                t.set(x, y, 'L' if x + y < 15 else 's')
    return t


# ---------------------------------------------------------------- narrow parts, at their width
def post():
    """The column's posts, 5 wide (columns 0 to 4), a block each: a light steel leg lit down its
    left edge and shaded down its right, and over each block's joint a bolted splice plate (rows
    12 to 15 of one block and 0 to 3 of the next): its lit top edge, a bolt either side of the
    joint, its shadowed foot."""
    t = Tex()
    for y in range(16):
        t.rect(1, y, 3, y, 'E')
        t.set(0, y, 'A')
        t.set(4, y, 'I')
    for y in (13, 14, 0, 1, 2):
        t.rect(1, y, 3, y, 'F')
    t.rect(0, 12, 3, 12, 'A')
    t.set(4, 12, 'J')
    t.rect(0, 15, 4, 15, 'J')
    t.rect(0, 0, 3, 0, 'A')
    t.rect(1, 3, 3, 3, 'I')
    t.set(4, 3, 'J')
    t.set(2, 13, 'J')
    t.set(2, 1, 'J')
    return t


def brace():
    """The lattice's braces and the crown's A-frame, 4 wide (rows 0 to 3), along u: angle steel in
    graphite, lit along its upper edge, a rivet every 8 pixels."""
    t = Tex()
    rows(t, ['M', 'b', 'S', 'U'] + ['S'] * 12)
    for x in range(4, 16, 8):
        t.set(x, 1, 'L')
        t.set(x, 2, 'U')
    return t


def leg():
    """The buttress legs, 14 wide (rows 0 to 13), along u: light steel plating with a lit chamfer
    along its edge, a groove down the middle, and a plate joint every block with a bolt either
    side of the groove on both plates."""
    t = Tex()
    rows(t, ['I', 'A', 'E', 'E', 'E', 'E', 'I', 'A', 'E', 'E', 'E', 'E', 'F', 'J', 'J', 'J'])
    t.rect(15, 0, 15, 13, 'J')
    t.rect(0, 1, 0, 13, 'A')
    t.set(0, 0, 'I')
    for x in (3, 12):
        for y in (3, 10):
            t.set(x, y, 'J')
            t.set(x - 1, y - 1, 'A')
    return t


def ram():
    """The hydraulic rams' barrels, 4 wide (rows 0 to 3): graphite, lit along the top, a gland ring
    every block."""
    t = Tex()
    rows(t, ['b', 'S', 'S', 'U'] + ['S'] * 12)
    for x in (14, 15):
        rows(t, ['M', 'b', 'b', 'T'], x, x)
    return t


def rod():
    """The rams' rods, 3 wide (rows 0 to 2): polished steel, bright along the top."""
    t = Tex()
    rows(t, ['H', 'L', 'm'] + ['m'] * 13)
    return t


def rail():
    """The handrail and the ladder's rungs (rows 0 and 1 show): safety yellow, its top catching
    the light."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'Y')
    for x in range(16):
        put(t, x, 0, LIT_YELLOW)
    t.rect(0, 15, 15, 15, 'y')
    return t


def rail_post():
    """The handrail's posts and the ladder's stiles (columns 0 and 1 show): safety yellow, lit down
    the left."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'Y')
    for y in range(16):
        put(t, 0, y, LIT_YELLOW)
    return t


def stack():
    """The exhaust stacks, 8 wide (columns 0 to 7), a block high: graphite, lit down the left, a
    lip at the top with soot under it."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(0, 0, 0, 15, 'b')
    t.rect(7, 0, 7, 15, 'U')
    t.rect(0, 0, 7, 0, 'M')
    t.rect(0, 1, 7, 1, 'U')
    t.rect(1, 2, 6, 2, 'T')
    t.rect(0, 15, 7, 15, 'U')
    return t


def stack_top():
    """A stack's open top, 8 x 8: a lit rim round its dark throat."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(0, 0, 7, 7, 'K')
    t.rect(0, 0, 7, 0, 'M')
    t.rect(0, 0, 0, 7, 'M')
    t.rect(7, 0, 7, 7, 'T')
    t.rect(0, 7, 7, 7, 'T')
    t.rect(1, 1, 6, 1, 'U')
    t.rect(1, 1, 1, 6, 'U')
    return t


def duct():
    """The coolant duct up the column, a block each (6 and 8 wide, so it changes only down its
    height bar its lit left edge): clean copper, a joint at the foot of each block under its clamp."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'R')
    t.rect(0, 0, 15, 0, 'e')
    t.rect(0, 0, 0, 15, 'e')
    t.rect(0, 14, 15, 14, 'r')
    t.rect(0, 15, 15, 15, 'o')
    return t


def pulley():
    """The sheaves' rims, 3 wide (columns 0 to 2): steel, the cable's groove dark down the middle."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'L')
    t.rect(1, 0, 1, 15, 'D')
    return t


# ---------------------------------------------------------------- bands, at their height
def band():
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(0, 12, 15, 12, 'M')
    t.rect(0, 15, 15, 15, 'U')
    for x in range(1, 16, 4):
        t.set(x, 13, 'l')
        t.set(x, 14, 'T')
    return t


def band6():
    """A graphite band 6 high (rows 10 to 15), the emitter's mount: a lit lip, a bolt every 4
    pixels, and the slot its light line sits in (row 13)."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    rows(t, ['M', 'S', 'T', 'U', 'S', 'U'], y0=10)
    for x in range(2, 16, 4):
        t.set(x, 11, 'l')
    return t


def band8():
    """A graphite band 8 high (rows 8 to 15), the deck's edge, the housing's cap and the crown's
    beam: a lit lip, a bolt every 4 pixels, the slot the deck's light line sits in (row 12), a
    shadowed foot."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    rows(t, ['M', 'b', 'S', 'S', 'U', 'T', 'S', 'U'], y0=8)
    for x in range(2, 16, 4):
        t.set(x, 10, 'l')
        t.set(x, 11, 'T')
    return t


def base():
    """The base's edge, 6 high (rows 10 to 15): a lit graphite lip over hazard stripes."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(0, 10, 15, 10, 'b')
    for y in range(11, 15):
        for x in range(16):
            t.set(x, y, 'Y' if (x + y) % 8 < 4 else 'B')
    t.rect(0, 15, 15, 15, 'U')
    return t


def pit_wall(cold=False):
    """The pit's walls, 5 high (rows 11 to 15): a steel lip, then firebrick in two courses, the
    bricks a block apart staggered, glowing where the melt laps them. Cold, the glow is gone."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'O')
    t.rect(0, 11, 15, 11, 'T')
    t.rect(0, 12, 15, 12, 'r')
    t.rect(0, 13, 15, 13, 'O')
    t.rect(0, 14, 15, 14, 'O' if cold else 'Z')
    t.rect(0, 15, 15, 15, 'o' if cold else 'X')
    for x in range(0, 16, 8):
        t.set(x, 12, 'o')
        t.set(x, 13, 'o')
        t.set(x + 4, 14, 'o' if cold else 'O')
    return t


def collar():
    """The column's collar, 12 high (rows 4 to 15): gunmetal, a lit lip, louvred vents every 8
    pixels, the slot its light line sits in (row 8), a bolt line and a shadowed foot."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    rows(t, ['s', 'u', 'u', 'z', 'U', 'z', 'u', 'u', 'u', 'u', 'z', 'K'], y0=4)
    for x in range(16):
        if x % 8 in (2, 3, 4, 5):
            t.set(x, 5, 'U')
            t.set(x, 6, 'T')
    for x in range(2, 16, 4):
        t.set(x, 12, 's')
        t.set(x, 13, 'z')
    return t


def desk():
    """The console's desk, 8 high (rows 8 to 15): a lit lip under its top, then a cabinet door a
    block wide, a dark gap round it, its handle on the right; a kick plate at the foot."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(0, 8, 15, 8, 'b')
    t.rect(0, 9, 15, 14, 'U')
    t.rect(1, 10, 14, 13, 'S')
    t.rect(1, 10, 14, 10, 'b')
    t.rect(1, 10, 1, 13, 'b')
    t.set(12, 11, 'l')
    t.set(12, 12, 'l')
    t.rect(0, 15, 15, 15, 'T')
    return t


def desk_top():
    """The console's tops, rows only (row 0 at the front): graphite, its front edge lit."""
    t = Tex()
    rows(t, ['b', 'S', 'S', 'S', 'S', 'S', 'S', 'S', 'S', 'S', 'S', 'S', 'S', 'S', 'S', 'T'])
    return t


# ---------------------------------------------------------------- patterns from the world grid
def grating():
    """Bar grating round the pit, repeating every 4 pixels across and 8 along: gunmetal bearing
    bars lit on top with their shadow beside them, a cross rod every 8, dark gaps between."""
    t = Tex()
    for y in range(16):
        for x in range(16):
            k = x % 4
            if k == 0:
                c = 's'
            elif k == 1:
                c = 'z'
            else:
                c = 'z' if y % 8 == 3 else 'U'
            t.set(x, y, c)
    return t


def tread():
    """Diamond tread plate for the decks and roofs, repeating every 8 pixels: raised bars three
    pixels long, rising and falling in turn along a diagonal, each lit on top with its shadow under
    it."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'm')
    for oy in range(0, 16, 8):
        for ox in range(0, 16, 8):
            for x, y in ((1, 3), (2, 2), (3, 1), (4, 4), (5, 5), (6, 6)):
                t.set(ox + x, oy + y + 1, 'd')
            for x, y in ((1, 3), (2, 2), (3, 1), (4, 4), (5, 5), (6, 6)):
                t.set(ox + x, oy + y, 'l')
    return t


def underside():
    """The undersides of the deck and the collar: graphite between joists a block apart."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'T')
    t.rect(0, 0, 15, 1, 'S')
    t.rect(0, 0, 1, 15, 'S')
    t.rect(0, 2, 15, 2, 'U')
    t.rect(2, 0, 2, 15, 'U')
    return t


def hazard():
    """Hazard stripes at 45 degrees, repeating every 8 pixels and the same read across or down, so
    a thin strip shows them evenly whichever way it runs."""
    t = Tex()
    for y in range(16):
        for x in range(16):
            t.set(x, y, 'Y' if (x + y) % 8 < 4 else 'B')
    return t


# ---------------------------------------------------------------- small hardware
def accent():
    """Orange hardware (clamps, the stacks' bands, the fans' hub caps), used from row 0: lit along
    its top, its foot in shade."""
    t = Tex()
    rows(t, ['e'] + ['X'] * 14 + ['Z'])
    return t


def trim():
    """Graphite for edges a pixel thin (plates' rims, the lamps' bases' tops)."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    return t


# ---------------------------------------------------------------- lit
def amber(off=False):
    """The warning lamps and the shaft's light band: amber lenses, a bright line every 4 pixels,
    a rim at the foot. Off, the lenses are dark."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'O' if off else 'X')
    for x in range(1, 16, 4):
        t.rect(x, 0, x, 15, 'Z' if off else 'e')
    t.rect(0, 15, 15, 15, 'o' if off else 'Z')
    return t


def lamps(off=False):
    """The console's status lights, 2 x 2 each at columns 0, 4 and 8: green, amber and red, a
    bright corner on each. Off, they are dim."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'U')
    for x0, (lit, glint, dim) in zip((0, 4, 8), (('n', 'N', '5'), ('a', 'e', 'Z'), ('x', 'e', '8'))):
        t.rect(x0, 0, x0 + 1, 1, dim if off else lit)
        if not off:
            t.set(x0, 0, glint)
    return t


def screen():
    """A decal, 16 x 9: the console's screen in its graphite bezel, a depth gauge on the left
    filling in cyan, readout lines beside it and a green status light."""
    t = Tex()
    t.rect(0, 0, 15, 8, 'T')
    t.rect(1, 1, 14, 7, 'K')
    t.rect(2, 2, 3, 6, 'f')
    t.rect(2, 4, 3, 6, 'i')
    t.set(2, 2, 'U')
    t.set(3, 2, 'U')
    t.rect(5, 2, 9, 2, 'i')
    t.rect(5, 4, 7, 4, 'f')
    t.rect(9, 4, 12, 4, 'i')
    t.rect(5, 6, 10, 6, 'i')
    t.set(12, 6, 'j')
    t.set(13, 2, 'n')
    return t


def window(off=False):
    """A decal, 6 x 10: the hatch's window onto the generator, its coils glowing orange behind the
    glass, a dark bar down the middle, a pale glint in the top corner. Off, the coils are dark."""
    t = Tex()
    lit = ('Z', 'X', 'e', 'e', 'X', 'X', 'e', 'e', 'X', 'Z')
    dark = ('U', 'T', 'S', 'S', 'T', 'T', 'S', 'S', 'T', 'U')
    for y in range(10):
        t.rect(0, y, 5, y, (dark if off else lit)[y])
    t.rect(2, 0, 2, 9, 'K' if off else 'o')
    t.set(0, 0, 'M' if off else 'W')
    t.set(1, 1, 'M' if off else 'W')
    return t


def spot():
    """A decal, 14 x 14: where the beam meets the melt, white hot in the middle through yellow and
    orange to dark orange at its edge."""
    t = Tex()
    for y in range(14):
        for x in range(14):
            r = math.hypot(x + 0.5 - 7, y + 0.5 - 7)
            t.set(x, y, 'W' if r < 1.5 else 'Y' if r < 3 else 'X' if r < 5 else 'Z')
    return t


def emitter(cold=False):
    """The lower part of the emitter's faces, the last block before its tip (rows run down the
    slope, the tip at the foot): graphite, heat-tinted bronze lower down and glowing orange at the
    tip, as metal discolours near a hot spot. Cold, the tint stays and the glow goes."""
    t = Tex()
    tip = ['r', 'O', 'o', 'T'] if cold else ['r', 'R', 'X', 'e']
    rows(t, ['b', 'S', 'S', 'S', 'S', 'S', 'S', 'T', 'o', 'o', 'O', 'O'] + tip)
    return t


def emitter_top():
    """The upper part of the emitter's faces (rows run down the slope): graphite cooling rings, a
    lit edge, a groove and its shadow every 4 pixels."""
    t = Tex()
    for y in range(16):
        t.rect(0, y, 15, y, ('b', 'S', 'S', 'U')[y % 4])
    return t


# ---------------------------------------------------------------- the sinking towers' (melt_drill_concept.py)
def casing():
    t = Tex()
    t.rect(0, 0, 15, 15, 'E')
    t.rect(0, 0, 15, 1, 'A')
    t.rect(0, 2, 0, 14, 'A')
    t.rect(15, 2, 15, 14, 'J')
    t.rect(0, 15, 15, 15, 'I')
    t.rect(1, 14, 14, 14, 'F')
    return t


def pot():
    """The pressure vessel's brushed steel: light, with fine vertical brushing (columns a shade
    apart), changing only across, so a round pot shows it evenly; lit along the top, shadowed at
    the foot."""
    t = Tex()
    for x in range(16):
        t.rect(x, 0, x, 15, ('E', 'E', 'F', 'E', 'A', 'E', 'F', 'F')[x % 8])
    t.rect(0, 0, 15, 0, 'A')
    t.rect(0, 15, 15, 15, 'I')
    return t


def vent():
    t = Tex()
    t.rect(0, 0, 15, 15, 'b')
    t.rect(0, 0, 15, 0, 'M')
    t.rect(0, 0, 0, 15, 'M')
    t.rect(15, 0, 15, 15, 'U')
    t.rect(0, 15, 15, 15, 'U')
    for y in range(2, 14):
        t.rect(2, y, 13, y, ('M', 'S', 'U')[(y - 2) % 3])
    return t


def frame():
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    t.rect(0, 12, 15, 12, 's')
    t.rect(0, 15, 15, 15, 'z')
    return t


def skirt():
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(0, 0, 15, 0, 'M')
    t.rect(0, 15, 15, 15, 'U')
    for x in (3, 7, 11):
        t.rect(x, 4, x + 1, 11, 'U')
        t.rect(x, 4, x + 1, 4, 'T')
    return t


def ring():
    t = Tex()
    rows(t, ['H', 's', 'u', 'u', 'z', 'T', 'T', 'z', 's', 'u', 'u', 'u', 'u', 'u', 'z', 'K'])
    return t


def warning():
    t = Tex()
    rows(t, ['x'] * 5 + ['A'] * 5 + ['x'] * 6)
    t.rect(0, 0, 15, 0, 'e')
    t.rect(0, 15, 15, 15, '8')
    return t


def crust():
    t = Tex()
    t.rect(0, 0, 15, 15, 'T')
    for x, y in ((2, 3), (3, 4), (4, 4), (5, 5), (9, 2), (10, 3), (10, 4), (11, 5), (12, 5), (6, 10), (7, 11),
                 (8, 11), (9, 12), (13, 9), (13, 10), (14, 11), (1, 12), (2, 13), (3, 13)):
        t.set(x, y, '8')
    for x, y in ((4, 4), (10, 4), (8, 11), (13, 10), (2, 13)):
        t.set(x, y, 'Z')
    for x, y in ((6, 1), (14, 3), (11, 14), (0, 7)):
        t.set(x, y, 'S')
    return t


# ---------------------------------------------------------------- animated
FRAMES = 16
MELT = [(60, 16, 6), (120, 30, 8), (190, 62, 10), (236, 112, 22), (252, 170, 50), (255, 222, 120)]


def melt():
    """Frames of the molten pit: two layers of slow noise; the low places are crust (dark), the
    high ones open melt glowing orange to yellow, drifting a pixel every other frame."""
    slow = smooth_noise(71)
    fine = smooth_noise(83, cells=8)
    out = []
    for f in range(FRAMES):
        for y in range(16):
            row = []
            for x in range(16):
                v = 0.7 * slow((x + f // 2) % 16, (y + f // 4) % 16) + 0.3 * fine((x + f) % 16, y)
                k = max(0, min(len(MELT) - 1, int((v - 0.2) / 0.6 * len(MELT))))
                row.append(MELT[k] + (255,))
            out.append(row)
    return out


def beam():
    """Frames of the laser beam (drawn along v): a white-hot core between cyan edges, its
    brightness flickering in bands that run along it."""
    out = []
    for f in range(FRAMES):
        for y in range(16):
            pulse = 0.75 + 0.25 * math.sin((y + f * 3) / 16 * 2 * math.pi)
            row = []
            for x in range(16):
                edge = abs(x - 7.5) / 7.5
                core = max(0.0, 1 - edge * 1.6)
                r = int(min(255, (53 + 200 * core) * pulse))
                g = int(min(255, (200 + 55 * core) * pulse))
                b = int(min(255, 245 * pulse + 10))
                row.append((r, g, b, 255))
            out.append(row)
    return out


HOT = [(0.0, (22, 6, 12)), (0.35, (70, 12, 16)), (0.6, (150, 30, 14)), (0.8, (226, 82, 22)), (1.0, (255, 176, 70))]


def hot_obsidian():
    """Frames of the pit floor while the drill runs: obsidian glowing red hot. Vanilla's obsidian
    (read from the jar at run time, never copied) is gradient-mapped by brightness onto a heat ramp,
    its dark glass deep red and its light flecks orange to yellow, and the heat swells and ebbs in
    slow patches across it. Off, the pit is plain vanilla obsidian."""
    base = vanilla('block/obsidian')
    lum = [[sum(int(base.px[y][x][i:i + 2], 16) * w for i, w in ((0, 0.3), (2, 0.59), (4, 0.11))) / 255
            for x in range(16)] for y in range(16)]
    lo = min(min(r) for r in lum)
    hi = max(max(r) for r in lum)
    slow = smooth_noise(57)
    out = []
    for f in range(FRAMES):
        phase = 2 * math.pi * f / FRAMES
        for y in range(16):
            row = []
            for x in range(16):
                heat = (lum[y][x] - lo) / (hi - lo)
                swell = 0.75 + 0.25 * math.sin(phase + 2 * math.pi * slow(x, y))
                v = max(0.0, min(1.0, heat * 0.85 * swell + 0.12 * swell))
                for (a, ca), (b, cb) in zip(HOT, HOT[1:]):
                    if v <= b:
                        k = (v - a) / (b - a)
                        row.append(tuple(round(ca[i] + (cb[i] - ca[i]) * k) for i in range(3)) + (255,))
                        break
            out.append(row)
    return out


def publish_animated(name, frames, frametime):
    for root in (ART_TEXTURES, MOD_TEXTURES):
        out = os.path.join(root, name + '.png')
        os.makedirs(os.path.dirname(out), exist_ok=True)
        write_png(out, frames)
        with open(out + '.mcmeta', 'w', encoding='utf-8') as f:
            json.dump({'animation': {'frametime': frametime, 'interpolate': True}}, f, indent=2)


TEXTURES = {
    # plates
    'panel': panel,
    'panel_hatch': panel_hatch,
    'panel_stencil': panel_stencil,
    'lid': lid,
    'shaft': shaft,
    'rotary': rotary,
    'fan': fan,
    'sheave': sheave,
    'sheave_end': sheave_end,
    # narrow parts
    'post': post,
    'brace': brace,
    'leg': leg,
    'ram': ram,
    'rod': rod,
    'rail': rail,
    'rail_post': rail_post,
    'stack': stack,
    'stack_top': stack_top,
    'pulley': pulley,
    'duct': duct,
    # bands
    'band': band,
    'band6': band6,
    'band8': band8,
    'base': base,
    'pit_wall': pit_wall,
    'pit_wall_cold': lambda: pit_wall(cold=True),
    'collar': collar,
    'desk': desk,
    'desk_top': desk_top,
    # patterns
    'grating': grating,
    'tread': tread,
    'underside': underside,
    'hazard': hazard,
    # hardware
    'accent': accent,
    'trim': trim,
    # decals and the emitter
    'emitter': emitter,
    'emitter_cold': lambda: emitter(cold=True),
    'emitter_top': emitter_top,
    'crust': crust,
    # the sinking towers'
    'casing': casing,
    'pot': pot,
    'vent': vent,
    'frame': frame,
    'skirt': skirt,
    'ring': ring,
    'warning': warning,
}
# Lit textures stay crisp (no surface finish).
LIT = {
    'amber': amber,
    'amber_off': lambda: amber(off=True),
    'lamps': lamps,
    'lamps_off': lambda: lamps(off=True),
    'screen': screen,
    'window': window,
    'window_off': lambda: window(off=True),
    'spot': spot,
}
NAMES = list(TEXTURES) + list(LIT) + ['melt', 'beam', 'hot_obsidian']


def main():
    for name, fn in TEXTURES.items():
        publish_finished(FOLDER + name, fn())
    for name, fn in LIT.items():
        publish(FOLDER + name, fn())
    publish_animated(FOLDER + 'melt', melt(), 6)
    publish_animated(FOLDER + 'hot_obsidian', hot_obsidian(), 4)
    publish_animated(FOLDER + 'beam', beam(), 2)


if __name__ == '__main__':
    main()
