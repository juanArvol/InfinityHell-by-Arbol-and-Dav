package Game.Engine.Physics.Kinematic;

import Game.Engine.Physics.Core.EvaluatorRegistry;
import Game.Engine.Physics.Core.PhysicsModule;
import Game.Engine.Physics.Core.RelationRegistry;
import Game.Engine.Physics.Core.RelationType;

/**
 * Módulo de registro del dominio cinemático.
 *
 * ── RELACIONES ────────────────────────────────────────────────────────────
 *   GRAVITY                    aceleración vertical constante → VELOCITY_Y  NEWTON
 *   FRICTION_HEAT              calor generado por rozamiento cinético        FRICTION_THERMAL
 *   KINETIC_ENERGY_DISSIPATION pérdida de energía cinética → calor + presión KINETIC_DISSIPATION
 *
 * ── EVALUADORES ───────────────────────────────────────────────────────────
 *   FRICTION_THERMAL     → FrictionThermalEvaluator
 *   KINETIC_DISSIPATION  → KineticDissipationEvaluator
 *
 * La relación GRAVITY utiliza RelationType.NEWTON.
 * La simulación debe disponer de un evaluador registrado para ese tipo.
 *
 * Para que la integración cinemática sea efectiva, las entidades deben:
 *   1. Tener Physics2DComponent         (Kinematic Physics activo).
 *   2. Tener SimulationContextComponent (contexto compuesto).
 *   3. Tener KinematicBridge como Component.
 */
public final class KinematicModule implements PhysicsModule {

    @Override
    public void registerRelations(RelationRegistry relations) {
        relations.registerAll(KinematicRelations.all());
        relations.registerAll(KinematicDerivedRelations.all());
    }

    @Override
    public void registerEvaluators(EvaluatorRegistry evaluators) {
        evaluators.register(RelationType.FRICTION_THERMAL,    new FrictionThermalEvaluator());
        evaluators.register(RelationType.KINETIC_DISSIPATION, new KineticDissipationEvaluator());
    }
}
