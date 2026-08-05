package Game.Engine.Physics.Electromagnetic;

import Game.Engine.Physics.Core.EvaluatorRegistry;
import Game.Engine.Physics.Core.PhysicsModule;
import Game.Engine.Physics.Core.RelationRegistry;

/**
 * Módulo de registro del dominio electromagnético.
 *
 * ── RELACIONES ────────────────────────────────────────────────────────────
 *   MAGNETISM         — fuerza entre dipolos magnéticos dentro de radio 128   OHM
 *   SUPERCONDUCTIVITY — resistencia cero por debajo de temperatura crítica    OHM
 *
 * ── EVALUADORES ───────────────────────────────────────────────────────────
 *   Ninguno propio. Las relaciones de este dominio utilizan RelationType.OHM.
 *   La simulación debe disponer de un evaluador registrado para ese tipo.
 *
 * Cuando el dominio requiera comportamiento diferenciado del eléctrico,
 * se crearán evaluadores propios y se registrarán aquí.
 */
public final class ElectromagneticModule implements PhysicsModule {

    @Override
    public void registerRelations(RelationRegistry relations) {
        relations.registerAll(ElectromagneticRelations.all());
    }

    @Override
    public void registerEvaluators(EvaluatorRegistry evaluators) {
        // Sin evaluadores propios.
        // Las relaciones de este dominio utilizan RelationType.OHM.
    }
}
