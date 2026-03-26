#version 330

in vec3 color;

uniform vec3 uColor;

out vec4 outColor;

void main() {
    outColor = vec4(uColor, 1);
}
