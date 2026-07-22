package Game.Engine.World.Physics.Electrical;

import Game.Engine.World.Physics.Core.ThermalProperties;
import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.FrameMagnitudes;
import Game.Engine.World.Physics.Core.RelationEvaluator;

import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo JOULE.
 *
 * ── HRFC-022 Corrección Arquitectónica Final ──────────────────────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Efecto Joule: la corriente eléctrica que fluye a través de un conductor
 * con resistencia genera calor proporcional al cuadrado de la intensidad.
 *
 *   Q = I² · R · t   →   ΔT = (I² · factor · dt) / max(C, 1)
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 * OhmEvaluator  → calcula y transfiere ΔCharge,
 *                 escribe corriente I en FrameMagnitudes.CURRENT
 * JouleEvaluator → lee I desde FrameMagnitudes.CURRENT,
 *                  produce ΔTemperature exclusivamente
 *
 * No existe ninguna propiedad puente entre ambos evaluadores.
 * La corriente I es una magnitud derivada transitoria del frame.
 * Nunca persiste en PhysicalState.
 *
 * ── ÚNICO FENÓMENO ────────────────────────────────────────────────────────
 * Este evaluador implementa exclusivamente la disipación de energía eléctrica
 * como calor. No transfiere carga. No conoce conductividades.
 *
 * ── FUENTE DE DATOS ───────────────────────────────────────────────────────
 * Lee FrameMagnitudes.CURRENT — escrito por OhmEvaluator en la misma pasada.
 * Si una entidad no tiene corriente en FrameState este frame, no produce calor.
 *
 * ── PROPIEDADES LEÍDAS (PhysicalState) ───────────────────────────────────
 *   HEAT_CAPACITY  (entidad, fallback 1000.0)
 *
 * ── MAGNITUDES LEÍDAS (FrameState) ───────────────────────────────────────
 *   FrameMagnitudes.CURRENT  (corriente producida por OhmEvaluator este frame)
 *
 * ── PROPIEDADES ESCRITAS (PhysicalState) ─────────────────────────────────
 *   TEMPERATURE    (ΔT positivo — calor generado)
 *
 * ── RESTRICCIONES DECLARATIVAS ────────────────────────────────────────────
 *   MIN_DELTA → corriente mínima para operar (1e-9)
 */
public final class JouleEvaluator implements RelationEvaluator {

    private static final double DEFAULT_MIN_DELTA     = 1e-9;
    private static final double DEFAULT_HEAT_CAPACITY = 1000.0;
    private static final double JOULE_COEFFICIENT     = 0.1;

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        double minDelta = constraintValue(relation, RelationConstraint.Type.MIN_DELTA,
                                          DEFAULT_MIN_DELTA);

        for (EvaluationView e : views) {
            if (!e.has(ThermalProperties.TEMPERATURE)) continue;

            // Leer la corriente acumulada en FrameState este frame
            double current = e.frameState().get(FrameMagnitudes.CURRENT);
            if (current < minDelta) continue;

            double capacity = e.has(ThermalProperties.HEAT_CAPACITY)
                ? Math.max(e.get(ThermalProperties.HEAT_CAPACITY), 1.0)
                : DEFAULT_HEAT_CAPACITY;

            // Fenómeno único: I² · R · t / C → ΔT
            double heat = current * current * JOULE_COEFFICIENT * deltaTime;
            e.add(ThermalProperties.TEMPERATURE, heat / capacity);
        }
    }

    private static double constraintValue(PhysicalRelation           relation,
                                           RelationConstraint.Type     type,
                                           double                      defaultValue) {
        RelationConstraint c = relation.getConstraint(type);
        return c != null ? c.getValue() : defaultValue;
    }
}
