package Game.Engine.Physics.Thermal;

import Game.Engine.Physics.Core.EvaluatorRegistry;
import Game.Engine.Physics.Core.PhysicsModule;
import Game.Engine.Physics.Core.RelationRegistry;
import Game.Engine.Physics.Core.RelationType;

/**
 * Módulo de registro del dominio térmico.
 *
 * ── RELACIONES ────────────────────────────────────────────────────────────
 *   VOLUMETRIC_EXPANSION        temperatura → presión interna          PASCAL
 *   THERMAL_CONDUCTION          temperatura ↔ temperatura (pares)      FOURIER
 *   THERMAL_AMBIENT_DISSIPATION temperatura → equilibrio ambiental     AMBIENT_DISSIPATION
 *   THERMAL_EXCESS_CORRECTION   corrección cuando temperatura > 500    HOOKE
 *
 * ── EVALUADORES ───────────────────────────────────────────────────────────
 *   FOURIER             → FourierEvaluator
 *   AMBIENT_DISSIPATION → AmbientDissipationEvaluator
 *
 * Las relaciones VOLUMETRIC_EXPANSION y THERMAL_EXCESS_CORRECTION utilizan
 * RelationType.PASCAL y RelationType.HOOKE respectivamente. La simulación
 * debe disponer de evaluadores registrados para ambos tipos.
 */
public final class ThermalModule implements PhysicsModule {

    @Override
    public void registerRelations(RelationRegistry relations) {
        relations.registerAll(ThermalRelations.all());
    }

    @Override
    public void registerEvaluators(EvaluatorRegistry evaluators) {
        evaluators.register(RelationType.FOURIER,             new FourierEvaluator());
        evaluators.register(RelationType.AMBIENT_DISSIPATION, new AmbientDissipationEvaluator());
    }
}
