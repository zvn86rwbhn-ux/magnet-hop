package adventure;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** 수업의 Main처럼 입력, 실행 흐름, 카메라와 화면 출력을 담당한다. */
public final class Main extends ApplicationAdapter {
    public interface Driver {
        void update(Main game);
    }

    public final GameWorld world = new GameWorld();
    public boolean title = true, help = false, muted = false;
    public int directionOverride = 0;
    public boolean useOverride = false, jumpQueued = false;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;
    private OrthographicCamera camera, hud;
    private FitViewport viewport;
    private final CameraFollow cameraFollow = new CameraFollow();
    private float accumulator, uiTime;
    private int lastAttach, lastRepel, lastFlip, lastDeaths;
    private Sound attachSound, jumpSound, flipSound, failSound;
    private Driver driver;
    private boolean pauseBeforeHelp, wasGrounded, wasClear;
    private int lastJumps, particleCursor;
    private float landingPulse, lastRotation, failureTime;
    private String failureText = "";
    private final float[] px = new float[80],
            py = new float[80],
            pvx = new float[80],
            pvy = new float[80],
            life = new float[80];
    private final Color[] particleColor = new Color[80];
    private final Color ink = Color.valueOf("23364D"),
            mutedInk = Color.valueOf("677D8A"),
            red = Color.valueOf("EF6865"),
            blue = Color.valueOf("4A91CF"),
            mint = Color.valueOf("39AF99"),
            yellow = Color.valueOf("E9B94B");

    @Override
    public void create() {
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont(Gdx.files.internal("ui.fnt"));
        font.getRegion()
                .getTexture()
                .setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        layout = new GlyphLayout();
        camera = new OrthographicCamera(1200, 750);
        hud = new OrthographicCamera();
        hud.setToOrtho(false, 1200, 750);
        viewport = new FitViewport(1200, 750, hud);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        attachSound = Gdx.audio.newSound(Gdx.files.internal("attach.wav"));
        jumpSound = Gdx.audio.newSound(Gdx.files.internal("jump.wav"));
        flipSound = Gdx.audio.newSound(Gdx.files.internal("switch.wav"));
        failSound = Gdx.audio.newSound(Gdx.files.internal("retry.wav"));
        Gdx.input.setInputProcessor(
                new InputAdapter() {
                    @Override
                    public boolean keyDown(int key) {
                        handleKey(key);
                        return true;
                    }

                    @Override
                    public boolean touchDown(int x, int y, int pointer, int button) {
                        if (button == Input.Buttons.LEFT) handlePointer(x, y);
                        return true;
                    }
                });
        resetCamera();
        String driverClass = System.getProperty("magnet.driver");
        if (driverClass != null)
            try {
                driver = (Driver) Class.forName(driverClass).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    private void handlePointer(int sx, int sy) {
        Vector3 v = new Vector3(sx, sy, 0);
        viewport.unproject(v);
        if (help) {
            if (v.y < 220) handleKey(Input.Keys.ESCAPE);
            return;
        }
        if (title) {
            if (v.x >= 76 && v.x <= 406) {
                if (v.y >= 275 && v.y <= 339) handleKey(Input.Keys.ENTER);
                if (v.y >= 195 && v.y <= 259) handleKey(Input.Keys.H);
                if (v.y >= 115 && v.y <= 179) handleKey(Input.Keys.Q);
            }
        } else if (!world.stageClear && !world.finished && v.x > 1116 && v.y > 674)
            handleKey(Input.Keys.ESCAPE);
    }

    public void handleKey(int key) {
        if (key == Input.Keys.M) {
            muted = !muted;
            return;
        }
        if (key == Input.Keys.H) {
            if (!help) {
                pauseBeforeHelp = world.paused;
                world.paused = true;
                help = true;
            } else {
                help = false;
                world.paused = pauseBeforeHelp;
            }
            accumulator = 0;
            return;
        }
        if (help) {
            if (key == Input.Keys.ESCAPE) {
                help = false;
                world.paused = pauseBeforeHelp;
            }
            return;
        }
        if (title) {
            if (key == Input.Keys.ENTER || key == Input.Keys.SPACE) start(0);

            if (key == Input.Keys.Q) Gdx.app.exit();
            return;
        }
        if (world.finished) {
            if (key == Input.Keys.ENTER) start(0);
            if (key == Input.Keys.ESCAPE) title = true;
            return;
        }
        if (key == Input.Keys.ESCAPE) {
            world.paused = !world.paused;
            accumulator = 0;
            jumpQueued = false;
            return;
        }
        if (world.paused) {
            if (key == Input.Keys.ENTER) world.paused = false;
            if (key == Input.Keys.BACKSPACE) {
                title = true;
                world.paused = false;
            }
            if (key == Input.Keys.R) {
                world.retry();
                world.paused = false;
                resetCamera();
            }
            return;
        }
        if (world.stageClear) {
            if (key == Input.Keys.ENTER) {
                world.advance();
                resetCamera();
                accumulator = 0;
            }
            if (key == Input.Keys.BACKSPACE) {
                title = true;
            }
            return;
        }
        if (key == Input.Keys.R) {
            world.retry();
            resetCamera();
            jumpQueued = false;
        }
        if (key == Input.Keys.SPACE) {
            jumpQueued = true;
        }
    }

    public void start(int stage) {
        world.restartCampaign();
        if (stage != 0) world.begin(stage);
        title = false;
        help = false;
        accumulator = 0;
        jumpQueued = false;
        lastAttach = lastRepel = lastFlip = lastDeaths = lastJumps = 0;
        wasGrounded = true;
        wasClear = false;
        landingPulse = lastRotation = failureTime = 0;
        java.util.Arrays.fill(life, 0);
        resetCamera();
    }

    private void resetCamera() {
        cameraFollow.reset(world);
        camera.position.set(cameraFollow.x, cameraFollow.y, 0);
        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
        if (!title && !world.finished) {
            world.paused = true;
            jumpQueued = false;
        }
    }

    @Override
    public void render() {
        if (driver != null) driver.update(this);
        float dt = Math.min(Gdx.graphics.getDeltaTime(), .05f);
        uiTime += dt;
        logic(dt);
        updateEffects(dt);
        playEvents();
        draw();
        String capture = System.getProperty("magnet.capture");
        if (capture != null && uiTime > 1) {
            screenshot(capture);
            System.clearProperty("magnet.capture");
        }
        if (Float.parseFloat(System.getProperty("magnet.exitAfter", "0")) > 0
                && uiTime > Float.parseFloat(System.getProperty("magnet.exitAfter")))
            Gdx.app.exit();
    }

    // 입력은 방향 값으로 전달하고, 실제 위치 변경은 GameWorld가 맡는다.
    private int input() {
        if (useOverride) return directionOverride; // 자동 검증에서만 사용
        int direction = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D))
            direction++;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A))
            direction--;
        return direction;
    }

    private void logic(float dt) {
        // 화면의 FPS가 달라도 물리는 1/120초씩 일정하게 계산한다.
        if (!title && !world.paused && !world.stageClear && !world.finished) {
            accumulator += dt;
            int direction = input();
            while (accumulator >= GameWorld.STEP) {
                boolean jump = jumpQueued;
                world.step(direction, jump);
                jumpQueued = false;
                accumulator -= GameWorld.STEP;
            }
            updateCamera(dt);
        } else accumulator = 0;
        // 마지막 출구에서는 중간 결과 확인 없이 전체 완료 화면을 바로 표시한다.
        if (world.stageClear && world.level.index == 2) world.advance();
    }

    private void updateCamera(float delta) {
        cameraFollow.update(world, delta);
        camera.position.set(cameraFollow.x, cameraFollow.y, 0);
        camera.update();
    }

    private void draw() {
        ScreenUtils.clear(.93f, .95f, .94f, 1);
        viewport.apply();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        if (title) {
            drawTitle();
            if (help) drawHelp();
        } else {
            drawGameWorld();
            drawHud();
            if (help) drawHelp();
            else if (world.paused) drawPause();
            else if (world.finished) drawFinish();
            else if (world.stageClear) drawClear();
        }
    }

    private void burst(float x, float y, Color color, int count) {
        for (int j = 0; j < count; j++) {
            int i = particleCursor++ % life.length;
            float angle = j * 2.39996f;
            px[i] = x;
            py[i] = y;
            pvx[i] = MathUtils.cos(angle) * (40 + j * 5);
            pvy[i] = 40 + Math.abs(MathUtils.sin(angle)) * 95;
            life[i] = .4f;
            particleColor[i] = color;
        }
    }

    private void updateEffects(float dt) {
        if (title || world.paused) return;
        for (int i = 0; i < life.length; i++)
            if (life[i] > 0) {
                life[i] -= dt;
                px[i] += pvx[i] * dt;
                py[i] += pvy[i] * dt;
                pvy[i] -= 260 * dt;
            }
        landingPulse = Math.max(0, landingPulse - dt);
        failureTime = Math.max(0, failureTime - dt);
        if (world.jumps > lastJumps)
            burst(
                    world.player.x,
                    world.player.y + 3,
                    world.floorPole() == 0 ? mint : poleColor(world.floorPole()),
                    12);
        if (world.player.grounded && !wasGrounded) {
            landingPulse = .16f;
            burst(world.player.x, world.player.y + 2, mutedInk, 8);
        }
        if (world.attaches > lastAttach)
            burst(world.player.x, world.player.y + GameWorld.H, mint, 12);
        if (world.flips > lastFlip) burst(world.player.x, world.player.y + 8, yellow, 15);
        if (world.deaths > lastDeaths) {
            java.util.Arrays.fill(life, 0);
            resetCamera();
            failureTime = 1.2f;
            failureText = "다시 한 번! 위험물과 틈을 살펴보세요";
        }
        lastJumps = world.jumps;
        wasGrounded = world.player.grounded;
    }

    private void playEvents() {
        if (!muted) {
            if (world.player.rotationTo > lastRotation) jumpSound.play(.15f);
            if (world.attaches > lastAttach) attachSound.play(.32f);
            if (world.repels > lastRepel) failSound.play(.22f);
            if (world.flips > lastFlip) flipSound.play(.26f);
            if (world.deaths > lastDeaths) failSound.play(.2f);
            if ((world.stageClear || world.finished) && !wasClear) flipSound.play(.4f);
        }
        lastAttach = world.attaches;
        lastRepel = world.repels;
        lastFlip = world.flips;
        lastDeaths = world.deaths;
        wasClear = world.stageClear || world.finished;
        lastRotation = world.player.rotationTo;
    }

    public void screenshot(String path) {
        Pixmap pix =
                Pixmap.createFromFrameBuffer(
                        0,
                        0,
                        Gdx.graphics.getBackBufferWidth(),
                        Gdx.graphics.getBackBufferHeight());
        PixmapIO.writePNG(Gdx.files.absolute(path), pix, -1, true);
        pix.dispose();
    }

    private void beginShapes(OrthographicCamera c) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(c.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void rect(float x, float y, float w, float h, Color c) {
        shapes.setColor(c);
        shapes.rect(x, y, w, h);
    }

    private void rounded(float x, float y, float w, float h, float r, Color c) {
        rect(x + r, y, w - 2 * r, h, c);
        rect(x, y + r, w, h - 2 * r, c);
        circle(x + r, y + r, r, c);
        circle(x + w - r, y + r, r, c);
        circle(x + r, y + h - r, r, c);
        circle(x + w - r, y + h - r, r, c);
    }

    private void circle(float x, float y, float r, Color c) {
        shapes.setColor(c);
        shapes.circle(x, y, r, 32);
    }

    private void line(float x1, float y1, float x2, float y2, float width, Color c) {
        shapes.setColor(c);
        shapes.rectLine(x1, y1, x2, y2, width);
    }

    private void beginText(OrthographicCamera c) {
        batch.setProjectionMatrix(c.combined);
        batch.begin();
    }

    private void text(String s, float x, float y, float scale, Color c) {
        font.getData().setScale(scale);
        font.setColor(c);
        font.draw(batch, s, x, y);
    }

    private void center(String s, float x, float y, float scale, Color c) {
        font.getData().setScale(scale);
        layout.setText(font, s);
        float width = layout.width;
        // 폰트 아틀라스의 마지막 칸에 남는 투명 여백을 중앙 정렬에서 제외한다.
        if (!s.isEmpty()) {
            BitmapFont.Glyph last = font.getData().getGlyph(s.charAt(s.length() - 1));
            if (last != null) width -= (last.width - last.xadvance) * scale;
        }
        text(s, x - width / 2, y, scale, c);
    }

    private Color poleColor(int pole) {
        return pole == 1 ? red : blue;
    }

    private String poleName(int pole) {
        return pole == 1 ? "N" : "S";
    }

    private void drawGameWorld() {
        Level l = world.level;
        float left = camera.position.x - 600, bottom = camera.position.y - 375;
        Color bg =
                l.index == 0
                        ? Color.valueOf("FFF5DD")
                        : l.index == 1 ? Color.valueOf("E4F4F5") : Color.valueOf("F5EAF4");
        beginShapes(camera);
        rect(left, bottom, 1200, 750, bg);
        circle(left + 980, bottom + 570, 58, Color.valueOf("F6D87A"));
        for (int i = 0; i < 10; i++) {
            float xx = i * 330 - left * .15f;
            circle(xx, 30, 180, Color.valueOf("DDE9D6"));
            circle(xx + 110, 5, 140, Color.valueOf("CCDCCB"));
            float cloudY = 470 + (i % 3) * 70;
            circle(xx + 40, cloudY, 28, Color.valueOf("FFFCF4"));
            circle(xx + 75, cloudY + 15, 37, Color.valueOf("FFFCF4"));
            circle(xx + 112, cloudY, 28, Color.valueOf("FFFCF4"));
        }
        // Electrical sea: visually distinct from safe solid platforms.
        rect(left, bottom, 1200, Math.max(0, 58 - bottom), Color.valueOf("D7E5E7"));
        for (float xx = (float) Math.floor(left / 48) * 48; xx < left + 1200; xx += 48) {
            float yy = 47 + 4 * (float) Math.sin(xx * .05f + world.time * 2);
            line(xx, yy, xx + 16, yy + 5, 3, blue);
            line(xx + 16, yy + 5, xx + 30, yy - 3, 3, blue);
        }
        for (Level.Switch s : l.switches) {
            for (Level.Platform p : l.platforms)
                if (p.group == s.group) {
                    line(s.x, s.y - 8, s.x, s.y - 50, 3, yellow);
                    line(s.x, s.y - 50, p.x + p.w / 2, s.y - 50, 3, yellow);
                    line(p.x + p.w / 2, s.y - 50, p.x + p.w / 2, p.y - 8, 3, yellow);
                }
        }
        for (Level.Platform p : l.platforms) {
            rect(p.x + 5, p.y - 33, p.w, 30, Color.valueOf("CAD8DA"));
            rect(p.x, p.y - 28, p.w, 28, ink);
            rect(p.x, p.y - 14, p.w, 14, p.pole == 0 ? mint : poleColor(p.pole));
            for (float xx = p.x + 18; xx < p.x + p.w - 8; xx += 34)
                rect(xx, p.y - 20, 10, 3, mutedInk);
        }
        for (Level.Magnet m : l.magnets) {
            if (m.moving()) {
                line(
                        m.ax + m.w / 2,
                        m.ay + 60,
                        m.bx + m.w / 2,
                        m.by + 60,
                        5,
                        Color.valueOf("ABBFC8"));
                circle(m.ax + m.w / 2, m.ay + 60, 8, ink);
                circle(m.bx + m.w / 2, m.by + 60, 8, ink);
                line(m.x + m.w / 2, m.y + 35, m.x + m.w / 2, m.y + 60, 6, ink);
            }
            rect(m.x + 4, m.y - 4, m.w, 42, Color.valueOf("C5D2D7"));
            rect(m.x, m.y, m.w, 38, ink);
            rect(m.x + 5, m.y + 20, m.w - 10, 13, poleColor(-m.pole));
            rect(m.x + 5, m.y + 5, m.w - 10, 15, poleColor(m.pole));
            for (float xx = m.x + 25; xx < m.x + m.w - 10; xx += 75) {
                rect(xx, m.y + 39, 24, 4, mutedInk);
            }
            // A few faint field marks hint at attraction; collision bounds stay invisible.
            if (Math.abs(world.player.x - (m.x + m.w / 2)) < m.w / 2 + 80
                    && Math.abs(world.player.y + GameWorld.H - m.y) < 95) {
                for (float xx = m.x + 18; xx < m.x + m.w; xx += 32)
                    rect(xx, m.y - 12, 2, 6, poleColor(m.pole));
            }
        }
        for (Level.Hazard h : l.hazards) {
            if (h.moving()) {
                line(h.x + h.w / 2, h.startY, h.x + h.w / 2, h.endY + h.h, 3, mutedInk);
                circle(h.x + h.w / 2, h.y + h.h / 2, h.w / 2 + 4, ink);
                circle(h.x + h.w / 2, h.y + h.h / 2, h.w / 2, red);
                line(h.x + 8, h.y + 8, h.x + h.w - 8, h.y + h.h - 8, 4, yellow);
                line(h.x + 8, h.y + h.h - 8, h.x + h.w - 8, h.y + 8, 4, yellow);
            } else if (h.ceiling) {
                rect(h.x, h.y + 14, h.w, h.h - 14, ink);
                for (float xx = h.x; xx < h.x + h.w; xx += 24) {
                    float end = Math.min(xx + 24, h.x + h.w);
                    shapes.setColor(red);
                    shapes.triangle(xx, h.y + 14, (xx + end) / 2, h.y, end, h.y + 14);
                }
            } else {
                rect(h.x, h.y, h.w, 6, ink);
                for (float xx = h.x; xx < h.x + h.w; xx += 15) {
                    float end = Math.min(xx + 15, h.x + h.w);
                    shapes.setColor(red);
                    shapes.triangle(xx, h.y, (xx + end) / 2, h.y + h.h, end, h.y);
                }
            }
        }
        for (Level.Switch s : l.switches) {
            rect(s.x - 36, s.y, 72, 8, ink);
            rect(s.x - 30, s.y + 8, 60, s.pressed() ? 4 : 9, yellow);
            if (s.flash > 0) circle(s.x, s.y + 32, 5 + s.flash * 3, yellow);
        }
        float ex = l.exitX, ey = l.exitY;
        rect(ex - 37, ey, 74, 100, ink);
        rect(ex - 29, ey + 7, 58, 84, mint);
        rect(ex - 22, ey + 14, 44, 70, Color.valueOf("DBFFF0"));
        circle(ex + 10, ey + 45, 4, mint);
        // 그림만 회전한다. 충돌 영역은 일정한 사각형으로 유지한다.
        float cx = world.player.x, cy = world.player.y + GameWorld.H / 2;
        if (world.player.grounded) {
            shapes.setColor(ink.r, ink.g, ink.b, .14f);
            shapes.ellipse(cx - 22, world.player.y - 5, 44, 9);
        }
        for (int i = 0; i < life.length; i++)
            if (life[i] > 0) {
                Color c = particleColor[i];
                shapes.setColor(c.r, c.g, c.b, Math.max(0, life[i] / .4f));
                shapes.circle(px[i], py[i], 2 + life[i] * 5, 12);
            }
        shapes.identity();
        shapes.translate(cx, cy, 0);
        shapes.rotate(0, 0, 1, -world.player.rotation);
        if (world.player.grounded && landingPulse > 0)
            shapes.scale(1 + landingPulse, 1 - landingPulse, 1);
        rect(-18, -25, 36, 50, ink);
        rect(-14, 1, 28, 20, red);
        rect(-14, -21, 28, 20, blue);
        rect(-14, -2, 28, 4, Color.valueOf("F6EFD9"));
        line(-5, 6, -5, 17, 2, Color.WHITE);
        line(-5, 17, 5, 6, 2, Color.WHITE);
        line(5, 6, 5, 17, 2, Color.WHITE);
        line(-5, -6, 5, -6, 2, Color.WHITE);
        line(-5, -6, -5, -12, 2, Color.WHITE);
        line(-5, -12, 5, -12, 2, Color.WHITE);
        line(5, -12, 5, -18, 2, Color.WHITE);
        line(5, -18, -5, -18, 2, Color.WHITE);
        shapes.identity();
        // 표정은 회전하지 않아 캐릭터를 알아보기 쉽다.
        circle(cx - 5, cy, 2.2f, ink);
        circle(cx + 5, cy, 2.2f, ink);
        if (world.attached != null) {
            circle(cx, world.player.y + GameWorld.H + 3, 4, mint);
        }
        shapes.end();
        beginText(camera);
        for (Level.Magnet m : l.magnets) {
            for (float xx = m.x + 38; xx < m.x + m.w - 10; xx += 90)
                center(poleName(m.pole), xx, m.y + 28, .45f, Color.WHITE);
        }
        for (Level.Switch s : l.switches) {
            String next = "N ↔ S";
            for (Level.Platform p : l.platforms)
                if (p.group == s.group) next = poleName(p.pole) + " → " + poleName(-p.pole);
            center(next, s.x, s.y - 57, .43f, ink);
        }
        for (Level.Platform p : l.platforms)
            if (p.pole != 0)
                for (float xx = p.x + 25; xx < p.x + p.w - 10; xx += 58)
                    center(poleName(p.pole), xx, p.y + 8, .46f, Color.WHITE);

        center("GOAL", ex, ey + 129, .48f, mint);
        // Surface-facing polarity label is above the character, away from the face.

        batch.end();
    }

    private String stageName() {
        return "STAGE " + (world.level.index + 1);
    }

    private void drawHud() {
        beginShapes(hud);
        rounded(22, 674, 190, 54, 12, Color.valueOf("FAFBF7"));
        rounded(493, 674, 214, 54, 12, Color.valueOf("FAFBF7"));
        for (int i = 0; i < 3; i++) {
            circle(554 + i * 46, 701, 7, i <= world.level.index ? mint : Color.valueOf("D4DEDC"));
            if (i < 2) rect(565 + i * 46, 699.5f, 25, 3, Color.valueOf("D4DEDC"));
        }
        rounded(1122, 674, 56, 54, 12, ink);
        rect(1141, 690, 6, 22, Color.WHITE);
        rect(1153, 690, 6, 22, Color.WHITE);
        if (failureTime > 0) rounded(405, 625, 390, 38, 10, Color.valueOf("FDE3DD"));
        shapes.end();
        beginText(hud);
        center(stageName(), 117, 726, .7f, ink);
        if (failureTime > 0) center(failureText, 600, 655, .5f, red);

        batch.end();
    }

    private void panel(float x, float y, float w, float h) {
        beginShapes(hud);
        rect(0, 0, 1200, 750, new Color(ink.r, ink.g, ink.b, .45f));
        rounded(x + 6, y - 7, w, h, 18, new Color(0, 0, 0, .12f));
        rounded(x, y, w, h, 18, Color.valueOf("FAFBF7"));
        rounded(x + 28, y + h - 28, 48, 5, 2, mint);
        shapes.end();
    }

    private void drawTitle() {
        beginShapes(hud);
        rect(0, 0, 1200, 750, Color.valueOf("FFF5DD"));
        circle(1040, 630, 100, Color.valueOf("F6D87A"));
        circle(830, -90, 370, Color.valueOf("DDE9D6"));
        circle(1160, -30, 300, Color.valueOf("CCDCCB"));
        rounded(76, 275, 330, 64, 14, ink);
        rounded(76, 195, 330, 64, 14, Color.valueOf("F6E8C9"));
        rounded(76, 115, 330, 64, 14, Color.valueOf("F6E8C9"));
        // 타이틀에서도 한 바퀴 도는 점프를 보여준다.
        float t = (uiTime * .5f) % 1f;
        float jumpY = 270 + 4 * 190 * t * (1 - t);
        rounded(655, 223, 205, 30, 8, ink);
        rect(662, 244, 191, 9, blue);
        rounded(940, 223, 190, 30, 8, ink);
        rect(947, 244, 176, 9, red);
        shapes.identity();
        shapes.translate(766, jumpY + 48, 0);
        shapes.rotate(0, 0, 1, -360 * t);
        rounded(-36, -52, 72, 104, 9, ink);
        rect(-29, 3, 58, 42, red);
        rect(-29, -45, 58, 42, blue);
        rect(-29, -3, 58, 6, Color.valueOf("FFF5DD"));
        circle(-10, 0, 4, Color.WHITE);
        circle(10, 0, 4, Color.WHITE);
        shapes.identity();
        for (int i = 0; i < 7; i++)
            circle(697 + i * 20, 280 + (float) Math.sin(i * .45f) * 95, 2, blue);
        shapes.end();
        beginText(hud);
        text("MAGNET HOP", 78, 628, .62f, mint);
        text("자석의 모험", 78, 568, 1.75f, ink);
        text("한 바퀴 점프, 발끝에서 달라지는 힘.", 78, 449, .64f, mutedInk);
        text("ENTER", 100, 325, .42f, Color.valueOf("B8D2D1"));
        center("시작", 241, 334, .79f, Color.WHITE);
        text("H", 100, 245, .42f, mutedInk);
        center("도움말", 241, 254, .79f, ink);
        text("Q", 100, 165, .42f, mutedInk);
        center("나가기", 241, 174, .79f, ink);
        center("S  높이", 758, 216, .56f, blue);
        center("N  낮게", 1034, 216, .56f, red);
        text("← → / A D  이동     Space  점프", 78, 92, .46f, mutedInk);
        text("M  소리 " + (muted ? "꺼짐" : "켜짐") + "     Q  종료", 78, 60, .38f, mutedInk);
        batch.end();
    }

    private void drawPause() {
        panel(330, 230, 540, 310);
        beginText(hud);
        center("잠깐 쉬어가기", 600, 493, 1.05f, ink);
        center("준비되면 다시 뛰어볼까요?", 600, 430, .58f, mutedInk);
        center("ENTER / ESC   계속하기", 600, 370, .68f, mint);
        center("R   스테이지 처음부터", 600, 321, .55f, ink);
        center("BACKSPACE   타이틀로", 600, 279, .48f, mutedInk);
        batch.end();
    }

    private void drawHelp() {
        panel(170, 135, 860, 470);
        beginText(hud);
        text("작은 자석 사용 설명서", 205, 572, 1f, ink);
        text("01   머리는 N, 발끝은 S. 점프하면 360도 돌아 원래 자세로 착지.", 205, 500, .62f, ink);
        text("02   발끝 S와 같은 S 바닥은 밀어 올려 높이 점프합니다.", 205, 451, .62f, ink);
        text("03   N 바닥은 발끝 S를 끌어당겨 낮고 짧게 점프합니다.", 205, 402, .62f, ink);
        text("04   머리 N은 S 천장에 붙습니다. 이동하고 Space로 내려오세요.", 205, 353, .62f, ink);
        text("05   노란 버튼은 위에서 내려와 밟아야 옆 바닥의 극이 바뀝니다.", 205, 304, .62f, ink);
        text("R  스테이지 처음부터     M  소리 " + (muted ? "끄기 상태" : "켜기 상태"), 205, 240, .55f, mutedInk);
        center("H / ESC   돌아가기", 600, 189, .6f, mint);
        batch.end();
    }

    private void drawClear() {
        panel(280, 215, 640, 320);
        beginText(hud);
        center(stageName() + " CLEAR!", 600, 470, 1.12f, mint);
        center("ENTER   다음 스테이지", 600, 360, .72f, ink);
        center("BACKSPACE   타이틀로", 600, 285, .48f, mutedInk);
        batch.end();
    }

    private void drawFinish() {
        panel(280, 215, 640, 320);
        beginText(hud);
        center("GAME CLEAR!", 600, 470, 1.3f, mint);
        center("ENTER   처음부터 다시 시작", 600, 360, .72f, ink);
        center("ESC   타이틀로", 600, 285, .48f, mutedInk);
        batch.end();
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
        attachSound.dispose();
        jumpSound.dispose();
        flipSound.dispose();
        failSound.dispose();
    }
}
