package adventure;

/** 검증 전용 조작자. 상태를 읽고 좌우/점프 입력만 보낸다. 좌표를 변경하지 않는다. */
public final class RoutePilot {
    private static final int WALK = 0,
            JUMP = 1,
            WAIT_MAGNET = 2,
            CAPTURE = 3,
            RIDE = 4,
            LAND = 5,
            WAIT_HAZARD = 6;
    private static final float[][][] ROUTES = {
        {
            {WALK, 300}, {JUMP, 500}, {WALK, 700}, {JUMP, 1030}, {WALK, 1410}, {JUMP, 1580},
            {WALK, 1880}, {JUMP, 2070}, {WALK, 2140}, {JUMP, 2340}, {WALK, 2420}, {JUMP, 2590},
            {WALK, 2610}, {JUMP, 2740}, {WALK, 2760}, {JUMP, 2950}, {WALK, 3030}, {JUMP, 3220},
            {WALK, 3460}, {JUMP, 3750}, {WALK, 4010}, {JUMP, 4170}, {WALK, 4270}, {JUMP, 4460}
        },
        {
            {WALK, 460},
            {WAIT_MAGNET, 0},
            {CAPTURE, 0},
            {RIDE, 910},
            {LAND, 0},
            {WALK, 1140},
            {JUMP, 1290},
            {WALK, 1590},
            {JUMP, 1900},
            {WALK, 2140},
            {JUMP, 2320},
            {WAIT_HAZARD, 0},
            {WALK, 2380},
            {JUMP, 2540},
            {WALK, 2570},
            {JUMP, 2760},
            {WALK, 2840},
            {JUMP, 3050},
            {WALK, 3130},
            {JUMP, 3320},
            {WALK, 3530},
            {WAIT_MAGNET, 1},
            {CAPTURE, 0},
            {RIDE, 4060},
            {LAND, 0},
            {WALK, 4070},
            {JUMP, 4200},
            {WALK, 4200},
            {JUMP, 4380},
            {WALK, 4480},
            {JUMP, 4670},
            {WALK, 4900},
            {JUMP, 5200}
        },
        {
            {WALK, 235},
            {JUMP, 235},
            {WALK, 420},
            {JUMP, 680},
            {WALK, 940},
            {JUMP, 1100},
            {WALK, 1300},
            {JUMP, 1300},
            {WALK, 1450},
            {CAPTURE, 0},
            {RIDE, 1940},
            {LAND, 0},
            {WALK, 2020},
            {JUMP, 2020},
            {WALK, 2200},
            {JUMP, 2470},
            {WALK, 2740},
            {JUMP, 2910},
            {WALK, 3210},
            {JUMP, 3400},
            {WALK, 3500},
            {JUMP, 3710},
            {WALK, 3790},
            {JUMP, 3960},
            {WALK, 3980},
            {JUMP, 4130},
            {WALK, 4160},
            {JUMP, 4350},
            {WALK, 4570},
            {JUMP, 4860}
        }
    };
    public int phase, seenStage = -1, direction;
    public boolean jump;
    private boolean launched;

    private int toward(float x, float goal) {
        return Math.abs(x - goal) < 3 ? 0 : x < goal ? 1 : -1;
    }

    private void next() {
        phase++;
        launched = false;
    }

    public void decide(GameWorld world) {
        direction = 0;
        jump = false;
        if (world.level.index != seenStage) {
            seenStage = world.level.index;
            phase = 0;
            launched = false;
        }
        GameCharacter player = world.player;
        float[][] route = ROUTES[seenStage];
        if (phase >= route.length) {
            direction = toward(player.x, world.level.exitX);
            return;
        }
        int action = (int) route[phase][0];
        float target = route[phase][1];
        switch (action) {
            case WALK:
                direction = toward(player.x, target);
                if (direction == 0) next();
                break;
            case JUMP:
                direction = toward(player.x, target);
                if (!launched && player.grounded) {
                    jump = true;
                    launched = true;
                } else if (launched && player.grounded) next();
                break;
            case WAIT_MAGNET:
                Level.Magnet magnet = world.level.magnets.get((int) target);
                if (magnet.x <= magnet.ax + 5 && magnet.wait > .5f) next();
                break;
            case CAPTURE:
                direction = 1;
                if (!launched && player.grounded) {
                    jump = true;
                    launched = true;
                }
                if (world.attached != null) {
                    next();
                    direction = 0;
                }
                break;
            case RIDE:
                if (world.attached != null) {
                    float goal = Math.min(target, world.attached.x + world.attached.w * .85f);
                    direction = toward(player.x, goal);
                    if (player.x >= target - 6) {
                        jump = true;
                        direction = 0;
                        next();
                    }
                }
                break;
            case LAND:
                if (player.grounded) next();
                break;
            case WAIT_HAZARD:
                for (Level.Hazard hazard : world.level.hazards) {
                    if (hazard.moving() && hazard.y > 570 && hazard.rising()) next();
                }
                break;
            default:
                throw new AssertionError("unknown action");
        }
    }
}
