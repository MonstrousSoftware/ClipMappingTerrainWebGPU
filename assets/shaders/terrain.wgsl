// Terrain shader based on the standard ModelBatch shader
// Copyright 2026 Monstrous Software.
// Licensed under the Apache License, Version 2.0 (the "License");

// Note this is an uber shader with conditional compilation depending on #define values from the shader prefix

// removed skinning & morphing

struct DirectionalLight {
    color: vec4f,
    direction: vec4f
}

struct PointLight {
    color: vec4f,
    position: vec4f,
    intensity: f32
}

struct FrameUniforms {
    projectionViewTransform: mat4x4f,
#ifdef CSM_SHADOW_MAP
    // N cascade projection-view transforms followed by cascade split thresholds
    shadowProjViewTransforms: array<mat4x4f, MAX_CASCADES>,
    cascadeSplits: vec4f,
    // Per-cascade shadow bias — scaled by each cascade's depth range so the
    // world-space bias is constant regardless of how much depth range a cascade covers.
    cascadeBiases: vec4f,
    // Projection-view of the CSM driver camera (may differ from the rendering camera
    // when an observer/debug camera is active). Used for cascade selection only.
    csmCameraProjectionView: mat4x4f,
#else
    shadowProjViewTransform: mat4x4f,
#endif
#ifdef MAX_DIR_LIGHTS
    directionalLights : array<DirectionalLight, MAX_DIR_LIGHTS>,
#endif
#ifdef MAX_POINT_LIGHTS
    pointLights : array<PointLight, MAX_POINT_LIGHTS>,
#endif
    ambientLight: vec4f,
    cameraPosition: vec4f,
    fogColor: vec4f,
    numDirectionalLights: f32,
    numPointLights: f32,
    shadowPcfOffset: f32,
    shadowBias: f32,
    normalMapStrength: f32,
    numRoughnessLevels: f32,
#ifdef CSM_SHADOW_MAP
    shadowPcfRadius: f32,       // PCF kernel half-size: 0=1×1, 1=3×3, 2=5×5, 3=7×7
    shadowFilterMode: f32,      // 0 = grid PCF, 1 = rotated Poisson disk PCF
    cascadeBlendFraction: f32,  // fraction of cascade range used for blending (0 = off, e.g. 0.1 = 10%)
#endif
    // specific to terrain shader
    heightMapSize : f32,
    scale : f32,
    amplitude : f32,
};

struct ModelUniforms {
    modelMatrix: mat4x4f,
    normalMatrix: mat4x4f,
    morphWeights: vec4f,
    morphWeights2: vec4f,
};

struct MaterialUniforms {
    diffuseColor: vec4f,
    shininess: f32,
    roughnessFactor: f32,
    metallicFactor: f32,
};

// frame bindings
@group(0) @binding(0) var<uniform> uFrame: FrameUniforms;
#ifdef SHADOW_MAP
    @group(0) @binding(1) var shadowMap: texture_depth_2d;
    @group(0) @binding(2) var shadowSampler: sampler_comparison;
#endif
#ifdef CSM_SHADOW_MAP
    @group(0) @binding(1) var csmShadowMap: texture_depth_2d_array;
    @group(0) @binding(2) var csmShadowSampler: sampler_comparison;
#endif
#ifdef ENVIRONMENT_MAP
    @group(0) @binding(3) var cubeMap:          texture_cube<f32>;
    @group(0) @binding(4) var cubeMapSampler:   sampler;
#endif
#ifdef USE_IBL
    @group(0) @binding(5) var irradianceMap:    texture_cube<f32>;
    @group(0) @binding(6) var irradianceSampler:       sampler;
    @group(0) @binding(7) var radianceMap:    texture_cube<f32>;
    @group(0) @binding(8) var radianceSampler:       sampler;
    @group(0) @binding(9) var brdfLUT:    texture_2d<f32>;
    @group(0) @binding(10) var lutSampler:       sampler;
#endif

// material bindings
@group(1) @binding(0) var<uniform> material: MaterialUniforms;
@group(1) @binding(1) var diffuseTexture: texture_2d<f32>;
@group(1) @binding(2) var diffuseSampler: sampler;
@group(1) @binding(3) var normalTexture: texture_2d<f32>;
@group(1) @binding(4) var normalSampler: sampler;
@group(1) @binding(5) var metallicRoughnessTexture: texture_2d<f32>;
@group(1) @binding(6) var metallicRoughnessSampler: sampler;
@group(1) @binding(7) var emissiveTexture: texture_2d<f32>;
@group(1) @binding(8) var emissiveSampler: sampler;

// renderables
@group(2) @binding(0) var<storage, read> instances: array<ModelUniforms>;

@group(3) @binding(1) var<storage, read> heightMap: array<f32>;

struct VertexInput {
    @location(0) position: vec3f,
#ifdef TEXTURE_COORDINATE
    @location(1) uv: vec2f,
#endif
#ifdef NORMAL
    @location(2) normal: vec3f,
#endif
#ifdef NORMAL_MAP
    @location(3) tangent: vec3f,
    @location(4) bitangent: vec3f,
#endif
#ifdef COLOR
    @location(5) color: vec4f,
#endif

};

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(1) uv: vec2f,
    @location(2) color: vec4f,
    @location(3) normal: vec3f,
    @location(4) worldPos : vec3f,
#ifdef NORMAL_MAP
    @location(5) tangent: vec3f,
    @location(6) bitangent: vec3f,
#endif
#ifdef FOG
    @location(7) fogDepth: f32,
#endif
#ifdef SHADOW_MAP
    @location(8)  shadowPos: vec3f,
#endif
};

@vertex
fn vs_main(in: VertexInput, @builtin(instance_index) instance: u32) -> VertexOutput {
   var out: VertexOutput;

   var pos = in.position;

   var normal_attr = vec3f(0,1,0);
#ifdef NORMAL
   normal_attr = in.normal;
#endif


   var worldPosition =  instances[instance].modelMatrix * vec4f(pos, 1.0);

   let terrainWorldSize:f32 = uFrame.heightMapSize * uFrame.scale;

   // find uv coordinate in height map
   // offset by 0.5 because terrain is centred on origin
   let uv:vec2f = (worldPosition.xz / terrainWorldSize) + vec2f(0.5);
   let uvi: vec2i = vec2i(uv * uFrame.heightMapSize);
   //let hh = sin(worldPosition.x / 1000.0 + cos(worldPosition.z/ 1700.0));
   let level :u32 = 0;
   let hh:f32 = heightMap[uvi.y * 2048 + uvi.x];
   //let hh:f32 = 1.0; //vec4f = textureLoad(heightMapTexture, uvi);
   let heightSample : f32 = select(0.0, hh, (uv.x >= 0.0 && uv.x < 1.0 && uv.y >= 0.0 && uv.y < 1.0));


   worldPosition.y = uFrame.amplitude * heightSample;

   out.position =   uFrame.projectionViewTransform * worldPosition;
   out.worldPos = worldPosition.xyz;

   out.uv = uv;

#ifdef COLOR
   var diffuseColor = in.color;
#else
   var diffuseColor = vec4f(1); // default white
#endif
   diffuseColor *= material.diffuseColor;
   out.color = diffuseColor;

#ifdef NORMAL
   // transform model normal to a world normal
   let normal = normalize((instances[instance].normalMatrix * vec4f(normal_attr, 0.0)).xyz);
#else
   let normal = vec3f(0,1,0);
#endif
    out.normal = normal;

#ifdef NORMAL_MAP
    out.tangent = in.tangent;
    out.bitangent = in.bitangent;
#endif

#ifdef FOG
    let flen:vec3f = uFrame.cameraPosition.xyz - worldPosition.xyz;
    let fog:f32 = dot(flen, flen) * uFrame.cameraPosition.w;
    out.fogDepth = min(fog, 1.0);
#endif


   return out;
}


@fragment
fn fs_main(in : VertexOutput) -> @location(0) vec4f {

   var color = in.color * textureSample(diffuseTexture, diffuseSampler, in.uv);

#ifdef FOG
    color = vec4f(mix(color.rgb, uFrame.fogColor.rgb, in.fogDepth), color.a);
#endif

#ifdef GAMMA_CORRECTION
    let linearColor: vec3f = pow(color.rgb, vec3f(1/2.2));
    color = vec4f(linearColor, color.a);
#endif
    return color;
};
