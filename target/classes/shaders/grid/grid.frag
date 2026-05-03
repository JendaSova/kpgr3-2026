#version 330

// ─── Vstupy z VS ──────────────────────────────────────────────────────────────
in vec2  vUV;
in vec3  vNormal;
in vec3  vWorldPos;

// ─── Uniform proměnné ─────────────────────────────────────────────────────────
uniform sampler2D uTexture;

uniform int  uFragMode;    // 0=textuta+osvětlení, 1=normála, 2=pozice, 3=hloubka, 4=UV, 5=difúzní

// Osvětlení
uniform vec3 uLightPos;
uniform vec3 uEyePos;
uniform int  uAmbient;
uniform int  uDiffuse;
uniform int  uSpecular;
uniform int  uSpot;

// ─── Výstup ───────────────────────────────────────────────────────────────────
out vec4 outColor;

// ═══════════════════════════════════════════════════════════════════════════════
//  BLINN-PHONG osvětlovací model
// ═══════════════════════════════════════════════════════════════════════════════
vec3 blinnPhong(vec3 baseColor) {
    vec3 N = normalize(vNormal);
    vec3 L = normalize(uLightPos - vWorldPos);
    vec3 V = normalize(uEyePos   - vWorldPos);
    vec3 H = normalize(L + V);                 // half-vektor (Blinn)

    // ── Ambientní ────────────────────────────────────────────────────────────
    vec3 ambient = vec3(0.0);
    if (uAmbient == 1) {
        ambient = 0.5 * baseColor;
    }

    // ── Vzdálenost a útlum prostředí ─────────────────────────────────────────
    float dist = length(uLightPos - vWorldPos);
    float kC = 1.0, kL = 0.09, kQ = 0.032;
    float attenuation = 1.0 / (kC + kL * dist + kQ * dist * dist);

    // ── Reflektorový efekt (spot light) ──────────────────────────────────────
    vec3  spotDir   = normalize(vec3(0.0, 0.0, -1.0)); // směr reflektoru dolů
    float spotAngle = 30.0;                             // polovina kužele ve stupních
    float spotOuter = 35.0;
    float cosTheta  = dot(-L, spotDir);
    float cosCutoff = cos(radians(spotAngle));
    float cosOuter  = cos(radians(spotOuter));
    float spotFactor = 1.0;
    if (uSpot == 1) {
        // Hladký přechod na okraji reflektoru
        spotFactor = clamp((cosTheta - cosOuter) / (cosCutoff - cosOuter), 0.0, 1.0);
    }

    // ── Difúzní složka ───────────────────────────────────────────────────────
    vec3 diffuse = vec3(0.0);
    float NdotL = max(dot(N, L), 0.0);
    if (uDiffuse == 1) {
        diffuse = NdotL * baseColor * attenuation * spotFactor;
    }

    // ── Zrcadlová složka ─────────────────────────────────────────────────────
    vec3 specular = vec3(0.0);
    if (uSpecular == 1 && NdotL > 0.0) {
        float NdotH = max(dot(N, H), 0.0);
        float shininess = 64.0;
        specular = pow(NdotH, shininess) * vec3(1.0) * attenuation * spotFactor;
    }

    return ambient + diffuse + specular;
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MAIN
// ═══════════════════════════════════════════════════════════════════════════════
void main() {
    vec3 N = normalize(vNormal);

    // ── DEBUG MÓDY ────────────────────────────────────────────────────────────
    if (uFragMode == 1) {
        // Normála jako barva
        outColor = vec4(N * 0.5 + 0.5, 1.0);
        return;
    }
    if (uFragMode == 2) {
        // Pozice jako barva (remapovaná na <0,1>)
        outColor = vec4(fract(vWorldPos * 0.5 + 0.5), 1.0);
        return;
    }
    if (uFragMode == 3) {
        // Hloubka Z (depth)
        float depth = gl_FragCoord.z;
        outColor = vec4(depth, depth, depth, 1.0);
        return;
    }
    if (uFragMode == 4) {
        // UV souřadnice
        outColor = vec4(vUV, 0.0, 1.0);
        return;
    }
    if (uFragMode == 5) {
        // Difúzní složka (bez textury)
        vec3 L = normalize(uLightPos - vWorldPos);
        float NdotL = max(dot(N, L), 0.0);
        outColor = vec4(vec3(NdotL), 1.0);
        return;
    }

    // ── PLNÉ OSVĚTLENÍ + TEXTURA (uFragMode == 0) ─────────────────────────────
    vec4 texColor = texture(uTexture, vUV);
    vec3 lit = blinnPhong(texColor.rgb);
    outColor = vec4(lit, texColor.a);
}
