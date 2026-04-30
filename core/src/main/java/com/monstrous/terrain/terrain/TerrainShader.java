package com.monstrous.terrain.terrain;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.github.xpenatan.webgpu.*;
import com.monstrous.gdx.webgpu.graphics.g3d.WgModelBatch;
import com.monstrous.gdx.webgpu.graphics.g3d.shaders.WgDefaultShader;
import com.monstrous.gdx.webgpu.wrappers.WebGPUBindGroupLayout;
import com.monstrous.gdx.webgpu.wrappers.WebGPUBuffer;
import com.monstrous.gdx.webgpu.wrappers.WebGPURenderPass;



/** By creating a dedicated TerrainShader class we can add some relevant uniforms */
public class TerrainShader extends WgDefaultShader {
    // terrain parameters
    private int heightMapSize;  // dimension of height map in vertices per side
    private float scale; // horizontal scale of one height map texel
    private float amplitude; // height multiplication factor
    private HeightMap heightMap;


    /** Simple constructor that uses some default settings. Use setXXX() to
     * set terrain parameters.
     */
//    public TerrainShader(Renderable renderable) {
//        this(renderable, 2048, 64, 25600);
//    }

    /** Constructor that includes the terrain parameters.
     * Terrain parameters can be changed at every frame via the setXXX() methods
     * */
    public TerrainShader(Renderable renderable, HeightMap map, float scale, float amplitude) {
        //super(renderable);

        super(renderable, new WgModelBatch.Config( Gdx.files.internal("shaders/terrain.wgsl").readString()));
        this.heightMap = map;
        setHeightMapSize(map.getSize());
        setScale(scale);
        setAmplitude(amplitude);
        bindHeightMap(heightMap.getBuffer());
    }

    private WebGPUBindGroupLayout createBindGroupLayout() {
        WebGPUBindGroupLayout layout = new WebGPUBindGroupLayout("TerrainShader Binding Group Layout");
        layout.begin();
        // what to put for minBindingSize?
        layout.addBuffer(1, WGPUShaderStage.Vertex, WGPUBufferBindingType.ReadOnlyStorage, 1024, false);
        layout.end();
        return layout;
    }

    private void bindHeightMap(WebGPUBuffer buffer){
        binder.setBuffer("heightMap", buffer, 0, buffer.getSize());
    }

    @Override
    protected void defineUniforms() {
        super.defineUniforms();
        defineUniform("heightMapSize", 4);
        defineUniform("scale", 4);
        defineUniform("amplitude", 4);
       //
        // we need to add to group 3 because we can use max 4 groups per pipeline at the same time (max_bind_groups)
        binder.defineGroup(3, createBindGroupLayout());
        binder.defineBinding("heightMap", 3, 1);
    }

    public void setHeightMapSize(int verticesPerSide){
        heightMapSize = verticesPerSide;
    }

    public void setScale(float horizontalScale){
        scale = horizontalScale;
    }

    public float getScale() {
        return scale;
    }

    public void setAmplitude(float amplitude){
        this.amplitude = amplitude;
    }

    public float getAmplitude() {
        return amplitude;
    }

    // assumes the shader is only ever called for terrain renderables
    // switch shader on change in primitive type (triangles vs/ lines)
    @Override
    public boolean canRender(Renderable renderable) {
        if (renderable.meshPart.primitiveType != primitiveType)
            return false;

        return true;
    }

    @Override
    public void begin(Camera camera, Renderable renderable, WebGPURenderPass renderPass) {
        //
        super.begin(camera, renderable, renderPass);
        this.binder.bindGroup(renderPass, 3);

    }

    @Override
    public void setUniforms(Camera camera, Renderable renderable) {
        super.setUniforms(camera, renderable);
        // set uniforms
        super.binder.setUniform("heightMapSize", heightMapSize);
        super.binder.setUniform("scale", scale);
        super.binder.setUniform("amplitude", amplitude);

        //bindHeightMap(heightMap.getBuffer());
    }
}
