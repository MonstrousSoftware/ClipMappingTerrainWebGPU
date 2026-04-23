package com.monstrous.terrain.terrain;

import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.monstrous.gdx.webgpu.graphics.g3d.utils.WgModelBuilder;
import com.monstrous.gdx.webgpu.graphics.utils.WgMeshBuilder;

public class GridModelBuilder {


    /** Make a Model consisting of a square 2D grid of NxN vertices */
    public Model makeGridModel( int N, int primitive, Material material) {
        return makeGridModel( N, N, primitive, material);
    }

    /** Make a Model consisting of a rectangular grid of size NxM vertices */
   public Model makeGridModel(int N, int M, int primitive, Material material) {

        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates;

        ModelBuilder modelBuilder = new WgModelBuilder();
        modelBuilder.begin();
        WgMeshBuilder meshBuilder = (WgMeshBuilder) modelBuilder.part("face", primitive, attr, material);
        final int numVerts = N * M;
        final int numTris = 2 * (N-1) * (M-1);
        Vector3[] vertices = new Vector3[numVerts];

        meshBuilder.ensureVertices(numVerts);
        meshBuilder.ensureTriangleIndices(numTris);


        Vector3 pos = new Vector3();
        float height = 0f; // this will be filled in by the vertex shader

        for (int z = 0; z < M; z++) {
            for (int x = 0; x < N; x++) {
                pos.set(x , height, z );            // swapping z,y to orient horizontally
                vertices[z * N + x] = new Vector3(pos);
            }
            if (z >= 1) {
                // add to index list to make a row of triangles using vertices at y and y-1
                short v0 = (short) ((z - 1) * N);    // vertex number at top left of this row
                for (short t = 0; t < N-1; t++) {
                    // counter-clockwise winding
                    addTriangle(meshBuilder, vertices,  v0, (short) (v0 + N), (short) (v0 + 1));
                    addTriangle(meshBuilder, vertices,  (short) (v0 + 1), (short) (v0 + N), (short) (v0 + N + 1));
                    v0++;                // next column
                }
            }
        }


        // and pass vertex to meshBuilder
        MeshPartBuilder.VertexInfo vert = new MeshPartBuilder.VertexInfo();
        vert.hasColor = false;
        vert.hasNormal = false;
        vert.hasPosition = true;
        vert.hasUV = false;

        for (int i = 0; i < numVerts; i++) {
            vert.position.set(vertices[i]);
            meshBuilder.vertex(vert);
        }

        return modelBuilder.end();
    }

    /** Create a list of triangles to close gaps with next enclosing level. These triangles close any gap between the tiles of this level
     * and the tiles of the enclosing level which are twice as big. */
    public Model makeTriangleFringe(int N, int M, int primitive, Material material) {

        int attr = VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates;

        ModelBuilder modelBuilder = new WgModelBuilder();
        modelBuilder.begin();
        WgMeshBuilder meshBuilder = (WgMeshBuilder) modelBuilder.part("fringe", primitive, attr, material);
        // N or M vertices per side
        final int numVerts = 2*N+2*M;
        // (N-1)/2 triangles per side = number of tiles at next higher level
        final int numTris = (N-1) * (M-1);

        Vector3[] positions = new Vector3[numVerts];
        meshBuilder.ensureVertices(numVerts);
        meshBuilder.ensureTriangleIndices(numTris);

        Vector3 pos = new Vector3();
        float ht = 0f; // this will be filled in by the vertex shader

        // add all the vertices on the circumference of the rectangle
        int index = 0;
        for (int x = 0; x < N; x++) {
            pos.set(x , ht, 0 );
            positions[index++] = new Vector3(pos);
        }
        for (int x = 0; x < N; x++) {
            pos.set(x , ht, M-1 );
            positions[index++] = new Vector3(pos);
        }
        for (int z = 0; z < M; z++) {
            pos.set(0 , ht, z );
            positions[index++] = new Vector3(pos);
        }
        for (int z = 0; z < M; z++) {
            pos.set(N-1 , ht, z );
            positions[index++] = new Vector3(pos);
        }

        // build triangles along the edges to match up with next level
        short v0 = (short) 0;    // index
        for (short t = 0; t < (N-1)/2; t++) {
            addTriangle(meshBuilder, positions,  v0, (short) (v0 + 1), (short) (v0 + 2));
            v0+=2;                // next triangle, reuse the last vertex for the start of the next triangle
        }
        v0++;
        for (short t = 0; t < (N-1)/2; t++) {
            addTriangle(meshBuilder, positions,  v0, (short) (v0 + 2), (short) (v0 + 1)); // reverse winding order
            v0+=2;                // next triangle, reuse the last vertex for the start of the next triangle
        }
        v0++;
        for (short t = 0; t < (M-1)/2; t++) {
            addTriangle(meshBuilder, positions,  v0, (short) (v0 + 2), (short) (v0 + 1)); // reverse winding order
            v0+=2;                // next triangle, reuse the last vertex for the start of the next triangle
        }
        v0++;
        for (short t = 0; t < (M-1)/2; t++) {
            addTriangle(meshBuilder, positions,  v0, (short) (v0 + 1), (short) (v0 + 2));
            v0+=2;                // next triangle, reuse the last vertex for the start of the next triangle
        }

        // pass vertex to meshBuilder
        MeshPartBuilder.VertexInfo vert = new MeshPartBuilder.VertexInfo();
        vert.hasColor = false;
        vert.hasNormal = false;
        vert.hasPosition = true;
        vert.hasUV = false;

        for (int i = 0; i < numVerts; i++) {
            vert.position.set(positions[i]);
            meshBuilder.vertex(vert);
        }

        return modelBuilder.end();
    }



    private void addTriangle(WgMeshBuilder meshBuilder, final Vector3[] vertices, short v0, short v1, short v2) {
        meshBuilder.triangle(v0, v1, v2);
    }
}
