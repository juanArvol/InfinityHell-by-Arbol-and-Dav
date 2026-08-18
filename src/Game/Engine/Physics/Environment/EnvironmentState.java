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
 *   Influencia gravitacional:
 *     gravityInfluenceX     factor multiplicador de gravedad en X (1.0 = sin modificación)
 *     gravityInfluenceY     factor multiplicador de gravedad en Y (1.0 = sin modificación)
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
 * ── FASE 2: ELIMINACIÓN DE DEFAULTS UNIVERSALES ─────────────────────────
 * EnvironmentState ya NO tiene constantes STANDARD/VACUUM públicas.
 * Las condiciones ambientales pertenecen a los Environment concretos:
 *   - StandardAtmosphere.INSTANCE.current() → condiciones terrestres
 *   - VacuumEnvironment.INSTANCE.current()  → condiciones de vacío
 *
 * La infraestructura NO decide cómo es el ambiente.
 * Cada Environment declara sus propias condiciones.
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

    // ── Influencia gravitacional ──────────────────────────────────────────

    /**
     * Factor de influencia gravitacional del ambiente en el eje X.
     * 
     * IMPORTANTE - HRFC-FASE2.5: CORRECCIÓN SEMÁNTICA DE OWNERSHIP
     * 
     * Este campo representa un FACTOR MULTIPLICADOR que el ambiente
     * aplica sobre la gravedad propia de las entidades, NO la gravedad base.
     * 
     * OWNERSHIP CORRECTO:
     * 
     *   ENTIDAD (Physics2DComponent / PhysicalState):
     *     gravity → propiedad gravitacional PROPIA de la entidad (ej: 9.8 m/s²)
     *     mass → masa inercial (GravityProperties.MASS)
     * 
     *   AMBIENTE (EnvironmentState):
     *     gravityInfluenceX, gravityInfluenceY → FACTOR de modificación ambiental
     *     Ejemplo: 1.0 = sin modificación, 0.5 = mitad, 2.0 = doble, 0.0 = anulada
     * 
     * COMPOSICIÓN CORRECTA:
     *   a_efectiva = entity.gravity × environment.gravityInfluenceY
     * 
     * EJEMPLOS:
     *   Entity.gravity = 9.8, Environment.gravityInfluence = 1.0
     *     → 9.8 × 1.0 = 9.8 (condiciones terrestres normales)
     * 
     *   Entity.gravity = 9.8, Environment.gravityInfluence = 0.5
     *     → 9.8 × 0.5 = 4.9 (zona de gravedad reducida)
     * 
     *   Entity.gravity = 9.8, Environment.gravityInfluence = 2.0
     *     → 9.8 × 2.0 = 19.6 (zona de gravedad intensificada)
     * 
     *   Entity.gravity = 9.8, Environment.gravityInfluence = 0.0
     *     → 9.8 × 0.0 = 0.0 (microgravedad / espacio)
     * 
     * PRINCIPIO FUNDAMENTAL:
     * El ambiente NO dice: "La gravedad aquí ES 19.6."
     * El ambiente dice: "La gravedad de la entidad se ve modificada por ×2."
     * 
     * La entidad es propietaria de su gravedad.
     * El ambiente solo la modifica mediante un factor de influencia.
     * Las Relations combinan ambos para calcular la aceleración efectiva.
     */
    private final double gravityInfluenceX;

    /**
     * Factor de influencia gravitacional del ambiente en el eje Y.
     * Positivo = modifica gravedad hacia abajo (convención AWT/juego).
     * 
     * IMPORTANTE - HRFC-FASE2.5: Factor multiplicador, NO magnitud absoluta.
     * 
     * Valores típicos:
     *   1.0  = StandardAtmosphere (sin modificación, gravedad terrestre normal)
     *   0.0  = VacuumEnvironment (anula gravedad → microgravedad)
     *   0.17 = MoonEnvironment (gravedad lunar, 1.62/9.8 ≈ 0.165)
     *   2.0  = HighGravityZone (duplica la gravedad de la entidad)
     * 
     * La aceleración efectiva resulta de:
     *   a_efectiva = entity.gravity × environment.gravityInfluenceY
     */
    private final double gravityInfluenceY;

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
        this.gravityInfluenceX   = b.gravityInfluenceX;
        this.gravityInfluenceY   = b.gravityInfluenceY;
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
     * 
     * @deprecated HRFC-FASE2: Usar {@link StandardAtmosphere#INSTANCE}.current()
     *             en lugar de esta constante estática. Las condiciones ambientales
     *             pertenecen al Environment concreto, no a la infraestructura.
     */
    @Deprecated
    public static EnvironmentState standard() {
        return StandardAtmosphere.INSTANCE.current();
    }

    /**
     * Condiciones de microgravedad / espacio exterior.
     * 
     * @deprecated HRFC-FASE2: Usar {@link VacuumEnvironment#INSTANCE}.current()
     *             en lugar de esta constante estática. Las condiciones ambientales
     *             pertenecen al Environment concreto, no a la infraestructura.
     */
    @Deprecated
    public static EnvironmentState vacuum() {
        return VacuumEnvironment.INSTANCE.current();
    }

    /**
     * @deprecated HRFC-FASE2: Usar {@link #standard()} o StandardAtmosphere.INSTANCE.current()
     */
    @Deprecated
    public static final EnvironmentState STANDARD = null; // Eliminado, usar standard()

    /**
     * @deprecated HRFC-FASE2: Usar {@link #vacuum()} o VacuumEnvironment.INSTANCE.current()
     */
    @Deprecated
    public static final EnvironmentState VACUUM = null; // Eliminado, usar vacuum()

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

    // ── Accesores — influencia gravitacional ──────────────────────────────

    /** 
     * Factor de influencia gravitacional del ambiente en el eje X.
     * 1.0 = sin modificación, 0.0 = anulada, 2.0 = duplicada.
     */
    public double getGravityInfluenceX()            { return gravityInfluenceX; }

    /** 
     * Factor de influencia gravitacional del ambiente en el eje Y.
     * 1.0 = sin modificación, 0.0 = anulada, 2.0 = duplicada.
     */
    public double getGravityInfluenceY()            { return gravityInfluenceY; }

    /** 
     * Módulo del factor de influencia gravitacional.
     * Útil para cálculos vectoriales de gravedad modificada.
     */
    public double getGravityInfluenceMagnitude() {
        return Math.sqrt(gravityInfluenceX * gravityInfluenceX + 
                        gravityInfluenceY * gravityInfluenceY);
    }
    
    // ── Accesores legacy (deprecated) ─────────────────────────────────────
    
    /**
     * @deprecated HRFC-FASE2.5: Usar {@link #getGravityInfluenceX()}.
     *             La semántica cambió de magnitud absoluta a factor multiplicador.
     */
    @Deprecated
    public double getGravityX() { return gravityInfluenceX; }
    
    /**
     * @deprecated HRFC-FASE2.5: Usar {@link #getGravityInfluenceY()}.
     *             La semántica cambió de magnitud absoluta a factor multiplicador.
     */
    @Deprecated
    public double getGravityY() { return gravityInfluenceY; }
    
    /**
     * @deprecated HRFC-FASE2.5: Usar {@link #getGravityInfluenceMagnitude()}.
     *             La semántica cambió de magnitud absoluta a factor multiplicador.
     */
    @Deprecated
    public double getGravityMagnitude() {
        return getGravityInfluenceMagnitude();
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
            "EnvironmentState[T=%.1f P=%.2f wind=(%.1f,%.1f) gInf=(%.2f,%.2f)]",
            ambientTemperature, atmosphericPressure,
            windX, windY, gravityInfluenceX, gravityInfluenceY);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de EnvironmentState.
     *
     * ── HRFC-FASE2 — Eliminación de defaults universales ──────────────────
     *
     * IMPORTANTE: Este Builder NO tiene valores por defecto.
     *
     * Los valores deben ser declarados explícitamente por el Environment
     * que construye el estado. La infraestructura NO impone condiciones
     * ambientales universales.
     *
     * ANTES (FASE 1):
     *   Builder con defaults hardcodeados (temp=0, pressure=1.0, gravity=9.8)
     *   → la infraestructura decidía cómo era el ambiente
     *
     * AHORA (FASE 2):
     *   Builder sin defaults → cada Environment declara sus condiciones
     *   → StandardAtmosphere declara temp=0, gravity=9.8
     *   → VacuumEnvironment declara temp=-270, gravity=0
     *   → CustomEnvironment declara lo que necesite
     *
     * Todos los valores se inicializan a 0.0 como base neutra matemática,
     * NO como "default ambiental". Un ambiente debe establecer explícitamente
     * cada propiedad relevante antes de construir el estado.
     */
    public static final class Builder {

        private double ambientTemperature  = 0.0;
        private double atmosphericPressure = 0.0;
        private double ambientHumidity     = 0.0;
        private double windX               = 0.0;
        private double windY               = 0.0;
        private double fluidDensity        = 0.0;
        private double fluidViscosity      = 0.0;
        private double gravityInfluenceX   = 0.0;
        private double gravityInfluenceY   = 0.0;
        private double electricFieldX      = 0.0;
        private double electricFieldY      = 0.0;
        private double magneticFieldZ      = 0.0;
        private double ambientRadiation    = 0.0;
        private double illuminance         = 0.0;

        private Builder() {}

        // ── Setters (fluent API) ──────────────────────────────────────────

        public Builder ambientTemperature(double v)  { this.ambientTemperature  = v;                     return this; }
        public Builder atmosphericPressure(double v) { this.atmosphericPressure = Math.max(0.0, v);      return this; }
        public Builder ambientHumidity(double v)     { this.ambientHumidity     = clamp01(v);            return this; }
        public Builder windX(double v)               { this.windX               = v;                     return this; }
        public Builder windY(double v)               { this.windY               = v;                     return this; }
        public Builder fluidDensity(double v)        { this.fluidDensity        = Math.max(0.0, v);      return this; }
        public Builder fluidViscosity(double v)      { this.fluidViscosity      = Math.max(0.0, v);      return this; }
        public Builder gravityInfluenceX(double v)   { this.gravityInfluenceX   = v;                     return this; }
        public Builder gravityInfluenceY(double v)   { this.gravityInfluenceY   = v;                     return this; }
        public Builder electricFieldX(double v)      { this.electricFieldX      = v;                     return this; }
        public Builder electricFieldY(double v)      { this.electricFieldY      = v;                     return this; }
        public Builder magneticFieldZ(double v)      { this.magneticFieldZ      = v;                     return this; }
        public Builder ambientRadiation(double v)    { this.ambientRadiation    = Math.max(0.0, v);      return this; }
        public Builder illuminance(double v)         { this.illuminance         = Math.max(0.0, v);      return this; }
        
        // ── Legacy setters (deprecated) ───────────────────────────────────
        
        /**
         * @deprecated HRFC-FASE2.5: Usar {@link #gravityInfluenceX(double)}.
         *             La semántica cambió de magnitud absoluta a factor multiplicador.
         */
        @Deprecated
        public Builder gravityX(double v) { return gravityInfluenceX(v); }
        
        /**
         * @deprecated HRFC-FASE2.5: Usar {@link #gravityInfluenceY(double)}.
         *             La semántica cambió de magnitud absoluta a factor multiplicador.
         */
        @Deprecated
        public Builder gravityY(double v) { return gravityInfluenceY(v); }

        // ── Getters (HRFC-FASE2.5 — compositional modification) ──────────

        /**
         * Retorna la temperatura actual del builder.
         * Usado por EnvironmentalContributors para composición aditiva.
         */
        public double getAmbientTemperature()  { return ambientTemperature; }

        /**
         * Retorna la presión atmosférica actual del builder.
         * Usado por EnvironmentalContributors para composición aditiva.
         */
        public double getAtmosphericPressure() { return atmosphericPressure; }

        /**
         * Retorna la humedad actual del builder.
         * Usado por EnvironmentalContributors para composición aditiva.
         */
        public double getAmbientHumidity()     { return ambientHumidity; }

        /**
         * Retorna el viento X actual del builder.
         * Usado por EnvironmentalContributors para composición vectorial.
         */
        public double getWindX()               { return windX; }

        /**
         * Retorna el viento Y actual del builder.
         * Usado por EnvironmentalContributors para composición vectorial.
         */
        public double getWindY()               { return windY; }

        /**
         * Retorna la densidad del fluido actual del builder.
         * Usado por EnvironmentalContributors para composición aditiva.
         */
        public double getFluidDensity()        { return fluidDensity; }

        /**
         * Retorna la viscosidad del fluido actual del builder.
         * Usado por EnvironmentalContributors para composición aditiva.
         */
        public double getFluidViscosity()      { return fluidViscosity; }

        /**
         * Retorna el factor de influencia gravitacional X actual del builder.
         * Usado por EnvironmentalContributors para composición multiplicativa.
         */
        public double getGravityInfluenceX()   { return gravityInfluenceX; }

        /**
         * Retorna el factor de influencia gravitacional Y actual del builder.
         * Usado por EnvironmentalContributors para composición multiplicativa.
         */
        public double getGravityInfluenceY()   { return gravityInfluenceY; }
        
        // ── Legacy getters (deprecated) ───────────────────────────────────
        
        /**
         * @deprecated HRFC-FASE2.5: Usar {@link #getGravityInfluenceX()}.
         */
        @Deprecated
        public double getGravityX() { return gravityInfluenceX; }
        
        /**
         * @deprecated HRFC-FASE2.5: Usar {@link #getGravityInfluenceY()}.
         */
        @Deprecated
        public double getGravityY() { return gravityInfluenceY; }

        /**
         * Retorna el campo eléctrico X actual del builder.
         * Usado por EnvironmentalContributors para composición vectorial.
         */
        public double getElectricFieldX()      { return electricFieldX; }

        /**
         * Retorna el campo eléctrico Y actual del builder.
         * Usado por EnvironmentalContributors para composición vectorial.
         */
        public double getElectricFieldY()      { return electricFieldY; }

        /**
         * Retorna el campo magnético Z actual del builder.
         * Usado por EnvironmentalContributors para composición aditiva.
         */
        public double getMagneticFieldZ()      { return magneticFieldZ; }

        /**
         * Retorna la radiación ambiental actual del builder.
         * Usado por EnvironmentalContributors para composición aditiva.
         */
        public double getAmbientRadiation()    { return ambientRadiation; }

        /**
         * Retorna la iluminación actual del builder.
         * Usado por EnvironmentalContributors para composición aditiva.
         */
        public double getIlluminance()         { return illuminance; }

        // ── Build ─────────────────────────────────────────────────────────

        /** Construye el EnvironmentState. */
        public EnvironmentState build() { return new EnvironmentState(this); }

        private static double clamp01(double v) {
            return Math.max(0.0, Math.min(1.0, v));
        }
    }
}
