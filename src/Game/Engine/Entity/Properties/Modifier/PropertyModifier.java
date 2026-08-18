package Game.Engine.Entity.Properties.Modifier;

import Game.Engine.Entity.Properties.Modifier.Causality.ModifierChain;
import Game.Engine.Entity.Properties.Modifier.Causality.ModifierInfluence;
import Game.Engine.Entity.Properties.Modifier.Causality.ModifierPredicate;
import Game.Engine.Entity.Properties.Modifier.Causality.ModifierScope;
import Game.Engine.Entity.Properties.Modifier.Causality.ModifierSource;
import Game.Engine.Entity.Properties.PropertyKey;

/**
 * Modificador que altera el valor de una propiedad específica durante la resolución.
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * PropertyModifier es la pieza de modificación de propiedades en la cadena
 * de resolución. Representa una única operación sobre una única propiedad:
 *
 *   +15 de daño (aditivo)
 *   ×1.5 de velocidad (multiplicativo)
 *   sobreescribir cooldown a 0 (override)
 *
 * ── PIPELINE DE RESOLUCIÓN ────────────────────────────────────────────────
 * Los modificadores no se aplican en orden arbitrario. La resolución sigue
 * un pipeline definido en PropertyResolver con fases ordenadas:
 *
 *   Fase 1 — ADDITIVE:       base + sum(aditivos)
 *   Fase 2 — MULTIPLICATIVE: resultado × product(multiplicativos)
 *   Fase 3 — OVERRIDE:       si hay override, sustituye el resultado
 *   Fase 4 — CLAMP:          clamp al rango [min, max]
 *   → Valor final
 *
 * ── CAPA DE CAUSALIDAD ────────────────────────────────────────────────────
 * Cada PropertyModifier puede poseer opcionalmente cinco campos de causalidad:
 *
 *   ModifierSource    → ¿de dónde proviene este modificador?
 *   ModifierScope     → ¿sobre qué tipo de objetivo actúa?
 *   ModifierPredicate → ¿cuándo debe ejecutarse?
 *   ModifierInfluence → ¿cómo puede ser alterado por otros modificadores?
 *   ModifierChain     → ¿de qué modificador nació?
 *
 * Si alguno de estos campos está ausente, el modificador se comporta
 * exactamente igual que sin causalidad. La compatibilidad es total.
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * PropertyModifier es inmutable. Los métodos with*() retornan NUEVAS instancias.
 */
public final class PropertyModifier {

    /** Fase en la que opera este modificador dentro del pipeline de resolución. */
    public enum Phase {
        /** Suma flat al valor base. */
        ADDITIVE,
        /** Multiplica al resultado de la fase aditiva. */
        MULTIPLICATIVE,
        /** Sustituye el valor calculado por un valor fijo. */
        OVERRIDE
    }

    // ── Campos base ───────────────────────────────────────────────────────

    private final PropertyKey<?> key;
    private final Phase          phase;
    private final double         value;
    
    // ── HRFC-FASE3: String sourceId → PropertyModifierSource source ───────
    private final PropertyModifierSource source;
    private final String                 debugDescription;

    // ── Campos de causalidad (opcionales) ─────────────────────────────────

    private final ModifierSource    causalSource;
    private final ModifierScope     scope;
    private final ModifierPredicate predicate;
    private final ModifierInfluence influence;
    private final ModifierChain     chain;
    
    /**
     * Sentinel para modificadores sin origen rastreable.
     * 
     * <p>Usar para modificadores permanentes que nunca se revocarán por fuente
     * (añadidos directamente sin un contributor específico). Es una implementación
     * anónima de PropertyModifierSource que existe únicamente como sentinel de
     * identidad para removeBySource().
     */
    public static final PropertyModifierSource NO_SOURCE = new PropertyModifierSource() {
        @Override
        public String debugName() { return "NO_SOURCE"; }
    };

    // ── Constructor privado ───────────────────────────────────────────────

    private PropertyModifier(
            PropertyKey<?> key, Phase phase, double value,
            PropertyModifierSource source, String debugDescription,
            ModifierSource causalSource, ModifierScope scope,
            ModifierPredicate predicate, ModifierInfluence influence, ModifierChain chain) {

        if (key == null)
            throw new IllegalArgumentException("key no puede ser null.");

        this.key              = key;
        this.phase            = phase;
        this.value            = value;
        this.source           = (source != null) ? source : NO_SOURCE;
        this.debugDescription = (debugDescription != null) ? debugDescription : "";
        this.causalSource     = causalSource;
        this.scope            = scope;
        this.predicate        = predicate;
        this.influence        = influence;
        this.chain            = chain;
    }

    // ── Factory methods (sin causalidad) ─────────────────────────────────

    /** Crea un modificador aditivo: suma el valor al base. */
    public static PropertyModifier additive(PropertyKey<?> key, double amount, PropertyModifierSource source) {
        return new PropertyModifier(key, Phase.ADDITIVE, amount, source, "",
            null, null, null, null, null);
    }

    /** Crea un modificador multiplicativo: multiplica el resultado aditivo. */
    public static PropertyModifier multiplicative(PropertyKey<?> key, double factor, PropertyModifierSource source) {
        return new PropertyModifier(key, Phase.MULTIPLICATIVE, factor, source, "",
            null, null, null, null, null);
    }

    /** Crea un modificador de sobreescritura: sustituye el valor calculado. */
    public static PropertyModifier override(PropertyKey<?> key, double fixedValue, PropertyModifierSource source) {
        return new PropertyModifier(key, Phase.OVERRIDE, fixedValue, source, "",
            null, null, null, null, null);
    }
    
    // ── Factory methods con debug description ─────────────────────────────
    
    /** Crea un modificador aditivo con descripción de debug. */
    public static PropertyModifier additive(PropertyKey<?> key, double amount, 
                                           PropertyModifierSource source, String debugDescription) {
        return new PropertyModifier(key, Phase.ADDITIVE, amount, source, debugDescription,
            null, null, null, null, null);
    }
    
    /** Crea un modificador multiplicativo con descripción de debug. */
    public static PropertyModifier multiplicative(PropertyKey<?> key, double factor,
                                                 PropertyModifierSource source, String debugDescription) {
        return new PropertyModifier(key, Phase.MULTIPLICATIVE, factor, source, debugDescription,
            null, null, null, null, null);
    }
    
    /** Crea un modificador de sobreescritura con descripción de debug. */
    public static PropertyModifier override(PropertyKey<?> key, double fixedValue,
                                           PropertyModifierSource source, String debugDescription) {
        return new PropertyModifier(key, Phase.OVERRIDE, fixedValue, source, debugDescription,
            null, null, null, null, null);
    }

    // ── with*() — añadir causalidad ───────────────────────────────────────

    public PropertyModifier withCausalSource(ModifierSource src) {
        return new PropertyModifier(key, phase, value, source, debugDescription, src, scope, predicate, influence, chain);
    }

    public PropertyModifier withScope(ModifierScope scp) {
        return new PropertyModifier(key, phase, value, source, debugDescription, causalSource, scp, predicate, influence, chain);
    }

    public PropertyModifier withPredicate(ModifierPredicate pred) {
        return new PropertyModifier(key, phase, value, source, debugDescription, causalSource, scope, pred, influence, chain);
    }

    public PropertyModifier withInfluence(ModifierInfluence inf) {
        return new PropertyModifier(key, phase, value, source, debugDescription, causalSource, scope, predicate, inf, chain);
    }

    public PropertyModifier withChain(ModifierChain c) {
        return new PropertyModifier(key, phase, value, source, debugDescription, causalSource, scope, predicate, influence, c);
    }

    /**
     * Retorna una copia heredando toda la causalidad de otro modificador.
     * Útil en ModifierInfluence.apply() cuando se transforma un modificador
     * y se quiere preservar su cadena causal.
     */
    public PropertyModifier withCausalityFrom(PropertyModifier origin) {
        if (origin == null) return this;
        return new PropertyModifier(key, phase, value, source, debugDescription,
            origin.causalSource, origin.scope, origin.predicate, origin.influence, origin.chain);
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    public PropertyKey<?> getKey()                      { return key; }
    public Phase getPhase()                             { return phase; }
    public double getValue()                            { return value; }
    public PropertyModifierSource getSource()           { return source; }
    public String getDebugDescription()                 { return debugDescription; }
    public ModifierSource getCausalSource()             { return causalSource; }
    public ModifierScope getScope()                     { return scope; }
    public ModifierPredicate getPredicate()             { return predicate; }
    public ModifierInfluence getInfluence()             { return influence; }
    public ModifierChain getChain()                     { return chain; }

    public boolean hasPredicate() { return predicate != null; }
    public boolean hasInfluence() { return influence != null; }
    public boolean hasChain()     { return chain != null; }
    
    // ── Deprecated (migration compatibility) ───────────────────────────────
    
    /**
     * @deprecated HRFC-FASE3: Usar {@link #getSource()} que ahora retorna PropertyModifierSource.
     *             Este método existe solo para compatibilidad temporal durante migración.
     */
    @Deprecated
    public String getSourceId() {
        return source != null ? source.debugName() : "NO_SOURCE";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PropertyModifier[")
          .append(key.displayName()).append(" ").append(phase).append(" ").append(value)
          .append(" from:").append(source != null ? source.debugName() : "null");
        if (!debugDescription.isEmpty()) sb.append(" desc:\"").append(debugDescription).append("\"");
        if (causalSource != null) sb.append(" causal:").append(causalSource.displayName());
        if (scope        != null) sb.append(" scope:").append(scope.displayName());
        if (predicate    != null) sb.append(" pred:yes");
        if (influence    != null) sb.append(" inf:yes");
        if (chain        != null) sb.append(" chain:").append(chain.getChainId());
        sb.append("]");
        return sb.toString();
    }
}
