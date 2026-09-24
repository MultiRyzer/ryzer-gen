"""Checks that GUI text fits. Run before every push: python art/tools/ui_text_check.py

Each check is a lang key, worst-case values for its placeholders and the pixels the line has on
screen (from where the text starts to the edge of its screen or well, less a 2 px margin). Text is
measured with the game's own glyph widths, read from the Minecraft jar (nothing is copied). When a
screen changes, update its checks here. Exits 1 if anything overflows.
"""
import json
import os
import sys
import zipfile

from pixelart import ROOT, VANILLA_JAR, read_png

LANG = os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'lang', 'en_us.json')

# (key, sample values, available width in px). Samples are the longest the screen can show.
CHECKS = [
    # Cable and pipe panel (cable/CableScreen): rows and footer inside a 156 px well, text inset 4.
    ('gui.ryzergen.cable.max', ['100,000'], 148),
    ('gui.ryzergen.cable.network_buffers', ['99', '99'], 148),
    ('gui.ryzergen.item_pipe.max', ['1,000'], 148),
    ('gui.ryzergen.item_pipe.network', ['99'], 148),
    ('gui.ryzergen.fluid_pipe.max', ['100,000'], 148),
    ('gui.ryzergen.gas_pipe.max', ['100,000'], 148),
    ('gui.ryzergen.gas_pipe.network', ['99'], 148),
    # Row values share the line with the source name, which is cut to fit, so they get half.
    ('gui.ryzergen.cable.rate', ['100,000'], 74),
    ('gui.ryzergen.fluid_pipe.rate', ['100,000'], 74),
    ('gui.ryzergen.item_pipe.rate', ['1,000'], 74),
    # Intake pump (machine/pump/IntakePumpScreen): screen x 44 to 166, text from x 52.
    ('gui.ryzergen.intake_pump.status.no_water', [], 110),
    ('gui.ryzergen.intake_pump.status.redstone', [], 110),
    ('gui.ryzergen.intake_pump.status.pumping', [], 110),
    ('gui.ryzergen.intake_pump.status.no_power', [], 110),
    ('gui.ryzergen.intake_pump.source.found', [], 110),
    ('gui.ryzergen.intake_pump.source.missing', [], 110),
    ('gui.ryzergen.intake_pump.rate', ['1,000'], 110),
    ('gui.ryzergen.intake_pump.power', ['1,000'], 110),
    # Station control core (machine/fission/StationCoreScreen): screen x 66 to 170, text from x 70;
    # part counts start after a 10 px icon, at x 82.
    ('gui.ryzergen.station_core.building', [], 96),
    ('gui.ryzergen.station_core.needs_parts', [], 96),
    ('gui.ryzergen.station_core.blocked', ['340'], 96),
    ('gui.ryzergen.station_core.formed', [], 96),
    ('gui.ryzergen.station_core.count', ['212', '212'], 84),
    # Station controls (machine/fission/StationControlScreen): screen x 100 to 210, text from x 104.
    ('gui.ryzergen.station.status.warming', [], 104),
    ('gui.ryzergen.station.status.no_water', [], 104),
    ('gui.ryzergen.station.status.overheat', [], 104),
    ('gui.ryzergen.station.status.no_fuel', [], 104),
    ('gui.ryzergen.station.status.overdrive', [], 104),
    ('gui.ryzergen.station.status.flux_tilt', ['59:59'], 104),
    ('gui.ryzergen.station.status.unstable', [], 104),
    ('gui.ryzergen.station.live', ['99.9k', '1000'], 104),
    ('gui.ryzergen.station.plan', ['99.9k'], 104),
    ('gui.ryzergen.station.plan_empty', [], 104),
    ('gui.ryzergen.station.too_hot', [], 104),
    ('gui.ryzergen.station.stranded', ['99.9k'], 104),
    ('gui.ryzergen.station.plan_temperature', ['40', '600'], 104),
    ('gui.ryzergen.station.rod_life', ['9999'], 104),
    ('gui.ryzergen.station.rating', ['100'], 104),
    # Home battery (battery/HomeBatteryScreen): screen x 30 to 114, text from x 34.
    ('gui.ryzergen.battery.stored', ['999.9k'], 76),
    ('gui.ryzergen.battery.capacity', ['999.9k'], 76),
    ('gui.ryzergen.battery.in', ['99.9k'], 76),
    ('gui.ryzergen.battery.out', ['99.9k'], 76),
    # Battery bays: label from x 7 to the charge lamp at x 39.
    ('gui.ryzergen.battery.chemistry.lead_acid', [], 30),
    ('gui.ryzergen.battery.chemistry.empty', [], 30),
]


def glyph_widths():
    """Advance of each character in the default font: the glyph's inked width plus one, as the game
    lays it out. Space is 4."""
    with zipfile.ZipFile(VANILLA_JAR) as jar:
        font = json.loads(jar.read('assets/minecraft/font/include/default.json'))
        widths = {' ': 4}
        for provider in font['providers']:
            if provider.get('type') != 'bitmap':
                continue
            name = provider['file'].split(':')[-1]
            rows = read_png(jar.read('assets/minecraft/textures/' + name))
            lines = provider['chars']
            cell_w = len(rows[0]) // len(lines[0])
            cell_h = len(rows) // len(lines)
            scale = (provider.get('height') or 8) / cell_h
            for row, line in enumerate(lines):
                for col, ch in enumerate(line):
                    if ch in widths or ch == '\u0000':
                        continue
                    inked = 0
                    for x in range(cell_w):
                        if any(rows[row * cell_h + y][col * cell_w + x][3] for y in range(cell_h)):
                            inked = x + 1
                    widths[ch] = int(inked * scale + 0.5) + 1
        return widths


def width(text, widths):
    return sum(widths.get(ch, 6) for ch in text)


def main():
    lang = json.load(open(LANG, encoding='utf-8'))
    widths = glyph_widths()
    failed = 0
    for key, args, room in CHECKS:
        if key not in lang:
            print(f'MISSING  {key}')
            failed += 1
            continue
        text = lang[key].replace('%%', '%')
        for arg in args:
            text = text.replace('%s', arg, 1)
        w = width(text, widths)
        ok = w <= room
        failed += not ok
        print(f'{"ok      " if ok else "TOO LONG"} {w:3d}/{room:<3d} {text}')
    sys.exit(1 if failed else 0)


if __name__ == '__main__':
    main()
