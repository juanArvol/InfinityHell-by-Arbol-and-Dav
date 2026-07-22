package Game.Engine.World.Physics;

/**
 * Nodo del PropertyDependencyGraph.
 *
 * ── HRFC-021 — Property-Driven Physics Architecture ───────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * PhysicalProperty es el nodo tipado que el PropertyDependencyGraph usa para
 * representar una propiedad física dentro del grafo de dependencias del universo.
 *
 * Envuelve un PropertyDescriptor y le añade la identidad necesaria para
 * participar como nodo del DependencyGraph<PhysicalProperty>.
 *
 * ── IDENTIDAD ─────────────────────────────────────────────────────────────
 * La identidad de un PhysicalProperty es la referencia al PropertyDescriptor
 * que envuelve, no su id textual. Dos PhysicalProperty con el mismo id textual
 * pero construidos desde descriptores distintos son nodos distintos del grafo.
 *
 * El nodeId del DependencyGraph<PhysicalProperty> extrae el id del descriptor
 * subyacente para indexación interna. El código cliente nunca manipula ese
 * string directamente.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 * PhysicalProperty es inmutable. Su única fuente de datos es el
 * PropertyDescriptor que recibe en construcción.
 *
 * No contiene lógica de simulación.
 * No conoce leyes.
 * No conoce entidades.
 * No conoce materiales.
 *
 * ── USO EN EL GRAFO ───────────────────────────────────────────────────────
 *
 *   // Construir nodos
 *   PhysicalProperty temperature = PhysicalProperty.of(CoreProperties.TEMPERATURE);
 *   PhysicalProperty pressure    = PhysicalProperty.of(CoreProperties.PRESSURE);
 *
 *   // Registrar dependencia: temperatura → presión (vía ley de expansión)
 *   graph.addRelation(
 *       temperature, pressure,
 *       PhysicalRelation.builder()
 *           .name("thermal_expansion")
 *           .relationType(RelationType.PASCAL)
 *           .participating(CoreProperties.TEMPERATURE, CoreProperties.PRESSURE)
 *           .build()
 *   );
 *
 * ── CATÁLOGO DE PROPIEDADES ───────────────────────────────────────────────
 * El catálogo de PhysicalProperty del Core vive en CorePhysicalProperties.
 * Las propiedades de extensión (gameplay, mods) declaran sus propias instancias
 * usando PhysicalProperty.of(descriptor).
 */
public final class PhysicalProperty {

    /** Descriptor subyacente. La fuente canónica de identidad y metadatos. */
    private final PropertyDescriptor descriptor;

    // ── Constructor privado — usar factory ────────────────────────────────

    private PhysicalProperty(PropertyDescriptor descriptor) {
        if (descriptor == null)
            throw new IllegalArgumentException("descriptor no puede ser null");
        this.descriptor = descriptor;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /**
     * Crea un nodo de grafo para la propiedad física dada.
     *
     * @param descriptor el descriptor de la propiedad. No puede ser null.
     * @return nodo de grafo para esa propiedad.
     */
    public static PhysicalProperty of(PropertyDescriptor descriptor) {
        return new PhysicalProperty(descriptor);
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /**
     * El PropertyDescriptor subyacente.
     * Usar para acceder al estado físico de una entidad y para declarar
     * inputs/outputs de leyes.
     *
     * @return descriptor de la propiedad. Nunca null.
     */
    public PropertyDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * El identificador único de la propiedad.
     * Extraído del descriptor subyacente.
     * Usado internamente por DependencyGraph como clave de indexación.
     *
     * @return id de la propiedad. Nunca null ni vacío.
     */
    public String getId() {
        return descriptor.getId();
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "PhysicalProperty[" + descriptor.getId() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhysicalProperty other)) return false;
        // identidad fuerte: mismo descriptor por referencia
        return this.descriptor == other.descriptor;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(descriptor);
    }
}
