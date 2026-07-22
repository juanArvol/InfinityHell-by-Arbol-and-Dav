package Game.Engine.World.Solver;

import Game.Engine.World.Physics.CoreProperties;
import Game.Engine.World.Physics.PhysicalRelation;
import Game.Engine.World.Physics.PropertyDescriptor;
import Game.Engine.World.Physics.RelationConstraint;
import java.util.List;
import java.util.Set;

/**
 * Evaluador de relaciones físicas de tipo ARCHIMEDES.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Principio de Arquímedes: empuje vertical en objetos inmersos en fluido.
 *
 * Un objeto con HUMIDITY alta (sumergido) experimenta una fuerza de empuje
 * vertical opuesta a la gravedad. El empuje es proporcional a la humedad del
 * entorno y a la viscosidad del fluido, e inversamente proporcional a la masa.
 *
 *   F_b = humedad · (1 + viscosidad) · factor / masa
 *   Δvy = −F_b · dt      (negativo = hacia arriba, opuesto a gravedad)
 *
 * ── PROPIEDADES REQUERIDAS ────────────────────────────────────────────────
 *   HUMIDITY    (entidad)
 *   VISCOSITY   (entidad, fallback a 0.0)
 *   MASS        (entidad via participatingProperties, fallback a 1.0)
 *   VELOCITY_Y  (entidad via participatingProperties)
 */
public final class ArchimedesEvaluator implements RelationEvaluator {

    private static final double BUOYANCY_FACTOR = 0.5;

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        Set<PropertyDescriptor> props = relation.getParticipatingProperties();
        PropertyDescriptor velocityY = findById(props, "velocity_y");
        PropertyDescriptor mass      = findById(props, "mass");

        if (velocityY == null) return;

        for (EvaluationView e : views) {
            if (!e.has(CoreProperties.HUMIDITY)) continue;
            if (!e.has(velocityY))               continue;

            double humidity = e.get(CoreProperties.HUMIDITY);
            if (humidity <= 0) continue;

            double viscosity = e.has(CoreProperties.VISCOSITY)
                ? e.get(CoreProperties.VISCOSITY) : 0.0;
            double m = (mass != null && e.has(mass))
                ? Math.max(e.get(mass), 0.01) : 1.0;

            double buoyancy = humidity * (1.0 + viscosity) * BUOYANCY_FACTOR / m;
            // Empuje hacia arriba (reduce VELOCITY_Y — convención: positivo = abajo)
            e.add(velocityY, -buoyancy * deltaTime);
        }
    }

    private static PropertyDescriptor findById(Set<PropertyDescriptor> props, String id) {
        for (PropertyDescriptor p : props)
            if (id.equals(p.getId())) return p;
        return null;
    }
}
