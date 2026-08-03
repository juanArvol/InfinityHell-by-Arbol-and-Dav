package Game.Engine.World.Physics.Kinematic;

import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.RelationConstraint;
import Game.Engine.World.Physics.Core.RelationType;
import Game.Engine.World.Physics.Mechanical.MechanicalProperties;
import Game.Engine.World.Physics.Thermal.ThermalProperties;

/**
 * Catálogo de relaciones físicas emergentes del movimiento.
 *
 * ── HRFC-030 — Integración entre Kinematic Physics y World Physics ────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * Este catálogo declara las PhysicalRelation que conectan las magnitudes
 * producidas por Kinematic Physics (via KinematicStateProperties) con los
 * fenómenos del World Simulation.
 *
 * Son las primeras relaciones emergentes del movimiento. No implementan
 * ningún algoritmo. Solo describen fenómenos mediante declaraciones.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Kinematic Physics no conoce estas relaciones.
 * World Physics no mueve entidades.
 * Las relaciones son el nexo puramente declarativo entre ambos sistemas.
 *
 * ── RELACIONES INCLUIDAS ─────────────────────────────────────────────────
 *
 *   FRICTION_HEAT
 *     Movimiento sobre superficie → Calor por rozamiento
 *     SPEED × FRICTION_FACTOR × ON_GROUND → ΔTemperature
 *     Evaluador: FrictionThermalEvaluator
 *
 *   KINETIC_DISSIPATION
 *     Pérdida de energía cinética → Calor + Presión
 *     DELTA_KINETIC_ENERGY (negativo) → ΔTemperature + ΔPressure
 *     Evaluador: KineticDissipationEvaluator
 *
 * ── FENÓMENOS FUTUROS ─────────────────────────────────────────────────────
 * Partiendo de las mismas propiedades de KinematicStateProperties, se pueden
 * declarar futuras relaciones sin cambiar ningún sistema existente:
 *
 *   SPEED_SOUND_GENERATION  → sonido por velocidad
 *   SPEED_AIR_IONIZATION    → ionización del aire a velocidades extremas
 *   IMPACT_DEFORMATION      → deformación permanente por impulso
 *   FLUID_DYNAMIC_PRESSURE  → presión de Bernoulli en fluidos
 *   CAVITATION              → cavitación en fluidos a velocidades extremas
 *   VIBRATION               → vibraciones por impacto o rozamiento
 *
 * ── PRIORIDADES ───────────────────────────────────────────────────────────
 * Estas relaciones se evalúan con prioridad 10 y 11, DESPUÉS de las relaciones
 * gravitacionales (prioridad 1-3) y ANTES de las relaciones térmicas
 * y mecánicas (prioridad 100+). Esto garantiza que el calor y la presión
 * generados por el movimiento sean visibles para los evaluadores térmicos
 * en el mismo frame.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Para añadir una nueva relación emergente del movimiento:
 *   1. Añadir una constante PhysicalRelation aquí.
 *   2. Añadir la constante al array de all().
 *   3. Implementar su RelationEvaluator y registrarlo en EvaluatorRegistry.
 *
 *   Ningún sistema existente cambia.
 */
public final class KinematicDerivedRelations {

    private KinematicDerivedRelations() {}

    // ── Cinemática → Térmica ──────────────────────────────────────────────

    /**
     * Calor generado por rozamiento cinético.
     *
     * Fenómeno: El movimiento de una entidad sobre una superficie con
     * coeficiente de fricción > 0 genera calor proporcional a la velocidad,
     * la masa y el coeficiente de rozamiento.
     *
     * Activa cuando:
     *   - ON_GROUND = 1.0  (contacto con superficie)
     *   - SPEED > 0.5 u/s  (velocidad mínima; valor codificado en THRESHOLD_ABOVE)
     *   - FRICTION_FACTOR > 0  (superficie con rozamiento)
     *
     * Propiedades consumidas: SPEED, FRICTION_FACTOR, ON_GROUND
     * Propiedades producidas: TEMPERATURE
     *
     * La eficiencia de conversión (fracción de energía de rozamiento que
     * se convierte en calor) está codificada en MIN_DELTA = 0.1 (10%).
     * Sobreescribir con MIN_DELTA diferente para superficies especiales.
     *
     * Prioridad 10: después de gravedad (1), antes de conducción térmica (100).
     */
    public static final PhysicalRelation FRICTION_HEAT = PhysicalRelation.builder()
        .name("friction_heat")
        .relationType(RelationType.FRICTION_THERMAL)
        .participating(
            KinematicStateProperties.SPEED,
            KinematicStateProperties.FRICTION_FACTOR,
            KinematicStateProperties.ON_GROUND,
            ThermalProperties.TEMPERATURE,
            ThermalProperties.HEAT_CAPACITY
        )
        .constraint(RelationConstraint.thresholdAbove(KinematicStateProperties.SPEED, 0.5))
        .constraint(RelationConstraint.propertyPresent(ThermalProperties.TEMPERATURE))
        .constraint(RelationConstraint.minDelta(0.1))  // efficiency = 10%
        .priority(10)
        .build();

    // ── Cinemática → Mecánica + Térmica ───────────────────────────────────

    /**
     * Disipación de energía cinética en calor y presión.
     *
     * Fenómeno: Cuando una entidad pierde energía cinética significativa
     * (frenado brusco, impacto, colisión), esa energía se convierte en
     * calentamiento del objeto y aumento de presión local.
     *
     * Activa cuando:
     *   - DELTA_KINETIC_ENERGY < -0.1  (pérdida mínima de KE significativa)
     *
     * Propiedades consumidas: DELTA_KINETIC_ENERGY
     * Propiedades producidas: TEMPERATURE, PRESSURE
     *
     * La distribución predeterminada es 70% → calor, 20% → presión.
     * El umbral de activación (valor absoluto del ΔKE mínimo) está codificado
     * en THRESHOLD_BELOW = -0.1 del evaluador (ajustable por Assembler).
     *
     * Prioridad 11: evaluado inmediatamente después de FRICTION_HEAT,
     * antes de que los evaluadores térmicos/mecánicos propaguen el resultado.
     */
    public static final PhysicalRelation KINETIC_ENERGY_DISSIPATION = PhysicalRelation.builder()
        .name("kinetic_energy_dissipation")
        .relationType(RelationType.KINETIC_DISSIPATION)
        .participating(
            KinematicStateProperties.DELTA_KINETIC_ENERGY,
            KinematicStateProperties.ACCELERATION,
            ThermalProperties.TEMPERATURE,
            ThermalProperties.HEAT_CAPACITY,
            MechanicalProperties.PRESSURE,
            MechanicalProperties.COMPRESSIBILITY
        )
        .constraint(RelationConstraint.thresholdBelow(
            KinematicStateProperties.DELTA_KINETIC_ENERGY, -0.1))
        .priority(11)
        .build();

    // ── Acceso al catálogo ────────────────────────────────────────────────

    /**
     * Todas las relaciones emergentes del movimiento.
     *
     * Usar para registrar en WorldSimulation:
     *   WorldSimulation.builder()
     *       .registerAll(new RelationRegistry()
     *           .registerAll(KinematicDerivedRelations.all()))
     *       .build();
     *
     * @return array con todas las relaciones cinemáticas derivadas.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] {
            FRICTION_HEAT,
            KINETIC_ENERGY_DISSIPATION
        };
    }
}
