package Game.Engine.World.Solver;

import Game.Engine.World.Physics.PhysicalRelation;
import Game.Engine.World.Physics.PropertyDescriptor;
import java.util.List;
import java.util.Set;

/**
 * Evaluador de relaciones físicas de tipo EVENT_HORIZON.
 *
 * ── HRFC-022 Corrección — Responsabilidad única ───────────────────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Horizonte de eventos: absorción total de velocidad cuando un objeto
 * cruza el radio de Schwarzschild de un cuerpo más masivo.
 *
 * Cuando la distancia entre dos cuerpos cae por debajo del radio de
 * Schwarzschild del más masivo, el menos masivo pierde toda su velocidad
 * instantáneamente — ha cruzado el horizonte de eventos.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 * SchwarzschildEvaluator → atracción gravitacional continua (F = Gm₁m₂/r²)
 * EventHorizonEvaluator  → absorción discontinua al cruzar el horizonte
 *
 * Estas son dos relaciones físicas cualitativamente distintas:
 *   SCHWARZSCHILD  → fenómeno continuo — aceleración proporcional a masa y distancia
 *   EVENT_HORIZON  → fenómeno discontinuo — absorción total al cruzar un umbral
 *
 * ── ÚNICO FENÓMENO ────────────────────────────────────────────────────────
 * Este evaluador implementa exclusivamente la absorción de velocidad.
 * No calcula ninguna fuerza gravitacional. No modifica masas ni posiciones.
 *
 * ── PROPIEDADES LEÍDAS ────────────────────────────────────────────────────
 *   mass                 (ambas entidades, via participatingProperties)
 *   schwarzschild_radius (entidad más masiva, via participatingProperties)
 *
 * ── PROPIEDADES ESCRITAS ──────────────────────────────────────────────────
 *   velocity_x / velocity_y (se anulan en el objeto que cruza el horizonte)
 */
public final class EventHorizonEvaluator implements RelationEvaluator {

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        Set<PropertyDescriptor> props = relation.getParticipatingProperties();

        PropertyDescriptor massProp = findById(props, "mass");
        PropertyDescriptor rsProp   = findById(props, "schwarzschild_radius");
        PropertyDescriptor velXProp = findById(props, "velocity_x");
        PropertyDescriptor velYProp = findById(props, "velocity_y");

        if (massProp == null || rsProp == null) return;

        int n = views.size();
        for (int i = 0; i < n - 1; i++) {
            EvaluationView a = views.get(i);
            if (!a.has(massProp)) continue;

            for (int j = i + 1; j < n; j++) {
                EvaluationView b = views.get(j);
                if (!b.has(massProp)) continue;

                double dist = distance(a, b);
                if (dist < 0.01) continue;

                double mA  = a.get(massProp);
                double mB  = b.get(massProp);
                double rsA = a.has(rsProp) ? a.get(rsProp) : 0.0;
                double rsB = b.has(rsProp) ? b.get(rsProp) : 0.0;

                // Fenómeno único: absorción al cruzar el horizonte de eventos
                if (rsA > 0 && dist <= rsA && mA > mB) {
                    cancelVelocity(b, velXProp, velYProp);
                } else if (rsB > 0 && dist <= rsB && mB > mA) {
                    cancelVelocity(a, velXProp, velYProp);
                }
            }
        }
    }

    private static void cancelVelocity(EvaluationView     e,
                                        PropertyDescriptor velX,
                                        PropertyDescriptor velY) {
        if (velX != null && e.has(velX)) e.add(velX, -e.get(velX));
        if (velY != null && e.has(velY)) e.add(velY, -e.get(velY));
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
