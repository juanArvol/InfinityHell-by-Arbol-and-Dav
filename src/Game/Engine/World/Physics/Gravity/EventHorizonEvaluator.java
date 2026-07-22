package Game.Engine.World.Physics.Gravity;

import Game.Engine.World.Physics.Core.GravityProperties;
import Game.Engine.World.Physics.Core.KinematicProperties;
import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.RelationEvaluator;

import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo EVENT_HORIZON.
 *
 * ── HRFC-022 Corrección — Responsabilidad única ───────────────────────────
 * ── HRFC-023 Auditoría — Eliminación de búsquedas por ID ─────────────────
 * ── HRFC-024 Auditoría — Consistencia Arquitectónica ─────────────────────
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
 *   GravityProperties.MASS                (ambas entidades)
 *   GravityProperties.SCHWARZSCHILD_RADIUS (entidad más masiva)
 *
 * ── PROPIEDADES ESCRITAS ──────────────────────────────────────────────────
 *   KinematicProperties.VELOCITY_X / VELOCITY_Y (se anulan al cruzar)
 */
public final class EventHorizonEvaluator implements RelationEvaluator {

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        int n = views.size();
        for (int i = 0; i < n - 1; i++) {
            EvaluationView a = views.get(i);
            if (!a.has(GravityProperties.MASS)) continue;

            for (int j = i + 1; j < n; j++) {
                EvaluationView b = views.get(j);
                if (!b.has(GravityProperties.MASS)) continue;

                double dist = distance(a, b);
                if (dist < 0.01) continue;

                double mA  = a.get(GravityProperties.MASS);
                double mB  = b.get(GravityProperties.MASS);
                double rsA = a.has(GravityProperties.SCHWARZSCHILD_RADIUS)
                    ? a.get(GravityProperties.SCHWARZSCHILD_RADIUS) : 0.0;
                double rsB = b.has(GravityProperties.SCHWARZSCHILD_RADIUS)
                    ? b.get(GravityProperties.SCHWARZSCHILD_RADIUS) : 0.0;

                // Fenómeno único: absorción al cruzar el horizonte de eventos
                if (rsA > 0 && dist <= rsA && mA > mB) {
                    cancelVelocity(b);
                } else if (rsB > 0 && dist <= rsB && mB > mA) {
                    cancelVelocity(a);
                }
            }
        }
    }

    private static void cancelVelocity(EvaluationView e) {
        if (e.has(KinematicProperties.VELOCITY_X))
            e.add(KinematicProperties.VELOCITY_X,
                  -e.get(KinematicProperties.VELOCITY_X));
        if (e.has(KinematicProperties.VELOCITY_Y))
            e.add(KinematicProperties.VELOCITY_Y,
                  -e.get(KinematicProperties.VELOCITY_Y));
    }

    private static double distance(EvaluationView a, EvaluationView b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
