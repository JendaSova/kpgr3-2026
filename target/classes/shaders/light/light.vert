#version 330
in vec2 inPosition;
uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;

void main() {
    // Malý čtverec/koule jako značka světla
    vec2 pos = inPosition * 2.0 - 1.0;
    gl_Position = uProj * uView * uModel * vec4(pos.x, pos.y, 0.0, 1.0);
}
