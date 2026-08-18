package Game.Engine.Physics.Gravity;

import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.RelationConstraint;
import Game.Engine.Physics.Core.RelationEvaluator;
import java.util.List;

/**
 * Evaluador de relaciones físicas de tipo NEWTON.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 * ── HRFC-023 Auditoría — Eliminación de búsquedas por ID ─────────────────
 * ── HRFC-024 Auditoría — Consistencia Arquitectónica ─────────────────────
 * ── HRFC-FASE3.5 — Ownership Correcto de Valores Físicos ─────────────────
 * ── HRFC-FASE3.5 CORRECCIÓN — Semántica Aceleración vs Fuerza ────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Aceleraciones uniformes sobre partículas (típicamente gravedad).
 * 
 * SEMÁNTICA: Este evaluador aplica una ACELERACIÓN uniforme, no una fuerza.
 * 
 *   Δv = a · dt
 * 
 * donde 'a' es la aceleración declarada (ej: gravedad g = 9.8 m/s²).
 *
 * La aceleración gravitacional es INDEPENDIENTE de la masa del objeto.
 * Por tanto, este evaluador NO divide por masa.
 *
 * NOTA: Si necesita aplicar FUERZAS (F = m·a), debe existir un evaluador
 * diferente que implemente a = F/m. NewtonEvaluator es específicamente
 * para aceleraciones uniformes constantes.
 *
 * ── OWNERSHIP DE VALORES FÍSICOS ─────────────────────────────────────────
 * La aceleración DEBE declararse explícitamente mediante la restricción
 * THRESHOLD_ABOVE de la PhysicalRelation. Este valor debe provenir del
 * owner correspondiente:
 *
 *   Entity.gravity × Environment.gravityInfluenceY
 *
 * Si la restricción no está presente, el evaluador NO inventa un valor.
 * La relación simplemente no se aplica (skip silencioso).
 *
 * IMPORTANTE: Este evaluador NO posee la aceleración. La recibe desde
 * la relación declarativa que fue configurada por el owner real.
 *
 * ── PROPIEDADES PARTICIPANTES ────────────────────────────────────────────
 * Las propiedades declaradas en getParticipatingProperties() reciben
 * el delta de aceleración · dt directamente (sin división por masa).
 *
 * GravityProperties.MASS puede declararse como participante para indicar
 * qué entidades deben tener masa configurada, pero NO se usa en el cálculo
 * (la aceleración gravitacional es independiente de masa).
 *
 * ── CONTRATO ──────────────────────────────────────────────────────────────
 * REQUIERE:
 *   - RelationConstraint.THRESHOLD_ABOVE → aceleración en u/s² (desde owner)
 *   - Propiedades participantes (velocidades) en entidades
 *
 * OPCIONAL:
 *   - GravityProperties.MASS si se quiere validar presencia de masa
 *
 * NO INVENTA:
 *   - Aceleraciones por defecto
 *   - Valores mínimos arbitrarios
 */
public final class NewtonEvaluator implements RelationEvaluator {

    @Override
    public void evaluate(PhysicalRelation     relation,
                         List<EvaluationView> views,
                         double               deltaTime) {
        // ── HRFC-FASE3.5: Ownership correcto de aceleración ───────────────
        // La aceleración DEBE estar declarada explícitamente en la relación.
        // NO se inventa un valor por defecto.
        // Si no está presente → la relación no se aplica (skip silencioso).
        RelationConstraint accelConstraint =
            relation.getConstraint(RelationConstraint.Type.THRESHOLD_ABOVE);
        
        if (accelConstraint == null) {
            // Sin aceleración declarada → fenómeno no aplicable
            // Esto expone configuraciones incorrectas en lugar de ocultarlas
            return;
        }
        
        // THRESHOLD_ABOVE contiene la aceleración en u/s² (ej: gravedad = 9.8)
        double acceleration = accelConstraint.getValue();
        
        // ── CORRECCIÓN SEMÁNTICA ──────────────────────────────────────────
        // Este evaluador aplica ACELERACIÓN uniforme, no fuerza.
        // La aceleración gravitacional es INDEPENDIENTE de la masa.
        // Por tanto: Δv = a · dt  (NO se divide por masa)
        double deltaV = acceleration * deltaTime;

        for (EvaluationView e : views) {
            // Aplicar delta a todas las propiedades participantes excepto MASS
            for (var p : relation.getParticipatingProperties()) {
                // MASS puede estar declarada para validar presencia,
                // pero NO se usa en el cálculo (aceleración independiente de masa)
                if (p == GravityProperties.MASS) continue;
                if (e.has(p)) e.add(p, deltaV);
            }
        }
    }
}
