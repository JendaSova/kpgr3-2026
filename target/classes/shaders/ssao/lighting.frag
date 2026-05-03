#version 330

in vec2 vUV;

uniform sampler2D gPosition;
uniform sampler2D gNormal;
uniform sampler2D gAlbedo;
uniform sampler2D uSSAOBlur;
uniform vec3  uLightPosView;
uniform int   uAmbient;
uniform int   uDiffuse;
uniform int   uSpecular;

out vec4 outColor;

void main() {
    vec3 fragPos = texture(gPosition, vUV).xyz;
    vec3 normal  = normalize(texture(gNormal, vUV).rgb);
    vec4 albedo  = texture(gAlbedo, vUV);
    float ao     = texture(uSSAOBlur, vUV).r;

    vec3 L = normalize(uLightPosView - fragPos);
    vec3 V = normalize(-fragPos);
    vec3 H = normalize(L + V);

    float NdotL = max(dot(normal, L), 0.0);
    float NdotH = max(dot(normal, H), 0.0);

    float dist = length(uLightPosView - fragPos);
    float att  = 1.0 / (1.0 + 0.09*dist + 0.032*dist*dist);

    vec3 result = vec3(0.0);
    if (uAmbient  == 1) result += 0.3 * albedo.rgb * ao;
    if (uDiffuse  == 1) result += NdotL * albedo.rgb * att;
    if (uSpecular == 1) result += pow(NdotH, 32.0) * vec3(0.5) * att;

    outColor = vec4(result, 1.0);
}
