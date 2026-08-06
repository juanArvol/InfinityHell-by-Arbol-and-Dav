package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Movimiento con gravedad — aplica aceleración gravitacional cada frame.
 *
 * El proyectil cae progresivamente a medida que avanza. Ideal para:
 *   - Flechas
 *   - Bombas lanzadas
 *   - Bolas de fuego
 *   - Proyectiles "pesados"
 *
 * Nota: BulletPhysics tiene su propio flag hasGravity que invoca
 * Physics2D.applyGravity() en Bullet.update(). GravityMovement es para
 * movimiento controlado externamente, con valor configurable independiente
 * del Physics2D base. Se puede usar en lugar de o además del flag nativo.
 *
 * Para la mayoría de casos, declarar gravityValue > 0 en ProjectileData
 * es suficiente — BulletFactory lo detecta automáticamente y compone el
 * movimiento con GravityMovement. GravityMovement directo es útil cuando
 * se quiere un valor de gravedad distinto al del ProjectileData, o para
 * composición dinámica en runtime.
 *
 * Uso:
 *   ProjectileMovement m = new GravityMovement(0.5);  // caída lenta
 *   ProjectileMovement m = new GravityMovement(1.5);  // caída pesada
 */
public final class GravityMovement implements ProjectileMovement {

    private final double gravity;
    private static final double MAX_FALL_SPEED = 20.0;

    /**
     * @param gravity aceleración gravitacional por frame (positivo = hacia abajo)
     */
    public GravityMovement(double gravity) {
        this.gravity = gravity;
    }

    @Override
    public void tick(Bullet bullet) {
        double vy = bullet.getPhysics().getYspeed();
        double newVy = Math.min(vy + gravity, MAX_FALL_SPEED);
        bullet.getPhysics().setYspeed(newVy);
    }

    /**
     * GravityMovement no tiene estado interno mutable.
     * La misma instancia puede compartirse entre proyectiles del mismo tipo.
     */
    @Override
    public boolean isStateless() {
        return true;
    }
}
