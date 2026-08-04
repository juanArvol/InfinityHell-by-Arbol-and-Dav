package Game.Engine.Physics.Thermal;

import Game.Engine.Physics.Thermal.ThermalProperties;
import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.RelationConstraint;
import Game.Engine.Physics.Core.RelationEvaluator;

import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo FOURIER.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Ley de Fourier: transferencia de calor por conducción entre dos cuerpos.
 * El calor fluye del cuerpo más caliente al más frío a una velocidad
 * proporcional a la menor conductividad de los dos y al inverso de la
 * capacidad calorífica del receptor.
 *
 * Simplificación discreta para simulación por frames:
 *   ΔT = (T_a − T_b) · min(k_a, k_b) · dt / max(C_b, 1)
 *
 * ── RESTRICCIONES DECLARATIVAS ────────────────────────────────────────────
 * El evaluador lee las restricciones de la PhysicalRelation recibida:
 *   MAX_DISTANCE    → radio máximo de transferencia (por defecto: 32 unidades)
 *   MIN_DELTA       → diferencia mínima de temperatura para operar (1e-6)
 *
 * ── PROPIEDADES REQUERIDAS ────────────────────────────────────────────────
 *   TEMPERATURE            (ambas entidades)
 *   THERMAL_CONDUCTIVITY   (ambas entidades, con fallback a 0.1)
 *   HEAT_CAPACITY          (receptor, con fallback a 1000.0)
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 * Este evaluador no contiene estado mutable.
 * Solo el PhysicsSolver llama a evaluate(). Nunca las entidades ni las leyes.
 */
public final class FourierEvaluator implements RelationEvaluator {

    private static final double DEFAULT_MAX_DISTANCE    = 32.0;
    private static final double DEFAULT_MIN_DELTA       = 1e-6;
    private static final double DEFAULT_CONDUCTIVITY    = 0.1;
    private static final double DEFAULT_HEAT_CAPACITY   = 1000.0;
    private static final double TRANSFER_COEFFICIENT    = 0.4;

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
            if (!a.has(ThermalProperties.TEMPERATURE)) continue;

            for (int j = i + 1; j < n; j++) {
                EvaluationView b = views.get(j);
                if (!b.has(ThermalProperties.TEMPERATURE)) continue;

                if (distance(a, b) > maxDist) continue;

                double tA   = a.get(ThermalProperties.TEMPERATURE);
                double tB   = b.get(ThermalProperties.TEMPERATURE);
                double diff = tA - tB;
                if (Math.abs(diff) < minDelta) continue;

                double kA = a.has(ThermalProperties.THERMAL_CONDUCTIVITY)
                    ? a.get(ThermalProperties.THERMAL_CONDUCTIVITY)
                    : DEFAULT_CONDUCTIVITY;
                double kB = b.has(ThermalProperties.THERMAL_CONDUCTIVITY)
                    ? b.get(ThermalProperties.THERMAL_CONDUCTIVITY)
                    : DEFAULT_CONDUCTIVITY;
                double k  = Math.min(kA, kB);

                double cA = a.has(ThermalProperties.HEAT_CAPACITY)
                    ? Math.max(a.get(ThermalProperties.HEAT_CAPACITY), 1.0)
                    : DEFAULT_HEAT_CAPACITY;
                double cB = b.has(ThermalProperties.HEAT_CAPACITY)
                    ? Math.max(b.get(ThermalProperties.HEAT_CAPACITY), 1.0)
                    : DEFAULT_HEAT_CAPACITY;

                double transferAtoB = diff * k * TRANSFER_COEFFICIENT * deltaTime / cB;
                double transferBtoA = diff * k * TRANSFER_COEFFICIENT * deltaTime / cA;

                a.add(ThermalProperties.TEMPERATURE, -transferBtoA);
                b.add(ThermalProperties.TEMPERATURE,  transferAtoB);
            }
        }
    }

    private static double constraintValue(PhysicalRelation            relation,
                                           RelationConstraint.Type      type,
                                           double                       defaultValue) {
        RelationConstraint c = relation.getConstraint(type);
        return c != null ? c.getValue() : defaultValue;
    }

    private static double distance(EvaluationView a, EvaluationView b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
