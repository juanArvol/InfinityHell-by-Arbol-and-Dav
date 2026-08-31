package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Movimiento con gravedad — aplica aceleración gravitacional cada frame.
 *
 * ── HRFC — Consolidación Final de Kinetic Physics ────────────────────────
 * ── HRFC FASE 2 — Corrección de Unidades ─────────────────────────────────
 * ── HRFC Phase 3 — Temporal Migration ─────────────────────────────────────
 * ── HRFC DOD Migration — Simplificación Drag ──────────────────────────────
 *
 * MIGRACIÓN TEMPORAL:
 *   GravityMovement ahora integra aceleración con deltaTime: Δv = a × dt
 *
 *   UNIDADES:
 *     gravity → units/s² (aceleración gravitacional)
 *     deltaTime → s (segundos)
 *     velocity → units/s
 *
 * El proyectil cae progresivamente a medida que avanza. Ideal para:
 *   - Flechas
 *   - Bombas lanzadas
 *   - Bolas de fuego
 *   - Proyectiles "pesados" (MetheorBullet)
 *
 * ── Drag Aerodinámico (SIMPLIFICADO EN DOD) ──────────────────────────────
 *
 * En DOD migration, dragCoefficient y effectiveArea fueron consolidados
 * en un único campo: drag (coeficiente simplificado).
 *
 * Fórmula simplificada:
 *   F_drag = drag × v²
 *
 * (asumimos área = 1.0, coeficiente incorporado en drag)
 *
 * La velocidad terminal emerge del balance:
 *   F_gravity = m × g
 *   F_drag = drag × v²
 *
 * Los proyectiles configuran su resistencia con:
 *   bullet.getPhysics().setDrag(0.0001);   // muy aerodinámico
 *   bullet.getPhysics().setDrag(0.001);    // alta resistencia
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
 *   ProjectileMovement m = new GravityMovement(30.0);  // caída lenta (0.5 @ 60 FPS)
 *   ProjectileMovement m = new GravityMovement(90.0);  // caída pesada (1.5 @ 60 FPS)
 */
public final class GravityMovement implements ProjectileMovement {

    private final double gravity;  // aceleración gravitacional en units/s²

    /**
     * ── HRFC Phase 3 — Temporal Migration ─────────────────────────────────
     *
     * @param gravity aceleración gravitacional en units/s² (positivo = hacia abajo)
     */
    public GravityMovement(double gravity) {
        this.gravity = gravity;
    }

    @Override
    public void tick(Bullet bullet, double dt) {
        var physics = bullet.getPhysics();

        // ── 1. Aceleración gravitatoria (constante, independiente de masa) ──
        double a_gravity = gravity;

        // ── 2. Resistencia aerodinámica (simplificada) ───────────────────
        // En DOD migration, dragCoefficient y effectiveArea fueron consolidados
        // en un único campo: drag (coeficiente simplificado).
        //
        // Fórmula simplificada: F_drag = drag × v²
        // (asumimos área = 1.0, Cd incorporado en drag)
        double vy = physics.getYspeed();
        double speed = Math.abs(vy);
        double mass = physics.getMass();
        double drag = physics.getDrag();

        double dragForce = drag * speed * speed;

        // Dirección del drag: opuesta a la velocidad
        double dragDirection = (vy >= 0) ? -1.0 : 1.0;

        // ── 3. Aceleración por drag (F / m) ──────────────────────────────
        double a_drag = (dragForce / mass) * dragDirection;

        // ── 4. Integración de aceleración neta ───────────────────────────
        // HRFC Phase 3: Δv = a × dt (temporal integration)
        double a_net = a_gravity + a_drag;
        double newVy = vy + (a_net * dt);

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
