package Game.Engine.Entity.Properties.Modifier.Causality;

/**
 * Condición lógica que determina si un modificador debe ejecutarse.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * ModifierPredicate representa una condición booleana sobre el contexto de
 * resolución de un modificador. El pipeline de PropertyResolver evalúa el
 * predicado de cada modificador antes de incluirlo en el cálculo.
 *
 *   "¿Aplica este modificador dado el contexto actual?"
 *
 * ── COMPOSICIÓN ──────────────────────────────────────────────────────────
 * Los predicados se componen con los operadores estáticos AND, OR y NOT.
 *
 * ── ALWAYS / NEVER ────────────────────────────────────────────────────────
 * ALWAYS es el predicado neutro: siempre retorna true.
 * Un modificador sin predicado explícito usa ALWAYS, garantizando
 * compatibilidad total — los modificadores existentes se comportan exactamente
 * igual que antes.
 *
 * @see ModifierContext
 */
@FunctionalInterface
public interface ModifierPredicate {

    /**
     * Evalúa la condición sobre el contexto dado.
     *
     * @param context contexto de resolución del modificador (nunca null)
     * @return true si el modificador debe ejecutarse; false para omitirlo
     */
    boolean test(ModifierContext context);

    // ── Predicados compuestos ─────────────────────────────────────────────

    /** AND lógico. Cortocircuita al primer false. Si vacío, retorna ALWAYS. */
    static ModifierPredicate and(ModifierPredicate... predicates) {
        if (predicates == null || predicates.length == 0) return ALWAYS;
        return ctx -> {
            for (ModifierPredicate p : predicates) {
                if (!p.test(ctx)) return false;
            }
            return true;
        };
    }

    /** OR lógico. Cortocircuita al primer true. Si vacío, retorna NEVER. */
    static ModifierPredicate or(ModifierPredicate... predicates) {
        if (predicates == null || predicates.length == 0) return NEVER;
        return ctx -> {
            for (ModifierPredicate p : predicates) {
                if (p.test(ctx)) return true;
            }
            return false;
        };
    }

    /** Negación lógica del predicado dado. */
    static ModifierPredicate not(ModifierPredicate predicate) {
        if (predicate == null) return NEVER;
        return ctx -> !predicate.test(ctx);
    }

    // ── Predicados constantes ─────────────────────────────────────────────

    /** Predicado siempre verdadero. Valor por defecto de todo PropertyModifier. */
    ModifierPredicate ALWAYS = ctx -> true;

    /** Predicado siempre falso. Desactiva un modificador sin eliminarlo. */
    ModifierPredicate NEVER  = ctx -> false;
}
