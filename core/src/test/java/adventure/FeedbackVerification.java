package adventure;

/** 사용자 피드백의 회귀 검사: 버튼 그림과 카메라의 실제 동작. */
public class FeedbackVerification {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("PASS " + message);
    }

    public static void main(String[] args) {
        GameWorld world = new GameWorld();
        world.begin(2);
        Level.Switch button = world.level.switches.get(0);
        for (int i = 0; i < 60; i++) world.step(1, false);
        check(
                button.occupied && !button.pressed() && world.flips == 0,
                "walking onto button changes neither pole nor pressed appearance");
        world.player.x = button.x;
        world.step(0, true);
        while (!world.player.grounded) world.step(0, false);
        check(
                button.pressed() && world.flips == 1,
                "landing changes pole and pressed appearance together");
        for (int i = 0; i < 35; i++) world.step(0, false);
        check(!button.pressed() && world.flips == 1, "pressed animation ends without retriggering");

        world = new GameWorld();
        CameraFollow camera = new CameraFollow();
        camera.reset(world);
        float initialY = camera.y;
        for (int i = 0; i < 150; i++) {
            world.step(0, i == 0);
            camera.update(world, GameWorld.STEP);
            checkQuiet(camera.y == initialY, "normal jump must not move camera vertically");
        }
        check(camera.y == initialY, "camera stays level throughout jump and landing on same floor");

        // 화면 중앙 부근에서도 점프 높이에 흔들리지 않는지 확인한다.
        world.player.x = 1800;
        world.player.y = 270;
        world.player.grounded = true;
        for (int i = 0; i < 600; i++) camera.update(world, GameWorld.STEP);
        float stableX = camera.x;
        float stableY = camera.y;
        world.player.grounded = false;
        for (int i = 0; i < 120; i++) {
            world.player.y = 270 + 250 * (float) Math.sin(Math.PI * i / 120);
            camera.update(world, GameWorld.STEP);
        }
        check(Math.abs(camera.y - stableY) < .01f, "high jump does not bob vertical camera");
        check(
                Math.abs(camera.x - stableX) < .01f,
                "no camera shift without player horizontal movement");

        world = new GameWorld();
        camera.reset(world);
        RoutePilot pilot = new RoutePilot();
        int frames = 0;
        while (!world.finished && frames++ < 120 * 300) {
            if (world.stageClear) {
                world.advance();
                camera.reset(world);
                continue;
            }
            pilot.decide(world);
            world.step(pilot.direction, pilot.jump);
            camera.update(world, GameWorld.STEP);
            checkQuiet(
                    world.player.y + GameWorld.H < camera.y + 375,
                    "camera clips head stage=" + world.level.index);
            checkQuiet(
                    world.player.y > camera.y - 375,
                    "camera clips feet stage=" + world.level.index);
        }
        check(
                world.finished && world.deaths == 0,
                "whole route stays vertically visible with stable camera");
        System.out.println("FEEDBACK VERIFIED");
    }

    private static void checkQuiet(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
