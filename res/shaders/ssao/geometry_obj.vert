#version 330

in vec3 inPosition;
in vec2 inTexCoord;
in vec3 inNormal;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;

out vec3 vViewPos;
out vec3 vViewNormal;
out vec2 vUV;

void main() {
    vec4 worldPos = uModel * vec4(inPosition, 1.0);
    vec4 viewPos4 = uView * worldPos;
    vViewPos = viewPos4.xyz;

    mat3 nm = transpose(inverse(mat3(uView * uModel)));
    vViewNormal = normalize(nm * inNormal);

    vUV = inTexCoord;
    gl_Position = uProj * viewPos4;
}
