package Game.Engine.World.Physics.Gravity;

import Game.Engine.World.Physics.Gravity.GravityProperties;
import Game.Engine.World.Physics.Kinematic.KinematicProperties;
import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.RelationEvaluator;

import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo NEWTON.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 * ── HRFC-023 Auditoría — Eliminación de búsquedas por ID ─────────────────
 * ── HRFC-024 Auditoría — Consistencia Arquitectónica ─────────────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Segunda ley de Newton: dinámica de partículas bajo fuerzas externas.
 * F = m · a  →  a = F / m  →  Δv = a · dt
 *
 * Para el contexto de simulación por frames este evaluador aplica
 * aceleraciones uniformes (como gravedad) a entidades con velocidades.
 *
 * La aceleración se lee de la restricción THRESHOLD_ABOVE de la relación
 * como la magnitud de la fuerza por unidad de masa. Si no se declara,
 * se aplica la gravedad estándar del juego (9.8 u/s²) a VELOCITY_Y.
 *
 * Las propiedades participantes de la relación identifican qué componentes
 * de velocidad se ven afectados. El evaluador itera sobre ellas.
 *
 * ── PROPIEDADES PARTICIPANTES ────────────────────────────────────────────
 * Las propiedades declaradas en getParticipatingProperties() que no sean
 * MASS reciben el delta de aceleración · dt.
 * GravityProperties.MASS se usa como divisor si la entidad la tiene.
 */
public final class NewtonEvaluator implements RelationEvaluator {

    private static final double DEFAULT_ACCELERATION = 9.8;

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        // La restricción THRESHOLD_ABOVE codifica la aceleración base
        RelationConstraint accelConstraint =
            relation.getConstraint(RelationConstraint.Type.THRESHOLD_ABOVE);
        double acceleration = accelConstraint != null
            ? accelConstraint.getValue()
            : DEFAULT_ACCELERATION;

        for (EvaluationView e : views) {
            // Si la entidad tiene masa la usamos como divisor
            double mass = e.has(GravityProperties.MASS)
                ? Math.max(e.get(GravityProperties.MASS), 0.01)
                : 1.0;
            double accel = acceleration / mass * deltaTime;

            // Aplicar delta a todas las propiedades participantes excepto MASS
            for (var p : relation.getParticipatingProperties()) {
                if (p == GravityProperties.MASS) continue;
                if (e.has(p)) e.add(p, accel);
            }
        }
    }
}
