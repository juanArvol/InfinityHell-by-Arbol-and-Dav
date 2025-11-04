package Game.Gameplay.Aimm;

import math.Vector2D;

public class AimDirection {
    private Vector2D direction;

    public AimDirection(double x, double y) {
        this.direction = new Vector2D(x, y).normalize();
    }

    public Vector2D getDirection() {
        return direction;
    }

    public void setDirection(double x, double y) {
        direction.setX(x);
        direction.setY(y);
        direction = direction.normalize();
    }
    public Vector2D getScaledDirection(double speed) {
        return direction.scale(speed);
    }
}
