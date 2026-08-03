package Game.Engine.World.Physics.Kinematic;

import Game.Engine.World.Physics.Gravity.GravityProperties;
import Game.Engine.World.Physics.Kinematic.KinematicProperties;
import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.RelationType;

/**
 * Catálogo de relaciones cinemáticas — gravedad uniforme.
 *
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Este catálogo agrupa las relaciones que describen el movimiento de objetos
 * bajo fuerzas externas uniformes.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Estas relaciones no ejecutan comportamiento. Describen fenómenos.
 * El procedimiento matemático vive exclusivamente en el evaluador especializado.
 *
 * ── RELACIONES INCLUIDAS ──────────────────────────────────────────────────
 *   GRAVITY → aceleración vertical constante sobre VELOCITY_Y (Newton)
 *
 * ── ORIGEN ────────────────────────────────────────────────────────────────
 * Migrado desde ExtensibilityRelations (HRFC-022).
 * HRFC-024 lo ubica en su catálogo correcto por dominio cinemático.
 */
public final class KinematicRelations {

    private KinematicRelations() {}

    /**
     * Gravedad uniforme hacia abajo.
     *
     * Fenómeno: NEWTON — F = m·a → Δv = g·dt
     * Dependencia física: VELOCITY_Y recibe aceleración gravitacional por frame.
     * La restricción THRESHOLD_ABOVE con valor 9.8 codifica la aceleración (u/s²).
     * Si el objeto tiene GravityProperties.MASS, la aceleración se divide por ella.
     * Evaluador: NewtonEvaluator
     */
    public static final PhysicalRelation GRAVITY = PhysicalRelation.builder()
        .name("gravity")
        .relationType(RelationType.NEWTON)
        .participating(KinematicProperties.VELOCITY_Y)
        .constraint(RelationConstraint.thresholdAbove(KinematicProperties.VELOCITY_Y, 9.8))
        .priority(1)
        .build();

    /**
     * Todas las relaciones cinemáticas.
     *
     * @return array con las relaciones cinemáticas.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] { GRAVITY };
    }
}
