package adventure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/** Real LWJGL render loop, using the same keyboard command handler and input-only route. */
public final class RenderVerification implements Main.Driver {
    private int frame, clearFrames, pauseFrames, phase, attachedFrames;
    private float pausedTime;
    private final RoutePilot pilot = new RoutePilot();
    private final boolean[] captured = new boolean[3];
    private final boolean[] extended = new boolean[4];
    private String folder = System.getProperty("magnet.evidence");

    private void capture(Main g, String name) {
        if (folder != null) g.screenshot(folder + "/" + name + ".png");
    }

    private void key(int key) {
        Gdx.input.getInputProcessor().keyDown(key);
        Gdx.input.getInputProcessor().keyUp(key);
    }

    public void update(Main g) {
        frame++;
        if (frame == 10) {
            capture(g, "00-title");
            Gdx.input
                    .getInputProcessor()
                    .touchDown(
                            (int) (Gdx.graphics.getWidth() * 240f / 1200),
                            (int) (Gdx.graphics.getHeight() * 523f / 750),
                            0,
                            Input.Buttons.LEFT);
            if (!g.help || !g.title) throw new AssertionError("title help");
            key(Input.Keys.ESCAPE);
            key(Input.Keys.NUM_2);
            if (!g.title) throw new AssertionError("stage selection must be removed");
            Gdx.input
                    .getInputProcessor()
                    .touchDown(
                            (int) (Gdx.graphics.getWidth() * 240f / 1200),
                            (int) (Gdx.graphics.getHeight() * 443f / 750),
                            0,
                            Input.Buttons.LEFT);
            if (g.title || g.world.level.index != 0) throw new AssertionError("start mouse click");
            g.useOverride = true;
        }
        if (frame < 11) return;
        if (frame == 11) key(Input.Keys.R);
        if (frame >= 11 && frame < 18) {
            if (frame == 16) capture(g, "03-retry");
            return;
        }
        if (frame == 18) {
            g.start(0);
            return;
        }
        GameWorld w = g.world;
        if (phase == 0 && w.level.index == 0 && w.jumps > 0 && w.player.grounded) {
            Gdx.input
                    .getInputProcessor()
                    .touchDown(
                            (int) (Gdx.graphics.getWidth() * 1150f / 1200),
                            (int) (Gdx.graphics.getHeight() * 49f / 750),
                            0,
                            Input.Buttons.LEFT);
            if (!w.paused) throw new AssertionError("pause mouse click");
            pausedTime = w.time;
            phase = 1;
        }
        if (phase == 1) {
            pauseFrames++;
            if (pauseFrames == 10) capture(g, "01-pause");
            if (w.time != pausedTime) throw new AssertionError("UI pause advanced physics");
            if (pauseFrames > 25) {
                key(Input.Keys.ESCAPE);
                phase = 2;
            }
            return;
        }
        if (phase == 2 && w.level.index == 0 && w.jumps > 0 && w.player.grounded) {
            key(Input.Keys.H);
            phase = 3;
            pauseFrames = 0;
        }
        if (phase == 3) {
            pauseFrames++;
            if (pauseFrames == 10) capture(g, "02-help");
            if (pauseFrames > 20) {
                key(Input.Keys.H);
                phase = 4;
            }
            return;
        }
        float[] thresholds = {2650, 2350, 3750, 2500};
        int[] stages = {0, 1, 1, 2};
        for (int i = 0; i < 4; i++) {
            if (!extended[i] && w.level.index == stages[i] && w.player.x > thresholds[i]) {
                capture(g, "extended-" + i);
                extended[i] = true;
            }
        }
        attachedFrames = w.attached != null ? attachedFrames + 1 : 0;
        if (phase >= 4
                && !captured[w.level.index]
                && ((w.level.index == 0 && w.player.x > 950)
                        || (w.level.index > 0 && attachedFrames > 10))) {
            capture(g, "stage-" + (w.level.index + 1));
            captured[w.level.index] = true;
        }
        if (w.stageClear) {
            clearFrames++;
            if (clearFrames == 3) capture(g, "clear-" + (w.level.index + 1));
            if (clearFrames > 15) {
                key(Input.Keys.ENTER);
                clearFrames = 0;
            }
            return;
        }
        if (w.finished) {
            clearFrames++;
            if (clearFrames == 5) capture(g, "04-finish");
            if (clearFrames > 15) {
                if (w.deaths != 0
                        || w.completed != 3
                        || !captured[0]
                        || !captured[1]
                        || !captured[2]) throw new AssertionError("render campaign incomplete");
                System.out.printf(
                        "RENDER VERIFIED stages=%d deaths=%d attaches=%d rides=%d flips=%d"
                                + " simulatedSeconds=%.2f frames=%d%n",
                        w.completed, w.deaths, w.attaches, w.rides, w.flips, w.time, frame);
                key(Input.Keys.ENTER);
                if (g.world.level.index != 0 || g.world.finished)
                    throw new AssertionError("UI restart failed");
                key(Input.Keys.ESCAPE);
                key(Input.Keys.BACKSPACE);
                if (!g.title) throw new AssertionError("return to title");
                Gdx.input
                        .getInputProcessor()
                        .touchDown(
                                (int) (Gdx.graphics.getWidth() * 240f / 1200),
                                (int) (Gdx.graphics.getHeight() * 603f / 750),
                                0,
                                Input.Buttons.LEFT);
                System.out.println(
                        "MENU VERIFIED start/help/quit pointer handlers and campaign restart");
            }
            return;
        }
        pilot.decide(w);
        g.directionOverride = pilot.direction;
        if (pilot.jump) key(Input.Keys.SPACE);
        if (w.time > 300 || frame > 25000)
            throw new AssertionError(
                    "render route stalled phase="
                            + pilot.phase
                            + " x="
                            + w.player.x
                            + " y="
                            + w.player.y);
    }
}
