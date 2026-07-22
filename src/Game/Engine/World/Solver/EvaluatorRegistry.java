package Game.Engine.World.Solver;

import Game.Engine.World.Physics.RelationType;
import java.util.EnumMap;
import java.util.Map;

/**
 * Registro de evaluadores especializados por RelationType.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * EvaluatorRegistry mapea cada RelationType a su RelationEvaluator
 * especializado. El PhysicsSolver lo consulta para obtener el evaluador
 * correcto dado el tipo de una PhysicalRelation.
 *
 * ── REGISTRO POR DEFECTO ─────────────────────────────────────────────────
 * EvaluatorRegistry.defaults() produce un registro con todos los evaluadores
 * del Core ya registrados, listo para usar sin configuración adicional.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Para un tipo personalizado:
 *   registry.register(RelationType.MI_TIPO, new MiEvaluador());
 *
 * Para reemplazar un evaluador del Core:
 *   registry.register(RelationType.FOURIER, new MiFourierPersonalizado());
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ No contiene lógica física.
 *   ✓ Solo mapea tipos a evaluadores.
 */
public final class EvaluatorRegistry {

    private final Map<RelationType, RelationEvaluator> evaluators =
        new EnumMap<>(RelationType.class);

    // ── Constructor ───────────────────────────────────────────────────────

    /** Crea un registro vacío. */
    public EvaluatorRegistry() {}

    // ── Registro por defecto ──────────────────────────────────────────────

    /**
     * Retorna un registro con todos los evaluadores del Core registrados.
     *
     * Mapeado:
     *   FOURIER              → FourierEvaluator
     *   OHM                  → OhmEvaluator
     *   PASCAL               → PascalEvaluator
     *   BERNOULLI            → BernoulliEvaluator
     *   NEWTON               → NewtonEvaluator
     *   HOOKE                → HookeEvaluator
     *   ARCHIMEDES           → ArchimedesEvaluator
     *   STOKES               → StokesEvaluator
     *   FICK                 → FickEvaluator
     *   SCHWARZSCHILD        → SchwarzschildEvaluator
     *   PLANCK               → PlanckEvaluator
     *   JOULE                → JouleEvaluator
     *   EVENT_HORIZON        → EventHorizonEvaluator
     *   RADIATION_THERMAL    → RadiationThermalEvaluator
     *   AMBIENT_DISSIPATION  → AmbientDissipationEvaluator
     *
     * @return registro con todos los evaluadores del Core.
     */
    public static EvaluatorRegistry defaults() {
        EvaluatorRegistry r = new EvaluatorRegistry();
        r.register(RelationType.FOURIER,             new FourierEvaluator());
        r.register(RelationType.OHM,                 new OhmEvaluator());
        r.register(RelationType.PASCAL,              new PascalEvaluator());
        r.register(RelationType.BERNOULLI,           new BernoulliEvaluator());
        r.register(RelationType.NEWTON,              new NewtonEvaluator());
        r.register(RelationType.HOOKE,               new HookeEvaluator());
        r.register(RelationType.ARCHIMEDES,          new ArchimedesEvaluator());
        r.register(RelationType.STOKES,              new StokesEvaluator());
        r.register(RelationType.FICK,                new FickEvaluator());
        r.register(RelationType.SCHWARZSCHILD,       new SchwarzschildEvaluator());
        r.register(RelationType.PLANCK,              new PlanckEvaluator());
        r.register(RelationType.JOULE,               new JouleEvaluator());
        r.register(RelationType.EVENT_HORIZON,       new EventHorizonEvaluator());
        r.register(RelationType.RADIATION_THERMAL,   new RadiationThermalEvaluator());
        r.register(RelationType.AMBIENT_DISSIPATION, new AmbientDissipationEvaluator());
        return r;
    }

    // ── Mutación ──────────────────────────────────────────────────────────

    /**
     * Registra (o reemplaza) el evaluador para un RelationType.
     *
     * @param type      el tipo de relación. No puede ser null.
     * @param evaluator el evaluador especializado. No puede ser null.
     * @return this (para encadenado).
     */
    public EvaluatorRegistry register(RelationType type, RelationEvaluator evaluator) {
        if (type == null)      throw new IllegalArgumentException("type no puede ser null");
        if (evaluator == null) throw new IllegalArgumentException("evaluator no puede ser null");
        evaluators.put(type, evaluator);
        return this;
    }

    // ── Consulta ──────────────────────────────────────────────────────────

    /**
     * Retorna el evaluador registrado para el tipo dado.
     *
     * @param type el tipo de relación.
     * @return el evaluador, o null si no hay ninguno registrado para ese tipo.
     */
    public RelationEvaluator get(RelationType type) {
        if (type == null) return null;
        return evaluators.get(type);
    }

    /**
     * True si hay un evaluador registrado para el tipo dado.
     *
     * @param type el tipo de relación.
     * @return true si existe un evaluador.
     */
    public boolean has(RelationType type) {
        return type != null && evaluators.containsKey(type);
    }

    /** Número de evaluadores registrados. */
    public int size() { return evaluators.size(); }
}
