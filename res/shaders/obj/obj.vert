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

void main() {
    vec4 worldPos4 = uModel * vec4(inPosition, 1.0);
    vWorldPos = worldPos4.xyz;
    vUV = inTexCoord;

    mat3 normalMat = transpose(inverse(mat3(uModel)));
    vNormal = normalize(normalMat * inNormal);

    gl_Position = uProj * uView * worldPos4;
}
