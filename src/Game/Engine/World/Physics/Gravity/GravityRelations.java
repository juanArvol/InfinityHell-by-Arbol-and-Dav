package Game.Engine.World.Physics.Gravity;

import Game.Engine.World.Physics.Core.GravityProperties;
import Game.Engine.World.Physics.Core.KinematicProperties;
import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.RelationType;

/**
 * Catálogo de relaciones gravitacionales — agujero negro y horizonte de eventos.
 *
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Este catálogo agrupa las relaciones que describen la gravedad entre cuerpos
 * masivos según la métrica de Schwarzschild y el fenómeno discontinuo del
 * horizonte de eventos.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Estas relaciones no ejecutan comportamiento. Describen fenómenos.
 * El procedimiento matemático vive exclusivamente en el evaluador especializado.
 *
 * ── RELACIONES INCLUIDAS ──────────────────────────────────────────────────
 *   BLACK_HOLE_GRAVITY  → atracción gravitacional F = G·m_a·m_b / d²  (Schwarzschild)
 *   BLACK_HOLE_HORIZON  → absorción de velocidad al cruzar el horizonte (EventHorizon)
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 * BLACK_HOLE_GRAVITY  → fenómeno continuo, evaluado por SchwarzschildEvaluator
 * BLACK_HOLE_HORIZON  → fenómeno discontinuo, evaluado por EventHorizonEvaluator
 * Son dos relaciones distintas con evaluadores distintos. La prioridad (2 → 3)
 * garantiza que la atracción ocurre antes de la comprobación del horizonte.
 *
 * ── ORIGEN ────────────────────────────────────────────────────────────────
 * Migrado desde ExtensibilityRelations (HRFC-022).
 * HRFC-024 lo ubica en su catálogo correcto por dominio gravitacional.
 */
public final class GravityRelations {

    private GravityRelations() {}

    /**
     * Atracción gravitacional entre cuerpos masivos.
     * F = G·m_a·m_b / d²
     *
     * Fenómeno: SCHWARZSCHILD — métrica de Schwarzschild (solo atracción)
     * Evaluador: SchwarzschildEvaluator
     * Restricción: los objetos deben tener GravityProperties.MASS.
     */
    public static final PhysicalRelation BLACK_HOLE_GRAVITY = PhysicalRelation.builder()
        .name("black_hole_gravity")
        .relationType(RelationType.SCHWARZSCHILD)
        .participating(GravityProperties.MASS, KinematicProperties.VELOCITY_X,
                       KinematicProperties.VELOCITY_Y)
        .constraint(RelationConstraint.propertyPresent(GravityProperties.MASS))
        .priority(2)
        .build();

    /**
     * Absorción total de velocidad cuando un objeto cruza el horizonte de eventos.
     * Evaluada después de la atracción gravitacional (prioridad 2 → 3).
     *
     * Fenómeno: EVENT_HORIZON — absorción discontinua al cruzar el radio de Schwarzschild
     * Evaluador: EventHorizonEvaluator
     * Restricciones: los objetos deben tener MASS y al menos uno SCHWARZSCHILD_RADIUS.
     */
    public static final PhysicalRelation BLACK_HOLE_HORIZON = PhysicalRelation.builder()
        .name("black_hole_horizon")
        .relationType(RelationType.EVENT_HORIZON)
        .participating(GravityProperties.MASS, GravityProperties.SCHWARZSCHILD_RADIUS,
                       KinematicProperties.VELOCITY_X, KinematicProperties.VELOCITY_Y)
        .constraint(RelationConstraint.propertyPresent(GravityProperties.MASS))
        .constraint(RelationConstraint.propertyPresent(GravityProperties.SCHWARZSCHILD_RADIUS))
        .priority(3)
        .build();

    /**
     * Todas las relaciones gravitacionales.
     *
     * @return array con las relaciones gravitacionales.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] { BLACK_HOLE_GRAVITY, BLACK_HOLE_HORIZON };
    }
}
