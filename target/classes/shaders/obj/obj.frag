#version 330

in vec2  vUV;
in vec3  vNormal;
in vec3  vWorldPos;

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

    if (uFragMode == 1) { outColor = vec4(N * 0.5 + 0.5, 1.0); return; }
    if (uFragMode == 2) { outColor = vec4(fract(vWorldPos * 0.5 + 0.5), 1.0); return; }
    if (uFragMode == 3) { float d = gl_FragCoord.z; outColor = vec4(d,d,d,1.0); return; }
    if (uFragMode == 4) { outColor = vec4(vUV, 0.0, 1.0); return; }

    vec4 texColor = texture(uTexture, vUV);
    float NdotL = max(dot(N, L), 0.0);
    float NdotH = max(dot(N, H), 0.0);

    vec3 ambient  = 0.15 * texColor.rgb;
    vec3 diffuse  = NdotL * texColor.rgb;
    vec3 specular = pow(NdotH, 32.0) * vec3(1.0);

    outColor = vec4(ambient + diffuse + specular, 1.0);
}
