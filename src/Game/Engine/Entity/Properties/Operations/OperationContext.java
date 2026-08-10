package Game.Engine.Entity.Properties.Operations;

import Game.Engine.Entity.Properties.Modifier.Causality.CausalNode;
import Game.Engine.Entity.Properties.Modifier.Causality.ModifierContext;
import Game.Engine.Entity.Properties.PropertyKey;
import Game.Engine.GameObjects;

/**
 * Contexto completo disponible en el momento en que una GameplayOperation se ejecuta.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * OperationContext agrega toda la información relevante para que una
 * GameplayOperation pueda decidir qué hace y sobre quién, sin necesidad de
 * conocer ninguna clase concreta del juego.
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * OperationContext es completamente inmutable una vez construido.
 * Construir mediante {@link Builder}.
 *
 * @see GameplayOperation
 * @see OperationPredicate
 */
public final class OperationContext {

    private final GameObjects     source;
    private final GameObjects     target;
    private final PropertyKey<?>  affectedProperty;
    private final double          previousValue;
    private final double          finalValue;
    private final Object          triggeringEvent;    // GameplayEvent (Object para evitar dep. circular)
    private final ModifierContext modifierContext;
    private final Object          resolutionContext;  // ResolutionContext (Object para evitar dep. circular)
    private final CausalNode      causalNode;
    private final long            timestamp;

    private OperationContext(Builder b) {
        this.source            = b.source;
        this.target            = b.target;
        this.affectedProperty  = b.affectedProperty;
        this.previousValue     = b.previousValue;
        this.finalValue        = b.finalValue;
        this.triggeringEvent   = b.triggeringEvent;
        this.modifierContext   = b.modifierContext;
        this.resolutionContext = b.resolutionContext;
        this.causalNode        = b.causalNode;
        this.timestamp         = b.timestamp;
    }

    public static Builder builder() { return new Builder(); }

    public static OperationContext minimal(
            GameObjects source, GameObjects target, PropertyKey<?> property,
            double previousValue, double finalValue, long timestamp) {
        return builder()
            .source(source).target(target).affectedProperty(property)
            .previousValue(previousValue).finalValue(finalValue)
            .timestamp(timestamp).build();
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    public GameObjects getSource()                 { return source; }
    public GameObjects getTarget()                 { return target; }
    public PropertyKey<?> getAffectedProperty()    { return affectedProperty; }
    public double getPreviousValue()               { return previousValue; }
    public double getFinalValue()                  { return finalValue; }
    public double getDelta()                       { return finalValue - previousValue; }
    public Object getTriggeringEvent()             { return triggeringEvent; }
    public ModifierContext getModifierContext()     { return modifierContext; }
    public Object getResolutionContext()           { return resolutionContext; }
    public CausalNode getCausalNode()              { return causalNode; }
    public long getTimestamp()                     { return timestamp; }

    @Override
    public String toString() {
        return "OperationContext["
            + "property=" + (affectedProperty != null ? affectedProperty.displayName() : "null")
            + ", prev=" + previousValue
            + ", final=" + finalValue
            + ", t=" + timestamp
            + "]";
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    public static final class Builder {

        private GameObjects       source;
        private GameObjects       target;
        private PropertyKey<?>    affectedProperty;
        private double            previousValue;
        private double            finalValue;
        private Object            triggeringEvent;
        private ModifierContext   modifierContext;
        private Object            resolutionContext;
        private CausalNode        causalNode;
        private long              timestamp;

        private Builder() {}

        public Builder source(GameObjects v)              { this.source = v; return this; }
        public Builder target(GameObjects v)              { this.target = v; return this; }
        public Builder affectedProperty(PropertyKey<?> v) { this.affectedProperty = v; return this; }
        public Builder previousValue(double v)            { this.previousValue = v; return this; }
        public Builder finalValue(double v)               { this.finalValue = v; return this; }
        public Builder triggeringEvent(Object v)          { this.triggeringEvent = v; return this; }
        public Builder modifierContext(ModifierContext v)  { this.modifierContext = v; return this; }
        public Builder resolutionContext(Object v)        { this.resolutionContext = v; return this; }
        public Builder causalNode(CausalNode v)           { this.causalNode = v; return this; }
        public Builder timestamp(long v)                  { this.timestamp = v; return this; }

        public OperationContext build() { return new OperationContext(this); }
    }
}
