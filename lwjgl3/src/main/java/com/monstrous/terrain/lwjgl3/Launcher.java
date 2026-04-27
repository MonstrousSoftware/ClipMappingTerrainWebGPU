package com.monstrous.terrain.lwjgl3;

import com.github.xpenatan.webgpu.JWebGPUBackend;
import com.monstrous.gdx.webgpu.backends.desktop.WgDesktopApplication;
import com.monstrous.gdx.webgpu.backends.desktop.WgDesktopApplicationConfiguration;
import com.monstrous.terrain.TerrainDemo;


public class Launcher {
    public static void main (String[] argv) {

        WgDesktopApplicationConfiguration config = new WgDesktopApplicationConfiguration();
        config.setWindowedMode(1200, 800);
        config.setTitle("ClipMappingTerrain");
        config.enableGPUtiming = false;
        config.useVsync(false);
        config.backendWebGPU = JWebGPUBackend.DAWN;
        //config.setForegroundFPS(WgDesktopApplicationConfiguration.getDisplayMode().refreshRate + 1);
        config.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

        new WgDesktopApplication(new TerrainDemo(), config);
    }
}
