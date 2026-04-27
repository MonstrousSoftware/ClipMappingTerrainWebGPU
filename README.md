# ClipMappingTerrainWebGPU

Port of geometric clip mapping terrain demo to gdx-webgpu.

To do: shader


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

gdx-webgpu doesn't support grey scale textures read from a file (Pixmap format Alpha).
Maybe we should use a regular storage buffer instead.

Note: we can use the shader function textureDimensions() instead of passing this via a uniform.


To force a refresh of the gdx-webgpu library snapshot:
 ./gradlew build --refresh-dependencies
