package com.monstrous.terrain;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.monstrous.gdx.webgpu.scene2d.WgSkin;
import com.monstrous.gdx.webgpu.scene2d.WgStage;
import com.monstrous.terrain.terrain.Terrain;

public class GUI {

    public Stage stage;
    public Skin skin;
    public TerrainDemo main;
    public Terrain terrain;

    public boolean showHeightmap = false;

    public boolean freezeLoD = false;
    public boolean showTerrain = true;
    public boolean showWireFrame = false;
    public boolean culling = true;
    public boolean showCameraPath = false;
    public boolean flyCamera = true;
    public int gridsize = 16;
    private Label fpsLabel;
    private Label instancesLabel;
    private Label ampLabel;
    private float amplitude;
    private Label scaleLabel;
    private float scale;
    private int clipMapSizePower;
    private int clipMapSize;
    private Label clipMapSizeLabel;
    private int numLevels;
    private float clipMapScale;
    private Label levelsLabel;
    private Label clipMapScaleLabel;


    public GUI ( TerrainDemo main, Terrain terrain ) {

        this.main = main;
        this.terrain = terrain;

        // GUI elements via Stage class
        skin = new WgSkin(Gdx.files.internal("ui2/uiskin.json"));
        stage = new WgStage(new ScreenViewport());

        clipMapSizePower = 8;
        clipMapSize = 255;
        numLevels = 7;
        clipMapScale = 32f;

        addActors();
    }

    private void addActors() {

        Table controls = new Table();
        controls.left();

        controls.add(new Label("FPS: ", skin)).left();

        fpsLabel = new Label("0", skin);
        controls.add(fpsLabel).left().row();

        controls.add(new Label("ModelInstances: ", skin)).left();

        instancesLabel = new Label("0", skin);
        controls.add(instancesLabel).left().row();

        // show heightmap
        //
        final CheckBox checkbox = new CheckBox("show map", skin);
        checkbox.setChecked(showHeightmap);
        checkbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showHeightmap = checkbox.isChecked();
             }
        });
        controls.add(checkbox).left().row();

        final CheckBox terrainCheckbox = new CheckBox("show terrain", skin);
        terrainCheckbox.setChecked(showTerrain);
        terrainCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showTerrain = terrainCheckbox.isChecked();
            }
        });
        controls.add(terrainCheckbox).left().row();


        final CheckBox linesCheckbox = new CheckBox("wire frame", skin);
        linesCheckbox.setChecked(showWireFrame);
        linesCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showWireFrame = linesCheckbox.isChecked();
                terrain.setWireFrameMode(showWireFrame);
                terrain.generateBlocks();
            }
        });
        controls.add(linesCheckbox).left().row();

        final CheckBox freezeCheckbox = new CheckBox("freeze Level of Detail", skin);
        freezeCheckbox.setChecked(freezeLoD);
        freezeCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                freezeLoD = freezeCheckbox.isChecked();

            }
        });
        controls.add(freezeCheckbox).left().row();

        final CheckBox cullingCheckbox = new CheckBox("frustum culling", skin);
        cullingCheckbox.setChecked(culling);
        cullingCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                culling = cullingCheckbox.isChecked();
                terrain.setCulling(culling);
            }
        });
        controls.add(cullingCheckbox).left().row();

        final CheckBox flyCheckbox = new CheckBox("fly camera", skin);
        flyCheckbox.setChecked(flyCamera);
        flyCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                flyCamera = flyCheckbox.isChecked();
            }
        });
        controls.add(flyCheckbox).left().row();


        final CheckBox camPathCheckbox = new CheckBox("camera spline", skin);
        camPathCheckbox.setChecked(showCameraPath);
        camPathCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showCameraPath = camPathCheckbox.isChecked();
            }
        });
        controls.add(camPathCheckbox).left().row();



        // amplitude
        amplitude = terrain.getAmplitude();
        final Slider ampSlider = new Slider(0f, 50000f, 100f, false, skin);
        ampSlider.setAnimateDuration(0.1f);
        ampSlider.setValue(amplitude);
        ampSlider.setSize(150, 20);
        controls.add(new Label("amplitude", skin));
        controls.add(ampSlider);

        ampLabel = new Label(String.valueOf(amplitude), skin);
        controls.add(ampLabel).row();
        ampSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                amplitude = ampSlider.getValue();
                ampLabel.setText(String.valueOf((int)amplitude));
                terrain.setAmplitude(amplitude);
                main.generateVegetation(terrain);

            }
        });

        // scale
        scale = terrain.getScale();
        final Slider scaleSlider = new Slider(0f, 256f, 1f, false, skin);
        scaleSlider.setAnimateDuration(0.1f);
        scaleSlider.setValue(scale);
        scaleSlider.setSize(150, 20);
        controls.add(new Label("scale    ", skin));
        controls.add(scaleSlider);

        scaleLabel = new Label(String.valueOf(scale), skin);
        controls.add(scaleLabel).row();
        scaleSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                scale = scaleSlider.getValue();
                scaleLabel.setText(String.valueOf(scale));
                terrain.setScale(scale);
                main.generateVegetation(terrain);

            }
        });

        // clipMapSize

        final Slider cmSizeSlider = new Slider(3, 10, 1, false, skin);
        cmSizeSlider.setAnimateDuration(0.1f);
        cmSizeSlider.setValue(clipMapSizePower);
        cmSizeSlider.setSize(150, 20);
        controls.add(new Label("clip map size    ", skin));
        controls.add(cmSizeSlider);

        clipMapSize = Math.round((float)Math.pow(2.0, clipMapSizePower) - 1f);
        clipMapSizeLabel = new Label(String.valueOf(clipMapSize), skin);
        controls.add(clipMapSizeLabel).row();
        cmSizeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                clipMapSizePower = (int)cmSizeSlider.getValue();
                clipMapSize = Math.round((float)Math.pow(2.0, clipMapSizePower) - 1f);
                clipMapSizeLabel.setText(String.valueOf(clipMapSize));
                terrain.setClipMapParameters(clipMapSize, numLevels, clipMapScale);
            }
        });

        // numLevels

        final Slider levelsSlider = new Slider(1, 10, 1, false, skin);
        levelsSlider.setAnimateDuration(0.1f);
        levelsSlider.setValue(numLevels);
        levelsSlider.setSize(150, 20);
        controls.add(new Label("numLevels    ", skin));
        controls.add(levelsSlider);

        levelsLabel = new Label(String.valueOf(numLevels), skin);
        controls.add(levelsLabel).row();
        levelsSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                numLevels = (int)levelsSlider.getValue();
                levelsLabel.setText(String.valueOf(numLevels));
                terrain.setClipMapParameters(clipMapSize, numLevels, clipMapScale);
            }
        });


        final Slider cmScaleSlider = new Slider(1f, 500f, 1f, false, skin);
        cmScaleSlider.setAnimateDuration(0.1f);
        cmScaleSlider.setValue(clipMapScale);
        cmScaleSlider.setSize(150, 20);
        controls.add(new Label("clip map scale    ", skin));
        controls.add(cmScaleSlider);

        clipMapScaleLabel = new Label(String.valueOf(clipMapScale), skin);
        controls.add(clipMapScaleLabel).row();
        cmScaleSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                clipMapScale = cmScaleSlider.getValue();
                clipMapScaleLabel.setText(String.valueOf(clipMapScale));
                terrain.setClipMapParameters(clipMapSize, numLevels, clipMapScale);
            }
        });





        controls.pack();
        controls.setPosition(0,stage.getHeight()-controls.getHeight());
        stage.addActor(controls);


//        // perlin grid size
//        final Slider slider = new Slider(2, 256, 1, false, skin);
//        slider.setAnimateDuration(0.1f);
//        slider.setValue(gridsize);
//        slider.setSize(150, 20);
//        slider.setPosition(100, yy);
//        stage.addActor(slider);
//        final Label label = new Label("gridsize", skin);
//        label.setPosition(0, yy);
//        stage.addActor(label);
//        final Label label2 = new Label(String.valueOf(gridsize), skin);
//        label2.setPosition(0, yy+20);
//        stage.addActor(label2);
//        slider.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                gridsize = (int)slider.getValue();
//                label2.setText(String.valueOf(gridsize));
//                refresh();
//            }
//        });
//        yy-= 30;
//
//        // perlin xoffset
//        final Slider sliderX = new Slider(0, 4, 0.01f, false, skin);
//        sliderX.setAnimateDuration(0.1f);
//        sliderX.setValue(xoffset);
//        sliderX.setSize(150, 20);
//        sliderX.setPosition(100, yy);
//        stage.addActor(sliderX);
//        final Label labelx1 = new Label("xoffset", skin);
//        labelx1.setPosition(0, yy);
//        stage.addActor(labelx1);
//        final Label labelx2 = new Label(String.valueOf(xoffset), skin);
//        labelx2.setPosition(0, yy+20);
//        stage.addActor(labelx2);
//        sliderX.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                xoffset = sliderX.getValue();
//                labelx2.setText(String.valueOf(xoffset));
//                refresh();
//            }
//        });
//        yy-= 30;
//
//        // octaves
//        final Slider octavesSlider = new Slider(1, 12, 1, false, skin);
//        octavesSlider.setAnimateDuration(0.1f);
//        octavesSlider.setValue(Noise.octaves);
//        octavesSlider.setSize(150, 20);
//        octavesSlider.setPosition(100, yy);
//        stage.addActor(octavesSlider);
//        final Label label3 = new Label("octaves", skin);
//        label3.setPosition(0, yy);
//        stage.addActor(label3);
//        final Label label4 = new Label(String.valueOf(Noise.octaves), skin);
//        label4.setPosition(0, yy+20);
//        stage.addActor(label4);
//        octavesSlider.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                Noise.octaves = (int)octavesSlider.getValue();
//                label4.setText(String.valueOf(Noise.octaves));
//                refresh();
//            }
//        });
//        yy-= 30;
//
//        // persistence
//        final Slider persSlider = new Slider(.1f, 1.0f, 0.05f, false, skin);
//        persSlider.setAnimateDuration(0.1f);
//        persSlider.setValue(Noise.persistence);
//        persSlider.setSize(150, 20);
//        persSlider.setPosition(100, yy);
//        stage.addActor(persSlider);
//        final Label label5 = new Label("persistence", skin);
//        label5.setPosition(0, yy);
//        stage.addActor(label5);
//        final Label label6 = new Label(String.valueOf(Noise.persistence), skin);
//        label6.setPosition(0, yy+20);
//        stage.addActor(label6);
//        persSlider.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                Noise.persistence = persSlider.getValue();
//                label6.setText(String.valueOf(Noise.persistence));
//                refresh();
//            }
//        });
//        yy-= 30;
//



    }






    public void resize (int width, int height) {
          stage.getViewport().update(width, height, true);
            // TODO ensure actors stay at top left
    }

    public void render( float delta ) {
        fpsLabel.setText(Gdx.graphics.getFramesPerSecond());
        instancesLabel.setText(terrain.getNumInstances());
        stage.act(delta);
        stage.draw();
    }

    public void dispose () {

        stage.dispose();

    }
}
