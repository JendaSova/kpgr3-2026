#version 330

// ─── Vstupy z VS ──────────────────────────────────────────────────────────────
in vec2  vUV;
in vec3  vNormal;
in vec3  vWorldPos;
in vec3  vViewPos;
in vec3  vViewNormal;

// ─── Uniform proměnné ─────────────────────────────────────────────────────────
uniform sampler2D uTexture;

// uFragMode: 0=osvetleni+textura, 1=normala(view), 2=pozice(view), 3=hloubka,
//             4=UV, 5=osvetleni bez textury, 6=vzdalenost od svetla, 7=textura RGBA
uniform int  uFragMode;

// Osvětlení
uniform vec3 uLightPos;
uniform vec3 uEyePos;
uniform int  uAmbient;
uniform int  uDiffuse;
uniform int  uSpecular;
uniform int  uSpot;
uniform vec3 uSpotDir;    // směr reflektoru (normalizovaný)
uniform float uSpotAngle; // polovina kužele ve stupních

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
    vec3  spotDir   = normalize(uSpotDir);   // směr z CPU (ovládaný klávesami)
    float spotOuter = uSpotAngle + 5.0;      // vnější kužel o 5° větší
    float cosTheta  = dot(-L, spotDir);
    float cosCutoff = cos(radians(uSpotAngle));
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

    // 1) Normála v soustavě pozorovatele (view space)
    if (uFragMode == 1) {
        outColor = vec4(normalize(vViewNormal) * 0.5 + 0.5, 1.0);
        return;
    }
    // 2) Pozice v souřadnicích pozorovatele (view space)
    if (uFragMode == 2) {
        outColor = vec4(fract(abs(vViewPos) * 0.2), 1.0);
        return;
    }
    // 3) Hloubka z depth bufferu
    if (uFragMode == 3) {
        float depth = gl_FragCoord.z;
        outColor = vec4(depth, depth, depth, 1.0);
        return;
    }
    // 4) UV souřadnice
    if (uFragMode == 4) {
        outColor = vec4(vUV, 0.0, 1.0);
        return;
    }
    // 5) Osvětlení bez textury (neutrální šedá barva)
    if (uFragMode == 5) {
        vec3 baseColor = vec3(0.7, 0.7, 0.7);
        vec3 lit = blinnPhong(baseColor);
        outColor = vec4(lit, 1.0);
        return;
    }
    // 6) Vzdálenost od zdroje světla (zelena=blizko, cervena=daleko)
    if (uFragMode == 6) {
        float dist = length(uLightPos - vWorldPos);
        float norm = clamp(dist / 10.0, 0.0, 1.0);
        outColor = vec4(norm, 1.0 - norm, 0.0, 1.0);
        return;
    }
    // 7) Textura RGBA bez osvětlení
    if (uFragMode == 7) {
        outColor = texture(uTexture, vUV);
        return;
    }

    // 0) Kompletní osvětlení s texturou (výchozí)
    vec4 texColor = texture(uTexture, vUV);
    vec3 lit = blinnPhong(texColor.rgb);
    outColor = vec4(lit, texColor.a);
}
