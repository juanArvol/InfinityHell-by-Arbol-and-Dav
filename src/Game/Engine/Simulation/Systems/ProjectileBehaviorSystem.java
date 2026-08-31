package Game.Engine.Simulation.Systems;

import Game.Engine.Simulation.Storage.EntityStore;
import Game.Engine.Simulation.Storage.PrimitiveStorage;
import Game.Items.Types.Bullets.Definition.Bullet;

/**
 * Sistema que ejecuta comportamientos de dominio de proyectiles.
 *
 * ── HRFC — Projectile DOD Migration ──────────────────────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 *
 * Este sistema es el bridge entre DOD (Game.Engine) y dominio (Game.Bullets).
 *
 * 1. Consulta flags de PrimitiveStorage para detectar eventos
 * 2. Llama comportamientos de dominio (BulletBehavior.onUpdate, onExpire)
 * 3. Detecta bullets pendientes de destrucción
 * 4. Flush del world container
 *
 * ── COMPONENTES LEÍDOS ───────────────────────────────────────────────────
 *
 * Este sistema es HÍBRIDO — lee FLAGS directamente de PrimitiveStorage:
 *   - FLAGS (read) — detecta FLAG_EXPIRED
 *   - FLAGS (write) — limpia FLAG_EXPIRED después de callback
 *
 * Los behaviors pueden escribir cualquier componente a través de Bullet handle.
 *
 * NO tiene REQUIRED_COMPONENTS porque es un bridge de lógica de dominio,
 * no un sistema DOD batch puro.
 *
 * ── ORDEN DE EJECUCIÓN ───────────────────────────────────────────────────
 *
 * DEBE ejecutarse DESPUÉS de todos los sistemas DOD:
 *
 * 1. ProjectileMovementSystem   — configura acceleration
 * 2. AccelerationSystem          — integra velocity
 * 3. MovementSystem              — integra position
 * 4. LifetimeSystem              — decrementa lifetime
 * 5. CollisionSystem             — detecta colisiones
 * 6. ProjectileBehaviorSystem (ESTE) — callbacks de dominio
 *
 * ── FLAGS CONSUMIDOS ─────────────────────────────────────────────────────
 *
 * FLAG_EXPIRED (bit 1) — LifetimeSystem marcó lifetime agotado
 *   → llama behavior.onExpire()
 *   → limpia el flag después
 *
 * FLAG_DEAD (bit 0) — kill() manual o colisión
 *   → bullet.isPendingDestruction() retorna true
 *   → world.flush() lo elimina
 *
 * ── ARQUITECTURA HÍBRIDA ─────────────────────────────────────────────────
 *
 * Los datos hot (position, velocity, lifetime) fueron procesados en batch
 * por sistemas DOD. Ahora ejecutamos la lógica de juego específica que
 * NO puede generalizarse (explosiones, spawns, efectos visuales).
 *
 * Esto preserva el balance:
 *   - Hot data → batch processing
 *   - Lógica de juego → comportamientos individuales
 */
public final class ProjectileBehaviorSystem implements SimulationSystem {

    private static final int FLAG_EXPIRED = 1 << 1;

    private final Iterable<Bullet> bullets;

    /**
     * Constructor.
     *
     * @param bullets iterable de todas las bullets activas (ej: world.getBullets())
     */
    public ProjectileBehaviorSystem(Iterable<Bullet> bullets) {
        this.bullets = bullets;
    }

    @Override
    public void update(EntityStore entityStore, double deltaTime) {
        PrimitiveStorage storage = entityStore.getStorage();
        int[] flags = storage.flags();

        // Procesar comportamientos de cada bullet
        for (Bullet bullet : bullets) {
            if (!bullet.shouldSimulate()) {
                continue; // skip bullets que no deben procesarse
            }

            // Obtener handle para acceder a flags
            int idx = bullet.getSimulationHandle().index();

            // Verificar si expiró por lifetime
            if ((flags[idx] & FLAG_EXPIRED) != 0) {
                bullet.getBehavior().onExpire(bullet, bullet.getProjectileContext());
                bullet.getBulletLife().kill(); // Marcar FLAG_DEAD
                flags[idx] &= ~FLAG_EXPIRED; // clear flag
            }

            // Ejecutar comportamiento por frame
            if (bullet.shouldSimulate()) {
                bullet.getBehavior().onUpdate(bullet);
            }
        }

        // Flush destruidas del world container
        // (esto debe implementarse en WorldObjectsContainer)
        // world.flushDestroyed();
    }

    @Override
    public String name() {
        return "ProjectileBehaviorSystem";
    }
}
