package Game.Fisics;

import GameMath.Vector2D;

/**
 * Física de balas.
 *
 * FIX BUG-005: sobreescribe isGravityManagedExternally() → retorna true.
 * Esto indica a CollisionsSystem que NO aplique gravedad externamente,
 * ya que Bullet.update() gestiona su propia gravedad vía hasGravity flag.
 */
public class BulletPhysics extends Physics {

    private final boolean hasGravity;

    public BulletPhysics(double xSpeed, double ySpeed, boolean hasGravity, double gravity) {
        super(gravity);
        this.hasGravity = hasGravity;
        this.gravity    = gravity;
        setMass(1);
        velocity.setX(xSpeed);
        velocity.setY(ySpeed);
    }

    /**
     * FIX BUG-005: la bala gestiona su propia gravedad en Bullet.update().
     * CollisionsSystem debe ignorar la gravedad para este objeto.
     */
    @Override
    public boolean isGravityManagedExternally() {
        return true;
    }

    public double getYspeed()          { return velocity.getY(); }
    public double getXspeed()          { return velocity.getX(); }
    public void   setYspeed(double ys) { velocity.setY(ys); }
    public void   setXspeed(double xs) { velocity.setX(xs); }

    public void update(Vector2D position) {
        if (hasGravity) applyGravity(false);
        updateMoves(position);
    }

    public void accelerate(double fx, double fy) { addForce(fx, fy); }

    @Override
    public void stopX()        { velocity.setX(0); }
    @Override
    public void stopY()        { velocity.setY(0); }
    @Override
    public void stopVelocity() { stopX(); stopY(); }
}
