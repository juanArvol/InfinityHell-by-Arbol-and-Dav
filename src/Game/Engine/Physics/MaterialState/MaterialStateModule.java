package Game.Engine.Physics.MaterialState;

import Game.Engine.Physics.Core.EvaluatorRegistry;
import Game.Engine.Physics.Core.PhysicsModule;
import Game.Engine.Physics.Core.RelationRegistry;
import Game.Engine.Physics.Core.RelationType;

/**
 * Módulo de registro del dominio de estado del material.
 *
 * ── RELACIONES ────────────────────────────────────────────────────────────
 *   CRYSTALLIZATION          temperatura negativa + humedad → cristalización  FICK
 *   PLASMA_TRANSITION        temperatura alta → ionización del material        PLANCK
 *   SURFACE_TENSION_RELATION cohesión entre fluidos próximos                  STOKES
 *
 * ── EVALUADORES ───────────────────────────────────────────────────────────
 *   ARCHIMEDES → ArchimedesEvaluator
 *   STOKES     → StokesEvaluator
 *
 * Las relaciones CRYSTALLIZATION y PLASMA_TRANSITION utilizan RelationType.FICK
 * y RelationType.PLANCK respectivamente. La simulación debe disponer de
 * evaluadores registrados para ambos tipos.
 */
public final class MaterialStateModule implements PhysicsModule {

    @Override
    public void registerRelations(RelationRegistry relations) {
        relations.registerAll(MaterialStateRelations.all());
    }

    @Override
    public void registerEvaluators(EvaluatorRegistry evaluators) {
        evaluators.register(RelationType.ARCHIMEDES, new ArchimedesEvaluator());
        evaluators.register(RelationType.STOKES,     new StokesEvaluator());
    }
}
