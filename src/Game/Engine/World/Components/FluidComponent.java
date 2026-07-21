package Game.Engine.World.Components;

import Game.Engine.Component;
import Game.Engine.World.Physics.CoreDomains;
import Game.Engine.World.Physics.PhysicalQuantity;

/**
 * Estado fluídico actual de un objeto: contenido de humedad.
 *
 * ── HRFC-015 — World Simulation Core (iteración final) ────────────────────
 *
 * ── CAMBIO RESPECTO A LA VERSIÓN ANTERIOR ─────────────────────────────────
 * El campo interno pasa de {@code double humidity} a
 * {@code PhysicalQuantity<CoreDomains.Fluid>}.
 *
 * El clamp natural del dominio fluídico [0, 1] se expresa directamente en
 * PhysicalQuantity.ofClamped(), eliminando el método clamp() interno.
 *
 * ── API NUEVA (PhysicalQuantity) ──────────────────────────────────────────
 *
 *   PhysicalQuantity<CoreDomains.Fluid> humidity = fc.getQuantity();
 *   humidity.add(delta);
 *   humidity.converge(ambientHumidity, absorption * rate);
 *
 * ── API LEGACY — mantenida para callers existentes ────────────────────────
 *
 *   fc.getHumidity()          → quantity.getValue()
 *   fc.setHumidity(h)         → quantity.set(h)
 *   fc.addHumidity(delta)     → quantity.add(delta)
 *   fc.isDry()                → quantity.getValue() < epsilon
 *   fc.isSaturated()          → quantity.getValue() > 1 - epsilon
 */
public final class FluidComponent extends Component {

    /**
     * Estado fluídico tipado. Dominio: CoreDomains.Fluid.
     * Rango natural [0, 1] expresado en la PhysicalQuantity.
     */
    private final PhysicalQuantity<CoreDomains.Fluid> quantity;

    // ── Constructores ─────────────────────────────────────────────────────

    /** Objeto completamente seco al comenzar. */
    public FluidComponent() {
        this(0.0);
    }

    /**
     * Humedad inicial en [0, 1]. Se clampea automáticamente.
     *
     * @param initialHumidity humedad inicial.
     */
    public FluidComponent(double initialHumidity) {
        this.quantity = PhysicalQuantity.ofClamped(initialHumidity, 0.0, 1.0);
    }

    // ── API PhysicalQuantity ──────────────────────────────────────────────

    /**
     * Acceso directo a la magnitud fluídica tipada.
     *
     * @return la magnitud fluídica del objeto (nunca null).
     */
    public PhysicalQuantity<CoreDomains.Fluid> getQuantity() { return quantity; }

    // ── API legacy — delegates ────────────────────────────────────────────

    /** Humedad actual en [0, 1]. Equivale a getQuantity().getValue(). */
    public double  getHumidity()                { return quantity.getValue(); }

    /** Establece la humedad. Equivale a getQuantity().set(h). */
    public void    setHumidity(double h)        { quantity.set(h); }

    /** Añade delta de humedad. Equivale a getQuantity().add(delta). */
    public void    addHumidity(double delta)    { quantity.add(delta); }

    /**
     * Transfiere humedad hacia otro componente.
     * Positivo = este pierde humedad, target gana.
     */
    public void transferTo(FluidComponent target, double amount) {
        if (target == null) return;
        this.quantity.add(-amount);
        target.quantity.add(amount);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /** True si el objeto está completamente seco. */
    public boolean isDry()       { return quantity.getValue() < 1e-6; }

    /** True si el objeto está saturado. */
    public boolean isSaturated() { return quantity.getValue() > 1.0 - 1e-6; }
}
