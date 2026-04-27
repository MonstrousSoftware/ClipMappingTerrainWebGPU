package com.monstrous.terrain.terrain;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.monstrous.gdx.webgpu.wrappers.WebGPUBuffer;

public interface HeightMap extends Disposable {

    /** returns a 2d texture with the height in the alpha channel. */
    public Texture getHeightMapTexture();

    /** get height at position (wx, wz). Coordinates must be in range [0.0 to 1.0]. */
    public float get(float wx, float wz);

    public int getSize();

    public WebGPUBuffer getBuffer();


}
