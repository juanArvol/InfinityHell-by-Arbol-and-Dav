package Game.Engine.Simulation.Systems;

import Game.Engine.Simulation.Storage.EntityStore;
import Game.Items.Types.Bullets.Definition.Bullet;

/**
 * Sistema que ejecuta estrategias de movimiento de proyectiles.
 *
 * ── HRFC — Projectile DOD Migration ──────────────────────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 *
 * Ejecuta ProjectileMovement.tick() para cada bullet activa.
 *
 * Las estrategias de movimiento configuran acceleration/velocity:
 * - LinearMovement: no hace nada (velocity constante)
 * - HomingMovement: calcula dirección al target y configura acceleration
 * - GravityMovement: configura gravityScale
 * - SinusoidalMovement: modifica velocity con oscilación perpendicular
 *
 * ── COMPONENTES AFECTADOS ────────────────────────────────────────────────
 *
 * Este sistema es HÍBRIDO — NO procesa PrimitiveStorage directamente.
 * Itera sobre objetos Bullet que internamente escriben:
 *   - ACCELERATION (write) — mayoría de movimientos
 *   - VELOCITY (write) — movimientos con control directo
 *   - GRAVITY_SCALE (write) — gravedad dinámica
 *   - ANGULAR_VELOCITY (write) — rotación visual
 *
 * NO tiene REQUIRED_COMPONENTS porque no lee/escribe arrays directamente.
 * Es un bridge entre lógica de dominio y DOD storage.
 *
 * ── REGLA FUNDAMENTAL ────────────────────────────────────────────────────
 *
 * ProjectileMovement configura acceleration, NO velocity directamente.
 * AccelerationSystem integrará acceleration → velocity.
 *
 * Excepción: movimientos que necesitan control directo de velocity
 * (SinusoidalMovement, BoomerangMovement) pueden modificarla, pero deben
 * poner acceleration = 0 para evitar doble integración.
 *
 * ── ORDEN DE EJECUCIÓN ───────────────────────────────────────────────────
 *
 * DEBE ejecutarse ANTES de AccelerationSystem:
 *
 * 1. ProjectileMovementSystem (ESTE) — configura acceleration
 * 2. AccelerationSystem — integra acceleration → velocity
 * 3. MovementSystem — integra velocity → position
 *
 * ── ARQUITECTURA HÍBRIDA ─────────────────────────────────────────────────
 *
 * Este sistema NO es batch DOD puro — itera sobre objetos Bullet.
 * Sin embargo, las modificaciones que hace (acceleration, gravityScale)
 * luego son procesadas en batch por AccelerationSystem.
 *
 * Es un bridge necesario: las estrategias de movimiento son lógica de
 * dominio que no puede generalizarse en un loop batch (cada bullet tiene
 * su propia estrategia: homing a diferentes targets, sinusoidal con
 * diferentes fases, etc.).
 */
public final class ProjectileMovementSystem implements SimulationSystem {

    private final Iterable<Bullet> bullets;

    /**
     * Constructor.
     *
     * @param bullets iterable de todas las bullets activas (ej: world.getBullets())
     */
    public ProjectileMovementSystem(Iterable<Bullet> bullets) {
        this.bullets = bullets;
    }

    @Override
    public void update(EntityStore entityStore, double deltaTime) {
        // Ejecutar estrategias de movimiento
        for (Bullet bullet : bullets) {
            if (!bullet.shouldSimulate()) {
                continue; // skip bullets que no deben procesarse
            }

            // ProjectileMovement.tick() modifica acceleration/velocity/gravityScale
            bullet.getMovement().tick(bullet, deltaTime);
        }
    }

    @Override
    public String name() {
        return "ProjectileMovementSystem";
    }
}
