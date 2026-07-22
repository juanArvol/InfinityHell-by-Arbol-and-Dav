package Game.Engine.Entity.Components;

import Game.Engine.Component;

/**
 * Estado térmico actual de un objeto.
 *
 * ── HRFC-021 — Property-Driven Physics Architecture ───────────────────────
 *
 * ── NOTA DE MIGRACIÓN ─────────────────────────────────────────────────────
 * En el modelo HRFC-021, el estado térmico vive en PhysicalState (como
 * propiedad CoreProperties.TEMPERATURE registrada en PhysicsComponent).
 *
 * ThermalComponent se mantiene por compatibilidad con código de gameplay
 * existente que lo usa como componente independiente (p.ej. WorldFieldPresets).
 * Su implementación es ahora un wrapper mínimo sobre un double mutable.
 *
 * Todo código nuevo debe usar PhysicsComponent + PhysicalState.
 *
 * ── API ───────────────────────────────────────────────────────────────────
 *
 *   ThermalComponent tc = obj.getComponent(ThermalComponent.class);
 *   if (tc != null) {
 *       tc.addHeat(delta);
 *       double t = tc.getTemperature();
 *   }
 */
public final class ThermalComponent extends Component {

    /** Temperatura actual. 0 = ambiente. Positivo = caliente. Negativo = frío. */
    private double temperature;

    // ── Constructores ─────────────────────────────────────────────────────

    /** Temperatura inicial en 0 (ambiente neutro). */
    public ThermalComponent() {
        this(0.0);
    }

    /**
     * Temperatura inicial fija.
     *
     * @param initialTemperature positivo = caliente, negativo = frío, 0 = ambiente.
     */
    public ThermalComponent(double initialTemperature) {
        this.temperature = initialTemperature;
    }

    // ── API ───────────────────────────────────────────────────────────────

    /** Temperatura actual como double. */
    public double getTemperature()         { return temperature; }

    /** Establece la temperatura directamente. */
    public void   setTemperature(double t) { this.temperature = t; }

    /** Añade un delta de temperatura. */
    public void   addHeat(double delta)    { this.temperature += delta; }

    // ── Consultas de estado ───────────────────────────────────────────────

    /** True si la temperatura está efectivamente en cero. */
    public boolean isAtAmbient() { return Math.abs(temperature) < 1e-9; }

    /** True si la temperatura es positiva (objeto más caliente que el ambiente). */
    public boolean isHot()  { return temperature > 0; }

    /** True si la temperatura es negativa (objeto más frío que el ambiente). */
    public boolean isCold() { return temperature < 0; }

    // ── Builder — retrocompatibilidad de compilación ──────────────────────

    /** @deprecated Usar {@code new ThermalComponent()} o {@code new ThermalComponent(temp)}. */
    @Deprecated
    public static final class Builder {
        private double temperature = 0.0;

        public Builder temperature(double v)                           { this.temperature = v; return this; }
        /** @deprecated → MaterialComponent.Builder.thermalConductivity() */
        @Deprecated public Builder conductivity(double v)             { return this; }
        /** @deprecated → MaterialComponent.Builder.heatCapacity() */
        @Deprecated public Builder heatCapacity(double v)             { return this; }
        /** @deprecated → RelationConstraint de disipación térmica en RelationRegistry */
        @Deprecated public Builder ignitionThreshold(double v)        { return this; }
        /** @deprecated → RelationConstraint de disipación térmica en RelationRegistry */
        @Deprecated public Builder freezingThreshold(double v)        { return this; }

        public ThermalComponent build() { return new ThermalComponent(temperature); }
    }

    /** @deprecated Usar constructores directos. */
    @Deprecated
    public static Builder builder() { return new Builder(); }
}
