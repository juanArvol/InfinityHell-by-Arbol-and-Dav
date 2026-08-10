package Game.Engine.Entity.Properties.Dependencies;

import Game.Engine.Entity.Properties.PropertyKey;

/**
 * Condición bajo la cual una dependencia entre propiedades está activa.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * DependencyCondition responde a:
 *   "¿Debe propagarse esta dependencia dado el estado actual?"
 *
 * @see PropertyDependency
 */
@FunctionalInterface
public interface DependencyCondition {

    boolean shouldPropagate(
        PropertyKey<?> sourceKey,
        double         previousValue,
        double         newValue,
        PropertyKey<?> targetKey
    );

    // ── Condiciones predefinidas ──────────────────────────────────────────

    DependencyCondition ALWAYS    = (src, prev, next, dst) -> true;
    DependencyCondition NEVER     = (src, prev, next, dst) -> false;
    DependencyCondition ON_CHANGE = (src, prev, next, dst) -> next != prev;
    DependencyCondition ON_INCREASE = (src, prev, next, dst) -> next > prev;
    DependencyCondition ON_DECREASE = (src, prev, next, dst) -> next < prev;

    // ── Factory methods ───────────────────────────────────────────────────

    static DependencyCondition whenBelow(double threshold) {
        return (src, prev, next, dst) -> next < threshold;
    }

    static DependencyCondition whenAbove(double threshold) {
        return (src, prev, next, dst) -> next > threshold;
    }

    static DependencyCondition whenCrosses(double threshold) {
        return (src, prev, next, dst) ->
            (prev < threshold && next >= threshold) ||
            (prev >= threshold && next < threshold);
    }

    static DependencyCondition and(DependencyCondition a, DependencyCondition b) {
        if (a == null && b == null) return ALWAYS;
        if (a == null) return b;
        if (b == null) return a;
        return (src, prev, next, dst) ->
            a.shouldPropagate(src, prev, next, dst) &&
            b.shouldPropagate(src, prev, next, dst);
    }

    static DependencyCondition or(DependencyCondition a, DependencyCondition b) {
        if (a == null && b == null) return NEVER;
        if (a == null) return b;
        if (b == null) return a;
        return (src, prev, next, dst) ->
            a.shouldPropagate(src, prev, next, dst) ||
            b.shouldPropagate(src, prev, next, dst);
    }
}
