package com.monstrous.terrain.terrain;


import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.github.xpenatan.webgpu.WGPUBufferUsage;
import com.github.xpenatan.webgpu.WGPUTextureFormat;
import com.github.xpenatan.webgpu.WGPUTextureUsage;
import com.monstrous.gdx.webgpu.graphics.WgTexture;
import com.monstrous.gdx.webgpu.wrappers.WebGPUBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


public class HeightMapFromFile implements HeightMap, Disposable {

    public int mapSize;
    private Texture heightMapTexture;
    private Pixmap pixmap;
    private byte[] heightData;
    private WebGPUBuffer buffer;


    /** Create height map from grey scale texture file (should be 8 bits greyscale) */
    public HeightMapFromFile(FileHandle textureFile) {
        pixmap = new Pixmap(textureFile);

        // read heights into an array
        ByteBuffer bytes = pixmap.getPixels();
        int numBytes = bytes.limit();
        heightData = new byte[numBytes];
        bytes.get(heightData);
        bytes.rewind();

        heightMapTexture = new WgTexture(pixmap); //, true);    // note useMipMaps is true by default

//        heightMapTexture = new WgTexture("heightmap", pixmap.getWidth(), pixmap.getHeight(),
//            1, false,
//            WGPUTextureUsage.TextureBinding.or(WGPUTextureUsage.CopyDst).or(WGPUTextureUsage.StorageBinding),
//            WGPUTextureFormat.RGBA8Uint);
//
//        ((WgTexture)heightMapTexture).load(pixmap.getPixels(), pixmap.getWidth(), pixmap.getHeight(), 0);
//


        heightMapTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        heightMapTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        mapSize = heightMapTexture.getWidth();  // assumes a square

        // create a GPU buffer
        // working buffer in native memory to use as input to WriteBuffer
        ByteBuffer dataBuf = BufferUtils.newUnsafeByteBuffer(numBytes * Float.BYTES);
        dataBuf.order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer floatData = dataBuf.asFloatBuffer();
        for(int i = 0; i < bytes.limit(); i++){
            int b = bytes.get(i) & 0xFF;
            floatData.put(b/255f);
        }
        dataBuf.rewind();

        buffer = new WebGPUBuffer("height map", WGPUBufferUsage.CopyDst.or(WGPUBufferUsage.Storage), numBytes * Float.BYTES);
        buffer.write(0, dataBuf, numBytes*Float.BYTES);
    }

    public WebGPUBuffer getBuffer(){
        return buffer;
    }

    public Texture getHeightMapTexture(){
        return heightMapTexture;
    }

    @Override
    public int getSize(){
        return mapSize;
    }

    /** get height at position (u, v). Coordinates must be in range [0.0 to 1.0].
     * Height will be in range [-0 .. 1], scale appropriately*/
    public float get(float u, float v){
        int x = Math.round(u * mapSize);
        int z = Math.round(v * mapSize);
        x = Math.min(x, mapSize-1); // clamp to prevent overflow
        z = Math.min(z, mapSize-1);

        int hi = heightData[z*mapSize+x] & 0xFF;    // interpret as unsigned byte
        return hi/255f;
    }

    @Override
    public void dispose() {
        if(heightMapTexture != null)
            heightMapTexture.dispose();
    }

}
