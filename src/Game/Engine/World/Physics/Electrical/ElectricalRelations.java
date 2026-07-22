package Game.Engine.World.Physics.Electrical;

import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.PropertyDescriptor;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.RelationType;
import Game.Engine.World.Physics.Core.ElectricalProperties;
import Game.Engine.World.Physics.Core.MechanicalProperties;
import Game.Engine.World.Physics.Core.ThermalProperties;

/**
 * Catálogo de relaciones del dominio eléctrico.
 *
 * ── HRFC-025 — Eliminación de la Deuda Histórica CoreProperties / CoreRelations ──
 *
 * ── DOMINIO FÍSICO ────────────────────────────────────────────────────────
 * Este catálogo agrupa exclusivamente las relaciones que describen fenómenos
 * de transferencia, disipación y corrección de carga eléctrica, así como
 * los efectos secundarios producidos por el flujo de corriente.
 *
 * Una relación pertenece a este catálogo si y solo si responde a la pregunta:
 *   ¿Modela un fenómeno cuya magnitud primaria es la carga eléctrica o la corriente?
 *
 * No se agrupan relaciones aquí por razones de distribución uniforme ni
 * por herencia histórica. La cohesión del dominio eléctrico prevalece.
 *
 * ── RELACIONES INCLUIDAS ──────────────────────────────────────────────────
 *
 *   [1] ELECTRICAL_TRANSFER          carga ↔ carga (pares)              OHM
 *   [2] ELECTRICAL_DISSIPATION       carga → equilibrio                 AMBIENT_DISSIPATION
 *   [3] ELECTRICAL_EXCESS_CORRECTION corrección cuando carga > 10       HOOKE
 *   [4] JOULE_HEATING                corriente² → calor                 JOULE
 *
 * ── NOTA ARQUITECTÓNICA: JOULE_HEATING ────────────────────────────────────
 * JOULE_HEATING produce un delta en TEMPERATURE (dominio térmico), pero su
 * causa es eléctrica: la corriente que fluye a través de una resistencia
 * genera calor. Es un efecto secundario del fenómeno eléctrico (Ohm).
 * La relación JOULE_HEATING forma parte del pipeline eléctrico:
 *   OHM (prio 100) → transfiere carga + escribe FrameMagnitudes.CURRENT
 *   JOULE (prio 105) → lee CURRENT → produce ΔTemperature
 * Por eso pertenece a ElectricalRelations, no a ThermalRelations.
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
public final class ElectricalRelations {

    private ElectricalRelations() {}

    // ── Relación 1: Transferencia eléctrica entre pares ───────────────────

    /**
     * La carga eléctrica fluye del objeto con mayor potencial al de menor.
     * La velocidad depende de la conductividad eléctrica mínima de los dos.
     *
     * OhmEvaluator escribe la corriente calculada en FrameMagnitudes.CURRENT
     * para que JOULE_HEATING la consuma en la misma pasada del frame.
     * No existe ninguna propiedad puente en PhysicalState.
     *
     * Fenómeno: OHM (ley de Ohm)
     * Dependencia física: carga → carga (entre pares)
     * Evaluador: OhmEvaluator
     * Restricciones: MAX_DISTANCE(32), MIN_DELTA(1e-6)
     */
    public static final PhysicalRelation ELECTRICAL_TRANSFER = PhysicalRelation.builder()
        .name("electrical_transfer")
        .relationType(RelationType.OHM)
        .participating(
            ElectricalProperties.CHARGE,
            ElectricalProperties.ELECTRICAL_CONDUCTIVITY)
        .constraint(RelationConstraint.maxDistance(32.0))
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(100)
        .build();

    // ── Relación 2: Disipación eléctrica ambiental ────────────────────────

    /**
     * La carga de cada objeto decae hacia el equilibrio (0).
     * La velocidad de disipación depende de la conductividad eléctrica.
     *
     * Fenómeno: AMBIENT_DISSIPATION
     * Propiedad que decae: CHARGE
     * Coeficiente: ELECTRICAL_CONDUCTIVITY
     * Evaluador: AmbientDissipationEvaluator
     */
    public static final PhysicalRelation ELECTRICAL_DISSIPATION = PhysicalRelation.builder()
        .name("electrical_dissipation")
        .relationType(RelationType.AMBIENT_DISSIPATION)
        .participating(
            ElectricalProperties.CHARGE,
            ElectricalProperties.ELECTRICAL_CONDUCTIVITY)
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(110)
        .build();

    // ── Relación 3: Corrección de exceso eléctrico ────────────────────────

    /**
     * Cuando la carga supera un umbral crítico (10 unidades), el exceso
     * se disipa para evitar divergencia numérica.
     *
     * Fenómeno: HOOKE (fuerza restauradora — corrección de exceso)
     * Evaluador: HookeEvaluator
     * Restricciones: THRESHOLD_ABOVE(CHARGE, 10)
     */
    public static final PhysicalRelation ELECTRICAL_EXCESS_CORRECTION = PhysicalRelation.builder()
        .name("electrical_excess_correction")
        .relationType(RelationType.HOOKE)
        .participating(
            ElectricalProperties.CHARGE,
            MechanicalProperties.PRESSURE,
            MechanicalProperties.COMPRESSIBILITY)
        .constraint(RelationConstraint.thresholdAbove(ElectricalProperties.CHARGE, 10.0))
        .priority(120)
        .build();

    // ── Relación 4: Efecto Joule ──────────────────────────────────────────

    /**
     * La corriente eléctrica que fluyó este frame (leída desde FrameMagnitudes.CURRENT,
     * escrita por OhmEvaluator) genera calor proporcional a I².
     *
     * Evaluada después de ELECTRICAL_TRANSFER (prio 100 → 105).
     * No existe ninguna propiedad puente en PhysicalState.
     *
     * Composición vía FrameState:
     *   OHM   (prio 100) → ΔCharge + escribe FrameMagnitudes.CURRENT
     *   JOULE (prio 105) → lee FrameMagnitudes.CURRENT → ΔTemperature
     *
     * Fenómeno: JOULE (Q = I² · R · t)
     * Evaluador: JouleEvaluator
     */
    public static final PhysicalRelation JOULE_HEATING = PhysicalRelation.builder()
        .name("joule_heating")
        .relationType(RelationType.JOULE)
        .participating(
            ThermalProperties.TEMPERATURE,
            ThermalProperties.HEAT_CAPACITY)
        .constraint(RelationConstraint.minDelta(1e-9))
        .priority(105)
        .build();

    // ── Colección completa ────────────────────────────────────────────────

    /**
     * Todas las relaciones del dominio eléctrico.
     *
     * @return array con las 4 relaciones eléctricas.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] {
            ELECTRICAL_TRANSFER,
            ELECTRICAL_DISSIPATION,
            ELECTRICAL_EXCESS_CORRECTION,
            JOULE_HEATING
        };
    }
}
