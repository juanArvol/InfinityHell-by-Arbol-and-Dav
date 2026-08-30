package Game.Engine.Simulation.Systems;

import Game.Engine.Simulation.ComponentMask;
import Game.Engine.Simulation.ComponentType;
import Game.Engine.Simulation.EntityId;
import Game.Engine.Simulation.Storage.EntityRecord;
import Game.Engine.Simulation.Storage.EntityStore;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Sistema que gestiona el tiempo de vida de entidades temporales.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * LifetimeSystem procesa entidades con componente LIFETIME:
 *
 * 1. Decrementa lifetime por deltaTime
 * 2. Marca como muertas las entidades cuyo lifetime llegó a cero
 *
 * ── ECUACIÓN ──────────────────────────────────────────────────────────────
 *
 *   lifetime(t + Δt) = lifetime(t) - Δt
 *
 *   if lifetime ≤ 0:
 *       marcar entidad como muerta
 *
 * ── REQUISITOS DE COMPONENTES ────────────────────────────────────────────
 *
 * Requiere:
 *   - LIFETIME (segundos restantes)
 *
 * Entidades sin LIFETIME no son procesadas (entidades permanentes).
 *
 * ── USO TÍPICO ───────────────────────────────────────────────────────────
 *
 * Lifetime se usa para entidades temporales:
 *   - Proyectiles (3-5 segundos de vuelo)
 *   - Efectos visuales (0.5-2 segundos)
 *   - Partículas (0.1-1 segundo)
 *   - Power-ups temporales (10-30 segundos)
 *   - Trampas temporales (5-10 segundos)
 *
 * Entidades permanentes (Player, Boss, Enemy) NO tienen componente LIFETIME.
 *
 * ── DESTRUCCIÓN ──────────────────────────────────────────────────────────
 *
 * Este sistema NO destruye entidades directamente durante update().
 * Solo las marca como muertas mediante store.destroy().
 *
 * La compactación real ocurre posteriormente:
 *   - Si SimulationPipeline.autoCompact = true → automático
 *   - Si no, el caller debe llamar entityStore.compact() manualmente
 *
 * ── ORDEN EN PIPELINE ────────────────────────────────────────────────────
 *
 * LifetimeSystem típicamente se ejecuta DESPUÉS de MovementSystem y
 * ANTES de CollisionSystem:
 *
 *   1. AccelerationSystem
 *   2. MovementSystem
 *   3. LifetimeSystem      ← marca entidades expiradas
 *   4. CollisionSystem     ← ignora entidades muertas
 *
 * ── CALLBACK DE EXPIRACIÓN (FUTURO) ──────────────────────────────────────
 *
 * Este HRFC NO implementa callbacks de expiración.
 * Los dominios que necesiten lógica custom al expirar deben:
 *
 *   a) Registrar un listener en EntityStore (extensión futura)
 *   b) Implementar un sistema propio que detecte lifetime=0 y ejecute lógica
 *   c) Usar el sistema existente de eventos (GameEventBus)
 *
 * ── PERFORMANCE ──────────────────────────────────────────────────────────
 *
 * Loop simple:
 *   - 1 lectura + 1 escritura por entidad con lifetime
 *   - 1 comparación (lifetime <= 0)
 *   - 1 resta (lifetime -= deltaTime)
 *
 * El batch destroy al final puede ser más costoso debido a la búsqueda
 * en HashMap, pero ocurre solo para entidades que expiraron (infrecuente).
 *
 * ── ALLOCATION-FREE (casi) ───────────────────────────────────────────────
 *
 * El loop principal es allocation-free.
 * Solo se allocan objetos cuando hay entidades que destruir:
 *   - ArrayList temporal para recolectar IDs
 *   - Esto solo ocurre cuando lifetime <= 0 (infrecuente)
 *
 * Alternativa futura: pool de ArrayList reutilizables.
 *
 * ── EJEMPLO ──────────────────────────────────────────────────────────────
 *
 *   // Crear proyectil con 3 segundos de vida
 *   ComponentMask mask = ComponentMask.EMPTY
 *       .with(ComponentType.POSITION.id())
 *       .with(ComponentType.VELOCITY.id())
 *       .with(ComponentType.LIFETIME.id());
 *   EntityId id = store.create(mask);
 *
 *   SimulationHandle h = store.getHandle(id);
 *   PrimitiveStorage s = store.getStorage();
 *   s.lifetimes()[h.index()] = 3.0f;  // 3 segundos
 *
 *   // Después de 3 segundos de updates:
 *   // lifetime = 3.0 - 3.0 = 0.0 → entidad marcada muerta → compactada
 */
public final class LifetimeSystem implements SimulationSystem {

    private final ComponentMask requirements;
    private final List<EntityId> toDestroy; // reutilizado entre frames

    public LifetimeSystem() {
        this.requirements = ComponentMask.EMPTY
            .with(ComponentType.LIFETIME.id());
        this.toDestroy = new ArrayList<>();
    }

    @Override
    public void update(EntityStore store, double deltaTime) {
        PrimitiveStorage storage = store.getStorage();

        // Acceso directo a los arrays
        float[] lifetimes = storage.lifetimes();

        int count = store.count();

        // Limpiar lista de destrucción del frame anterior
        toDestroy.clear();

        // Loop denso para decrementar lifetimes
        for (int i = 0; i < count; i++) {
            // Validar que entidad tiene lifetime
            EntityId entityId = store.getEntityAt(i);
            if (entityId == null) continue;

            EntityRecord record = store.getRecord(entityId);
            if (!record.mask().matches(requirements)) continue;

            // Decrementar lifetime
            lifetimes[i] -= deltaTime;

            // Marcar para destrucción si expiró
            if (lifetimes[i] <= 0.0f) {
                toDestroy.add(entityId);
            }
        }

        // Destruir entidades expiradas (batch)
        for (EntityId entityId : toDestroy) {
            store.destroy(entityId);
        }

        // Nota: La compactación real ocurre después si autoCompact está habilitado
    }

    @Override
    public String name() {
        return "LifetimeSystem";
    }

    /**
     * Retorna el número de entidades destruidas en el último update.
     * Útil para debugging y métricas.
     */
    public int getLastDestroyedCount() {
        return toDestroy.size();
    }
}
