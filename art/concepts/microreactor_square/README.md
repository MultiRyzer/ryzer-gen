# Square microreactor preview

A 2 x 2 block footprint, two blocks tall. This is a separate visual concept.

The upright pressure vessel sits in a broad shield bowl. Four ribs carry a
bolted lid and central fuel-loading hatch. Copper cooling cassettes sit on
either side, with a low console and a small protected inspection window in
front. Pale casing, graphite structure and cyan status lighting follow the
machine family.

The physical basis is a sealed pressure vessel with external heat exchangers.
The inspection window is a gameplay visualisation, not a literal reactor design.

- `front.png`: front three-quarter view.
- `front_and_back.png`: front and rear views.
- `footprint.png`: raised view showing the four deck plates.
- `square.json`: exported quad geometry.
- `textures/`: concept textures.
- `layout.json`: dimensions and integration note.

Regenerate with `python art/tools/microreactor_square_concept.py`.
The script checks geometry bounds, normals and texture references.

This concept is not installed or tested in game. The existing microreactor
formation is narrower. Integration would require a new formation layout,
port mapping, renderer alignment and animation handling. Rear input sockets
are on the player's left when facing the front, outputs on the right;
energy has its own side socket.
