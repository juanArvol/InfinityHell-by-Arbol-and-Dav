package Game.Engine.World.Solver;

import Game.Engine.World.Components.MaterialComponent;
import Game.Engine.World.Physics.PhysicalDomain;
import Game.Engine.World.Physics.PhysicalQuantity;

/**
 * Descripción declarativa de una ley de transferencia entre pares de objetos.
 *
 * ── HRFC-017 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * PairEquation es el reemplazo declarativo de TransferStrategy + TransferSimulation.
 *
 * Donde antes TransferStrategies.thermal(), TransferStrategies.electrical() y
 * TransferStrategies.fluid() eran tres objetos distintos con lógica específica
 * por dominio, PairEquation es un único descriptor genérico que el Solver
 * recorre de forma uniforme.
 *
 * El Solver aplica para cada par (A, B) dentro del radio de contacto:
 *
 *   conductivity = pairEq.conductivity(matA, matB, qA, qB)
 *   delta        = (qA - qB) × conductivity × timeScale
 *   qA.add(-delta × scaleA)
 *   qB.add(+delta × scaleB)
 *
 * No hay lógica específica para temperatura, carga ni humedad.
 * El comportamiento depende exclusivamente de los datos inyectados.
 *
 * ── EQUIVALENCIA CON EL MODELO ANTERIOR ──────────────────────────────────
 *
 *   ANTES                                           AHORA
 *   TransferSimulation(TransferStrategies.thermal()) →
 *       PairEquation.of(CoreDomains.Thermal.class,
 *           (matA, matB, qA, qB) → Math.min(matA.getThermalConductivity(),
 *                                            matB.getThermalConductivity()),
 *           (mat, q) → 1.0 / Math.max(0.01, mat.getHeatCapacity()))
 *
 *   TransferSimulation(TransferStrategies.electrical()) →
 *       PairEquation.of(CoreDomains.Electrical.class,
 *           (matA, matB, qA, qB) → matA.getElectricalConductivity()
 *                                 * matB.getElectricalConductivity(),
 *           (mat, q) → 1.0)
 *
 *   TransferSimulation(TransferStrategies.fluid()) →
 *       PairEquation.of(CoreDomains.Fluid.class,
 *           (matA, matB, qA, qB) → Math.min(matA.getHumidityAbsorption(),
 *                                            matB.getHumidityAbsorption()),
 *           (mat, q) → 1.0)
 *
 * La diferencia semántica es absoluta: las clases anteriores se llamaban a
 * sí mismas "simulaciones térmicas/eléctricas/fluídicas". PairEquation no
 * tiene nombre de fenómeno. Es simplemente una ley de transferencia.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir una nueva transferencia entre pares = instanciar PairEquation.
 * El PhysicsSolver no cambia.
 *
 * @param <D> dominio de la propiedad que esta ecuación transfiere.
 */
public final class PairEquation<D extends PhysicalDomain> {

    private final Class<D>          domain;
    private final ConductivityFn<D> conductivityFn;
    private final TransferScaleFn<D> transferScaleFn;
    private final double            contactRadius;
    private final double            timeScale;
    private final double            epsilon;
    private final int               priority;

    // ── Interfaces funcionales ─────────────────────────────────────────────

    /**
     * Función de conductividad efectiva entre dos materiales.
     * Determina qué fracción del diferencial se transfiere por frame.
     * Retornar ≤ 0 para indicar que el par no intercambia en este dominio.
     *
     * @param <D> dominio de la propiedad.
     */
    @FunctionalInterface
    public interface ConductivityFn<D extends PhysicalDomain> {
        /**
         * @param matA    material del objeto A.
         * @param matB    material del objeto B.
         * @param quantityA magnitud actual del objeto A.
         * @param quantityB magnitud actual del objeto B.
         * @return conductividad efectiva ≥ 0. 0 = sin transferencia.
         */
        double compute(MaterialComponent matA, MaterialComponent matB,
                       PhysicalQuantity<D> quantityA, PhysicalQuantity<D> quantityB);
    }

    /**
     * Factor de escala de la transferencia para un objeto receptor.
     * Permite modelar inercia (p.ej. capacidad calorífica).
     * La mayoría de propiedades retornan 1.0.
     *
     * @param <D> dominio de la propiedad.
     */
    @FunctionalInterface
    public interface TransferScaleFn<D extends PhysicalDomain> {
        /**
         * @param mat      material del objeto receptor.
         * @param quantity magnitud actual del receptor.
         * @return factor de escala (típicamente 1.0 o 1/heatCapacity).
         */
        double compute(MaterialComponent mat, PhysicalQuantity<D> quantity);
    }

    // ── Constructor privado — usar factories ──────────────────────────────

    private PairEquation(Class<D>           domain,
                          ConductivityFn<D>  conductivityFn,
                          TransferScaleFn<D> transferScaleFn,
                          double             contactRadius,
                          double             timeScale,
                          double             epsilon,
                          int                priority) {
        if (domain         == null) throw new IllegalArgumentException("domain no puede ser null");
        if (conductivityFn == null) throw new IllegalArgumentException("conductivityFn no puede ser null");
        this.domain           = domain;
        this.conductivityFn   = conductivityFn;
        this.transferScaleFn  = transferScaleFn != null ? transferScaleFn : (m, q) -> 1.0;
        this.contactRadius    = Math.max(0.0, contactRadius);
        this.timeScale        = timeScale;
        this.epsilon          = epsilon;
        this.priority         = priority;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Crea una ecuación de par con configuración completa.
     *
     * @param domain          dominio de la propiedad a transferir.
     * @param conductivityFn  función de conductividad efectiva.
     * @param transferScaleFn factor de escala de transferencia (null = 1.0).
     * @param contactRadius   radio de contacto en unidades del mundo.
     * @param timeScale       escala temporal de la transferencia.
     * @param <D>             dominio físico.
     * @return ecuación de par configurada.
     */
    public static <D extends PhysicalDomain>
    PairEquation<D> of(Class<D>           domain,
                        ConductivityFn<D>  conductivityFn,
                        TransferScaleFn<D> transferScaleFn,
                        double             contactRadius,
                        double             timeScale) {
        return new PairEquation<>(domain, conductivityFn, transferScaleFn,
            contactRadius, timeScale, 1e-6, 100);
    }

    /**
     * Crea una ecuación de par con valores por defecto (radio=32, escala=0.05, escala de transfer=1.0).
     *
     * @param domain         dominio de la propiedad a transferir.
     * @param conductivityFn función de conductividad efectiva.
     * @param <D>            dominio físico.
     * @return ecuación de par con valores por defecto.
     */
    public static <D extends PhysicalDomain>
    PairEquation<D> of(Class<D> domain, ConductivityFn<D> conductivityFn) {
        return new PairEquation<>(domain, conductivityFn, null, 32.0, 0.05, 1e-6, 100);
    }

    /**
     * Punto de entrada del Builder para configuración explícita.
     *
     * @param domain         dominio de la propiedad a transferir.
     * @param conductivityFn función de conductividad efectiva.
     * @param <D>            dominio físico.
     * @return nuevo Builder.
     */
    public static <D extends PhysicalDomain>
    Builder<D> builder(Class<D> domain, ConductivityFn<D> conductivityFn) {
        return new Builder<>(domain, conductivityFn);
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Dominio de la propiedad que esta ecuación transfiere. */
    public Class<D> getDomain() { return domain; }

    /** Radio de contacto en unidades del mundo. */
    public double getContactRadius() { return contactRadius; }

    /** Escala temporal de la transferencia. */
    public double getTimeScale() { return timeScale; }

    /** Epsilon de convergencia (delta mínimo). */
    public double getEpsilon() { return epsilon; }

    /** Prioridad de evaluación. Menor = antes. */
    public int getPriority() { return priority; }

    // ── Evaluación ────────────────────────────────────────────────────────

    /**
     * Calcula la conductividad efectiva entre los materiales y cantidades del par.
     *
     * @param matA      material del objeto A.
     * @param matB      material del objeto B.
     * @param quantityA magnitud actual del objeto A.
     * @param quantityB magnitud actual del objeto B.
     * @return conductividad efectiva ≥ 0.
     */
    public double conductivity(MaterialComponent matA, MaterialComponent matB,
                                PhysicalQuantity<D> quantityA, PhysicalQuantity<D> quantityB) {
        return conductivityFn.compute(matA, matB, quantityA, quantityB);
    }

    /**
     * Factor de escala de la transferencia para el material y cantidad dados.
     *
     * @param mat      material del objeto receptor.
     * @param quantity magnitud del receptor.
     * @return factor de escala.
     */
    public double transferScale(MaterialComponent mat, PhysicalQuantity<D> quantity) {
        return transferScaleFn.compute(mat, quantity);
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "PairEquation[" + domain.getSimpleName()
            + " radius=" + contactRadius
            + " timeScale=" + timeScale + "]";
    }

    // ── Builder ───────────────────────────────────────────────────────────

    /**
     * Builder de PairEquation.
     *
     * @param <D> dominio físico.
     */
    public static final class Builder<D extends PhysicalDomain> {

        private final Class<D>          domain;
        private final ConductivityFn<D> conductivityFn;

        private TransferScaleFn<D> transferScaleFn = null;
        private double             contactRadius   = 32.0;
        private double             timeScale       = 0.05;
        private double             epsilon         = 1e-6;
        private int                priority        = 100;

        private Builder(Class<D> domain, ConductivityFn<D> conductivityFn) {
            if (domain == null) throw new IllegalArgumentException("domain no puede ser null");
            if (conductivityFn == null) throw new IllegalArgumentException("conductivityFn no puede ser null");
            this.domain         = domain;
            this.conductivityFn = conductivityFn;
        }

        /**
         * Factor de escala de transferencia (opcional; default=1.0).
         * Usar para modelar inercia: p.ej. 1/heatCapacity para temperatura.
         */
        public Builder<D> transferScale(TransferScaleFn<D> fn) {
            this.transferScaleFn = fn;
            return this;
        }

        /** Radio de contacto en unidades del mundo (default=32). */
        public Builder<D> contactRadius(double r) { this.contactRadius = r; return this; }

        /** Escala temporal (default=0.05). */
        public Builder<D> timeScale(double s) { this.timeScale = s; return this; }

        /** Epsilon de convergencia (default=1e-6). */
        public Builder<D> epsilon(double e) { this.epsilon = e; return this; }

        /** Prioridad de evaluación (default=100). */
        public Builder<D> priority(int p) { this.priority = p; return this; }

        /** Construye la PairEquation. */
        public PairEquation<D> build() {
            return new PairEquation<>(domain, conductivityFn, transferScaleFn,
                contactRadius, timeScale, epsilon, priority);
        }
    }
}
