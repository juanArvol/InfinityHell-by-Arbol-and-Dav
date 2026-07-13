package Game.Items.Types.Bullets;

import Game.Engine.GameMath.Physics.Types.Physics2D;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;

/**
 * Física de balas.
 *
 * Implementación específica del Game que extiende Physics2D del Engine.
 * Vive en Game.Items.Types.Bullets porque es una regla del juego
 * (comportamiento de proyectiles), no infraestructura reutilizable.
 *
 * MIGRADO DESDE: Game.Engine.GameMath.Physics.Implementation.BulletPhysics
 * RAZÓN: BulletPhysics es lógica específica del Game (proyectiles de Infinity
 * Hell). No es infraestructura genérica reutilizable por cualquier juego.
 *
 * La bala gestiona su propia gravedad (isGravityManagedExternally() = true),
 * indicando a CollisionsSystem que no aplique gravedad externa.
 */
public class BulletPhysics extends Physics2D {

    private final boolean hasGravity;

    public BulletPhysics(double xSpeed, double ySpeed, boolean hasGravity, double gravity) {
        super(gravity);
        this.hasGravity = hasGravity;
        setMass(1);
        velocity.setX(xSpeed);
        velocity.setY(ySpeed);
    }

    /** CollisionsSystem NO aplica gravedad a este objeto. */
    @Override
    public boolean isGravityManagedExternally() { return true; }

    public void update(Vector2D position) {
        if (hasGravity) applyGravity(false);
        updateMoves(position);
    }

    public void accelerate(double fx, double fy) { addForce(fx, fy); }

    public double getYspeed()          { return velocity.getY(); }
    public double getXspeed()          { return velocity.getX(); }
    public void   setYspeed(double ys) { velocity.setY(ys); }
    public void   setXspeed(double xs) { velocity.setX(xs); }

    @Override public void stopX()        { velocity.setX(0); }
    @Override public void stopY()        { velocity.setY(0); }
    @Override public void stopVelocity() { stopX(); stopY(); }
}
