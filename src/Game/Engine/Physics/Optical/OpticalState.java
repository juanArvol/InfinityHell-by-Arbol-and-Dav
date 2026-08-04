package Game.Engine.Physics.Optical;

import Game.Engine.Physics.Core.DomainState;

/**
 * Estado óptico de un objeto dentro del simulador.
 *
 * ── HRFC-032 — Evolución del SimulationContext hacia un registro extensible ─
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * OpticalState describe las propiedades ópticas físicas de un objeto.
 * No representa información de renderizado — representa cómo el objeto
 * interactúa con la luz desde el punto de vista de la física óptica.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * OpticalState solo describe. No traza rayos. No calcula iluminación.
 *
 *   OpticalState  →  describe (cómo se comporta el objeto ante la luz)
 *   Relation      →  interpreta (qué fenómenos ópticos produce)
 *
 * La distinción entre óptica física y renderizado es deliberada:
 *   - OpticalState: transparencia como propiedad física (afecta a absorción
 *     de radiación, temperatura, interacción con otros materiales).
 *   - Renderizado: usa una representación visual propia, no este estado.
 *
 * ── DOMINIO ───────────────────────────────────────────────────────────────
 *
 *   transparency        fracción de luz que atraviesa el objeto [0, 1].
 *                       0 = completamente opaco. 1 = completamente transparente.
 *                       Afecta a la absorción de radiación solar/ambiental.
 *
 *   reflectivity        fracción de luz incidente reflejada [0, 1].
 *                       0 = sin reflexión. 1 = espejo perfecto.
 *                       Afecta a la temperatura: objetos muy reflectantes
 *                       absorben menos radiación.
 *
 *   refractiveIndex     índice de refracción del material [1, +∞).
 *                       1.0 = vacío (sin refracción). Agua ≈ 1.33.
 *                       Vidrio ≈ 1.5. Diamante ≈ 2.42.
 *
 *   lightAbsorption     fracción de luz absorbida como energía [0, 1].
 *                       Relacionado con absorptivity de MaterialState, pero
 *                       específico del espectro visible. Afecta a calentamiento.
 *
 *   scattering          nivel de dispersión de luz en el interior del objeto [0, 1].
 *                       0 = sin dispersión (claro). 1 = dispersión máxima (opaco lechoso).
 *                       Materiales como leche, niebla o polvo tienen scattering alto.
 *
 *   emission            emisión propia de luz [0, +∞).
 *                       0 = no luminoso. > 0 = objeto luminoso (bioluminiscencia,
 *                       incandescencia, fosforescencia).
 *                       Las relaciones ópticas pueden incrementarlo en función
 *                       de la temperatura (cuerpo negro).
 *
 *   fluorescence        nivel de fluorescencia [0, 1].
 *                       Capacidad de absorber radiación UV y emitirla como luz visible.
 *
 * ── RELACIONES QUE CONSUMEN ESTE ESTADO ──────────────────────────────────
 *
 *   OpticalRelation     — interacción luminosa entre objetos y entorno
 *   RadiationRelation   — ya existente: puede consumir transparency y reflectivity
 *   ThermalRelation     — lightAbsorption afecta al calentamiento por radiación solar
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * OpticalState es completamente inmutable tras su construcción.
 *
 * ── ESTADO NEUTRO ─────────────────────────────────────────────────────────
 * OpticalState.OPAQUE representa un objeto físicamente opaco, no reflectante
 * y sin emisión propia. Es el valor por defecto para la mayoría de objetos.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 */
public final class OpticalState implements DomainState {

    // ── Interacción con la luz incidente ──────────────────────────────────

    /** Fracción de luz que atraviesa el objeto [0, 1]. 0=opaco, 1=transparente. */
    private final double transparency;

    /** Fracción de luz reflejada [0, 1]. 0=sin reflexión, 1=espejo perfecto. */
    private final double reflectivity;

    /** Fracción de luz absorbida como energía [0, 1]. */
    private final double lightAbsorption;

    // ── Propagación interna ───────────────────────────────────────────────

    /** Índice de refracción [1, +∞). 1.0 = vacío. */
    private final double refractiveIndex;

    /** Dispersión interna de la luz [0, 1]. 0=claro, 1=completamente disperso. */
    private final double scattering;

    // ── Emisión propia ────────────────────────────────────────────────────

    /** Emisión luminosa propia [0, +∞). 0=no luminoso. */
    private final double emission;

    /** Nivel de fluorescencia [0, 1]. Absorción UV → emisión visible. */
    private final double fluorescence;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private OpticalState(Builder b) {
        this.transparency    = b.transparency;
        this.reflectivity    = b.reflectivity;
        this.lightAbsorption = b.lightAbsorption;
        this.refractiveIndex = b.refractiveIndex;
        this.scattering      = b.scattering;
        this.emission        = b.emission;
        this.fluorescence    = b.fluorescence;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * Objeto opaco estándar.
     * Sin transparencia, sin reflexión especular, sin emisión propia.
     * Valor por defecto para la mayoría de objetos sólidos.
     */
    public static final OpticalState OPAQUE = builder().build();

    /**
     * Objeto transparente estándar (similar al vidrio).
     * Alta transparencia, índice de refracción 1.5, absorción baja.
     */
    public static final OpticalState GLASS = builder()
        .transparency(0.9)
        .reflectivity(0.05)
        .refractiveIndex(1.5)
        .lightAbsorption(0.05)
        .scattering(0.01)
        .build();

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Fracción de luz que atraviesa el objeto [0, 1]. */
    public double getTransparency()    { return transparency; }

    /** Fracción de luz reflejada [0, 1]. */
    public double getReflectivity()    { return reflectivity; }

    /** Fracción de luz absorbida como energía [0, 1]. */
    public double getLightAbsorption() { return lightAbsorption; }

    /** Índice de refracción [1, +∞). */
    public double getRefractiveIndex() { return refractiveIndex; }

    /** Dispersión interna de la luz [0, 1]. */
    public double getScattering()      { return scattering; }

    /** Emisión luminosa propia [0, +∞). */
    public double getEmission()        { return emission; }

    /** Nivel de fluorescencia [0, 1]. */
    public double getFluorescence()    { return fluorescence; }

    // ── Helpers de conveniencia ───────────────────────────────────────────

    /** True si el objeto emite luz propia (emission > 0). */
    public boolean isLuminous()      { return emission > 0.0; }

    /** True si el objeto permite el paso de luz (transparency > 0.1). */
    public boolean isTransparent()   { return transparency > 0.1; }

    /** True si el objeto es un buen reflector (reflectivity > 0.7). */
    public boolean isReflective()    { return reflectivity > 0.7; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "OpticalState[transp=%.2f refl=%.2f n=%.2f abs=%.2f emit=%.2f]",
            transparency, reflectivity, refractiveIndex, lightAbsorption, emission);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de OpticalState.
     *
     * Valores por defecto (objeto opaco estándar):
     *   transparency    = 0.0    completamente opaco
     *   reflectivity    = 0.0    sin reflexión especular
     *   lightAbsorption = 0.5    absorción media
     *   refractiveIndex = 1.0    sin refracción (vacío/sólido opaco)
     *   scattering      = 0.0    sin dispersión interna
     *   emission        = 0.0    sin emisión propia
     *   fluorescence    = 0.0    sin fluorescencia
     */
    public static final class Builder {

        private double transparency    = 0.0;
        private double reflectivity    = 0.0;
        private double lightAbsorption = 0.5;
        private double refractiveIndex = 1.0;
        private double scattering      = 0.0;
        private double emission        = 0.0;
        private double fluorescence    = 0.0;

        private Builder() {}

        public Builder transparency(double v)    { this.transparency    = clamp01(v);            return this; }
        public Builder reflectivity(double v)    { this.reflectivity    = clamp01(v);            return this; }
        public Builder lightAbsorption(double v) { this.lightAbsorption = clamp01(v);            return this; }
        public Builder refractiveIndex(double v) { this.refractiveIndex = Math.max(1.0, v);      return this; }
        public Builder scattering(double v)      { this.scattering      = clamp01(v);            return this; }
        public Builder emission(double v)        { this.emission        = Math.max(0.0, v);      return this; }
        public Builder fluorescence(double v)    { this.fluorescence    = clamp01(v);            return this; }

        /** Construye el OpticalState. */
        public OpticalState build() { return new OpticalState(this); }

        private static double clamp01(double v) {
            return Math.max(0.0, Math.min(1.0, v));
        }
    }
}
