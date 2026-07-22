package Game.Engine.World.Solver;

import Game.Engine.World.Physics.CoreProperties;
import Game.Engine.World.Physics.PhysicalRelation;
import Game.Engine.World.Physics.RelationConstraint;
import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo FICK.
 *
 * ── HRFC-022 Corrección — Responsabilidad única ───────────────────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Primera ley de Fick: difusión de masa por gradiente de concentración
 * entre pares de objetos.
 *
 *   J = −D · ΔC  →  ΔH = (H_a − H_b) · D · dt
 *
 * Difiere de BERNOULLI en que Fick no considera viscosidad — modela
 * difusión pura de masa entre dos objetos con gradiente de concentración.
 *
 * ── ÚNICO FENÓMENO ────────────────────────────────────────────────────────
 * Este evaluador implementa exclusivamente la difusión de masa entre pares.
 * La disipación ambiental (objeto → ambiente) es un fenómeno distinto
 * y pertenece a AmbientDissipationEvaluator con RelationType.AMBIENT_DISSIPATION.
 *
 * ── RESTRICCIONES DECLARATIVAS ────────────────────────────────────────────
 *   MAX_DISTANCE → radio máximo de difusión (por defecto: 32 unidades)
 *   MIN_DELTA    → diferencia mínima de concentración para operar (1e-6)
 *
 * ── PROPIEDADES LEÍDAS ────────────────────────────────────────────────────
 *   HUMIDITY              (ambas entidades)
 *   HUMIDITY_ABSORPTION   (ambas entidades, fallback 0.05)
 *
 * ── PROPIEDADES ESCRITAS ──────────────────────────────────────────────────
 *   HUMIDITY              (deltas de difusión)
 */
public final class FickEvaluator implements RelationEvaluator {

    private static final double DEFAULT_MAX_DISTANCE = 32.0;
    private static final double DEFAULT_MIN_DELTA    = 1e-6;
    private static final double DEFAULT_ABSORPTION   = 0.05;

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        double maxDist  = constraintValue(relation, RelationConstraint.Type.MAX_DISTANCE,
                                          DEFAULT_MAX_DISTANCE);
        double minDelta = constraintValue(relation, RelationConstraint.Type.MIN_DELTA,
                                          DEFAULT_MIN_DELTA);

        int n = views.size();
        for (int i = 0; i < n - 1; i++) {
            EvaluationView a = views.get(i);
            if (!a.has(CoreProperties.HUMIDITY)) continue;

            for (int j = i + 1; j < n; j++) {
                EvaluationView b = views.get(j);
                if (!b.has(CoreProperties.HUMIDITY)) continue;

                if (distance(a, b) > maxDist) continue;

                double hA   = a.get(CoreProperties.HUMIDITY);
                double hB   = b.get(CoreProperties.HUMIDITY);
                double diff = hA - hB;
                if (Math.abs(diff) < minDelta) continue;

                double absA = a.has(CoreProperties.HUMIDITY_ABSORPTION)
                    ? a.get(CoreProperties.HUMIDITY_ABSORPTION)
                    : DEFAULT_ABSORPTION;
                double absB = b.has(CoreProperties.HUMIDITY_ABSORPTION)
                    ? b.get(CoreProperties.HUMIDITY_ABSORPTION)
                    : DEFAULT_ABSORPTION;

                // Fenómeno único: difusión por gradiente de concentración (Fick)
                double transferred = diff * Math.min(absA, absB) * deltaTime;
                a.add(CoreProperties.HUMIDITY, -transferred);
                b.add(CoreProperties.HUMIDITY,  transferred);
            }
        }
    }

    private static double constraintValue(PhysicalRelation           relation,
                                           RelationConstraint.Type     type,
                                           double                      defaultValue) {
        RelationConstraint c = relation.getConstraint(type);
        return c != null ? c.getValue() : defaultValue;
    }

    private static double distance(EvaluationView a, EvaluationView b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
