package Game.Engine.World.Components;

import Game.Engine.Component;
import Game.Engine.World.Physics.CoreDomains;
import Game.Engine.World.Physics.PhysicalQuantity;

/**
 * Estado de presión local de un objeto.
 *
 * ── HRFC-015 — World Simulation Core (iteración final) ────────────────────
 *
 * ── CAMBIO RESPECTO A LA VERSIÓN ANTERIOR ─────────────────────────────────
 * El campo interno pasa de {@code double pressure} a
 * {@code PhysicalQuantity<CoreDomains.Pressure>}.
 *
 * El límite estructural (structuralLimit) se expresa como clamp simétrico
 * en PhysicalQuantity.ofClamped(-limit, limit). Para objetos sin límite
 * estructural se usa PhysicalQuantity.of().
 *
 * ── API NUEVA (PhysicalQuantity) ──────────────────────────────────────────
 *
 *   PhysicalQuantity<CoreDomains.Pressure> pres = pc.getQuantity();
 *   pres.add(delta);
 *   pres.converge(0.0, dissipationRate);
 *
 * ── API LEGACY — mantenida para callers existentes ────────────────────────
 *
 *   pc.getPressure()           → quantity.getValue()
 *   pc.setPressure(p)          → quantity.set(p)
 *   pc.addPressure(delta)      → quantity.add(delta)
 *   pc.dissipate(rate)         → quantity.converge(0.0, rate)
 *   pc.isAtEquilibrium()       → quantity.isZero()
 *   pc.isAtStructuralLimit()   → Math.abs(value) >= structuralLimit
 *   pc.getStructuralStress()   → ratio en [0, 1]
 */
public final class PressureComponent extends Component {

    /** Estado de presión tipado. Dominio: CoreDomains.Pressure. */
    private final PhysicalQuantity<CoreDomains.Pressure> quantity;

    /**
     * Límite estructural en módulo.
     * Guardado explícitamente para las consultas isAtStructuralLimit() y
     * getStructuralStress(), que necesitan el valor sin acceder al clamp interno.
     */
    private final double structuralLimit;

    // ── Constructores ─────────────────────────────────────────────────────

    /** Presión en equilibrio (0), sin límite estructural. */
    public PressureComponent() {
        this(0.0, Double.POSITIVE_INFINITY);
    }

    /** Presión inicial fija, sin límite estructural. */
    public PressureComponent(double initialPressure) {
        this(initialPressure, Double.POSITIVE_INFINITY);
    }

    /**
     * Presión inicial y límite estructural.
     *
     * @param initialPressure presión inicial.
     * @param structuralLimit presión máxima soportada en módulo.
     *                        Double.POSITIVE_INFINITY = sin límite.
     */
    public PressureComponent(double initialPressure, double structuralLimit) {
        double limit = (structuralLimit > 0) ? structuralLimit : Double.POSITIVE_INFINITY;
        this.structuralLimit = limit;
        this.quantity = Double.isFinite(limit)
            ? PhysicalQuantity.ofClamped(initialPressure, -limit, limit)
            : PhysicalQuantity.of(initialPressure);
    }

    // ── API PhysicalQuantity ──────────────────────────────────────────────

    /**
     * Acceso directo a la magnitud de presión tipada.
     *
     * @return la magnitud de presión del objeto (nunca null).
     */
    public PhysicalQuantity<CoreDomains.Pressure> getQuantity() { return quantity; }

    // ── API legacy — delegates ────────────────────────────────────────────

    /** Presión actual. Equivale a getQuantity().getValue(). */
    public double  getPressure()                  { return quantity.getValue(); }

    /** Establece la presión. Equivale a getQuantity().set(p). */
    public void    setPressure(double p)          { quantity.set(p); }

    /** Añade delta de presión. Equivale a getQuantity().add(delta). */
    public void    addPressure(double delta)      { quantity.add(delta); }

    /**
     * Disipación pasiva hacia el equilibrio (0).
     * Equivale a getQuantity().converge(0.0, rate).
     *
     * @param dissipationRate fracción de la presión disipada por frame [0, 1].
     */
    public void    dissipate(double dissipationRate) {
        if (dissipationRate <= 0) return;
        quantity.converge(0.0, Math.min(1.0, dissipationRate));
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /** Límite estructural de presión. */
    public double  getStructuralLimit()    { return structuralLimit; }

    /** True si la presión está efectivamente en equilibrio. */
    public boolean isAtEquilibrium()       { return quantity.isZero(); }

    /**
     * True si la presión ha alcanzado el límite estructural del material.
     * El InteractionRegistry evalúa consecuencias cuando esto es true.
     */
    public boolean isAtStructuralLimit() {
        return Double.isFinite(structuralLimit)
            && Math.abs(quantity.getValue()) >= structuralLimit;
    }

    /** Fracción de la presión respecto al límite estructural [0, 1]. */
    public double  getStructuralStress() {
        if (!Double.isFinite(structuralLimit) || structuralLimit == 0) return 0.0;
        return Math.min(1.0, Math.abs(quantity.getValue()) / structuralLimit);
    }
}
