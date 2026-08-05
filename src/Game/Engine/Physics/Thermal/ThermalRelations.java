package Game.Engine.Physics.Thermal;

import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.RelationConstraint;
import Game.Engine.Physics.Core.RelationType;
import Game.Engine.Physics.Mechanical.MechanicalProperties;

/**
 * Catálogo de relaciones del dominio térmico.
 *
 * ── HRFC-025 — Eliminación de la Deuda Histórica CoreProperties / CoreRelations ──
 * ── HRFC — Auditoría Arquitectónica Final ────────────────────────────────
 *
 * ── DOMINIO FÍSICO ────────────────────────────────────────────────────────
 * Este catálogo agrupa exclusivamente las relaciones cuya magnitud motriz
 * es la energía térmica: fenómenos originados por la temperatura.
 *
 * Una relación pertenece a este catálogo si y solo si responde a la pregunta:
 *   ¿Modela un fenómeno cuya magnitud primaria es la energía térmica?
 *
 * ── RELACIONES INCLUIDAS ──────────────────────────────────────────────────
 *
 *   [1] THERMAL_CONDUCTION           temperatura ↔ temperatura (pares)      FOURIER
 *   [2] THERMAL_AMBIENT_DISSIPATION  temperatura → equilibrio               AMBIENT_DISSIPATION
 *   [3] THERMAL_EXCESS_CORRECTION    corrección cuando temperatura > 500    HOOKE
 *   [4] VOLUMETRIC_EXPANSION         temperatura → presión interna          PASCAL
 *
 * ── NOTA ARQUITECTÓNICA: VOLUMETRIC_EXPANSION ────────────────────────────
 * Aunque VOLUMETRIC_EXPANSION produce un delta en PRESSURE (dominio mecánico),
 * su causa física es térmica: la temperatura genera expansión del material,
 * que se manifiesta como presión interna. La magnitud motriz es TEMPERATURE.
 * En termodinámica, la expansión volumétrica es un fenómeno térmico.
 * Por eso pertenece a ThermalRelations, no a MechanicalRelations.
 *
 * ── NOTA ARQUITECTÓNICA: RADIATION_THERMAL_CONVERSION ────────────────────
 * La conversión de radiación absorbida en calor (RADIATION_THERMAL) pertenece
 * a RadiationRelations porque su magnitud motriz es la radiación recibida,
 * producida por PlanckEvaluator (dominio radiante). Aunque el efecto final
 * sea térmico, la causa es radiante. RadiationModule es el responsable de
 * registrar esa relación y su evaluador (RadiationThermalEvaluator).
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Estas relaciones no ejecutan comportamiento. Describen el universo.
 * El procedimiento matemático vive exclusivamente en el evaluador especializado.
 */
public final class ThermalRelations {

    private ThermalRelations() {}

    // ── Relación 1: Conducción térmica entre pares ────────────────────────

    /**
     * El calor fluye del objeto más caliente al más frío por conducción.
     * La velocidad depende de la menor conductividad de los dos.
     * La inercia depende de la capacidad calorífica del receptor.
     *
     * Fenómeno: FOURIER (ley de conducción térmica de Fourier)
     * Dependencia física: temperatura → temperatura (entre pares)
     * Evaluador: FourierEvaluator
     * Restricciones: MAX_DISTANCE(32), MIN_DELTA(1e-6)
     */
    public static final PhysicalRelation THERMAL_CONDUCTION = PhysicalRelation.builder()
        .name("thermal_conduction")
        .relationType(RelationType.FOURIER)
        .participating(
            ThermalProperties.TEMPERATURE,
            ThermalProperties.THERMAL_CONDUCTIVITY,
            ThermalProperties.HEAT_CAPACITY)
        .constraint(RelationConstraint.maxDistance(32.0))
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(100)
        .build();

    // ── Relación 2: Disipación térmica ambiental ──────────────────────────

    /**
     * La temperatura de cada objeto decae hacia el equilibrio ambiental (0).
     * La velocidad de disipación depende de la difusividad térmica del material.
     *
     * Fenómeno: AMBIENT_DISSIPATION
     * Propiedad que decae: TEMPERATURE
     * Coeficiente: THERMAL_DIFFUSIVITY
     * Evaluador: AmbientDissipationEvaluator
     */
    public static final PhysicalRelation THERMAL_AMBIENT_DISSIPATION = PhysicalRelation.builder()
        .name("thermal_ambient_dissipation")
        .relationType(RelationType.AMBIENT_DISSIPATION)
        .participating(
            ThermalProperties.TEMPERATURE,
            ThermalProperties.THERMAL_DIFFUSIVITY)
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(110)
        .build();

    // ── Relación 3: Corrección de exceso térmico ──────────────────────────

    /**
     * Cuando la temperatura supera un umbral crítico (500 unidades), la energía
     * en exceso se disipa más agresivamente para evitar divergencia numérica.
     * La causa es térmica: la condición de activación es temperatura > 500.
     *
     * Fenómeno: HOOKE (ley de Hooke — fuerza restauradora elástica)
     * Evaluador: HookeEvaluator
     * Restricciones: THRESHOLD_ABOVE(TEMPERATURE, 500)
     */
    public static final PhysicalRelation THERMAL_EXCESS_CORRECTION = PhysicalRelation.builder()
        .name("thermal_excess_correction")
        .relationType(RelationType.HOOKE)
        .participating(
            ThermalProperties.TEMPERATURE,
            MechanicalProperties.PRESSURE,
            MechanicalProperties.COMPRESSIBILITY)
        .constraint(RelationConstraint.thresholdAbove(ThermalProperties.TEMPERATURE, 500.0))
        .priority(120)
        .build();

    // ── Relación 4: Expansión volumétrica ─────────────────────────────────

    /**
     * La temperatura genera presión interna proporcional a la incompresibilidad.
     * Fenómeno térmico: la expansión del material por calor produce presión.
     *
     * Aunque el efecto resultante es PRESSURE (dominio mecánico), la causa
     * física es la temperatura. Pertenece al dominio térmico.
     *
     * Fenómeno: PASCAL (expansión volumétrica / ley de Pascal)
     * Dependencia física: temperatura → presión
     * Evaluador: PascalEvaluator
     * Restricciones: MIN_DELTA(1e-6)
     */
    public static final PhysicalRelation VOLUMETRIC_EXPANSION = PhysicalRelation.builder()
        .name("volumetric_expansion")
        .relationType(RelationType.PASCAL)
        .participating(
            ThermalProperties.TEMPERATURE,
            MechanicalProperties.PRESSURE,
            MechanicalProperties.COMPRESSIBILITY)
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(5)
        .build();

    // ── Colección completa ────────────────────────────────────────────────

    /**
     * Todas las relaciones del dominio térmico.
     *
     * @return array con las 4 relaciones térmicas.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] {
            VOLUMETRIC_EXPANSION,
            THERMAL_CONDUCTION,
            THERMAL_AMBIENT_DISSIPATION,
            THERMAL_EXCESS_CORRECTION
        };
    }
}
