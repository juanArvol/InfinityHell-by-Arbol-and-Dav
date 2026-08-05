package Game.Engine.Physics.Quantum;

import Game.Engine.Physics.Core.EvaluatorRegistry;
import Game.Engine.Physics.Core.PhysicsModule;
import Game.Engine.Physics.Core.RelationRegistry;

/**
 * Módulo de registro del dominio cuántico.
 *
 * ── RELACIONES ────────────────────────────────────────────────────────────
 *   QUANTUM_WAVE_COLLAPSE — colapso de función de onda por proximidad   PLANCK
 *
 * ── EVALUADORES ───────────────────────────────────────────────────────────
 *   Ninguno propio. Las relaciones de este dominio utilizan RelationType.PLANCK.
 *   La simulación debe disponer de un evaluador registrado para ese tipo.
 *
 * Cuando el dominio requiera comportamiento diferenciado del radiante,
 * se crearán evaluadores propios y se registrarán aquí.
 */
public final class QuantumModule implements PhysicsModule {

    @Override
    public void registerRelations(RelationRegistry relations) {
        relations.registerAll(QuantumRelations.all());
    }

    @Override
    public void registerEvaluators(EvaluatorRegistry evaluators) {
        // Sin evaluadores propios.
        // Las relaciones de este dominio utilizan RelationType.PLANCK.
    }
}
