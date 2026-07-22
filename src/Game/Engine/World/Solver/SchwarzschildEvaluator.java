package Game.Engine.World.Solver;

import Game.Engine.World.Physics.PhysicalRelation;
import Game.Engine.World.Physics.PropertyDescriptor;
import java.util.List;
import java.util.Set;

/**
 * Evaluador de relaciones físicas de tipo SCHWARZSCHILD.
 *
 * ── HRFC-022 Corrección — Responsabilidad única ───────────────────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Métrica de Schwarzschild: atracción gravitacional entre cuerpos masivos.
 * La fuerza es proporcional al producto de las masas e inversamente
 * proporcional al cuadrado de la distancia (ley de Newton escalada).
 *
 *   F = G · m_a · m_b / d²
 *   Δv = F / m · dt
 *
 * ── ÚNICO FENÓMENO ────────────────────────────────────────────────────────
 * Este evaluador implementa exclusivamente la atracción gravitacional.
 * La absorción de velocidad al cruzar el horizonte de eventos pertenece
 * a EventHorizonEvaluator. No existe ningún código de horizonte aquí.
 *
 * ── PROPIEDADES LEÍDAS ────────────────────────────────────────────────────
 *   mass      (ambas entidades, via participatingProperties)
 *
 * ── PROPIEDADES ESCRITAS ──────────────────────────────────────────────────
 *   velocity_x / velocity_y (deltas de aceleración gravitacional)
 */
public final class SchwarzschildEvaluator implements RelationEvaluator {

    private static final double G_SCALED = 6.674e-4; // G escalada para el juego

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        Set<PropertyDescriptor> props = relation.getParticipatingProperties();

        PropertyDescriptor massProp = findById(props, "mass");
        PropertyDescriptor velXProp = findById(props, "velocity_x");
        PropertyDescriptor velYProp = findById(props, "velocity_y");

        if (massProp == null) return;

        int n = views.size();
        for (int i = 0; i < n - 1; i++) {
            EvaluationView a = views.get(i);
            if (!a.has(massProp)) continue;

            for (int j = i + 1; j < n; j++) {
                EvaluationView b = views.get(j);
                if (!b.has(massProp)) continue;

                double dist = distance(a, b);
                if (dist < 0.1) continue;

                double mA = a.get(massProp);
                double mB = b.get(massProp);

                // Fenómeno único: atracción gravitacional newtoniana
                double force  = G_SCALED * mA * mB / (dist * dist) * deltaTime;
                double accelA = force / Math.max(mA, 0.01);
                double accelB = force / Math.max(mB, 0.01);

                if (velXProp != null) {
                    if (a.has(velXProp)) a.add(velXProp,  accelA * 0.01);
                    if (b.has(velXProp)) b.add(velXProp, -accelB * 0.01);
                }
                if (velYProp != null) {
                    if (a.has(velYProp)) a.add(velYProp,  accelA * 0.01);
                    if (b.has(velYProp)) b.add(velYProp, -accelB * 0.01);
                }
            }
        }
    }

    private static PropertyDescriptor findById(Set<PropertyDescriptor> props, String id) {
        for (PropertyDescriptor p : props)
            if (id.equals(p.getId())) return p;
        return null;
    }

    private static double distance(EvaluationView a, EvaluationView b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
