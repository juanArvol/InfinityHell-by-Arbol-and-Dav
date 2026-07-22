package Game.Engine.World.Solver;

import Game.Engine.World.Physics.CoreProperties;
import Game.Engine.World.Physics.PhysicalRelation;
import Game.Engine.World.Physics.RelationConstraint;
import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo PASCAL.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Ley de Pascal (combinada con expansión volumétrica): la temperatura de un
 * cuerpo genera presión interna proporcional a su incompresibilidad.
 *
 *   ΔP = T · (1 − compresibilidad) · 0.05
 *
 * Operación sobre entidad individual (no pares).
 *
 * ── RESTRICCIONES DECLARATIVAS ────────────────────────────────────────────
 *   PROPERTY_PRESENT(COMPRESSIBILITY) → solo entidades con esa propiedad
 *   MIN_DELTA(1e-6)                   → ignorar temperaturas cercanas a 0
 *
 * ── PROPIEDADES REQUERIDAS ────────────────────────────────────────────────
 *   TEMPERATURE      (entidad)
 *   PRESSURE         (entidad)
 *   COMPRESSIBILITY  (entidad, fallback a 0.0)
 */
public final class PascalEvaluator implements RelationEvaluator {

    private static final double DEFAULT_MIN_DELTA      = 1e-6;
    private static final double EXPANSION_COEFFICIENT  = 0.05;

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        double minDelta = constraintValue(relation, RelationConstraint.Type.MIN_DELTA,
                                          DEFAULT_MIN_DELTA);

        for (EvaluationView e : views) {
            if (!e.has(CoreProperties.TEMPERATURE)) continue;
            if (!e.has(CoreProperties.PRESSURE))    continue;

            double temp = e.get(CoreProperties.TEMPERATURE);
            if (Math.abs(temp) < minDelta) continue;

            double compressibility = e.has(CoreProperties.COMPRESSIBILITY)
                ? e.get(CoreProperties.COMPRESSIBILITY) : 0.0;
            double coeff = (1.0 - compressibility) * EXPANSION_COEFFICIENT;
            e.add(CoreProperties.PRESSURE, temp * coeff);
        }
    }

    private static double constraintValue(PhysicalRelation           relation,
                                           RelationConstraint.Type     type,
                                           double                      defaultValue) {
        RelationConstraint c = relation.getConstraint(type);
        return c != null ? c.getValue() : defaultValue;
    }
}
