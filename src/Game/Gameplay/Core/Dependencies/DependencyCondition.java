package Game.Gameplay.Core.Dependencies;

import Game.Gameplay.Core.Properties.PropertyKey;

/**
 * Condición bajo la cual una dependencia entre propiedades está activa.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * DependencyCondition responde a:
 *
 *   "¿Debe propagarse esta dependencia dado el estado actual?"
 *
 * Es el equivalente de OperationPredicate y ModifierPredicate para el
 * sistema de dependencias entre propiedades.
 *
 *   ModifierPredicate  → ¿aplica el modificador? (sobre ModifierContext)
 *   OperationPredicate → ¿se ejecuta la operación? (sobre OperationContext)
 *   DependencyCondition → ¿se propaga la dependencia? (sobre DependencyPropagationContext)
 *
 * ── DISEÑO: INTERFAZ FUNCIONAL ───────────────────────────────────────────
 * DependencyCondition es una interfaz funcional. Recibe:
 *   - La propiedad origen
 *   - El valor anterior de la propiedad origen
 *   - El valor nuevo de la propiedad origen
 *   - La propiedad destino
 *
 * Y retorna boolean: true si la dependencia debe propagarse, false si debe ignorarse.
 *
 * ── EJEMPLOS CONCEPTUALES ────────────────────────────────────────────────
 *
 *   // Solo propagar cuando la temperatura es negativa (frío real)
 *   DependencyCondition onlyCold = (src, prev, next, dst) -> next < 0.0;
 *
 *   // Solo propagar cuando hay cambio real (evitar propagaciones de delta=0)
 *   DependencyCondition.ON_CHANGE
 *
 *   // Solo propagar cuando el valor cruza un umbral (de positivo a negativo)
 *   DependencyCondition crossesZero = (src, prev, next, dst) ->
 *       (prev >= 0.0 && next < 0.0) || (prev <= 0.0 && next > 0.0);
 *
 * @see PropertyDependency
 */
@FunctionalInterface
public interface DependencyCondition {

    /**
     * Evalúa si la dependencia debe propagarse.
     *
     * @param sourceKey    propiedad que cambió
     * @param previousValue valor anterior de la propiedad origen
     * @param newValue      valor nuevo de la propiedad origen
     * @param targetKey    propiedad destino que recibiría el efecto
     * @return true si la propagación debe ocurrir
     */
    boolean shouldPropagate(
        PropertyKey<?> sourceKey,
        double         previousValue,
        double         newValue,
        PropertyKey<?> targetKey
    );

    // ── Condiciones predefinidas ──────────────────────────────────────────

    /**
     * Siempre propaga, sin importar los valores.
     * Útil para dependencias incondicionales.
     */
    DependencyCondition ALWAYS = (src, prev, next, dst) -> true;

    /**
     * Nunca propaga. Desactiva la dependencia sin eliminarla del grafo.
     */
    DependencyCondition NEVER = (src, prev, next, dst) -> false;

    /**
     * Solo propaga cuando el valor realmente cambió (delta ≠ 0).
     * Evita propagaciones de cero que no producen ningún efecto.
     */
    DependencyCondition ON_CHANGE = (src, prev, next, dst) -> next != prev;

    /**
     * Solo propaga cuando el valor aumentó (delta positivo).
     */
    DependencyCondition ON_INCREASE = (src, prev, next, dst) -> next > prev;

    /**
     * Solo propaga cuando el valor disminuyó (delta negativo).
     */
    DependencyCondition ON_DECREASE = (src, prev, next, dst) -> next < prev;

    // ── Factory methods ───────────────────────────────────────────────────

    /**
     * Solo propaga cuando el nuevo valor está por debajo de {@code threshold}.
     *
     * @param threshold umbral inferior de activación
     */
    static DependencyCondition whenBelow(double threshold) {
        return (src, prev, next, dst) -> next < threshold;
    }

    /**
     * Solo propaga cuando el nuevo valor supera {@code threshold}.
     *
     * @param threshold umbral superior de activación
     */
    static DependencyCondition whenAbove(double threshold) {
        return (src, prev, next, dst) -> next > threshold;
    }

    /**
     * Solo propaga cuando el valor cruza {@code threshold} en cualquier dirección.
     * Útil para efectos de punto de congelamiento, ebullición, etc.
     *
     * @param threshold umbral de cruce
     */
    static DependencyCondition whenCrosses(double threshold) {
        return (src, prev, next, dst) ->
            (prev < threshold && next >= threshold) ||
            (prev >= threshold && next < threshold);
    }

    /**
     * Combina dos condiciones con AND lógico.
     */
    static DependencyCondition and(DependencyCondition a, DependencyCondition b) {
        if (a == null && b == null) return ALWAYS;
        if (a == null) return b;
        if (b == null) return a;
        return (src, prev, next, dst) ->
            a.shouldPropagate(src, prev, next, dst) &&
            b.shouldPropagate(src, prev, next, dst);
    }

    /**
     * Combina dos condiciones con OR lógico.
     */
    static DependencyCondition or(DependencyCondition a, DependencyCondition b) {
        if (a == null && b == null) return NEVER;
        if (a == null) return b;
        if (b == null) return a;
        return (src, prev, next, dst) ->
            a.shouldPropagate(src, prev, next, dst) ||
            b.shouldPropagate(src, prev, next, dst);
    }
}
