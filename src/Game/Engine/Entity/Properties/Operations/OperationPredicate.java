package Game.Engine.Entity.Properties.Operations;

import Game.Engine.Entity.Properties.PropertyKey;

/**
 * Condición lógica que determina si una GameplayOperation debe ejecutarse.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * OperationPredicate responde a:
 *   "¿Debe ejecutarse esta operación dado el contexto actual?"
 *
 * ── COMPOSICIÓN ──────────────────────────────────────────────────────────
 * Los predicados se componen con AND, OR y NOT.
 *
 * ── ALWAYS / NEVER ────────────────────────────────────────────────────────
 * ALWAYS es el predicado por defecto de toda entrada en OperationRegistry.
 *
 * @see OperationContext
 * @see GameplayOperation
 * @see OperationRegistry
 */
@FunctionalInterface
public interface OperationPredicate {

    boolean test(OperationContext context);

    // ── Predicados compuestos ─────────────────────────────────────────────

    static OperationPredicate and(OperationPredicate... predicates) {
        if (predicates == null || predicates.length == 0) return ALWAYS;
        return ctx -> {
            for (OperationPredicate p : predicates) { if (!p.test(ctx)) return false; }
            return true;
        };
    }

    static OperationPredicate or(OperationPredicate... predicates) {
        if (predicates == null || predicates.length == 0) return NEVER;
        return ctx -> {
            for (OperationPredicate p : predicates) { if (p.test(ctx)) return true; }
            return false;
        };
    }

    static OperationPredicate not(OperationPredicate predicate) {
        if (predicate == null) return NEVER;
        return ctx -> !predicate.test(ctx);
    }

    // ── Predicados de conveniencia ────────────────────────────────────────

    static OperationPredicate deltaBelow(double threshold)       { return ctx -> ctx.getDelta() < threshold; }
    static OperationPredicate deltaAbove(double threshold)       { return ctx -> ctx.getDelta() > threshold; }
    static OperationPredicate finalValueAbove(double threshold)  { return ctx -> ctx.getFinalValue() > threshold; }
    static OperationPredicate finalValueBelow(double threshold)  { return ctx -> ctx.getFinalValue() < threshold; }

    static OperationPredicate onProperty(PropertyKey<?> key) {
        if (key == null) return NEVER;
        return ctx -> ctx.getAffectedProperty() == key;
    }

    static OperationPredicate hasTarget()  { return ctx -> ctx.getTarget() != null; }
    static OperationPredicate hasSource()  { return ctx -> ctx.getSource() != null; }

    // ── Predicados constantes ─────────────────────────────────────────────

    OperationPredicate ALWAYS = ctx -> true;
    OperationPredicate NEVER  = ctx -> false;
}
