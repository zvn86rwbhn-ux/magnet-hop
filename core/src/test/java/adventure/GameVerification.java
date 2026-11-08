package adventure;

public final class GameVerification {
    private static int checks;

    private static void check(boolean b, String s) {
        if (!b) throw new AssertionError(s);
        System.out.println("PASS " + s);
        checks++;
    }

    private static void ticks(GameWorld w, int count, int move, boolean jump) {
        for (int i = 0; i < count; i++) w.step(move, jump && i == 0);
    }

    private static float apex(int pole) {
        GameWorld w = new GameWorld();
        w.level.platforms.get(0).pole = pole;
        float max = w.player.y;
        w.step(0, true);
        for (int i = 0; i < 180; i++) {
            w.step(0, false);
            max = Math.max(max, w.player.y);
        }
        return max - 80;
    }

    public static void main(String[] args) {
        GameWorld w = new GameWorld();
        check(w.player.topPole == 1, "head N and feet S at rest");
        ticks(w, 60, 0, true);
        check(
                Math.abs(w.player.rotation - 360) < .01 && w.player.topPole == 1,
                "exact full turn with unchanged poles");
        ticks(w, 90, 0, false);
        check(w.player.grounded && w.player.y == 80, "lands feet S down");
        float high = apex(-1), normal = apex(0), low = apex(1);
        check(
                high > normal * 2 && normal > low * 2,
                "S high / neutral normal / N low jump heights");
        System.out.printf("HEIGHTS S=%.1f neutral=%.1f N=%.1f%n", high, normal, low);
        w = new GameWorld();
        w.player.x = 1410;
        w.player.y = 270;
        w.support = w.level.platforms.get(4);
        w.support.pole = 0;
        ticks(w, 120, 1, true);
        check(w.deaths > 0, "normal jump hits low spikes: N low jump has a real purpose");
        w.begin(1);
        ticks(w, 300, 0, false);
        check(
                w.level.magnets.get(0).x > w.level.magnets.get(0).ax,
                "moving magnet starts without checkpoint or reaching it");
        w.paused = true;
        float t = w.time, mx = w.level.magnets.get(0).x;
        ticks(w, 180, 1, true);
        check(w.time == t && w.level.magnets.get(0).x == mx, "pause freezes all simulation");
        w.paused = false;
        w.retry();
        check(
                w.player.x == w.level.startX && w.player.y == w.level.startY,
                "retry always returns to stage start");
        w.begin(2);
        int previousFlips = w.flips;
        ticks(w, 60, 1, false);
        check(w.flips == previousFlips, "walking sideways does not activate button");
        w.player.x = 235;
        ticks(w, 150, 0, true);
        check(
                w.flips == previousFlips + 1 && w.level.platforms.get(1).pole == -1,
                "descending landing activates adjacent floor once");
        ticks(w, 150, 0, false);
        check(w.flips == previousFlips + 1, "standing on button never retriggers");
        w.retry();
        check(w.level.platforms.get(1).pole == 1, "retry resets all floor switches");
        w = new GameWorld();
        w.begin(1);
        Level.Hazard movingHazard = w.level.hazards.get(1);
        ticks(w, 80, 0, false);
        check(movingHazard.y > movingHazard.startY, "moving hazard starts immediately");
        w.paused = true;
        float hazardY = movingHazard.y;
        ticks(w, 120, 0, false);
        check(movingHazard.y == hazardY, "pause freezes moving hazard");
        w.paused = false;
        w.retry();
        check(movingHazard.y == movingHazard.startY, "retry resets moving hazard");
        w.begin(2);
        int before = w.flips;
        w.player.x = 235;
        w.step(0, true);
        check(w.flips == before, "upward jump cannot trigger button");
        ticks(w, 150, 0, false);
        check(w.flips == before + 1, "first landing triggers button");
        ticks(w, 150, 0, true);
        check(w.flips == before + 2, "new jump and landing can toggle button back");
        w = new GameWorld();
        w.begin(2);
        w.player.x = 235;
        w.player.y = 80.1f;
        w.player.vy = -1;
        w.player.grounded = false;
        w.step(0, false);
        check(w.flips == 1, "subpixel descent still counts as landing on button");
        w = new GameWorld();
        RoutePilot p = new RoutePilot();
        int total = 0, clears = 0;
        while (!w.finished && total < 120 * 300) {
            if (w.stageClear) {
                System.out.printf(
                        "CLEAR %d time=%.2f jumps=%d high=%d low=%d attach=%d flips=%d%n",
                        w.level.index + 1,
                        w.time,
                        w.jumps,
                        w.boosts,
                        w.lowJumps,
                        w.attaches,
                        w.flips);
                w.advance();
                clears++;
            } else {
                p.decide(w);
                w.step(p.direction, p.jump);
                total++;
                if (w.deaths > 0)
                    throw new AssertionError(
                            "route death stage="
                                    + w.level.index
                                    + " phase="
                                    + p.phase
                                    + " flips="
                                    + w.flips);
            }
            if (total % 1200 == 0 && !w.finished)
                System.out.printf(
                        "TRACE stage=%d phase=%d x=%.1f y=%.1f deaths=%d%n",
                        w.level.index + 1, p.phase, w.player.x, w.player.y, w.deaths);
        }
        check(w.finished && clears == 3, "new maps full input-only completion");
        check(w.deaths == 0, "reference route without death");
        check(
                w.boosts >= 6
                        && w.lowJumps >= 5
                        && w.attaches >= 3
                        && w.flips >= 5
                        && w.jumps >= 35,
                "full run uses surface jumps, ceiling capture, and floor switches");
        w.restartCampaign();
        check(
                w.completed == 0 && w.level.index == 0 && w.jumps == 0,
                "restart resets campaign and counters");
        for (int stage = 0; stage < 3; stage++) {
            w = new GameWorld();
            w.begin(stage);
            for (int i = 0; i < 1000 && w.deaths == 0; i++) w.step(1, false);
            check(
                    w.deaths == 1 && w.player.x == w.level.startX,
                    "fall returns to stage start " + stage);
        }
        System.out.println("VERIFIED " + checks + " assertions");
    }
}
