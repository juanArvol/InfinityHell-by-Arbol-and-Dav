package Game.Engine.World.Components;

import Game.Engine.Component;
import Game.Engine.World.Physics.CoreDomains;
import Game.Engine.World.Physics.PhysicalQuantity;

/**
 * Estado eléctrico actual de un objeto.
 *
 * ── HRFC-015 — World Simulation Core (iteración final) ────────────────────
 *
 * ── CAMBIO RESPECTO A LA VERSIÓN ANTERIOR ─────────────────────────────────
 * El campo interno pasa de {@code double charge} a
 * {@code PhysicalQuantity<CoreDomains.Electrical>}.
 *
 * La capacidad máxima (maxCharge) se expresa como clamp en la PhysicalQuantity,
 * eliminando la lógica duplicada de clampCharge(). Para objetos sin límite
 * de capacidad se usa PhysicalQuantity.of() (sin clamp); para condensadores
 * se usa PhysicalQuantity.ofClamped(-max, max).
 *
 * ── API NUEVA (PhysicalQuantity) ──────────────────────────────────────────
 *
 *   PhysicalQuantity<CoreDomains.Electrical> charge = ec.getQuantity();
 *   charge.add(delta);
 *   charge.converge(0.0, dissipationRate);
 *
 * ── API LEGACY — mantenida para callers existentes ────────────────────────
 *
 *   ec.getCharge()         → quantity.getValue()
 *   ec.setCharge(q)        → quantity.set(q)
 *   ec.addCharge(delta)    → quantity.add(delta)
 *   ec.isNeutral()         → quantity.isZero()
 */
public final class ElectricalComponent extends Component {

    /** Estado eléctrico tipado. Dominio: CoreDomains.Electrical. */
    private final PhysicalQuantity<CoreDomains.Electrical> quantity;

    /** Capacidad máxima en módulo. Positivo = tiene límite; POSITIVE_INFINITY = sin límite. */
    private final double maxCharge;

    // ── Constructores ─────────────────────────────────────────────────────

    /** Carga inicial 0, sin límite de capacidad. */
    public ElectricalComponent() {
        this(0.0, Double.POSITIVE_INFINITY);
    }

    /** Carga inicial fija, sin límite de capacidad. */
    public ElectricalComponent(double initialCharge) {
        this(initialCharge, Double.POSITIVE_INFINITY);
    }

    /**
     * Carga inicial y capacidad máxima.
     *
     * @param initialCharge carga inicial.
     * @param maxCharge     límite de carga en módulo. Double.POSITIVE_INFINITY = sin límite.
     */
    public ElectricalComponent(double initialCharge, double maxCharge) {
        double cap = (maxCharge > 0) ? maxCharge : Double.POSITIVE_INFINITY;
        this.maxCharge = cap;
        this.quantity  = Double.isFinite(cap)
            ? PhysicalQuantity.ofClamped(initialCharge, -cap, cap)
            : PhysicalQuantity.of(initialCharge);
    }

    // ── API PhysicalQuantity ──────────────────────────────────────────────

    /**
     * Acceso directo a la magnitud eléctrica tipada.
     *
     * @return la magnitud eléctrica del objeto (nunca null).
     */
    public PhysicalQuantity<CoreDomains.Electrical> getQuantity() { return quantity; }

    // ── API legacy — delegates ─────────────────────────────────────────────

    /** Carga eléctrica actual. Equivale a getQuantity().getValue(). */
    public double  getCharge()              { return quantity.getValue(); }

    /** Establece la carga. Equivale a getQuantity().set(q). */
    public void    setCharge(double q)      { quantity.set(q); }

    /** Añade delta de carga. Equivale a getQuantity().add(delta). */
    public void    addCharge(double delta)  { quantity.add(delta); }

    /**
     * Transfiere carga hacia otro componente.
     * Positivo = este pierde carga, target gana.
     */
    public void transferTo(ElectricalComponent target, double amount) {
        if (target == null) return;
        this.quantity.add(-amount);
        target.quantity.add(amount);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /** Capacidad máxima en módulo. */
    public double  getMaxCharge()                  { return maxCharge; }

    /** True si la carga está efectivamente en cero. */
    public boolean isNeutral()                     { return quantity.isZero(); }

    /** True si está en capacidad máxima positiva. */
    public boolean isAtMaxPositiveCapacity() {
        return Double.isFinite(maxCharge) && quantity.getValue() >= maxCharge;
    }

    /** True si está en capacidad máxima negativa. */
    public boolean isAtMaxNegativeCapacity() {
        return Double.isFinite(maxCharge) && quantity.getValue() <= -maxCharge;
    }
}
