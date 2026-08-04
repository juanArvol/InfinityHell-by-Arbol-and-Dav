package Game.Engine.Physics.Thermal;

import Game.Engine.Physics.Thermal.ThermalProperties;
import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.RelationConstraint;
import Game.Engine.Physics.Core.FrameMagnitudes;
import Game.Engine.Physics.Core.RelationEvaluator;

import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo RADIATION_THERMAL.
 *
 * ── HRFC-022 Corrección Arquitectónica Final ──────────────────────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Conversión de radiación absorbida en variación de temperatura.
 * La energía radiante que un objeto absorbe se convierte en energía térmica
 * a través de su capacidad calorífica:
 *
 *   ΔT = R_absorbida · factor / max(C, 1)
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 * PlanckEvaluator       → transfiere radiación entre pares,
 *                         escribe R_absorbida en FrameMagnitudes.ABSORBED_RADIATION
 * RadiationThermalEvaluator → lee R_absorbida desde FrameState,
 *                             produce ΔTemperature exclusivamente
 *
 * No existe ninguna propiedad puente entre ambos evaluadores.
 * La radiación absorbida es una magnitud derivada transitoria del frame.
 * Nunca persiste en PhysicalState.
 *
 * ── ÚNICO FENÓMENO ────────────────────────────────────────────────────────
 * Este evaluador implementa exclusivamente la conversión energética
 * radiación → calor. No transfiere radiación. No conoce distancias ni pares.
 * Opera sobre cada entidad de forma independiente.
 *
 * ── FUENTE DE DATOS ───────────────────────────────────────────────────────
 * Lee FrameMagnitudes.ABSORBED_RADIATION — escrito por PlanckEvaluator en la misma pasada.
 * Si una entidad no tiene radiación absorbida en FrameState este frame, no actúa.
 *
 * ── PROPIEDADES LEÍDAS (PhysicalState) ───────────────────────────────────
 *   HEAT_CAPACITY  (entidad, fallback 1000.0)
 *
 * ── MAGNITUDES LEÍDAS (FrameState) ───────────────────────────────────────
 *   FrameMagnitudes.ABSORBED_RADIATION  (radiación absorbida por PlanckEvaluator este frame)
 *
 * ── PROPIEDADES ESCRITAS (PhysicalState) ─────────────────────────────────
 *   TEMPERATURE    (ΔT positivo — calor generado por absorción)
 *
 * ── RESTRICCIONES DECLARATIVAS ────────────────────────────────────────────
 *   MIN_DELTA → radiación mínima para operar (1e-9)
 */
public final class RadiationThermalEvaluator implements RelationEvaluator {

    private static final double DEFAULT_MIN_DELTA     = 1e-9;
    private static final double DEFAULT_HEAT_CAPACITY = 1000.0;
    private static final double CONVERSION_FACTOR     = 0.5;

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        double minDelta = constraintValue(relation, RelationConstraint.Type.MIN_DELTA,
                                          DEFAULT_MIN_DELTA);

        for (EvaluationView e : views) {
            if (!e.has(ThermalProperties.TEMPERATURE)) continue;

            // Leer la radiación absorbida del FrameState este frame
            double absorbed = e.frameState().get(FrameMagnitudes.ABSORBED_RADIATION);
            if (absorbed < minDelta) continue;

            double capacity = e.has(ThermalProperties.HEAT_CAPACITY)
                ? Math.max(e.get(ThermalProperties.HEAT_CAPACITY), 1.0)
                : DEFAULT_HEAT_CAPACITY;

            // Fenómeno único: radiación absorbida → calor
            e.add(ThermalProperties.TEMPERATURE, absorbed * CONVERSION_FACTOR / capacity);
        }
    }

    private static double constraintValue(PhysicalRelation           relation,
                                           RelationConstraint.Type     type,
                                           double                      defaultValue) {
        RelationConstraint c = relation.getConstraint(type);
        return c != null ? c.getValue() : defaultValue;
    }
}
