package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Movimiento con gravedad — aplica aceleración gravitacional cada frame.
 *
 * ── HRFC — Consolidación Final de Kinetic Physics ────────────────────────
 *
 * El proyectil cae progresivamente a medida que avanza. Ideal para:
 *   - Flechas
 *   - Bombas lanzadas
 *   - Bolas de fuego
 *   - Proyectiles "pesados" (MetheorBullet)
 *
 * ── Drag Aerodinámico ────────────────────────────────────────────────────
 *
 * GravityMovement ahora aplica resistencia aerodinámica para producir
 * velocidad terminal natural, coherente con Physics2D.applyGravity().
 *
 * La velocidad terminal emerge del balance:
 *   F_gravity = m × g
 *   F_drag = 0.5 × ρ × Cd × A × v²
 *
 * Los proyectiles pueden configurar sus propiedades aerodinámicas:
 *   bullet.getPhysics().setEffectiveArea(0.3);      // proyectil pequeño
 *   bullet.getPhysics().setDragCoefficient(0.15);   // muy aerodinámico
 *
 * ── Diferencia con Physics2D.applyGravity() ──────────────────────────────
 *
 * BulletPhysics.isGravityManagedExternally() → true, por lo que
 * CollisionsSystem NO llama applyGravity() automáticamente.
 *
 * GravityMovement gestiona la gravedad explícitamente para proyectiles,
 * permitiendo valores configurables independientes del entorno.
 *
 * Para la mayoría de casos, declarar gravityValue > 0 en ProjectileData
 * es suficiente — BulletFactory lo detecta automáticamente y compone el
 * movimiento con GravityMovement.
 *
 * Uso:
 *   ProjectileMovement m = new GravityMovement(0.5);  // caída lenta
 *   ProjectileMovement m = new GravityMovement(1.5);  // caída pesada
 */
public final class GravityMovement implements ProjectileMovement {

    private final double gravity;

    /**
     * @param gravity aceleración gravitacional por frame (positivo = hacia abajo)
     */
    public GravityMovement(double gravity) {
        this.gravity = gravity;
    }

    @Override
    public void tick(Bullet bullet) {
        var physics = bullet.getPhysics();

        // ── 1. Aceleración gravitatoria (constante, independiente de masa) ──
        double a_gravity = gravity;

        // ── 2. Resistencia aerodinámica ──────────────────────────────────
        // F_drag = 0.5 × ρ × Cd × A × v²
        double vy = physics.getYspeed();
        double speed = Math.abs(vy);
        double mass = physics.getMass();
        double rho = physics.getMediumDensity();
        double cd = physics.getDragCoefficient();
        double area = physics.getEffectiveArea();

        double dragForce = 0.5 * rho * cd * area * speed * speed;

        // Dirección del drag: opuesta a la velocidad
        double dragDirection = (vy >= 0) ? -1.0 : 1.0;

        // ── 3. Aceleración por drag (F / m) ──────────────────────────────
        double a_drag = (dragForce / mass) * dragDirection;

        // ── 4. Integración de aceleración neta ───────────────────────────
        double a_net = a_gravity + a_drag;
        double newVy = vy + a_net;

        physics.setYspeed(newVy);
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
