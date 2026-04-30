package com.monstrous.terrain;


import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.monstrous.gdx.webgpu.graphics.g2d.WgSpriteBatch;
import com.monstrous.gdx.webgpu.graphics.g3d.WgModelBatch;
import com.monstrous.gdx.webgpu.graphics.g3d.utils.WgModelBuilder;
import com.monstrous.gdx.webgpu.graphics.utils.WgScreenUtils;
import com.monstrous.gdx.webgpu.graphics.utils.WgShapeRenderer;
import com.monstrous.terrain.terrain.Terrain;

public class TerrainDemo extends ApplicationAdapter {
	public PerspectiveCamera cam;
	public CameraInputController camController;
	public Environment environment;
	public WgSpriteBatch batch;
	public GUI gui;
    public Terrain terrain;
    private Model cube;
    private WgModelBatch modelBatch;
	private CatmullRomSpline<Vector3> myCatmull;
	private WgShapeRenderer shapeRenderer;
	private float time;
	private final Vector3 tmp = new Vector3();
	private final Vector3[] pathPoints = new Vector3[100];	// to render spline (debug)
    private Array<ModelInstance> vegetation;   // to show placement at terrain height


	@Override
	public void create() {
        terrain = new Terrain(1023, 7, 32f);

        gui = new GUI(this, terrain);


        generateVegetation(terrain);

		// create perspective camera
		cam = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(0, 20000, 0);
		cam.lookAt(0, 0, 0);
        // far distance is world distance of diagonal over height map
		cam.far =  terrain.heightMap.getSize() * terrain.getScale();
		cam.near = 10f;
		cam.update();

		// add camera controller
		camController = new CameraInputController(cam);
        camController.scrollFactor = -100f;

		// input multiplexer to send inputs to GUI and to cam controller
		InputMultiplexer im = new InputMultiplexer();
		Gdx.input.setInputProcessor(im);
		im.addProcessor(gui.stage); // set stage as first input processor
		im.addProcessor(camController);

		// define some lighting
		environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.Fog, Color.SKY));

        modelBatch = new WgModelBatch();

		batch = new WgSpriteBatch();
		buildCameraPath();
		shapeRenderer = new WgShapeRenderer();
	}

	@Override
	public void resize(int width, int height) {
		cam.viewportWidth = Gdx.graphics.getWidth();
		cam.viewportHeight = Gdx.graphics.getHeight();
		cam.update();

        batch.getProjectionMatrix().setToOrtho2D(0,0,width, height);

		gui.resize(width, height);
	}

	@Override
	public void render() {
		// update camera positioning
		camController.update();
        float delta = Gdx.graphics.getDeltaTime();
		time += delta;
        if(gui.flyCamera)
		    moveCameraAlongSpline(time);
        else
            cam.lookAt(0, 0, 0);

//        float height = terrain.getHeight(cam.position.x, cam.position.z);
//        if(height + 10f > cam.position.y)
//            cam.position.y = height + 10f;

        if(!gui.freezeLoD && gui.showTerrain)
            terrain.update(cam);

		// clear screen
        WgScreenUtils.clear(Color.SKY, true);

        if(gui.showTerrain)
            terrain.render(cam, environment);

        if(gui.showCameraPath)
		    renderPath();

        // enable this to demonstrate we can get accurate terrain height by placing blocks on the terrain
        //renderVegetation();

		if (gui.showHeightmap) {
			batch.begin();
			batch.draw(terrain.getHeightMapTexture(), Gdx.graphics.getWidth()-256, Gdx.graphics.getHeight()-256, 256, 256);
			batch.end();
		}
		gui.render(Gdx.graphics.getDeltaTime());
	}

	@Override
	public void dispose() {
		batch.dispose();
        terrain.dispose();
        gui.dispose();
        cube.dispose();
        modelBatch.dispose();
	}

    // not guaranteed to not collide into terrain
	private void buildCameraPath() {
        float ht = 0.5f*terrain.getAmplitude();
        float scl = 16f;

		Vector3[] controlPoints = {
				new Vector3(-2000*scl, ht+400f*scl, 2000*scl),
				new Vector3(2000*scl, ht+500*scl, 2500*scl),

				new Vector3(2500*scl, ht+800*scl, -3000*scl),

				new Vector3(-1500*scl, ht+300*scl, -2400*scl),
                new Vector3(-500*scl, ht+800*scl, -500*scl),

                new Vector3(500*scl, ht+400*scl, 500*scl),

        };
		myCatmull = new CatmullRomSpline<Vector3>(controlPoints, true);

		// fill array of points for debug render
		for(int i = 0; i < 100; i++) {
			Vector3 out = new Vector3();
			myCatmull.valueAt(out, i/100f);
			pathPoints[i] = out;
		}
	}

    // randomly place little cubes on the terrain to demonstrate we can get terrain height correctly
    // call again whenever terrain scale or amplitude is changed
    public void generateVegetation(Terrain terrain){
        ModelBuilder builder = new WgModelBuilder();
        float SZ = 250f;
        cube = builder.createBox(SZ, SZ, SZ, new Material(ColorAttribute.createDiffuse(Color.BROWN)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        final int N = 1000;
        final float worldSize = terrain.heightMap.getSize() * terrain.getScale();
        vegetation = new Array<>();

        for(int i = 0; i < N; i++){
            float x = ((float)Math.random() -0.5f) * worldSize;
            float z = ((float)Math.random() -0.5f) * worldSize;
            float h = 5f + terrain.getHeight(x*0.9f, z*0.9f);
            vegetation.add( new ModelInstance(cube, x*0.9f, h, z*0.9f));
        }
    }

    private void renderVegetation(){
        modelBatch.begin(cam);
        modelBatch.render(vegetation);
        modelBatch.end();
    }


	private void moveCameraAlongSpline(float time) {
		float t = 0.015f*time;
		if (t > 1)
			t -= (int)t;
		myCatmull.valueAt(tmp, t);
		cam.position.set(tmp);
		myCatmull.derivativeAt(tmp, t);
		cam.direction.set(tmp);
        cam.up.set(Vector3.Y);
		cam.update();
	}

	// render path as a red line (debug)
	private void renderPath() {
		shapeRenderer.setProjectionMatrix(cam.combined);
		shapeRenderer.begin(WgShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(1,0,0,1);
    	for(int i = 0; i < 100-1; i++)
		{
			shapeRenderer.line(pathPoints[i], pathPoints[i+1]);
		}
		shapeRenderer.line(pathPoints[99], pathPoints[0]);
		shapeRenderer.end();
	}

}

