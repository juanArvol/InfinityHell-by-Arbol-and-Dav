package Game.Engine.Physics.Biomechanical;

import Game.Engine.Physics.Core.DomainState;

/**
 * Estado biomecánico de una entidad viva dentro del simulador.
 *
 * ── HRFC-032 — Evolución del SimulationContext hacia un registro extensible ─
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * BiomechanicalState describe el estado biomecánico de entidades vivas.
 * Agrupa propiedades relativas a la resistencia estructural del cuerpo vivo,
 * su fatiga, tensión muscular y capacidad de respuesta física.
 *
 * Este dominio no pertenece al dominio físico clásico (PhysicalState)
 * ni al material inerte (MaterialState). Es específico de entidades con
 * estructura biológica o biomecánica activa.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * BiomechanicalState solo describe. No mueve al personaje. No aplica daño.
 *
 *   BiomechanicalState  →  describe (estado físico del organismo vivo)
 *   Relation            →  interpreta (qué consecuencias mecánicas produce)
 *
 * Ejemplos de fenómenos que las relaciones pueden generar:
 *   - fatigue alta + movimiento sostenido → reducción de aceleración disponible.
 *   - injuryLevel alto + impacto → ruptura de structuralIntegrity.
 *   - muscleTension alta + reposo → reducción gradual de fatigue.
 *   - structuralIntegrity = 0 → la entidad ya no puede mantener cohesión.
 *
 * ── DOMINIO ───────────────────────────────────────────────────────────────
 *
 *   fatigue             nivel de fatiga acumulada [0, 1].
 *                       0 = sin fatiga. 1 = agotamiento total.
 *                       Las relaciones biomecánicas pueden reducir la
 *                       aceleración máxima o la velocidad sostenible en
 *                       función de este nivel.
 *
 *   muscleTension       tensión muscular actual [0, 1].
 *                       0 = relajado. 1 = tensión máxima.
 *                       Alta tensión sostenida incrementa fatigue.
 *                       Tensión repentina reduce el tiempo de reacción.
 *
 *   structuralIntegrity integridad estructural del cuerpo vivo [0, 1].
 *                       1 = estructura completamente intacta.
 *                       0 = colapso estructural.
 *                       No es equivalente a "puntos de vida": es la capacidad
 *                       del cuerpo de mantener su forma y resistir fuerzas.
 *
 *   stiffness           rigidez biomecánica del cuerpo [0, 1].
 *                       0 = sin rigidez (floppy). 1 = rigidez máxima.
 *                       Afecta a la respuesta ante impactos y colisiones.
 *
 *   flexibility         flexibilidad activa del cuerpo [0, 1].
 *                       0 = sin flexibilidad. 1 = máxima flexibilidad.
 *                       Determina el rango de movimiento disponible.
 *
 *   injuryLevel         nivel de lesiones biomecánicas acumuladas [0, 1].
 *                       0 = sin lesiones. 1 = lesiones máximas (incapacitante).
 *                       Las relaciones de impacto incrementan este nivel.
 *                       La recuperación biomecánica lo reduce con el tiempo.
 *
 *   metabolicRate       tasa metabólica activa [0, +∞).
 *                       0 = metabolismo nulo (criogénico, muerto).
 *                       1 = metabolismo estándar en reposo.
 *                       > 1 = metabolismo activo (esfuerzo, estrés).
 *                       Las relaciones pueden usarlo para calcular generación
 *                       de calor corporal (interacción con PhysicalState).
 *
 * ── RELACIONES QUE CONSUMEN ESTE ESTADO ──────────────────────────────────
 *
 *   BiomechanicalRelation — fatiga, tensión, integridad por esfuerzo y tiempo
 *   ImpactInjuryRelation  — lesiones por impacto (ContactState + BiomechanicalState)
 *   MetabolicHeatRelation — calor metabólico (BiomechanicalState → PhysicalState.TEMPERATURE)
 *   RecoveryRelation      — recuperación gradual de injuryLevel y fatigue en reposo
 *
 * ── DISTINCIÓN CON EL SISTEMA DE DAÑO DE GAMEPLAY ────────────────────────
 * BiomechanicalState describe propiedades físicas del cuerpo vivo.
 * El sistema de gameplay (daño, vida, muerte) observa el resultado de las
 * relaciones biomecánicas para decidir consecuencias en el juego.
 * Esta clase nunca contiene lógica de gameplay.
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * BiomechanicalState es completamente inmutable tras su construcción.
 *
 * ── ESTADO NEUTRO ─────────────────────────────────────────────────────────
 * BiomechanicalState.RESTED representa una entidad viva en reposo óptimo:
 * sin fatiga, sin lesiones, con plena integridad estructural y flexibilidad.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 */
public final class BiomechanicalState implements DomainState {

    // ── Propiedades de esfuerzo ───────────────────────────────────────────

    /**
     * Nivel de fatiga acumulada [0, 1].
     * 0 = sin fatiga. 1 = agotamiento total.
     */
    private final double fatigue;

    /**
     * Tensión muscular actual [0, 1].
     * 0 = completamente relajado. 1 = tensión máxima.
     */
    private final double muscleTension;

    // ── Propiedades estructurales ─────────────────────────────────────────

    /**
     * Integridad estructural del cuerpo vivo [0, 1].
     * 1 = completamente intacto. 0 = colapso estructural.
     */
    private final double structuralIntegrity;

    /**
     * Rigidez biomecánica del cuerpo [0, 1].
     * 0 = sin rigidez. 1 = máxima rigidez.
     */
    private final double stiffness;

    /**
     * Flexibilidad activa del cuerpo [0, 1].
     * 0 = sin flexibilidad. 1 = máxima flexibilidad.
     */
    private final double flexibility;

    // ── Propiedades de lesión ─────────────────────────────────────────────

    /**
     * Nivel de lesiones biomecánicas acumuladas [0, 1].
     * 0 = sin lesiones. 1 = lesiones incapacitantes.
     */
    private final double injuryLevel;

    // ── Propiedades metabólicas ───────────────────────────────────────────

    /**
     * Tasa metabólica activa [0, +∞).
     * 0 = metabolismo nulo. 1 = metabolismo estándar en reposo.
     * > 1 = metabolismo activo (esfuerzo, estrés).
     */
    private final double metabolicRate;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private BiomechanicalState(Builder b) {
        this.fatigue             = b.fatigue;
        this.muscleTension       = b.muscleTension;
        this.structuralIntegrity = b.structuralIntegrity;
        this.stiffness           = b.stiffness;
        this.flexibility         = b.flexibility;
        this.injuryLevel         = b.injuryLevel;
        this.metabolicRate       = b.metabolicRate;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * Entidad viva en reposo óptimo.
     * Sin fatiga, sin lesiones, plena integridad estructural.
     * Valor por defecto para entidades vivas sin historial de esfuerzo.
     */
    public static final BiomechanicalState RESTED = builder().build();

    /**
     * Entidad viva con fatiga severa (tras esfuerzo prolongado).
     * Alta fatiga, alta tensión muscular, leve reducción de integridad.
     */
    public static final BiomechanicalState EXHAUSTED = builder()
        .fatigue(0.85)
        .muscleTension(0.7)
        .structuralIntegrity(0.9)
        .metabolicRate(1.8)
        .build();

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Nivel de fatiga acumulada [0, 1]. */
    public double getFatigue()             { return fatigue; }

    /** Tensión muscular actual [0, 1]. */
    public double getMuscleTension()       { return muscleTension; }

    /** Integridad estructural del cuerpo vivo [0, 1]. */
    public double getStructuralIntegrity() { return structuralIntegrity; }

    /** Rigidez biomecánica [0, 1]. */
    public double getStiffness()           { return stiffness; }

    /** Flexibilidad activa [0, 1]. */
    public double getFlexibility()         { return flexibility; }

    /** Nivel de lesiones biomecánicas [0, 1]. */
    public double getInjuryLevel()         { return injuryLevel; }

    /** Tasa metabólica activa [0, +∞). */
    public double getMetabolicRate()       { return metabolicRate; }

    // ── Helpers de conveniencia ───────────────────────────────────────────

    /** True si la entidad está fatigada (fatigue > 0.6). */
    public boolean isFatigued()           { return fatigue > 0.6; }

    /** True si la entidad está lesionada (injuryLevel > 0.1). */
    public boolean isInjured()            { return injuryLevel > 0.1; }

    /** True si la integridad estructural es crítica (structuralIntegrity < 0.2). */
    public boolean isCritical()           { return structuralIntegrity < 0.2; }

    /** True si la entidad tiene metabolismo activo (metabolicRate > 1.0). */
    public boolean isMetabolicallyActive(){ return metabolicRate > 1.0; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "BiomechanicalState[fatigue=%.2f tension=%.2f integrity=%.2f injury=%.2f meta=%.2f]",
            fatigue, muscleTension, structuralIntegrity, injuryLevel, metabolicRate);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de BiomechanicalState.
     *
     * Valores por defecto (entidad en reposo óptimo):
     *   fatigue             = 0.0     sin fatiga
     *   muscleTension       = 0.0     completamente relajado
     *   structuralIntegrity = 1.0     completamente intacto
     *   stiffness           = 0.3     rigidez moderada (cuerpo vivo en reposo)
     *   flexibility         = 0.7     buena flexibilidad en reposo
     *   injuryLevel         = 0.0     sin lesiones
     *   metabolicRate       = 1.0     metabolismo estándar en reposo
     */
    public static final class Builder {

        private double fatigue             = 0.0;
        private double muscleTension       = 0.0;
        private double structuralIntegrity = 1.0;
        private double stiffness           = 0.3;
        private double flexibility         = 0.7;
        private double injuryLevel         = 0.0;
        private double metabolicRate       = 1.0;

        private Builder() {}

        public Builder fatigue(double v)             { this.fatigue             = clamp01(v);           return this; }
        public Builder muscleTension(double v)       { this.muscleTension       = clamp01(v);           return this; }
        public Builder structuralIntegrity(double v) { this.structuralIntegrity = clamp01(v);           return this; }
        public Builder stiffness(double v)           { this.stiffness           = clamp01(v);           return this; }
        public Builder flexibility(double v)         { this.flexibility         = clamp01(v);           return this; }
        public Builder injuryLevel(double v)         { this.injuryLevel         = clamp01(v);           return this; }
        public Builder metabolicRate(double v)       { this.metabolicRate       = Math.max(0.0, v);     return this; }

        /** Construye el BiomechanicalState. */
        public BiomechanicalState build() { return new BiomechanicalState(this); }

        private static double clamp01(double v) {
            return Math.max(0.0, Math.min(1.0, v));
        }
    }
}
