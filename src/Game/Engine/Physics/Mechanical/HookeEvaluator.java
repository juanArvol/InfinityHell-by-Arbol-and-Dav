package Game.Engine.Physics.Mechanical;

import Game.Engine.Physics.Mechanical.MechanicalProperties;
import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.RelationConstraint;
import Game.Engine.Physics.Core.RelationEvaluator;

import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo HOOKE.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Ley de Hooke: deformación elástica proporcional a la presión aplicada.
 *
 * Aquí se modela la disipación de la presión excess por compresibilidad
 * del material: la presión que supera el umbral de equilibrio se absorbe
 * parcialmente por la compresibilidad del material.
 *
 * Cuando la presión acumulada supera un umbral (THRESHOLD_ABOVE), se reduce
 * la presión en exceso proporcional a la compresibilidad:
 *   ΔP = −(P − P_umbral) · compresibilidad · factor
 *
 * ── RESTRICCIONES DECLARATIVAS ────────────────────────────────────────────
 *   THRESHOLD_ABOVE(PRESSURE, umbral) → umbral a partir del cual actúa Hooke
 *
 * ── PROPIEDADES REQUERIDAS ────────────────────────────────────────────────
 *   PRESSURE          (entidad)
 *   COMPRESSIBILITY   (entidad, fallback a 0.5)
 */
public final class HookeEvaluator implements RelationEvaluator {

    private static final double DEFAULT_PRESSURE_THRESHOLD = 0.0;
    private static final double DEFAULT_COMPRESSIBILITY    = 0.5;
    private static final double HOOKE_FACTOR               = 0.1;

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        RelationConstraint threshConstraint =
            relation.getConstraint(RelationConstraint.Type.THRESHOLD_ABOVE);
        double threshold = threshConstraint != null
            ? threshConstraint.getValue()
            : DEFAULT_PRESSURE_THRESHOLD;

        for (EvaluationView e : views) {
            if (!e.has(MechanicalProperties.PRESSURE)) continue;

            double pressure = e.get(MechanicalProperties.PRESSURE);
            double excess   = Math.abs(pressure) - threshold;
            if (excess <= 0) continue;

            double compressibility = e.has(MechanicalProperties.COMPRESSIBILITY)
                ? e.get(MechanicalProperties.COMPRESSIBILITY)
                : DEFAULT_COMPRESSIBILITY;

            double sign      = Math.signum(pressure);
            double reduction = sign * excess * compressibility * HOOKE_FACTOR * deltaTime;
            e.add(MechanicalProperties.PRESSURE, -reduction);
        }
    }
}
