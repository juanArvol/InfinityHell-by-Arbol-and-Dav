package Game.Engine.World.Physics.Electromagnetic;

import Game.Engine.World.Physics.Electrical.ElectricalProperties;
import Game.Engine.World.Physics.Electromagnetic.ElectromagneticProperties;
import Game.Engine.World.Physics.Thermal.ThermalProperties;
import Game.Engine.World.Physics.Kinematic.KinematicProperties;
import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.RelationType;

/**
 * Catálogo de relaciones electromagnéticas — magnetismo y superconductividad.
 *
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Este catálogo agrupa las relaciones que describen fenómenos electromagnéticos
 * más allá de la transferencia eléctrica básica definida en CoreRelations.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Estas relaciones no ejecutan comportamiento. Describen fenómenos.
 * El procedimiento matemático vive exclusivamente en el evaluador especializado.
 *
 * ── RELACIONES INCLUIDAS ──────────────────────────────────────────────────
 *   MAGNETISM          → fuerza entre dipolos magnéticos dentro de radio 128 (OHM)
 *   SUPERCONDUCTIVITY  → resistencia cero por debajo de temperatura crítica (OHM)
 *
 * ── ORIGEN ────────────────────────────────────────────────────────────────
 * Migrado desde ExtensibilityRelations (HRFC-022).
 * HRFC-024 lo ubica en su catálogo correcto por dominio electromagnético.
 */
public final class ElectromagneticRelations {

    private ElectromagneticRelations() {}

    /**
     * Fuerza magnética entre pares dentro de radio 128.
     * Cargas del mismo signo se repelen; signos opuestos se atraen.
     *
     * Fenómeno: OHM — los campos magnéticos se modelan como diferencia de
     *           potencial magnético entre pares.
     * Evaluador: OhmEvaluator
     */
    public static final PhysicalRelation MAGNETISM = PhysicalRelation.builder()
        .name("magnetism")
        .relationType(RelationType.OHM)
        .participating(ElectromagneticProperties.MAGNETIC_FIELD,
                       KinematicProperties.VELOCITY_X, KinematicProperties.VELOCITY_Y)
        .constraint(RelationConstraint.maxDistance(128.0))
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(80)
        .build();

    /**
     * Cuando la temperatura cae por debajo del umbral de superconductividad,
     * la disipación eléctrica queda cancelada (resistencia = 0).
     *
     * Fenómeno: OHM — corrección de la disipación cuando T < umbral
     * Evaluador: OhmEvaluator (con restricción THRESHOLD_BELOW activa)
     */
    public static final PhysicalRelation SUPERCONDUCTIVITY = PhysicalRelation.builder()
        .name("superconductivity")
        .relationType(RelationType.OHM)
        .participating(
            ThermalProperties.TEMPERATURE,
            ElectricalProperties.CHARGE,
            ElectricalProperties.ELECTRICAL_CONDUCTIVITY,
            ElectromagneticProperties.SUPERCONDUCTIVITY_THRESHOLD)
        .constraint(RelationConstraint.thresholdBelow(ThermalProperties.TEMPERATURE, 0.0))
        .constraint(RelationConstraint.propertyPresent(
            ElectromagneticProperties.SUPERCONDUCTIVITY_THRESHOLD))
        .priority(60)
        .build();

    /**
     * Todas las relaciones electromagnéticas.
     *
     * @return array con las relaciones electromagnéticas.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] { MAGNETISM, SUPERCONDUCTIVITY };
    }
}
