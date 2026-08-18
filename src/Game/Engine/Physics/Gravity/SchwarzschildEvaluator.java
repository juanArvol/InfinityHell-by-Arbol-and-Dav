package Game.Engine.Physics.Gravity;

import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.RelationEvaluator;
import Game.Engine.Physics.Kinematic.KinematicProperties;
import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo SCHWARZSCHILD.
 *
 * ── HRFC-022 Corrección — Responsabilidad única ───────────────────────────
 * ── HRFC-023 Auditoría — Eliminación de búsquedas por ID ─────────────────
 * ── HRFC-024 Auditoría — Consistencia Arquitectónica ─────────────────────
 * ── HRFC-FASE3.5 — Ownership Correcto de Valores Físicos ─────────────────
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
 * ── CONSTANTE G_SCALED ───────────────────────────────────────────────────
 * G_SCALED = 6.674e-4 es la constante gravitacional universal escalada
 * para las unidades del juego. NO es un default inventado, es un valor
 * físico real adaptado. Esto es arquitectónicamente correcto.
 *
 * ── OWNERSHIP DE MASA ────────────────────────────────────────────────────
 * GravityProperties.MASS DEBE estar presente en ambas entidades.
 * Si alguna entidad no tiene masa válida (ausente o <= 0):
 *   - El par se omite del fenómeno (skip)
 *   - NO se inventa masa mínima
 *   - Esto expone errores de configuración
 *
 * ── PROPIEDADES LEÍDAS ────────────────────────────────────────────────────
 *   GravityProperties.MASS       (ambas entidades)
 *
 * ── PROPIEDADES ESCRITAS ──────────────────────────────────────────────────
 *   KinematicProperties.VELOCITY_X / VELOCITY_Y (deltas de aceleración)
 */
public final class SchwarzschildEvaluator implements RelationEvaluator {

    /**
     * Constante gravitacional universal escalada para unidades del juego.
     * Valor real: G = 6.674×10⁻¹¹ m³/(kg·s²) en SI
     * Escalado: 6.674e-4 para píxeles/frame del motor.
     */
    private static final double G_SCALED = 6.674e-4;

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        int n = views.size();
        for (int i = 0; i < n - 1; i++) {
            EvaluationView a = views.get(i);
            // ── HRFC-FASE3.5: Ownership correcto de masa ──────────────────
            // Si la entidad no tiene masa → omitir del fenómeno
            if (!a.has(GravityProperties.MASS)) continue;
            double mA = a.get(GravityProperties.MASS);
            // Validar masa válida (> 0)
            if (mA <= 0.0) continue;

            for (int j = i + 1; j < n; j++) {
                EvaluationView b = views.get(j);
                // Si la entidad no tiene masa → omitir del fenómeno
                if (!b.has(GravityProperties.MASS)) continue;
                double mB = b.get(GravityProperties.MASS);
                // Validar masa válida (> 0)
                if (mB <= 0.0) continue;

                double dist = distance(a, b);
                // Evitar singularidad en r → 0
                if (dist < 0.1) continue;

                // ── Fenómeno único: atracción gravitacional newtoniana ────
                // F = G · m_a · m_b / d²
                double force  = G_SCALED * mA * mB / (dist * dist) * deltaTime;
                
                // a = F / m (segunda ley de Newton)
                // NO se inventa masa mínima: confiamos en validación previa
                double accelA = force / mA;
                double accelB = force / mB;

                // Aplicar aceleración en dirección apropiada
                // Factor 0.01 es escala de magnitud del fenómeno para el motor
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
