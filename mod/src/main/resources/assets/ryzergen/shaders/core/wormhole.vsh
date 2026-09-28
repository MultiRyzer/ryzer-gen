#version 150

// A full-screen pass: the quad's corners come in as 0 to 1 and double as the texture coordinates.

in vec3 Position;

out vec2 texCoord;

void main() {
    gl_Position = vec4(Position.xy * 2.0 - 1.0, 0.0, 1.0);
    texCoord = Position.xy;
}
