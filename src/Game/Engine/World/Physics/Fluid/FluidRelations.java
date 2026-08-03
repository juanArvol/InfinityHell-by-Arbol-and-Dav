package Game.Engine.World.Physics.Fluid;

import Game.Engine.World.Physics.Fluid.FluidProperties;
import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.RelationType;

/**
 * Catálogo de relaciones del dominio fluídico.
 *
 * ── HRFC-025 — Eliminación de la Deuda Histórica CoreProperties / CoreRelations ──
 *
 * ── DOMINIO FÍSICO ────────────────────────────────────────────────────────
 * Este catálogo agrupa exclusivamente las relaciones que describen fenómenos
 * de difusión de masa: cómo la humedad fluye entre objetos por gradiente
 * de concentración, se disipa hacia el ambiente, o se libera al alcanzar
 * el umbral de saturación.
 *
 * Una relación pertenece a este catálogo si y solo si responde a la pregunta:
 *   ¿Modela un fenómeno cuya magnitud primaria es el contenido fluídico (humedad)?
 *
 * No se agrupan relaciones aquí por razones de distribución uniforme ni
 * por herencia histórica. La cohesión del dominio fluídico prevalece.
 *
 * ── RELACIONES INCLUIDAS ──────────────────────────────────────────────────
 *
 *   [1] FLUID_DIFFUSION          humedad ↔ humedad (pares)           BERNOULLI
 *   [2] FLUID_AMBIENT_DISSIPATION humedad → equilibrio               AMBIENT_DISSIPATION
 *   [3] FLUID_SATURATION_RELEASE  corrección fluídica en saturación  FICK
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Estas relaciones no ejecutan comportamiento. Describen el universo.
 * El procedimiento matemático vive exclusivamente en el evaluador especializado.
 *
 * ── CATÁLOGOS SIMÉTRICOS ──────────────────────────────────────────────────
 * ThermalRelations    ↔ ThermalProperties
 * ElectricalRelations ↔ ElectricalProperties
 * FluidRelations      ↔ FluidProperties
 */
public final class FluidRelations {

    private FluidRelations() {}

    // ── Relación 1: Difusión fluídica entre pares ─────────────────────────

    /**
     * La humedad se difunde del objeto con mayor concentración al de menor.
     * La velocidad está modulada por la viscosidad del fluido: alta viscosidad
     * reduce la velocidad de difusión.
     *
     * Fenómeno: BERNOULLI (principio de Bernoulli — flujo en fluidos)
     * Dependencia física: humedad → humedad (entre pares)
     * Evaluador: BernoulliEvaluator
     * Restricciones: MAX_DISTANCE(32), MIN_DELTA(1e-6)
     */
    public static final PhysicalRelation FLUID_DIFFUSION = PhysicalRelation.builder()
        .name("fluid_diffusion")
        .relationType(RelationType.BERNOULLI)
        .participating(
            FluidProperties.HUMIDITY,
            FluidProperties.HUMIDITY_ABSORPTION,
            FluidProperties.VISCOSITY)
        .constraint(RelationConstraint.maxDistance(32.0))
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(100)
        .build();

    // ── Relación 2: Disipación fluídica ambiental ─────────────────────────

    /**
     * La humedad de cada objeto decae hacia el equilibrio (0).
     * La velocidad depende del coeficiente de absorción del material.
     *
     * Fenómeno: AMBIENT_DISSIPATION
     * Propiedad que decae: HUMIDITY
     * Coeficiente: HUMIDITY_ABSORPTION
     * Evaluador: AmbientDissipationEvaluator
     */
    public static final PhysicalRelation FLUID_AMBIENT_DISSIPATION = PhysicalRelation.builder()
        .name("fluid_ambient_dissipation")
        .relationType(RelationType.AMBIENT_DISSIPATION)
        .participating(
            FluidProperties.HUMIDITY,
            FluidProperties.HUMIDITY_ABSORPTION)
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(110)
        .build();

    // ── Relación 3: Liberación en saturación fluídica ─────────────────────

    /**
     * Cuando la humedad supera el umbral de saturación (0.6), el exceso
     * se libera progresivamente hacia el ambiente.
     *
     * Fenómeno: FICK (difusión de masa excess hacia el ambiente)
     * Evaluador: FickEvaluator
     * Restricciones: THRESHOLD_ABOVE(HUMIDITY, 0.6)
     */
    public static final PhysicalRelation FLUID_SATURATION_RELEASE = PhysicalRelation.builder()
        .name("fluid_saturation_release")
        .relationType(RelationType.FICK)
        .participating(
            FluidProperties.HUMIDITY,
            FluidProperties.HUMIDITY_ABSORPTION)
        .constraint(RelationConstraint.thresholdAbove(FluidProperties.HUMIDITY, 0.6))
        .priority(115)
        .build();

    // ── Colección completa ────────────────────────────────────────────────

    /**
     * Todas las relaciones del dominio fluídico.
     *
     * @return array con las 3 relaciones fluídicas.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] {
            FLUID_DIFFUSION,
            FLUID_AMBIENT_DISSIPATION,
            FLUID_SATURATION_RELEASE
        };
    }
}
