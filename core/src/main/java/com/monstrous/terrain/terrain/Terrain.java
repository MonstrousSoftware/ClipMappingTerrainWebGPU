package com.monstrous.terrain.terrain;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.monstrous.gdx.webgpu.graphics.WgTexture;
import com.monstrous.gdx.webgpu.graphics.g3d.WgModelBatch;
import com.monstrous.gdx.webgpu.graphics.g3d.shaders.WgDefaultShaderProvider;


public class Terrain implements Disposable {
    private final WgModelBatch terrainBatch;
    public int clipMapSize;   // should be 2^N-1, e.g. 63 or 127  = vertices per side
    public int numLevels;     // number of LOD levels
    public float tileSize;    // size of one grid tile in world units
    private boolean wireFrameMode;
    public float worldSize;   // world size of terrain, centered on the origin
    public HeightMap heightMap;
    public final Array<TerrainElement> elements = new Array<>();
    private final Array<ModelInstance> instances = new Array<>();
    private Model squareMxM;
    private Model fillerMX3;
    private Model filler3XM;
    private Model filler3X3;
    private Model horizontalTrim;
    private Model verticalTrim;
    private Model fringe;
    private final Vector3 focus = new Vector3();
    public boolean frustumCulling = true;
    private TerrainShader terrainShader;
    private float amplitude;
    private float scale;        // world scale of one height map tile (not clip map tile)

    /** Construct terrain.
     *
     * @param clipMapSize size of each LoD level's grid (in vertices). Should be power of two minus one, e.g. 63. Max is 1023.
     * @param numLevels number of LoD levels, i.e. concentric rings
     * @param tileSize size of a single tile in world units
     */
    public Terrain(int clipMapSize, int numLevels, float tileSize) {


        this.scale = 64;    // hmm...
        this.amplitude = 25600;
        this.wireFrameMode = false;

        //heightMap = new HeightMapGenerated(2048); //clipMapSize+1);
        heightMap = new HeightMapFromFile(Gdx.files.internal("terrain/everest_2048_2048_8bit.png"));


        setClipMapParameters(clipMapSize, numLevels, tileSize);

        // to create a shader we need a renderable
        // use the renderable from the first terrain element
        Renderable renderable = new Renderable();
        elements.get(0).modelInstance.getRenderable(renderable);
        renderable.environment = new Environment();     // force lighting so that fog will work


        terrainBatch = new WgModelBatch(new WgDefaultShaderProvider() {
            @Override
            protected Shader createShader(final Renderable renderable) {
                terrainShader = new TerrainShader(renderable, heightMap, scale, amplitude);
                return terrainShader;
            }
        });
    }

    public void setWireFrameMode(boolean mode){
        this.wireFrameMode = mode;
    }

    public void setClipMapParameters(int clipMapSize, int numLevels, float tileSize) {

        this.numLevels = numLevels;
        this.tileSize = tileSize;
        if(clipMapSize != this.clipMapSize) {
            this.clipMapSize = clipMapSize;
            generateBlocks();
        }
        this.worldSize = (clipMapSize-1) * tileSize * (float)Math.pow(2.0, numLevels);

        buildTerrain();
        Gdx.app.log("instances", ""+ elements.size);
    }



    public float getHeight(float worldX, float worldZ){
        float worldSize = heightMap.getSize() * scale;
        // scale [-0.5*worldSize .. 0.5*worldSize] to [0 .. 1]
        float u = (worldX / worldSize) + 0.5f;
        float v = (worldZ / worldSize) + 0.5f;
        if(u < 0 || u > 1f || v < 0 || v > 1f)
            return 0;
        return amplitude * heightMap.get(u, v);
    }

    /** set terrain amplitude, i.e. height multiplication factor */
    public void setAmplitude(float amplitude){
        this.amplitude = amplitude;
        terrainShader.setAmplitude(amplitude);
    }

    public float getAmplitude() {
        return amplitude;
    }

    public void setScale(float scale) {
        this.scale = scale;
        terrainShader.setScale(scale);
    }

    public float getScale() {
        return scale;
    }



    /** Generate terrain building block models. This can be called to change the appearance (e.g. wire frame mode).
     *
     */
    public void generateBlocks(){
        instances.clear();
        disposeBlocks();
        GridModelBuilder gridBuilder = new GridModelBuilder();
        final int N = clipMapSize;
        final int M = (N+1)/4;
        final int primitive = wireFrameMode ? GL20.GL_LINES : GL20.GL_TRIANGLES;

        Texture diffuseTexture  = new WgTexture(Gdx.files.internal("terrain/everest_2048_2048_albedo_topo.png"), true);
        diffuseTexture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        diffuseTexture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);

        Material mat = new Material(
            TextureAttribute.createDiffuse(diffuseTexture),
            TextureAttribute.createEmissive(heightMap.getHeightMapTexture())    // misuse "emissive texture" for height map
        );

        // each NxN level (the central level and surrounding ring levels)
        // is built up from building blocks: MxM blocks and fillers
        //
        // vertex positions range is [0..M][0..M]
        squareMxM = gridBuilder.makeGridModel( M, M, primitive, mat);
        // vertical filler blocks to close the ring
        filler3XM = gridBuilder.makeGridModel(3, M, primitive, mat);
        // horizontal filler blocks to close the ring
        fillerMX3 = gridBuilder.makeGridModel(  M, 3, primitive, mat);
        // central filler for the central square
        filler3X3 = gridBuilder.makeGridModel(  3, 3, primitive, mat);

        // top/bottom trim
        horizontalTrim = gridBuilder.makeGridModel(  2*M+1, 2, primitive, mat);

        // left/right trim
        verticalTrim = gridBuilder.makeGridModel( 2, 2*M, primitive, mat);

        // degenerate triangles to close gaps on border with next level
        fringe = gridBuilder.makeTriangleFringe(N, N, primitive, mat);
    }


    /** Enable frustum culling for better performance */
    public void setCulling(boolean culling){
        frustumCulling = culling;
    }




    private final Vector3 previousCameraPosition = new Vector3();

    /** update terrain to have the highest level of detail near the focal instance and perform frustum clipping */
    public void update(Camera camera){
        this.focus.set(camera.position);
        // rebuild terrain if focal point has moved
        if(instances.isEmpty() || focus.dst2(previousCameraPosition) > 0.1f)
            buildTerrain();

        // build list of visible model instances
        // (camera may be in same position but rotated)
        instances.clear();
        if (frustumCulling) {
            for (TerrainElement element : elements) {
                if (camera.frustum.boundsInFrustum(element.bbox)) {
                    instances.add(element.modelInstance);
                }
            }
        } else {
            for (TerrainElement element : elements)
                instances.add(element.modelInstance);
        }

        previousCameraPosition.set(focus);
    }

    public int getNumInstances(){
        return instances.size;
    }

    public void render(Camera cam, Environment environment) {
        terrainBatch.begin(cam);
        terrainBatch.render(instances, environment);
        terrainBatch.end();
        //System.out.println("draw calls: "+terrainBatch.drawCalls + " #renderables:"+terrainBatch.numRenderables+" shader switches:"+terrainBatch.shaderSwitches);

    }

    public Texture getHeightMapTexture(){
        return heightMap.getHeightMapTexture();
    }

    private void buildTerrain(){
        elements.clear();
        float scale = this.tileSize;
        for(int level = 0; level < this.numLevels; level++) {
            buildTerrainLevel(level, this.numLevels, scale );
            scale *= 2f;
        }
    }
    /** Make one of the terrain levels. level 0 is smallest and finest level, level 1 is half the resolution, etc. */
    private void buildTerrainLevel(int level, int numLevels, float scale) {
        addRing(elements, scale);
        if(level == 0)   // central square grid
            addCentralSquare(elements, scale);
        // fill the gap to the next level
        if(level < numLevels -1)
            addTrim(elements, scale);
     }



    // scale is the size in world units of one tile at this level
    private void addCentralSquare(Array<TerrainElement> elements, float scale){
        final int N = clipMapSize;
        final int M = (N+1)/4;
        // offset for corner of ring
        float xf = -(float)  (N-1) * scale/2f;
        float zf = -(float)  (N-1) * scale/2f;

        // snap to multiple of 2 tiles
        xf = 2 * scale * Math.round((focus.x+xf) / (2*scale));
        zf = 2 * scale * Math.round((focus.z+zf) / (2*scale));


        // add 4 blocks of size MxM
        addSquare(elements, squareMxM, scale, M, M, xf, zf, M-1, M-1);
        addSquare(elements, squareMxM, scale, M, M, xf, zf, N-2*M+1, M-1);
        addSquare(elements, squareMxM, scale, M, M, xf, zf, M-1, N-2*M+1);
        addSquare(elements, squareMxM, scale, M, M, xf, zf, N-2*M+1, N-2*M+1);

        // vertical filler blocks to close the ring
        addSquare(elements, filler3XM, scale,3, M,  xf, zf, 2*(M-1), M-1);
        addSquare(elements, filler3XM, scale, 3, M, xf, zf, 2*(M-1), N-2*M+1);

        // horizontal filler blocks to close the ring
        addSquare(elements, fillerMX3, scale, M, 3, xf, zf, M-1, 2*(M-1));
        addSquare(elements, fillerMX3, scale, M, 3, xf, zf, N-2*M+1, 2*(M-1));

        // fill in centre gap
        addSquare(elements, filler3X3, scale, 3, 3, xf, zf, 2*(M-1), 2*(M-1));

    }


    // scale is the size in world units of one tile at this level
    private void addRing(Array<TerrainElement> elements, float scale){
        final int N = clipMapSize;
        final int M = (N+1)/4;
        // offset for corner of ring
        float xf = -(float)  (N-1) * scale/2f;
        float zf = -(float)  (N-1) * scale/2f;

        // snap to multiple of 2 tiles
        xf = 2 * scale * Math.round((focus.x+xf) / (2*scale));
        zf = 2 * scale * Math.round((focus.z+zf) / (2*scale));


        // add 12 blocks of size MxM
        addSquare(elements, squareMxM, scale, M, M, xf, zf,  0, 0);
        addSquare(elements, squareMxM, scale, M, M, xf, zf, M-1, 0);
        addSquare(elements, squareMxM, scale, M, M, xf, zf, 0, M-1);

        addSquare(elements, squareMxM, scale, M, M, xf, zf, N-M, 0);
        addSquare(elements, squareMxM, scale, M, M, xf, zf, N-2*M+1, 0);
        addSquare(elements, squareMxM, scale, M, M, xf, zf, N-M, M-1);

        addSquare(elements, squareMxM, scale, M, M, xf, zf, 0, N-M);
        addSquare(elements, squareMxM, scale, M, M, xf, zf, M-1, N-M);
        addSquare(elements, squareMxM, scale, M, M, xf, zf, 0, N-2*M+1);

        addSquare(elements, squareMxM, scale, M, M, xf, zf, N-M, N-M);
        addSquare(elements, squareMxM, scale, M, M, xf, zf, N-2*M+1, N-M);
        addSquare(elements, squareMxM, scale, M, M, xf, zf, N-M, N-2*M+1);

        // vertical filler blocks to close the ring
        addSquare(elements, filler3XM, scale,3, M,  xf, zf, 2*(M-1), 0);
        addSquare(elements, filler3XM, scale, 3, M, xf, zf, 2*(M-1), N-M);

        // horizontal filler blocks to close the ring
        addSquare(elements, fillerMX3, scale, M, 3, xf, zf, 0, 2*(M-1));
        addSquare(elements, fillerMX3, scale, M, 3, xf, zf, N-M, 2*(M-1));

        addSquare(elements, fringe, scale, N, N, xf, zf,  0, 0);
    }

    /** Add L shaped trim around the level to fill in the gap with the next larger level.
     * The trim is same resolution as the surrounding level, i.e. half the resolution of the enclosed level. */
    private void addTrim(Array<TerrainElement> elements, float scale) {
        final int N = clipMapSize;
        final int M = (N+1)/4;

        // offset for corner of ring
        float xf = -(float) (N - 1) * scale/2f ;
        float zf = -(float) (N - 1) * scale/2f ;

        int xc = Math.round((focus.x + xf) / ( scale*2));
        int zc = Math.round((focus.z + zf) / ( scale*2));

        // snap to multiple of 2 tiles
        xf =  scale*2 * xc;
        zf =  scale*2 * zc;

        if(zc % 2 == 0)
            addSquare(elements, horizontalTrim, scale*2, 2*M+1, 3, xf, zf, (xc % 2 == 0 ? -1: 0), -1); // top trim
        else
            addSquare(elements, horizontalTrim, scale*2, 2*M+1, 2, xf, zf, (xc % 2 == 0 ? -1 : 0), (N-1)/2); // bottom trim

        if(xc % 2 == 0)
            addSquare(elements, verticalTrim, scale*2, 2, 2*M, xf, zf, -1, 0); // left trim
        else
            addSquare(elements, verticalTrim, scale*2, 2, 2*M, xf, zf, (N-1)/2, 0); // right trim
    }



    private final Vector3 min = new Vector3();
    private final Vector3 max = new Vector3();

    /** add a terrain element
     * xo,zo: position of level (bottom left corner)
     * x,z: position of this element (in tiles)
     * */
    private void addSquare(Array<TerrainElement> elements, Model model, float scale, int w, int h, float xo, float zo, int x, int z){
        ModelInstance instance = new ModelInstance(model);
        instance.transform.translate(xo + x * scale, 0, zo + z*scale);
        instance.transform.scale(scale, 1f, scale);
        BoundingBox bbox = new BoundingBox();

        min.set(xo + x * scale, 0, zo + z*scale);
        max.set(min);
        max.add(scale * (w-1), amplitude, scale*(h-1));
        bbox.set(min, max);
        elements.add(new TerrainElement(instance, bbox));
    }

    @Override
    public void dispose() {
        heightMap.dispose();
        disposeBlocks();
        terrainBatch.dispose();
    }

    private void disposeBlocks() {
        if(squareMxM == null)
            return;
        squareMxM.dispose();
        squareMxM = null;
        filler3XM.dispose();
        fillerMX3.dispose();
        filler3X3.dispose();
        horizontalTrim.dispose();
        verticalTrim.dispose();
        fringe.dispose();

    }
}
