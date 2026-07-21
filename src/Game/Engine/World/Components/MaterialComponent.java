package Game.Engine.World.Components;

import Game.Engine.Component;

/**
 * Propiedades intrínsecas del material de un objeto.
 *
 * ── HRFC-016 — Consolidación del modelo emergente ────────────────────────
 *
 * ── QUÉ ES MaterialComponent ─────────────────────────────────────────────
 * Describe QUÉ ES el objeto desde el punto de vista físico fundamental.
 * No contiene estado que cambia en runtime — contiene la naturaleza del material.
 *
 * La distinción con los componentes de estado es esencial:
 *
 *   MaterialComponent   → naturaleza   (constante tras construcción)
 *   ThermalComponent    → estado térmico actual    (cambia por simulación)
 *   ElectricalComponent → estado eléctrico actual  (cambia por simulación)
 *   FluidComponent      → estado fluídico actual   (cambia por simulación)
 *   PressureComponent   → estado de presión actual (cambia por simulación)
 *
 * ── QUÉ NO CONTIENE ──────────────────────────────────────────────────────
 * MaterialComponent NO contiene reglas de gameplay ni umbrales de ignición,
 * congelación u otros fenómenos concretos.
 *
 * Nunca deberá contener:
 *   ✗ ignitionThreshold   → es una regla, no una propiedad
 *   ✗ canBurn             → es una capacidad derivada, no una propiedad
 *   ✗ MaterialCategory    → es una clasificación, no una propiedad física
 *   ✗ Lógica de ningún tipo
 *   ✗ Condiciones de ningún tipo
 *
 * ── QUÉ CONTIENE ─────────────────────────────────────────────────────────
 * Únicamente propiedades físicas fundamentales:
 *
 *   Térmicas:
 *   ✓ thermalConductivity      → velocidad de intercambio térmico
 *   ✓ heatCapacity             → resistencia al cambio de temperatura
 *   ✓ thermalDiffusivity       → velocidad de equilibrio térmico interno
 *   ✓ thermalExpansionCoeff    → expansión volumétrica por unidad de temperatura
 *   ✓ meltingPoint             → temperatura en la que el material cambia de fase (sólido→líquido)
 *   ✓ boilingPoint             → temperatura en la que el material cambia de fase (líquido→gas)
 *
 *   Eléctricas:
 *   ✓ electricalResistance     → resistencia al flujo de carga eléctrica
 *
 *   Fluídicas:
 *   ✓ humidityAbsorption       → velocidad de absorción/desorción de humedad
 *   ✓ viscosity                → resistencia del material al flujo interno
 *
 *   Mecánicas:
 *   ✓ compressibility          → resistencia a cambios de presión
 *   ✓ elasticity               → fracción de energía cinética conservada en rebotes
 *   ✓ hardness                 → resistencia a deformación / penetración mecánica
 *   ✓ density                  → masa por unidad de volumen
 *
 * La combinación de estas propiedades permite a la simulación determinar, por
 * composición, qué fenómenos emergen — sin que MaterialComponent conozca ninguno.
 *
 * ── DISEÑO: INMUTABLE TRAS CONSTRUCCIÓN ───────────────────────────────────
 * Todos los campos son final. Un material no cambia en runtime.
 * La totalidad del comportamiento del material emerge de sus propiedades.
 *
 * ── USO EN ASSEMBLER ──────────────────────────────────────────────────────
 *
 *   // Madera
 *   addComponent(new MaterialComponent.Builder()
 *       .thermalConductivity(0.12)
 *       .heatCapacity(1700.0)
 *       .thermalDiffusivity(0.08)
 *       .thermalExpansionCoeff(0.000050)
 *       .meltingPoint(Double.POSITIVE_INFINITY)  // la madera no se funde, se carboniza
 *       .boilingPoint(Double.POSITIVE_INFINITY)
 *       .electricalResistance(0.95)
 *       .humidityAbsorption(0.6)
 *       .viscosity(0.0)                          // sólido: sin flujo interno
 *       .compressibility(0.3)
 *       .elasticity(0.2)
 *       .hardness(0.4)
 *       .density(600.0)
 *       .build());
 *
 *   // Metal
 *   addComponent(new MaterialComponent.Builder()
 *       .thermalConductivity(0.8)
 *       .heatCapacity(500.0)
 *       .thermalDiffusivity(0.6)
 *       .thermalExpansionCoeff(0.000012)
 *       .meltingPoint(1200.0)
 *       .boilingPoint(3000.0)
 *       .electricalResistance(0.01)
 *       .humidityAbsorption(0.02)
 *       .viscosity(0.0)
 *       .compressibility(0.05)
 *       .elasticity(0.6)
 *       .hardness(0.9)
 *       .density(7800.0)
 *       .build());
 *
 *   // O usar MaterialPresets para valores predefinidos:
 *   addComponent(MaterialPresets.oak().build());
 *   addComponent(MaterialPresets.iron().build());
 *
 * ── ACCESO DESDE LA SIMULACIÓN ────────────────────────────────────────────
 *
 *   MaterialComponent mat = obj.getComponent(MaterialComponent.class);
 *   if (mat == null) mat = MaterialComponent.DEFAULT;
 *   double cond = mat.getThermalConductivity();
 *
 * ── RELACIÓN CON PropertyKeys ─────────────────────────────────────────────
 * Las propiedades de MaterialComponent tienen claves correspondientes en
 * PropertyKeys (THERMAL_CONDUCTIVITY, HEAT_CAPACITY, ELECTRICAL_RESISTANCE…).
 * El sistema de modificadores puede ampliar o reducir estas propiedades
 * mediante buffs, debuffs, magia o equipamiento, sin que MaterialComponent
 * conozca nada de ese mecanismo.
 */
public final class MaterialComponent extends Component {

    // ── Propiedades térmicas ──────────────────────────────────────────────

    /**
     * Conductividad térmica.
     * Velocidad a la que este material intercambia energía térmica con otros
     * materiales o con campos de calor del entorno.
     * Rango [0, 1]: 0 = aislante perfecto, 1 = conductor perfecto.
     */
    private final double thermalConductivity;

    /**
     * Capacidad calorífica específica.
     * Cantidad de energía necesaria para cambiar la temperatura del material
     * en 1 unidad. Mayor valor = mayor inercia térmica.
     * Rango (0, +∞). Unidad: J/kg·° (arbitrario, relativo al juego).
     */
    private final double heatCapacity;

    /**
     * Difusividad térmica.
     * Velocidad a la que el calor se distribuye internamente dentro del material.
     * Rango [0, 1]: 0 = no difunde, 1 = difusión instantánea.
     */
    private final double thermalDiffusivity;

    /**
     * Coeficiente de expansión térmica.
     * Fracción del cambio de volumen por unidad de temperatura.
     * Afecta la magnitud de la expansión en CoreEquations.thermalExpansion().
     * Rango (0, +∞). Valor típico de referencia: 0.000010–0.000100 (escala de juego).
     *
     * Ejemplos orientativos en escala de juego:
     *   Metal:   0.000012  (poco expansivo)
     *   Madera:  0.000050  (moderadamente expansivo)
     *   Caucho:  0.000200  (muy expansivo)
     *   Gas:     0.003300  (altamente expansivo)
     */
    private final double thermalExpansionCoeff;

    /**
     * Punto de fusión relativo del material.
     * Temperatura (en unidades del juego) en la que el material transita
     * de estado sólido a líquido.
     *
     * El Engine no conoce el concepto de "fusión" — este valor es simplemente
     * una propiedad que Gameplay puede leer para decidir si producir un cambio
     * de fase. Por convención, Double.POSITIVE_INFINITY indica que el material
     * no tiene punto de fusión observable en el universo del juego.
     *
     * Rango: (0, +∞] o Double.POSITIVE_INFINITY si no aplica.
     */
    private final double meltingPoint;

    /**
     * Punto de ebullición relativo del material.
     * Temperatura (en unidades del juego) en la que el material transita
     * de estado líquido a gas.
     *
     * El Engine no conoce el concepto de "ebullición". Este valor es observable
     * por Gameplay para determinar cambios de fase (evaporación, vaporización).
     * Por convención, Double.POSITIVE_INFINITY indica que no aplica.
     *
     * Rango: (0, +∞] o Double.POSITIVE_INFINITY si no aplica.
     */
    private final double boilingPoint;

    // ── Propiedades eléctricas ────────────────────────────────────────────

    /**
     * Resistencia eléctrica del material.
     * Resistencia al flujo de carga eléctrica a través del material.
     * Rango [0, 1]: 0 = conductor perfecto, 1 = aislante perfecto.
     */
    private final double electricalResistance;

    // ── Propiedades fluídicas ─────────────────────────────────────────────

    /**
     * Coeficiente de absorción de humedad.
     * Velocidad a la que el material absorbe o libera humedad del entorno.
     * Rango [0, 1]: 0 = impermeable, 1 = absorción/liberación instantánea.
     */
    private final double humidityAbsorption;

    /**
     * Viscosidad del material.
     * Resistencia del material al flujo interno. Relevante para materiales
     * en estado líquido o semilíquido. Para sólidos, el valor es 0.
     *
     * Rango [0, +∞). 0 = fluido perfecto (sin resistencia).
     * Valores de referencia (escala de juego):
     *   Agua pura:   0.001
     *   Aceite:      0.1
     *   Miel:        5.0
     *   Lava:        100.0+
     */
    private final double viscosity;

    // ── Propiedades mecánicas ─────────────────────────────────────────────

    /**
     * Compresibilidad del material.
     * Facilidad con la que el material cambia de volumen bajo presión.
     * Rango [0, 1]: 0 = incompresible (líquido ideal), 1 = muy compresible (gas).
     */
    private final double compressibility;

    /**
     * Elasticidad del material.
     * Fracción de energía cinética conservada al rebotar o deformarse.
     * Rango [0, 1]: 0 = completamente inelástico, 1 = elástico perfecto.
     */
    private final double elasticity;

    /**
     * Dureza del material.
     * Resistencia a la deformación permanente o penetración mecánica.
     * Rango [0, 1]: 0 = extremadamente blando, 1 = durísimo.
     */
    private final double hardness;

    /**
     * Densidad del material en unidades relativas del juego.
     * Masa por unidad de volumen. Afecta la inercia térmica, eléctrica y mecánica.
     * Rango (0, +∞). Unidad: kg/m³ (arbitrario, relativo al juego).
     */
    private final double density;

    // ── Instancia por defecto ─────────────────────────────────────────────

    /**
     * Material neutro por defecto.
     * Representa un material estándar sin propiedades especiales.
     * Usado cuando una entidad no tiene MaterialComponent explícito.
     */
    public static final MaterialComponent DEFAULT = new MaterialComponent.Builder().build();

    // ── Constructor privado (usar Builder) ────────────────────────────────

    private MaterialComponent(Builder b) {
        this.thermalConductivity   = b.thermalConductivity;
        this.heatCapacity          = b.heatCapacity;
        this.thermalDiffusivity    = b.thermalDiffusivity;
        this.thermalExpansionCoeff = b.thermalExpansionCoeff;
        this.meltingPoint          = b.meltingPoint;
        this.boilingPoint          = b.boilingPoint;
        this.electricalResistance  = b.electricalResistance;
        this.humidityAbsorption    = b.humidityAbsorption;
        this.viscosity             = b.viscosity;
        this.compressibility       = b.compressibility;
        this.elasticity            = b.elasticity;
        this.hardness              = b.hardness;
        this.density               = b.density;
    }

    // ── Accesores — térmicos ──────────────────────────────────────────────

    /** Conductividad térmica [0, 1]. */
    public double getThermalConductivity()   { return thermalConductivity; }

    /** Capacidad calorífica específica (0, +∞). */
    public double getHeatCapacity()          { return heatCapacity; }

    /** Difusividad térmica [0, 1]. */
    public double getThermalDiffusivity()    { return thermalDiffusivity; }

    /**
     * Coeficiente de expansión térmica (0, +∞).
     * Cuánto se expande el material por unidad de temperatura.
     */
    public double getThermalExpansionCoeff() { return thermalExpansionCoeff; }

    /**
     * Punto de fusión en unidades del juego.
     * Double.POSITIVE_INFINITY si no aplica para este material.
     */
    public double getMeltingPoint()          { return meltingPoint; }

    /**
     * Punto de ebullición en unidades del juego.
     * Double.POSITIVE_INFINITY si no aplica para este material.
     */
    public double getBoilingPoint()          { return boilingPoint; }

    // ── Accesores — eléctricos ────────────────────────────────────────────

    /** Resistencia eléctrica [0, 1]. */
    public double getElectricalResistance()  { return electricalResistance; }

    /** Conductividad eléctrica efectiva: 1 − resistencia. */
    public double getElectricalConductivity() { return 1.0 - electricalResistance; }

    // ── Accesores — fluídicos ─────────────────────────────────────────────

    /** Coeficiente de absorción de humedad [0, 1]. */
    public double getHumidityAbsorption()    { return humidityAbsorption; }

    /**
     * Viscosidad del material [0, +∞).
     * 0 para sólidos; valores positivos para líquidos/semilíquidos.
     */
    public double getViscosity()             { return viscosity; }

    // ── Accesores — mecánicos ─────────────────────────────────────────────

    /** Compresibilidad [0, 1]. */
    public double getCompressibility()       { return compressibility; }

    /** Elasticidad [0, 1]. */
    public double getElasticity()            { return elasticity; }

    /** Dureza [0, 1]. */
    public double getHardness()              { return hardness; }

    /** Densidad relativa (0, +∞). */
    public double getDensity()               { return density; }

    // ── Builder ───────────────────────────────────────────────────────────

    /** Crea un Builder con valores por defecto neutros. */
    public static Builder builder() { return new Builder(); }

    /**
     * Builder de MaterialComponent.
     *
     * Valores por defecto (material neutro estándar):
     *   thermalConductivity  = 0.1      baja conductividad
     *   heatCapacity         = 1000.0   capacidad estándar
     *   thermalDiffusivity   = 0.1      baja difusividad
     *   thermalExpansionCoeff= 0.00005  expansión moderada
     *   meltingPoint         = +∞       sin punto de fusión
     *   boilingPoint         = +∞       sin punto de ebullición
     *   electricalResistance = 0.8      semi-aislante
     *   humidityAbsorption   = 0.1      baja absorción
     *   viscosity            = 0.0      sólido (sin flujo)
     *   compressibility      = 0.1      semi-rígido
     *   elasticity           = 0.3      algo elástico
     *   hardness             = 0.5      dureza media
     *   density              = 1000.0   similar al agua
     */
    public static final class Builder {

        private double thermalConductivity   = 0.1;
        private double heatCapacity          = 1000.0;
        private double thermalDiffusivity    = 0.1;
        private double thermalExpansionCoeff = 0.00005;
        private double meltingPoint          = Double.POSITIVE_INFINITY;
        private double boilingPoint          = Double.POSITIVE_INFINITY;
        private double electricalResistance  = 0.8;
        private double humidityAbsorption    = 0.1;
        private double viscosity             = 0.0;
        private double compressibility       = 0.1;
        private double elasticity            = 0.3;
        private double hardness              = 0.5;
        private double density               = 1000.0;

        public Builder thermalConductivity(double v)   { this.thermalConductivity   = clamp01(v);           return this; }
        public Builder heatCapacity(double v)          { this.heatCapacity          = Math.max(0.01, v);     return this; }
        public Builder thermalDiffusivity(double v)    { this.thermalDiffusivity    = clamp01(v);            return this; }
        public Builder thermalExpansionCoeff(double v) { this.thermalExpansionCoeff = Math.max(0.0, v);      return this; }
        public Builder meltingPoint(double v)          { this.meltingPoint          = Math.max(0.0, v);      return this; }
        public Builder boilingPoint(double v)          { this.boilingPoint          = Math.max(0.0, v);      return this; }
        public Builder electricalResistance(double v)  { this.electricalResistance  = clamp01(v);            return this; }
        public Builder humidityAbsorption(double v)    { this.humidityAbsorption    = clamp01(v);            return this; }
        public Builder viscosity(double v)             { this.viscosity             = Math.max(0.0, v);      return this; }
        public Builder compressibility(double v)       { this.compressibility       = clamp01(v);            return this; }
        public Builder elasticity(double v)            { this.elasticity            = clamp01(v);            return this; }
        public Builder hardness(double v)              { this.hardness              = clamp01(v);            return this; }
        public Builder density(double v)               { this.density               = Math.max(0.01, v);     return this; }

        public MaterialComponent build() {
            return new MaterialComponent(this);
        }

        private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
    }
}
