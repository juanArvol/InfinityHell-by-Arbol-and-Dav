package Game.Gameplay.Core.Modifiers;

import Game.Gameplay.Core.Causality.ModifierChain;
import Game.Gameplay.Core.Causality.ModifierInfluence;
import Game.Gameplay.Core.Causality.ModifierPredicate;
import Game.Gameplay.Core.Causality.ModifierScope;
import Game.Gameplay.Core.Causality.ModifierSource;
import Game.Gameplay.Core.Properties.PropertyKey;

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
 * ── PIPELINE DE RESOLUCIÓN (CFCC-001) ────────────────────────────────────
 * Los modificadores no se aplican en orden arbitrario. La resolución sigue
 * un pipeline definido en PropertyResolver con fases ordenadas:
 *
 *   Fase 1 — ADDITIVE:       base + sum(aditivos)
 *   Fase 2 — MULTIPLICATIVE: resultado × product(multiplicativos)
 *   Fase 3 — OVERRIDE:       si hay override, sustituye el resultado
 *   Fase 4 — CLAMP:          clamp al rango [min, max]
 *   → Valor final
 *
 * ── CAPA DE CAUSALIDAD (CFCC-002) ────────────────────────────────────────
 * Cada PropertyModifier puede poseer opcionalmente cinco campos de causalidad:
 *
 *   ModifierSource    → ¿de dónde proviene este modificador?
 *   ModifierScope     → ¿sobre qué tipo de objetivo actúa?
 *   ModifierPredicate → ¿cuándo debe ejecutarse? (condición lógica)
 *   ModifierInfluence → ¿cómo puede ser alterado por otros modificadores?
 *   ModifierChain     → ¿de qué modificador nació? ¿qué originó?
 *
 * Si alguno de estos campos está ausente, el modificador se comporta
 * exactamente igual que en CFCC-001. La compatibilidad es total.
 *
 * ── TIPOS DE MODIFICADOR ──────────────────────────────────────────────────
 *
 *   ADDITIVE:       Suma flat al valor base.
 *                   "+8 de daño" → PropertyModifier.additive(DAMAGE, 8, "source")
 *
 *   MULTIPLICATIVE: Multiplica el resultado aditivo.
 *                   "+50% de daño" → PropertyModifier.multiplicative(DAMAGE, 1.5, "source")
 *
 *   OVERRIDE:       Sustituye el valor calculado por un valor fijo.
 *                   "Cooldown siempre 1" → PropertyModifier.override(COOLDOWN, 1.0, "source")
 *
 * ── FACTORY METHODS ───────────────────────────────────────────────────────
 * Existen dos niveles de factory:
 *
 * 1. Factories de CFCC-001 (sin causalidad) — siguen funcionando exactamente igual:
 *      PropertyModifier.additive(key, value, sourceId)
 *      PropertyModifier.multiplicative(key, factor, sourceId)
 *      PropertyModifier.override(key, fixedValue, sourceId)
 *
 * 2. Método with*() para añadir causalidad a un modificador existente:
 *      modifier.withSource(src)
 *      modifier.withScope(scope)
 *      modifier.withPredicate(pred)
 *      modifier.withInfluence(influence)
 *      modifier.withChain(chain)
 *      modifier.withCausalityFrom(otherModifier)  // copia toda la causalidad
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

    // ── Campos CFCC-001 ───────────────────────────────────────────────────

    private final PropertyKey<?> key;
    private final Phase          phase;
    private final double         value;
    private final String         sourceId;

    // ── Campos CFCC-002 (opcionales) ──────────────────────────────────────

    /** Origen semántico de este modificador. Null si no se especifica. */
    private final ModifierSource    source;

    /** Alcance de este modificador. Null si no se especifica. */
    private final ModifierScope     scope;

    /**
     * Condición lógica que debe cumplirse para que este modificador se ejecute.
     * Si es null, equivale a ModifierPredicate.ALWAYS (siempre aplica).
     */
    private final ModifierPredicate predicate;

    /**
     * Influencia que este modificador puede recibir de otros modificadores.
     * Si es null, el modificador no puede ser alterado por influencias externas.
     * Nota: las influencias son aplicadas POR otros modificadores SOBRE éste,
     * no por éste sobre otros.
     */
    private final ModifierInfluence influence;

    /** Nodo en la cadena causal. Null si no participa en ninguna cadena. */
    private final ModifierChain     chain;

    // ── Constructor privado ───────────────────────────────────────────────

    private PropertyModifier(
            PropertyKey<?> key,
            Phase phase,
            double value,
            String sourceId,
            ModifierSource source,
            ModifierScope scope,
            ModifierPredicate predicate,
            ModifierInfluence influence,
            ModifierChain chain) {

        if (key == null)
            throw new IllegalArgumentException("key no puede ser null.");
        if (sourceId == null || sourceId.isBlank())
            throw new IllegalArgumentException("sourceId no puede ser null o vacío.");

        this.key       = key;
        this.phase     = phase;
        this.value     = value;
        this.sourceId  = sourceId;
        this.source    = source;
        this.scope     = scope;
        this.predicate = predicate;
        this.influence = influence;
        this.chain     = chain;
    }

    // ── Factory methods CFCC-001 (sin causalidad) ─────────────────────────

    /**
     * Crea un modificador aditivo: suma el valor al base.
     *
     * @param key      propiedad afectada
     * @param amount   cantidad a sumar (puede ser negativa para reducción)
     * @param sourceId quién aplica este modificador (para debug y deduplicación)
     */
    public static PropertyModifier additive(PropertyKey<?> key, double amount, String sourceId) {
        return new PropertyModifier(key, Phase.ADDITIVE, amount, sourceId,
            null, null, null, null, null);
    }

    /**
     * Crea un modificador multiplicativo: multiplica el resultado aditivo.
     *
     * @param key      propiedad afectada
     * @param factor   factor multiplicativo (1.0 = sin efecto, 1.5 = +50%)
     * @param sourceId quién aplica este modificador
     */
    public static PropertyModifier multiplicative(PropertyKey<?> key, double factor, String sourceId) {
        return new PropertyModifier(key, Phase.MULTIPLICATIVE, factor, sourceId,
            null, null, null, null, null);
    }

    /**
     * Crea un modificador de sobreescritura: sustituye el valor calculado.
     *
     * @param key        propiedad afectada
     * @param fixedValue valor fijo que sustituye el resultado
     * @param sourceId   quién aplica este modificador
     */
    public static PropertyModifier override(PropertyKey<?> key, double fixedValue, String sourceId) {
        return new PropertyModifier(key, Phase.OVERRIDE, fixedValue, sourceId,
            null, null, null, null, null);
    }

    // ── with*() — añadir causalidad (CFCC-002) ────────────────────────────

    /**
     * Retorna una copia de este modificador con el ModifierSource especificado.
     *
     * @param src origen semántico del modificador
     * @return nuevo PropertyModifier con source = src; resto idéntico
     */
    public PropertyModifier withSource(ModifierSource src) {
        return new PropertyModifier(key, phase, value, sourceId,
            src, scope, predicate, influence, chain);
    }

    /**
     * Retorna una copia de este modificador con el ModifierScope especificado.
     *
     * @param scp alcance del modificador
     * @return nuevo PropertyModifier con scope = scp; resto idéntico
     */
    public PropertyModifier withScope(ModifierScope scp) {
        return new PropertyModifier(key, phase, value, sourceId,
            source, scp, predicate, influence, chain);
    }

    /**
     * Retorna una copia de este modificador con el ModifierPredicate especificado.
     *
     * @param pred condición de activación
     * @return nuevo PropertyModifier con predicate = pred; resto idéntico
     */
    public PropertyModifier withPredicate(ModifierPredicate pred) {
        return new PropertyModifier(key, phase, value, sourceId,
            source, scope, pred, influence, chain);
    }

    /**
     * Retorna una copia de este modificador con la ModifierInfluence especificada.
     *
     * @param inf influencia que este modificador puede recibir
     * @return nuevo PropertyModifier con influence = inf; resto idéntico
     */
    public PropertyModifier withInfluence(ModifierInfluence inf) {
        return new PropertyModifier(key, phase, value, sourceId,
            source, scope, predicate, inf, chain);
    }

    /**
     * Retorna una copia de este modificador con el ModifierChain especificado.
     *
     * @param c nodo en la cadena causal
     * @return nuevo PropertyModifier con chain = c; resto idéntico
     */
    public PropertyModifier withChain(ModifierChain c) {
        return new PropertyModifier(key, phase, value, sourceId,
            source, scope, predicate, influence, c);
    }

    /**
     * Retorna una copia de este modificador heredando toda la causalidad
     * (source, scope, predicate, influence, chain) de otro modificador.
     *
     * Útil en ModifierInfluence.apply() cuando se transforma un modificador
     * y se quiere preservar su cadena causal:
     *
     *   return PropertyModifier.additive(key, newValue, newSourceId)
     *                          .withCausalityFrom(original);
     *
     * @param origin modificador del que heredar la causalidad
     * @return nuevo PropertyModifier con los campos de causalidad de origin
     */
    public PropertyModifier withCausalityFrom(PropertyModifier origin) {
        if (origin == null) return this;
        return new PropertyModifier(key, phase, value, sourceId,
            origin.source, origin.scope, origin.predicate,
            origin.influence, origin.chain);
    }

    // ── Acceso CFCC-001 ───────────────────────────────────────────────────

    /** Propiedad sobre la que actúa este modificador. */
    public PropertyKey<?> getKey()      { return key; }

    /** Fase del pipeline en la que actúa. */
    public Phase getPhase()             { return phase; }

    /** Valor del modificador (semántica depende de la fase). */
    public double getValue()            { return value; }

    /** Identificador de quién aplica este modificador (para debug y logging). */
    public String getSourceId()         { return sourceId; }

    // ── Acceso CFCC-002 ───────────────────────────────────────────────────

    /**
     * Origen semántico de este modificador, o null si no se especificó.
     * Ejemplo: ModifierSource de tipo "Weapon.Ranged".
     */
    public ModifierSource getSource()       { return source; }

    /**
     * Alcance de este modificador, o null si no se especificó.
     * Ejemplo: ModifierScope de tipo "Target".
     */
    public ModifierScope getScope()         { return scope; }

    /**
     * Predicado de activación, o null si siempre aplica.
     * PropertyResolver usa ModifierPredicate.ALWAYS cuando este campo es null.
     */
    public ModifierPredicate getPredicate() { return predicate; }

    /**
     * Influencia que este modificador puede recibir de otros, o null.
     * Cuando es null, el modificador no declara cómo puede ser transformado.
     */
    public ModifierInfluence getInfluence() { return influence; }

    /**
     * Nodo en la cadena causal, o null si no participa en ninguna cadena.
     */
    public ModifierChain getChain()         { return chain; }

    // ── Consultas de conveniencia ─────────────────────────────────────────

    /**
     * True si este modificador tiene un predicado explícito (no siempre-activo).
     */
    public boolean hasPredicate() { return predicate != null; }

    /**
     * True si este modificador tiene una influencia recibida declarada.
     */
    public boolean hasInfluence() { return influence != null; }

    /**
     * True si este modificador participa en una cadena causal.
     */
    public boolean hasChain()     { return chain != null; }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PropertyModifier[")
          .append(key.id()).append(" ").append(phase).append(" ").append(value)
          .append(" from:").append(sourceId);
        if (source    != null) sb.append(" src:").append(source.id());
        if (scope     != null) sb.append(" scope:").append(scope.id());
        if (predicate != null) sb.append(" pred:yes");
        if (influence != null) sb.append(" inf:yes");
        if (chain     != null) sb.append(" chain:").append(chain.getChainId());
        sb.append("]");
        return sb.toString();
    }
}
