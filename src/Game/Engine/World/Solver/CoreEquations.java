package Game.Engine.World.Solver;

import Game.Engine.World.Physics.CoreDomains;
import Game.Engine.World.Physics.PhysicsConstraint;
import Game.Engine.World.Physics.PhysicsEquation;

/**
 * Catálogo de ecuaciones y restricciones físicas fundamentales.
 *
 * ── HRFC-017 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * CoreEquations es donde reside el conocimiento físico del Engine.
 * No en el Solver. No en las clases de fenómenos.
 *
 * Cada método de esta clase retorna una ecuación o restricción declarativa
 * lista para ser registrada en un PhysicsSolver. El Solver nunca conoce los
 * nombres de estos métodos — simplemente recorre lo que se le ha registrado.
 *
 * ── EQUIVALENCIA CON EL MODELO ANTERIOR ──────────────────────────────────
 *
 *   ANTES (clases de fenómeno)          AHORA (datos declarativos)
 *   ─────────────────────────────────   ──────────────────────────────────
 *   ThermalExpansionRelation(0.05)   →  CoreEquations.thermalExpansion(0.05)
 *   ThermalEnergyTransferRelation    →  CoreEquations.thermalDissipation(500,0.1)
 *   ChargeTransferRelation           →  CoreEquations.chargeDissipation(10,0.08)
 *   FluidTransferRelation            →  CoreEquations.fluidSaturationRelease(0.6,0.05)
 *   TransferStrategies.thermal()     →  CoreEquations.thermalTransfer(...)
 *   TransferStrategies.electrical()  →  CoreEquations.electricalTransfer(...)
 *   TransferStrategies.fluid()       →  CoreEquations.fluidTransfer(...)
 *
 * La diferencia arquitectónica es fundamental:
 *   - Las clases anteriores eran tipos Java con nombres de fenómeno.
 *     El Engine las conocía por nombre. Añadir una nueva implicaba
 *     añadir una nueva clase y enseñar al Engine a usarla.
 *   - Los métodos de esta clase retornan datos anónimos. El Solver
 *     no sabe qué producen. Solo sabe que son PhysicsEquation o
 *     PairEquation o PhysicsConstraint. El algoritmo es uniforme.
 *
 * ── USO EN WorldSimulation ────────────────────────────────────────────────
 *
 *   PhysicsSolver solver = new PhysicsSolver();
 *   solver.addEquation(CoreEquations.thermalExpansion(0.05));
 *   solver.addConstraint(CoreEquations.thermalDissipation(500.0, 0.1));
 *   solver.addConstraint(CoreEquations.chargeDissipation(10.0, 0.08));
 *   solver.addConstraint(CoreEquations.fluidSaturationRelease(0.6, 0.05));
 *   solver.addPairEquation(CoreEquations.thermalTransfer());
 *   solver.addPairEquation(CoreEquations.electricalTransfer());
 *   solver.addPairEquation(CoreEquations.fluidTransfer());
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir una nueva ley física no requiere modificar PhysicsSolver.
 * Únicamente añadir un nuevo método aquí (o en un catálogo propio del Gameplay)
 * y registrarlo en el PhysicsSolver durante la configuración del mundo.
 */
public final class CoreEquations {

    private CoreEquations() {}

    // ═══════════════════════════════════════════════════════════════════════
    // Ecuaciones intra-objeto (PhysicsEquation)
    // Corresponden al antiguo PhysicalInteractionRegistry
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Expansión volumétrica: temperatura → presión.
     *
     * Un cambio de temperatura en un material rígido produce un cambio de
     * presión proporcional a la temperatura y a (1 − compresibilidad).
     *
     *   ΔP = temperatura × (1 − compresibilidad) × expansionFactor
     *
     * Material compresible (gas ≈ 1.0) → poca presión.
     * Material rígido (metal ≈ 0.05) → mucha presión.
     *
     * Equivalente anterior: {@code ThermalExpansionRelation(expansionFactor)}
     *
     * @param expansionFactor factor de escala. Valor típico: 0.01–0.1.
     * @return ecuación declarativa lista para registrar en el Solver.
     */
    public static PhysicsEquation<CoreDomains.Thermal, CoreDomains.Pressure>
    thermalExpansion(double expansionFactor) {
        return PhysicsEquation.of(
            CoreDomains.Thermal.class,
            CoreDomains.Pressure.class,
            ctx -> ctx.hasSource()
                && ctx.hasTarget()
                && Math.abs(ctx.source()) > 1e-6,
            ctx -> (1.0 - ctx.getMaterial().getCompressibility()) * expansionFactor,
            5   // prioridad baja: se evalúa antes que otras que lean presión
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Restricciones (PhysicsConstraint)
    // Corresponden al antiguo PhysicalInteractionRegistry — relaciones de umbral
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Disipación de temperatura acumulada por encima de un umbral de energía.
     *
     * Si la energía acumulada (|temperatura| × heatCapacity) supera el umbral,
     * se disipa una fracción de la temperatura en cada frame.
     *
     * Equivalente anterior: {@code ThermalEnergyTransferRelation(energyThreshold, dissipationRate)}
     *
     * @param energyThreshold umbral de energía acumulada para activar la disipación.
     * @param dissipationRate fracción de temperatura disipada por frame [0, 1].
     * @return restricción declarativa lista para registrar en el Solver.
     */
    public static PhysicsConstraint<CoreDomains.Thermal>
    thermalDissipation(double energyThreshold, double dissipationRate) {
        double rate = Math.max(0.0, Math.min(1.0, dissipationRate));
        return PhysicsConstraint.of(
            CoreDomains.Thermal.class,
            (q, mat) -> {
                double temp = q.getValue();
                if (Math.abs(temp) < 1e-6) return false;
                return Math.abs(temp) * mat.getHeatCapacity() >= energyThreshold;
            },
            (q, mat) -> q.add(-q.getValue() * rate),
            10
        );
    }

    /**
     * Disipación de carga eléctrica acumulada por encima de un umbral efectivo.
     *
     * La carga efectiva = |carga| × conductividad. Conductores disipan antes.
     * Aislantes acumulan más carga antes de disipar.
     *
     * Equivalente anterior: {@code ChargeTransferRelation(chargeThreshold, dissipationRate)}
     *
     * @param chargeThreshold umbral de carga efectiva para activar la disipación.
     * @param dissipationRate fracción de carga disipada por frame [0, 1].
     * @return restricción declarativa lista para registrar en el Solver.
     */
    public static PhysicsConstraint<CoreDomains.Electrical>
    chargeDissipation(double chargeThreshold, double dissipationRate) {
        double rate = Math.max(0.0, Math.min(1.0, dissipationRate));
        return PhysicsConstraint.of(
            CoreDomains.Electrical.class,
            (q, mat) -> {
                double charge = q.getValue();
                if (Math.abs(charge) < 1e-9) return false;
                return Math.abs(charge) * mat.getElectricalConductivity() >= chargeThreshold;
            },
            (q, mat) -> q.add(-q.getValue() * rate),
            20
        );
    }

    /**
     * Liberación de fluido en saturación.
     *
     * Si la saturación efectiva (humedad / absorción del material) supera el
     * umbral, se libera una fracción de la humedad.
     *
     * Material absorbente (madera 0.6) retiene más.
     * Material impermeable (metal 0.02) se satura con poca humedad.
     *
     * Equivalente anterior: {@code FluidTransferRelation(saturationThreshold, releaseRate)}
     *
     * @param saturationThreshold umbral de saturación efectiva [0.5, 1.0].
     * @param releaseRate         fracción de humedad liberada por frame [0, 1].
     * @return restricción declarativa lista para registrar en el Solver.
     */
    public static PhysicsConstraint<CoreDomains.Fluid>
    fluidSaturationRelease(double saturationThreshold, double releaseRate) {
        double rate = Math.max(0.0, Math.min(1.0, releaseRate));
        return PhysicsConstraint.of(
            CoreDomains.Fluid.class,
            (q, mat) -> {
                double humidity = q.getValue();
                if (humidity < 1e-6) return false;
                double absorption = Math.max(0.01, mat.getHumidityAbsorption());
                return (humidity / absorption) >= saturationThreshold;
            },
            (q, mat) -> q.add(-q.getValue() * rate),
            30
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Ecuaciones de par (PairEquation)
    // Corresponden al antiguo TransferSimulation + TransferStrategies
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Transferencia de temperatura entre pares de objetos adyacentes.
     *
     * Conductividad efectiva = mínima de los dos materiales (el par intercambia
     * a la velocidad del material menos conductor).
     * Escala de transferencia = 1/heatCapacity (inercia térmica).
     *
     * Equivalente anterior: {@code TransferSimulation.of(TransferStrategies.thermal())}
     *
     * @return ecuación de par declarativa con configuración por defecto.
     */
    public static PairEquation<CoreDomains.Thermal> thermalTransfer() {
        return thermalTransfer(32.0, 0.05);
    }

    /**
     * Transferencia de temperatura con radio y escala temporal configurables.
     *
     * @param contactRadius radio de contacto en unidades del mundo.
     * @param timeScale     escala temporal de la transferencia.
     * @return ecuación de par declarativa.
     */
    public static PairEquation<CoreDomains.Thermal> thermalTransfer(
            double contactRadius, double timeScale) {
        return PairEquation.builder(
                CoreDomains.Thermal.class,
                (matA, matB, qA, qB) ->
                    Math.min(matA.getThermalConductivity(), matB.getThermalConductivity()))
            .transferScale((mat, q) -> 1.0 / Math.max(0.01, mat.getHeatCapacity()))
            .contactRadius(contactRadius)
            .timeScale(timeScale)
            .build();
    }

    /**
     * Transferencia de carga eléctrica entre pares de objetos adyacentes.
     *
     * Conductividad efectiva = producto de conductividades individuales
     * (ambos materiales deben ser conductores para que haya flujo).
     * Escala de transferencia = 1.0 (sin inercia eléctrica).
     *
     * Equivalente anterior: {@code TransferSimulation.of(TransferStrategies.electrical())}
     *
     * @return ecuación de par declarativa con configuración por defecto.
     */
    public static PairEquation<CoreDomains.Electrical> electricalTransfer() {
        return electricalTransfer(32.0, 0.05);
    }

    /**
     * Transferencia de carga eléctrica con radio y escala temporal configurables.
     *
     * @param contactRadius radio de contacto en unidades del mundo.
     * @param timeScale     escala temporal de la transferencia.
     * @return ecuación de par declarativa.
     */
    public static PairEquation<CoreDomains.Electrical> electricalTransfer(
            double contactRadius, double timeScale) {
        return PairEquation.builder(
                CoreDomains.Electrical.class,
                (matA, matB, qA, qB) -> {
                    double cA = matA.getElectricalConductivity();
                    double cB = matB.getElectricalConductivity();
                    return (cA <= 0 || cB <= 0) ? 0.0 : cA * cB;
                })
            .contactRadius(contactRadius)
            .timeScale(timeScale)
            .epsilon(1e-9)
            .build();
    }

    /**
     * Difusión de humedad entre pares de objetos adyacentes.
     *
     * Conductividad efectiva = mínima de las absorciones (el par difunde a la
     * velocidad del material menos absorbente).
     * Escala de transferencia = 1.0.
     *
     * Equivalente anterior: {@code TransferSimulation.of(TransferStrategies.fluid())}
     *
     * @return ecuación de par declarativa con configuración por defecto.
     */
    public static PairEquation<CoreDomains.Fluid> fluidTransfer() {
        return fluidTransfer(32.0, 0.05);
    }

    /**
     * Difusión de humedad con radio y escala temporal configurables.
     *
     * @param contactRadius radio de contacto en unidades del mundo.
     * @param timeScale     escala temporal de la difusión.
     * @return ecuación de par declarativa.
     */
    public static PairEquation<CoreDomains.Fluid> fluidTransfer(
            double contactRadius, double timeScale) {
        return PairEquation.builder(
                CoreDomains.Fluid.class,
                (matA, matB, qA, qB) -> {
                    double aA = matA.getHumidityAbsorption();
                    double aB = matB.getHumidityAbsorption();
                    return (aA <= 0 || aB <= 0) ? 0.0 : Math.min(aA, aB);
                })
            .contactRadius(contactRadius)
            .timeScale(timeScale)
            .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Restricciones de disipación ambiental (PhysicsConstraint)
    // Corresponden al antiguo Fase 2 de TransferSimulation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Disipación ambiental de temperatura.
     *
     * En ausencia de influencias externas, la temperatura converge hacia
     * el valor ambiente a una tasa determinada por la difusividad del material.
     *
     * Equivalente anterior: Fase 2 de {@code TransferSimulation} con
     * {@code ambientDissipationRate = mat.getThermalDiffusivity()}.
     *
     * @param ambientTemperature temperatura ambiente hacia la que converge.
     * @param timeScale          escala temporal de la convergencia.
     * @return restricción declarativa.
     */
    public static PhysicsConstraint<CoreDomains.Thermal>
    thermalAmbientDissipation(double ambientTemperature, double timeScale) {
        return PhysicsConstraint.of(
            CoreDomains.Thermal.class,
            (q, mat) -> !q.isZero() && mat.getThermalDiffusivity() > 0,
            (q, mat) -> q.converge(ambientTemperature, mat.getThermalDiffusivity() * timeScale),
            50
        );
    }

    /**
     * Disipación ambiental de carga eléctrica.
     *
     * Los conductores disipan carga hacia el equilibrio neutro (0).
     *
     * @param dissipationScale factor multiplicativo sobre la conductividad.
     * @return restricción declarativa.
     */
    public static PhysicsConstraint<CoreDomains.Electrical>
    electricalAmbientDissipation(double dissipationScale) {
        return PhysicsConstraint.of(
            CoreDomains.Electrical.class,
            (q, mat) -> !q.isZero() && mat.getElectricalConductivity() > 0,
            (q, mat) -> q.converge(0.0, mat.getElectricalConductivity() * dissipationScale),
            50
        );
    }

    /**
     * Disipación ambiental de humedad (evaporación/absorción ambiental).
     *
     * La humedad converge hacia el valor ambiente a una tasa determinada
     * por la absorción del material y la tasa configurada.
     *
     * @param ambientHumidity humedad ambiente [0, 1].
     * @param ambientRate     velocidad de convergencia base.
     * @return restricción declarativa.
     */
    public static PhysicsConstraint<CoreDomains.Fluid>
    fluidAmbientDissipation(double ambientHumidity, double ambientRate) {
        double clampedAmbient = Math.max(0.0, Math.min(1.0, ambientHumidity));
        double clampedRate    = Math.max(0.0, ambientRate);
        return PhysicsConstraint.of(
            CoreDomains.Fluid.class,
            (q, mat) -> !q.isZero() && mat.getHumidityAbsorption() > 0,
            (q, mat) -> q.converge(clampedAmbient,
                mat.getHumidityAbsorption() * clampedRate),
            50
        );
    }
}
