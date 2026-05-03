#version 330

in vec2 inPosition;
out vec2 vUV;

void main() {
    vUV = inPosition;
    gl_Position = vec4(inPosition * 2.0 - 1.0, 0.0, 1.0);
}
