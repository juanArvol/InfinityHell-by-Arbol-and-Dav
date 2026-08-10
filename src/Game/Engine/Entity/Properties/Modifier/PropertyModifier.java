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
    private final String         sourceId;

    // ── Campos de causalidad (opcionales) ─────────────────────────────────

    private final ModifierSource    source;
    private final ModifierScope     scope;
    private final ModifierPredicate predicate;
    private final ModifierInfluence influence;
    private final ModifierChain     chain;

    // ── Constructor privado ───────────────────────────────────────────────

    private PropertyModifier(
            PropertyKey<?> key, Phase phase, double value, String sourceId,
            ModifierSource source, ModifierScope scope,
            ModifierPredicate predicate, ModifierInfluence influence, ModifierChain chain) {

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

    // ── Factory methods (sin causalidad) ─────────────────────────────────

    /** Crea un modificador aditivo: suma el valor al base. */
    public static PropertyModifier additive(PropertyKey<?> key, double amount, String sourceId) {
        return new PropertyModifier(key, Phase.ADDITIVE, amount, sourceId,
            null, null, null, null, null);
    }

    /** Crea un modificador multiplicativo: multiplica el resultado aditivo. */
    public static PropertyModifier multiplicative(PropertyKey<?> key, double factor, String sourceId) {
        return new PropertyModifier(key, Phase.MULTIPLICATIVE, factor, sourceId,
            null, null, null, null, null);
    }

    /** Crea un modificador de sobreescritura: sustituye el valor calculado. */
    public static PropertyModifier override(PropertyKey<?> key, double fixedValue, String sourceId) {
        return new PropertyModifier(key, Phase.OVERRIDE, fixedValue, sourceId,
            null, null, null, null, null);
    }

    // ── with*() — añadir causalidad ───────────────────────────────────────

    public PropertyModifier withSource(ModifierSource src) {
        return new PropertyModifier(key, phase, value, sourceId, src, scope, predicate, influence, chain);
    }

    public PropertyModifier withScope(ModifierScope scp) {
        return new PropertyModifier(key, phase, value, sourceId, source, scp, predicate, influence, chain);
    }

    public PropertyModifier withPredicate(ModifierPredicate pred) {
        return new PropertyModifier(key, phase, value, sourceId, source, scope, pred, influence, chain);
    }

    public PropertyModifier withInfluence(ModifierInfluence inf) {
        return new PropertyModifier(key, phase, value, sourceId, source, scope, predicate, inf, chain);
    }

    public PropertyModifier withChain(ModifierChain c) {
        return new PropertyModifier(key, phase, value, sourceId, source, scope, predicate, influence, c);
    }

    /**
     * Retorna una copia heredando toda la causalidad de otro modificador.
     * Útil en ModifierInfluence.apply() cuando se transforma un modificador
     * y se quiere preservar su cadena causal.
     */
    public PropertyModifier withCausalityFrom(PropertyModifier origin) {
        if (origin == null) return this;
        return new PropertyModifier(key, phase, value, sourceId,
            origin.source, origin.scope, origin.predicate, origin.influence, origin.chain);
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    public PropertyKey<?> getKey()          { return key; }
    public Phase getPhase()                 { return phase; }
    public double getValue()                { return value; }
    public String getSourceId()             { return sourceId; }
    public ModifierSource getSource()       { return source; }
    public ModifierScope getScope()         { return scope; }
    public ModifierPredicate getPredicate() { return predicate; }
    public ModifierInfluence getInfluence() { return influence; }
    public ModifierChain getChain()         { return chain; }

    public boolean hasPredicate() { return predicate != null; }
    public boolean hasInfluence() { return influence != null; }
    public boolean hasChain()     { return chain != null; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PropertyModifier[")
          .append(key.displayName()).append(" ").append(phase).append(" ").append(value)
          .append(" from:").append(sourceId);
        if (source    != null) sb.append(" src:").append(source.displayName());
        if (scope     != null) sb.append(" scope:").append(scope.displayName());
        if (predicate != null) sb.append(" pred:yes");
        if (influence != null) sb.append(" inf:yes");
        if (chain     != null) sb.append(" chain:").append(chain.getChainId());
        sb.append("]");
        return sb.toString();
    }
}
