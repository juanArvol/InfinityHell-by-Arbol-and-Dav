package Game.Engine.Physics.MaterialState;

import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.RelationConstraint;
import Game.Engine.Physics.Core.RelationEvaluator;
import Game.Engine.Physics.Fluid.FluidProperties;
import Game.Engine.Physics.Gravity.GravityProperties;
import Game.Engine.Physics.Kinematic.KinematicProperties;
import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo ARCHIMEDES.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 * ── HRFC-023 Auditoría — Eliminación de búsquedas por ID ─────────────────
 * ── HRFC-024 Auditoría — Consistencia Arquitectónica ─────────────────────
 * ── HRFC-FASE3.5 — Ownership Correcto de Valores Físicos ─────────────────
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
 * ── OWNERSHIP DE FACTOR DE FLOTACIÓN ─────────────────────────────────────
 * El factor de flotación (buoyancy factor) debe provenir de:
 *   - RelationConstraint.THRESHOLD_ABOVE de la relación, O
 *   - FluidProperties del medio
 *
 * Si no está declarado → usa 1.0 como neutro (empuje = desplazamiento).
 * Esto NO es un default físico inventado, es el valor teórico para
 * flotación neutral (ρ_objeto = ρ_fluido).
 *
 * ── OWNERSHIP DE MASA ────────────────────────────────────────────────────
 * GravityProperties.MASS DEBE estar presente en la entidad.
 * Si no está presente o es inválida → entidad omitida del fenómeno.
 * NO se inventa masa por defecto.
 *
 * ── PROPIEDADES REQUERIDAS ────────────────────────────────────────────────
 *   FluidProperties.HUMIDITY        (entidad, indica inmersión)
 *   FluidProperties.VISCOSITY       (entidad, aumenta resistencia)
 *   GravityProperties.MASS          (entidad, determina empuje/masa)
 *   KinematicProperties.VELOCITY_Y  (entidad, recibe empuje)
 */
public final class ArchimedesEvaluator implements RelationEvaluator {

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        // ── HRFC-FASE3.5 CORRECCIÓN: Justificación de valores neutros ─────
        // 
        // buoyancyFactor = 1.0 → IDENTIDAD FÍSICA (flotación neutral teórica)
        //   Representa ρ_objeto = ρ_fluido (empuje = peso desplazado)
        //   NO es un default arbitrario, es el valor físico neutro
        //
        RelationConstraint factorConstraint =
            relation.getConstraint(RelationConstraint.Type.THRESHOLD_ABOVE);
        double buoyancyFactor = factorConstraint != null
            ? factorConstraint.getValue()
            : 1.0;  // IDENTIDAD FÍSICA: flotación neutral (ρ_obj = ρ_fluid)

        for (EvaluationView e : views) {
            // Requiere HUMIDITY (indica inmersión en fluido)
            if (!e.has(FluidProperties.HUMIDITY)) continue;
            if (!e.has(KinematicProperties.VELOCITY_Y)) continue;

            double humidity = e.get(FluidProperties.HUMIDITY);
            // Sin inmersión → sin empuje
            if (humidity <= 0) continue;

            // ── VISCOSITY: 0.0 = IDENTIDAD MATEMÁTICA (fluido no viscoso) ─
            // La ausencia de viscosidad NO es un default arbitrario.
            // Representa físicamente un fluido ideal sin resistencia viscosa.
            // Es válido que un fluido no tenga viscosidad configurada.
            double viscosity = e.has(FluidProperties.VISCOSITY)
                ? e.get(FluidProperties.VISCOSITY)
                : 0.0;  // IDENTIDAD MATEMÁTICA: sin resistencia viscosa

            // ── HRFC-FASE3.5: Ownership correcto de masa ──────────────────
            // La masa DEBE estar presente en la entidad.
            // Si no está → entidad omitida del fenómeno.
            if (!e.has(GravityProperties.MASS)) continue;
            
            double mass = e.get(GravityProperties.MASS);
            // Validar masa válida (> 0)
            if (mass <= 0.0) continue;

            // ── Cálculo del empuje ────────────────────────────────────────
            // F_b = humedad · (1 + viscosidad) · factor / masa
            // 
            // El (1.0 + viscosity) representa:
            //   1.0 → componente base del empuje (desplazamiento)
            //   viscosity → resistencia adicional del medio viscoso
            // 
            // Si viscosity = 0 → (1.0 + 0) = 1.0 (empuje puro sin resistencia)
            // Es correcto matemáticamente, NO un default arbitrario.
            double buoyancy = humidity * (1.0 + viscosity) * buoyancyFactor / mass;
            
            // Empuje hacia arriba (reduce VELOCITY_Y — convención: positivo = abajo)
            e.add(KinematicProperties.VELOCITY_Y, -buoyancy * deltaTime);
        }
    }
}
