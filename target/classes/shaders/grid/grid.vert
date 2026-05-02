#version 330

// ─── Vstupy ───────────────────────────────────────────────────────────────────
in vec2 inPosition;      // UV souřadnice gridu <0,1>

// ─── Uniform proměnné ─────────────────────────────────────────────────────────
uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;
uniform int  uFunction;  // 0-5: výběr parametrické funkce
uniform float uTime;     // animace (mění se v čase)

// ─── Výstupy do fragment shaderu ──────────────────────────────────────────────
out vec2  vUV;
out vec3  vNormal;
out vec3  vWorldPos;

// ═══════════════════════════════════════════════════════════════════════════════
//  PARAMETRICKÉ FUNKCE
//  Vstup:  s, t ∈ <0,1>
//  Výstup: 3D souřadnice vrcholu
// ═══════════════════════════════════════════════════════════════════════════════

const float PI = 3.14159265;

// ── 0) KARTÉZSKÁ – zvlněná plocha cos (s animací přes uTime) ─────────────────
vec3 fn_cartesian_wave(float s, float t) {
    float x = (s * 2.0 - 1.0) * 1.5;
    float y = (t * 2.0 - 1.0) * 1.5;
    float r = sqrt(x*x + y*y);
    float z = 0.3 * cos(3.0 * r + uTime);   // uTime animuje vlnu
    return vec3(x, y, z);
}

// ── 1) KARTÉZSKÁ – hyperbolické sedlo ────────────────────────────────────────
vec3 fn_cartesian_saddle(float s, float t) {
    float x = (s * 2.0 - 1.0) * 1.5;
    float y = (t * 2.0 - 1.0) * 1.5;
    float z = 0.4 * (x*x - y*y);
    return vec3(x, y, z);
}

// ── 2) SFÉRICKÁ – koule ───────────────────────────────────────────────────────
vec3 fn_sphere(float s, float t) {
    float zenith  = t * PI;
    float azimuth = s * 2.0 * PI;
    float r = 1.0;
    float x = r * sin(zenith) * cos(azimuth);
    float y = r * sin(zenith) * sin(azimuth);
    float z = r * cos(zenith);
    return vec3(x, y, z);
}

// ── 3) SFÉRICKÁ – torus ───────────────────────────────────────────────────────
vec3 fn_torus(float s, float t) {
    float u = s * 2.0 * PI;
    float v = t * 2.0 * PI;
    float R = 0.8;   // velký poloměr
    float r = 0.3;   // malý poloměr
    float x = (R + r * cos(v)) * cos(u);
    float y = (R + r * cos(v)) * sin(u);
    float z =  r * sin(v);
    return vec3(x, y, z);
}

// ── 4) CYLINDRICKÁ – válec ────────────────────────────────────────────────────
vec3 fn_cylinder(float s, float t) {
    float phi = s * 2.0 * PI;
    float h   = (t * 2.0 - 1.0) * 1.0;
    float r   = 0.8;
    float x = r * cos(phi);
    float y = r * sin(phi);
    float z = h;
    return vec3(x, y, z);
}

// ── 5) CYLINDRICKÁ – šroubovice (helikoid) ────────────────────────────────────
vec3 fn_helix(float s, float t) {
    float phi = s * 4.0 * PI;  // 2 otáčky
    float h   = (t * 2.0 - 1.0) * 1.0;
    float r   = 0.3 + 0.5 * t; // proměnný poloměr
    float x = r * cos(phi);
    float y = r * sin(phi);
    float z = h;
    return vec3(x, y, z);
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Dispatch – volá správnou funkci dle uFunction
// ═══════════════════════════════════════════════════════════════════════════════
vec3 evalFunction(float s, float t) {
    if      (uFunction == 0) return fn_cartesian_wave(s, t);
    else if (uFunction == 1) return fn_cartesian_saddle(s, t);
    else if (uFunction == 2) return fn_sphere(s, t);
    else if (uFunction == 3) return fn_torus(s, t);
    else if (uFunction == 4) return fn_cylinder(s, t);
    else                     return fn_helix(s, t);
}

// ═══════════════════════════════════════════════════════════════════════════════
//  NORMÁLA metodou konečných diferencí
// ═══════════════════════════════════════════════════════════════════════════════
vec3 computeNormal(float s, float t) {
    float eps = 0.001;
    vec3 ps = evalFunction(s + eps, t) - evalFunction(s - eps, t);
    vec3 pt = evalFunction(s, t + eps) - evalFunction(s, t - eps);
    return normalize(cross(ps, pt));
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MAIN
// ═══════════════════════════════════════════════════════════════════════════════
void main() {
    float s = inPosition.x;
    float t = inPosition.y;

    vec3 pos3 = evalFunction(s, t);

    // Světové souřadnice (s model maticí)
    vec4 worldPos4 = uModel * vec4(pos3, 1.0);
    vWorldPos = worldPos4.xyz;

    // UV pro texturu
    vUV = inPosition;

    // Normála transformovaná správně (transpose(inverse(model)))
    mat3 normalMat = transpose(inverse(mat3(uModel)));
    vNormal = normalize(normalMat * computeNormal(s, t));

    gl_Position = uProj * uView * worldPos4;
}
