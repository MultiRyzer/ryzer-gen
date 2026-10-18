"""Checks that GUI text fits. Run before every push: python art/tools/ui_text_check.py

Each check is a lang key, worst-case values for its placeholders and the pixels the line has on
screen (from where the text starts to the edge of its screen or well, less a 2 px margin). Text is
measured with the game's own glyph widths, read from the Minecraft jar (nothing is copied). When a
screen changes, update its checks here. Lines in the mod's markup are measured as the game draws
them, icons at their own width. Exits 1 if anything
overflows.
"""
import json
import os
import re
import sys
import zipfile

import font_icons
import terminal_font

from pixelart import ROOT, VANILLA_JAR, read_png

LANG = os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'lang', 'en_us.json')

# (key, sample values, available width in px). Samples are the longest the screen can show.
CHECKS = [
    # Cable and pipe panel (cable/CableScreen): the footer is a 156 px well, text inset 4.
    ('gui.ryzergen.cable.max', ['100,000'], 148),
    ('gui.ryzergen.cable.network_buffers', ['99', '99'], 148),
    ('gui.ryzergen.item_pipe.max', ['1,000'], 148),
    ('gui.ryzergen.item_pipe.network', ['99'], 148),
    ('gui.ryzergen.fluid_pipe.max', ['100,000'], 148),
    ('gui.ryzergen.gas_pipe.max', ['100,000'], 148),
    ('gui.ryzergen.gas_pipe.network', ['99'], 148),
    # Row values share a 108 px line with the source name, which is cut to fit; the largest
    # (cryogenic fittings) leave the name at least 28 px.
    ('gui.ryzergen.cable.rate', ['1,024,000'], 80),
    ('gui.ryzergen.fluid_pipe.rate', ['1,024,000'], 80),
    ('gui.ryzergen.item_pipe.rate', ['10,240'], 80),
    # Intake pump (machine/pump/IntakePumpScreen): screen x 44 to 166, text from x 52.
    ('gui.ryzergen.intake_pump.status.no_water', [], 110),
    ('gui.ryzergen.intake_pump.status.redstone', [], 110),
    ('gui.ryzergen.intake_pump.status.pumping', [], 110),
    ('gui.ryzergen.intake_pump.status.no_power', [], 110),
    ('gui.ryzergen.intake_pump.source.found', [], 110),
    ('gui.ryzergen.intake_pump.source.missing', [], 110),
    ('gui.ryzergen.intake_pump.rate', ['1,000'], 110),
    ('gui.ryzergen.intake_pump.power', ['1,000'], 110),
    # Battery rack title (battery/HomeBatteryScreen): from x 8 to the panel's right edge, less a margin.
    ('block.ryzergen.home_battery', [], 160),
    # Fuel cycle machines (machine/processing/ProcessingScreen): status screen x 24 to 110, text from x 28.
    ('gui.ryzergen.processing.status.idle', [], 78),
    ('gui.ryzergen.processing.status.running', [], 78),
    ('gui.ryzergen.processing.status.running_time', ['59:59'], 78),
    ('gui.ryzergen.processing.status.no_power', [], 78),
    ('gui.ryzergen.processing.status.no_water', [], 78),
    ('gui.ryzergen.processing.status.output_full', [], 78),
    ('gui.ryzergen.processing.status.redstone', [], 78),
    ('gui.ryzergen.processing.status.off', [], 78),
    ('gui.ryzergen.processing.status.underpowered', [], 78),
    ('gui.ryzergen.processing.status.no_sodium', [], 78),
    # Reactor Fuel page in JEI and EMI (compat/*/Fuel*): 160 px wide, text from x 0.
    ('jei.ryzergen.fuel.microreactor', ['99,999'], 160),
    ('jei.ryzergen.fuel.station', ['99,999'], 160),
    ('jei.ryzergen.fuel.life', ['9999'], 160),
    ('jei.ryzergen.fuel.assembly', ['99,999'], 160),
    ('jei.ryzergen.fuel.blanket_uranium', [], 160),
    ('jei.ryzergen.fuel.blanket_thorium', [], 160),
    ('jei.ryzergen.fuel.blanket_slow', [], 160),
    ('jei.ryzergen.fuel.target', [], 160),
    ('jei.ryzergen.fuel.target_cost', [], 160),
    # Station control core (machine/fission/StationCoreScreen): screen x 66 to 170, text from x 70;
    # part counts start after a 10 px icon, at x 82.
    ('gui.ryzergen.station_core.building', [], 96),
    ('gui.ryzergen.station_core.needs_parts', [], 96),
    ('gui.ryzergen.station_core.blocked', ['340'], 96),
    ('gui.ryzergen.station_core.formed', [], 96),
    ('gui.ryzergen.station_core.count', ['212', '212'], 84),
    # Spent Fuel Pool (machine/pool/PoolScreen): the title from x 8, the status right-aligned to
    # x 169 on the same row. The title may take 90 and the status 70, with a gap between.
    ('container.ryzergen.pool_controller', [], 90),
    ('gui.ryzergen.pool.cooling', [], 70),
    ('gui.ryzergen.pool.low_water', [], 70),
    ('gui.ryzergen.pool.idle', [], 70),
    # Container Battery (battery/container/ContainerScreen): screen x 42 to 120, text from x 46;
    # the slot count under the map from x 126 to the panel's edge at 171.
    ('gui.ryzergen.battery.stored', ['99.99M'], 72),
    ('gui.ryzergen.battery.capacity', ['99.99M'], 72),
    ('gui.ryzergen.battery.in', ['99.9k'], 72),
    ('gui.ryzergen.battery.out', ['99.9k'], 72),
    ('gui.ryzergen.container.cooled', [], 72),
    ('gui.ryzergen.container.no_coolant', [], 72),
    ('gui.ryzergen.container.racks', ['20', '20'], 44),
    ('container.ryzergen.battery_controller', [], 160),
    # Breeder control (machine/breeder/BreederControlScreen): readout text from x 104 to 208; the
    # flow readout between its keys, x 121 to 151.
    ('gui.ryzergen.breeder.status.offline', [], 104),
    ('gui.ryzergen.breeder.status.no_fuel', [], 104),
    ('gui.ryzergen.breeder.status.no_sodium', [], 104),
    ('gui.ryzergen.breeder.status.warming', [], 104),
    ('gui.ryzergen.breeder.status.online', [], 104),
    ('gui.ryzergen.breeder.status.overheat', [], 104),
    ('gui.ryzergen.breeder.status.scram', [], 104),
    ('gui.ryzergen.breeder.status.sodium_fire', [], 104),
    ('gui.ryzergen.breeder.live', ['-99.9k', '650'], 104),
    ('gui.ryzergen.breeder.plan_empty', [], 104),
    ('gui.ryzergen.breeder.too_hot', [], 104),
    ('gui.ryzergen.breeder.hot_spots', ['19'], 104),
    ('gui.ryzergen.breeder.hot_spot_hint', [], 104),
    ('gui.ryzergen.breeder.more_flow', ['100'], 104),
    ('gui.ryzergen.breeder.plan', ['99.9k'], 104),
    ('gui.ryzergen.breeder.plan_temperature', ['42', '550'], 104),
    ('gui.ryzergen.breeder.fuel_life', ['9999'], 104),
    ('gui.ryzergen.breeder.blankets', ['19'], 104),
    # Mega drill (machine/drill/MegaDrillScreen): screen x 27 to 134, text from x 31.
    ('gui.ryzergen.mega_drill.status.charging', [], 100),
    ('gui.ryzergen.mega_drill.status.firing', [], 100),
    ('gui.ryzergen.mega_drill.status.scanning', [], 100),
    ('gui.ryzergen.mega_drill.status.no_head', [], 100),
    ('gui.ryzergen.mega_drill.status.no_tip', [], 100),
    ('gui.ryzergen.mega_drill.status.no_power', [], 100),
    ('gui.ryzergen.mega_drill.status.no_water', [], 100),
    ('gui.ryzergen.mega_drill.status.output_full', [], 100),
    ('gui.ryzergen.mega_drill.status.redstone', [], 100),
    ('gui.ryzergen.mega_drill.status.off', [], 100),
    ('gui.ryzergen.mega_drill.status.unloaded', [], 100),
    ('gui.ryzergen.mega_drill.status.finished', [], 100),
    ('gui.ryzergen.mega_drill.layer', ['-2032'], 100),
    ('gui.ryzergen.mega_drill.deep', ['-2032'], 100),
    ('gui.ryzergen.mega_drill.status.boring', [], 100),
    ('gui.ryzergen.mega_drill.speed', ['7'], 100),
    ('gui.ryzergen.mega_drill.speed_none', [], 100),
    ('container.ryzergen.mega_drill', [], 160),
    ('gui.ryzergen.assembler.status.off', [], 100),
    ('gui.ryzergen.assembler.status.redstone', [], 100),
    ('gui.ryzergen.assembler.status.no_pattern', [], 100),
    ('gui.ryzergen.assembler.status.starved', [], 100),
    ('gui.ryzergen.assembler.status.output_full', [], 100),
    ('gui.ryzergen.assembler.status.no_power', [], 100),
    ('gui.ryzergen.assembler.status.working', [], 100),
    ('gui.ryzergen.assembler.making', ['5'], 81),
    ('gui.ryzergen.assembler.needs', [], 81),
    ('gui.ryzergen.assembler.no_pattern', [], 96),
    ('container.ryzergen.assembler', [], 60),
    # Progression map (client/progression/ProgressionScreen): column headings in a 132-wide column
    # less its gap, and the spoiler button across the 210-wide panel less its margins.
    ('progression.ryzergen.column.dig', [], 120),
    ('progression.ryzergen.column.materials', [], 120),
    ('progression.ryzergen.column.microreactor', [], 120),
    ('progression.ryzergen.column.fuel_cycle', [], 120),
    ('progression.ryzergen.column.fission', [], 120),
    ('progression.ryzergen.column.breeder', [], 120),
    ('progression.ryzergen.column.fusion', [], 120),
    ('progression.ryzergen.column.swarm', [], 120),
    ('progression.ryzergen.column.more', [], 120),
    ('progression.ryzergen.spoilers.show', [], 190),
    ('progression.ryzergen.spoilers.hide', [], 190),
    # Home battery (battery/HomeBatteryScreen): screen x 30 to 114, text from x 34.
    ('gui.ryzergen.battery.stored', ['999.9k'], 76),
    ('gui.ryzergen.battery.capacity', ['999.9k'], 76),
    ('gui.ryzergen.battery.in', ['99.9k'], 76),
    ('gui.ryzergen.battery.out', ['99.9k'], 76),
    # Battery bays: label from x 7 to the charge lamp at x 39.
    ('gui.ryzergen.battery.chemistry.lead_acid', [], 30),
    ('gui.ryzergen.battery.chemistry.empty', [], 30),
    # Operator's Handbook (client/handbook/HandbookScreen): page buttons are 128 px, text centred
    # with 2 px either side; chapter tabs leave 51 px for their label beside the colour strip.
    ('handbook.ryzergen.button.map', ['Y'], 124),
    ('handbook.ryzergen.button.discord', [], 124),
    ('handbook.ryzergen.button.bug', [], 124),
    ('handbook.ryzergen.header', [], 128),
    ('handbook.ryzergen.start.key.sub', [], 100),
    ('handbook.ryzergen.start.tab', [], 51),
    ('handbook.ryzergen.basics.tab', [], 51),
    ('handbook.ryzergen.power.tab', [], 51),
    ('handbook.ryzergen.heat.tab', [], 51),
    ('handbook.ryzergen.radiation.tab', [], 51),
    ('handbook.ryzergen.help.tab', [], 51),
    # The "Press Y" toast (client/handbook/HandbookToast): text from x 30 to the toast's 160 edge.
    ('handbook.ryzergen.toast.line', ['Y'], 126),
]


C = 'gui.ryzergen.console.'
# The station's control screen (machine/fission/StationControlScreen), in the terminal font: 6 units
# a character on its 960 x 540 grid of half GUI pixels. Each check is the parts drawn on one line
# (key and worst-case values; a label and its value count a character's gap between them), the room
# they have in grid units, and the scale they are drawn at.
SHORT = ['25', '25']
OUTCOMES = ('injected', 'injected_part', 'no_input', 'nothing_needed', 'ejected', 'ejected_part', 'no_output',
            'nothing_to_eject', 'draining')
TERMINAL = [
    ([(C + 'crumbs.root', []), (C + 'crumbs.path', [])], 330, 1),
    # The pill holds the status, and the four switches sit left of it: together from x 400 to 946.
    ([(C + 'status.injecting', ['300:00'])], 190, 2),
    ([(C + 'status.runaway', [])], 190, 2),
    ([(C + 'status.flux_tilt', ['59:59'])], 190, 2),
    ([(C + 'power.off', []), (C + 'redstone.ignored', []), (C + 'safeties.on', []), (C + 'sound.off', [])], 310, 1),
    # Output box: rows from x 26 to 286.
    ([(C + 'verdict.won', [])], 260, 2),
    ([(C + 'verdict.dry', ['13'])], 260, 2),
    ([(C + 'verdict.wild', ['13'])], 260, 2),
    ([(C + 'verdict.empty', [])], 260, 2),
    ([(C + 'stars', ['3'])], 96, 1),
    ([(C + 'row.best', []), (C + 'unit.heat', ['-47'])], 260, 1),
    ([(C + 'row.graphite', []), ('', ['99'])], 260, 1),
    ([(C + 'row.tamed', []), ('', ['13 / 13'])], 260, 1),
    ([(C + 'row.power', []), (C + 'unit.fet', ['99,999'])], 260, 1),
    # Load-out box: each rack's label and note on the left, its count (2x) on the right, from x 26 to 286.
    ([(C + 'loadout', []), (C + 'loadout.paused', [])], 270, 1),
    ([(C + 'rack.fuel', []), (C + 'rack.note.uranium', [])], 200, 1),
    ([(C + 'rack.graphite', [])], 200, 1),
    ([(C + 'rack.control', [])], 200, 1),
    ([(C + 'rack.none', [])], 260, 1),
    ([(C + 'row.water', []), ('', ['XXXXXXXXXXXXXXXXXXXXXXXXXXX']), (C + 'unit.mb', ['16,000'])], 260, 1),
    ([(C + 'port.in', []), (C + 'port.err.input', []), (C + 'port.out', []), (C + 'port.err.output', [])], 260, 1),
    ([(C + 'port.in', []), (C + 'port.none', []), (C + 'port.out', []), (C + 'port.chest_free', ['9999'])], 260, 1),
    ([(C + 'port.in', []), (C + 'port.pipe', []), (C + 'port.out', []), (C + 'port.err.full', [])], 260, 1),
    ([(C + 'inject', [])], 256, 2),
    ([(C + 'eject', [])], 256, 2),
    # Core box, x 312 to 648: the tabs on its top line, five tools 50 wide from their swatch, the hint,
    # the legend on one line (its swatches and gaps padded as X) and the rules on two, all centred.
    ([(C + 'tab.uranium', []), ('', ['34']), (C + 'tab.mox', []), ('', ['47']), ('', ['XXXXXX'])], 220, 1),
    ([(C + 'core.live', [])], 100, 1),
    ([(C + 'tool.fuel', [])], 50, 1),
    ([(C + 'tool.graphite', [])], 50, 1),
    ([(C + 'tool.water', [])], 50, 1),
    ([(C + 'tool.control', [])], 50, 1),
    ([(C + 'tool.clear', [])], 50, 1),
    ([(C + 'hint', ['5'])], 330, 1),
    ([(C + 'legend.water', []), (C + 'legend.dry', []), (C + 'legend.graphite', []), (C + 'legend.mox', []),
      (C + 'legend.control', []), (C + 'legend.unloaded', []), ('', ['XXXXXXXXXXXXXXX'])], 330, 1),
    ([(C + 'rules.uranium.1', [])], 330, 1),
    ([(C + 'rules.uranium.2', [])], 330, 1),
    ([(C + 'rules.mox.1', [])], 330, 1),
    ([(C + 'rules.mox.2', [])], 330, 1),
    # Physics and forecast boxes: rows from x 674 to 934.
    ([(C + 'physics', []), ('', ['T+99:59:59'])], 270, 1),
    ([(C + 'row.thermal', []), (C + 'unit.fet', ['999,999'])], 260, 1),
    ([(C + 'row.efficiency', []), (C + 'unit.percent', ['45.0'])], 260, 1),
    ([(C + 'row.temperature', []), (C + 'unit.c', ['1000'])], 260, 1),
    ([(C + 'row.flux', []), (C + 'unit.flux', ['99.99'])], 260, 1),
    ([(C + 'row.k', []), ('', ['1.0099'])], 260, 1),
    ([(C + 'row.reactivity', []), (C + 'unit.pcm', ['-990'])], 260, 1),
    ([(C + 'row.controls', []), ('', ['25'])], 260, 1),
    ([(C + 'row.channels', []), ('', ['25 / 25'])], 260, 1),
    ([(C + 'row.draw', []), (C + 'unit.mbt', ['99,999'])], 260, 1),
    ([(C + 'row.steam', []), (C + 'unit.mbt', ['99,999'])], 260, 1),
    ([(C + 'row.burnup', []), (C + 'unit.percent', ['100.0'])], 260, 1),
    ([(C + 'row.tilt', []), ('', ['9.99'])], 260, 1),
    ([(C + 'row.steady', []), (C + 'steady.value', ['1.25', '999.9'])], 260, 1),
    ([(C + 'row.reserve', []), (C + 'reserve.value', ['5', '300:00'])], 260, 1),
    ([(C + 'history', [])], 260, 1),
    ([(C + 'forecast', []), (C + 'forecast.note', [])], 270, 1),
    ([(C + 'forecast.settles', ['1000'])], 260, 2),
    ([(C + 'forecast.rising', ['1000'])], 260, 2),
    ([(C + 'forecast.cools', ['1000'])], 260, 2),
    ([(C + 'forecast.scram', ['59:59'])], 260, 2),
    ([(C + 'forecast.meltdown', ['59:59'])], 260, 2),
    ([(C + 'forecast.line.meltdown', ['1000'])], 250, 1),
    ([(C + 'forecast.line.scram', ['900'])], 250, 1),
    ([(C + 'forecast.mid', []), (C + 'forecast.end', [])], 250, 1),
    # The switch warning: text from x 284 to 676, its title on the amber bar, the two keys at 2x.
    ([(C + 'warn.head', ['URANIUM'])], 404, 1),
    ([(C + 'warn.big', ['25'])], 392, 2),
    ([(C + 'warn.1', ['URANIUM', '13 FUEL RODS, 12 GRAPHITE, 25 OTHER'])], 392, 1),
    ([(C + 'warn.2', [])], 392, 1),
    ([(C + 'warn.3', ['CHEST, 9999 FREE', 'URANIUM'])], 392, 1),
    ([(C + 'warn.fuel', ['25']), (C + 'warn.graphite', ['25']), (C + 'warn.control', ['25']), (C + 'warn.other', ['25'])], 392, 1),
    ([(C + 'warn.go', [])], 240, 2),
    ([(C + 'warn.cancel', [])], 120, 2),
    # The status bar: the log has what the four readouts on the right leave, about 480 units.
    ([('', ['> ']), (C + 'log.rule1', [])], 480, 1),
    ([('', ['> ']), (C + 'log.rule2', [])], 480, 1),
    ([('', ['> ']), (C + 'log.mox.rule1', [])], 480, 1),
    ([('', ['> ']), (C + 'log.mox.rule2', [])], 480, 1),
    ([('', ['> ']), (C + 'log.mox.rule3', [])], 480, 1),
    ([('', ['> ']), (C + 'log.ready', [])], 480, 1),
    ([('', ['> ']), (C + 'log.won', ['47'])], 480, 1),
    ([('', ['> ']), (C + 'log.placed', ['E5', 'CONTROL ROD', '47']), (C + 'log.dry', ['13']), (C + 'log.wild', ['13'])], 480, 1),
    ([('', ['> ']), (C + 'log.cleared', ['E5', '47']), (C + 'log.dry', ['13'])], 480, 1),
    ([('', ['> ']), (C + 'log.hover', ['E5', 'CONTROL ROD', '-18']), (C + 'log.hover.needs', ['4'])], 480, 1),
    ([('', ['> ']), (C + 'log.hover', ['E5', 'CONTROL ROD', '-18']), (C + 'log.hover.control', [])], 480, 1),
    ([('', ['> ']), (C + 'log.hover', ['E5', 'CONTROL ROD', '-18']), (C + 'log.hover.dries', ['13'])], 480, 1),
    ([('', ['> ']), (C + 'log.hover', ['E5', 'CLEARING', '-18']), (C + 'log.hover.cools', ['13'])], 480, 1),
    ([('', ['> ']), (C + 'log.safeties', [])], 480, 1),
    ([('', ['> ']), (C + 'log.switch_cancelled', [])], 480, 1),
    ([('', ['> ']), (C + 'result.switched.log', ['URANIUM', '25'])], 480, 1),
] + [([('', ['> ']), (C + 'result.%s.log' % outcome, SHORT)], 480, 1) for outcome in OUTCOMES] + [
    ([(C + 'bar.plan', ['47', '47']), (C + 'bar.water', ['99,999']), (C + 'bar.steam', ['99,999']),
      (C + 'bar.core', ['1000'])], 440, 1),
] + [([(C + 'status.' + s, [])], 190, 2) for s in ('offline', 'no_fuel', 'warming', 'online', 'no_water', 'overheat',
                                                  'scram', 'overdrive', 'unstable')] + [
    ([(C + k, [])], 260, 1) for k in ('part.fuel', 'part.graphite', 'part.water', 'part.control', 'part.clear', 'power.on',
                                      'safeties.off', 'sound.on', 'redstone.high', 'redstone.low', 'output', 'output.note',
                                      'loadout.uranium', 'loadout.mox', 'rack.note.mox', 'port.chest', 'water.none',
                                      'core.plan', 'verdict.dry_one', 'verdict.wild_one', 'verdict.steady')
]


def fill(text, args):
    """A lang string with its placeholders filled, numbered (%1$s) or in turn (%s)."""
    for n, arg in enumerate(args, 1):
        text = text.replace('%%%d$s' % n, arg)
    for arg in args:
        text = text.replace('%s', arg, 1)
    return text.replace('%%', '%')


def check_terminal(lang):
    """The terminal font's lines: width at their scale, and every character in the font."""
    failed = 0
    for parts, room, scale in TERMINAL:
        pieces = []
        for key, args in parts:
            if key and key not in lang:
                print(f'MISSING  {key}')
                failed += 1
                continue
            pieces.append(fill(lang[key], args) if key else ''.join(args))
        shown = ' '.join(p for p in pieces if p).upper()
        w = terminal_font.width(shown) * scale
        unknown = sorted({ch for ch in shown if ch not in terminal_font.GLYPHS})
        ok = w <= room and not unknown
        failed += not ok
        verdict = 'ok      ' if ok else 'NO GLYPH' if unknown else 'TOO LONG'
        note = '  (not in the font: ' + ''.join(unknown) + ')' if unknown else ''
        print(f'{verdict} {w:3d}/{room:<3d} {shown}{note}')
    return failed


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


# The mod's markup (client/text/RichText): key terms in colour with an icon before them, numbers
# and key nouns in white, controls in brackets.
TOKEN = re.compile(r'\{([a-z_]+)\}|<([a-z]+)(-?)>(.*?)</\2\3>')
# Glossary words and the term (icon) each takes, as Term.WORDS.
WORD_TERMS = {'power': 'power', 'energy': 'power', 'heat': 'heat', 'meltdown': 'heat', 'coolant': 'coolant',
              'water': 'coolant', 'sodium': 'coolant', 'steam': 'steam', 'fuel': 'fuel', 'moderator': 'moderator',
              'control': 'control', 'radiation': 'radiation', 'speed': 'speed', 'blanket': 'breeding',
              'target': 'breeding', 'tritium': 'breeding', 'time': 'time', 'items': 'items'}


def icon_widths():
    """Advance of each icon in the mod's icons font: its inked width plus one."""
    rows = read_png(open(os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'textures',
                                      'font', 'icons.png'), 'rb').read())
    out = {}
    for n, name in enumerate(font_icons.ICONS):
        inked = 0
        for x in range(8):
            if any(rows[y][8 * n + x][3] for y in range(8)):
                inked = x + 1
        out[name] = inked + 1
    return out


def measure(text, lang, widths, icons):
    """A marked-up line's width, and its text as it reads."""
    def bold(t):
        # Marked text is never bold (the game's fake bold smears small letters), so it is as wide as plain.
        return width(t, widths)

    total, plain, pos = 0, [], 0
    for m in TOKEN.finditer(text):
        total += width(text[pos:m.start()], widths)
        plain.append(text[pos:m.start()])
        if m.group(1):
            word = lang.get('term.ryzergen.' + m.group(1), m.group(1))
            total += icons[WORD_TERMS[m.group(1)]] + bold(word)
            plain.append(word)
        else:
            tag, inner = m.group(2), m.group(4)
            if tag in icons:
                total += (0 if m.group(3) else icons[tag]) + bold(inner)
            elif tag in ('n', 'b'):
                total += bold(inner)
            elif tag == 'k':
                total += width('[]', widths) + bold(inner)
                inner = '[' + inner + ']'
            else:
                total += width(inner, widths)
            plain.append(inner)
        pos = m.end()
    total += width(text[pos:], widths)
    plain.append(text[pos:])
    return total, ''.join(plain)


def main():
    lang = json.load(open(LANG, encoding='utf-8'))
    widths = glyph_widths()
    icons = icon_widths()
    failed = 0
    for key, args, room in CHECKS:
        if key not in lang:
            print(f'MISSING  {key}')
            failed += 1
            continue
        text = fill(lang[key], args)
        w, shown = measure(text, lang, widths, icons)
        ok = w <= room
        failed += not ok
        print(f'{"ok      " if ok else "TOO LONG"} {w:3d}/{room:<3d} {shown}')
    failed += check_terminal(lang)
    sys.exit(1 if failed else 0)


if __name__ == '__main__':
    main()
