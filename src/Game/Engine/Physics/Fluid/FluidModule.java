package Game.Engine.Physics.Fluid;

import Game.Engine.Physics.Core.EvaluatorRegistry;
import Game.Engine.Physics.Core.PhysicsModule;
import Game.Engine.Physics.Core.RelationRegistry;
import Game.Engine.Physics.Core.RelationType;

/**
 * Módulo de registro del dominio fluídico.
 *
 * ── RELACIONES ────────────────────────────────────────────────────────────
 *   FLUID_DIFFUSION              humedad ↔ humedad (entre pares)       BERNOULLI
 *   FLUID_AMBIENT_DISSIPATION    humedad → equilibrio ambiental        AMBIENT_DISSIPATION
 *   FLUID_SATURATION_RELEASE     liberación en saturación              FICK
 *
 * ── EVALUADORES ───────────────────────────────────────────────────────────
 *   BERNOULLI → BernoulliEvaluator
 *   FICK      → FickEvaluator
 *
 * La relación FLUID_AMBIENT_DISSIPATION utiliza RelationType.AMBIENT_DISSIPATION.
 * La simulación debe disponer de un evaluador registrado para ese tipo.
 */
public final class FluidModule implements PhysicsModule {

    @Override
    public void registerRelations(RelationRegistry relations) {
        relations.registerAll(FluidRelations.all());
    }

    @Override
    public void registerEvaluators(EvaluatorRegistry evaluators) {
        evaluators.register(RelationType.BERNOULLI, new BernoulliEvaluator());
        evaluators.register(RelationType.FICK,      new FickEvaluator());
    }
}
