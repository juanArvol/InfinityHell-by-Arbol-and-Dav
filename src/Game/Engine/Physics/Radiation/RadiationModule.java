package Game.Engine.Physics.Radiation;

import Game.Engine.Physics.Core.EvaluatorRegistry;
import Game.Engine.Physics.Core.PhysicsModule;
import Game.Engine.Physics.Core.RelationRegistry;
import Game.Engine.Physics.Core.RelationType;
import Game.Engine.Physics.Thermal.RadiationThermalEvaluator;

/**
 * Módulo de registro del dominio radiante.
 *
 * ── RELACIONES ────────────────────────────────────────────────────────────
 *   RADIATION         — transferencia de radiación entre pares   PLANCK
 *   RADIATION_THERMAL — radiación absorbida → calor              RADIATION_THERMAL
 *
 * ── EVALUADORES ───────────────────────────────────────────────────────────
 *   PLANCK            → PlanckEvaluator
 *   RADIATION_THERMAL → RadiationThermalEvaluator
 */
public final class RadiationModule implements PhysicsModule {

    @Override
    public void registerRelations(RelationRegistry relations) {
        relations.registerAll(RadiationRelations.all());
    }

    @Override
    public void registerEvaluators(EvaluatorRegistry evaluators) {
        evaluators.register(RelationType.PLANCK,            new PlanckEvaluator());
        evaluators.register(RelationType.RADIATION_THERMAL, new RadiationThermalEvaluator());
    }
}
