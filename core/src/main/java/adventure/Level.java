package adventure;

import java.util.ArrayList;
import java.util.List;

/** Each platform's visible top face is its actual magnetic contact surface. */
public final class Level {
    public static final class Platform {
        public final float x, y, w;
        public final int initialPole, group;
        public int pole;

        Platform(float x, float y, float w, int pole, int group) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.pole = this.initialPole = pole;
            this.group = group;
        }
    }

    public static final class Magnet {
        public final float ax, ay, bx, by, w, speed;
        public final int pole;
        public float x, y, dx, dy, travel, wait;
        public int direction = 1;

        Magnet(float x, float y, float w, int pole, float bx, float by, float speed) {
            ax = x;
            ay = y;
            this.bx = bx;
            this.by = by;
            this.w = w;
            this.pole = pole;
            this.speed = speed;
            reset();
        }

        public boolean moving() {
            return speed > 0;
        }

        void reset() {
            x = ax;
            y = ay;
            dx = dy = travel = 0;
            wait = 1;
            direction = 1;
        }

        void update(float dt) {
            float ox = x, oy = y, len = (float) Math.hypot(bx - ax, by - ay);
            if (moving()) {
                if (wait > 0) wait = Math.max(0, wait - dt);
                else {
                    travel += direction * speed * dt;
                    if (travel >= len) {
                        travel = len;
                        direction = -1;
                        wait = 1;
                    }
                    if (travel <= 0) {
                        travel = 0;
                        direction = 1;
                        wait = 1;
                    }
                    x = ax + (bx - ax) * travel / len;
                    y = ay + (by - ay) * travel / len;
                }
            }
            dx = x - ox;
            dy = y - oy;
        }
    }

    public static final class Switch {
        public final float x, y;
        public final int group;
        public boolean occupied;
        public float flash;

        // 실제 전환이 일어난 직후에만 눌림 모션을 보여준다.
        public boolean pressed() {
            return flash > .8f;
        }

        Switch(float x, float y, int g) {
            this.x = x;
            this.y = y;
            group = g;
        }
    }

    public static final class Hazard {
        public float x, y;
        public final float w, h, startY, endY, speed;
        public final boolean ceiling;
        private int direction = 1;

        Hazard(float x, float y, float w, float h) {
            this(x, y, w, h, true, y, 0);
        }

        Hazard(float x, float y, float w, float h, boolean ceiling, float endY, float speed) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.ceiling = ceiling;
            this.startY = y;
            this.endY = endY;
            this.speed = speed;
        }

        public boolean moving() {
            return speed > 0;
        }

        public boolean rising() {
            return direction > 0;
        }

        void reset() {
            y = startY;
            direction = 1;
        }

        void update(float delta) {
            if (!moving()) return;
            y += direction * speed * delta;
            if (y >= endY) {
                y = endY;
                direction = -1;
            }
            if (y <= startY) {
                y = startY;
                direction = 1;
            }
        }
    }

    public final List<Hazard> hazards = new ArrayList<Hazard>();

    public final List<Platform> platforms = new ArrayList<Platform>();
    public final List<Magnet> magnets = new ArrayList<Magnet>();
    public final List<Switch> switches = new ArrayList<Switch>();
    public final int index;
    public final String name, subtitle;
    public float width, height, startX = 100, startY = 80, exitX, exitY;

    private Level(int i, String n, String s) {
        index = i;
        name = n;
        subtitle = s;
    }

    private void floor(float x, float y, float w, int pole) {
        floor(x, y, w, pole, -1);
    }

    private void floor(float x, float y, float w, int pole, int g) {
        platforms.add(new Platform(x, y, w, pole, g));
    }

    private void roof(float x, float y, float w, int pole) {
        magnets.add(new Magnet(x, y, w, pole, x, y, 0));
    }

    public void reset() {
        for (Platform p : platforms) p.pole = p.initialPole;
        for (Magnet m : magnets) m.reset();
        for (Hazard h : hazards) h.reset();
        for (Switch s : switches) {
            s.occupied = false;
            s.flash = 0;
        }
    }

    // 맵 좌표는 수업의 blocks.add(...)처럼 직접 읽고 수정할 수 있게 작성한다.
    public static Level create(int stage) {
        if (stage == 0) return createFirstStage();
        if (stage == 1) return createSecondStage();
        return createThirdStage();
    }

    private static Level createFirstStage() {
        Level l = new Level(0, "STAGE 1", "보통 점프, 높은 점프, 낮은 점프");
        l.width = 4520;
        l.height = 1000;
        // 1. 안전한 첫 틈 → S 바닥으로 높은 턱.
        l.floor(0, 80, 350, 0);
        l.floor(440, 80, 170, 0);
        l.floor(610, 80, 150, -1);
        l.floor(940, 270, 260, 0);
        l.floor(1200, 270, 250, 1);
        l.floor(1520, 270, 140, 1);
        l.floor(1660, 270, 260, 0);
        l.hazards.add(new Hazard(1190, 400, 450, 30));
        // 2. 높이가 조금씩 달라지는 징검다리. 점프 후 잠깐 멈출 자리도 둔다.
        l.floor(2010, 270, 170, 0);
        l.floor(2270, 330, 190, 0);
        l.floor(2550, 270, 250, 0);
        l.hazards.add(new Hazard(2650, 270, 45, 28, false, 270, 0));
        l.floor(2890, 270, 180, 0);
        l.floor(3160, 270, 180, 0);
        // 3. 다시 높은 턱, 낮은 가시 통로, 마지막 보통 점프.
        l.floor(3340, 270, 170, -1);
        l.floor(3690, 460, 180, 0);
        l.floor(3870, 460, 180, 1);
        l.floor(4120, 460, 120, 1);
        l.floor(4240, 460, 70, 0);
        l.floor(4390, 460, 130, 0);
        l.hazards.add(new Hazard(3880, 590, 340, 30));
        l.exitX = 4470;
        l.exitY = 460;
        return l;
    }

    private static Level createSecondStage() {
        Level l = new Level(1, "STAGE 2", "자석을 타고, 틈을 뛰고, 위험물의 때를 기다려요");
        l.width = 5270;
        l.height = 1300;
        // 1. 첫 이동 자석. 도착 후에도 직접 낮은 점프와 높은 점프를 한다.
        l.floor(0, 80, 330, 0);
        l.floor(330, 80, 170, -1);
        l.floor(880, 150, 300, 1);
        l.floor(1250, 150, 220, 1);
        l.floor(1470, 150, 150, -1);
        l.floor(1810, 360, 370, 0);
        l.magnets.add(new Magnet(400, 400, 220, -1, 740, 400, 110));
        l.hazards.add(new Hazard(1010, 280, 450, 30));
        // 2. 점프 구간과 위아래로 왕복하는 위험물. 왼쪽에 대기할 넓은 바닥.
        l.floor(2260, 360, 350, 0);
        l.floor(2700, 360, 180, 0);
        l.hazards.add(new Hazard(2440, 370, 38, 38, false, 650, 110));
        l.floor(2970, 420, 200, 0);
        l.floor(3250, 360, 160, 0);
        l.floor(3410, 360, 170, -1);
        // 3. 두 번째 자석은 위쪽으로도 이동한다. 끝에서 내려 다음 바닥으로.
        l.magnets.add(new Magnet(3480, 680, 220, -1, 3910, 740, 120));
        l.floor(4010, 490, 230, 0);
        l.hazards.add(new Hazard(4120, 490, 45, 28, false, 490, 0));
        l.floor(4320, 490, 200, 0);
        l.floor(4610, 490, 180, 0);
        l.floor(4790, 490, 160, -1);
        l.floor(5120, 680, 150, 0);
        l.exitX = 5210;
        l.exitY = 680;
        return l;
    }

    private static Level createThirdStage() {
        Level l = new Level(2, "STAGE 3", "버튼 위로 점프해서 내려오면 옆 바닥의 극이 바뀝니다");
        l.width = 4930;
        l.height = 1600;
        // 1. 버튼을 위에서 밟아 S 발판을 만든다.
        l.floor(0, 80, 300, 0);
        l.floor(300, 80, 160, 1, 0);
        l.floor(620, 290, 120, 0);
        l.floor(740, 290, 230, -1, 1);
        l.floor(1050, 290, 310, 1);
        l.floor(1360, 290, 150, 1, 2);
        l.floor(1840, 510, 260, 0);
        l.floor(2100, 510, 140, 1, 3);
        l.hazards.add(new Hazard(790, 420, 450, 30));
        l.roof(1470, 620, 550, -1);
        l.switches.add(new Switch(235, 80, 0));
        l.switches.add(new Switch(680, 290, 1));
        l.switches.add(new Switch(1300, 290, 2));
        l.switches.add(new Switch(2020, 510, 3));
        // 2. 높은 턱 다음에는 N으로 바꿔 낮은 통로를 지난다.
        l.floor(2400, 700, 180, 0);
        l.floor(2580, 700, 200, -1, 4);
        l.floor(2850, 700, 140, 1);
        l.floor(2990, 700, 260, 0);
        l.switches.add(new Switch(2470, 700, 4));
        l.hazards.add(new Hazard(2640, 830, 340, 30));
        // 3. 배운 점프를 이어 쓰는 마지막 정원. 추가 스위치 퍼즐은 강요하지 않는다.
        l.floor(3340, 700, 200, 0);
        l.floor(3630, 760, 200, 0);
        l.floor(3920, 700, 280, 0);
        l.hazards.add(new Hazard(4020, 700, 45, 28, false, 700, 0));
        l.floor(4290, 700, 180, 0);
        l.floor(4460, 700, 160, -1);
        l.floor(4780, 890, 150, 0);
        l.exitX = 4870;
        l.exitY = 890;
        return l;
    }
}
