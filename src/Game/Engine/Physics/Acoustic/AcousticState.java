package Game.Engine.Physics.Acoustic;

import Game.Engine.Physics.Core.DomainState;

/**
 * Estado acústico de un objeto dentro del simulador.
 *
 * ── HRFC-032 — Evolución del SimulationContext hacia un registro extensible ─
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * AcousticState describe las propiedades acústicas físicas de un objeto.
 * Representa cómo el objeto interactúa con las ondas sonoras y mecánicas,
 * y qué características vibracionales posee por su composición.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * AcousticState solo describe. No genera sonido. No propaga ondas.
 *
 *   AcousticState  →  describe (propiedades acústicas del objeto)
 *   Relation       →  interpreta (qué fenómenos acústicos produce un evento)
 *
 * Ejemplos de fenómenos que las relaciones pueden generar a partir de este estado:
 *   - Un impacto sobre un objeto con resonantFrequency alta → sonido de campana.
 *   - Contacto prolongado entre objetos con dampingFactor bajo → vibración persistente.
 *   - Objeto en fluido con alta soundAbsorption → silencia las ondas circundantes.
 *
 * ── DOMINIO ───────────────────────────────────────────────────────────────
 *
 *   resonantFrequency   frecuencia de resonancia natural del objeto en Hz.
 *                       La frecuencia a la que el objeto vibra con mayor amplitud
 *                       ante excitaciones externas. 0 = sin resonancia definida.
 *
 *   soundAbsorption     fracción de energía sonora absorbida [0, 1].
 *                       0 = reflexión total (metal pulido). 1 = absorción total
 *                       (material acústicamente anecóico).
 *                       Afecta a la propagación de ondas de choque en el entorno.
 *
 *   naturalFrequency    frecuencia propia de vibración mecánica en Hz.
 *                       Puede diferir de resonantFrequency: la frecuencia natural
 *                       es inherente a la geometría y el material, mientras que
 *                       la resonante es la de mayor amplificación.
 *
 *   dampingFactor       fracción de energía disipada por ciclo de vibración [0, 1].
 *                       0 = vibración perpetua (ideal). 1 = amortiguación crítica
 *                       (sin oscilación, vuelta al equilibrio sin rebote).
 *                       Los materiales con dampingFactor alto absorben vibraciones
 *                       rápidamente (caucho, espuma).
 *
 *   propagationFactor   eficiencia de transmisión de ondas mecánicas a través del
 *                       material [0, 1]. 0 = aislante acústico total. 1 = conductor
 *                       acústico perfecto (acero, aluminio).
 *                       Las relaciones usan este factor para calcular si una vibración
 *                       se transmite al objeto siguiente.
 *
 *   currentAmplitude    amplitud de la vibración actual en el objeto [0, +∞).
 *                       Actualizada por las relaciones acústicas cuando el objeto
 *                       recibe un impacto o es excitado.
 *                       0 = en reposo. > 0 = vibrando.
 *
 * ── RELACIONES QUE CONSUMEN ESTE ESTADO ──────────────────────────────────
 *
 *   AcousticRelation    — propagación de vibraciones entre objetos en contacto
 *   ImpactSoundRelation — generación de sonido por colisión (ContactState + AcousticState)
 *   ResonanceRelation   — amplificación cuando la frecuencia de excitación ≈ resonantFrequency
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * AcousticState es completamente inmutable tras su construcción.
 * Las relaciones que generan vibración producen un nuevo AcousticState con
 * la currentAmplitude actualizada.
 *
 * ── ESTADO NEUTRO ─────────────────────────────────────────────────────────
 * AcousticState.SILENT representa un objeto en reposo acústico: sin vibración
 * activa, absorción media y sin resonancia definida.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 */
public final class AcousticState implements DomainState {

    // ── Propiedades de resonancia y frecuencia ────────────────────────────

    /**
     * Frecuencia de resonancia natural en Hz.
     * La frecuencia a la que el objeto vibra con mayor amplitud.
     * 0 = sin frecuencia de resonancia definida.
     */
    private final double resonantFrequency;

    /**
     * Frecuencia propia de vibración mecánica en Hz.
     * Determinada por la geometría y el material del objeto.
     */
    private final double naturalFrequency;

    // ── Propiedades de interacción con ondas ──────────────────────────────

    /**
     * Fracción de energía sonora absorbida [0, 1].
     * 0 = reflexión total. 1 = absorción total.
     */
    private final double soundAbsorption;

    /**
     * Eficiencia de transmisión de ondas mecánicas [0, 1].
     * 0 = aislante acústico. 1 = conductor acústico perfecto.
     */
    private final double propagationFactor;

    // ── Propiedades de amortiguación ──────────────────────────────────────

    /**
     * Fracción de energía disipada por ciclo de vibración [0, 1].
     * 0 = vibración perpetua. 1 = amortiguación crítica.
     */
    private final double dampingFactor;

    // ── Estado vibracional actual ─────────────────────────────────────────

    /**
     * Amplitud de la vibración actual [0, +∞).
     * 0 = en reposo. > 0 = vibrando activamente.
     * Actualizada por las relaciones acústicas al producirse excitaciones.
     */
    private final double currentAmplitude;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private AcousticState(Builder b) {
        this.resonantFrequency = b.resonantFrequency;
        this.naturalFrequency  = b.naturalFrequency;
        this.soundAbsorption   = b.soundAbsorption;
        this.propagationFactor = b.propagationFactor;
        this.dampingFactor     = b.dampingFactor;
        this.currentAmplitude  = b.currentAmplitude;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * Objeto en reposo acústico.
     * Sin vibración activa, absorción media, sin frecuencia de resonancia definida.
     * Valor por defecto para objetos sin dominio acústico específico.
     */
    public static final AcousticState SILENT = builder().build();

    /**
     * Metal rígido (acero / aluminio).
     * Alta frecuencia natural, baja absorción, alta propagación, bajo amortiguamiento.
     */
    public static final AcousticState RIGID_METAL = builder()
        .naturalFrequency(440.0)
        .resonantFrequency(440.0)
        .soundAbsorption(0.02)
        .propagationFactor(0.95)
        .dampingFactor(0.02)
        .build();

    /**
     * Material absorbente (espuma, caucho blando).
     * Alta absorción, alta amortiguación, baja propagación.
     */
    public static final AcousticState ABSORPTIVE = builder()
        .soundAbsorption(0.9)
        .propagationFactor(0.1)
        .dampingFactor(0.85)
        .build();

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Frecuencia de resonancia natural en Hz. */
    public double getResonantFrequency() { return resonantFrequency; }

    /** Frecuencia propia de vibración mecánica en Hz. */
    public double getNaturalFrequency()  { return naturalFrequency; }

    /** Fracción de energía sonora absorbida [0, 1]. */
    public double getSoundAbsorption()   { return soundAbsorption; }

    /** Eficiencia de transmisión de ondas mecánicas [0, 1]. */
    public double getPropagationFactor() { return propagationFactor; }

    /** Fracción de energía disipada por ciclo [0, 1]. */
    public double getDampingFactor()     { return dampingFactor; }

    /** Amplitud de vibración actual [0, +∞). */
    public double getCurrentAmplitude()  { return currentAmplitude; }

    // ── Helpers de conveniencia ───────────────────────────────────────────

    /** True si el objeto está vibrando activamente (currentAmplitude > umbral). */
    public boolean isVibrating()         { return currentAmplitude > 1e-6; }

    /** True si el objeto es buen conductor acústico (propagationFactor > 0.7). */
    public boolean isConductor()         { return propagationFactor > 0.7; }

    /** True si el objeto es un buen absorbente acústico (soundAbsorption > 0.7). */
    public boolean isAbsorptive()        { return soundAbsorption > 0.7; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "AcousticState[freq=%.1fHz abs=%.2f prop=%.2f damp=%.2f amp=%.4f]",
            resonantFrequency, soundAbsorption, propagationFactor,
            dampingFactor, currentAmplitude);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de AcousticState.
     *
     * Valores por defecto (objeto en reposo acústico):
     *   resonantFrequency = 0.0     sin frecuencia de resonancia definida
     *   naturalFrequency  = 0.0     sin frecuencia natural definida
     *   soundAbsorption   = 0.3     absorción media-baja
     *   propagationFactor = 0.5     transmisión media
     *   dampingFactor     = 0.3     amortiguación moderada
     *   currentAmplitude  = 0.0     en reposo
     */
    public static final class Builder {

        private double resonantFrequency = 0.0;
        private double naturalFrequency  = 0.0;
        private double soundAbsorption   = 0.3;
        private double propagationFactor = 0.5;
        private double dampingFactor     = 0.3;
        private double currentAmplitude  = 0.0;

        private Builder() {}

        public Builder resonantFrequency(double v) { this.resonantFrequency = Math.max(0.0, v);  return this; }
        public Builder naturalFrequency(double v)  { this.naturalFrequency  = Math.max(0.0, v);  return this; }
        public Builder soundAbsorption(double v)   { this.soundAbsorption   = clamp01(v);        return this; }
        public Builder propagationFactor(double v) { this.propagationFactor = clamp01(v);        return this; }
        public Builder dampingFactor(double v)     { this.dampingFactor     = clamp01(v);        return this; }
        public Builder currentAmplitude(double v)  { this.currentAmplitude  = Math.max(0.0, v);  return this; }

        /** Construye el AcousticState. */
        public AcousticState build() { return new AcousticState(this); }

        private static double clamp01(double v) {
            return Math.max(0.0, Math.min(1.0, v));
        }
    }
}
