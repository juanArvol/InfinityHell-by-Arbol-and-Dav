package Game.Engine.Simulation.Systems;

import Game.Engine.Simulation.Storage.EntityStore;

/**
 * Sistema de simulación que procesa entidades en batch.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── CONTRATO ─────────────────────────────────────────────────────────────
 *
 * update(entityStore, deltaTime) se llama una vez por frame desde
 * SimulationPipeline. El sistema accede directamente a PrimitiveStorage
 * para procesar los datos de todas las entidades en loops densos.
 *
 * ── ORDEN DE EJECUCIÓN ───────────────────────────────────────────────────
 *
 * El orden de los sistemas es crítico. SimulationPipeline ejecuta en
 * el orden de registro:
 *
 * 1. ProjectileMovementSystem   — behaviors configuran acceleration
 * 2. AccelerationSystem          — velocity += acceleration * dt
 * 3. MovementSystem              — position += velocity * dt
 * 4. LifetimeSystem              — lifetime -= dt, marcar expired
 * 5. CollisionSystem             — detectar y resolver colisiones
 * 6. ProjectileBehaviorSystem    — callbacks de dominio
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 *
 * Los sistemas NO son thread-safe.
 * Responsabilidad de sincronización es del SimulationPipeline.
 *
 * ── EXTENSIBILIDAD ───────────────────────────────────────────────────────
 *
 * Dominios pueden registrar sus propios sistemas:
 *
 * pipeline.register(new EnemyAISystem());
 * pipeline.register(new ProjectileBehaviorSystem());
 *
 * Los sistemas solo necesitan acceso a EntityStore — no se acoplan
 * entre sí.
 */
public interface SimulationSystem {

    /**
     * Procesa todas las entidades relevantes para este sistema.
     *
     * @param entityStore store con los datos de simulación
     * @param deltaTime tiempo del simulation step en segundos
     */
    void update(EntityStore entityStore, double deltaTime);

    /**
     * Nombre del sistema para profiling y debugging.
     *
     * @return nombre descriptivo (ej: "MovementSystem", "LifetimeSystem")
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
