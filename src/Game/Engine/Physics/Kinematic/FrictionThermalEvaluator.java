package Game.Engine.Physics.Kinematic;

import Game.Engine.Physics.Contact.ContactState;
import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.RelationConstraint;
import Game.Engine.Physics.Core.RelationEvaluator;
import Game.Engine.Physics.Core.SimulationContext;
import Game.Engine.Physics.Material.MaterialState;
import Game.Engine.Physics.Thermal.ThermalProperties;
import java.util.List;

/**
 * Evaluador del fenómeno FRICTION_THERMAL.
 *
 * ── HRFC-030 — Integración entre Kinematic Physics y World Physics ────────
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Generación de calor por fricción cinética entre una entidad en movimiento
 * y la superficie de contacto.
 *
 * Fórmula simplificada (trabajo de la fuerza de rozamiento):
 *
 *   Q = μ × m × g × v × dt × efficiency
 *
 *   donde:
 *     μ            = coeficiente de fricción combinado (material + superficie)
 *     m × g        = fuerza normal aproximada (masa × gravedad efectiva)
 *     v            = módulo de la velocidad horizontal (KinematicState.velocity)
 *     dt           = deltaTime del KinematicState
 *     efficiency   = fracción de la energía de rozamiento convertida en calor
 *
 * El resultado se añade como delta de TEMPERATURE, dividido por la capacidad
 * calorífica del MaterialState. Si no hay MaterialState, el divisor es 1.0.
 *
 * ── HRFC-031: FUENTE DE DATOS ─────────────────────────────────────────────
 * Antes (HRFC-030):
 *   Leía SPEED, FRICTION_FACTOR, ON_GROUND desde PropertyDescriptors
 *   registrados en el PhysicalState. Requería registerKinematic() en el Builder.
 *
 * Ahora (HRFC-031):
 *   Lee directamente desde SimulationContext vía view.context():
 *     - velocidad     → context.currentKinematic().getVelocity()
 *     - onGround      → context.contact().isOnGround()
 *     - fricción      → combinación de contact().surfaceFriction
 *                       y material().frictionCoefficient
 *     - masa          → context.currentKinematic().getMass()
 *     - heatCapacity  → context.material().getHeatCapacity()
 *     - gravedad      → entity.gravity × environment.getGravityInfluenceMagnitude()
 *                       (aproximado como 9.8 × gravityInfluence para este evaluador)
 *
 * Ya no requiere PropertyDescriptors cinemáticos en el PhysicalState.
 * Las entidades no necesitan registerKinematic() para participar en este fenómeno.
 *
 * ── ACCESO ALTERNATIVO (HRFC-032) ────────────────────────────────────────
 * Los mismos datos son accesibles mediante el registro genérico:
 *   KinematicState kin  = context.state(KinematicState.class);
 *   MaterialState  mat  = context.state(MaterialState.class);
 *   ContactState   con  = context.state(ContactState.class);
 *   EnvironmentState env = context.state(EnvironmentState.class);
 * Los getters específicos (context.currentKinematic(), context.material()…)
 * son wrappers que delegan en el mismo mecanismo y siguen siendo válidos.
 *
 * ── CONDICIONES DE ACTIVACIÓN ────────────────────────────────────────────
 *   1. La entidad tiene SimulationContext activo (view.context() != null).
 *   2. La entidad tiene integración cinemática activa (context.hasKinematic()).
 *   3. El ContactState indica onGround = true.
 *   4. La velocidad supera el umbral mínimo (THRESHOLD_ABOVE de la relación).
 *   5. El coeficiente de fricción combinado > 0.
 *   6. El PhysicalState tiene TEMPERATURE registrada (puede calentarse).
 *
 * ── PROPIEDADES PRODUCIDAS ────────────────────────────────────────────────
 *   TEMPERATURE (ThermalProperties) — incremento de temperatura por fricción.
 *
 * ── COMPATIBILIDAD HRFC-030 ───────────────────────────────────────────────
 * Las entidades con solo PhysicsComponent (sin SimulationContextComponent)
 * no participan en este evaluador. El comportamiento para esas entidades
 * es el mismo que antes de HRFC-030 (sin calor por fricción).
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ No mueve entidades.
 *   ✗ No modifica velocidades.
 *   ✗ No conoce Physics2D.
 *   ✓ Lee desde SimulationContext vía view.context().
 *   ✓ Produce solo deltas de TEMPERATURE sobre PhysicalState via view.add().
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Sin estado mutable → thread-safe por diseño.
 */
public final class FrictionThermalEvaluator implements RelationEvaluator {

    /**
     * Velocidad mínima (u/s) para que el rozamiento cinético sea activo.
     * Por debajo de este valor se considera que el objeto está prácticamente
     * en reposo y el rozamiento estático no genera calor cinético.
     */
    private static final double DEFAULT_MIN_SPEED = 0.5;

    /**
     * Fracción de la energía de rozamiento convertida en calor cuando la
     * relación no declara MIN_DELTA.
     */
    private static final double DEFAULT_EFFICIENCY = 0.1;

    @Override
    public void evaluate(PhysicalRelation      relation,
                         List<EvaluationView>  views,
                         double                deltaTime) {

        // Leer parámetros opcionales de la relación
        RelationConstraint speedConstraint =
            relation.getConstraint(RelationConstraint.Type.THRESHOLD_ABOVE);
        double minSpeed = speedConstraint != null
            ? speedConstraint.getValue()
            : DEFAULT_MIN_SPEED;

        RelationConstraint efficiencyConstraint =
            relation.getConstraint(RelationConstraint.Type.MIN_DELTA);
        double efficiency = efficiencyConstraint != null
            ? efficiencyConstraint.getValue()
            : DEFAULT_EFFICIENCY;

        for (EvaluationView view : views) {

            // ── Condición 1: la entidad tiene SimulationContext ───────────
            SimulationContext context = view.context();
            if (context == null) continue;

            // ── Condición 2: la entidad tiene integración cinemática ──────
            if (!context.hasKinematic()) continue;
            KinematicState kin = context.currentKinematic();

            // ── Condición 3: la entidad está en contacto con el suelo ─────
            ContactState contact = context.contact();
            if (!contact.isOnGround()) continue;

            // ── Condición 4: velocidad suficiente para rozamiento cinético ─
            double speed = kin.getVelocity();
            if (speed < minSpeed) continue;

            // ── Condición 5: coeficiente de fricción combinado > 0 ────────
            // Fricción efectiva = media de superficie y material del objeto.
            // Si la superficie no tiene fricción, no hay rozamiento cinético.
            MaterialState material = context.material();
            double surfaceFriction = contact.getSurfaceFriction();
            double objectFriction  = material.getFrictionCoefficient();
            double combinedFriction = (surfaceFriction + objectFriction) * 0.5;
            if (combinedFriction <= 0.0) continue;

            // ── Condición 6: la entidad puede calentarse ──────────────────
            if (!view.has(ThermalProperties.TEMPERATURE)) continue;

            // ── Cálculo de calor generado ──────────────────────────────────
            //
            // Q = μ × N × v × efficiency
            // donde N = fuerza normal ≈ m × g_efectiva
            //
            // IMPORTANTE - HRFC-FASE2.5:
            // En el modelo corregido, la gravedad efectiva es:
            //   g_efectiva = entity.gravity × environment.gravityInfluence
            //
            // Como este evaluador no tiene acceso directo a entity.gravity
            // (está en Physics2D), usamos environment.gravityInfluenceMagnitude
            // como proxy asumiendo entity.gravity ≈ 9.8 para objetos normales.
            //
            // Para ambientes con gravityInfluence = 1.0 (normal), esto es correcto.
            // Para ambientes con gravityInfluence = 0.0 (vacío), g_efectiva = 0
            // y no hay fricción térmica, lo cual es físicamente correcto.
            //
            double mass               = kin.getMass();
            double gravityInfluence   = context.environment().getGravityInfluenceMagnitude();
            double assumedEntityGravity = 9.8; // gravedad típica de entidades
            double effectiveGravity   = assumedEntityGravity * gravityInfluence;

            double heatGenerated = combinedFriction
                * mass * effectiveGravity
                * speed * efficiency;

            // Dividir por capacidad calorífica del material
            double heatCapacity = Math.max(material.getHeatCapacity(), 0.01);
            double deltaTemperature = (heatGenerated * kin.getDeltaTime()) / heatCapacity;

            view.add(ThermalProperties.TEMPERATURE, deltaTemperature);
        }
    }
}
