"""Turns a machine's generated block models into a Blockbench project, for refining by hand.

Reads the models datagen wrote (mod/src/generated) and the textures they use (mod/src/main), and
writes art/blockbench/<machine>.bbmodel: every box as a named cube in groups (the body, then each
moving part in a group of its own, pivoted where MachineMotion moves it), with every face's UV as
the game maps it, and each texture linked to its file in the mod with a copy embedded. The glows
the renderer draws over the model (MachineMotion) go in a hidden group that is not exported, so
you can see where they sit without them becoming part of the model.

Run from the repo root after runData:
    python art/tools/blockbench_export.py electrorefiner
"""
import base64
import json
import os
import sys
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODELS = os.path.join(ROOT, 'mod', 'src', 'generated', 'resources', 'assets', 'ryzergen', 'models', 'block')
ASSETS = os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'assets')
OUT = os.path.join(ROOT, 'art', 'blockbench')

# Each machine: its body model, a name for each of its boxes and the groups they go in, its moving
# parts (model, group name, pivot, a name for each box), and the renderer's glows.
MACHINES = {
    'electrorefiner': {
        'body': 'electrorefiner',
        # The body's boxes, named in the order datagen adds them (electrorefinerBoxes).
        'order': ['skid', 'cabinet', 'readout', 'light strip', 'rectifier', 'worktop', 'well',
                  'front wall west', 'front wall east', 'front wall below glass', 'front wall above glass',
                  'back wall', 'west wall', 'east wall', 'frozen salt', 'sight glass',
                  'frame top', 'frame bottom', 'frame west', 'frame east',
                  'label plate', 'heater terminal box', 'heater conduit',
                  'lid', 'drive head', 'charging port', 'off-gas stack', 'stack cap', 'cathode gland',
                  'west insulator', 'west riser', 'east insulator', 'east riser',
                  'west bus bar', 'west bus link', 'east bus bar', 'east bus link', 'back connector'],
        # How they are grouped in the outliner.
        'groups': [
            ('base', ['skid', 'cabinet', 'readout', 'light strip', 'worktop', 'well']),
            ('rectifier', ['rectifier']),
            ('pot', ['front wall west', 'front wall east', 'front wall below glass', 'front wall above glass',
                     'back wall', 'west wall', 'east wall', 'frozen salt', 'sight glass',
                     'frame top', 'frame bottom', 'frame west', 'frame east',
                     'label plate', 'heater terminal box', 'heater conduit']),
            ('lid', ['lid', 'drive head', 'charging port', 'off-gas stack', 'stack cap', 'cathode gland']),
            ('bus bars', ['west insulator', 'west riser', 'east insulator', 'east riser',
                          'west bus bar', 'west bus link', 'east bus bar', 'east bus link']),
            ('back connector', ['back connector']),
        ],
        # Boxes that glow (full bright) in the game.
        'lit': {'readout'},
        'parts': [
            ('electrorefiner_basket', 'basket (turns about x 8, z 6.5)', [8, 10, 6.5],
             ['shaft', 'hub arm east-west', 'hub arm north-south',
              'west basket', 'east basket', 'north basket', 'south basket']),
            ('electrorefiner_cathode', 'cathode (lifts 1.25 for harvest)', [11, 12, 3.75],
             ['rod', 'plated uranium', 'crosshead']),
        ],
        'glows': [
            ('molten salt', 'ryzergen:block/machine/electrorefiner/salt_molten',
             [2.75, 9.27, 2.75], [13.25, 9.27, 10.25], 'up'),
            ('heater ring', 'ryzergen:block/machine/electrorefiner/heater',
             [1.5, 7.82, 1.5], [14.5, 7.82, 11.5], 'up'),
            ('status lamp', 'ryzergen:block/machine/electrorefiner/lamp',
             [4.5, 4.5, 1.23], [5.5, 5.5, 1.23], 'north'),
        ],
    },
}


def default_uv(face, f, t):
    """A face's UV when the model gives none: mapped by position, as the game does it."""
    return {
        'down': [f[0], 16 - t[2], t[0], 16 - f[2]],
        'up': [f[0], f[2], t[0], t[2]],
        'north': [16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]],
        'south': [f[0], 16 - t[1], t[0], 16 - f[1]],
        'west': [f[2], 16 - t[1], t[2], 16 - f[1]],
        'east': [16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]],
    }[face]


class Project:
    def __init__(self, name):
        self.name = name
        self.elements = []
        self.textures = []
        self.by_location = {}

    def texture(self, key, location, particle=False):
        """The index of a texture (by resource location), added on first use."""
        if location in self.by_location:
            return self.by_location[location]
        namespace, path = location.split(':', 1)
        file = os.path.join(ASSETS, namespace, 'textures', *path.split('/')) + '.png'
        data = open(file, 'rb').read()
        width, height = int.from_bytes(data[16:20], 'big'), int.from_bytes(data[20:24], 'big')
        ids = {t['id'] for t in self.textures}
        tex_id = key if key not in ids else f'{key}_{len(self.textures)}'
        texture = {
            'path': file, 'relative_path': os.path.relpath(file, OUT).replace(os.sep, '/'),
            'name': os.path.basename(file), 'folder': os.path.dirname(path),
            'namespace': namespace, 'id': tex_id, 'particle': particle,
            'render_mode': 'default', 'render_sides': 'auto', 'visible': True, 'internal': False,
            'saved': True, 'uuid': str(uuid.uuid4()),
            'width': width, 'height': height, 'uv_width': 16, 'uv_height': 16,
            'source': 'data:image/png;base64,' + base64.b64encode(data).decode(),
        }
        meta = file + '.mcmeta'
        if os.path.exists(meta):
            animation = json.load(open(meta, encoding='utf-8')).get('animation', {})
            texture['frame_time'] = animation.get('frametime', 1)
            texture['frame_interpolate'] = animation.get('interpolate', False)
        self.by_location[location] = len(self.textures)
        self.textures.append(texture)
        return self.by_location[location]

    def cube(self, name, f, t, faces, origin=(8, 8, 8), light=0):
        element = {
            'name': name, 'box_uv': False, 'rescale': False, 'locked': False, 'light_emission': light,
            'render_order': 'default', 'allow_mirror_modeling': True,
            'from': f, 'to': t, 'autouv': 0, 'color': len(self.elements) % 8, 'origin': list(origin),
            'faces': faces, 'type': 'cube', 'uuid': str(uuid.uuid4()),
        }
        self.elements.append(element)
        return element['uuid']

    def model_cubes(self, model_name, names, origin=(8, 8, 8), lit=()):
        """Adds every box of a generated model as a cube; returns their uuids, by name."""
        model = json.load(open(os.path.join(MODELS, model_name + '.json'), encoding='utf-8'))
        textures = model['textures']

        def resolve(ref):
            while ref.startswith('#'):
                ref = textures[ref[1:]]
            return ref

        particle = resolve('#particle')
        made = []
        for i, element in enumerate(model['elements']):
            f, t = element['from'], element['to']
            faces = {}
            for side in ('north', 'east', 'south', 'west', 'up', 'down'):
                face = element['faces'].get(side)
                if face is None:
                    faces[side] = {'uv': [0, 0, 0, 0], 'texture': None}
                    continue
                key = face['texture'][1:]
                location = resolve(face['texture'])
                out = {'uv': face.get('uv', default_uv(side, f, t)),
                       'texture': self.texture(key, location, location == particle)}
                if 'cullface' in face:
                    out['cullface'] = face['cullface']
                faces[side] = out
            name = names[i] if i < len(names) and names[i] else f'{model_name} {i}'
            made.append((name, self.cube(name, f, t, faces, origin, 15 if name in lit else 0)))
        return made

    def glow(self, name, location, f, t, side):
        faces = {s: {'uv': [0, 0, 0, 0], 'texture': None} for s in ('north', 'east', 'south', 'west', 'up', 'down')}
        faces[side] = {'uv': default_uv(side, f, t), 'texture': self.texture(location.split('/')[-1], location)}
        return self.cube(name, f, t, faces, light=15)


def group(name, children, origin=(8, 8, 8), export=True, visible=True, locked=False):
    return {'name': name, 'origin': list(origin), 'color': 0, 'uuid': str(uuid.uuid4()), 'export': export,
            'mirror_uv': False, 'isOpen': name not in ('renderer glows (reference, not exported)',),
            'locked': locked, 'visibility': visible, 'autouv': 0, 'children': children}


def export(machine):
    spec = MACHINES[machine]
    project = Project(machine)
    made = dict(project.model_cubes(spec['body'], spec['order'], lit=spec['lit']))
    assert len(made) == len(spec['order']), f"{spec['body']} has {len(made)} boxes, named {len(spec['order'])}"
    outliner = []
    body = []
    for name, names in spec['groups']:
        body.append(group(name, [made[n] for n in names]))
    outliner.append(group('body', body))
    for model_name, label, pivot, names in spec['parts']:
        cubes = project.model_cubes(model_name, names, origin=pivot)
        outliner.append(group(label, [uid for _, uid in cubes], origin=pivot))
    glows = [project.glow(name, location, f, t, side) for name, location, f, t, side in spec['glows']]
    outliner.append(group('renderer glows (reference, not exported)', glows, export=False, visible=False, locked=True))
    bbmodel = {
        'meta': {'format_version': '4.10', 'model_format': 'java_block', 'box_uv': False},
        'name': machine, 'parent': 'block/block', 'ambientocclusion': True, 'front_gui_light': False,
        'visible_box': [1, 1, 0], 'variable_placeholders': '', 'variable_placeholder_buttons': [],
        'unhandled_root_fields': {}, 'resolution': {'width': 16, 'height': 16},
        'elements': project.elements, 'outliner': outliner, 'textures': project.textures,
    }
    os.makedirs(OUT, exist_ok=True)
    out = os.path.join(OUT, machine + '.bbmodel')
    with open(out, 'w', encoding='utf-8') as fh:
        json.dump(bbmodel, fh, indent=1)
    print(f'{out}: {len(project.elements)} cubes, {len(project.textures)} textures')


if __name__ == '__main__':
    for name in sys.argv[1:] or MACHINES:
        export(name)
