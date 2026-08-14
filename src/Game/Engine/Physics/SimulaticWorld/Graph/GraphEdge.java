package Game.Engine.Physics.SimulaticWorld.Graph;

/**
 * Arista dirigida genérica del DependencyGraph<T>.
 *
 * ── HRFC-016 — Consolidación del modelo emergente ────────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * GraphEdge<T> representa una relación dirigida entre dos nodos del mismo
 * tipo T en un DependencyGraph<T>:
 *
 *   "El nodo A tiene una relación con el nodo B"
 *
 * El contenido semántico de la relación se almacena en la carga útil
 * (payload), cuyo tipo es también paramétrico para mantener la genericidad
 * del motor sin perder type-safety en las especializaciones.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 * GraphEdge<T> no impone una interfaz de payload específica.
 * Las especializaciones del Engine llevan su propio tipo de carga:
 *
 *   DependencyGraph<PhysicalProperty> → GraphEdge<PhysicalProperty>
 *       payload: PhysicalRelation (relación entre propiedades físicas)
 *
 *   DependencyGraph<PropertyKey<?>>   → GraphEdge<PropertyKey<?>>
 *       payload: PropertyDependency (transformación de gameplay)
 *
 * ── NODO DESTINO OPCIONAL ────────────────────────────────────────────────
 * El nodo destino puede ser null. Esto representa relaciones que producen
 * efectos sin modificar un nodo específico del grafo (por ejemplo, una
 * relación física cuya consecuencia es observable por el Gameplay pero no
 * modifica ninguna propiedad del núcleo directamente).
 *
 * ── INMUTABILIDAD ────────────────────────────────────────────────────────
 * GraphEdge es completamente inmutable tras construcción.
 *
 * @param <T> tipo de nodo del grafo al que pertenece esta arista.
 */
public final class GraphEdge<T> {

    /** Nodo de origen. Nunca null. */
    private final T source;

    /**
     * Nodo de destino. Puede ser null para relaciones sin nodo destino
     * explícito (efectos sin dominio físico concreto).
     */
    private final T target;

    /**
     * Carga útil de la arista: el objeto que implementa la semántica de la
     * relación. Su tipo concreto depende de la especialización del grafo.
     * Nunca null.
     */
    private final Object payload;

    /**
     * Identificador de la arista para remoción eficiente por grupo.
     * Por convención: "sistema_concepto", ej: "thermal_causes_pressure".
     * Nunca null ni vacío.
     */
    private final String tag;

    /**
     * Prioridad de evaluación. Menor valor = se evalúa antes.
     * Usado por DependencyGraph para ordenar allEdges().
     */
    private final int priority;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private GraphEdge(Builder<T> b) {
        if (b.source == null) throw new IllegalArgumentException("source no puede ser null");
        if (b.payload == null) throw new IllegalArgumentException("payload no puede ser null");
        if (b.tag == null || b.tag.isBlank())
            throw new IllegalArgumentException("tag no puede ser vacío");

        this.source   = b.source;
        this.target   = b.target;
        this.payload  = b.payload;
        this.tag      = b.tag;
        this.priority = b.priority;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static <T> Builder<T> builder() { return new Builder<>(); }

    /**
     * Crea una arista entre dos nodos con prioridad por defecto (100).
     *
     * @param source  nodo de origen.
     * @param target  nodo de destino (puede ser null).
     * @param payload carga útil de la relación.
     * @param tag     identificador del grupo.
     * @param <T>     tipo de nodo.
     */
    public static <T> GraphEdge<T> of(T source, T target, Object payload, String tag) {
        return GraphEdge.<T>builder()
            .source(source)
            .target(target)
            .payload(payload)
            .tag(tag)
            .build();
    }

    /**
     * Crea una arista de origen sin nodo destino explícito.
     * Usar para relaciones que producen efectos sin dominio destino concreto.
     *
     * @param source  nodo de origen.
     * @param payload carga útil de la relación.
     * @param tag     identificador del grupo.
     * @param <T>     tipo de nodo.
     */
    public static <T> GraphEdge<T> ofSource(T source, Object payload, String tag) {
        return of(source, null, payload, tag);
    }

    /**
     * Crea una arista entre dos nodos con prioridad explícita.
     *
     * @param source   nodo de origen.
     * @param target   nodo de destino (puede ser null).
     * @param payload  carga útil de la relación.
     * @param tag      identificador del grupo.
     * @param priority prioridad de evaluación.
     * @param <T>      tipo de nodo.
     */
    public static <T> GraphEdge<T> of(T source, T target, Object payload, String tag, int priority) {
        return GraphEdge.<T>builder()
            .source(source)
            .target(target)
            .payload(payload)
            .tag(tag)
            .priority(priority)
            .build();
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Nodo de origen. Nunca null. */
    public T getSource()    { return source; }

    /** Nodo de destino. Puede ser null. */
    public T getTarget()    { return target; }

    /** True si esta arista tiene nodo destino explícito. */
    public boolean hasTarget() { return target != null; }

    /**
     * Carga útil de la arista. Nunca null.
     * Usar {@link #getPayloadAs(Class)} para acceder de forma tipada.
     */
    public Object getPayload() { return payload; }

    /**
     * Retorna la carga útil casteada al tipo indicado.
     *
     * @param type clase del tipo esperado.
     * @param <P>  tipo de la carga útil.
     * @return payload casteado.
     * @throws ClassCastException si el tipo no coincide.
     */
    public <P> P getPayloadAs(Class<P> type) {
        return type.cast(payload);
    }

    /** Identificador de la arista. */
    public String getTag()    { return tag; }

    /** Prioridad de evaluación. */
    public int getPriority()  { return priority; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        String src = source != null ? source.toString() : "?";
        String dst = target != null ? target.toString() : "Ø";
        return "GraphEdge[" + src + " → " + dst + " tag=" + tag
            + " priority=" + priority + "]";
    }

    // ── Builder ───────────────────────────────────────────────────────────

    /**
     * Builder de GraphEdge<T>.
     *
     * @param <T> tipo de nodo.
     */
    public static final class Builder<T> {

        private T      source;
        private T      target;
        private Object payload;
        private String tag;
        private int    priority = 100;

        private Builder() {}

        /** Nodo de origen. Obligatorio. */
        public Builder<T> source(T s)      { this.source  = s; return this; }

        /** Nodo de destino. Opcional (null = sin dominio destino explícito). */
        public Builder<T> target(T t)      { this.target  = t; return this; }

        /** Carga útil. Obligatoria. */
        public Builder<T> payload(Object p){ this.payload = p; return this; }

        /** Tag de identificación. Obligatorio. */
        public Builder<T> tag(String t)    { this.tag     = t; return this; }

        /** Prioridad de evaluación (default: 100). */
        public Builder<T> priority(int p)  { this.priority= p; return this; }

        /** Construye la arista. */
        public GraphEdge<T> build()        { return new GraphEdge<>(this); }
    }
}
