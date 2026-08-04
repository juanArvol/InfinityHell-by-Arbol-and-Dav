package Game.Engine.Physics.Gravity;

import Game.Engine.Physics.Gravity.GravityProperties;
import Game.Engine.Physics.Kinematic.KinematicProperties;
import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.RelationEvaluator;

import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo SCHWARZSCHILD.
 *
 * ── HRFC-022 Corrección — Responsabilidad única ───────────────────────────
 * ── HRFC-023 Auditoría — Eliminación de búsquedas por ID ─────────────────
 * ── HRFC-024 Auditoría — Consistencia Arquitectónica ─────────────────────
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
 *   GravityProperties.MASS       (ambas entidades)
 *
 * ── PROPIEDADES ESCRITAS ──────────────────────────────────────────────────
 *   KinematicProperties.VELOCITY_X / VELOCITY_Y (deltas de aceleración)
 */
public final class SchwarzschildEvaluator implements RelationEvaluator {

    private static final double G_SCALED = 6.674e-4; // G escalada para el juego

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
                if (dist < 0.1) continue;

                double mA = a.get(GravityProperties.MASS);
                double mB = b.get(GravityProperties.MASS);

                // Fenómeno único: atracción gravitacional newtoniana
                double force  = G_SCALED * mA * mB / (dist * dist) * deltaTime;
                double accelA = force / Math.max(mA, 0.01);
                double accelB = force / Math.max(mB, 0.01);

                if (a.has(KinematicProperties.VELOCITY_X))
                    a.add(KinematicProperties.VELOCITY_X,  accelA * 0.01);
                if (b.has(KinematicProperties.VELOCITY_X))
                    b.add(KinematicProperties.VELOCITY_X, -accelB * 0.01);

                if (a.has(KinematicProperties.VELOCITY_Y))
                    a.add(KinematicProperties.VELOCITY_Y,  accelA * 0.01);
                if (b.has(KinematicProperties.VELOCITY_Y))
                    b.add(KinematicProperties.VELOCITY_Y, -accelB * 0.01);
            }
        }
    }

    private static double distance(EvaluationView a, EvaluationView b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
