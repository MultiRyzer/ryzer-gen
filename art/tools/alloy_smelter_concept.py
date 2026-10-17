"""The Alloy Smelter as a plain block, in the spirit of vanilla's furnaces, rendered beside them.

The block is one cube but for the fire door, which sits a pixel back in an opening on the front
(8 x 5, centred, below the band), so that only the firebox's face glows while it burns. Five
boxes: the body from a pixel back, and four round the opening. Every face of every box takes its
side of the block's texture by position, and the opening's reveal takes the family's gunmetal.
Textures: art/tools/alloy_smelter_textures.py. The same boxes go into datagen once approved.

    python art/tools/alloy_smelter_concept.py
writes art/concepts/alloy_smelter/{off,on}.json and a sheet: off, burning, then vanilla's
furnace and blast furnace.
"""
import json
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, 'art', 'concepts', 'alloy_smelter')
FOLDER = 'ryzergen:block/machine/alloy_smelter/'
FACES = ('north', 'south', 'east', 'west', 'up', 'down')
# The opening, in model pixels.
X0, X1, Y0, Y1 = 4, 12, 3, 8


def faces_for(box):
    """Each face's texture: the block's own side by position, the opening's reveal in gunmetal."""
    (x0, y0, z0), (x1, y1, z1) = box
    out = {}
    for face in FACES:
        outer = {'north': z0 == 0, 'south': z1 == 16, 'west': x0 == 0, 'east': x1 == 16,
                 'up': y1 == 16, 'down': y0 == 0}[face]
        if face == 'north' and z0 == 1:
            out[face] = {'texture': '#firebox'}
        elif outer:
            out[face] = {'texture': {'north': '#front', 'up': '#top', 'down': '#bottom'}.get(face, '#side')}
        else:
            out[face] = {'texture': '#reveal'}
    return out


def model(lit):
    boxes = [((0, 0, 1), (16, 16, 16)),
             ((0, 0, 0), (X0, 16, 1)), ((X1, 0, 0), (16, 16, 1)),
             ((X0, 0, 0), (X1, Y0, 1)), ((X0, Y1, 0), (X1, 16, 1))]
    elements = [{'from': list(box[0]), 'to': list(box[1]), 'faces': faces_for(box)} for box in boxes]
    textures = {'front': FOLDER + 'front', 'side': FOLDER + 'side', 'top': FOLDER + 'top',
                'bottom': FOLDER + 'bottom', 'firebox': FOLDER + ('firebox_on' if lit else 'firebox'),
                'reveal': 'ryzergen:block/machine/family/metal', 'particle': FOLDER + 'side'}
    return {'textures': textures, 'elements': elements}


def vanilla(front, side, top):
    return {'textures': {'front': 'minecraft:block/' + front, 'side': 'minecraft:block/' + side,
                         'top': 'minecraft:block/' + top},
            'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {
                f: {'texture': '#front' if f == 'north' else '#top' if f in ('up', 'down') else '#side'} for f in FACES}}]}


def main():
    os.makedirs(OUT, exist_ok=True)
    models = {'off': model(False), 'on': model(True),
              'vanilla_furnace': vanilla('furnace_front', 'furnace_side', 'furnace_top'),
              'vanilla_blast_furnace': vanilla('blast_furnace_front', 'blast_furnace_side', 'blast_furnace_top')}
    paths = []
    for name, m in models.items():
        path = os.path.join(OUT, name + '.json')
        json.dump(m, open(path, 'w'), indent=1)
        paths.append(path)
    tools = os.path.join(ROOT, 'art', 'tools')
    print(subprocess.run([sys.executable, os.path.join(tools, 'grid_check.py')] + paths[:2],
                         capture_output=True, text=True).stdout.strip())
    # A row east to west, so the front view (from the north west) reads left to right.
    sheet = os.path.join(OUT, 'sheet.png')
    subprocess.run([sys.executable, os.path.join(tools, 'model_preview.py'), '--scale', '5', sheet]
                   + [f'{p}@{-i * 2},0,0' for i, p in enumerate(paths)], check=True)
    print(sheet)


if __name__ == '__main__':
    main()
