#version 330

in vec2 inPosition;

uniform mat4 uView;
uniform mat4 uProj;

out vec2 uv;

const float PI = 3.14;

void main() {
    // Nastavení souřadnic do textury <0; 1>
    uv = inPosition;

    // 1) Jednoduché ohnutí gridu
    vec2 pos = inPosition * 2 - 1;
    float z = 0.1 * cos(sqrt(20 * pow(pos.x, 2) + 20 * pow(pos.y, 2)));
    gl_Position = uProj * uView * vec4(pos, z, 1.0);

    // 2) Koule
//    // rozsah x <0,1> na zenit <0, PI>
//    float zenith  = inPosition.y * PI;
//    // rozsah y <0,1> na azimut <0,2PI>
//    float azimuth = inPosition.x * 2 * PI;
//    float r = 1;
//
//    float x = r * sin(zenith) * cos(azimuth);
//    float y = r * sin(zenith) * sin(azimuth);
//    float z = r * cos(zenith);
//    gl_Position = uProj * uView * vec4(x, y, z, 1.0);
}
