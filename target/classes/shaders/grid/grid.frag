#version 330

in vec2 uv;

out vec4 outColor;

uniform sampler2D textureBricks;

void main() {
    outColor = texture(textureBricks, uv);
}
