package Game.Engine.Physics.Kinematic;

import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.RelationConstraint;
import Game.Engine.Physics.Core.RelationEvaluator;
import Game.Engine.Physics.Core.SimulationContext;
import Game.Engine.Physics.Core.StateSnapshot;
import Game.Engine.Physics.Material.MaterialState;
import Game.Engine.Physics.Mechanical.MechanicalProperties;
import Game.Engine.Physics.Thermal.ThermalProperties;
import java.util.List;

/**
 * Evaluador del fenómeno KINETIC_DISSIPATION.
 *
 * ── HRFC-030 — Integración entre Kinematic Physics y World Physics ────────
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Conversión de la pérdida de energía cinética (frenado brusco, impacto,
 * desaceleración extrema) en fenómenos físicos del mundo.
 *
 * Cuando una entidad pierde energía cinética, esa energía no desaparece:
 * se transforma en calor, deformación y presión local.
 *
 * Distribución de la disipación:
 *
 *   |ΔKE|  →  thermalFraction  × |ΔKE| / heatCapacity   → ΔTemperature
 *          →  pressureFraction × |ΔKE| × compressibility → ΔPressure
 *
 *   thermalFraction  default = 0.7  (la mayoría del impacto → calor)
 *   pressureFraction default = 0.2  (parte → sobrepresión local)
 *
 * ── HRFC-031: FUENTE DE DATOS ─────────────────────────────────────────────
 * Antes (HRFC-030):
 *   Leía DELTA_KINETIC_ENERGY y ACCELERATION desde PropertyDescriptors
 *   registrados en el PhysicalState mediante registerKinematic().
 *
 * Ahora (HRFC-031):
 *   El delta de energía cinética se calcula directamente desde StateSnapshot:
 *
 *     StateSnapshot<KinematicState> snap = context.kinematic();
 *     double deltaKE = snap.current().deltaKineticEnergyFrom(snap.previous());
 *
 *   Esto es:
 *     - Más preciso: usa los valores reales del DTO, no copias en PropertyDescriptors.
 *     - Sin acoplamiento: no requiere PropertyDescriptors cinemáticos en PhysicalState.
 *     - Genérico: funciona automáticamente para cualquier nueva magnitud de KinematicState.
 *
 * ── ACCESO ALTERNATIVO (HRFC-032) ────────────────────────────────────────
 * La instancia del frame actual es también accesible mediante el registro genérico:
 *   KinematicState kin = context.state(KinematicState.class); // = currentKinematic()
 *   MaterialState  mat = context.state(MaterialState.class);
 * Para acceso al snapshot completo (current + previous) seguir usando
 * context.kinematic() / context.hasKinematic(), que siguen siendo válidos.
 *
 * ── CONDICIONES DE ACTIVACIÓN ────────────────────────────────────────────
 *   1. La entidad tiene SimulationContext activo (view.context() != null).
 *   2. La entidad tiene integración cinemática activa (context.hasKinematic()).
 *   3. El snapshot tiene historial real (hasPrevious() = true).
 *   4. deltaKE < -umbral (pérdida de energía cinética significativa).
 *   5. El PhysicalState tiene TEMPERATURE o PRESSURE registradas.
 *
 * ── PROPIEDADES PRODUCIDAS ────────────────────────────────────────────────
 *   TEMPERATURE (ThermalProperties)    — calentamiento por disipación inelástica.
 *   PRESSURE    (MechanicalProperties) — incremento de presión local.
 *
 * ── COMPATIBILIDAD HRFC-030 ───────────────────────────────────────────────
 * Las entidades con solo PhysicsComponent (sin SimulationContextComponent)
 * no participan en este evaluador. Los PropertyDescriptors cinemáticos
 * (DELTA_KINETIC_ENERGY, ACCELERATION) ya no son necesarios ni se escriben.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ No mueve entidades.
 *   ✗ No modifica velocidades.
 *   ✗ No conoce Physics2D.
 *   ✓ Lee delta de KE desde StateSnapshot<KinematicState> vía view.context().
 *   ✓ Lee propiedades de material desde context.material().
 *   ✓ Produce solo deltas sobre PhysicalState via view.add().
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Sin estado mutable → thread-safe por diseño.
 */
public final class KineticDissipationEvaluator implements RelationEvaluator {

    /**
     * Umbral mínimo de pérdida de energía cinética (en valor absoluto) para
     * que la disipación sea considerada significativa. Evita ruido numérico.
     */
    private static final double DEFAULT_MIN_DISSIPATION = 0.1;

    /**
     * Fracción de |ΔKE| convertida en calor (sin declaración explícita).
     * 0.7 = 70% del impacto se convierte en temperatura.
     */
    private static final double DEFAULT_THERMAL_FRACTION = 0.7;

    /**
     * Fracción de |ΔKE| convertida en presión local (sin declaración explícita).
     * 0.2 = 20% del impacto genera sobrepresión local.
     */
    private static final double DEFAULT_PRESSURE_FRACTION = 0.2;

    @Override
    public void evaluate(PhysicalRelation      relation,
                         List<EvaluationView>  views,
                         double                deltaTime) {

        // Umbral de disipación mínima significativa
        RelationConstraint thresholdConstraint =
            relation.getConstraint(RelationConstraint.Type.THRESHOLD_BELOW);
        double minDissipation = thresholdConstraint != null
            ? Math.abs(thresholdConstraint.getValue())
            : DEFAULT_MIN_DISSIPATION;

        for (EvaluationView view : views) {

            // ── Condición 1: la entidad tiene SimulationContext ───────────
            SimulationContext context = view.context();
            if (context == null) continue;

            // ── Condición 2: la entidad tiene integración cinemática ──────
            if (!context.hasKinematic()) continue;
            StateSnapshot<KinematicState> snap = context.kinematic();

            // ── Condición 3: existe historial real para calcular el delta ──
            // En el primer frame (hasPrevious() = false), current == previous
            // y el delta sería 0, que no supera el umbral. Esta condición
            // es una optimización que evita el cálculo en el primer frame.
            if (!snap.hasPrevious()) continue;

            // ── Cálculo del delta de energía cinética ─────────────────────
            //
            // Usa el helper de KinematicState para calcular KE_actual - KE_anterior.
            // Negativo = el objeto perdió energía (frenado, impacto, fricción).
            double deltaKE = snap.current().deltaKineticEnergyFrom(snap.previous());

            // Solo procesar pérdidas de energía cinética (valores negativos)
            if (deltaKE >= 0.0) continue;

            double dissipated = Math.abs(deltaKE);

            // Filtrar disipaciones por debajo del umbral mínimo (ruido)
            if (dissipated < minDissipation) continue;

            // ── Propiedades de material para la distribución ──────────────
            MaterialState material = context.material();

            // ── Fracción térmica: calentamiento por disipación inelástica ──
            if (view.has(ThermalProperties.TEMPERATURE)) {
                double heatCapacity = Math.max(material.getHeatCapacity(), 0.01);
                double thermalEnergy = dissipated * DEFAULT_THERMAL_FRACTION;
                double deltaTemperature = thermalEnergy / heatCapacity;
                view.add(ThermalProperties.TEMPERATURE, deltaTemperature);
            }

            // ── Fracción mecánica: sobrepresión local ──────────────────────
            if (view.has(MechanicalProperties.PRESSURE)) {
                double compressibility = Math.max(material.getCompressibility(), 0.001);
                // Más compresible → el mismo impulso genera más ΔPressure
                double pressureEnergy = dissipated * DEFAULT_PRESSURE_FRACTION;
                double deltaPressure  = pressureEnergy * compressibility;
                view.add(MechanicalProperties.PRESSURE, deltaPressure);
            }
        }
    }
}
