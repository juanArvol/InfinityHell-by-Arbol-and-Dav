package Game.Engine.Physics.Environment;

/**
 * Estado descriptivo de las condiciones del entorno donde ocurre la simulación.
 *
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * EnvironmentState describe el medio donde existe el objeto simulado.
 * No describe al objeto. No describe lo que le ocurre al objeto.
 * Describe las condiciones del mundo que rodean al objeto.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * EnvironmentState solo describe. No aplica fuerzas. No modifica propiedades.
 * Las relaciones físicas son las responsables de interpretar estas condiciones
 * y producir los efectos correspondientes sobre los estados del objeto.
 *
 *   EnvironmentState  →  describe (el entorno donde existe el objeto)
 *   Relation          →  interpreta (qué efecto tiene ese entorno sobre el objeto)
 *
 * ── DOMINIO ───────────────────────────────────────────────────────────────
 * Propiedades incluidas:
 *
 *   Condiciones térmicas:
 *     ambientTemperature    temperatura del entorno (referencia para disipación)
 *
 *   Condiciones atmosféricas / fluídicas:
 *     atmosphericPressure   presión del fluido circundante
 *     ambientHumidity       humedad relativa del aire/fluido [0, 1]
 *     windX                 componente X del viento (u/s, con signo)
 *     windY                 componente Y del viento (u/s, con signo)
 *     fluidDensity          densidad del fluido circundante (para empuje de Arquímedes)
 *     fluidViscosity        viscosidad del fluido circundante (para ley de Stokes)
 *
 *   Gravedad local:
 *     gravityX              componente X de la gravedad local (u/s²)
 *     gravityY              componente Y de la gravedad local (u/s²)
 *
 *   Campos externos:
 *     electricFieldX        componente X del campo eléctrico local (V/m relativos)
 *     electricFieldY        componente Y del campo eléctrico local
 *     magneticFieldZ        componente Z del campo magnético local (en 2D solo Z)
 *
 *   Radiación:
 *     ambientRadiation      nivel de radiación ambiental (para PlanckEvaluator)
 *     illuminance           nivel de iluminación (lux relativos, para óptica)
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * EnvironmentState es completamente inmutable tras su construcción.
 * Un cambio en las condiciones del entorno produce un nuevo EnvironmentState.
 *
 * ── ESTADO POR DEFECTO ───────────────────────────────────────────────────
 * EnvironmentState.STANDARD representa condiciones estándar (temperatura ambiente
 * 20°C, gravedad estándar (0, 9.8), sin viento, presión atmosférica 1.0).
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 */
public final class EnvironmentState implements Game.Engine.Physics.Core.DomainState {

    // ── Condiciones térmicas ──────────────────────────────────────────────

    /**
     * Temperatura del entorno en unidades del juego.
     * Referencia para los evaluadores de disipación térmica ambiental.
     * Los objetos más calientes que el entorno disipan calor hacia él.
     */
    private final double ambientTemperature;

    // ── Condiciones atmosféricas ──────────────────────────────────────────

    /** Presión atmosférica del fluido circundante. 1.0 = presión estándar. */
    private final double atmosphericPressure;

    /**
     * Humedad relativa del aire/fluido circundante [0, 1].
     * Usada por evaluadores fluídicos (Fick, Archimedes).
     */
    private final double ambientHumidity;

    /** Componente X del viento en u/s. Positivo = derecha. */
    private final double windX;

    /** Componente Y del viento en u/s. Positivo = abajo (conv. AWT). */
    private final double windY;

    /**
     * Densidad del fluido circundante.
     * Usada por ArchimedesEvaluator para calcular el empuje hidrostático.
     * 0 = vacío (sin empuje).
     */
    private final double fluidDensity;

    /**
     * Viscosidad del fluido circundante.
     * Usada por StokesEvaluator para calcular la resistencia viscosa.
     * 0 = vacío / fluido no viscoso.
     */
    private final double fluidViscosity;

    // ── Gravedad local ────────────────────────────────────────────────────

    /**
     * Componente X de la gravedad local en u/s².
     * 0 en la mayoría de los casos (gravedad estrictamente vertical).
     */
    private final double gravityX;

    /**
     * Componente Y de la gravedad local en u/s².
     * Positivo = hacia abajo (convención AWT/juego).
     * Default: 9.8 (gravedad estándar).
     */
    private final double gravityY;

    // ── Campos externos ───────────────────────────────────────────────────

    /** Componente X del campo eléctrico local en V/m relativos. */
    private final double electricFieldX;

    /** Componente Y del campo eléctrico local en V/m relativos. */
    private final double electricFieldY;

    /**
     * Componente Z del campo magnético local (en simulación 2D, solo la
     * componente perpendicular al plano es relevante).
     */
    private final double magneticFieldZ;

    // ── Radiación e iluminación ───────────────────────────────────────────

    /**
     * Nivel de radiación ambiental [0, +∞).
     * Usada por PlanckEvaluator y RadiationThermalEvaluator.
     * 0 = sin radiación ambiental.
     */
    private final double ambientRadiation;

    /**
     * Nivel de iluminación ambiental en lux relativos [0, +∞).
     * 0 = oscuridad total. 1 = iluminación estándar de referencia.
     */
    private final double illuminance;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private EnvironmentState(Builder b) {
        this.ambientTemperature  = b.ambientTemperature;
        this.atmosphericPressure = b.atmosphericPressure;
        this.ambientHumidity     = b.ambientHumidity;
        this.windX               = b.windX;
        this.windY               = b.windY;
        this.fluidDensity        = b.fluidDensity;
        this.fluidViscosity      = b.fluidViscosity;
        this.gravityX            = b.gravityX;
        this.gravityY            = b.gravityY;
        this.electricFieldX      = b.electricFieldX;
        this.electricFieldY      = b.electricFieldY;
        this.magneticFieldZ      = b.magneticFieldZ;
        this.ambientRadiation    = b.ambientRadiation;
        this.illuminance         = b.illuminance;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * Condiciones estándar del entorno.
     * Temperatura ambiente 20°C, gravedad estándar (0, 9.8), sin viento,
     * sin campos externos, presión atmosférica 1.0.
     */
    public static final EnvironmentState STANDARD = builder().build();

    /**
     * Condiciones de microgravedad / espacio exterior.
     * Sin gravedad, sin atmósfera, sin viento. Temperatura ambiente = -270°C relativo.
     */
    public static final EnvironmentState VACUUM = builder()
        .ambientTemperature(-270.0)
        .atmosphericPressure(0.0)
        .ambientHumidity(0.0)
        .gravityX(0.0)
        .gravityY(0.0)
        .fluidDensity(0.0)
        .fluidViscosity(0.0)
        .build();

    // ── Accesores — condiciones térmicas ──────────────────────────────────

    /** Temperatura del entorno en unidades del juego. */
    public double getAmbientTemperature()  { return ambientTemperature; }

    // ── Accesores — condiciones atmosféricas ──────────────────────────────

    /** Presión atmosférica del fluido circundante. */
    public double getAtmosphericPressure() { return atmosphericPressure; }

    /** Humedad relativa del aire/fluido [0, 1]. */
    public double getAmbientHumidity()     { return ambientHumidity; }

    /** Componente X del viento en u/s. */
    public double getWindX()               { return windX; }

    /** Componente Y del viento en u/s. */
    public double getWindY()               { return windY; }

    /** Módulo del viento en u/s. */
    public double getWindSpeed() {
        return Math.sqrt(windX * windX + windY * windY);
    }

    /** Densidad del fluido circundante. */
    public double getFluidDensity()        { return fluidDensity; }

    /** Viscosidad del fluido circundante. */
    public double getFluidViscosity()      { return fluidViscosity; }

    // ── Accesores — gravedad ──────────────────────────────────────────────

    /** Componente X de la gravedad local en u/s². */
    public double getGravityX()            { return gravityX; }

    /** Componente Y de la gravedad local en u/s². */
    public double getGravityY()            { return gravityY; }

    /** Módulo de la gravedad local en u/s². */
    public double getGravityMagnitude() {
        return Math.sqrt(gravityX * gravityX + gravityY * gravityY);
    }

    // ── Accesores — campos externos ───────────────────────────────────────

    /** Componente X del campo eléctrico local. */
    public double getElectricFieldX()      { return electricFieldX; }

    /** Componente Y del campo eléctrico local. */
    public double getElectricFieldY()      { return electricFieldY; }

    /** Componente Z del campo magnético local. */
    public double getMagneticFieldZ()      { return magneticFieldZ; }

    // ── Accesores — radiación ─────────────────────────────────────────────

    /** Nivel de radiación ambiental [0, +∞). */
    public double getAmbientRadiation()    { return ambientRadiation; }

    /** Nivel de iluminación ambiental. */
    public double getIlluminance()         { return illuminance; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "EnvironmentState[T=%.1f P=%.2f wind=(%.1f,%.1f) g=(%.1f,%.1f)]",
            ambientTemperature, atmosphericPressure,
            windX, windY, gravityX, gravityY);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de EnvironmentState.
     *
     * Valores por defecto (condiciones estándar):
     *   ambientTemperature  = 0.0     temperatura ambiente relativa = 0
     *   atmosphericPressure = 1.0     presión atmosférica estándar
     *   ambientHumidity     = 0.6     humedad relativa 60%
     *   windX               = 0.0     sin viento
     *   windY               = 0.0     sin viento
     *   fluidDensity        = 1.2     densidad del aire (kg/m³ relativo)
     *   fluidViscosity      = 0.0     sin viscosidad apreciable
     *   gravityX            = 0.0     sin componente horizontal
     *   gravityY            = 9.8     gravedad estándar (positivo = abajo)
     *   electricFieldX      = 0.0     sin campo eléctrico
     *   electricFieldY      = 0.0     sin campo eléctrico
     *   magneticFieldZ      = 0.0     sin campo magnético
     *   ambientRadiation    = 0.0     sin radiación ambiental
     *   illuminance         = 1.0     iluminación estándar
     */
    public static final class Builder {

        private double ambientTemperature  = 0.0;
        private double atmosphericPressure = 1.0;
        private double ambientHumidity     = 0.6;
        private double windX               = 0.0;
        private double windY               = 0.0;
        private double fluidDensity        = 1.2;
        private double fluidViscosity      = 0.0;
        private double gravityX            = 0.0;
        private double gravityY            = 9.8;
        private double electricFieldX      = 0.0;
        private double electricFieldY      = 0.0;
        private double magneticFieldZ      = 0.0;
        private double ambientRadiation    = 0.0;
        private double illuminance         = 1.0;

        private Builder() {}

        public Builder ambientTemperature(double v)  { this.ambientTemperature  = v;                     return this; }
        public Builder atmosphericPressure(double v) { this.atmosphericPressure = Math.max(0.0, v);      return this; }
        public Builder ambientHumidity(double v)     { this.ambientHumidity     = clamp01(v);            return this; }
        public Builder windX(double v)               { this.windX               = v;                     return this; }
        public Builder windY(double v)               { this.windY               = v;                     return this; }
        public Builder fluidDensity(double v)        { this.fluidDensity        = Math.max(0.0, v);      return this; }
        public Builder fluidViscosity(double v)      { this.fluidViscosity      = Math.max(0.0, v);      return this; }
        public Builder gravityX(double v)            { this.gravityX            = v;                     return this; }
        public Builder gravityY(double v)            { this.gravityY            = v;                     return this; }
        public Builder electricFieldX(double v)      { this.electricFieldX      = v;                     return this; }
        public Builder electricFieldY(double v)      { this.electricFieldY      = v;                     return this; }
        public Builder magneticFieldZ(double v)      { this.magneticFieldZ      = v;                     return this; }
        public Builder ambientRadiation(double v)    { this.ambientRadiation    = Math.max(0.0, v);      return this; }
        public Builder illuminance(double v)         { this.illuminance         = Math.max(0.0, v);      return this; }

        /** Construye el EnvironmentState. */
        public EnvironmentState build() { return new EnvironmentState(this); }

        private static double clamp01(double v) {
            return Math.max(0.0, Math.min(1.0, v));
        }
    }
}
