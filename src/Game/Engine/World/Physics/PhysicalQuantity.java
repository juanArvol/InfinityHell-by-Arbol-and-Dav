package Game.Engine.World.Physics;

/**
 * Magnitud física tipada — valor numérico asociado a un dominio físico.
 *
 * ── HRFC-015 — World Simulation Core (iteración final) ────────────────────
 *
 * ── QUÉ ES PhysicalQuantity ──────────────────────────────────────────────
 * PhysicalQuantity<D> es un value object que representa una magnitud física
 * en el universo de Infinity Hell. Combina dos aspectos:
 *
 *   1. Un valor numérico (double) — la magnitud en sí.
 *   2. Un dominio físico D (parámetro de tipo) — la naturaleza de la magnitud.
 *
 * El parámetro de tipo D garantiza en tiempo de compilación que solo se
 * realizan operaciones entre magnitudes del mismo dominio:
 *
 *   PhysicalQuantity<CoreDomains.Thermal>    t1 = PhysicalQuantity.of(100.0);
 *   PhysicalQuantity<CoreDomains.Electrical> q1 = PhysicalQuantity.of(5.0);
 *
 *   t1.add(q1);    // ERROR en compilación — tipos incompatibles
 *   t1.add(t1);    // OK
 *
 * ── POR QUÉ ESTO IMPORTA ──────────────────────────────────────────────────
 * Sin esta abstracción, la API del Engine expone primitivos:
 *
 *   double temperature = thermalComponent.getTemperature();   // ¿temperatura? ¿qué unidad?
 *   double charge      = electricalComponent.getCharge();     // ¿carga? ¿es comparable?
 *   thermalComponent.addHeat(charge);   // SILENCIOSO: suma carga a temperatura
 *
 * Con PhysicalQuantity:
 *
 *   PhysicalQuantity<CoreDomains.Thermal>    temp  = thermalComponent.get();
 *   PhysicalQuantity<CoreDomains.Electrical> charge = electricalComponent.get();
 *   thermalComponent.add(charge);   // ERROR DE COMPILACIÓN — dominios distintos
 *
 * ── DISEÑO: MUTABLE POR INTENCIÓN ────────────────────────────────────────
 * PhysicalQuantity es mutable. Los componentes de estado (ThermalComponent,
 * ElectricalComponent, etc.) exponen una instancia mutable que los módulos
 * de simulación modifican directamente:
 *
 *   thermalComponent.get().add(delta);
 *
 * Esto evita allocations en el game loop y mantiene la API idiomática.
 * Para uso en contextos donde la inmutabilidad es necesaria (logging,
 * InteractionContext), usar snapshot():
 *
 *   PhysicalQuantity<D> snap = quantity.snapshot(); // copia inmutable del valor actual
 *
 * ── OPERACIONES ──────────────────────────────────────────────────────────
 * Las operaciones son in-place (mutan this) y retornan this para encadenado:
 *
 *   quantity.add(delta).clamp(-100, 100);
 *
 * Para obtener un resultado sin mutar, usar los métodos estáticos:
 *
 *   PhysicalQuantity<D> result = PhysicalQuantity.sum(a, b);
 *
 * ── CLAMP ────────────────────────────────────────────────────────────────
 * Los dominios con rango natural (Fluid: [0,1]) pueden configurarse con
 * límites al construirse:
 *
 *   PhysicalQuantity<CoreDomains.Fluid> h = PhysicalQuantity.ofClamped(0.0, 0.0, 1.0);
 *   h.add(2.0);   // valor queda en 1.0 — el clamp es automático
 *
 * Para magnitudes sin límite, usar PhysicalQuantity.of() (clamp desactivado).
 *
 * ── ACCESO AL VALOR PRIMITIVO ─────────────────────────────────────────────
 * Los módulos de simulación que necesitan el double subyacente usan getValue().
 * Esto es intencional: la simulación trabaja con doubles por eficiencia,
 * pero la API pública de los componentes trabaja con PhysicalQuantity.
 *
 * @param <D> dominio físico que esta magnitud representa.
 *            Debe implementar {@link PhysicalDomain}.
 *            Garantiza type safety entre dominios en tiempo de compilación.
 */
public final class PhysicalQuantity<D extends PhysicalDomain> {

    private double value;
    private final double min;
    private final double max;
    private final boolean bounded;

    // ── Constructores privados — usar factories ───────────────────────────

    private PhysicalQuantity(double value, double min, double max, boolean bounded) {
        this.min     = min;
        this.max     = max;
        this.bounded = bounded;
        this.value   = bounded ? clamp(value) : value;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Magnitud sin límites. El valor puede crecer o decrecer sin restricción.
     * Usar para temperatura, carga eléctrica, presión.
     *
     * @param initialValue valor inicial.
     * @param <D>          dominio físico.
     */
    public static <D extends PhysicalDomain> PhysicalQuantity<D> of(double initialValue) {
        return new PhysicalQuantity<>(initialValue, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false);
    }

    /**
     * Magnitud con valor inicial cero y sin límites.
     *
     * @param <D> dominio físico.
     */
    public static <D extends PhysicalDomain> PhysicalQuantity<D> zero() {
        return of(0.0);
    }

    /**
     * Magnitud con clamp automático en [min, max].
     * Usar para magnitudes con rango natural: humedad [0,1], ángulos [0,360], etc.
     *
     * @param initialValue valor inicial (será clampeado a [min, max]).
     * @param min          límite inferior.
     * @param max          límite superior.
     * @param <D>          dominio físico.
     */
    public static <D extends PhysicalDomain> PhysicalQuantity<D> ofClamped(double initialValue, double min, double max) {
        if (min > max) throw new IllegalArgumentException("min (" + min + ") no puede ser mayor que max (" + max + ")");
        return new PhysicalQuantity<>(initialValue, min, max, true);
    }

    /**
     * Magnitud con límite inferior en 0 (magnitudes que no pueden ser negativas).
     * Usar para masa, densidad, capacidades.
     *
     * @param initialValue valor inicial (mínimo 0).
     * @param <D>          dominio físico.
     */
    public static <D extends PhysicalDomain> PhysicalQuantity<D> ofPositive(double initialValue) {
        return ofClamped(initialValue, 0.0, Double.POSITIVE_INFINITY);
    }

    // ── Lectura ───────────────────────────────────────────────────────────

    /**
     * Valor numérico actual.
     * Para uso interno de simulaciones y comparaciones. La API pública del
     * Engine trabaja con PhysicalQuantity, no con este double directamente.
     */
    public double getValue() { return value; }

    /** True si el valor está efectivamente en cero (dentro de epsilon). */
    public boolean isZero()    { return Math.abs(value) < 1e-9; }

    /** True si el valor es positivo (mayor que epsilon). */
    public boolean isPositive(){ return value > 1e-9; }

    /** True si el valor es negativo (menor que -epsilon). */
    public boolean isNegative(){ return value < -1e-9; }

    /** True si esta magnitud tiene clamp configurado. */
    public boolean isBounded() { return bounded; }

    /** Límite inferior configurado. Double.NEGATIVE_INFINITY si no tiene límite. */
    public double getMin()     { return min; }

    /** Límite superior configurado. Double.POSITIVE_INFINITY si no tiene límite. */
    public double getMax()     { return max; }

    // ── Mutación in-place ─────────────────────────────────────────────────

    /**
     * Añade delta al valor actual.
     * Si la magnitud tiene clamp, el resultado se limita automáticamente.
     *
     * @param delta cantidad a añadir. Negativo = restar.
     * @return this (para encadenado).
     */
    public PhysicalQuantity<D> add(double delta) {
        value = bounded ? clamp(value + delta) : value + delta;
        return this;
    }

    /**
     * Establece el valor directamente.
     * Si la magnitud tiene clamp, el valor se limita automáticamente.
     *
     * @param newValue nuevo valor.
     * @return this (para encadenado).
     */
    public PhysicalQuantity<D> set(double newValue) {
        value = bounded ? clamp(newValue) : newValue;
        return this;
    }

    /**
     * Escala el valor por un factor.
     *
     * @param factor multiplicador.
     * @return this (para encadenado).
     */
    public PhysicalQuantity<D> scale(double factor) {
        value = bounded ? clamp(value * factor) : value * factor;
        return this;
    }

    /**
     * Aplica convergencia exponencial hacia un valor objetivo.
     * Útil para disipación natural: la magnitud se acerca a 'target'
     * a una velocidad proporcional a 'rate'.
     *
     *   ΔV = (target - current) × rate
     *
     * @param target valor objetivo (p.ej. temperatura ambiente = 0).
     * @param rate   fracción del delta a aplicar por frame [0, 1].
     * @return this (para encadenado).
     */
    public PhysicalQuantity<D> converge(double target, double rate) {
        if (rate <= 0) return this;
        double r = Math.min(1.0, rate);
        return add((target - value) * r);
    }

    /**
     * Clampea el valor al rango [lo, hi], independientemente del clamp configurado.
     * Útil para imponer límites temporales sin cambiar la configuración de la magnitud.
     *
     * @param lo límite inferior.
     * @param hi límite superior.
     * @return this (para encadenado).
     */
    public PhysicalQuantity<D> clampTo(double lo, double hi) {
        value = Math.max(lo, Math.min(hi, value));
        return this;
    }

    /**
     * Resetea el valor a cero.
     *
     * @return this (para encadenado).
     */
    public PhysicalQuantity<D> reset() {
        value = bounded ? clamp(0.0) : 0.0;
        return this;
    }

    // ── Snapshot ──────────────────────────────────────────────────────────

    /**
     * Retorna una nueva instancia con el valor actual.
     * La copia es independiente — modificaciones posteriores a this no la afectan.
     * Usar cuando se necesita una instantánea del estado para logging,
     * comparaciones o contextos inmutables (InteractionContext).
     *
     * @return nueva PhysicalQuantity con el mismo valor, clamp y configuración.
     */
    public PhysicalQuantity<D> snapshot() {
        return new PhysicalQuantity<>(value, min, max, bounded);
    }

    // ── Operaciones estáticas — sin mutar los operandos ───────────────────

    /**
     * Retorna el delta entre dos magnitudes del mismo dominio.
     *
     * @param a primera magnitud.
     * @param b segunda magnitud.
     * @param <D> dominio compartido.
     * @return a.getValue() - b.getValue()
     */
    public static <D extends PhysicalDomain> double delta(PhysicalQuantity<D> a, PhysicalQuantity<D> b) {
        return a.value - b.value;
    }

    /**
     * Retorna el mínimo entre los valores de dos magnitudes del mismo dominio.
     */
    public static <D extends PhysicalDomain> double min(PhysicalQuantity<D> a, PhysicalQuantity<D> b) {
        return Math.min(a.value, b.value);
    }

    /**
     * Retorna el máximo entre los valores de dos magnitudes del mismo dominio.
     */
    public static <D extends PhysicalDomain> double max(PhysicalQuantity<D> a, PhysicalQuantity<D> b) {
        return Math.max(a.value, b.value);
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        if (bounded) {
            return "PhysicalQuantity[" + value + " in [" + min + ", " + max + "]]";
        }
        return "PhysicalQuantity[" + value + "]";
    }

    // ── Implementación ────────────────────────────────────────────────────

    private double clamp(double v) {
        return Math.max(min, Math.min(max, v));
    }
}
