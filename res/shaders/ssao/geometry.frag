#version 330

in vec3 vViewPos;
in vec3 vViewNormal;
in vec2 vUV;

uniform sampler2D uTexture;

layout (location = 0) out vec3 gPosition;
layout (location = 1) out vec3 gNormal;
layout (location = 2) out vec4 gAlbedo;

void main() {
    gPosition = vViewPos;
    gNormal   = normalize(vViewNormal);
    gAlbedo   = texture(uTexture, vUV);
}
