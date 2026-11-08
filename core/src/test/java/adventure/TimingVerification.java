package adventure;

public final class TimingVerification {
    public static void main(String[] args) {
        int runs = 0;
        for (int stage = 0; stage < 3; stage++)
            for (float delay : new float[] {0, .2f, .5f, 1, 2, 3, 4, 5, 8, 12}) {
                GameWorld w = new GameWorld();
                w.begin(stage);
                for (int i = 0; i < delay / GameWorld.STEP; i++) w.step(0, false);
                RoutePilot p = new RoutePilot();
                int t = 0;
                while (!w.stageClear && t++ < 120 * 100) {
                    p.decide(w);
                    w.step(p.direction, p.jump);
                }
                if (!w.stageClear || w.deaths > 0)
                    throw new AssertionError(
                            "stage="
                                    + stage
                                    + " delay="
                                    + delay
                                    + " phase="
                                    + p.phase
                                    + " x="
                                    + w.player.x
                                    + " y="
                                    + w.player.y
                                    + " deaths="
                                    + w.deaths);
                runs++;
            }
        System.out.println("TIMING VERIFIED " + runs + " routes");
    }
}
