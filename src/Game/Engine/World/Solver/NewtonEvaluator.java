package Game.Engine.World.Solver;

import Game.Engine.World.Physics.PhysicalRelation;
import Game.Engine.World.Physics.PropertyDescriptor;
import Game.Engine.World.Physics.RelationConstraint;
import java.util.List;
import java.util.Set;

/**
 * Evaluador de relaciones físicas de tipo NEWTON.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
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
 * Las propiedades declaradas en getParticipatingProperties() que sean de
 * velocidad reciben el delta de aceleración · dt.
 * La propiedad MASS se usa como divisor si está presente.
 */
public final class NewtonEvaluator implements RelationEvaluator {

    private static final double DEFAULT_ACCELERATION = 9.8;

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {

        Set<PropertyDescriptor> props = relation.getParticipatingProperties();

        // La restricción THRESHOLD_ABOVE sobre MASS codifica la aceleración base
        RelationConstraint accelConstraint =
            relation.getConstraint(RelationConstraint.Type.THRESHOLD_ABOVE);
        double acceleration = accelConstraint != null
            ? accelConstraint.getValue()
            : DEFAULT_ACCELERATION;

        for (EvaluationView e : views) {
            double mass = 1.0;
            // Si MASS está declarada como participante, la usamos
            for (PropertyDescriptor p : props) {
                if ("mass".equals(p.getId()) && e.has(p)) {
                    mass = Math.max(e.get(p), 0.01);
                    break;
                }
            }
            double accel = acceleration / mass * deltaTime;

            for (PropertyDescriptor p : props) {
                if ("mass".equals(p.getId())) continue;
                if (e.has(p)) e.add(p, accel);
            }
        }
    }
}
