#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_diffuseTexture;
uniform vec4 u_fogColor;

varying vec2 v_UV;
varying float v_fog;

void main() {
    vec4 diffuse = texture2D(u_diffuseTexture, v_UV);

    gl_FragColor = diffuse;
    gl_FragColor.rgb = mix(gl_FragColor.rgb, u_fogColor.rgb, v_fog);

    //gl_FragColor.rgb = vec3(v_UV.r, v_UV.g, 0.0);
}
