"""Checks block models against the house grid, as numbers, before anyone renders them.

For each element it reports coordinates off the whole-pixel grid. A half-pixel thickness is
allowed (flush plates, such as the back connector's flange), but its outline must sit on whole
pixels. For each face it reports texture problems: UVs off whole texels, and a tiling texture
whose face starts part way into a tile, so the plate's edge or a brick course is cut.

    python art/tools/grid_check.py MODEL [MODEL ...]

MODEL is a generated model's name (alloy_smelter) or a path to a model JSON.
Exits 1 if anything is off, so it can run before a commit.
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODELS = os.path.join(ROOT, 'mod', 'src', 'generated', 'resources', 'assets', 'ryzergen', 'models', 'block')
AXES = 'xyz'


def load(name):
    path = name if name.endswith('.json') else os.path.join(MODELS, name + '.json')
    model = json.load(open(path))
    parts = list(model['children'].values()) if 'children' in model else [model]
    return [el for part in parts for el in part.get('elements', [])], model.get('textures', {})


def whole(v):
    return abs(v - round(v)) < 1e-6


def hidden(el, elements, a, v):
    """Whether the box's end at {v} on axis {a} is buried in a thin plate that covers it, as a
    connector's neck is behind its flange: an end nobody sees may sit off the grid."""
    f, t = el['from'], el['to']
    for other in elements:
        of, ot = other['from'], other['to']
        if other is el or not (of[a] - 1e-6 <= v <= ot[a] + 1e-6) or ot[a] - of[a] > 0.5 + 1e-6:
            continue
        if all(of[b] <= f[b] + 1e-6 and ot[b] >= t[b] - 1e-6 for b in range(3) if b != a):
            return True
    return False


def check(name):
    elements, textures = load(name)
    problems = []
    for i, el in enumerate(elements):
        f, t = el['from'], el['to']
        textures_used = sorted({spec['texture'].lstrip('#') for spec in el.get('faces', {}).values()})
        label = f'#{i} {"/".join(textures_used)} {f} to {t}'
        thin = [abs(t[a] - f[a]) <= 0.5 + 1e-6 and (whole(f[a]) or whole(t[a])) for a in range(3)]
        # A thin plate may sit half a pixel deep, but not off the grid across its face.
        off = [f'{AXES[a]} {", ".join(str(v) for v in (f[a], t[a]) if not whole(v) and not hidden(el, elements, a, v))}'
               for a in range(3) if not thin[a] and not all(whole(v) or hidden(el, elements, a, v) for v in (f[a], t[a]))]
        if off:
            problems.append(f'{label}: off the grid in {"; ".join(off)}')
            continue
        bad_uv = sorted(face for face, spec in el.get('faces', {}).items()
                        if spec.get('uv') and not all(whole(v) for v in spec['uv']))
        if bad_uv:
            problems.append(f'{label}: UVs off whole texels on {", ".join(bad_uv)}')
    return problems


def main():
    bad = 0
    for name in sys.argv[1:]:
        problems = check(name)
        bad += len(problems)
        print(f'{name}: {"on the grid" if not problems else str(len(problems)) + " problem(s)"}')
        for p in problems:
            print('  ' + p)
    sys.exit(1 if bad else 0)


if __name__ == '__main__':
    main()
