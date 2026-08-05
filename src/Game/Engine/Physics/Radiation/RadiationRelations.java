package Game.Engine.Physics.Radiation;

import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.RelationConstraint;
import Game.Engine.Physics.Core.RelationType;
import Game.Engine.Physics.Thermal.ThermalProperties;

/**
 * Catálogo de relaciones del dominio radiante.
 *
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
 * ── HRFC — Auditoría Arquitectónica Final ────────────────────────────────
 *
 * ── DOMINIO FÍSICO ────────────────────────────────────────────────────────
 * Este catálogo agrupa las relaciones cuya magnitud motriz es la radiación:
 * fenómenos originados por la emisión, absorción o transformación de energía
 * radiante.
 *
 * ── RELACIONES INCLUIDAS ──────────────────────────────────────────────────
 *
 *   RADIATION         — transferencia de radiación entre pares        PLANCK
 *   RADIATION_THERMAL — radiación absorbida → calor                   RADIATION_THERMAL
 *
 * ── PIPELINE RADIANTE ────────────────────────────────────────────────────
 * Las dos relaciones forman un pipeline secuencial dentro del mismo frame:
 *
 *   PLANCK (prio 110)            → transfiere radiación entre pares
 *                                   escribe FrameMagnitudes.ABSORBED_RADIATION
 *   RADIATION_THERMAL (prio 112) → lee ABSORBED_RADIATION → ΔTemperature
 *
 * Ambas relaciones pertenecen al dominio radiante:
 *   - RADIATION: la magnitud motriz es RADIATION_LEVEL.
 *   - RADIATION_THERMAL: la magnitud motriz es la radiación absorbida
 *     (escrita por PlanckEvaluator). Aunque el efecto final sea térmico,
 *     la causa es radiante. La relación es parte del pipeline de radiación.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Estas relaciones no ejecutan comportamiento. Describen fenómenos.
 * El procedimiento matemático vive exclusivamente en el evaluador especializado.
 */
public final class RadiationRelations {

    private RadiationRelations() {}

    /**
     * Transferencia de radiación entre pares dentro del radio de emisión.
     *
     * PlanckEvaluator calcula la radiación transferida y escribe la cantidad
     * absorbida en FrameMagnitudes.ABSORBED_RADIATION para que RADIATION_THERMAL
     * la convierta en calor en el mismo frame.
     *
     * Fenómeno: PLANCK — ley de radiación de cuerpo negro
     * Evaluador: PlanckEvaluator
     * Restricciones: MAX_DISTANCE(96), MIN_DELTA(1e-6)
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
     * Conversión de la radiación absorbida este frame en calor.
     *
     * Lee FrameMagnitudes.ABSORBED_RADIATION escrita por PlanckEvaluator
     * y produce ΔTemperature a través de la capacidad calorífica del material.
     * Evaluada después de RADIATION (prioridad 110 → 112).
     *
     * La magnitud motriz es la radiación recibida (causa radiante).
     * Aunque el efecto sea térmico, el fenómeno pertenece al pipeline radiante.
     *
     * Fenómeno: RADIATION_THERMAL — conversión de radiación en calor
     * Evaluador: RadiationThermalEvaluator
     * Restricciones: MIN_DELTA(1e-9)
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
     * Todas las relaciones del dominio radiante, en orden de pipeline.
     *
     * @return array con las relaciones radiantes.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] { RADIATION, RADIATION_THERMAL };
    }
}
