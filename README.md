# ClipMappingTerrainWebGPU

Port of geometric clip mapping terrain demo to gdx-webgpu.
(see also https://github.com/MonstrousSoftware/ClipMappingTerrain).

The terrain shader is derived from the WgDefaultShader.
It adds a few uniforms (amplitude, scale, heightMapSize) by overriding `defineUniforms()` and `setUniforms()`.

The height maps needs to be sampled in the vertex shader instead of the fragment shader, so the usual material 
textures can't be used (these have visibility set to only the fragment shader)

However, you cannot sample a texture in the vertex shader:

    [Error Message: ] Error while parsing WGSL: :108:19 error: built-in cannot be used by vertex pipeline stage
    let hh:vec4f = textureSample(heightMapTexture, heightMapSampler, uv);

To read from a texture in the vertex shader we need to use a texture_storage_2d and use textureLoad instead of textureSample.

https://www.youtube.com/watch?v=580xlsQCVL4

This also means we need to create the height map texture including the usage as StorageTexture (WGPUTextureUsage.StorageBinding).

Not all pixel formats can be used for a storage texture, e.g. you can't use R8Uint to have one byte per pixel.

Currently, gdx-webgpu doesn't support grey scale textures read from a file (Pixmap format Alpha).
It only supports RGBA8 formats.
Therefore, the height map is stored as a regular storage buffer instead.

Most of the code is a straightforward port of the libgdx version.  One thing that was implemented
differently is to use a StorageBuffer for the height map instead of a grey scale texture. 

Unfortunately performance is lower on the webgpu version: 100 FPS (using WGPU backend) instead of 500 FPS on the original version (vanilla libgdx).
Dawn backend is slower still (30 FPS).

A typical scene is rendered with 51 renderables (frustum culled from 140 renderables) in 7 draw calls 
(for the 7 different mesh types: MxM, 3xM, Mx3, 3x3, horizTrim, vertTrim, fringe) and no shader switches
(considering only the terrain render). So that seems pretty much optimal; model instances from the same model are
rendered in a single draw call (i.e. instancing).


TIP: To force a refresh of the gdx-webgpu library snapshot (i.e. if you want to force the use of a newer snapshot, but 
gradle keeps using an older, cached version):
 ./gradlew build --refresh-dependencies
