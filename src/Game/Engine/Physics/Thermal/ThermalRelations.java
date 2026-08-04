package Game.Engine.Physics.Thermal;

import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.PropertyDescriptor;
import Game.Engine.Physics.Core.RelationConstraint;
import Game.Engine.Physics.Core.RelationType;
import Game.Engine.Physics.Mechanical.MechanicalProperties;
import Game.Engine.Physics.Thermal.ThermalProperties;

/**
 * Catálogo de relaciones del dominio térmico.
 *
 * ── HRFC-025 — Eliminación de la Deuda Histórica CoreProperties / CoreRelations ──
 *
 * ── DOMINIO FÍSICO ────────────────────────────────────────────────────────
 * Este catálogo agrupa exclusivamente las relaciones que describen fenómenos
 * de transferencia, disipación y corrección de energía térmica.
 *
 * Una relación pertenece a este catálogo si y solo si responde a la pregunta:
 *   ¿Modela un fenómeno cuya magnitud primaria es la energía térmica?
 *
 * No se agrupan relaciones aquí por razones de distribución uniforme ni
 * por herencia histórica. La cohesión del dominio térmico prevalece.
 *
 * ── RELACIONES INCLUIDAS ──────────────────────────────────────────────────
 *
 *   [1] THERMAL_CONDUCTION           temperatura ↔ temperatura (pares)      FOURIER
 *   [2] THERMAL_AMBIENT_DISSIPATION  temperatura → equilibrio               AMBIENT_DISSIPATION
 *   [3] THERMAL_EXCESS_CORRECTION    corrección cuando temperatura > 500    HOOKE
 *   [4] VOLUMETRIC_EXPANSION         temperatura → presión interna          PASCAL
 *   [5] RADIATION_THERMAL_CONVERSION radiación absorbida → temperatura      RADIATION_THERMAL
 *
 * ── NOTA ARQUITECTÓNICA: VOLUMETRIC_EXPANSION ────────────────────────────
 * Aunque VOLUMETRIC_EXPANSION produce un delta en PRESSURE (dominio mecánico),
 * su causa física es térmica: la temperatura genera expansión del material,
 * que se manifiesta como presión interna. La magnitud motriz es TEMPERATURE.
 * En termodinámica, la expansión volumétrica es un fenómeno térmico.
 * Por eso pertenece a ThermalRelations, no a MechanicalRelations.
 *
 * ── NOTA ARQUITECTÓNICA: RADIATION_THERMAL_CONVERSION ────────────────────
 * Esta relación convierte radiación absorbida (escrita en FrameState por
 * PlanckEvaluator) en variación de temperatura. El efecto final es térmico:
 * la propiedad modificada es TEMPERATURE. Pertenece a ThermalRelations.
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
     * excess se disipa más agresivamente para evitar divergencia numérica.
     *
     * La presión acumulada por la expansión volumétrica se corrige por la
     * compresibilidad del material. La causa sigue siendo térmica.
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
     * física es la temperatura. Este fenómeno es la expansión volumétrica
     * térmica, clasificada en el dominio térmico.
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

    // ── Relación 5: Conversión térmica de radiación absorbida ─────────────

    /**
     * La radiación absorbida este frame (leída desde FrameMagnitudes.ABSORBED_RADIATION,
     * escrita por PlanckEvaluator) se convierte en calor a través de la
     * capacidad calorífica del material.
     *
     * Evaluada después de RADIATION en RadiationRelations (prio 110 → 112).
     * No existe ninguna propiedad puente en PhysicalState.
     *
     * Composición vía FrameState:
     *   PLANCK            (prio 110) → ΔRadiation + escribe FrameMagnitudes.ABSORBED_RADIATION
     *   RADIATION_THERMAL (prio 112) → lee FrameMagnitudes.ABSORBED_RADIATION → ΔTemperature
     *
     * Fenómeno: RADIATION_THERMAL (conversión de radiación en calor)
     * Evaluador: RadiationThermalEvaluator
     */
    public static final PhysicalRelation RADIATION_THERMAL_CONVERSION = PhysicalRelation.builder()
        .name("radiation_thermal_conversion")
        .relationType(RelationType.RADIATION_THERMAL)
        .participating(
            ThermalProperties.TEMPERATURE,
            ThermalProperties.HEAT_CAPACITY)
        .constraint(RelationConstraint.minDelta(1e-9))
        .priority(112)
        .build();

    // ── Colección completa ────────────────────────────────────────────────

    /**
     * Todas las relaciones del dominio térmico.
     *
     * @return array con las 5 relaciones térmicas.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] {
            VOLUMETRIC_EXPANSION,
            THERMAL_CONDUCTION,
            THERMAL_AMBIENT_DISSIPATION,
            THERMAL_EXCESS_CORRECTION,
            RADIATION_THERMAL_CONVERSION
        };
    }
}
