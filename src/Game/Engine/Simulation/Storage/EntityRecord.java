package Game.Engine.Simulation.Storage;

import Game.Engine.Simulation.ComponentMask;
import Game.Engine.Simulation.EntityId;

/**
 * Record de metadata de una entidad en el dense storage.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * EntityRecord almacena la metadata necesaria para gestionar una entidad
 * en el sistema de almacenamiento denso:
 *
 *   entityId    → identidad lógica estable
 *   denseIndex  → ubicación física actual en los arrays
 *   generation  → contador para detectar reuso de slots
 *   mask        → qué componentes tiene esta entidad
 *   alive       → si la entidad está activa o fue destruida
 *
 * ── SEPARACIÓN ENTITYID ↔ DENSE INDEX ────────────────────────────────────
 *
 * El denseIndex puede cambiar debido a compactación:
 *
 *   1. Entidad A está en denseIndex=5
 *   2. Entidad B (en denseIndex=10) se destruye
 *   3. Compactación: última entidad (denseIndex=99) se mueve a index=10
 *   4. EntityRecord de esa entidad actualiza denseIndex: 99 → 10
 *
 * El EntityId NUNCA cambia. El EntityRecord mantiene el mapping actualizado.
 *
 * ── GENERATION COUNTER ────────────────────────────────────────────────────
 *
 * Cada vez que un slot se reutiliza, se incrementa su generation:
 *
 *   1. Slot 5: generation=1, entidad A
 *   2. Entidad A se destruye
 *   3. Slot 5: generation=2, entidad B (nuevo)
 *
 * Los SimulationHandle antiguos (generation=1) detectan que son inválidos
 * porque el slot ahora tiene generation=2.
 *
 * ── COMPONENTMASK ────────────────────────────────────────────────────────
 *
 * El mask determina qué arrays de componentes contienen datos válidos para
 * esta entidad:
 *
 *   mask.has(POSITION)  → positionsX[index], positionsY[index] son válidos
 *   mask.has(VELOCITY)  → velocitiesX[index], velocitiesY[index] son válidos
 *   !mask.has(LIFETIME) → lifetimes[index] NO es válido (no usar)
 *
 * Los sistemas usan el mask para filtrar qué entidades procesar:
 *
 *   if (record.mask().matches(movementRequirements)) {
 *       // Procesar esta entidad en MovementSystem
 *   }
 *
 * ── ALIVE FLAG ───────────────────────────────────────────────────────────
 *
 * alive=false marca una entidad como destruida pero aún no compactada.
 * El EntityStore puede hacer batch compaction de múltiples entidades
 * destruidas en un solo pase.
 *
 * ── MUTABLE ──────────────────────────────────────────────────────────────
 *
 * EntityRecord es mutable — el EntityStore actualiza denseIndex y mask
 * cuando es necesario.
 *
 * El EntityId es inmutable y no puede cambiarse después de la construcción.
 */
public final class EntityRecord {

    private final EntityId entityId;
    private int denseIndex;
    private int generation;
    private ComponentMask mask;
    private boolean alive;

    /**
     * Constructor package-private.
     * Solo EntityStore debe crear records.
     *
     * @param entityId ID único de la entidad
     * @param denseIndex índice inicial en el storage denso
     * @param generation generation counter inicial
     * @param mask máscara de componentes inicial
     */
    EntityRecord(EntityId entityId, int denseIndex, int generation, ComponentMask mask) {
        this.entityId = entityId;
        this.denseIndex = denseIndex;
        this.generation = generation;
        this.mask = mask;
        this.alive = true;
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public EntityId entityId() {
        return entityId;
    }

    public int denseIndex() {
        return denseIndex;
    }

    public int generation() {
        return generation;
    }

    public ComponentMask mask() {
        return mask;
    }

    public boolean isAlive() {
        return alive;
    }

    // ── Mutators (package-private) ────────────────────────────────────────

    void setDenseIndex(int newIndex) {
        this.denseIndex = newIndex;
    }

    void incrementGeneration() {
        this.generation++;
    }

    void setMask(ComponentMask newMask) {
        this.mask = newMask;
    }

    void markDead() {
        this.alive = false;
    }

    // ── toString ──────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "EntityRecord[" +
            "id=" + entityId +
            ", index=" + denseIndex +
            ", gen=" + generation +
            ", alive=" + alive +
            ", components=" + mask.count() +
            "]";
    }
}
