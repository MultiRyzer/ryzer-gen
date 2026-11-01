"""Six drafts for a 2 x 2 x 2 microreactor (32 x 32 x 32 design pixels, front to the north, z = 0).

    python drafts2x2.py [name ...]
"""
import math
import os
import sys

from mr_base import C, UP, mp, panel, fins
import microreactor_concept as mc

X, Z = (1, 0, 0), (0, 0, 1)
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'renders2x2')
MR = 'ryzergen:block/microreactor/'


class D(C):
    BLEND = ('glass_inner', 'water_far', 'water', 'bubbles', 'glass', 'steam')
    ADD = ('light',)
    render = mc.Lantern.render

    def __init__(self):
        super().__init__()
        for n in ('pedestal', 'pedestal_hazard', 'bolted', 'lid_rim', 'lid_plate', 'lid_band', 'clad', 'absorber',
                  'pipe', 'roof', 'trim', 'fan', 'floor_rim', 'floor_plate', 'floor_groove'):
            self.textures[n] = MR + n
        mc.preview_textures(self, 'running')
        for name, rows in (('plate', panel((201, 208, 215), rivets=True)), ('plate_dark', panel((70, 76, 86), rivets=True)),
                           ('roofing', panel((88, 94, 104), bevel=False)), ('fins_cu', fins((205, 120, 70), (110, 56, 30)))):
            mp._tex_cache['d2:' + name] = rows
            self.textures[name] = 'd2:' + name

    def g(self, name):
        self.group = name
        return self

    # ---- shared pieces
    def skid(self, h=2):
        self.g('static').box('trim', 0, 0, 0, 32, h, 32, sides='hazard', top='deck')

    def port(self, kind, face, a, b):
        """A 10 x 10 port flange flush with a block face, centred on (a, b): (z, y) on east/west, (x, y)
        on north/south."""
        self.g('static')
        # The flange on a graphite post down to the base, so it never floats.
        if face in ('east', 'west'):
            x1, x2 = (31, 32) if face == 'east' else (0, 1)
            p1, p2 = (28, 31) if face == 'east' else (1, 4)
            self.box('trim', p1, 1, a - 4, p2, b + 4, a + 4)
            self.box('steel_dark', x1, b - 5, a - 5, x2, b + 5, a + 5, decals={face: kind})
        else:
            z1, z2 = (31, 32) if face == 'south' else (0, 1)
            p1, p2 = (28, 31) if face == 'south' else (1, 4)
            self.box('trim', a - 4, 1, p1, a + 4, b + 4, p2)
            self.box('steel_dark', a - 5, b - 5, z1, a + 5, b + 5, z2, decals={face: kind})

    def jar(self, c, r, y0, y1, level, core_top, bubbles=10):
        """Water and its glow in a round glass jar standing on y0, up to y1."""
        self.g('water_far').rev('water_far', c, UP, [(level, r - 0.1), (y0, r - 0.1)], n=32)
        self.g('water').rev('water', c, UP, [(y0, r - 0.05), (level, r - 0.05)], n=32)
        self.rev('water_top', c, UP, [(level, r - 0.05), (level, 0)], n=32)
        self.g('light')
        self.rev('ck_core', c, UP, [(y0 + 0.5, r * 0.45), (core_top + 2, r * 0.45)], n=16)
        self.rev('ck_mid', c, UP, [(y0 + 0.3, r * 0.65), (core_top + 4, r * 0.65)], n=20)
        self.g('bubbles')
        for i in range(bubbles):
            a = i * 2.39996
            rr = 1 + (i * 0.618 % 1) * (r - 2.5)
            y = core_top + (i * 0.37 % 1) * (level - core_top - 0.6)
            self.ball('bubble_p', (c[0] + rr * math.cos(a), y, c[2] + rr * math.sin(a)), 0.35, n=6)
        self.g('glass').rev('glass', c, UP, [(y0, r), (y1, r)], n=32)
        self.g('glass_inner').rev('glass', c, UP, [(y1, r - 0.15), (y0, r - 0.15)], n=32)
        self.g('static')

    def bundle(self, x, z, y, h=8, r=2.4):
        """A fuel bundle: seven rods between two hex fittings."""
        self.g('static')
        self.rev(None, (x, y, z), UP, [(0, 0), (0, r + 0.5), (0.8, r + 0.5), (0.8, 0)], n=6, phase=math.pi / 6,
                 mats=['steel_dark'] * 3)
        for k in range(7):
            px, pz = (x, z) if k == 6 else (x + r * 0.7 * math.cos(math.radians(60 * k)), z + r * 0.7 * math.sin(math.radians(60 * k)))
            self.rod('clad', (px, y + 0.8, pz), UP, h, 0.7, n=6)
        self.rev(None, (x, y + h + 0.8, z), UP, [(0, 0), (0, r + 0.5), (0.8, r + 0.5), (0.8, 0)], n=6, phase=math.pi / 6,
                 mats=['steel_dark'] * 3)

    def save2(self, name):
        os.makedirs(OUT, exist_ok=True)
        self.save_png(os.path.join(OUT, name + '.png'), [((1, 1), 8, 0.5), ((-1, -1), 8, 0.5)])


# ================================================================ 1. Lantern XL: a big jar on its plant
def lantern_xl():
    d = D()
    d.skid(1)
    # The plant: a cabinet a block high under the jar, the ports on its faces.
    d.box('plate', 0.5, 1, 0.5, 31.5, 14, 31.5, top='deck')
    d.box('trim', 0.3, 13, 0.3, 31.7, 14.5, 31.7)
    d.g('strips').box('light_strip', 0.2, 13.5, 0.2, 31.8, 14, 31.8)
    for kind, face, a in (('port_coolant', 'east', 8), ('port_fuel', 'east', 24), ('port_steam', 'west', 8), ('port_fuel', 'west', 24),
                          ('port_energy', 'south', 16)):
        d.port(kind, face, a, 8)
    d.g('static').box('trim', 12.5, 3.5, 0.2, 19.5, 6.5, 0.5)
    d.box('trim', 13, 4.5, 0.1, 19, 5.5, 0.3, decals={'north': 'status'})
    # The jar.
    c = (16, 0, 16)
    d.rev(None, c, UP, [(14.5, 14.6), (15.5, 14.6), (15.5, 13.6)], n=32, mats=['bolted', 'bolted'])
    d.rev(None, (16, 15.51, 16), UP, [(0, 13.5), (0, 11), (0, 9), (0, 0)], n=32, mats=['floor_rim', 'floor_plate', 'floor_groove'])
    for k in range(8):
        phi = math.radians(22.5 + 45 * k)
        d.vrod('steel', 16 + 14.3 * math.cos(phi), 16 + 14.3 * math.sin(phi), 15.5, 29, 0.55, n=8)
    for x, z in ((12, 12), (20, 12), (12, 20), (20, 20)):
        d.bundle(x, z, 15.6, h=8)
    for x, z in ((16, 9), (9, 16), (23, 16), (16, 23), (16, 16)):
        d.vrod('absorber', x, z, 19, 29, 0.6, n=8)
    d.jar(c, 13.6, 15.5, 29, 27.5, 25)
    d.rev(None, c, UP, [(29, 13.6), (29, 14.6), (30, 14.6), (30, 0)], n=32, mats=['bolted', 'bolted', 'lid_plate'])
    d.g('strips').band('light_strip', c, UP, 30, 30.6, 14.5, n=32)
    d.g('static').rev(None, c, UP, [(30.6, 14.4), (31.4, 14.4), (31.4, 0)], n=32, mats=['lid_band', 'lid_plate'])
    for x, z in ((16, 9), (9, 16), (23, 16), (16, 23), (16, 16)):
        d.vrod('steel_dark', x, z, 31.4, 32.6, 1.1, n=10)
    return d


# ================================================================ 2. Plant in miniature: dome, turbine hall, cooling tower
def diorama():
    d = D()
    d.skid(2)
    # Containment: a concrete drum and a white dome, front left.
    c = (10, 0, 10)
    d.rev(None, c, UP, [(2, 9), (17, 9)], n=32, mats=['concrete'])
    d.band('steel_dark', c, UP, 16, 17.2, 9.15, n=32)
    d.g('strips').band('light_strip', c, UP, 16.3, 16.8, 9.2, n=32)
    dome = [(17.2, 9)] + [(17.2 + 9 * math.sin(math.pi / 2 * k / 8), 9 * math.cos(math.pi / 2 * k / 8)) for k in range(1, 9)]
    d.g('static').rev('shell', c, UP, dome, n=32)
    d.box('steel_dark', 7.5, 2, 0.6, 12.5, 8, 2)
    d.box('lamp_amber', 9.2, 8.5, 0.8, 10.8, 9.3, 1.6)
    # Turbine hall, front right: a long shed with a pitched roof and a window band.
    d.box('plate', 20, 2, 1, 31, 12, 15)
    d.box('porthole_on', 20, 8, 0.8, 31, 10, 1)
    d.prism_x('roofing', [(0.5, 12), (15.5, 12), (8, 16)], 19.5, 31.5)
    d.pipe('copper', [(18.6, 9, 9), (20, 9, 9)], 1.0)
    # Cooling tower, back right: a hyperboloid shell, steam rising out of it.
    t = (23.5, 0, 23.5)
    d.rev('concrete', t, UP, [(2, 8.2), (8, 6.4), (16, 5.2), (24, 5.6), (30, 6.4)], n=32)
    d.rev('concrete', t, UP, [(30, 6.1), (24, 5.3), (16, 4.9)], n=32)
    d.ring('trim', t, UP, 2, 3.2, 0, 8.5, n=32)
    d.g('steam')
    for x, y, z, r in ((23.5, 32, 23.5, 4.2), (25, 35, 22.5, 3.6), (22.5, 37.5, 24.5, 3)):
        d.ball('smoke', (x, y, z), r, n=10)
    # Switchyard, back left: a transformer with radiator fins and a small pylon.
    d.g('static').box('plate_dark', 2.5, 2, 22, 11.5, 10, 30)
    for z in (22.5, 25, 27.5):
        d.box('fins_cu', 1, 3, z, 2.5, 9, z + 1.2)
    for x in (4.5, 9.5):
        d.vrod('steel', x, 26, 10, 13, 0.6, n=6, caps='accent')
    d.pipe('steel_dark', [(14, 2, 27), (14, 20, 27), (14, 20, 20.5)], 0.45, n=6)
    # Water to the tower, steam to the hall.
    d.pipe('copper', [(19.5, 5, 13), (19.5, 5, 19), (17, 5, 21.5)], 0.8)
    # Ports on the base where each line belongs: water into the tower's basin, energy out of the
    # switchyard, fuel in at the containment's hatch side, steam out of the hall.
    d.port('port_coolant', 'east', 24, 8)
    d.port('port_energy', 'west', 26, 8)
    d.port('port_fuel', 'west', 8, 8)
    d.port('port_steam', 'east', 8, 8)
    return d


# ================================================================ 3. Engine room: a vessel lying down, a turbine set in front
def engine_room():
    d = D()
    d.skid(2)
    # The vessel: a capsule along x at the back, on saddles, a glass band round its middle showing the core.
    v = (1, 13, 22.5)
    prof = [(0, 0), (0.8, 4), (2, 6.8), (3.5, 8), (13.5, 8)]
    d.rev('lead', v, X, prof, n=28)
    d.rev('lead', (31, 13, 22.5), (-1, 0, 0), prof, n=28)
    d.g('light').rod('ck_core', (13.5, 13, 22.5), X, 5, 6.6, n=20)
    d.g('static').rod('cherenkov', (13.4, 13, 22.5), X, 5.2, 4.5, n=16)
    d.g('glass').band('glass', v, X, 12.5, 17.5, 8.05, n=28)
    d.g('static')
    for x in (4.5, 11.5, 18.5, 25.5):
        d.band('steel_dark', v, X, x, x + 1, 8.25, n=28)
    d.g('strips').band('light_strip', v, X, 12, 12.5, 8.3, n=28)
    d.band('light_strip', v, X, 17.5, 18, 8.3, n=28)
    d.g('static')
    for x in (6, 24):
        d.box('steel_dark', x, 2, 17, x + 3, 7, 28)
    # The turbine set in front: turbine casing, flywheel, generator with cooling fins.
    t = (2, 8, 8)
    d.rev('steel', t, X, [(0, 0), (0, 3), (2, 3.5), (11, 5.5), (12, 5.5), (12, 0)], n=24)
    d.rod('accent', (14.2, 8, 8), X, 1.6, 6.2, n=24, caps='steel_dark')
    d.rod('steel_dark', (13.5, 8, 8), X, 3, 1.2, n=10)
    d.rev('steel', (16.5, 8, 8), X, [(0, 0), (0, 5.4), (11, 5.4), (11, 0)], n=24)
    for x in range(18, 27, 2):
        d.band('fins_cu', (0, 8, 8), X, x, x + 0.8, 5.9, n=24)
    for x in (4, 21):
        d.box('steel_dark', x, 2, 4, x + 3, 4, 12)
    # Steam: hot leg from the vessel's top over to the turbine's inlet.
    d.pipe('copper', [(8, 21, 22.5), (8, 23, 22.5), (8, 23, 8), (8, 13.6, 8)], 1.0)
    d.pipe('copper', [(26, 6, 13), (26, 6, 15)], 0.9)
    # Gauge board at the front right corner.
    d.box('plate_dark', 28.5, 2, 1, 31.5, 14, 4)
    d.plate('dial', (30, 11, 0.95), (0, 0, -1), 2.6, 2.6)
    d.plate('dial_small', (30, 7.5, 0.95), (0, 0, -1), 2.6, 2.6)
    d.port('port_energy', 'east', 8, 8)
    d.port('port_coolant', 'west', 24, 8)
    d.port('port_steam', 'east', 24, 8)
    d.port('port_fuel', 'west', 8, 8)
    return d


# ================================================================ 4. Heat-pipe star: core in the middle, four engines at the corners
def star():
    d = D()
    d.skid(2)
    c = (16, 0, 16)
    d.rev(None, c, UP, [(2, 0), (2, 6.4), (12, 6.4), (12, 0)], n=24, mats=['steel_dark', 'lead', 'steel_dark'])
    for y in (4, 9):
        d.band('steel_dark', c, UP, y, y + 1, 6.7, n=24)
    d.g('strips').band('light_strip', c, UP, 6.4, 6.9, 6.6, n=24)
    d.g('static').vrod('steel_dark', 16, 16, 12, 20, 1.4, n=10, caps='glow')
    corners = ((6, 6), (26, 6), (6, 26), (26, 26))
    for x, z in corners:
        d.vrod('steel', x, z, 2, 19, 3.6, n=16, caps='steel_dark')
        for y in range(19, 25):
            d.vrod('fins_cu', x, z, y + 0.1, y + 0.6, 4.3, n=16)
        d.vrod('steel_dark', x, z, 25, 26, 3.6, n=16)
        d.vrod('accent', x, z, 26, 28 + (1 if x == z else 0), 1.6, n=10)
        # Two glowing heat pipes from the core's head out and up to each engine.
        dx, dz = (x - 16) / abs(x - 16), (z - 16) / abs(z - 16)
        for s in (-1, 1):
            a = (16 + dx * 3 + s * dz * 1.5, 12.3, 16 + dz * 3 - s * dx * 1.5)
            b = (16 + dx * 6 + s * dz * 1.5, 15.5, 16 + dz * 6 - s * dx * 1.5)
            e = (x - dx * 3.2 + s * dz * 1.2, 15.5, z - dz * 3.2 - s * dx * 1.2)
            d.pipe('heat', [a, b, e], 0.55, n=6)
    # Radiator walls between the engines on each side: thin copper fins standing on the skid.
    for i in range(11, 22, 2):
        for x1, z1, x2, z2 in ((i, 1, i + 0.8, 3), (i, 29, i + 0.8, 31), (1, i, 3, i + 0.8), (29, i, 31, i + 0.8)):
            d.box('fins_cu', x1, 2, z1, x2, 10, z2)
    for x1, z1, x2, z2 in ((10, 0.5, 22, 3.5), (10, 28.5, 22, 31.5), (0.5, 10, 3.5, 22), (28.5, 10, 31.5, 22)):
        d.box('steel_dark', x1, 10, z1, x2, 11.5, z2)
    d.port('port_energy', 'east', 24, 8)
    d.port('port_fuel', 'west', 8, 8)
    return d


# ================================================================ 5. Pebble hourglass plant
def hourglass():
    d = D()
    d.skid(2)
    c = (16, 0, 16)
    k = 1.6
    d.rev('steel_dark', (16, 2, 16), UP, [(0, 11.8), (2.5, 11.8), (2.5, 0)], n=32)
    d.rev('steel_dark', (16, 28, 16), UP, [(0, 0), (0, 11.8), (2, 11.8), (2, 0)], n=32)
    for y in (3.4, 28.6):
        d.g('strips').band('light_strip', c, UP, y, y + 0.6, 11.9, n=32)
    d.g('static')
    for kk in range(4):
        phi = math.radians(45 + 90 * kk)
        d.vrod('steel', 16 + 11 * math.cos(phi), 16 + 11 * math.sin(phi), 4.5, 28, 1.0, n=8)
    d.rev(None, c, UP, [(4.5, 10.2), (10, 10.2), (12.5, 0)], n=32, mats=['pebbles_spent', 'pebbles_spent'])
    d.rev('pebbles_warm', c, UP, [(12, 0.9), (14.5, 0.9)], n=8)
    d.rev(None, c, UP, [(13.6, 0), (13.6, 5.2), (15.4, 3.4), (16.4, 2.4), (18.4, 4.2), (21, 6.5), (23, 9.3), (25.6, 9.6), (26.8, 4), (27.2, 0)],
          n=32, mats=['pebbles_hot', 'pebbles_hot', 'pebbles_hot', 'pebbles_hot', 'pebbles_warm', 'pebbles', 'pebbles', 'pebbles', 'pebbles'])
    d.g('glass').rev('glass', c, UP, [(4.5, 10.6), (11, 10.6), (14.5, 5), (16.4, 2.8), (18.5, 5), (22, 10.6), (28, 10.6)], n=32)
    d.g('static')
    d.rev(None, (16, 30, 16), UP, [(0, 3), (1.6, 5.5), (2, 5.5), (2, 0)], n=16, mats=['copper', 'copper', 'pebbles'])
    # Pebble lift up the back left corner, gas blower and steam generator in the back right.
    lift = [(10, 4, 22), (3, 4, 29), (3, 31, 29), (12, 31, 20)]
    d.pipe('pebbles_spent', lift, 1.0, n=8)
    d.g('glass').pipe('glass', lift, 1.5, n=10)
    d.g('static').vrod('shell', 28, 28, 2, 26, 3.4, n=16, caps='steel_dark')
    d.band('steel_dark', (28, 0, 28), UP, 12, 13, 3.6, n=16)
    d.rod('steel', (24, 6, 30), X, 7.5, 2.4, n=14, caps='steel_dark')
    d.pipe('copper', [(25, 20, 25), (21, 20, 21)], 0.9)
    d.port('port_energy', 'south', 8, 8)
    d.port('port_coolant', 'east', 24, 8)
    d.port('port_steam', 'west', 8, 8)
    return d


# ================================================================ 6. Modular skid: four modules, one per quarter
def skid_modules():
    d = D()
    d.skid(2)
    # Front left: the reactor module, a tall capsule with a lit porthole band.
    r = (8, 0, 8)
    d.rev('lead', r, UP, [(2, 6.2), (24, 6.2), (26, 5.6), (27.5, 4), (28.6, 1.8), (29, 0)], n=24)
    for y in (7, 17):
        d.band('steel_dark', r, UP, y, y + 1.2, 6.45, n=24)
    d.g('strips').band('light_strip', r, UP, 8.2, 8.7, 6.5, n=24)
    d.g('static').plate('porthole_on', (8, 13, 1.75), (0, 0, -1), 5, 5)
    d.vrod('steel_dark', 8, 8, 29, 31, 1.5, n=10, caps='glow')
    # Front right: the steam generator, banded, with a steam drum lying across its head.
    s = (24, 0, 8)
    d.rev('steel', s, UP, [(2, 5.4), (22, 5.4)], n=24)
    for y in (5, 11, 17):
        d.band('steel_dark', s, UP, y, y + 1, 5.65, n=24)
    d.rod('steel', (18, 25, 8), X, 12, 2.8, n=16, caps='steel_dark')
    d.box('steel_dark', 22, 22, 6, 26, 23, 10)
    # Back left: turbine and generator in a cabinet, a fan on top.
    d.box('plate', 1.5, 2, 17.5, 14.5, 16, 30.5, top='roofing')
    d.rev(None, (8, 16, 24), UP, [(0, 5.4), (1.2, 5.4), (1.2, 4.6), (0.8, 4.6), (0.8, 0)], n=20, mats=['steel_dark'] * 4)
    d.hplate('fan_round', 8, 16.85, 24, 9.2)
    d.box('louvre', 2, 5, 30.5, 14, 13, 30.8)
    # Back right: the condenser, a finned block with two fans.
    d.box('steel_dark', 17.5, 2, 17.5, 30.5, 12, 30.5)
    for z in range(18, 30, 2):
        d.box('fins_cu', 17, 3, z, 31, 11, z + 0.8)
    d.box('plate_dark', 17.5, 12, 17.5, 30.5, 13.5, 30.5)
    for x in (20.75, 27.25):
        d.hplate('fan_round', x, 13.55, 24, 5.5)
    # Pipe runs over the top: reactor to steam generator, steam generator to turbine, condenser back.
    d.pipe('copper', [(11, 24, 8), (18.2, 24, 8)], 1.0)
    d.pipe('copper', [(24, 22, 11), (24, 22, 15), (12, 22, 15), (12, 16.5, 20)], 0.8)
    d.pipe('pipe', [(17.4, 7, 22), (14.6, 7, 22)], 0.7)
    d.pipe('pipe', [(24, 4, 17.4), (24, 4, 13.4)], 0.7)
    # A grated walkway across the middle, between the modules.
    d.box('grate', 14.5, 2, 1, 17.5, 2.6, 31)
    d.port('port_fuel', 'west', 8, 8)
    d.port('port_energy', 'west', 24, 8)
    d.port('port_coolant', 'east', 24, 8)
    d.port('port_steam', 'east', 8, 8)
    return d


DRAFTS = {'1_lantern_xl': lantern_xl, '2_diorama': diorama, '3_engine_room': engine_room, '4_heat_pipe_star': star,
          '5_hourglass': hourglass, '6_modular_skid': skid_modules}


# ================================================================ 1b. Lantern XL, fleshed out
def lantern_xl2(state='running'):
    """The Lantern XL with more to it: a framed plant base with a console, four core sockets round a
    cruciform control blade, jets stirring the water, flow sight glasses and a lid with a beacon."""
    d = D()
    c = (16, 0, 16)
    # ---- The base: graphite corner pilasters, plated sides holding the ports, a deck lip with its
    # light strip, a console on the front.
    d.box('trim', 0, 0, 0, 32, 1, 32, top='deck')
    d.box('plate', 0.5, 1, 0.5, 31.5, 13, 31.5, top='deck')
    for x1, z1 in ((0, 0), (29, 0), (0, 29), (29, 29)):
        d.box('trim', x1, 1, z1, x1 + 3, 14, z1 + 3)
    d.box('trim', 0.2, 13, 0.2, 31.8, 14.5, 31.8, top='deck')
    d.g('strips').box('light_strip', 0.1, 13.5, 0.1, 31.9, 14, 31.9)
    d.g('static')
    for kind, face, a in (('port_coolant', 'east', 24), ('port_fuel', 'east', 8), ('port_steam', 'west', 24),
                          ('port_fuel', 'west', 8), ('port_energy', 'south', 16)):
        if face == 'east':
            d.box('steel_dark', 31, 3, a - 5, 32, 13, a + 5, decals={'east': kind})
        elif face == 'west':
            d.box('steel_dark', 0, 3, a - 5, 1, 13, a + 5, decals={'west': kind})
        else:
            d.box('steel_dark', a - 5, 3, 31, a + 5, 13, 32, decals={'south': kind})
    # The console: a dark panel across the front with two gauges and the status strip.
    d.box('plate_dark', 6, 3, 0.1, 26, 11, 0.5)
    d.plate('dial', (10, 7, 0.05), (0, 0, -1), 5, 5)
    d.plate('dial_small', (22, 7, 0.05), (0, 0, -1), 5, 5)
    d.box('trim', 13, 6.2, 0, 19, 7.8, 0.1)
    d.box('trim', 13.5, 6.6, -0.05, 18.5, 7.4, 0.05, decals={'north': 'status'})
    # ---- The jar: bolted flanges, tie rods with orange clips, the floor in rings.
    d.rev(None, c, UP, [(14.5, 13.6), (14.5, 14.3), (15.5, 14.3), (15.5, 13.6)], n=32, mats=['bolted'] * 3)
    d.g('strips').band('light_strip', c, UP, 15.5, 16, 14.2, n=32)
    d.g('static').rev(None, (16, 16.01, 16), UP, [(0, 13.5), (0, 11), (0, 9.5), (0, 0)], n=32,
                      mats=['floor_rim', 'floor_plate', 'floor_groove'])
    for k in range(8):
        phi = math.radians(22.5 + 45 * k)
        x, z = 16 + 14 * math.cos(phi), 16 + 14 * math.sin(phi)
        d.vrod('steel', x, z, 16, 28, 0.5, n=8)
        d.vrod('accent', x, z, 21.4, 22.6, 0.8, n=8)
    # Four core sockets (a sealed core in each) round a cruciform control blade on its drive shaft.
    for x, z in ((11.8, 11.8), (20.2, 11.8), (11.8, 20.2), (20.2, 20.2)):
        d.bundle(x, z, 16.1, h=7.5)
    lift = {'running': 2.5, 'overdrive': 5.5}.get(state, 0)
    d.box('absorber', 9.5, 16.6 + lift, 15.4, 22.5, 24.6 + lift, 16.6)
    d.box('absorber', 15.4, 16.6 + lift, 9.5, 16.6, 24.6 + lift, 22.5)
    d.vrod('steel', 16, 16, 24.6 + lift, 29.5, 0.45, n=6)
    # Jets in the floor between the cores, stirring the water: streams of bubbles rise off them.
    for x, z in ((16, 7.5), (16, 24.5), (7.5, 16), (24.5, 16)):
        d.vrod('steel_dark', x, z, 16, 16.8, 0.9, n=8, caps='trim')
        d.g('bubbles')
        for i in range(5):
            d.ball('bubble_p', (x + 0.4 * math.sin(i * 1.7), 17.5 + i * 1.9, z + 0.4 * math.cos(i * 2.3)), 0.3 + 0.05 * (i % 2), n=6)
        d.g('static')
    d.jar(c, 13.6, 16, 28, 26.5, 24.5, bubbles=6)
    # ---- The lid: flange, light ring, a deck with bolts, the blade's drive in the middle and a beacon on it.
    d.rev(None, c, UP, [(28, 13.6), (28, 14.3), (29, 14.3), (29, 0)], n=32, mats=['bolted'] * 3)
    d.g('strips').band('light_strip', c, UP, 29, 29.5, 14.2, n=32)
    d.g('static').rev(None, c, UP, [(29.5, 14.2), (30.3, 14.2), (30.3, 13.4), (30.3, 0)], n=32,
                      mats=['lid_band', 'lid_rim', 'lid_plate'])
    for k in range(12):
        a = math.radians(15 + 30 * k)
        d.rod('steel', (16 + 13.8 * math.cos(a), 30.3, 16 + 13.8 * math.sin(a)), UP, 0.3, 0.3, n=6)
    d.vrod('steel_dark', 16, 16, 30.3, 31.5, 2.6, n=16)
    d.g('strips').band('light_strip', c, UP, 31, 31.25, 2.65, n=16)
    d.g('static').vrod('lamp_amber', 16, 16, 31.5, 32.4, 0.9, n=8)
    for k in range(4):
        a = math.radians(45 + 90 * k)
        d.vrod('steel_dark', 16 + 8 * math.cos(a), 16 + 8 * math.sin(a), 30.3, 31.1, 1.1, n=10, caps='lid_plate')
    # ---- Flow sight glasses: the cooling loop rises up the two back corners, past a round window
    # where a paddle wheel spins with the flow, and into the lid.
    for x, z, nx in ((4.2, 27.8, -1), (27.8, 27.8, 1)):
        n = (nx * 0.7071, 0, 0.7071)
        top = (16 + nx * -1 * -10.0 * 1, 0, 0)
        d.pipe('pipe', [(x, 14.5, z), (x, 26.5, z), (16 + (x - 16) * 0.62, 28.5, 16 + (z - 16) * 0.62)], 0.8, n=8)
        d.rod('steel_dark', (x - n[0] * 0.2, 20, z - n[2] * 0.2), n, 1.2, 2.2, n=14)
        d.plate('fan_round', (x + n[0] * 1.05, 20, z + n[2] * 1.05), n, 3.4, 3.4)
    return d


DRAFTS['1b_lantern_xl'] = lantern_xl2

if __name__ == '__main__':
    names = sys.argv[1:] or list(DRAFTS)
    for name in names:
        DRAFTS[name]().save2(name)
        print('rendered', name)
