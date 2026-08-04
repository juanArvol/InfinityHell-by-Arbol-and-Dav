package Game.Engine.Physics.Material;

/**
 * Estado descriptivo de las propiedades intrínsecas del material de un objeto.
 *
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * MaterialState describe QUÉ ES el objeto desde el punto de vista de su
 * composición material. Agrupa todas las propiedades que son inherentes al
 * material, estables durante la simulación y no describen lo que le está
 * ocurriendo al objeto.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * MaterialState solo describe. No calcula. No transforma. No produce efectos.
 *
 * Las transformaciones, cálculos y fenómenos derivados del material pertenecen
 * exclusivamente a los RelationEvaluators que consumen este estado.
 *
 *   MaterialState  →  describe (qué es el objeto)
 *   Relation       →  interpreta (qué le ocurre dadas sus propiedades)
 *
 * ── DOMINIO ───────────────────────────────────────────────────────────────
 * Propiedades incluidas:
 *
 *   Térmicas:
 *     thermalConductivity     coeficiente de conducción de calor [0, 1]
 *     heatCapacity            inercia térmica (resistencia al cambio de T)
 *     thermalDiffusivity      velocidad de disipación hacia el ambiente [0, 1]
 *     meltingPoint            temperatura de fusión (sólido → líquido)
 *     boilingPoint            temperatura de ebullición (líquido → gas)
 *
 *   Eléctricas:
 *     electricalConductivity  coeficiente de conducción eléctrica [0, 1]
 *
 *   Mecánicas:
 *     compressibility         facilidad de cambio de volumen bajo presión [0, 1]
 *     elasticity              fracción de energía conservada en deformaciones [0, 1]
 *     hardness                resistencia a deformación superficial [0, 1]
 *     density                 masa por unidad de volumen (> 0)
 *
 *   Fricción:
 *     frictionCoefficient     coeficiente de rozamiento cinético [0, +∞)
 *     rollingResistance       resistencia al rodamiento [0, 1]
 *
 *   Fluídicas:
 *     viscosity               resistencia al flujo (0 = sólido, > 0 = fluido)
 *     humidityAbsorption      capacidad de absorción de humedad [0, 1]
 *
 *   Ópticas / energéticas:
 *     absorptivity            fracción de radiación incidente absorbida [0, 1]
 *     emissivity              eficiencia de emisión de radiación térmica [0, 1]
 *     combustibility          susceptibilidad a la ignición [0, 1]
 *
 * ── RELACIÓN CON MaterialComponent ──────────────────────────────────────
 * MaterialComponent (Components/) existía antes de este HRFC como puente hacia
 * PhysicalState. MaterialState es su sucesor semánticamente correcto:
 * en lugar de registrarse en PropertyDescriptors dentro de PhysicalState,
 * ahora vive como dominio independiente dentro del SimulationContext.
 *
 * Los RelationEvaluators que antes leían propiedades de material desde
 * PhysicalState ahora las obtienen desde SimulationContext.material().
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * MaterialState es completamente inmutable tras su construcción.
 * El material de un objeto no cambia durante la simulación.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Nuevas propiedades de material (permeabilidad, sonoluminiscencia, etc.)
 * se añaden aquí sin tocar PhysicalState, KinematicState ni ningún otro
 * dominio del SimulationContext.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 */
public final class MaterialState implements Game.Engine.Physics.Core.DomainState {

    // ── Propiedades térmicas ──────────────────────────────────────────────

    /** Conductividad térmica [0, 1]. 0 = aislante perfecto, 1 = conductor perfecto. */
    private final double thermalConductivity;

    /** Capacidad calorífica específica (> 0). Mayor valor = más inercia térmica. */
    private final double heatCapacity;

    /** Difusividad térmica [0, 1]. Velocidad de disipación hacia el ambiente. */
    private final double thermalDiffusivity;

    /** Temperatura de fusión en unidades del juego. +∞ si no aplica. */
    private final double meltingPoint;

    /** Temperatura de ebullición en unidades del juego. +∞ si no aplica. */
    private final double boilingPoint;

    // ── Propiedades eléctricas ────────────────────────────────────────────

    /** Conductividad eléctrica efectiva [0, 1]. 0 = aislante, 1 = conductor perfecto. */
    private final double electricalConductivity;

    // ── Propiedades mecánicas ─────────────────────────────────────────────

    /** Compresibilidad [0, 1]. 0 = incompresible, 1 = muy compresible. */
    private final double compressibility;

    /** Elasticidad [0, 1]. 0 = completamente inelástico, 1 = elástico perfecto. */
    private final double elasticity;

    /** Dureza [0, 1]. 0 = muy blando, 1 = muy duro. */
    private final double hardness;

    /** Densidad relativa del material (> 0). */
    private final double density;

    // ── Propiedades de fricción ───────────────────────────────────────────

    /**
     * Coeficiente de rozamiento cinético del material [0, +∞).
     * Relevante para FrictionThermalEvaluator: Q ≈ μ × N × v × dt.
     */
    private final double frictionCoefficient;

    /**
     * Resistencia al rodamiento [0, 1].
     * 0 = sin resistencia al rodamiento, 1 = resistencia máxima.
     */
    private final double rollingResistance;

    // ── Propiedades fluídicas ─────────────────────────────────────────────

    /** Viscosidad del material [0, +∞). 0 para sólidos rígidos. */
    private final double viscosity;

    /** Absorción de humedad [0, 1]. Fracción de humedad ambiente que el objeto absorbe. */
    private final double humidityAbsorption;

    // ── Propiedades ópticas y energéticas ────────────────────────────────

    /** Absortividad [0, 1]. Fracción de radiación incidente que el objeto absorbe. */
    private final double absorptivity;

    /** Emisividad [0, 1]. Eficiencia de emisión de radiación térmica (ley de Stefan-Boltzmann). */
    private final double emissivity;

    /** Combustibilidad [0, 1]. Susceptibilidad a la ignición. 0 = no combustible. */
    private final double combustibility;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private MaterialState(Builder b) {
        this.thermalConductivity    = b.thermalConductivity;
        this.heatCapacity           = b.heatCapacity;
        this.thermalDiffusivity     = b.thermalDiffusivity;
        this.meltingPoint           = b.meltingPoint;
        this.boilingPoint           = b.boilingPoint;
        this.electricalConductivity = b.electricalConductivity;
        this.compressibility        = b.compressibility;
        this.elasticity             = b.elasticity;
        this.hardness               = b.hardness;
        this.density                = b.density;
        this.frictionCoefficient    = b.frictionCoefficient;
        this.rollingResistance      = b.rollingResistance;
        this.viscosity              = b.viscosity;
        this.humidityAbsorption     = b.humidityAbsorption;
        this.absorptivity           = b.absorptivity;
        this.emissivity             = b.emissivity;
        this.combustibility         = b.combustibility;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * MaterialState neutro estándar.
     * Valores de material genérico sin propiedades extremas.
     */
    public static final MaterialState DEFAULT = builder().build();

    // ── Accesores — dominio térmico ───────────────────────────────────────

    /** Conductividad térmica [0, 1]. */
    public double getThermalConductivity()    { return thermalConductivity; }

    /** Capacidad calorífica específica (> 0). */
    public double getHeatCapacity()           { return heatCapacity; }

    /** Difusividad térmica [0, 1]. */
    public double getThermalDiffusivity()     { return thermalDiffusivity; }

    /** Temperatura de fusión. */
    public double getMeltingPoint()           { return meltingPoint; }

    /** Temperatura de ebullición. */
    public double getBoilingPoint()           { return boilingPoint; }

    // ── Accesores — dominio eléctrico ─────────────────────────────────────

    /** Conductividad eléctrica efectiva [0, 1]. */
    public double getElectricalConductivity() { return electricalConductivity; }

    // ── Accesores — dominio mecánico ──────────────────────────────────────

    /** Compresibilidad [0, 1]. */
    public double getCompressibility()        { return compressibility; }

    /** Elasticidad [0, 1]. */
    public double getElasticity()             { return elasticity; }

    /** Dureza [0, 1]. */
    public double getHardness()               { return hardness; }

    /** Densidad relativa del material (> 0). */
    public double getDensity()                { return density; }

    // ── Accesores — fricción ──────────────────────────────────────────────

    /** Coeficiente de rozamiento cinético [0, +∞). */
    public double getFrictionCoefficient()    { return frictionCoefficient; }

    /** Resistencia al rodamiento [0, 1]. */
    public double getRollingResistance()      { return rollingResistance; }

    // ── Accesores — dominio fluídico ──────────────────────────────────────

    /** Viscosidad [0, +∞). */
    public double getViscosity()              { return viscosity; }

    /** Absorción de humedad [0, 1]. */
    public double getHumidityAbsorption()     { return humidityAbsorption; }

    // ── Accesores — dominio óptico / energético ───────────────────────────

    /** Absortividad [0, 1]. */
    public double getAbsorptivity()           { return absorptivity; }

    /** Emisividad [0, 1]. */
    public double getEmissivity()             { return emissivity; }

    /** Combustibilidad [0, 1]. */
    public double getCombustibility()         { return combustibility; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "MaterialState[tc=%.2f hc=%.1f μ=%.2f d=%.1f e=%.2f]",
            thermalConductivity, heatCapacity,
            frictionCoefficient, density, elasticity);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de MaterialState.
     *
     * Valores por defecto (material neutro):
     *   thermalConductivity    = 0.1      baja conductividad
     *   heatCapacity           = 1000.0   capacidad estándar
     *   thermalDiffusivity     = 0.1      baja difusividad
     *   meltingPoint           = +∞       sin punto de fusión
     *   boilingPoint           = +∞       sin punto de ebullición
     *   electricalConductivity = 0.2      semi-aislante
     *   compressibility        = 0.1      semi-rígido
     *   elasticity             = 0.3      algo elástico
     *   hardness               = 0.5      dureza media
     *   density                = 1000.0   similar al agua
     *   frictionCoefficient    = 0.3      rozamiento moderado
     *   rollingResistance      = 0.02     resistencia al rodamiento baja
     *   viscosity              = 0.0      sólido
     *   humidityAbsorption     = 0.1      baja absorción
     *   absorptivity           = 0.5      absorción media
     *   emissivity             = 0.5      emisividad media
     *   combustibility         = 0.0      no combustible
     */
    public static final class Builder {

        private double thermalConductivity    = 0.1;
        private double heatCapacity           = 1000.0;
        private double thermalDiffusivity     = 0.1;
        private double meltingPoint           = Double.POSITIVE_INFINITY;
        private double boilingPoint           = Double.POSITIVE_INFINITY;
        private double electricalConductivity = 0.2;
        private double compressibility        = 0.1;
        private double elasticity             = 0.3;
        private double hardness               = 0.5;
        private double density                = 1000.0;
        private double frictionCoefficient    = 0.3;
        private double rollingResistance      = 0.02;
        private double viscosity              = 0.0;
        private double humidityAbsorption     = 0.1;
        private double absorptivity           = 0.5;
        private double emissivity             = 0.5;
        private double combustibility         = 0.0;

        private Builder() {}

        public Builder thermalConductivity(double v)    { this.thermalConductivity    = clamp01(v);           return this; }
        public Builder heatCapacity(double v)           { this.heatCapacity           = Math.max(0.01, v);    return this; }
        public Builder thermalDiffusivity(double v)     { this.thermalDiffusivity     = clamp01(v);           return this; }
        public Builder meltingPoint(double v)           { this.meltingPoint           = Math.max(0.0, v);     return this; }
        public Builder boilingPoint(double v)           { this.boilingPoint           = Math.max(0.0, v);     return this; }
        public Builder electricalConductivity(double v) { this.electricalConductivity = clamp01(v);           return this; }
        public Builder compressibility(double v)        { this.compressibility        = clamp01(v);           return this; }
        public Builder elasticity(double v)             { this.elasticity             = clamp01(v);           return this; }
        public Builder hardness(double v)               { this.hardness               = clamp01(v);           return this; }
        public Builder density(double v)                { this.density                = Math.max(0.01, v);    return this; }
        public Builder frictionCoefficient(double v)    { this.frictionCoefficient    = Math.max(0.0, v);     return this; }
        public Builder rollingResistance(double v)      { this.rollingResistance      = clamp01(v);           return this; }
        public Builder viscosity(double v)              { this.viscosity              = Math.max(0.0, v);     return this; }
        public Builder humidityAbsorption(double v)     { this.humidityAbsorption     = clamp01(v);           return this; }
        public Builder absorptivity(double v)           { this.absorptivity           = clamp01(v);           return this; }
        public Builder emissivity(double v)             { this.emissivity             = clamp01(v);           return this; }
        public Builder combustibility(double v)         { this.combustibility         = clamp01(v);           return this; }

        /** Construye el MaterialState con la configuración acumulada. */
        public MaterialState build() { return new MaterialState(this); }

        private static double clamp01(double v) {
            return Math.max(0.0, Math.min(1.0, v));
        }
    }
}
