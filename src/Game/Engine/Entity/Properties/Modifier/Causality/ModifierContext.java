package Game.Engine.Entity.Properties.Modifier.Causality;

import Game.Engine.Entity.Capabilities.CapabilityComponent;
import Game.Engine.Entity.Properties.PropertyKey;
import Game.Engine.Entity.Tags.TagComponent;
import Game.Engine.GameObjects;

/**
 * Contexto rico de resolución de un modificador de propiedad.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * ModifierContext agrega toda la información disponible en el momento en que
 * un modificador es evaluado. Es el objeto que reciben ModifierPredicate e
 * ModifierInfluence para tomar decisiones sin conocer ningún tipo concreto.
 *
 * Responde a preguntas como:
 *   "¿Quién originó esta modificación?"
 *   "¿Sobre qué entidad está actuando?"
 *   "¿Qué propiedad se está modificando?"
 *   "¿Cuál es el valor base y el valor actual en este punto del pipeline?"
 *   "¿Tiene la entidad destino el tag ORGANIC?"
 *   "¿En qué frame ocurre esto?"
 *
 * ── DISEÑO: STATELESS E INMUTABLE ────────────────────────────────────────
 * ModifierContext es completamente inmutable una vez construido.
 * Construir vía {@link Builder}.
 *
 * ── COMPATIBILIDAD ────────────────────────────────────────────────────────
 * PropertyResolver construye un ModifierContext mínimo antes de evaluar
 * cada modificador. Un modificador sin predicado usa ALWAYS, que ignora
 * el contexto por completo.
 */
public final class ModifierContext {

    // ── Participantes ─────────────────────────────────────────────────────

    private final GameObjects source;
    private final GameObjects target;

    // ── Propiedad ─────────────────────────────────────────────────────────

    private final PropertyKey<?> property;
    private final double baseValue;
    private final double currentValue;

    // ── Causalidad ────────────────────────────────────────────────────────

    private final ModifierSource modifierSource;
    private final ModifierScope  modifierScope;

    // ── Semántica de la entidad destino ───────────────────────────────────

    private final TagComponent         tags;
    private final CapabilityComponent  capabilities;

    // ── Evento causal ─────────────────────────────────────────────────────

    private final Object triggeringEvent;   // GameplayEvent — tipado genérico para evitar dependencia circular

    // ── Contexto de resolución ────────────────────────────────────────────

    private final Object resolutionContext; // ResolutionContext — tipado genérico para evitar dependencia circular

    // ── Tiempo ────────────────────────────────────────────────────────────

    private final long timestamp;

    // ── Constructor privado (usar Builder) ────────────────────────────────

    private ModifierContext(Builder b) {
        this.source            = b.source;
        this.target            = b.target;
        this.property          = b.property;
        this.baseValue         = b.baseValue;
        this.currentValue      = b.currentValue;
        this.modifierSource    = b.modifierSource;
        this.modifierScope     = b.modifierScope;
        this.tags              = b.tags;
        this.capabilities      = b.capabilities;
        this.triggeringEvent   = b.triggeringEvent;
        this.resolutionContext  = b.resolutionContext;
        this.timestamp         = b.timestamp;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    /**
     * Crea un contexto mínimo solo con la propiedad y sus valores.
     * Usado internamente por PropertyResolver cuando no hay información
     * adicional disponible.
     */
    public static ModifierContext minimal(PropertyKey<?> property, double baseValue, double currentValue) {
        return builder()
            .property(property)
            .baseValue(baseValue)
            .currentValue(currentValue)
            .build();
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    public GameObjects getSource()                 { return source; }
    public GameObjects getTarget()                 { return target; }
    public PropertyKey<?> getProperty()            { return property; }
    public double getBaseValue()                   { return baseValue; }
    public double getCurrentValue()                { return currentValue; }
    public ModifierSource getModifierSource()      { return modifierSource; }
    public ModifierScope getModifierScope()        { return modifierScope; }
    public TagComponent getTags()                  { return tags; }
    public CapabilityComponent getCapabilities()   { return capabilities; }
    public Object getTriggeringEvent()             { return triggeringEvent; }
    public Object getResolutionContext()           { return resolutionContext; }
    public long getTimestamp()                     { return timestamp; }

    // ── Variante actualizada ──────────────────────────────────────────────

    /**
     * Retorna una copia de este contexto con el currentValue actualizado.
     * El PropertyResolver lo usa para reflejar el valor tras cada fase.
     */
    public ModifierContext withCurrentValue(double newCurrentValue) {
        return builder()
            .source(source).target(target).property(property)
            .baseValue(baseValue).currentValue(newCurrentValue)
            .modifierSource(modifierSource).modifierScope(modifierScope)
            .tags(tags).capabilities(capabilities)
            .triggeringEvent(triggeringEvent)
            .resolutionContext(resolutionContext)
            .timestamp(timestamp)
            .build();
    }

    @Override
    public String toString() {
        return "ModifierContext["
            + "property=" + (property != null ? property.displayName() : "null")
            + ", base=" + baseValue
            + ", current=" + currentValue
            + ", source=" + (modifierSource != null ? modifierSource.displayName() : "null")
            + ", scope="  + (modifierScope  != null ? modifierScope.displayName()  : "null")
            + ", t=" + timestamp
            + "]";
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    public static final class Builder {

        private GameObjects        source;
        private GameObjects        target;
        private PropertyKey<?>     property;
        private double             baseValue;
        private double             currentValue;
        private ModifierSource     modifierSource;
        private ModifierScope      modifierScope;
        private TagComponent       tags;
        private CapabilityComponent capabilities;
        private Object             triggeringEvent;
        private Object             resolutionContext;
        private long               timestamp;

        private Builder() {}

        public Builder source(GameObjects v)             { this.source = v; return this; }
        public Builder target(GameObjects v)             { this.target = v; return this; }
        public Builder property(PropertyKey<?> v)        { this.property = v; return this; }
        public Builder baseValue(double v)               { this.baseValue = v; return this; }
        public Builder currentValue(double v)            { this.currentValue = v; return this; }
        public Builder modifierSource(ModifierSource v)  { this.modifierSource = v; return this; }
        public Builder modifierScope(ModifierScope v)    { this.modifierScope = v; return this; }
        public Builder tags(TagComponent v)              { this.tags = v; return this; }
        public Builder capabilities(CapabilityComponent v) { this.capabilities = v; return this; }
        public Builder triggeringEvent(Object v)         { this.triggeringEvent = v; return this; }
        public Builder resolutionContext(Object v)       { this.resolutionContext = v; return this; }
        public Builder timestamp(long v)                 { this.timestamp = v; return this; }

        public ModifierContext build() {
            if (property == null) {
                throw new IllegalStateException("ModifierContext requiere un PropertyKey (property no puede ser null).");
            }
            return new ModifierContext(this);
        }
    }
}
