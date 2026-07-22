package Game.Engine.World.Physics.MaterialState;

import Game.Engine.World.Physics.Core.FluidProperties;
import Game.Engine.World.Physics.Core.ElectricalProperties;
import Game.Engine.World.Physics.Core.KinematicProperties;
import Game.Engine.World.Physics.Core.MaterialStateProperties;
import Game.Engine.World.Physics.Core.ThermalProperties;
import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.RadiationProperties;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.RelationType;

/**
 * Catálogo de relaciones de estado del material — cristalización, plasma y tensión superficial.
 *
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Este catálogo agrupa las relaciones que describen transiciones de fase
 * y estados especiales del material:
 *
 *   CRYSTALLIZATION      → precipitación de cristales por frío y humedad (FICK)
 *   PLASMA_TRANSITION    → ionización del material a altas temperaturas (PLANCK)
 *   SURFACE_TENSION_RELATION → cohesión entre objetos líquidos próximos (STOKES)
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Estas relaciones no ejecutan comportamiento. Describen fenómenos.
 * El procedimiento matemático vive exclusivamente en el evaluador especializado.
 *
 * ── ORIGEN ────────────────────────────────────────────────────────────────
 * Migrado desde ExtensibilityRelations (HRFC-022).
 * HRFC-024 lo ubica en su catálogo correcto por dominio de estado del material.
 */
public final class MaterialStateRelations {

    private MaterialStateRelations() {}

    /**
     * Cuando la temperatura es negativa y hay humedad suficiente,
     * la humedad se convierte en masa cristalizada liberando calor latente.
     *
     * Fenómeno: FICK — difusión de masa (humedad → cristal)
     * Evaluador: FickEvaluator (con restricción THRESHOLD_BELOW(TEMPERATURE, 0))
     */
    public static final PhysicalRelation CRYSTALLIZATION = PhysicalRelation.builder()
        .name("crystallization")
        .relationType(RelationType.FICK)
        .participating(
            ThermalProperties.TEMPERATURE,
            FluidProperties.HUMIDITY,
            MaterialStateProperties.CRYSTALLIZATION_RATE,
            MaterialStateProperties.CRYSTAL_CONCENTRATION)
        .constraint(RelationConstraint.thresholdBelow(ThermalProperties.TEMPERATURE, 0.0))
        .constraint(RelationConstraint.thresholdAbove(FluidProperties.HUMIDITY, 0.1))
        .priority(40)
        .build();

    /**
     * Cuando la temperatura supera el umbral del material, el estado de plasma
     * aumenta progresivamente amplificando conductividad y emitiendo radiación.
     *
     * Fenómeno: PLANCK — radiación de plasma y transición de estado
     * Evaluador: PlanckEvaluator (con restricción PROPERTY_PRESENT activa)
     */
    public static final PhysicalRelation PLASMA_TRANSITION = PhysicalRelation.builder()
        .name("plasma_transition")
        .relationType(RelationType.PLANCK)
        .participating(
            ThermalProperties.TEMPERATURE,
            MaterialStateProperties.PLASMA_THRESHOLD,
            MaterialStateProperties.PLASMA_STATE,
            ElectricalProperties.ELECTRICAL_CONDUCTIVITY,
            RadiationProperties.RADIATION_LEVEL)
        .constraint(RelationConstraint.propertyPresent(
            MaterialStateProperties.PLASMA_THRESHOLD))
        .constraint(RelationConstraint.propertyPresent(
            MaterialStateProperties.PLASMA_STATE))
        .priority(35)
        .build();

    /**
     * Los objetos líquidos se atraen mutuamente cuando están muy próximos.
     * Solo actúa a distancias muy cortas (≤ 8 unidades).
     *
     * Fenómeno: STOKES — resistencia viscosa y cohesión entre partículas fluidas
     * Evaluador: StokesEvaluator
     */
    public static final PhysicalRelation SURFACE_TENSION_RELATION = PhysicalRelation.builder()
        .name("surface_tension")
        .relationType(RelationType.STOKES)
        .participating(
            MaterialStateProperties.SURFACE_TENSION,
            FluidProperties.VISCOSITY,
            KinematicProperties.VELOCITY_X,
            KinematicProperties.VELOCITY_Y)
        .constraint(RelationConstraint.maxDistance(8.0))
        .constraint(RelationConstraint.propertyPresent(
            MaterialStateProperties.SURFACE_TENSION))
        .priority(90)
        .build();

    /**
     * Todas las relaciones de estado del material.
     *
     * @return array con las relaciones de estado del material.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] {
            CRYSTALLIZATION,
            PLASMA_TRANSITION,
            SURFACE_TENSION_RELATION
        };
    }
}
