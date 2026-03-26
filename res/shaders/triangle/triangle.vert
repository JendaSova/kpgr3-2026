#version 330

in vec2 inPosition;
in vec3 inColor;

out vec3 color;

void main() {
    color = inColor;

    gl_Position = vec4(inPosition, 0, 1.0);
}
