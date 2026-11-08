package adventure;

/** 수업의 GameWorld처럼 입력 결과, 중력, 충돌과 스테이지 규칙을 처리한다. */
public class GameWorld {
    public static final float STEP = 1f / 120f;
    public static final float SPEED = 270;
    public static final float GRAVITY = 1500;
    public static final float W = 32;
    public static final float H = 48;
    public static final float RANGE = 42;
    public static final float NORMAL_JUMP = 620;
    public static final float N_JUMP = 420;
    public static final float S_JUMP = 930;
    public static final float TURN_TIME = .42f;

    public final GameCharacter player = new GameCharacter();
    public Level level;
    public float time, stageTime, lock, coyote, buffer, eventTime;
    public int deaths, completed, attaches, repels, flips, rides, jumps, boosts, lowJumps;
    public boolean paused, finished, stageClear;
    public Level.Platform support;
    public Level.Magnet attached, excluded;
    public String event = "";
    private boolean landedThisStep;

    public GameWorld() {
        begin(0);
    }

    public void begin(int stage) {
        level = Level.create(stage);
        stageTime = 0;
        paused = false;
        finished = false;
        stageClear = false;
        spawn();
    }

    private void spawn() {
        player.reset(level.startX, level.startY);
        lock = 0;
        buffer = 0;
        coyote = .1f;
        attached = null;
        excluded = null;
        support = level.platforms.get(0);
    }

    public void restartCampaign() {
        time = 0;
        deaths = completed = attaches = repels = flips = rides = jumps = boosts = lowJumps = 0;
        begin(0);
    }

    public void retry() {
        if (finished || stageClear) return;
        deaths++;
        level.reset();
        spawn();
        signal("스테이지 처음부터", 1);
    }

    public void advance() {
        if (!stageClear) return;
        completed = Math.max(completed, level.index + 1);
        if (level.index == 2) {
            finished = true;
            stageClear = false;
        } else {
            begin(level.index + 1);
        }
    }

    public void signal(String message, float duration) {
        event = message;
        eventTime = duration;
    }

    public int floorPole() {
        return support == null ? 0 : support.pole;
    }

    public float jumpSpeed() {
        if (floorPole() == -1) return S_JUMP;
        if (floorPole() == 1) return N_JUMP;
        return NORMAL_JUMP;
    }

    /** 입력 → 장치 → 점프 → 이동/충돌 → 버튼 → 위험물 → 출구 순서. */
    public void step(int move, boolean jump) {
        if (paused || finished || stageClear) return;
        time += STEP;
        stageTime += STEP;
        eventTime = Math.max(0, eventTime - STEP);
        lock = Math.max(0, lock - STEP);
        player.updateRotation(STEP);
        updateObjects();
        updateJump(jump);
        landedThisStep = false;
        player.vx = move * SPEED;
        if (attached != null) moveAlongMagnet();
        else {
            moveX();
            moveY();
            checkMagneticField();
        }
        player.x = Math.max(W / 2, Math.min(level.width - W / 2, player.x));
        checkSwitches();
        if (checkHazards()) return;
        checkGoal();
    }

    private void updateObjects() {
        // 장치는 플레이어 위치와 관계없이 시작부터 움직인다.
        for (Level.Magnet magnet : level.magnets) magnet.update(STEP);
        for (Level.Hazard hazard : level.hazards) hazard.update(STEP);
        for (Level.Switch button : level.switches) button.flash = Math.max(0, button.flash - STEP);
        if (excluded != null
                && (Math.abs(player.y + H - excluded.y) > RANGE + 25
                        || player.x < excluded.x - 30
                        || player.x > excluded.x + excluded.w + 30)) {
            excluded = null;
        }
    }

    private void updateJump(boolean jump) {
        // 착지 직전 입력과 발판 끝의 짧은 오차를 허용한다.
        buffer = jump ? .13f : Math.max(0, buffer - STEP);
        coyote = player.grounded ? .1f : Math.max(0, coyote - STEP);
        if (buffer <= 0 || (attached == null && coyote <= 0)) return;
        if (attached != null) {
            excluded = attached;
            attached = null;
            player.jump(-85);
            lock = .35f;
        } else {
            player.jump(jumpSpeed());
            jumps++;
            if (floorPole() == -1) boosts++;
            if (floorPole() == 1) lowJumps++;
        }
        coyote = 0;
        buffer = 0;
    }

    private void moveX() {
        player.x += player.vx * STEP;
    }

    private void moveY() {
        float previousY = player.y;
        boolean wasGrounded = player.grounded;
        player.vy -= GRAVITY * STEP;
        player.y += player.vy * STEP;
        player.grounded = false;
        checkCeilings(previousY);
        if (player.vy > 0) return;
        for (Level.Platform platform : level.platforms) {
            if (!overlapsX(platform.x, platform.w)) continue;
            // 위에서 발판 높이를 통과했을 때만 착지한다.
            if (previousY < platform.y - .2f || player.y > platform.y) continue;
            player.y = platform.y;
            player.vy = 0;
            player.grounded = true;
            landedThisStep = !wasGrounded;
            support = platform;
            player.rotation = player.rotationTo;
            player.rotationTime = 0;
            break;
        }
    }

    private boolean overlapsX(float left, float width) {
        return player.x + W / 2 > left && player.x - W / 2 < left + width;
    }

    private void checkCeilings(float previousY) {
        if (player.vy <= 0) return;
        for (Level.Magnet magnet : level.magnets) {
            if (!overlapsX(magnet.x, magnet.w)) continue;
            if (previousY + H > magnet.y || player.y + H < magnet.y) continue;
            player.y = magnet.y - H - .1f;
            player.vy = 0;
            if (magnet.pole == player.topPole) repel(magnet);
            break;
        }
    }

    private void checkMagneticField() {
        if (player.grounded || player.rotationTime > 0 || lock > 0) return;
        Level.Magnet nearest = null;
        float distance = RANGE;
        for (Level.Magnet magnet : level.magnets) {
            float gap = magnet.y - player.y - H;
            if (magnet == excluded
                    || player.x < magnet.x + W / 2
                    || player.x > magnet.x + magnet.w - W / 2) continue;
            if (gap >= -8 && gap < distance) {
                nearest = magnet;
                distance = gap;
            }
        }
        if (nearest == null) return;
        if (nearest.pole == player.topPole) repel(nearest);
        else {
            attached = nearest;
            player.y = nearest.y - H;
            player.vy = 0;
            attaches++;
            if (nearest.moving()) rides++;
        }
    }

    private void repel(Level.Magnet magnet) {
        excluded = magnet;
        player.vy = Math.min(player.vy, -200);
        lock = .35f;
        repels++;
    }

    private void moveAlongMagnet() {
        player.x += attached.dx + player.vx * STEP;
        player.y = attached.y - H;
        if (player.x < attached.x + W / 2 || player.x > attached.x + attached.w - W / 2) {
            excluded = attached;
            attached = null;
            player.vy = -35;
            lock = .2f;
        }
    }

    private void checkSwitches() {
        for (Level.Switch button : level.switches) {
            boolean onButton =
                    player.grounded
                            && Math.abs(player.y - button.y) < 1
                            && Math.abs(player.x - button.x) < 27;
            // 걷기/옆면 접촉은 무시. 공중에서 내려와 버튼 위에 착지해야 한다.
            if (landedThisStep && onButton && !button.occupied) {
                for (Level.Platform platform : level.platforms) {
                    if (platform.group == button.group) platform.pole = -platform.pole;
                }
                flips++;
                button.flash = 1;
                signal("착지! 옆 바닥의 극이 바뀌었어요", 1.2f);
            }
            button.occupied = onButton;
        }
    }

    private boolean checkHazards() {
        for (Level.Hazard hazard : level.hazards) {
            if (overlapsX(hazard.x, hazard.w)
                    && player.y + H > hazard.y
                    && player.y < hazard.y + hazard.h) {
                retry();
                signal("위험물에 닿았어요. 다시 도전!", 1.5f);
                return true;
            }
        }
        if (player.y < -100) {
            retry();
            return true;
        }
        return false;
    }

    private void checkGoal() {
        if (player.grounded
                && Math.abs(player.x - level.exitX) < 35
                && Math.abs(player.y - level.exitY) < 1) {
            stageClear = true;
            completed = Math.max(completed, level.index + 1);
        }
    }
}
