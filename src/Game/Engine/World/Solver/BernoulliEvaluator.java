package Game.Engine.World.Solver;

import Game.Engine.World.Physics.CoreProperties;
import Game.Engine.World.Physics.PhysicalRelation;
import Game.Engine.World.Physics.RelationConstraint;
import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo BERNOULLI.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Principio de Bernoulli aplicado a difusión fluídica entre pares.
 * La humedad fluye del cuerpo con mayor concentración al de menor,
 * a una velocidad modulada por la viscosidad y la absorción del receptor.
 *
 *   ΔH = (H_a − H_b) · min(abs_a, abs_b) · viscosidad_factor · dt
 *
 * La viscosidad alta reduce la velocidad de difusión.
 *
 * ── RESTRICCIONES DECLARATIVAS ────────────────────────────────────────────
 *   MAX_DISTANCE → radio máximo de difusión (por defecto: 32 unidades)
 *   MIN_DELTA    → diferencia mínima de humedad para operar (1e-6)
 *
 * ── PROPIEDADES REQUERIDAS ────────────────────────────────────────────────
 *   HUMIDITY              (ambas entidades)
 *   HUMIDITY_ABSORPTION   (ambas entidades, fallback a 0.05)
 *   VISCOSITY             (ambas entidades, fallback a 0.0)
 */
public final class BernoulliEvaluator implements RelationEvaluator {

    private static final double DEFAULT_MAX_DISTANCE = 32.0;
    private static final double DEFAULT_MIN_DELTA    = 1e-6;
    private static final double DEFAULT_ABSORPTION   = 0.05;
    private static final double TRANSFER_COEFFICIENT = 0.3;

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

                // La viscosidad frena la difusión
                double visA = a.has(CoreProperties.VISCOSITY)
                    ? a.get(CoreProperties.VISCOSITY) : 0.0;
                double visB = b.has(CoreProperties.VISCOSITY)
                    ? b.get(CoreProperties.VISCOSITY) : 0.0;
                double viscosityFactor = 1.0 / (1.0 + Math.max(visA, visB));

                double transferred = diff
                    * Math.min(absA, absB)
                    * TRANSFER_COEFFICIENT
                    * viscosityFactor
                    * deltaTime;

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
