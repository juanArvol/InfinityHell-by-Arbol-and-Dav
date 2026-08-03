package Game.Engine.World.Physics.MaterialState;

import Game.Engine.World.Physics.Fluid.FluidProperties;
import Game.Engine.World.Physics.Gravity.GravityProperties;
import Game.Engine.World.Physics.Kinematic.KinematicProperties;
import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.RelationEvaluator;

import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo ARCHIMEDES.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 * ── HRFC-023 Auditoría — Eliminación de búsquedas por ID ─────────────────
 * ── HRFC-024 Auditoría — Consistencia Arquitectónica ─────────────────────
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
 *   CoreProperties.HUMIDITY        (entidad)
 *   CoreProperties.VISCOSITY       (entidad, fallback a 0.0)
 *   GravityProperties.MASS         (entidad, fallback a 1.0)
 *   KinematicProperties.VELOCITY_Y (entidad)
 */
public final class ArchimedesEvaluator implements RelationEvaluator {

    private static final double BUOYANCY_FACTOR = 0.5;

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        for (EvaluationView e : views) {
            if (!e.has(FluidProperties.HUMIDITY))              continue;
            if (!e.has(KinematicProperties.VELOCITY_Y))      continue;

            double humidity = e.get(FluidProperties.HUMIDITY);
            if (humidity <= 0) continue;

            double viscosity = e.has(FluidProperties.VISCOSITY)
                ? e.get(FluidProperties.VISCOSITY) : 0.0;
            double m = e.has(GravityProperties.MASS)
                ? Math.max(e.get(GravityProperties.MASS), 0.01) : 1.0;

            double buoyancy = humidity * (1.0 + viscosity) * BUOYANCY_FACTOR / m;
            // Empuje hacia arriba (reduce VELOCITY_Y — convención: positivo = abajo)
            e.add(KinematicProperties.VELOCITY_Y, -buoyancy * deltaTime);
        }
    }
}
