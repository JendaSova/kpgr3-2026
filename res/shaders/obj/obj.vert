#version 330

in vec3 inPosition;
in vec2 inTexCoord;
in vec3 inNormal;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;

out vec2  vUV;
out vec3  vNormal;
out vec3  vWorldPos;
out vec3  vViewPos;
out vec3  vViewNormal;

void main() {
    vec4 worldPos4 = uModel * vec4(inPosition, 1.0);
    vWorldPos = worldPos4.xyz;
    vUV = inTexCoord;

    mat3 normalMat = transpose(inverse(mat3(uModel)));
    vNormal = normalize(normalMat * inNormal);

    vec4 viewPos4 = uView * worldPos4;
    vViewPos = viewPos4.xyz;
    mat3 viewNormalMat = transpose(inverse(mat3(uView * uModel)));
    vViewNormal = normalize(viewNormalMat * inNormal);

    gl_Position = uProj * uView * worldPos4;
}
