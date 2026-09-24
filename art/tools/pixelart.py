"""Shared pixel-art toolkit for Ryzer Gen textures: the palette, a canvas, PNG output and the
recurring pieces of the house style (bevelled steel housings, bolts, hazard paint, copper panels).

Pure Python, no PIL needed. Every texture script in art/tools imports this, so new machines and
GUIs pick up the same colours and details.
"""
import os
import random
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ART_TEXTURES = os.path.join(ROOT, 'art', 'textures')
MOD_TEXTURES = os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'textures')


PAL = {
    # steel, dark to light
    'K': '15171b', 'D': '2a2e35', 'd': '373c44', 'm': '454b54', 'M': '535a64',
    'l': '646c77', 'L': '7b848f', 'h': '9aa3ae', 'H': 'c2c9d2',
    # hazard paint
    'Y': 'e2b124', 'y': 'a87c16', 'B': '242428',
    # reactor glow (Cherenkov blue)
    'g': '15407e', 'G': '2a7cd6', 'c': '62c2ff', 'C': 'b8ecff', 'W': 'f4fdff',
    # lead
    'p': '44465a', 'P': '5a5d72', 'q': '767a90', 'Q': '9599ad',
    # copper and patina
    'o': '552b1b', 'O': '7e4128', 'r': 'a65a34', 'R': 'c97745', 'e': 'e9a36a', 't': '4f8f7b',
    # water
    'w': '1d4a8c', 'v': '3571c0', 'V': '78b2ec',
    # status lights
    'n': '44d65e', 'N': 'b6ffc2', 'x': 'd13c3c', 'a': 'f0a030',
    # graphite
    'k': '1f2024',
    # Modern ramp (the formed machines): clean casing greys, light to dark
    'A': 'e1e6eb', 'E': 'c9d0d7', 'F': 'b3bbc4', 'I': '9ea7b1', 'J': '868f9a',
    # graphite trim, light to dark
    'b': '4d545d', 'S': '3a4047', 'T': '2b3036', 'U': '1d2126',
    # gunmetal (reactor vessel), light to dark
    's': '6a7280', 'u': '545b68', 'z': '3f4550',
    # cyan accent light (dim, lit, bright) and orange accent
    'f': '1b5a73', 'i': '35c8f5', 'j': 'a6eeff', 'X': 'ff8a1e', 'Z': 'a8520c',
    # circuit board green, dark to light
    '5': '1c4a30', '6': '2a6b45', '7': '3f9160',
    # deep red (energy stripes), and fluorite purples dark to light
    '8': '8a2626', '1': '2a1247', '2': '5a2a8f', '3': '8a4ccc', '4': 'c9a0f5',
}


class Tex:
    def __init__(self, w=16, h=None):
        self.w = w
        self.h = h or w
        self.px = [[None] * self.w for _ in range(self.h)]

    def set(self, x, y, k):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = PAL[k]

    def rect(self, x0, y0, x1, y1, k):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, k)

    def stamp(self, x0, y0, rows):
        for dy, row in enumerate(rows):
            for dx, k in enumerate(row):
                if k != '.':
                    self.set(x0 + dx, y0 + dy, k)

    def rgba(self, x, y):
        c = self.px[y][x]
        if c is None:
            return (0, 0, 0, 0)
        return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), 255)


def write_png(path, rows):
    h, w = len(rows), len(rows[0])
    raw = b''.join(b'\x00' + bytes(v for p in row for v in p) for row in rows)

    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    png = (b'\x89PNG\r\n\x1a\n'
           + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
           + chunk(b'IDAT', zlib.compress(raw, 9))
           + chunk(b'IEND', b''))
    with open(path, 'wb') as f:
        f.write(png)


def save(tex, path):
    write_png(path, [[tex.rgba(x, y) for x in range(tex.w)] for y in range(tex.h)])


def mirrored(tex):
    out = Tex(tex.w, tex.h)
    out.px = [list(reversed(row)) for row in tex.px]
    return out


def housing(t, x0, y0, x1, y1, seed, fill='M', scuffs=('m', 'l', 'm')):
    """Steel casing over a region: plate with light scuffs, bevelled edge lit from the top left."""
    t.rect(x0, y0, x1, y1, fill)
    rnd = random.Random(seed)
    for _ in range((x1 - x0 + 1) * (y1 - y0 + 1) // 16):
        t.set(rnd.randint(x0 + 2, x1 - 2), rnd.randint(y0 + 2, y1 - 2), rnd.choice(scuffs))
    t.rect(x0, y0, x1, y0, 'h')
    t.rect(x0, y0, x0, y1, 'h')
    t.rect(x0, y1, x1, y1, 'D')
    t.rect(x1, y0, x1, y1, 'D')
    t.set(x1, y0, 'l')
    t.set(x0, y1, 'l')
    t.rect(x0 + 1, y0 + 1, x1 - 1, y0 + 1, 'L')
    t.rect(x0 + 1, y0 + 1, x0 + 1, y1 - 1, 'L')
    t.rect(x0 + 1, y1 - 1, x1 - 1, y1 - 1, 'd')
    t.rect(x1 - 1, y0 + 1, x1 - 1, y1 - 1, 'd')
    t.set(x1 - 1, y0 + 1, 'm')
    t.set(x0 + 1, y1 - 1, 'm')


def framed(seed):
    t = Tex()
    housing(t, 0, 0, 15, 15, seed)
    return t


def copper_infill(t, x0, y0, x1, y1, seed):
    """Copper panel set into the steel frame, with a recessed edge and a little patina."""
    t.rect(x0, y0, x1, y1, 'r')
    rnd = random.Random(seed)
    for _ in range(10):
        t.set(rnd.randint(x0 + 1, x1 - 1), rnd.randint(y0 + 1, y1 - 1), rnd.choice('ORR'))
    for _ in range(3):
        t.set(rnd.randint(x0 + 1, x1 - 1), rnd.randint(y0 + 1, y1 - 1), 't')
    t.rect(x0, y0, x1, y0, 'o')
    t.rect(x0, y0, x0, y1, 'o')
    t.rect(x0, y1, x1, y1, 'e')
    t.rect(x1, y0, x1, y1, 'R')


def bolt(t, x, y):
    t.set(x, y, 'H')
    t.set(x + 1, y, 'l')
    t.set(x, y + 1, 'l')
    t.set(x + 1, y + 1, 'D')


def hazard_band(t, y0, y1, x0=0, x1=15):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t.set(x, y, 'Y' if (x + y) % 4 < 2 else 'B')
    t.rect(x0, y0 - 1, x1, y0 - 1, 'K')
    t.rect(x0, y1 + 1, x1, y1 + 1, 'D')


def material(fill, noise, seed, count=24):
    t = Tex()
    t.rect(0, 0, 15, 15, fill)
    rnd = random.Random(seed)
    for _ in range(count):
        t.set(rnd.randint(0, 15), rnd.randint(0, 15), rnd.choice(noise))
    return t


def publish(name, tex):
    """Writes a texture to art/textures and into the mod's assets, e.g. publish('gui/microreactor', t)."""
    for base in (ART_TEXTURES, MOD_TEXTURES):
        out = os.path.join(base, name + '.png')
        os.makedirs(os.path.dirname(out), exist_ok=True)
        save(tex, out)


# ---------------------------------------------------------------- reading PNGs and vanilla textures

def read_png(data):
    """Decodes a PNG (any bit depth; grey, RGB, palette, grey+alpha or RGBA) into rows of RGBA tuples."""
    pos, idat, palette, trns = 8, b'', None, None
    while pos < len(data):
        length, tag = struct.unpack('>I4s', data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        if tag == b'IHDR':
            w, h, depth, ctype, _, _, interlace = struct.unpack('>IIBBBBB', body)
            assert interlace == 0, 'interlaced PNGs are not supported'
        elif tag == b'PLTE':
            palette = [tuple(body[i:i + 3]) for i in range(0, len(body), 3)]
        elif tag == b'tRNS':
            trns = body
        elif tag == b'IDAT':
            idat += body
        pos += 12 + length
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    bits = depth * channels
    stride = (w * bits + 7) // 8
    bpp = max(1, bits // 8)
    raw = zlib.decompress(idat)
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        f, line = raw[i], bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        prev = line
        # Unpack samples.
        samples = []
        if depth < 8:
            for byte in line:
                for shift in range(8 - depth, -1, -depth):
                    samples.append((byte >> shift) & ((1 << depth) - 1))
        elif depth == 8:
            samples = list(line)
        else:
            samples = [line[k] << 8 | line[k + 1] for k in range(0, len(line), 2)]
            samples = [v >> 8 for v in samples]
        row = []
        for x in range(w):
            s = samples[x * channels:(x + 1) * channels]
            if ctype == 3:
                r, g, b = palette[s[0]]
                alpha = trns[s[0]] if trns is not None and s[0] < len(trns) else 255
                row.append((r, g, b, alpha))
            elif ctype == 0:
                v = s[0] * 255 // ((1 << depth) - 1)
                row.append((v, v, v, 255))
            elif ctype == 4:
                row.append((s[0], s[0], s[0], s[1]))
            elif ctype == 2:
                row.append((s[0], s[1], s[2], 255))
            else:
                row.append(tuple(s))
        rows.append(row)
    return rows


VANILLA_JAR = os.path.join(ROOT, 'mod', 'build', 'moddev', 'artifacts',
                           'neoforge-21.1.251-client-extra-aka-minecraft-resources.jar')


def vanilla(path):
    """A vanilla texture as a Tex, read straight from the Minecraft jar the build downloads
    (run a Gradle build once first). Nothing from Mojang is copied into the repo; only our
    recoloured results are. Example: vanilla('block/stone')."""
    import zipfile
    with zipfile.ZipFile(VANILLA_JAR) as jar:
        rows = read_png(jar.read('assets/minecraft/textures/' + path + '.png'))
    t = Tex(len(rows[0]), len(rows))
    for y, row in enumerate(rows):
        for x, (r, g, b, a) in enumerate(row):
            t.px[y][x] = '%02x%02x%02x' % (r, g, b) if a >= 128 else None
    return t


def rgb(hex_colour):
    return tuple(int(hex_colour[i:i + 2], 16) for i in (0, 2, 4))


def luminance(hex_colour):
    r, g, b = rgb(hex_colour)
    return 0.299 * r + 0.587 * g + 0.114 * b
