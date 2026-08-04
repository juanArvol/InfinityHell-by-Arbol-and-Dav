package Game.Engine.Physics.Chemical;

import Game.Engine.Physics.Core.DomainState;

/**
 * Estado químico de un objeto dentro del simulador.
 *
 * ── HRFC-032 — Evolución del SimulationContext hacia un registro extensible ─
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * ChemicalState describe el estado químico instantáneo de un objeto.
 * Agrupa todas las propiedades relativas a su composición, reactividad y
 * transformaciones químicas posibles.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * ChemicalState solo describe. No reacciona. No transforma. No produce efectos.
 *
 *   ChemicalState  →  describe (qué estado químico tiene el objeto)
 *   Relation       →  interpreta (qué fenómenos produce ese estado)
 *
 * Las relaciones químicas (ChemicalRelation) son las responsables de
 * interpretar este estado y producir cambios en PhysicalState o en otros
 * dominios del SimulationContext.
 *
 * ── DOMINIO ───────────────────────────────────────────────────────────────
 *
 *   oxidationLevel      nivel de oxidación acumulada [0, 1].
 *                       0 = sin oxidación, 1 = completamente oxidado.
 *                       Afecta a propiedades mecánicas (dureza, resistencia)
 *                       y térmicas (conductividad). Fuente: ChemicalRelation.
 *
 *   combustibilityIndex susceptibilidad actual a la ignición [0, 1].
 *                       Puede diferir del MaterialState.combustibility base
 *                       si el objeto ha acumulado productos químicos inflamables.
 *
 *   pH                  nivel de acidez/alcalinidad [0, 14].
 *                       7 = neutro. < 7 = ácido. > 7 = alcalino.
 *                       Afecta a procesos de corrosión y reactividad.
 *
 *   corrosionLevel      nivel de corrosión acumulada [0, 1].
 *                       Degradación química de la superficie del objeto.
 *                       Las relaciones de corrosión lo incrementan en función
 *                       del pH, la humedad y el tiempo.
 *
 *   reactivity          capacidad de reaccionar con otras sustancias [0, 1].
 *                       0 = inerte, 1 = extremadamente reactivo.
 *                       Determina con qué intensidad participará en reacciones.
 *
 *   saturation          fracción de la capacidad química del objeto que ya
 *                       está ocupada por reacciones activas [0, 1].
 *                       Un objeto saturado (= 1) no puede absorber más
 *                       reactivos hasta que las reacciones previas terminen.
 *
 *   concentration       concentración de la sustancia activa principal [0, +∞).
 *                       En objetos homogéneos: densidad molar relativa.
 *                       En objetos contenedores: concentración del fluido.
 *
 * ── RELACIONES QUE CONSUMEN ESTE ESTADO ──────────────────────────────────
 *
 *   ChemicalRelation    — reacciones entre objetos en contacto
 *   CorrosionRelation   — degradación por humedad + pH + tiempo
 *   CombustionRelation  — ignición cuando combustibilityIndex + T > umbral
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * ChemicalState es completamente inmutable tras su construcción.
 * Las relaciones químicas producen un nuevo ChemicalState que el sistema
 * de simulación registra en el SimulationContext vía updateContext().
 *
 * ── ESTADO NEUTRO ─────────────────────────────────────────────────────────
 * ChemicalState.INERT representa un objeto químicamente inerte:
 * sin oxidación, sin corrosión, pH neutro, sin reactividad.
 * Es el valor por defecto para entidades sin dominio químico activo.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 */
public final class ChemicalState implements DomainState {

    // ── Propiedades de oxidación y corrosión ──────────────────────────────

    /**
     * Nivel de oxidación acumulada [0, 1].
     * 0 = sin oxidación, 1 = completamente oxidado.
     */
    private final double oxidationLevel;

    /**
     * Nivel de corrosión acumulada [0, 1].
     * 0 = sin corrosión, 1 = corrosión total (objeto degradado).
     */
    private final double corrosionLevel;

    // ── Propiedades ácido-base ────────────────────────────────────────────

    /**
     * Nivel de pH del objeto o del fluido que contiene [0, 14].
     * 7 = neutro. < 7 = ácido. > 7 = alcalino.
     */
    private final double pH;

    // ── Propiedades de reactividad y saturación ───────────────────────────

    /**
     * Capacidad de reaccionar con otras sustancias [0, 1].
     * 0 = inerte. 1 = extremadamente reactivo.
     */
    private final double reactivity;

    /**
     * Fracción de la capacidad química actualmente ocupada por reacciones [0, 1].
     * Un objeto saturado no puede absorber más reactivos.
     */
    private final double saturation;

    // ── Concentración de sustancia activa ─────────────────────────────────

    /**
     * Concentración de la sustancia activa principal [0, +∞).
     * En objetos homogéneos: densidad molar relativa.
     * En contenedores: concentración del fluido interno.
     */
    private final double concentration;

    // ── Combustibilidad dinámica ──────────────────────────────────────────

    /**
     * Susceptibilidad actual a la ignición [0, 1].
     * Puede diferir de MaterialState.combustibility si el objeto ha
     * acumulado productos inflamables o ha sido tratado químicamente.
     */
    private final double combustibilityIndex;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private ChemicalState(Builder b) {
        this.oxidationLevel     = b.oxidationLevel;
        this.corrosionLevel     = b.corrosionLevel;
        this.pH                 = b.pH;
        this.reactivity         = b.reactivity;
        this.saturation         = b.saturation;
        this.concentration      = b.concentration;
        this.combustibilityIndex = b.combustibilityIndex;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * Estado químico neutro.
     * Sin oxidación, sin corrosión, pH neutro (7), sin reactividad.
     * Valor por defecto para entidades sin dominio químico activo.
     */
    public static final ChemicalState INERT = builder().build();

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Nivel de oxidación acumulada [0, 1]. */
    public double getOxidationLevel()      { return oxidationLevel; }

    /** Nivel de corrosión acumulada [0, 1]. */
    public double getCorrosionLevel()      { return corrosionLevel; }

    /** Nivel de pH [0, 14]. 7 = neutro. */
    public double getPH()                  { return pH; }

    /** Capacidad de reaccionar con otras sustancias [0, 1]. */
    public double getReactivity()          { return reactivity; }

    /** Fracción de la capacidad química ocupada por reacciones activas [0, 1]. */
    public double getSaturation()          { return saturation; }

    /** Concentración de la sustancia activa principal [0, +∞). */
    public double getConcentration()       { return concentration; }

    /** Susceptibilidad actual a la ignición [0, 1]. */
    public double getCombustibilityIndex() { return combustibilityIndex; }

    // ── Helpers de conveniencia ───────────────────────────────────────────

    /** True si el objeto es ácido (pH < 6.5). */
    public boolean isAcidic()    { return pH < 6.5; }

    /** True si el objeto es alcalino (pH > 7.5). */
    public boolean isAlkaline()  { return pH > 7.5; }

    /** True si el objeto está en riesgo de ignición (combustibilityIndex > 0.5). */
    public boolean isFlammable() { return combustibilityIndex > 0.5; }

    /** True si el objeto está químicamente saturado (saturation >= 1.0). */
    public boolean isSaturated() { return saturation >= 1.0; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "ChemicalState[ox=%.2f cor=%.2f pH=%.1f react=%.2f sat=%.2f conc=%.2f]",
            oxidationLevel, corrosionLevel, pH, reactivity, saturation, concentration);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de ChemicalState.
     *
     * Valores por defecto (estado inerte):
     *   oxidationLevel      = 0.0    sin oxidación
     *   corrosionLevel      = 0.0    sin corrosión
     *   pH                  = 7.0    neutro
     *   reactivity          = 0.0    inerte
     *   saturation          = 0.0    sin reacciones activas
     *   concentration       = 0.0    sin sustancia activa
     *   combustibilityIndex = 0.0    no inflamable
     */
    public static final class Builder {

        private double oxidationLevel      = 0.0;
        private double corrosionLevel      = 0.0;
        private double pH                  = 7.0;
        private double reactivity          = 0.0;
        private double saturation          = 0.0;
        private double concentration       = 0.0;
        private double combustibilityIndex = 0.0;

        private Builder() {}

        public Builder oxidationLevel(double v)     { this.oxidationLevel      = clamp01(v);           return this; }
        public Builder corrosionLevel(double v)     { this.corrosionLevel      = clamp01(v);           return this; }
        public Builder pH(double v)                 { this.pH                  = Math.max(0.0, Math.min(14.0, v)); return this; }
        public Builder reactivity(double v)         { this.reactivity          = clamp01(v);           return this; }
        public Builder saturation(double v)         { this.saturation          = clamp01(v);           return this; }
        public Builder concentration(double v)      { this.concentration       = Math.max(0.0, v);     return this; }
        public Builder combustibilityIndex(double v){ this.combustibilityIndex = clamp01(v);           return this; }

        /** Construye el ChemicalState. */
        public ChemicalState build() { return new ChemicalState(this); }

        private static double clamp01(double v) {
            return Math.max(0.0, Math.min(1.0, v));
        }
    }
}
