package Game.Engine.World.Physics.Quantum;

import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.QuantumProperties;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.RelationType;

/**
 * Catálogo de relaciones cuánticas — colapso de función de onda.
 *
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Este catálogo agrupa las relaciones que describen fenómenos cuánticos:
 * la superposición y el colapso de la función de onda por proximidad.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Estas relaciones no ejecutan comportamiento. Describen fenómenos.
 * El procedimiento matemático vive exclusivamente en el evaluador especializado.
 *
 * ── RELACIONES INCLUIDAS ──────────────────────────────────────────────────
 *   QUANTUM_WAVE_COLLAPSE → colapso de la función de onda por proximidad (PLANCK)
 *
 * ── ORIGEN ────────────────────────────────────────────────────────────────
 * Migrado desde ExtensibilityRelations (HRFC-022).
 * HRFC-024 lo ubica en su catálogo correcto por dominio cuántico.
 */
public final class QuantumRelations {

    private QuantumRelations() {}

    /**
     * Cuando dos objetos con función de onda activa se acercan lo suficiente,
     * la función de onda de ambos colapsa progresivamente hacia 0.
     *
     * Fenómeno: PLANCK — emisión/colapso cuántico
     * Evaluador: PlanckEvaluator (con restricción MAX_DISTANCE activa)
     * Restricción: los objetos deben tener QuantumProperties.WAVE_FUNCTION.
     * Prioridad 200 — se evalúa después de todos los demás fenómenos del frame.
     */
    public static final PhysicalRelation QUANTUM_WAVE_COLLAPSE = PhysicalRelation.builder()
        .name("quantum_wave_collapse")
        .relationType(RelationType.PLANCK)
        .participating(QuantumProperties.WAVE_FUNCTION, QuantumProperties.QUANTUM_SPIN)
        .constraint(RelationConstraint.maxDistance(16.0))
        .constraint(RelationConstraint.propertyPresent(QuantumProperties.WAVE_FUNCTION))
        .priority(200)
        .build();

    /**
     * Todas las relaciones cuánticas.
     *
     * @return array con las relaciones cuánticas.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] { QUANTUM_WAVE_COLLAPSE };
    }
}
