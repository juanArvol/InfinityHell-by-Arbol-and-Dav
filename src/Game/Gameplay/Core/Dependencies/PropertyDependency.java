package Game.Gameplay.Core.Dependencies;

import Game.Gameplay.Core.Properties.PropertyKey;

/**
 * Arista dirigida del grafo de dependencias entre propiedades.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * PropertyDependency representa una sola relación:
 *
 *   "La propiedad A influye sobre la propiedad B"
 *
 * Una PropertyDependency describe:
 *   - Propiedad origen (fuente del cambio)
 *   - Propiedad destino (receptora del efecto)
 *   - Transformación aplicada (cómo se calcula el delta de B dado el cambio de A)
 *   - Condición de propagación (cuándo debe activarse esta dependencia)
 *   - Prioridad (si múltiples dependencias afectan la misma propiedad destino)
 *   - Tag de identificación (para dar de baja la dependencia del grafo)
 *
 * ── POR QUÉ ES UN OBJETO Y NO UN PAR DE STRINGS ──────────────────────────
 * Usar pares de strings ("Temperature", "Speed") impide:
 *   - Asociar transformaciones ricas a la relación.
 *   - Asociar condiciones de activación a la relación.
 *   - Priorizar entre múltiples dependencias del mismo destino.
 *   - Dar de baja una dependencia específica entre las que afectan B.
 *   - Serializar/inspeccionar la relación para debugging.
 *
 * PropertyDependency es un objeto de primera clase que encapsula todo
 * lo necesario para describir la arista de forma completa.
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * PropertyDependency es completamente inmutable. Construir mediante Builder
 * o usando los factory methods de conveniencia.
 *
 * ── EJEMPLOS ─────────────────────────────────────────────────────────────
 *
 *   // Temperature → MovementSpeed: cada grado de baja reduce velocidad en 0.5%
 *   PropertyDependency tempToSpeed = PropertyDependency.builder()
 *       .from(PropertyKeys.TEMPERATURE)
 *       .to(PropertyKeys.SPEED)
 *       .transform(DependencyTransform.linear(-0.005))
 *       .condition(DependencyCondition.ON_CHANGE)
 *       .priority(100)
 *       .tag("temperature_affects_speed")
 *       .build();
 *
 *   // Atajo para dependencia lineal simple:
 *   PropertyDependency dep = PropertyDependency.linear(
 *       PropertyKeys.TEMPERATURE, PropertyKeys.SPEED,
 *       -0.005, "temperature_affects_speed"
 *   );
 *
 * @see PropertyDependencyGraph
 * @see DependencyTransform
 * @see DependencyCondition
 */
public final class PropertyDependency {

    // ── Campos ────────────────────────────────────────────────────────────

    /** Propiedad que origina el cambio. Nunca null. */
    private final PropertyKey<?> sourceKey;

    /** Propiedad que recibe el efecto. Nunca null. */
    private final PropertyKey<?> targetKey;

    /**
     * Transformación que calcula el delta del destino dado el cambio del origen.
     * Nunca null — si no se especifica, se usa DependencyTransform.DIRECT.
     */
    private final DependencyTransform transform;

    /**
     * Condición bajo la cual esta dependencia propaga.
     * Nunca null — si no se especifica, se usa DependencyCondition.ON_CHANGE.
     */
    private final DependencyCondition condition;

    /**
     * Prioridad de esta dependencia. Menor = se aplica primero.
     * Relevante cuando múltiples dependencias apuntan al mismo destino
     * desde el mismo origen.
     */
    private final int priority;

    /**
     * Identificador único para poder dar de baja esta dependencia del grafo.
     * Por convención: "system_concept", ej: "temperature_affects_speed".
     */
    private final String tag;

    // ── Constructor privado ───────────────────────────────────────────────

    private PropertyDependency(Builder b) {
        if (b.sourceKey == null)
            throw new IllegalArgumentException("PropertyDependency requiere sourceKey no null.");
        if (b.targetKey == null)
            throw new IllegalArgumentException("PropertyDependency requiere targetKey no null.");
        if (b.sourceKey.id().equals(b.targetKey.id()))
            throw new IllegalArgumentException(
                "sourceKey y targetKey no pueden ser la misma propiedad: '"
                + b.sourceKey.id() + "'. Una propiedad no puede depender de sí misma.");
        if (b.tag == null || b.tag.isBlank())
            throw new IllegalArgumentException("PropertyDependency requiere un tag no vacío.");

        this.sourceKey  = b.sourceKey;
        this.targetKey  = b.targetKey;
        this.transform  = (b.transform  != null) ? b.transform  : DependencyTransform.DIRECT;
        this.condition  = (b.condition  != null) ? b.condition  : DependencyCondition.ON_CHANGE;
        this.priority   = b.priority;
        this.tag        = b.tag;
    }

    // ── Factory methods de conveniencia ───────────────────────────────────

    /**
     * Punto de entrada del Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Crea una dependencia lineal simple: cuando A cambia, B cambia en
     * {@code factor} × delta(A).
     *
     * @param from   propiedad origen
     * @param to     propiedad destino
     * @param factor multiplicador del delta (ej: -0.005 para un 0.5% inverso)
     * @param tag    identificador de la dependencia
     */
    public static PropertyDependency linear(
            PropertyKey<?> from,
            PropertyKey<?> to,
            double factor,
            String tag) {
        return builder()
            .from(from)
            .to(to)
            .transform(DependencyTransform.linear(factor))
            .condition(DependencyCondition.ON_CHANGE)
            .tag(tag)
            .build();
    }

    /**
     * Crea una dependencia directa: cuando A cambia en X, B también cambia en X.
     *
     * @param from propiedad origen
     * @param to   propiedad destino
     * @param tag  identificador de la dependencia
     */
    public static PropertyDependency direct(
            PropertyKey<?> from,
            PropertyKey<?> to,
            String tag) {
        return builder()
            .from(from)
            .to(to)
            .transform(DependencyTransform.DIRECT)
            .condition(DependencyCondition.ON_CHANGE)
            .tag(tag)
            .build();
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Propiedad que origina el cambio. */
    public PropertyKey<?> getSourceKey()           { return sourceKey; }

    /** Propiedad que recibe el efecto. */
    public PropertyKey<?> getTargetKey()           { return targetKey; }

    /** Transformación que calcula el delta del destino. */
    public DependencyTransform getTransform()      { return transform; }

    /** Condición de activación de esta dependencia. */
    public DependencyCondition getCondition()      { return condition; }

    /** Prioridad de esta dependencia (menor = primero). */
    public int getPriority()                       { return priority; }

    /** Tag de identificación para dar de baja del grafo. */
    public String getTag()                         { return tag; }

    // ── Evaluación ────────────────────────────────────────────────────────

    /**
     * True si esta dependencia debe propagarse dados los valores de la propiedad origen.
     *
     * @param previousValue valor anterior de la propiedad origen
     * @param newValue      valor nuevo de la propiedad origen
     * @return true si la condición de esta dependencia se cumple
     */
    public boolean shouldPropagate(double previousValue, double newValue) {
        return condition.shouldPropagate(sourceKey, previousValue, newValue, targetKey);
    }

    /**
     * Calcula el delta que debe aplicarse a la propiedad destino.
     *
     * @param previousValue valor anterior de la propiedad origen
     * @param newValue      valor nuevo de la propiedad origen
     * @return delta a aplicar en la propiedad destino
     */
    public double computeDelta(double previousValue, double newValue) {
        return transform.compute(previousValue, newValue);
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "PropertyDependency["
            + sourceKey.id() + " → " + targetKey.id()
            + " tag=" + tag
            + " priority=" + priority
            + "]";
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de PropertyDependency.
     *
     * Campos obligatorios: from (sourceKey), to (targetKey), tag.
     * Campos opcionales: transform (default: DIRECT), condition (default: ON_CHANGE),
     *                    priority (default: 500).
     */
    public static final class Builder {

        private PropertyKey<?>    sourceKey;
        private PropertyKey<?>    targetKey;
        private DependencyTransform transform;
        private DependencyCondition condition;
        private int               priority = 500;
        private String            tag;

        private Builder() {}

        /** Propiedad origen de la dependencia. */
        public Builder from(PropertyKey<?> sourceKey)              { this.sourceKey = sourceKey; return this; }

        /** Propiedad destino de la dependencia. */
        public Builder to(PropertyKey<?> targetKey)                { this.targetKey = targetKey; return this; }

        /** Transformación a aplicar. Por defecto: DependencyTransform.DIRECT. */
        public Builder transform(DependencyTransform transform)    { this.transform = transform; return this; }

        /** Condición de activación. Por defecto: DependencyCondition.ON_CHANGE. */
        public Builder condition(DependencyCondition condition)    { this.condition = condition; return this; }

        /** Prioridad (menor = primero). Por defecto: 500. */
        public Builder priority(int priority)                      { this.priority = priority; return this; }

        /** Tag de identificación. Obligatorio. */
        public Builder tag(String tag)                             { this.tag = tag; return this; }

        /** Construye la PropertyDependency. */
        public PropertyDependency build() {
            return new PropertyDependency(this);
        }
    }
}
