package Game.Engine.World.Physics.MaterialState;

import Game.Engine.World.Physics.Core.FluidProperties;
import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.PropertyDescriptor;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.RelationEvaluator;
import java.util.List;
import java.util.Set;

/**
 * Evaluador de relaciones físicas de tipo STOKES.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Ley de Stokes: resistencia viscosa que se opone al movimiento de un objeto
 * en un fluido. La fuerza de arrastre es proporcional a la viscosidad del
 * fluido y a la velocidad del objeto.
 *
 *   F_d = viscosidad · velocidad · factor
 *   Δv  = −F_d · dt    (se opone al movimiento)
 *
 * Operación sobre cada componente de velocidad declarada como participante.
 *
 * ── PROPIEDADES REQUERIDAS ────────────────────────────────────────────────
 *   VISCOSITY    (entidad)
 *   VELOCITY_X   (entidad, si participante)
 *   VELOCITY_Y   (entidad, si participante)
 */
public final class StokesEvaluator implements RelationEvaluator {

    private static final double DRAG_FACTOR = 0.05;

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        Set<PropertyDescriptor> props = relation.getParticipatingProperties();

        for (EvaluationView e : views) {
            if (!e.has(FluidProperties.VISCOSITY)) continue;
            double viscosity = e.get(FluidProperties.VISCOSITY);
            if (viscosity <= 0) continue;

            for (PropertyDescriptor p : props) {
                if (FluidProperties.VISCOSITY == p) continue;
                if (!e.has(p)) continue;
                double velocity = e.get(p);
                if (Math.abs(velocity) < 1e-9) continue;
                // La fuerza de arrastre se opone al movimiento
                double drag = Math.signum(velocity) * viscosity * Math.abs(velocity)
                              * DRAG_FACTOR * deltaTime;
                e.add(p, -drag);
            }
        }
    }
}
