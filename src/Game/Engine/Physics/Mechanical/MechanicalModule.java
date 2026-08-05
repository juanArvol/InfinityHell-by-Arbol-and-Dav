package Game.Engine.Physics.Mechanical;

import Game.Engine.Physics.Core.EvaluatorRegistry;
import Game.Engine.Physics.Core.PhysicsModule;
import Game.Engine.Physics.Core.RelationRegistry;
import Game.Engine.Physics.Core.RelationType;

/**
 * Módulo de registro del dominio mecánico.
 *
 * ── RELACIONES ────────────────────────────────────────────────────────────
 *   (catálogo actualmente vacío — ver MechanicalRelations)
 *
 * ── EVALUADORES ───────────────────────────────────────────────────────────
 *   PASCAL → PascalEvaluator
 *   HOOKE  → HookeEvaluator
 *
 * PascalEvaluator y HookeEvaluator implementan leyes mecánicas utilizadas
 * también por relaciones de otros dominios físicos (térmico, eléctrico).
 * Cualquier dominio que declare relaciones con RelationType.PASCAL o
 * RelationType.HOOKE requiere que la simulación disponga de evaluadores
 * para esos tipos.
 */
public final class MechanicalModule implements PhysicsModule {

    @Override
    public void registerRelations(RelationRegistry relations) {
        relations.registerAll(MechanicalRelations.all());
    }

    @Override
    public void registerEvaluators(EvaluatorRegistry evaluators) {
        evaluators.register(RelationType.PASCAL, new PascalEvaluator());
        evaluators.register(RelationType.HOOKE,  new HookeEvaluator());
    }
}
