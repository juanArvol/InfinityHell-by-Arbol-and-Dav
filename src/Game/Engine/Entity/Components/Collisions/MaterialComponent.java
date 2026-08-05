package Game.Engine.Entity.Components.Collisions;

import Game.Engine.Component;
import Game.Engine.Physics.Core.MaterialData;
import Game.Engine.Physics.Core.PhysicalState;
import Game.Engine.Physics.Electrical.ElectricalProperties;
import Game.Engine.Physics.Fluid.FluidProperties;
import Game.Engine.Physics.Mechanical.MechanicalProperties;
import Game.Engine.Physics.Thermal.ThermalProperties;

/**
 * Propiedades intrínsecas del material de un objeto.
 *
 * ── HRFC-019 — Eliminación Definitiva del Modelo Orientado a Tipos de Ley ─
 *
 * ── QUÉ ES MaterialComponent ─────────────────────────────────────────────
 * Describe la naturaleza física del material de un objeto.
 * Sus valores son constantes tras la construcción — un material no cambia
 * en runtime.
 *
 * En el modelo HRFC-019, las propiedades de material ya no son un sistema
 * separado del estado físico. Cuando se construye el PhysicalState de un
 * objeto, las propiedades de material se registran en él junto con las
 * propiedades de estado (temperatura, carga, humedad...).
 *
 * MaterialComponent actúa como fuente conveniente para obtener los valores
 * de material al construir el PhysicalState de un objeto en su Assembler.
 *
 * ── RELACIÓN CON PhysicalState ───────────────────────────────────────────
 * Las leyes físicas leen las propiedades de material con el mismo get() que
 * usan para leer temperatura o carga. No hay distinción en el Core entre
 * "propiedad de estado" y "propiedad de material". Ambas son entradas en
 * el mapa del PhysicalState, identificadas por string.
 *
 * ── PATRÓN DE USO EN ASSEMBLER ───────────────────────────────────────────
 *
 *   MaterialComponent mat = new MaterialComponent.Builder()
 *       .thermalConductivity(0.8)
 *       .heatCapacity(500.0)
 *       ...
 *       .build();
 *
 *   PhysicalState state = PhysicalState.builder()
 *       .register(CoreProperties.TEMPERATURE, 20.0)
 *       .register(CoreProperties.CHARGE)
 *       .register(CoreProperties.HUMIDITY)
 *       .register(CoreProperties.PRESSURE)
 *       .registerMaterial(mat::registerInto)    // registra todas las props del material
 *       .build();
 *
 *   addComponent(new PhysicsComponent(state));
 *
 * ── QUÉ NO CONTIENE ──────────────────────────────────────────────────────
 *   ✗ Ninguna lógica de simulación.
 *   ✗ Ninguna regla de gameplay (ignición, congelación, etc.).
 *   ✗ Ninguna clasificación de material (metal, madera, gas...).
 *   ✗ Ninguna referencia a CoreDomains ni a dominios físicos.
 */
public final class MaterialComponent extends Component implements MaterialData {

    private final double thermalConductivity;
    private final double heatCapacity;
    private final double thermalDiffusivity;
    private final double meltingPoint;
    private final double boilingPoint;
    private final double electricalConductivity;
    private final double humidityAbsorption;
    private final double viscosity;
    private final double compressibility;
    private final double elasticity;
    private final double hardness;
    private final double density;

    /** Material neutro estándar. Usado cuando un objeto no tiene MaterialComponent. */
    public static final MaterialComponent DEFAULT = new MaterialComponent.Builder().build();

    private MaterialComponent(Builder b) {
        this.thermalConductivity   = b.thermalConductivity;
        this.heatCapacity          = b.heatCapacity;
        this.thermalDiffusivity    = b.thermalDiffusivity;
        this.meltingPoint          = b.meltingPoint;
        this.boilingPoint          = b.boilingPoint;
        this.electricalConductivity = b.electricalConductivity;
        this.humidityAbsorption    = b.humidityAbsorption;
        this.viscosity             = b.viscosity;
        this.compressibility       = b.compressibility;
        this.elasticity            = b.elasticity;
        this.hardness              = b.hardness;
        this.density               = b.density;
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Conductividad térmica [0, 1]. */
    @Override
    public double getThermalConductivity()    { return thermalConductivity; }

    /** Capacidad calorífica específica (0, +∞). */
    @Override
    public double getHeatCapacity()           { return heatCapacity; }

    /** Difusividad térmica [0, 1]. */
    @Override
    public double getThermalDiffusivity()     { return thermalDiffusivity; }

    /** Punto de fusión. Double.POSITIVE_INFINITY si no aplica. */
    @Override
    public double getMeltingPoint()           { return meltingPoint; }

    /** Punto de ebullición. Double.POSITIVE_INFINITY si no aplica. */
    @Override
    public double getBoilingPoint()           { return boilingPoint; }

    /** Conductividad eléctrica efectiva [0, 1]. */
    @Override
    public double getElectricalConductivity() { return electricalConductivity; }

    /** Coeficiente de absorción de humedad [0, 1]. */
    @Override
    public double getHumidityAbsorption()     { return humidityAbsorption; }

    /** Viscosidad [0, +∞). 0 para sólidos. */
    @Override
    public double getViscosity()              { return viscosity; }

    /** Compresibilidad [0, 1]. */
    @Override
    public double getCompressibility()        { return compressibility; }

    /** Elasticidad [0, 1]. */
    @Override
    public double getElasticity()             { return elasticity; }

    /** Dureza [0, 1]. */
    @Override
    public double getHardness()               { return hardness; }

    /** Densidad relativa (0, +∞). */
    @Override
    public double getDensity()                { return density; }

    // ── Integración con PhysicalState ─────────────────────────────────────

    /**
     * Registra todas las propiedades de este material en el builder de
     * PhysicalState dado. Las leyes físicas las leerán con get() junto
     * al resto del estado del objeto.
     *
     * Uso en Assembler (via referencia a método):
     *   PhysicalState.builder()
     *       .register(ThermalProperties.TEMPERATURE, 20.0)
     *       .registerMaterial(mat::registerInto)
     *       .build();
     *
     * @param builder el builder de PhysicalState donde registrar.
     * @return el mismo builder, para encadenado.
     */
    public PhysicalState.Builder registerInto(PhysicalState.Builder builder) {
        if (builder == null) return null;
        builder.register(ThermalProperties.THERMAL_CONDUCTIVITY,    thermalConductivity);
        builder.register(ThermalProperties.HEAT_CAPACITY,           heatCapacity);
        builder.register(ThermalProperties.THERMAL_DIFFUSIVITY,     thermalDiffusivity);
        builder.register(ThermalProperties.MELTING_POINT,           meltingPoint);
        builder.register(ThermalProperties.BOILING_POINT,           boilingPoint);
        builder.register(ElectricalProperties.ELECTRICAL_CONDUCTIVITY, electricalConductivity);
        builder.register(FluidProperties.HUMIDITY_ABSORPTION,       humidityAbsorption);
        builder.register(FluidProperties.VISCOSITY,                 viscosity);
        builder.register(MechanicalProperties.COMPRESSIBILITY,      compressibility);
        builder.register(MechanicalProperties.ELASTICITY,           elasticity);
        builder.register(MechanicalProperties.HARDNESS,             hardness);
        builder.register(MechanicalProperties.DENSITY,              density);
        return builder;
    }

    // ── Builder ───────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    /**
     * Builder de MaterialComponent.
     *
     * Valores por defecto (material neutro estándar):
     *   thermalConductivity    = 0.1     baja conductividad
     *   heatCapacity           = 1000.0  capacidad estándar
     *   thermalDiffusivity     = 0.1     baja difusividad
     *   meltingPoint           = +∞      sin punto de fusión
     *   boilingPoint           = +∞      sin punto de ebullición
     *   electricalConductivity = 0.2     semi-aislante
     *   humidityAbsorption     = 0.1     baja absorción
     *   viscosity              = 0.0     sólido
     *   compressibility        = 0.1     semi-rígido
     *   elasticity             = 0.3     algo elástico
     *   hardness               = 0.5     dureza media
     *   density                = 1000.0  similar al agua
     */
    public static final class Builder {

        private double thermalConductivity    = 0.1;
        private double heatCapacity           = 1000.0;
        private double thermalDiffusivity     = 0.1;
        private double meltingPoint           = Double.POSITIVE_INFINITY;
        private double boilingPoint           = Double.POSITIVE_INFINITY;
        private double electricalConductivity = 0.2;
        private double humidityAbsorption     = 0.1;
        private double viscosity              = 0.0;
        private double compressibility        = 0.1;
        private double elasticity             = 0.3;
        private double hardness               = 0.5;
        private double density                = 1000.0;

        public Builder thermalConductivity(double v)    { this.thermalConductivity    = c01(v);           return this; }
        public Builder heatCapacity(double v)           { this.heatCapacity           = Math.max(0.01,v); return this; }
        public Builder thermalDiffusivity(double v)     { this.thermalDiffusivity     = c01(v);           return this; }
        public Builder meltingPoint(double v)           { this.meltingPoint           = Math.max(0.0,v);  return this; }
        public Builder boilingPoint(double v)           { this.boilingPoint           = Math.max(0.0,v);  return this; }
        public Builder electricalConductivity(double v) { this.electricalConductivity = c01(v);           return this; }
        public Builder humidityAbsorption(double v)     { this.humidityAbsorption     = c01(v);           return this; }
        public Builder viscosity(double v)              { this.viscosity              = Math.max(0.0,v);  return this; }
        public Builder compressibility(double v)        { this.compressibility        = c01(v);           return this; }
        public Builder elasticity(double v)             { this.elasticity             = c01(v);           return this; }
        public Builder hardness(double v)               { this.hardness               = c01(v);           return this; }
        public Builder density(double v)                { this.density                = Math.max(0.01,v); return this; }

        public MaterialComponent build() { return new MaterialComponent(this); }

        private static double c01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
    }
}
