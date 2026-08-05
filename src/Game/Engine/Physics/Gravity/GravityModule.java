package Game.Engine.Physics.Gravity;

import Game.Engine.Physics.Core.EvaluatorRegistry;
import Game.Engine.Physics.Core.PhysicsModule;
import Game.Engine.Physics.Core.RelationRegistry;
import Game.Engine.Physics.Core.RelationType;

/**
 * Módulo de registro del dominio gravitacional.
 *
 * ── RELACIONES ────────────────────────────────────────────────────────────
 *   BLACK_HOLE_GRAVITY — atracción gravitacional relativista F = G·m_a·m_b / d²  SCHWARZSCHILD
 *   BLACK_HOLE_HORIZON — absorción al cruzar el horizonte de eventos              EVENT_HORIZON
 *
 * ── EVALUADORES ───────────────────────────────────────────────────────────
 *   NEWTON        → NewtonEvaluator
 *   SCHWARZSCHILD → SchwarzschildEvaluator
 *   EVENT_HORIZON → EventHorizonEvaluator
 *
 * NewtonEvaluator implementa la segunda ley de Newton (F = m·a).
 * Es una ley genérica aplicable a cualquier aceleración uniforme —
 * su uso no se limita a las relaciones declaradas en este dominio.
 */
public final class GravityModule implements PhysicsModule {

    @Override
    public void registerRelations(RelationRegistry relations) {
        relations.registerAll(GravityRelations.all());
    }

    @Override
    public void registerEvaluators(EvaluatorRegistry evaluators) {
        evaluators.register(RelationType.NEWTON,        new NewtonEvaluator());
        evaluators.register(RelationType.SCHWARZSCHILD, new SchwarzschildEvaluator());
        evaluators.register(RelationType.EVENT_HORIZON, new EventHorizonEvaluator());
    }
}
