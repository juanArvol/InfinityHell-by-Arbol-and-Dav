package Game.Gameplay.Core.Operations;

import Game.Engine.GameObjects;
import Game.Gameplay.Core.Causality.CausalNode;
import Game.Gameplay.Core.Causality.ModifierContext;
import Game.Gameplay.Core.Events.GameplayEvent;
import Game.Gameplay.Core.Properties.PropertyKey;
import Game.Gameplay.Core.Resolution.ResolutionContext;

/**
 * Contexto completo disponible en el momento en que una GameplayOperation se ejecuta.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * OperationContext agrega toda la información relevante para que una
 * GameplayOperation pueda decidir qué hace y sobre quién, sin necesidad de
 * conocer ninguna clase concreta del juego.
 *
 * Responde a preguntas como:
 *   "¿Quién originó esta consecuencia?"
 *   "¿Sobre qué entidad actúa?"
 *   "¿Qué propiedad cambió para desencadenar esto?"
 *   "¿Cuánto cambió ese valor?"
 *   "¿Qué evento disparó la cadena?"
 *   "¿En qué frame ocurre?"
 *   "¿Qué modificadores están activos en la entidad origen?"
 *
 * ── RELACIÓN CON ModifierContext ─────────────────────────────────────────
 * ModifierContext responde "¿qué información hay en el momento de modificar?"
 * OperationContext responde "¿qué información hay en el momento de ejecutar
 * una consecuencia?"
 *
 * Son complementarios: OperationContext CONTIENE un ModifierContext (el que
 * estaba activo cuando se resolvió la propiedad que disparó la operación)
 * para que las operaciones puedan inspeccionar el estado completo del pipeline.
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * OperationContext es completamente inmutable una vez construido.
 * Construir mediante {@link Builder}.
 *
 * ── EJEMPLO ──────────────────────────────────────────────────────────────
 *
 *   OperationContext ctx = OperationContext.builder()
 *       .source(attacker)
 *       .target(defender)
 *       .affectedProperty(PropertyKeys.TEMPERATURE)
 *       .previousValue(10.0)
 *       .finalValue(-30.0)
 *       .triggeringEvent(freezeEvent)
 *       .modifierContext(activeModifierCtx)
 *       .resolutionContext(ResolutionContext.of(attacker))
 *       .causalNode(causalNode)
 *       .timestamp(currentFrame)
 *       .build();
 *
 * @see GameplayOperation
 * @see OperationPredicate
 */
public final class OperationContext {

    // ── Participantes ─────────────────────────────────────────────────────

    /**
     * Entidad que origina la acción que produjo este contexto.
     * Puede ser null para efectos de entorno o consecuencias sin origen explícito.
     */
    private final GameObjects source;

    /**
     * Entidad sobre la que se ejecuta la operación.
     * Puede ser null para operaciones globales o de mundo.
     */
    private final GameObjects target;

    // ── Propiedad afectada ────────────────────────────────────────────────

    /**
     * La propiedad cuya resolución desencadenó esta operación.
     * Por ejemplo, PropertyKeys.TEMPERATURE cuando la temperatura cambió.
     * Puede ser null si la operación fue disparada sin una propiedad concreta.
     */
    private final PropertyKey<?> affectedProperty;

    /**
     * Valor de la propiedad ANTES de la resolución que disparó la operación.
     */
    private final double previousValue;

    /**
     * Valor FINAL de la propiedad tras la resolución completa del pipeline.
     */
    private final double finalValue;

    // ── Causalidad ────────────────────────────────────────────────────────

    /**
     * Evento de gameplay que inició la cadena que produjo esta operación.
     * Por ejemplo, un OnDamage que desencadenó un cambio de temperatura
     * que a su vez disparó FreezeMovementOperation.
     * Puede ser null si no hay un evento raíz identificable.
     */
    private final GameplayEvent triggeringEvent;

    /**
     * El ModifierContext activo cuando se resolvió la propiedad afectada.
     * Contiene entidades, tags, capacidades, fuente del modificador, etc.
     * Puede ser null si la resolución se hizo en modo CFCC-001 (sin contexto).
     */
    private final ModifierContext modifierContext;

    /**
     * Contexto de resolución de la entidad origen, si está disponible.
     * Permite que la operación consulte otras propiedades del origen.
     */
    private final ResolutionContext resolutionContext;

    /**
     * Nodo causal del grafo que corresponde al hecho que disparó esta operación.
     * Permite vincular los efectos de la operación al grafo causal del frame.
     * Puede ser null si no se usa trazabilidad de alta fidelidad.
     */
    private final CausalNode causalNode;

    // ── Tiempo ────────────────────────────────────────────────────────────

    /**
     * Timestamp en frames o unidades de tiempo del juego.
     */
    private final long timestamp;

    // ── Constructor privado ───────────────────────────────────────────────

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

    // ── Factory ───────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Crea un contexto mínimo solo con source, target, propiedad y valores.
     * Útil para operaciones simples donde el contexto completo no está disponible.
     *
     * @param source        entidad origen (puede ser null)
     * @param target        entidad destino (puede ser null)
     * @param property      propiedad afectada (puede ser null)
     * @param previousValue valor anterior
     * @param finalValue    valor final
     * @param timestamp     frame actual
     */
    public static OperationContext minimal(
            GameObjects source,
            GameObjects target,
            PropertyKey<?> property,
            double previousValue,
            double finalValue,
            long timestamp) {
        return builder()
            .source(source)
            .target(target)
            .affectedProperty(property)
            .previousValue(previousValue)
            .finalValue(finalValue)
            .timestamp(timestamp)
            .build();
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Entidad origen de la acción, o null para efectos de entorno. */
    public GameObjects getSource()                  { return source; }

    /** Entidad destino de la operación, o null para operaciones globales. */
    public GameObjects getTarget()                  { return target; }

    /** La propiedad cuyo cambio desencadenó la operación, o null. */
    public PropertyKey<?> getAffectedProperty()     { return affectedProperty; }

    /** Valor de la propiedad antes de la resolución. */
    public double getPreviousValue()                { return previousValue; }

    /** Valor final de la propiedad tras la resolución completa. */
    public double getFinalValue()                   { return finalValue; }

    /**
     * Delta de la propiedad: finalValue - previousValue.
     * Positivo si la propiedad aumentó, negativo si disminuyó.
     */
    public double getDelta()                        { return finalValue - previousValue; }

    /** Evento de gameplay que originó la cadena, o null. */
    public GameplayEvent getTriggeringEvent()        { return triggeringEvent; }

    /** ModifierContext activo durante la resolución, o null. */
    public ModifierContext getModifierContext()      { return modifierContext; }

    /** ResolutionContext de la entidad origen, o null. */
    public ResolutionContext getResolutionContext()  { return resolutionContext; }

    /** Nodo causal asociado a este hecho, o null. */
    public CausalNode getCausalNode()               { return causalNode; }

    /** Timestamp en frames. 0 si no fue establecido. */
    public long getTimestamp()                      { return timestamp; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "OperationContext["
            + "property=" + (affectedProperty != null ? affectedProperty.id() : "null")
            + ", prev=" + previousValue
            + ", final=" + finalValue
            + ", t=" + timestamp
            + "]";
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de OperationContext.
     * Todos los campos son opcionales — ningún campo es obligatorio.
     * Un OperationContext vacío es válido (todo null/0) para operaciones
     * que no necesitan información contextual.
     */
    public static final class Builder {

        private GameObjects       source;
        private GameObjects       target;
        private PropertyKey<?>    affectedProperty;
        private double            previousValue;
        private double            finalValue;
        private GameplayEvent     triggeringEvent;
        private ModifierContext   modifierContext;
        private ResolutionContext resolutionContext;
        private CausalNode        causalNode;
        private long              timestamp;

        private Builder() {}

        public Builder source(GameObjects source)                        { this.source = source; return this; }
        public Builder target(GameObjects target)                        { this.target = target; return this; }
        public Builder affectedProperty(PropertyKey<?> property)         { this.affectedProperty = property; return this; }
        public Builder previousValue(double previousValue)               { this.previousValue = previousValue; return this; }
        public Builder finalValue(double finalValue)                     { this.finalValue = finalValue; return this; }
        public Builder triggeringEvent(GameplayEvent event)              { this.triggeringEvent = event; return this; }
        public Builder modifierContext(ModifierContext ctx)              { this.modifierContext = ctx; return this; }
        public Builder resolutionContext(ResolutionContext ctx)           { this.resolutionContext = ctx; return this; }
        public Builder causalNode(CausalNode node)                       { this.causalNode = node; return this; }
        public Builder timestamp(long timestamp)                         { this.timestamp = timestamp; return this; }

        /** Construye el OperationContext. */
        public OperationContext build() {
            return new OperationContext(this);
        }
    }
}
