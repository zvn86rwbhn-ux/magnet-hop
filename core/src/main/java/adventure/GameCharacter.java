package adventure;

/** 캐릭터의 위치, 속도와 점프 회전을 관리한다. */
public class GameCharacter {
    public float x, y, vx, vy;
    public float rotation, rotationFrom, rotationTo, rotationTime;
    public boolean grounded;
    public final int topPole = 1; // 머리 N, 발끝 S

    public void reset(float startX, float startY) {
        x = startX;
        y = startY;
        vx = 0;
        vy = 0;
        rotation = 0;
        rotationFrom = 0;
        rotationTo = 0;
        rotationTime = 0;
        grounded = true;
    }

    public void jump(float speed) {
        vy = speed;
        grounded = false;
        startTurn();
    }

    public void startTurn() {
        rotationFrom = rotation;
        rotationTo = rotation + 360;
        rotationTime = GameWorld.TURN_TIME;
    }

    public void updateRotation(float delta) {
        if (rotationTime <= 0) return;
        rotationTime = Math.max(0, rotationTime - delta);
        float progress = 1 - rotationTime / GameWorld.TURN_TIME;
        float eased = progress * progress * (3 - 2 * progress);
        rotation = rotationFrom + (rotationTo - rotationFrom) * eased;
    }
}
