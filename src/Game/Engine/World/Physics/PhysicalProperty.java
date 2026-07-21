package Game.Engine.World.Physics;

/**
 * Descriptor inmutable de una magnitud física.
 *
 * ── HRFC-017 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * PhysicalProperty es la descripción de una magnitud, no de un fenómeno.
 *
 * Donde antes existían CoreDomains.Thermal, CoreDomains.Electrical, etc.,
 * únicamente como marcadores de tipo, PhysicalProperty añade la capa
 * semántica que esos marcadores no podían expresar:
 *
 *   - un nombre legible (para depuración y registro)
 *   - el dominio al que pertenece (vínculo con el sistema de tipos)
 *   - los límites naturales del valor (clamp semántico)
 *   - el valor de equilibrio ambiental hacia el que disipa naturalmente
 *
 * PhysicalProperty no contiene lógica. No conoce el Solver. No conoce
 * ningún fenómeno. Únicamente describe una magnitud.
 *
 * ── RELACIÓN CON CoreDomains ──────────────────────────────────────────────
 * CoreDomains siguen siendo los marcadores de dominio en el sistema de tipos
 * genéricos (PhysicalQuantity<D>). PhysicalProperty los referencia como
 * metadato pero no los reemplaza.
 *
 * Un mismo dominio (CoreDomains.Thermal.class) puede tener múltiples
 * PhysicalProperty registradas si el juego lo requiere. La identidad de una
 * PhysicalProperty es su id de texto, no su dominio.
 *
 * ── QUÉ NO CONTIENE ──────────────────────────────────────────────────────
 *   ✗ Algoritmos
 *   ✗ Condiciones if/switch
 *   ✗ Referencias al Solver
 *   ✗ Fenómenos físicos concretos
 *   ✗ Consecuencias de gameplay
 *
 * ── USO EN REGISTRO ───────────────────────────────────────────────────────
 *
 *   // Registrar una propiedad en PhysicalState
 *   PhysicalProperty temperature = PhysicalProperty
 *       .builder(CoreDomains.Thermal.class, "temperature")
 *       .equilibrium(0.0)
 *       .build();
 *
 *   PhysicalProperty humidity = PhysicalProperty
 *       .builder(CoreDomains.Fluid.class, "humidity")
 *       .range(0.0, 1.0)
 *       .equilibrium(0.0)
 *       .build();
 *
 *   PhysicalProperty charge = PhysicalProperty
 *       .builder(CoreDomains.Electrical.class, "charge")
 *       .equilibrium(0.0)
 *       .build();
 *
 *   PhysicalProperty pressure = PhysicalProperty
 *       .builder(CoreDomains.Pressure.class, "pressure")
 *       .equilibrium(0.0)
 *       .build();
 *
 * ── CATÁLOGO RECOMENDADO ─────────────────────────────────────────────────
 * Las propiedades concretas del juego se definen en PhysicalProperties
 * (catálogo estático), no aquí. Esto garantiza que el Engine no crece para
 * representar nuevos fenómenos — el conocimiento vive en los datos.
 *
 * @param <D> dominio físico al que pertenece esta propiedad.
 */
public final class PhysicalProperty<D extends PhysicalDomain> {

    /** Identificador único de texto. Nunca null ni vacío. */
    private final String     id;

    /** Dominio físico de la propiedad. Nunca null. */
    private final Class<D>   domain;

    /** Límite inferior del valor. Double.NEGATIVE_INFINITY si no acotado. */
    private final double     min;

    /** Límite superior del valor. Double.POSITIVE_INFINITY si no acotado. */
    private final double     max;

    /** True si el valor está acotado entre min y max. */
    private final boolean    bounded;

    /**
     * Valor de equilibrio ambiental.
     * Valor al que la propiedad converge en ausencia de influencias externas.
     * Por convención 0.0 para magnitudes de desvío respecto al ambiente.
     */
    private final double     equilibrium;

    /** Descripción legible. Puede ser null. */
    private final String     description;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private PhysicalProperty(Builder<D> b) {
        this.id          = b.id;
        this.domain      = b.domain;
        this.min         = b.min;
        this.max         = b.max;
        this.bounded     = b.bounded;
        this.equilibrium = b.equilibrium;
        this.description = b.description;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /**
     * Punto de entrada del Builder.
     *
     * @param domain clase del dominio físico.
     * @param id     identificador único. No puede ser null ni vacío.
     * @param <D>    dominio físico.
     * @return nuevo Builder.
     */
    public static <D extends PhysicalDomain> Builder<D> builder(Class<D> domain, String id) {
        return new Builder<>(domain, id);
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Identificador único de la propiedad. */
    public String   getId()          { return id; }

    /** Clase del dominio físico. */
    public Class<D> getDomain()      { return domain; }

    /** Límite inferior del valor. Double.NEGATIVE_INFINITY si sin límite. */
    public double   getMin()         { return min; }

    /** Límite superior del valor. Double.POSITIVE_INFINITY si sin límite. */
    public double   getMax()         { return max; }

    /** True si el valor de la propiedad está acotado entre min y max. */
    public boolean  isBounded()      { return bounded; }

    /**
     * Valor de equilibrio ambiental.
     * La propiedad converge hacia este valor en ausencia de influencias.
     */
    public double   getEquilibrium() { return equilibrium; }

    /** Descripción legible de la propiedad. Puede ser null. */
    public String   getDescription() { return description; }

    /**
     * Crea una PhysicalQuantity<D> inicializada con el valor de equilibrio
     * y con los límites de esta propiedad.
     *
     * Usar al inicializar el PhysicalState de un nuevo objeto:
     *
     *   PhysicalQuantity<D> q = property.createQuantity();
     *
     * @return nueva instancia de PhysicalQuantity para esta propiedad.
     */
    public PhysicalQuantity<D> createQuantity() {
        return createQuantity(equilibrium);
    }

    /**
     * Crea una PhysicalQuantity<D> con el valor inicial dado y los límites
     * de esta propiedad.
     *
     * @param initialValue valor inicial.
     * @return nueva instancia de PhysicalQuantity para esta propiedad.
     */
    public PhysicalQuantity<D> createQuantity(double initialValue) {
        if (bounded) {
            return PhysicalQuantity.ofClamped(initialValue, min, max);
        }
        return PhysicalQuantity.of(initialValue);
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        String domainName = domain != null ? domain.getSimpleName() : "?";
        return "PhysicalProperty[" + id + " domain=" + domainName + "]";
    }

    // ── Builder ───────────────────────────────────────────────────────────

    /**
     * Builder de PhysicalProperty.
     *
     * @param <D> dominio físico.
     */
    public static final class Builder<D extends PhysicalDomain> {

        private final Class<D> domain;
        private final String   id;

        private double  min         = Double.NEGATIVE_INFINITY;
        private double  max         = Double.POSITIVE_INFINITY;
        private boolean bounded     = false;
        private double  equilibrium = 0.0;
        private String  description = null;

        private Builder(Class<D> domain, String id) {
            if (domain == null) throw new IllegalArgumentException("domain no puede ser null");
            if (id == null || id.isBlank())
                throw new IllegalArgumentException("id no puede ser null ni vacío");
            this.domain = domain;
            this.id     = id;
        }

        /**
         * Define los límites naturales del valor [min, max].
         * Activa el clamp automático en la PhysicalQuantity creada por esta propiedad.
         *
         * @param min límite inferior.
         * @param max límite superior.
         * @return this (para encadenado).
         */
        public Builder<D> range(double min, double max) {
            if (min > max) throw new IllegalArgumentException(
                "min (" + min + ") no puede ser mayor que max (" + max + ")");
            this.min     = min;
            this.max     = max;
            this.bounded = true;
            return this;
        }

        /**
         * Define el valor de equilibrio ambiental.
         * La propiedad converge hacia este valor en ausencia de influencias.
         *
         * @param equilibrium valor de equilibrio.
         * @return this (para encadenado).
         */
        public Builder<D> equilibrium(double equilibrium) {
            this.equilibrium = equilibrium;
            return this;
        }

        /**
         * Descripción legible de la propiedad (opcional, para depuración).
         *
         * @param description texto descriptivo.
         * @return this (para encadenado).
         */
        public Builder<D> description(String description) {
            this.description = description;
            return this;
        }

        /** Construye la PhysicalProperty. */
        public PhysicalProperty<D> build() {
            return new PhysicalProperty<>(this);
        }
    }
}
