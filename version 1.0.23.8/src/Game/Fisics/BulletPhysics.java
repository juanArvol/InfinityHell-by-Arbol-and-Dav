package Game.Fisics;

import GameMath.Vector2D;

public class BulletPhysics extends Physics {

    private final boolean hasGravity;

    public BulletPhysics(
            double xSpeed,
            double ySpeed,
            boolean hasGravity,
            double gravity
    ) {
        super(gravity);
        this.hasGravity = hasGravity;
        this.gravity = gravity;
        setMass(1);

        velocity.setX(xSpeed);
        velocity.setY(ySpeed);
    }

    public double getYspeed() {
        return velocity.getY();
    }

    public double getXspeed() {
        return velocity.getX();
    }

    public void setYspeed(double ySpeed) {
        velocity.setY(ySpeed);
    }

    public void setXspeed(double xSpeed) {
        velocity.setX(xSpeed);
    }

    /** Actualiza la posición según la física */
    public void update(Vector2D position) {
        if (hasGravity) {
            applyGravity(false);
        }
        updateMoves(position);
    }

    /** Aplicar fuerzas externas */
    public void accelerate(double fx, double fy) {
        addForce(fx, fy);
    }

    /** Detener movimiento */
    public void stopX() {
        velocity.setX(0);
    }

    public void stopY() {
        velocity.setY(0);
    }

    public void stopVelocity() {
        stopX();
        stopY();
    }
}