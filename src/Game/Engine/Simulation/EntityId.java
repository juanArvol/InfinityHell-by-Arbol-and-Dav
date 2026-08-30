package Game.Engine.Simulation;

/**
 * Identificador único e inmutable de una entidad en el sistema de simulación.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * EntityId es la identidad lógica estable de una entidad simulada.
 * Es completamente independiente de la ubicación física en el storage.
 *
 * Regla fundamental:
 *
 *   EntityId     → identidad lógica, NUNCA cambia
 *   dense index  → ubicación física, PUEDE cambiar por compactación
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   EntityId         → identifica la entidad de forma estable
 *   SimulationHandle → resuelve EntityId → dense index + validación
 *   EntityStore      → almacena el mapping y valida handles
 *
 * ── INVARIANTES ──────────────────────────────────────────────────────────
 *
 * 1. Un EntityId NUNCA se reutiliza — aunque la entidad sea destruida,
 *    su ID permanece inválido para siempre en esa sesión de juego.
 *
 * 2. EntityId es inmutable — no hay setters, no puede cambiar después
 *    de la construcción.
 *
 * 3. EntityId no conoce su ubicación física — eso es responsabilidad
 *    del EntityStore.
 *
 * ── GENERACIÓN ───────────────────────────────────────────────────────────
 *
 * Los EntityId se generan mediante un contador global monotónicamente
 * creciente en EntityStore. El ID 0 está reservado como "INVALID".
 *
 * No usar:
 *   - UUID (demasiado pesado, no secuencial, perjudica cache locality)
 *   - dense array index directamente (se rompe con compactación)
 *   - object identity / reference (no serializable, no persistible)
 *
 * ── COMPARACIÓN Y HASHING ────────────────────────────────────────────────
 *
 * EntityId implementa equals/hashCode correctamente para poder usarse
 * como clave en HashMap si algún sistema lo necesita.
 *
 * La comparación es trivial: dos EntityId son iguales si tienen el mismo id.
 *
 * ── EJEMPLO DE USO ───────────────────────────────────────────────────────
 *
 *   // Dominio crea una entidad:
 *   EntityId playerId = entityStore.create();
 *
 *   // Dominio accede a datos de simulación:
 *   SimulationHandle handle = entityStore.getHandle(playerId);
 *   float x = positionsX[handle.index()];
 *   float y = positionsY[handle.index()];
 *
 *   // El handle valida automáticamente si la entidad sigue viva:
 *   if (!handle.isValid()) {
 *       // Entidad fue destruida
 *   }
 *
 * ── SERIALIZACIÓN (FUTURO) ───────────────────────────────────────────────
 *
 * EntityId puede serializarse como un long. Al cargar un savegame,
 * el EntityStore debe restaurar el contador para que los nuevos IDs
 * no colisionen con los ya asignados.
 *
 * Este HRFC NO implementa serialización — es una extensión futura.
 */
public final class EntityId {

    /** ID inválido — reservado para representar "ninguna entidad". */
    public static final EntityId INVALID = new EntityId(0);

    private final long id;

    /**
     * Constructor público para EntityStore.
     *
     * @param id identificador numérico único
     */
    public EntityId(long id) {
        this.id = id;
    }

    /**
     * Retorna el identificador numérico.
     * Expuesto para debugging y logging — NO usar para indexing directo.
     */
    public long id() {
        return id;
    }

    /**
     * Retorna true si este ID es válido (no es INVALID).
     */
    public boolean isValid() {
        return id != 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EntityId)) return false;
        EntityId other = (EntityId) obj;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return id == 0 ? "EntityId[INVALID]" : "EntityId[" + id + "]";
    }
}
