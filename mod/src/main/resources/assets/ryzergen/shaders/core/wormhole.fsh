#version 150

// The wormhole where the sun was (client/WormholeRenderer). A full-screen pass over a copy of the
// finished frame: only sky pixels near the wormhole change, so anything in front of it (terrain,
// clouds, the player) draws as it was.
//
// Units: r is the distance from the wormhole's middle in throats (the throat's edge is r = 1).
//   Outside, gravitational lensing: each pixel shows the sky from where a point mass would bend
//   its light from. Light passing inside the Einstein radius comes round from the far side, which
//   draws the sky behind into a ring. Real physics; only the numbers are chosen for the look.
//   Inside, the far mouth: deep space with a new star, bulged like the view through a sphere.
//   At the throat's edge, the photon ring: light that orbits before escaping, a thin bright line.

uniform sampler2D Scene;
uniform sampler2D SceneDepth;

uniform vec2 Centre;   // the middle, in screen coordinates (0 to 1)
uniform float Radius;  // the throat's radius, in screen heights
uniform float Aspect;  // screen width over height
uniform float Spin;    // the far side's slow turn, radians
uniform float Time;    // seconds, for the stars' twinkle and the nebula's drift

in vec2 texCoord;

out vec4 fragColor;

const float EINSTEIN = 1.45;  // the Einstein radius, in throats
const float REACH = 4.0;      // where the lensing has faded out, in throats

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float sum = 0.0, amp = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += amp * noise(p);
        p *= 2.03;
        amp *= 0.5;
    }
    return sum;
}

vec2 turn(vec2 p, float a) {
    float c = cos(a), s = sin(a);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

// One layer of stars: a few cells in a grid hold a star, twinkling.
vec3 stars(vec2 q, float scale, float chance, float size) {
    vec2 g = q * scale;
    vec2 cell = floor(g);
    float h = hash(cell);
    if (h < chance) {
        return vec3(0.0);
    }
    vec2 at = cell + vec2(hash(cell + 1.7), hash(cell + 3.1)) * 0.8 + 0.1;
    float glow = 1.0 - smoothstep(0.0, size, length(g - at));
    float twinkle = 0.6 + 0.4 * sin(Time * 2.5 + h * 60.0);
    vec3 tint = mix(vec3(0.75, 0.85, 1.0), vec3(1.0, 0.9, 0.75), hash(cell + 5.3));
    return tint * glow * twinkle;
}

// The far side: deep space, a faint nebula and the new star, for a point p in the throat (|p| < 1).
vec3 farSide(vec2 p) {
    float l = length(p);
    // Bulged like the view through a sphere: more of the far sky is squeezed in towards the rim.
    vec2 q = turn(p * (1.0 + 1.6 * l * l * l), Spin);
    vec3 col = vec3(0.008, 0.01, 0.025);
    float n = fbm(q * 1.5 + vec2(Time * 0.008, 0.0));
    col += vec3(0.28, 0.1, 0.38) * pow(n, 2.6);
    col += vec3(0.05, 0.16, 0.32) * pow(fbm(q * 2.2 + 7.0), 3.0);
    col += stars(q, 34.0, 0.9, 0.16) + stars(q, 13.0, 0.95, 0.12) * 1.4;
    // The new star: a white-hot disc with a warm corona, off to one side.
    float ds = length(q - vec2(0.24, 0.14));
    col += vec3(1.0, 0.97, 0.9) * (1.0 - smoothstep(0.13, 0.15, ds));
    col += vec3(1.0, 0.72, 0.42) * exp(-ds * 9.0) * 0.9 + vec3(1.0, 0.85, 0.7) * exp(-ds * 3.0) * 0.12;
    // Darker towards the rim, where the view grazes the throat.
    return col * mix(0.35, 1.0, 1.0 - smoothstep(0.75, 1.0, l));
}

// Outside the throat: the sky, lensed. Only what lies behind the wormhole can be bent round it: the
// sky, and terrain far enough off to be at the horizon. Anything nearer (a build, a hill) is in
// front of it, so where the bent light would come from one of those, this pixel keeps its own sky.
// Depth here is the frame's raw depth, about 1 - 0.05 / distance: 0.999 is some 50 blocks off,
// 0.9997 some 170.
vec3 lensed(vec2 d, float r, vec3 own) {
    float fade = 1.0 - smoothstep(REACH * 0.55, REACH, r);
    float beta = r - EINSTEIN * EINSTEIN / r * fade;
    vec2 from = clamp(Centre + normalize(d) * beta * Radius / vec2(Aspect, 1.0), vec2(0.001), vec2(0.999));
    float behind = smoothstep(0.999, 0.9997, texture(SceneDepth, from).r);
    vec3 col = mix(own, texture(Scene, from).rgb, behind);
    // A little of the new star's light spills out round the mouth.
    col += vec3(1.0, 0.85, 0.62) * 0.22 * exp(-(r - 1.0) * 2.5) * fade;
    return col;
}

void main() {
    vec3 scene = texture(Scene, texCoord).rgb;
    vec2 d = (texCoord - Centre) * vec2(Aspect, 1.0);
    float r = length(d) / max(Radius, 1e-5);
    bool sky = texture(SceneDepth, texCoord).r >= 0.99999;
    if (!sky || Radius < 1e-4 || r > REACH) {
        fragColor = vec4(scene, 1.0);
        return;
    }
    vec3 col;
    if (r < 0.97) {
        col = farSide(d / Radius);
    } else if (r > 1.0) {
        col = lensed(d, r, scene);
    } else {
        col = mix(farSide(d / Radius), lensed(d, 1.0, scene), smoothstep(0.97, 1.0, r));
    }
    // The photon ring, a hair outside the throat.
    col += vec3(0.75, 0.87, 1.0) * 1.3 * exp(-pow((r - 1.02) / 0.035, 2.0));
    fragColor = vec4(col, 1.0);
}
