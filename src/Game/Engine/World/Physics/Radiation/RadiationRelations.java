package Game.Engine.World.Physics.Radiation;

import Game.Engine.World.Physics.Thermal.ThermalProperties;
import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Radiation.RadiationProperties;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.RelationType;

/**
 * Catálogo de relaciones radiantes — transferencia de radiación y conversión térmica.
 *
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Este catálogo agrupa las relaciones que describen el fenómeno radiante
 * en dos etapas secuenciales dentro del mismo frame:
 *
 *   1. RADIATION         → transferencia de radiación entre pares (PlanckEvaluator)
 *   2. RADIATION_THERMAL → conversión de la radiación absorbida en calor
 *                          (RadiationThermalEvaluator)
 *
 * ── COMPOSICIÓN VÍA FRAMESTATE ───────────────────────────────────────────
 * PlanckEvaluator (prio 110) escribe FrameMagnitudes.ABSORBED_RADIATION.
 * RadiationThermalEvaluator (prio 112) lo lee y produce ΔTemperature.
 * No existe ninguna propiedad puente en PhysicalState.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Estas relaciones no ejecutan comportamiento. Describen fenómenos.
 * El procedimiento matemático vive exclusivamente en el evaluador especializado.
 *
 * ── ORIGEN ────────────────────────────────────────────────────────────────
 * Migrado desde ExtensibilityRelations (HRFC-022).
 * HRFC-024 lo ubica en su catálogo correcto por dominio radiante.
 */
public final class RadiationRelations {

    private RadiationRelations() {}

    /**
     * Transferencia de radiación entre pares dentro del radio de emisión.
     *
     * PlanckEvaluator escribe la radiación absorbida en FrameState bajo
     * FrameMagnitudes.ABSORBED_RADIATION para que RADIATION_THERMAL la
     * convierta en calor en el mismo frame.
     * No existe ninguna propiedad puente en PhysicalState.
     *
     * Fenómeno: PLANCK — ley de radiación de cuerpo negro
     * Evaluador: PlanckEvaluator
     */
    public static final PhysicalRelation RADIATION = PhysicalRelation.builder()
        .name("radiation")
        .relationType(RelationType.PLANCK)
        .participating(
            RadiationProperties.RADIATION_LEVEL,
            RadiationProperties.RADIATION_ABSORPTION)
        .constraint(RelationConstraint.maxDistance(96.0))
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(110)
        .build();

    /**
     * Convierte la radiación absorbida este frame (FrameMagnitudes.ABSORBED_RADIATION,
     * escrita por PlanckEvaluator) en calor a través de la capacidad calorífica.
     * Evaluada después de RADIATION (prioridad 110 → 112).
     *
     * Fenómeno: RADIATION_THERMAL — conversión de radiación en calor
     * Evaluador: RadiationThermalEvaluator
     */
    public static final PhysicalRelation RADIATION_THERMAL = PhysicalRelation.builder()
        .name("radiation_thermal")
        .relationType(RelationType.RADIATION_THERMAL)
        .participating(
            ThermalProperties.TEMPERATURE,
            ThermalProperties.HEAT_CAPACITY)
        .constraint(RelationConstraint.minDelta(1e-9))
        .priority(112)
        .build();

    /**
     * Todas las relaciones radiantes.
     *
     * @return array con las relaciones radiantes en orden correcto de prioridad.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] { RADIATION, RADIATION_THERMAL };
    }
}
