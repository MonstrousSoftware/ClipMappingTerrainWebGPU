package com.monstrous.terrain.terrain;



import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.monstrous.gdx.webgpu.graphics.WgTexture;
import com.monstrous.gdx.webgpu.wrappers.WebGPUBuffer;

// BROKEN?

public class HeightMapGenerated implements HeightMap, Disposable {
    final int PERLIN_GRID_SIZE = 16;

    public int mapSize;
    private float[][] heightMap;
    private Noise noise;
    private Texture heightMapTexture;
    private Pixmap pixmap;


    /** Create height map using Perlin noise */
    public HeightMapGenerated(int mapSize) {
        this.mapSize = mapSize;
        noise = new Noise();
        // generate a noise map
        heightMap = noise.generateSmoothedPerlinMap(mapSize, mapSize, 0,0, PERLIN_GRID_SIZE);
    }

    public WebGPUBuffer getBuffer(){
        return null;
    }

    @Override
    public int getSize(){
        return mapSize;
    }

    public Texture getHeightMapTexture(){
        // create on demand
        if(heightMapTexture == null){
            // copy to a texture (for debug)
            pixmap = noise.generatePixmap(heightMap, mapSize);

            heightMapTexture = new WgTexture(pixmap);
            heightMapTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
            heightMapTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        return heightMapTexture;
    }

    /** get height at position (wx, wz). Coordinates must be in range [0.0 to 1.0]. */
    public float get(float wx, float wz){
        int x = Math.round(wx * mapSize);
        int z = Math.round(wz * mapSize);

        return heightMap[z][x];
    }

    @Override
    public void dispose() {
        if(heightMapTexture != null)
            heightMapTexture.dispose();
    }

}
