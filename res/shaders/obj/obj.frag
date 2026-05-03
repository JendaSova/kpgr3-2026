#version 330

in vec2  vUV;
in vec3  vNormal;
in vec3  vWorldPos;
in vec3  vViewPos;
in vec3  vViewNormal;

uniform sampler2D uTexture;
uniform vec3 uLightPos;
uniform vec3 uEyePos;
uniform int  uFragMode;

out vec4 outColor;

void main() {
    vec3 N = normalize(vNormal);
    vec3 L = normalize(uLightPos - vWorldPos);
    vec3 V = normalize(uEyePos   - vWorldPos);
    vec3 H = normalize(L + V);
    float NdotL = max(dot(N, L), 0.0);
    float NdotH = max(dot(N, H), 0.0);

    // 1) Normála ve view space
    if (uFragMode == 1) { outColor = vec4(normalize(vViewNormal) * 0.5 + 0.5, 1.0); return; }
    // 2) Pozice ve view space
    if (uFragMode == 2) { outColor = vec4(fract(abs(vViewPos) * 0.2), 1.0); return; }
    // 3) Hloubka
    if (uFragMode == 3) { float d = gl_FragCoord.z; outColor = vec4(d,d,d,1.0); return; }
    // 4) UV
    if (uFragMode == 4) { outColor = vec4(vUV, 0.0, 1.0); return; }
    // 5) Osvětlení bez textury
    if (uFragMode == 5) {
        vec3 base = vec3(0.7, 0.7, 0.7);
        vec3 lit = 0.4*base + NdotL*base + pow(NdotH,32.0)*vec3(1.0);
        outColor = vec4(lit, 1.0); return;
    }
    // 6) Vzdálenost od světla
    if (uFragMode == 6) {
        float dist = length(uLightPos - vWorldPos);
        float norm = clamp(dist / 10.0, 0.0, 1.0);
        outColor = vec4(norm, 1.0 - norm, 0.0, 1.0); return;
    }
    // 7) Textura RGBA bez osvětlení
    if (uFragMode == 7) { outColor = texture(uTexture, vUV); return; }

    // 0) Kompletní osvětlení s texturou
    vec4 texColor = texture(uTexture, vUV);
    vec3 ambient  = 0.4 * texColor.rgb;
    vec3 diffuse  = NdotL * texColor.rgb;
    vec3 specular = pow(NdotH, 32.0) * vec3(1.0);
    outColor = vec4(ambient + diffuse + specular, 1.0);
}
