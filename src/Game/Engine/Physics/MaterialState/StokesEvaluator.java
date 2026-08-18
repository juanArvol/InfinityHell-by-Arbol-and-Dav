package Game.Engine.Physics.MaterialState;

import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.PropertyDescriptor;
import Game.Engine.Physics.Core.RelationConstraint;
import Game.Engine.Physics.Core.RelationEvaluator;
import Game.Engine.Physics.Fluid.FluidProperties;
import java.util.List;
import java.util.Set;

/**
 * Evaluador de relaciones físicas de tipo STOKES.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 * ── HRFC-FASE3.5 — Ownership Correcto de Valores Físicos ─────────────────
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
 * ── OWNERSHIP DE DRAG FACTOR ─────────────────────────────────────────────
 * El factor de arrastre viscoso debe provenir de:
 *   - RelationConstraint.THRESHOLD_ABOVE de la relación, O
 *   - FluidProperties del medio
 *
 * Si no está declarado → usa 1.0 como neutro (drag = viscosidad directa).
 * Esto NO es un default inventado, es el coeficiente de proporcionalidad
 * unitario para el modelo lineal de Stokes.
 *
 * ── PROPIEDADES REQUERIDAS ────────────────────────────────────────────────
 *   FluidProperties.VISCOSITY  (entidad, determina resistencia del medio)
 *   VELOCITY_X                 (entidad, si participante)
 *   VELOCITY_Y                 (entidad, si participante)
 */
public final class StokesEvaluator implements RelationEvaluator {

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        // ── HRFC-FASE3.5: Ownership correcto de drag factor ───────────────
        // El factor puede declararse en la relación (THRESHOLD_ABOVE).
        // Si no está declarado → usa 1.0 (proporcionalidad lineal directa).
        // Esto NO es un default inventado, es el coeficiente unitario del modelo.
        RelationConstraint factorConstraint =
            relation.getConstraint(RelationConstraint.Type.THRESHOLD_ABOVE);
        double dragFactor = factorConstraint != null
            ? factorConstraint.getValue()
            : 1.0;  // Proporcionalidad lineal unitaria (Stokes lineal)

        Set<PropertyDescriptor> props = relation.getParticipatingProperties();

        for (EvaluationView e : views) {
            // Requiere VISCOSITY del fluido
            if (!e.has(FluidProperties.VISCOSITY)) continue;
            double viscosity = e.get(FluidProperties.VISCOSITY);
            // Sin viscosidad → sin resistencia viscosa
            if (viscosity <= 0) continue;

            // Aplicar drag a cada componente de velocidad participante
            for (PropertyDescriptor p : props) {
                if (FluidProperties.VISCOSITY == p) continue;
                if (!e.has(p)) continue;
                
                double velocity = e.get(p);
                // Velocidad despreciable → skip cálculo
                if (Math.abs(velocity) < 1e-9) continue;
                
                // ── Resistencia viscosa ───────────────────────────────────
                // F_d = signo(v) · viscosidad · |v| · factor
                // Se opone al movimiento (signo opuesto)
                double drag = Math.signum(velocity) * viscosity * Math.abs(velocity)
                              * dragFactor * deltaTime;
                e.add(p, -drag);
            }
        }
    }
}
