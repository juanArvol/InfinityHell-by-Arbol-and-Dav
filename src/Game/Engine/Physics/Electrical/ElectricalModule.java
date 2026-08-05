package Game.Engine.Physics.Electrical;

import Game.Engine.Physics.Core.EvaluatorRegistry;
import Game.Engine.Physics.Core.PhysicsModule;
import Game.Engine.Physics.Core.RelationRegistry;
import Game.Engine.Physics.Core.RelationType;

/**
 * Módulo de registro del dominio eléctrico.
 *
 * ── RELACIONES ────────────────────────────────────────────────────────────
 *   ELECTRICAL_TRANSFER          carga ↔ carga (entre pares)           OHM
 *   ELECTRICAL_DISSIPATION       carga → equilibrio ambiental          AMBIENT_DISSIPATION
 *   ELECTRICAL_EXCESS_CORRECTION corrección cuando carga > 10          HOOKE
 *   JOULE_HEATING                corriente² → calor (efecto Joule)     JOULE
 *
 * ── EVALUADORES ───────────────────────────────────────────────────────────
 *   OHM   → OhmEvaluator
 *   JOULE → JouleEvaluator
 *
 * Las relaciones ELECTRICAL_DISSIPATION y ELECTRICAL_EXCESS_CORRECTION utilizan
 * RelationType.AMBIENT_DISSIPATION y RelationType.HOOKE respectivamente.
 * La simulación debe disponer de evaluadores registrados para ambos tipos.
 */
public final class ElectricalModule implements PhysicsModule {

    @Override
    public void registerRelations(RelationRegistry relations) {
        relations.registerAll(ElectricalRelations.all());
    }

    @Override
    public void registerEvaluators(EvaluatorRegistry evaluators) {
        evaluators.register(RelationType.OHM,   new OhmEvaluator());
        evaluators.register(RelationType.JOULE, new JouleEvaluator());
    }
}
