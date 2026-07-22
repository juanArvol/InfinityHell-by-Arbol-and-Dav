package Game.Engine.World.Physics.Thermal;

import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.PropertyDescriptor;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.RelationEvaluator;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Evaluador de relaciones físicas de tipo AMBIENT_DISSIPATION.
 *
 * ── HRFC-022 Corrección — Responsabilidad única ───────────────────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Disipación ambiental: decaimiento de cualquier propiedad extensiva
 * hacia su valor de equilibrio (0) a una velocidad proporcional al
 * coeficiente de disipación del material.
 *
 *   Δvalue = −value · coef · factor · dt
 *
 * Este fenómeno es cualitativamente distinto de la difusión entre pares
 * (Fourier, Fick, Ohm): aquí no hay intercambio entre objetos, sino
 * decaimiento individual hacia el ambiente.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 * FickEvaluator               → difusión de masa entre pares (gradiente)
 * FourierEvaluator            → conducción de calor entre pares (gradiente)
 * OhmEvaluator                → transferencia de carga entre pares (gradiente)
 * AmbientDissipationEvaluator → decaimiento individual hacia el ambiente
 *
 * ── ÚNICO FENÓMENO ────────────────────────────────────────────────────────
 * Este evaluador implementa exclusivamente el decaimiento ambiental.
 * Nunca calcula intercambios entre pares. No conoce distancias.
 *
 * ── DISEÑO GENÉRICO ───────────────────────────────────────────────────────
 * El evaluador es agnóstico al dominio físico. La relación declara:
 *   - primera propiedad participante → la que se disipa
 *   - segunda propiedad participante → el coeficiente de disipación
 *
 * Ejemplos de uso:
 *   TEMPERATURE + THERMAL_DIFFUSIVITY → disipación térmica ambiental
 *   CHARGE      + ELECTRICAL_CONDUCTIVITY → disipación eléctrica ambiental
 *   HUMIDITY    + HUMIDITY_ABSORPTION     → disipación fluídica ambiental
 *
 * ── PROPIEDADES LEÍDAS ────────────────────────────────────────────────────
 *   prop[0]  → valor a disipar (declarado primero en participating)
 *   prop[1]  → coeficiente de disipación del material (fallback: MIN_DELTA)
 *
 * ── PROPIEDADES ESCRITAS ──────────────────────────────────────────────────
 *   prop[0]  → delta negativo proporcional al valor actual
 *
 * ── RESTRICCIONES DECLARATIVAS ────────────────────────────────────────────
 *   MIN_DELTA → valor mínimo por debajo del cual no se aplica (1e-6)
 */
public final class AmbientDissipationEvaluator implements RelationEvaluator {

    private static final double DEFAULT_MIN_DELTA   = 1e-6;
    private static final double DEFAULT_COEFFICIENT = 0.05;
    private static final double DECAY_FACTOR        = 0.02;

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        double minDelta = constraintValue(relation, RelationConstraint.Type.MIN_DELTA,
                                          DEFAULT_MIN_DELTA);

        // Resolver la propiedad que se disipa y el coeficiente
        // de la lista de propiedades participantes (orden de declaración).
        Iterator<PropertyDescriptor> it   = relation.getParticipatingProperties().iterator();
        PropertyDescriptor           prop = it.hasNext() ? it.next() : null;
        PropertyDescriptor           coef = it.hasNext() ? it.next() : null;

        if (prop == null) return;

        for (EvaluationView e : views) {
            if (!e.has(prop)) continue;

            double value = e.get(prop);
            if (Math.abs(value) < minDelta) continue;

            double coefficient = (coef != null && e.has(coef))
                ? e.get(coef)
                : DEFAULT_COEFFICIENT;

            // Fenómeno único: decaimiento proporcional hacia el equilibrio (0)
            e.add(prop, -value * coefficient * DECAY_FACTOR * deltaTime);
        }
    }

    private static double constraintValue(PhysicalRelation           relation,
                                           RelationConstraint.Type     type,
                                           double                      defaultValue) {
        RelationConstraint c = relation.getConstraint(type);
        return c != null ? c.getValue() : defaultValue;
    }
}
