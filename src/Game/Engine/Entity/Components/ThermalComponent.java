package Game.Engine.Entity.Components;

import Game.Engine.Component;
import Game.Engine.World.Physics.CoreDomains;
import Game.Engine.World.Physics.PhysicalQuantity;

/**
 * Estado térmico actual de un objeto.
 *
 * ── HRFC-015 — World Simulation Core (iteración final) ────────────────────
 *
 * ── CAMBIO RESPECTO A LA VERSIÓN ANTERIOR ─────────────────────────────────
 * El campo interno pasa de {@code double temperature} a
 * {@code PhysicalQuantity<CoreDomains.Thermal>}.
 *
 * Esto elimina los primitivos de la API pública del Engine: el dominio térmico
 * ahora está representado por un tipo con semántica explícita. El compilador
 * rechaza mezclar magnitudes de dominios distintos.
 *
 * La API legacy (getTemperature / setTemperature / addHeat) se mantiene como
 * delegates hacia PhysicalQuantity para no romper los callers existentes:
 *   - ThermalSimulation
 *   - WorldFieldPresets
 *
 * ── API NUEVA (PhysicalQuantity) ──────────────────────────────────────────
 *
 *   ThermalComponent tc = obj.getComponent(ThermalComponent.class);
 *
 *   // Leer el estado térmico tipado:
 *   PhysicalQuantity<CoreDomains.Thermal> temp = tc.getQuantity();
 *
 *   // Modificar mediante la abstracción:
 *   tc.getQuantity().add(delta);
 *   tc.getQuantity().converge(0.0, diffusivity * timeScale);
 *
 *   // Snapshot para InteractionContext (no muta el estado):
 *   PhysicalQuantity<CoreDomains.Thermal> snap = tc.getQuantity().snapshot();
 *
 * ── API LEGACY (double) — mantenida por compatibilidad ───────────────────
 *
 *   tc.getTemperature()       → quantity.getValue()
 *   tc.setTemperature(t)      → quantity.set(t)
 *   tc.addHeat(delta)         → quantity.add(delta)
 *
 * ── SEPARACIÓN Estado / Material (sin cambios) ────────────────────────────
 *
 *   ThermalComponent    → "temperatura actual"         (cambia cada frame)
 *   MaterialComponent   → "conductividad del material" (constante)
 */
public final class ThermalComponent extends Component {

    /** Estado térmico tipado. Dominio: CoreDomains.Thermal. */
    private final PhysicalQuantity<CoreDomains.Thermal> quantity;

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
        this.quantity = PhysicalQuantity.of(initialTemperature);
    }

    // ── API PhysicalQuantity ──────────────────────────────────────────────

    /**
     * Acceso directo a la magnitud térmica tipada.
     *
     * Usar para toda lógica nueva. La magnitud es mutable — los módulos de
     * simulación la modifican directamente:
     *
     *   tc.getQuantity().add(heatDelta);
     *   tc.getQuantity().converge(ambientTemperature, diffusivity * scale);
     *
     * @return la magnitud térmica del objeto (nunca null).
     */
    public PhysicalQuantity<CoreDomains.Thermal> getQuantity() { return quantity; }

    // ── API legacy — delegates hacia PhysicalQuantity ─────────────────────
    // Mantenida para compatibilidad con ThermalSimulation y WorldFieldPresets.

    /** Temperatura actual como double. Equivale a getQuantity().getValue(). */
    public double getTemperature()      { return quantity.getValue(); }

    /** Establece la temperatura. Equivale a getQuantity().set(t). */
    public void   setTemperature(double t) { quantity.set(t); }

    /** Añade delta de temperatura. Equivale a getQuantity().add(delta). */
    public void   addHeat(double delta) { quantity.add(delta); }

    // ── Consultas de estado ───────────────────────────────────────────────

    /** True si la temperatura está efectivamente en cero. */
    public boolean isAtAmbient() { return quantity.isZero(); }

    /** True si la temperatura es positiva (objeto más caliente que el ambiente). */
    public boolean isHot()  { return quantity.isPositive(); }

    /** True si la temperatura es negativa (objeto más frío que el ambiente). */
    public boolean isCold() { return quantity.isNegative(); }

    // ── Builder deprecado — retrocompatibilidad de compilación ────────────

    /** @deprecated Usar {@code new ThermalComponent()} o {@code new ThermalComponent(temp)}. */
    @Deprecated
    public static final class Builder {
        private double temperature = 0.0;
        @SuppressWarnings("unused") private double ignitionThreshold = Double.POSITIVE_INFINITY;
        @SuppressWarnings("unused") private double freezingThreshold = Double.NEGATIVE_INFINITY;
        @SuppressWarnings("unused") private double conductivity      = 0.1;
        @SuppressWarnings("unused") private double heatCapacity      = 1.0;

        public Builder temperature(double v)       { this.temperature = v; return this; }
        /** @deprecated → MaterialComponent.Builder.thermalConductivity() */
        @Deprecated public Builder conductivity(double v)      { this.conductivity = v; return this; }
        /** @deprecated → MaterialComponent.Builder.heatCapacity() */
        @Deprecated public Builder heatCapacity(double v)      { this.heatCapacity = v; return this; }
        /** @deprecated → PhysicsSolver + LawRegistry (ley de disipación térmica) */
        @Deprecated public Builder ignitionThreshold(double v) { this.ignitionThreshold = v; return this; }
        /** @deprecated → PhysicsSolver + LawRegistry (ley de disipación térmica) */
        @Deprecated public Builder freezingThreshold(double v) { this.freezingThreshold = v; return this; }

        public ThermalComponent build() { return new ThermalComponent(temperature); }
    }

    /** @deprecated Usar constructores directos. */
    @Deprecated
    public static Builder builder() { return new Builder(); }
}
