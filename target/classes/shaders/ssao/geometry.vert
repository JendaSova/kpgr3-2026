#version 330

in vec2 inPosition;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;
uniform int  uFunction;
uniform float uTime;

out vec3 vViewPos;
out vec3 vViewNormal;
out vec2 vUV;

const float PI = 3.14159265;

vec3 fn0(float s, float t) {
    float x = (s * 2.0 - 1.0) * 1.5;
    float y = (t * 2.0 - 1.0) * 1.5;
    float r = sqrt(x*x + y*y);
    float z = 0.3 * cos(3.0 * r + uTime);
    return vec3(x, y, z);
}
vec3 fn1(float s, float t) {
    float x = (s * 2.0 - 1.0) * 1.5;
    float y = (t * 2.0 - 1.0) * 1.5;
    return vec3(x, y, 0.4*(x*x - y*y));
}
vec3 fn2(float s, float t) {
    float zen = t * PI;
    float azi = s * 2.0 * PI;
    return vec3(sin(zen)*cos(azi), sin(zen)*sin(azi), cos(zen));
}
vec3 fn3(float s, float t) {
    float u = s * 2.0 * PI;
    float v = t * 2.0 * PI;
    return vec3((0.8+0.3*cos(v))*cos(u), (0.8+0.3*cos(v))*sin(u), 0.3*sin(v));
}
vec3 fn4(float s, float t) {
    float phi = s * 2.0 * PI;
    float h = (t * 2.0 - 1.0);
    return vec3(0.8*cos(phi), 0.8*sin(phi), h);
}
vec3 fn5(float s, float t) {
    float phi = s * 4.0 * PI;
    float h = (t * 2.0 - 1.0);
    float r = 0.3 + 0.5 * t;
    return vec3(r*cos(phi), r*sin(phi), h);
}

vec3 evalF(float s, float t) {
    if (uFunction == 0) return fn0(s, t);
    if (uFunction == 1) return fn1(s, t);
    if (uFunction == 2) return fn2(s, t);
    if (uFunction == 3) return fn3(s, t);
    if (uFunction == 4) return fn4(s, t);
    return fn5(s, t);
}

vec3 calcNormal(float s, float t) {
    float e = 0.001;
    vec3 ds = evalF(s+e, t) - evalF(s-e, t);
    vec3 dt = evalF(s, t+e) - evalF(s, t-e);
    return normalize(cross(ds, dt));
}

void main() {
    float s = inPosition.x;
    float t = inPosition.y;
    vec3 pos = evalF(s, t);

    vec4 worldPos = uModel * vec4(pos, 1.0);
    vec4 viewPos4 = uView * worldPos;
    vViewPos = viewPos4.xyz;

    mat3 nm = transpose(inverse(mat3(uView * uModel)));
    vec3 n = calcNormal(s, t);
    vViewNormal = normalize(nm * calcNormal(s, t));

    vUV = inPosition;
    gl_Position = uProj * viewPos4;
}
