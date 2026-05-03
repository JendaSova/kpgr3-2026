#version 330

in vec2 vUV;

uniform sampler2D gPosition;
uniform sampler2D gNormal;
uniform sampler2D uNoiseTex;
uniform vec3 uSamples[64];
uniform mat4 uProj;
uniform vec2 uNoiseScale;

out float outOcclusion;

const int   KERNEL_SIZE = 64;
const float RADIUS      = 0.5;
const float BIAS        = 0.025;

void main() {
    vec3 fragPos = texture(gPosition, vUV).xyz;

    // Prázdný pixel - žádná geometrie
    if (fragPos.z == 0.0) { outOcclusion = 1.0; return; }

    vec3 normal  = normalize(texture(gNormal, vUV).rgb);
    vec3 randVec = normalize(texture(uNoiseTex, vUV * uNoiseScale).xyz);

    vec3 tangent   = normalize(randVec - normal * dot(randVec, normal));
    vec3 bitangent = cross(normal, tangent);
    mat3 TBN       = mat3(tangent, bitangent, normal);

    float occlusion = 0.0;
    for (int i = 0; i < KERNEL_SIZE; i++) {
        vec3 samplePos = TBN * uSamples[i];
        samplePos = fragPos + samplePos * RADIUS;

        vec4 offset = uProj * vec4(samplePos, 1.0);
        offset.xyz /= offset.w;
        offset.xyz  = offset.xyz * 0.5 + 0.5;

        float sampleDepth = texture(gPosition, offset.xy).z;
        if (sampleDepth == 0.0) continue;
        float rangeCheck  = smoothstep(0.0, 1.0, RADIUS / abs(fragPos.z - sampleDepth));
        occlusion += (sampleDepth >= samplePos.z + BIAS ? 1.0 : 0.0) * rangeCheck;
    }

    outOcclusion = 1.0 - (occlusion / float(KERNEL_SIZE));
}
