package adventure.desktop;

import adventure.Main;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public final class Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
        c.setTitle("자석의 모험 v3 · MAGNET HOP");
        c.setWindowedMode(1200, 750);
        c.setResizable(true);
        c.useVsync(true);
        c.setForegroundFPS(60);
        c.setIdleFPS(30);
        c.setInitialVisible(!Boolean.getBoolean("magnet.hidden"));
        c.disableAudio(Boolean.getBoolean("magnet.silent"));
        new Lwjgl3Application(new Main(), c);
    }
}
