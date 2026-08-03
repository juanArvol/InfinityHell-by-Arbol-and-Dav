package Game.Engine.World.Physics.Radiation;

import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Radiation.RadiationProperties;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.FrameMagnitudes;
import Game.Engine.World.Physics.Core.RelationEvaluator;

import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo PLANCK.
 *
 * ── HRFC-022 Corrección Arquitectónica Final ──────────────────────────────
 * ── HRFC-023 Auditoría — Eliminación de búsquedas por ID ─────────────────
 * ── HRFC-024 Auditoría — Consistencia Arquitectónica ─────────────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Ley de Planck: transferencia de energía radiante entre pares de objetos.
 * Un objeto emite radiación proporcional a su nivel acumulado; el receptor
 * la absorbe según su coeficiente de absorción.
 *
 *   ΔR = (R_a − R_b) · min(abs_a, abs_b) · coef · dt
 *
 * ── ÚNICO FENÓMENO ────────────────────────────────────────────────────────
 * Este evaluador implementa exclusivamente la transferencia de radiación.
 * La conversión de radiación absorbida en calor es un fenómeno distinto
 * implementado por RadiationThermalEvaluator.
 *
 * La radiación absorbida por el receptor se escribe en su FrameState bajo
 * la clave FrameMagnitudes.ABSORBED_RADIATION. RadiationThermalEvaluator la leerá
 * en la misma pasada del frame sin propiedades puente persistentes.
 *
 * ── RESTRICCIONES DECLARATIVAS ────────────────────────────────────────────
 *   MAX_DISTANCE → radio máximo de emisión (por defecto: 96 unidades)
 *   MIN_DELTA    → diferencia mínima de radiación para operar (1e-6)
 *
 * ── PROPIEDADES LEÍDAS (PhysicalState) ───────────────────────────────────
 *   RadiationProperties.RADIATION_LEVEL      (ambas entidades)
 *   RadiationProperties.RADIATION_ABSORPTION (ambas entidades, fallback 0.1)
 *
 * ── PROPIEDADES ESCRITAS (PhysicalState) ─────────────────────────────────
 *   RadiationProperties.RADIATION_LEVEL      (ΔR de transferencia)
 *
 * ── MAGNITUDES ESCRITAS (FrameState) ─────────────────────────────────────
 *   FrameMagnitudes.ABSORBED_RADIATION  (energía absorbida por el receptor este frame)
 */
public final class PlanckEvaluator implements RelationEvaluator {

    private static final double DEFAULT_MAX_DISTANCE = 96.0;
    private static final double DEFAULT_MIN_DELTA    = 1e-6;
    private static final double DEFAULT_ABSORPTION   = 0.1;
    private static final double TRANSFER_COEFFICIENT = 0.02;

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
            if (!a.has(RadiationProperties.RADIATION_LEVEL)) continue;

            for (int j = i + 1; j < n; j++) {
                EvaluationView b = views.get(j);
                if (!b.has(RadiationProperties.RADIATION_LEVEL)) continue;

                if (distance(a, b) > maxDist) continue;

                double rA   = a.get(RadiationProperties.RADIATION_LEVEL);
                double rB   = b.get(RadiationProperties.RADIATION_LEVEL);
                double diff = rA - rB;
                if (Math.abs(diff) < minDelta) continue;

                double absA = a.has(RadiationProperties.RADIATION_ABSORPTION)
                    ? a.get(RadiationProperties.RADIATION_ABSORPTION)
                    : DEFAULT_ABSORPTION;
                double absB = b.has(RadiationProperties.RADIATION_ABSORPTION)
                    ? b.get(RadiationProperties.RADIATION_ABSORPTION)
                    : DEFAULT_ABSORPTION;
                double absorption = Math.min(absA, absB);

                // Fenómeno único: transferencia de radiación entre pares (Planck)
                double transferred = diff * absorption * TRANSFER_COEFFICIENT * deltaTime;
                a.add(RadiationProperties.RADIATION_LEVEL, -transferred);
                b.add(RadiationProperties.RADIATION_LEVEL,  transferred);

                // Magnitud derivada transitoria: radiación absorbida para conversión térmica
                // Escrita en FrameState — no persiste en PhysicalState
                double absorbed = Math.abs(transferred);
                if (diff > 0) {
                    b.frameState().add(FrameMagnitudes.ABSORBED_RADIATION, absorbed);
                } else {
                    a.frameState().add(FrameMagnitudes.ABSORBED_RADIATION, absorbed);
                }
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
