package Game.Engine.Entity.Properties.Dependencies;

import Game.Engine.Entity.Properties.PropertyKey;

/**
 * Arista dirigida del grafo de dependencias entre propiedades.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * PropertyDependency representa una sola relación:
 *   "La propiedad A influye sobre la propiedad B"
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * PropertyDependency es completamente inmutable.
 * Construir mediante Builder o factory methods de conveniencia.
 *
 * @see PropertyDependencyGraph
 * @see DependencyTransform
 * @see DependencyCondition
 */
public final class PropertyDependency {

    private final PropertyKey<?>      sourceKey;
    private final PropertyKey<?>      targetKey;
    private final DependencyTransform transform;
    private final DependencyCondition condition;
    private final int                 priority;
    private final String              tag;

    private PropertyDependency(Builder b) {
        if (b.sourceKey == null)
            throw new IllegalArgumentException("PropertyDependency requiere sourceKey no null.");
        if (b.targetKey == null)
            throw new IllegalArgumentException("PropertyDependency requiere targetKey no null.");
        if (b.sourceKey == b.targetKey)
            throw new IllegalArgumentException(
                "sourceKey y targetKey no pueden ser la misma propiedad: '"
                + b.sourceKey.displayName() + "'.");
        if (b.tag == null || b.tag.isBlank())
            throw new IllegalArgumentException("PropertyDependency requiere un tag no vacío.");

        this.sourceKey = b.sourceKey;
        this.targetKey = b.targetKey;
        this.transform = (b.transform != null) ? b.transform : DependencyTransform.DIRECT;
        this.condition = (b.condition != null) ? b.condition : DependencyCondition.ON_CHANGE;
        this.priority  = b.priority;
        this.tag       = b.tag;
    }

    // ── Factory methods ───────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static PropertyDependency linear(
            PropertyKey<?> from, PropertyKey<?> to, double factor, String tag) {
        return builder()
            .from(from).to(to)
            .transform(DependencyTransform.linear(factor))
            .condition(DependencyCondition.ON_CHANGE)
            .tag(tag).build();
    }

    public static PropertyDependency direct(
            PropertyKey<?> from, PropertyKey<?> to, String tag) {
        return builder()
            .from(from).to(to)
            .transform(DependencyTransform.DIRECT)
            .condition(DependencyCondition.ON_CHANGE)
            .tag(tag).build();
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    public PropertyKey<?> getSourceKey()           { return sourceKey; }
    public PropertyKey<?> getTargetKey()           { return targetKey; }
    public DependencyTransform getTransform()      { return transform; }
    public DependencyCondition getCondition()      { return condition; }
    public int getPriority()                       { return priority; }
    public String getTag()                         { return tag; }

    // ── Evaluación ────────────────────────────────────────────────────────

    public boolean shouldPropagate(double previousValue, double newValue) {
        return condition.shouldPropagate(sourceKey, previousValue, newValue, targetKey);
    }

    public double computeDelta(double previousValue, double newValue) {
        return transform.compute(previousValue, newValue);
    }

    @Override
    public String toString() {
        return "PropertyDependency["
            + sourceKey.displayName() + " → " + targetKey.displayName()
            + " tag=" + tag
            + " priority=" + priority
            + "]";
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    public static final class Builder {

        private PropertyKey<?>    sourceKey;
        private PropertyKey<?>    targetKey;
        private DependencyTransform transform;
        private DependencyCondition condition;
        private int               priority = 500;
        private String            tag;

        private Builder() {}

        public Builder from(PropertyKey<?> v)               { this.sourceKey = v; return this; }
        public Builder to(PropertyKey<?> v)                 { this.targetKey = v; return this; }
        public Builder transform(DependencyTransform v)     { this.transform = v; return this; }
        public Builder condition(DependencyCondition v)     { this.condition = v; return this; }
        public Builder priority(int v)                      { this.priority = v; return this; }
        public Builder tag(String v)                        { this.tag = v; return this; }

        public PropertyDependency build() { return new PropertyDependency(this); }
    }
}
