package Game.Engine.World.Physics.Nuclear;

import Game.Engine.World.Physics.Core.DomainState;

/**
 * Estado nuclear de un objeto dentro del simulador.
 *
 * ── HRFC-032 — Evolución del SimulationContext hacia un registro extensible ─
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * NuclearState describe las propiedades nucleares de un objeto.
 * Representa la estabilidad atómica, la emisión de partículas y la
 * energía asociada a los procesos de desintegración nuclear.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * NuclearState solo describe. No calcula decaimiento. No emite partículas.
 *
 *   NuclearState  →  describe (estado nuclear del objeto)
 *   Relation      →  interpreta (qué fenómenos produce ese estado nuclear)
 *
 * Ejemplos de fenómenos que las relaciones pueden generar:
 *   - Un objeto con radioactivity alta en contacto con otro → irradiación del segundo.
 *   - decayRate > 0 → reducción gradual de nuclearEnergy por frame.
 *   - particleEmission alta → incremento de temperatura en el entorno (EnvironmentState).
 *
 * ── DOMINIO ───────────────────────────────────────────────────────────────
 *
 *   radioactivity       nivel de actividad radiactiva [0, 1].
 *                       0 = no radiactivo (elemento estable). 1 = altamente radiactivo.
 *                       Las relaciones usan este nivel para calcular la dosis
 *                       absorbida por objetos cercanos y la irradiación del entorno.
 *
 *   decayRate           velocidad de desintegración nuclear por segundo [0, 1].
 *                       Fracción de la energía nuclear que se emite por unidad de tiempo.
 *                       0 = elemento estable (vida media infinita).
 *                       1 = desintegración instantánea.
 *
 *   nuclearEnergy       reserva de energía nuclear disponible [0, +∞).
 *                       Disminuye con el tiempo a una velocidad proporcional a decayRate.
 *                       Cuando llega a 0, el objeto es considerado estable.
 *
 *   isotopeStability    estabilidad isotópica del núcleo atómico [0, 1].
 *                       0 = isótopo completamente inestable (proto-nuclear).
 *                       1 = isótopo completamente estable (no decae espontáneamente).
 *                       Determina la susceptibilidad a la fisión inducida.
 *
 *   particleEmission    intensidad de emisión de partículas (alfa, beta, neutrones)
 *                       [0, +∞). 0 = sin emisión. > 0 = emitiendo partículas.
 *                       Las relaciones pueden usar este valor para calcular
 *                       daños en entidades vivas (BiomechanicalState) o
 *                       calentamiento en materiales próximos.
 *
 *   criticalityIndex    proximidad al estado crítico [0, 1].
 *                       0 = subcrítico (sin reacción en cadena posible).
 *                       1 = crítico (reacción en cadena activa).
 *                       Las relaciones verifican este índice para determinar
 *                       si puede iniciarse una reacción en cadena.
 *
 * ── RELACIONES QUE CONSUMEN ESTE ESTADO ──────────────────────────────────
 *
 *   NuclearDecayRelation     — reduce nuclearEnergy y particleEmission con el tiempo
 *   RadiationExposureRelation— irradia objetos cercanos (EnvironmentState + NuclearState)
 *   NuclearFissionRelation   — cuando criticalityIndex ≈ 1 y las condiciones se cumplen
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * NuclearState es completamente inmutable tras su construcción.
 *
 * ── ESTADO NEUTRO ─────────────────────────────────────────────────────────
 * NuclearState.STABLE representa un objeto completamente estable desde el
 * punto de vista nuclear: sin radiactividad, sin energía nuclear, sin emisión.
 * Es el valor por defecto para la mayoría de objetos del simulador.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 */
public final class NuclearState implements DomainState {

    // ── Propiedades de radiactividad ──────────────────────────────────────

    /**
     * Nivel de actividad radiactiva [0, 1].
     * 0 = no radiactivo. 1 = altamente radiactivo.
     */
    private final double radioactivity;

    /**
     * Velocidad de desintegración nuclear por segundo [0, 1].
     * 0 = elemento estable. 1 = desintegración instantánea.
     */
    private final double decayRate;

    // ── Energía y estabilidad nuclear ─────────────────────────────────────

    /**
     * Reserva de energía nuclear disponible [0, +∞).
     * Se reduce con el tiempo a una velocidad proporcional a decayRate.
     */
    private final double nuclearEnergy;

    /**
     * Estabilidad isotópica del núcleo atómico [0, 1].
     * 0 = isótopo inestable. 1 = isótopo completamente estable.
     */
    private final double isotopeStability;

    // ── Emisión de partículas ─────────────────────────────────────────────

    /**
     * Intensidad de emisión de partículas (alfa, beta, neutrones) [0, +∞).
     * 0 = sin emisión de partículas.
     */
    private final double particleEmission;

    // ── Criticidad ────────────────────────────────────────────────────────

    /**
     * Proximidad al estado crítico [0, 1].
     * 0 = subcrítico. 1 = crítico (reacción en cadena activa).
     */
    private final double criticalityIndex;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private NuclearState(Builder b) {
        this.radioactivity    = b.radioactivity;
        this.decayRate        = b.decayRate;
        this.nuclearEnergy    = b.nuclearEnergy;
        this.isotopeStability = b.isotopeStability;
        this.particleEmission = b.particleEmission;
        this.criticalityIndex = b.criticalityIndex;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * Objeto completamente estable desde el punto de vista nuclear.
     * Sin radiactividad, sin energía nuclear, sin emisión de partículas.
     * Valor por defecto para la inmensa mayoría de objetos del simulador.
     */
    public static final NuclearState STABLE = builder().build();

    /**
     * Material débilmente radiactivo (referencia: uranio empobrecido).
     * Radiactividad baja, decaimiento muy lento, sin criticidad.
     */
    public static final NuclearState MILDLY_RADIOACTIVE = builder()
        .radioactivity(0.1)
        .decayRate(1e-6)
        .nuclearEnergy(500.0)
        .isotopeStability(0.7)
        .particleEmission(0.05)
        .build();

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Nivel de actividad radiactiva [0, 1]. */
    public double getRadioactivity()    { return radioactivity; }

    /** Velocidad de desintegración nuclear por segundo [0, 1]. */
    public double getDecayRate()        { return decayRate; }

    /** Reserva de energía nuclear disponible [0, +∞). */
    public double getNuclearEnergy()    { return nuclearEnergy; }

    /** Estabilidad isotópica del núcleo [0, 1]. */
    public double getIsotopeStability() { return isotopeStability; }

    /** Intensidad de emisión de partículas [0, +∞). */
    public double getParticleEmission() { return particleEmission; }

    /** Proximidad al estado crítico [0, 1]. */
    public double getCriticalityIndex() { return criticalityIndex; }

    // ── Helpers de conveniencia ───────────────────────────────────────────

    /** True si el objeto es radiactivo (radioactivity > 0.01). */
    public boolean isRadioactive()     { return radioactivity > 0.01; }

    /** True si el objeto tiene energía nuclear restante (nuclearEnergy > 0). */
    public boolean hasNuclearEnergy()  { return nuclearEnergy > 0.0; }

    /** True si el objeto está cerca del estado crítico (criticalityIndex > 0.9). */
    public boolean isNearlyCritical()  { return criticalityIndex > 0.9; }

    /** True si el objeto emite partículas activamente (particleEmission > 0). */
    public boolean emitsParticles()    { return particleEmission > 0.0; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "NuclearState[radio=%.3f decay=%.6f energy=%.1f stability=%.2f crit=%.2f]",
            radioactivity, decayRate, nuclearEnergy, isotopeStability, criticalityIndex);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de NuclearState.
     *
     * Valores por defecto (objeto completamente estable):
     *   radioactivity    = 0.0    no radiactivo
     *   decayRate        = 0.0    elemento estable (sin desintegración)
     *   nuclearEnergy    = 0.0    sin energía nuclear disponible
     *   isotopeStability = 1.0    isótopo completamente estable
     *   particleEmission = 0.0    sin emisión de partículas
     *   criticalityIndex = 0.0    subcrítico
     */
    public static final class Builder {

        private double radioactivity    = 0.0;
        private double decayRate        = 0.0;
        private double nuclearEnergy    = 0.0;
        private double isotopeStability = 1.0;
        private double particleEmission = 0.0;
        private double criticalityIndex = 0.0;

        private Builder() {}

        public Builder radioactivity(double v)    { this.radioactivity    = clamp01(v);           return this; }
        public Builder decayRate(double v)        { this.decayRate        = clamp01(v);           return this; }
        public Builder nuclearEnergy(double v)    { this.nuclearEnergy    = Math.max(0.0, v);     return this; }
        public Builder isotopeStability(double v) { this.isotopeStability = clamp01(v);           return this; }
        public Builder particleEmission(double v) { this.particleEmission = Math.max(0.0, v);     return this; }
        public Builder criticalityIndex(double v) { this.criticalityIndex = clamp01(v);           return this; }

        /** Construye el NuclearState. */
        public NuclearState build() { return new NuclearState(this); }

        private static double clamp01(double v) {
            return Math.max(0.0, Math.min(1.0, v));
        }
    }
}
