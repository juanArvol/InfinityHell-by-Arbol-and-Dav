package Game.Engine.World.Physics.Electrical;

import Game.Engine.World.Physics.Core.ElectricalProperties;
import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.FrameMagnitudes;
import Game.Engine.World.Physics.Core.RelationEvaluator;

import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo OHM.
 *
 * ── HRFC-022 Corrección Arquitectónica Final ──────────────────────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Ley de Ohm: transferencia de carga eléctrica entre dos cuerpos.
 * La carga fluye del potencial mayor al menor, a una velocidad proporcional
 * a la conductividad eléctrica mínima de los dos.
 *
 *   ΔQ = (Q_a − Q_b) · min(σ_a, σ_b) · 0.3 · dt
 *
 * ── ÚNICO FENÓMENO ────────────────────────────────────────────────────────
 * Este evaluador implementa exclusivamente la transferencia de carga.
 * La generación de calor (efecto Joule) es un fenómeno distinto implementado
 * por JouleEvaluator.
 *
 * La corriente calculada (|ΔQ| / dt) se escribe en el FrameState de cada
 * entidad bajo la clave FrameMagnitudes.CURRENT. JouleEvaluator la leerá en la
 * misma pasada del frame sin necesidad de propiedades puente persistentes.
 *
 * ── RESTRICCIONES DECLARATIVAS ────────────────────────────────────────────
 *   MAX_DISTANCE → radio máximo de transferencia (por defecto: 32 unidades)
 *   MIN_DELTA    → diferencia mínima de carga para operar (1e-6)
 *
 * ── PROPIEDADES LEÍDAS (PhysicalState) ───────────────────────────────────
 *   CHARGE                   (ambas entidades)
 *   ELECTRICAL_CONDUCTIVITY  (ambas entidades, fallback 0.1)
 *
 * ── PROPIEDADES ESCRITAS (PhysicalState) ─────────────────────────────────
 *   CHARGE                   (ΔQ negativo en cedente, positivo en receptor)
 *
 * ── MAGNITUDES ESCRITAS (FrameState) ─────────────────────────────────────
 *   FrameMagnitudes.CURRENT       (corriente = |ΔQ| acumulada por entidad este frame)
 */
public final class OhmEvaluator implements RelationEvaluator {

    private static final double DEFAULT_MAX_DISTANCE = 32.0;
    private static final double DEFAULT_MIN_DELTA    = 1e-6;
    private static final double DEFAULT_CONDUCTIVITY = 0.1;
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
            if (!a.has(ElectricalProperties.CHARGE)) continue;

            for (int j = i + 1; j < n; j++) {
                EvaluationView b = views.get(j);
                if (!b.has(ElectricalProperties.CHARGE)) continue;

                if (distance(a, b) > maxDist) continue;

                double qA   = a.get(ElectricalProperties.CHARGE);
                double qB   = b.get(ElectricalProperties.CHARGE);
                double diff = qA - qB;
                if (Math.abs(diff) < minDelta) continue;

                double sA = a.has(ElectricalProperties.ELECTRICAL_CONDUCTIVITY)
                    ? a.get(ElectricalProperties.ELECTRICAL_CONDUCTIVITY)
                    : DEFAULT_CONDUCTIVITY;
                double sB = b.has(ElectricalProperties.ELECTRICAL_CONDUCTIVITY)
                    ? b.get(ElectricalProperties.ELECTRICAL_CONDUCTIVITY)
                    : DEFAULT_CONDUCTIVITY;
                double sigma = Math.min(sA, sB);

                double transferred = diff * sigma * TRANSFER_COEFFICIENT * deltaTime;

                // Fenómeno único: transferencia de carga (Ohm)
                a.add(ElectricalProperties.CHARGE, -transferred);
                b.add(ElectricalProperties.CHARGE,  transferred);

                // Magnitud derivada transitoria: corriente para efecto Joule
                // Escrita en FrameState — no persiste en PhysicalState
                double current = Math.abs(transferred);
                a.frameState().add(FrameMagnitudes.CURRENT, current);
                b.frameState().add(FrameMagnitudes.CURRENT, current);
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
