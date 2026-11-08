package adventure;

/** 점프의 위아래 흔들림 대신 착지한 지형 높이를 따라가는 카메라. */
public class CameraFollow {
    public float x, y;
    private float groundHeight;

    public void reset(GameWorld world) {
        groundHeight = world.level.startY;
        x = horizontalTarget(world);
        y = verticalTarget(world);
    }

    public void update(GameWorld world, float delta) {
        // 점프 중에는 기준 높이를 유지한다. 이동 자석도 불필요하게 따라 흔들지 않는다.
        if (world.player.grounded) groundHeight = world.player.y;
        float targetX = horizontalTarget(world);
        float targetY = verticalTarget(world);
        float horizontalBlend = 1 - (float) Math.exp(-8 * delta);
        float verticalBlend = 1 - (float) Math.exp(-4 * delta);
        x += (targetX - x) * horizontalBlend;
        y += (targetY - y) * verticalBlend;
    }

    private float horizontalTarget(GameWorld world) {
        // 방향키를 놓거나 반대로 눌러도 앞보기 거리가 바뀌지 않는다.
        return clamp(world.player.x + 140, 600, Math.max(600, world.level.width - 600));
    }

    private float verticalTarget(GameWorld world) {
        // 높은 점프도 화면 안에 들어오도록 바닥보다 140 위를 중심으로 둔다.
        return clamp(groundHeight + 140, 260, world.level.height - 375);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
