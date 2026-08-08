package Game.Items.Types.Bullets.BulletComport;

import Game.Engine.Physics.KineticPhysics.Types.Physics2D;

/**
 * Física de proyectiles.
 *
 * Almacena y expone la velocidad del proyectil. La integración de posición
 * la hace PhysicsStepper en Bullet.moveByPhysics().
 *
 * ── HRFC — Weapon & Projectile System ────────────────────────────────────
 *
 * La gravedad ya no se gestiona en BulletPhysics. Antes el constructor
 * recibía hasGravity y gravityValue, y Bullet.update() llamaba:
 *
 *   if (behavior.hasGravity()) getPhysics().applyGravity(false);
 *
 * Esa responsabilidad ahora pertenece a {@link Movement.GravityMovement},
 * una implementación de ProjectileMovement. BulletPhysics es solo un
 * almacén de velocidad con acceso conveniente para los behaviors.
 *
 * ── CollisionsSystem ─────────────────────────────────────────────────────
 *
 * isGravityManagedExternally() retorna true, indicando a CollisionsSystem
 * que NO aplique gravedad externa sobre este objeto (la gestiona el propio
 * sistema de movimiento del proyectil).
 */
public class BulletPhysics extends Physics2D {

    /**
     * @param xSpeed velocidad inicial en X (unidades/frame)
     * @param ySpeed velocidad inicial en Y (unidades/frame)
     */
    public BulletPhysics(double xSpeed, double ySpeed) {
        super(0.0); // gravedad base cero — la gestiona ProjectileMovement
        setMass(1);
        velocity.setX(xSpeed);
        velocity.setY(ySpeed);
    }

    /** CollisionsSystem NO aplica gravedad externa a este objeto. */
    @Override
    public boolean isGravityManagedExternally() { return true; }

    // ── API conveniente para behaviors y movimientos ──────────────────────

    public double getXspeed()          { return velocity.getX(); }
    public double getYspeed()          { return velocity.getY(); }
    public void   setXspeed(double xs) { velocity.setX(xs); }
    public void   setYspeed(double ys) { velocity.setY(ys); }

    public void accelerate(double fx, double fy) { addForce(fx, fy); }

    @Override public void stopX()        { velocity.setX(0); }
    @Override public void stopY()        { velocity.setY(0); }
    @Override public void stopVelocity() { stopX(); stopY(); }
}
