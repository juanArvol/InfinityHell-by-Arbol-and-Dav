package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;
import Game.Items.Types.Bullets.ResettableMovement;

/**
 * Movimiento compuesto — combina múltiples estrategias de movimiento.
 *
 * Aplica cada estrategia en orden. Las modificaciones de una estrategia
 * son visibles para las siguientes en el mismo frame.
 *
 * Ejemplos de combinaciones útiles:
 *   - HomingMovement + SinusoidalMovement → misil serpenteante
 *   - AcceleratingMovement + GravityMovement → proyectil pesado que acelera
 *   - OrbitalMovement + SinusoidalMovement → órbita ondulante
 *
 * Uso:
 *   ProjectileMovement m = new CompositeMovement(
 *       new HomingMovement(target, 90, 8.0),
 *       new SinusoidalMovement(3.0, 0.2)
 *   );
 *
 *   // O via andThen() (equivalente):
 *   ProjectileMovement m = new HomingMovement(target, 90, 8.0)
 *       .andThen(new SinusoidalMovement(3.0, 0.2));
 *
 * ── Pool / ResettableMovement (§12) ───────────────────────────────────────
 *
 * CompositeMovement implementa ResettableMovement si ALGUNO de sus componentes
 * es stateful, para que el pool pueda resetear el composite completo de forma
 * segura. Si todos los componentes son stateless, el composite también lo es.
 *
 * ── Gravity detection (§17/§18) ───────────────────────────────────────────
 *
 * containsGravity(movement) en BulletFactory debe poder inspeccionar los
 * componentes del composite para determinar si alguno es GravityMovement.
 * getComponents() expone el array interno solo para lectura desde la factory.
 */
public final class CompositeMovement implements ResettableMovement {

    private final ProjectileMovement[] movements;

    public CompositeMovement(ProjectileMovement... movements) {
        if (movements == null || movements.length == 0) {
            throw new IllegalArgumentException("CompositeMovement necesita al menos una estrategia");
        }
        this.movements = movements;
    }

    @Override
    public void tick(Bullet bullet, double dt) {
        for (ProjectileMovement m : movements) {
            m.tick(bullet, dt);
        }
    }

    // ── Estado ────────────────────────────────────────────────────────────

    /**
     * Un CompositeMovement es stateless solo si TODOS sus componentes son stateless.
     * Si cualquier componente tiene estado, el composite también lo tiene.
     */
    @Override
    public boolean isStateless() {
        for (ProjectileMovement m : movements) {
            if (!m.isStateless()) return false;
        }
        return true;
    }

    /**
     * Resetea todos los componentes que implementen ResettableMovement.
     * Los componentes stateless no necesitan reset.
     *
     * Llamado por Bullet.resetState() (via ProjectilePool.acquire()) cuando
     * el composite es reutilizado en un nuevo ciclo de vida.
     */
    @Override
    public void reset() {
        for (ProjectileMovement m : movements) {
            if (m instanceof ResettableMovement rm) {
                rm.reset();
            }
        }
    }

    // ── Introspección (solo para BulletFactory.containsGravity) ──────────

    /**
     * Retorna los componentes de este composite.
     *
     * Uso exclusivo para inspección en BulletFactory.statsFrom() —
     * determinar si el composite contiene un GravityMovement para
     * calcular correctamente BulletStats.hasGravity() en la UI.
     *
     * No usar fuera de BulletFactory.
     *
     * @return array de componentes (solo lectura — no modificar)
     */
    public ProjectileMovement[] getComponents() {
        return movements;
    }
}
