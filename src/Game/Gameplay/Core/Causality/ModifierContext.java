package Game.Gameplay.Core.Causality;

import Game.Engine.GameObjects;
import Game.Gameplay.Core.Capabilities.CapabilityComponent;
import Game.Gameplay.Core.Events.GameplayEvent;
import Game.Gameplay.Core.Properties.PropertyKey;
import Game.Gameplay.Core.Resolution.ResolutionContext;
import Game.Gameplay.Core.Tags.TagComponent;

/**
 * Contexto rico de resolución de un modificador.
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
 *   "¿Qué capacidades tiene la entidad origen?"
 *   "¿En qué frame ocurre esto?"
 *   "¿Hay un evento activo que causó esta modificación?"
 *
 * ── DISEÑO: STATELESS E INMUTABLE ────────────────────────────────────────
 * ModifierContext es completamente inmutable una vez construido.
 * No contiene referencias a sistemas concretos — solo a las abstracciones
 * del núcleo (GameObjects, TagComponent, CapabilityComponent, etc.).
 *
 * Construir vía {@link Builder}:
 *
 *   ModifierContext ctx = ModifierContext.builder()
 *       .source(attackerEntity)
 *       .target(defenderEntity)
 *       .property(PropertyKeys.DAMAGE)
 *       .baseValue(20.0)
 *       .currentValue(28.0)          // tras aditivos ya aplicados
 *       .modifierSource(WeaponSources.WEAPON)
 *       .modifierScope(CoreScopes.TARGET)
 *       .tags(targetTags)
 *       .capabilities(targetCaps)
 *       .resolutionContext(ctx)
 *       .timestamp(currentFrame)
 *       .build();
 *
 * ── CAMPOS OPCIONALES ─────────────────────────────────────────────────────
 * Todos los campos son opcionales salvo property, baseValue y currentValue.
 * Los campos ausentes retornan null o 0. Los predicados y las influencias
 * deben comprobar la nulidad antes de usarlos.
 *
 * ── COMPATIBILIDAD CON CFCC-001 ───────────────────────────────────────────
 * PropertyResolver construye un ModifierContext mínimo antes de evaluar
 * cada modificador. Un modificador sin predicado usa ALWAYS, que ignora
 * el contexto por completo, manteniendo el comportamiento de CFCC-001.
 */
public final class ModifierContext {

    // ── Participantes ─────────────────────────────────────────────────────

    /** Entidad que origina la modificación (puede ser null para efectos de entorno). */
    private final GameObjects source;

    /** Entidad que recibe la modificación (puede ser null en modificaciones globales). */
    private final GameObjects target;

    // ── Propiedad ─────────────────────────────────────────────────────────

    /** Propiedad que está siendo modificada. Nunca null. */
    private final PropertyKey<?> property;

    /** Valor base de la propiedad antes de cualquier modificador. */
    private final double baseValue;

    /**
     * Valor actual de la propiedad en este punto del pipeline.
     * Al comenzar la resolución es igual a baseValue.
     * Después de los aditivos refleja base + suma.
     * Los predicados e influencias pueden leer este valor para decisiones
     * condicionales ("solo si el daño supera X").
     */
    private final double currentValue;

    // ── Causalidad ────────────────────────────────────────────────────────

    /** Origen semántico del modificador (ej: "Weapon", "Spell.Fire"). */
    private final ModifierSource modifierSource;

    /** Alcance del modificador (ej: "Target", "Projectile"). */
    private final ModifierScope modifierScope;

    // ── Semántica de la entidad destino ───────────────────────────────────

    /** Tags de la entidad destino. Puede ser null si no tiene TagComponent. */
    private final TagComponent tags;

    /** Capacidades de la entidad destino. Puede ser null si no tiene CapabilityComponent. */
    private final CapabilityComponent capabilities;

    // ── Evento causal ─────────────────────────────────────────────────────

    /**
     * Evento de gameplay que originó esta cadena de modificación.
     * Por ejemplo, un OnDamage que disparó la resolución de modificadores
     * de daño. Puede ser null si la modificación no fue iniciada por un evento.
     */
    private final GameplayEvent triggeringEvent;

    // ── Contexto de resolución ────────────────────────────────────────────

    /**
     * Contexto de resolución de la entidad origen, si está disponible.
     * Permite que un predicado consulte otras propiedades del origen.
     */
    private final ResolutionContext resolutionContext;

    // ── Tiempo ────────────────────────────────────────────────────────────

    /**
     * Timestamp de la resolución en frames o unidades de tiempo del juego.
     * Permite predicados temporales: "solo aplica durante los primeros 30 frames".
     */
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

    /**
     * Punto de entrada del Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Crea un contexto mínimo solo con la propiedad y sus valores.
     * Usado internamente por PropertyResolver cuando no hay información
     * adicional disponible. Garantiza compatibilidad con CFCC-001.
     *
     * @param property     propiedad que se está resolviendo
     * @param baseValue    valor base de la propiedad
     * @param currentValue valor actual en este punto del pipeline
     */
    public static ModifierContext minimal(PropertyKey<?> property, double baseValue, double currentValue) {
        return builder()
            .property(property)
            .baseValue(baseValue)
            .currentValue(currentValue)
            .build();
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Entidad origen de la modificación, o null. */
    public GameObjects getSource()                  { return source; }

    /** Entidad destino de la modificación, o null. */
    public GameObjects getTarget()                  { return target; }

    /** Propiedad que está siendo modificada. */
    public PropertyKey<?> getProperty()             { return property; }

    /** Valor base de la propiedad (sin modificadores). */
    public double getBaseValue()                    { return baseValue; }

    /**
     * Valor actual de la propiedad en el momento de evaluar este modificador.
     * Refleja las transformaciones ya aplicadas en el pipeline hasta este punto.
     */
    public double getCurrentValue()                 { return currentValue; }

    /** Origen semántico del modificador, o null. */
    public ModifierSource getModifierSource()       { return modifierSource; }

    /** Alcance del modificador, o null. */
    public ModifierScope getModifierScope()         { return modifierScope; }

    /** Tags de la entidad destino, o null si no tiene TagComponent. */
    public TagComponent getTags()                   { return tags; }

    /** Capacidades de la entidad destino, o null si no tiene CapabilityComponent. */
    public CapabilityComponent getCapabilities()    { return capabilities; }

    /** Evento que originó esta cadena de modificación, o null. */
    public GameplayEvent getTriggeringEvent()        { return triggeringEvent; }

    /** Contexto de resolución del origen, o null. */
    public ResolutionContext getResolutionContext()  { return resolutionContext; }

    /** Timestamp en frames. 0 si no fue establecido. */
    public long getTimestamp()                      { return timestamp; }

    // ── Variante actualizada ──────────────────────────────────────────────

    /**
     * Retorna una copia de este contexto con el currentValue actualizado.
     * El PropertyResolver lo usa para reflejar el valor tras cada fase del pipeline.
     *
     * @param newCurrentValue nuevo valor actual
     * @return nuevo ModifierContext con currentValue actualizado; resto idéntico
     */
    public ModifierContext withCurrentValue(double newCurrentValue) {
        return builder()
            .source(source)
            .target(target)
            .property(property)
            .baseValue(baseValue)
            .currentValue(newCurrentValue)
            .modifierSource(modifierSource)
            .modifierScope(modifierScope)
            .tags(tags)
            .capabilities(capabilities)
            .triggeringEvent(triggeringEvent)
            .resolutionContext(resolutionContext)
            .timestamp(timestamp)
            .build();
    }

    // ── toString ─────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "ModifierContext["
            + "property=" + (property != null ? property.id() : "null")
            + ", base=" + baseValue
            + ", current=" + currentValue
            + ", source=" + (modifierSource != null ? modifierSource.id() : "null")
            + ", scope="  + (modifierScope  != null ? modifierScope.id()  : "null")
            + ", t=" + timestamp
            + "]";
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de ModifierContext.
     *
     * Todos los campos son opcionales salvo property.
     * baseValue y currentValue por defecto son 0.0.
     */
    public static final class Builder {

        private GameObjects       source;
        private GameObjects       target;
        private PropertyKey<?>    property;
        private double            baseValue;
        private double            currentValue;
        private ModifierSource    modifierSource;
        private ModifierScope     modifierScope;
        private TagComponent      tags;
        private CapabilityComponent capabilities;
        private GameplayEvent     triggeringEvent;
        private ResolutionContext resolutionContext;
        private long              timestamp;

        private Builder() {}

        public Builder source(GameObjects source)                        { this.source = source; return this; }
        public Builder target(GameObjects target)                        { this.target = target; return this; }
        public Builder property(PropertyKey<?> property)                 { this.property = property; return this; }
        public Builder baseValue(double baseValue)                       { this.baseValue = baseValue; return this; }
        public Builder currentValue(double currentValue)                 { this.currentValue = currentValue; return this; }
        public Builder modifierSource(ModifierSource modifierSource)     { this.modifierSource = modifierSource; return this; }
        public Builder modifierScope(ModifierScope modifierScope)        { this.modifierScope = modifierScope; return this; }
        public Builder tags(TagComponent tags)                           { this.tags = tags; return this; }
        public Builder capabilities(CapabilityComponent capabilities)    { this.capabilities = capabilities; return this; }
        public Builder triggeringEvent(GameplayEvent triggeringEvent)    { this.triggeringEvent = triggeringEvent; return this; }
        public Builder resolutionContext(ResolutionContext ctx)           { this.resolutionContext = ctx; return this; }
        public Builder timestamp(long timestamp)                         { this.timestamp = timestamp; return this; }

        /**
         * Construye el ModifierContext.
         *
         * @throws IllegalStateException si property es null
         */
        public ModifierContext build() {
            if (property == null) {
                throw new IllegalStateException("ModifierContext requiere un PropertyKey (property no puede ser null).");
            }
            return new ModifierContext(this);
        }
    }
}
