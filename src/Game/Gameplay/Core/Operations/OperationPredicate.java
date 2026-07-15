package Game.Gameplay.Core.Operations;

/**
 * Condición lógica que determina si una GameplayOperation debe ejecutarse.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * OperationPredicate responde a una sola pregunta:
 *
 *   "¿Debe ejecutarse esta operación dado el contexto actual?"
 *
 * Es el equivalente exacto de {@link Game.Gameplay.Core.Causality.ModifierPredicate}
 * para el sistema de operaciones.
 *
 *   ModifierPredicate  → ¿aplica este modificador? (sobre ModifierContext)
 *   OperationPredicate → ¿se ejecuta esta operación? (sobre OperationContext)
 *
 * ── DESACOPLAMIENTO TOTAL ─────────────────────────────────────────────────
 * OperationPredicate:
 *
 *   - No usa instanceof.
 *   - No conoce clases concretas de entidades.
 *   - Solo puede consultar el OperationContext que recibe.
 *   - Es completamente independiente del sistema de gameplay concreto.
 *
 * ── COMPOSICIÓN ──────────────────────────────────────────────────────────
 * Los predicados se componen con AND, OR y NOT.
 * La composición es perezosa: no evalúa hasta que se llama test().
 *
 *   // La operación aplica si la temperatura bajó Y el objetivo tiene tag ICE
 *   OperationPredicate pred = OperationPredicate.and(
 *       ctx -> ctx.getDelta() < 0,
 *       ctx -> {
 *           TagComponent tags = ctx.getTarget() != null
 *               ? ctx.getTarget().getComponent(TagComponent.class)
 *               : null;
 *           return tags != null && tags.hasTagOrAncestor(GameplayTags.ICE);
 *       }
 *   );
 *
 *   // Solo si la propiedad afectada es TEMPERATURE
 *   OperationPredicate onlyTemp = ctx ->
 *       ctx.getAffectedProperty() != null
 *       && ctx.getAffectedProperty().id().equals(PropertyKeys.TEMPERATURE.id());
 *
 *   // Combinación:
 *   OperationPredicate combined = OperationPredicate.and(onlyTemp, pred);
 *
 * ── ALWAYS / NEVER ────────────────────────────────────────────────────────
 * ALWAYS es el predicado neutro: siempre retorna true.
 * Una operación sin predicado explícito en OperationRegistry usa ALWAYS.
 *
 * NEVER es el predicado nulo: siempre retorna false.
 * Útil para desactivar una operación temporalmente sin eliminarla del registro.
 *
 * ── IMPLEMENTACIÓN ────────────────────────────────────────────────────────
 * OperationPredicate es una interfaz funcional. Cualquier lambda que tome
 * un OperationContext y retorne boolean es un predicado válido:
 *
 *   OperationPredicate p = ctx -> ctx.getFinalValue() > 100.0;
 *
 * @see OperationContext
 * @see GameplayOperation
 * @see OperationRegistry
 */
@FunctionalInterface
public interface OperationPredicate {

    /**
     * Evalúa la condición sobre el contexto dado.
     *
     * @param context contexto de operación (nunca null)
     * @return true si la operación debe ejecutarse; false para omitirla
     */
    boolean test(OperationContext context);

    // ── Predicados compuestos ─────────────────────────────────────────────

    /**
     * Retorna un predicado que evalúa true solo si TODOS los predicados dados
     * evalúan true (AND lógico). Cortocircuita al primer false.
     *
     * Si no se pasan predicados, retorna ALWAYS.
     *
     * @param predicates predicados a combinar con AND
     * @return predicado compuesto
     */
    static OperationPredicate and(OperationPredicate... predicates) {
        if (predicates == null || predicates.length == 0) return ALWAYS;
        return ctx -> {
            for (OperationPredicate p : predicates) {
                if (!p.test(ctx)) return false;
            }
            return true;
        };
    }

    /**
     * Retorna un predicado que evalúa true si AL MENOS UNO de los predicados
     * dados evalúa true (OR lógico). Cortocircuita al primer true.
     *
     * Si no se pasan predicados, retorna NEVER.
     *
     * @param predicates predicados a combinar con OR
     * @return predicado compuesto
     */
    static OperationPredicate or(OperationPredicate... predicates) {
        if (predicates == null || predicates.length == 0) return NEVER;
        return ctx -> {
            for (OperationPredicate p : predicates) {
                if (p.test(ctx)) return true;
            }
            return false;
        };
    }

    /**
     * Retorna la negación lógica del predicado dado (NOT).
     *
     * @param predicate predicado a negar
     * @return predicado que retorna !predicate.test(ctx)
     */
    static OperationPredicate not(OperationPredicate predicate) {
        if (predicate == null) return NEVER;
        return ctx -> !predicate.test(ctx);
    }

    // ── Predicados de conveniencia ────────────────────────────────────────

    /**
     * Retorna un predicado que verifica que el delta de la propiedad afectada
     * es menor que {@code threshold}.
     *
     * Ejemplo: solo ejecutar FreezeOperation cuando la temperatura baja más de 5:
     *   OperationPredicate.deltaBelow(-5.0)
     */
    static OperationPredicate deltaBelow(double threshold) {
        return ctx -> ctx.getDelta() < threshold;
    }

    /**
     * Retorna un predicado que verifica que el delta es mayor que {@code threshold}.
     */
    static OperationPredicate deltaAbove(double threshold) {
        return ctx -> ctx.getDelta() > threshold;
    }

    /**
     * Retorna un predicado que verifica que el valor final supera un umbral.
     *
     * Ejemplo: solo ejecutar si Temperature > 50:
     *   OperationPredicate.finalValueAbove(50.0)
     */
    static OperationPredicate finalValueAbove(double threshold) {
        return ctx -> ctx.getFinalValue() > threshold;
    }

    /**
     * Retorna un predicado que verifica que el valor final está por debajo de un umbral.
     */
    static OperationPredicate finalValueBelow(double threshold) {
        return ctx -> ctx.getFinalValue() < threshold;
    }

    /**
     * Retorna un predicado que verifica que la propiedad afectada tiene el ID indicado.
     *
     * Ejemplo: solo ejecutar cuando la propiedad afectada es TEMPERATURE:
     *   OperationPredicate.onProperty(PropertyKeys.TEMPERATURE)
     */
    static OperationPredicate onProperty(Game.Gameplay.Core.Properties.PropertyKey<?> key) {
        if (key == null) return NEVER;
        return ctx -> ctx.getAffectedProperty() != null
                   && ctx.getAffectedProperty().id().equals(key.id());
    }

    /**
     * Retorna un predicado que verifica que existe un target no null.
     */
    static OperationPredicate hasTarget() {
        return ctx -> ctx.getTarget() != null;
    }

    /**
     * Retorna un predicado que verifica que existe un source no null.
     */
    static OperationPredicate hasSource() {
        return ctx -> ctx.getSource() != null;
    }

    // ── Predicados constantes ─────────────────────────────────────────────

    /**
     * Predicado siempre verdadero.
     *
     * Es el predicado por defecto de toda entrada en OperationRegistry.
     * Una entrada sin predicado explícito usa ALWAYS.
     */
    OperationPredicate ALWAYS = ctx -> true;

    /**
     * Predicado siempre falso.
     *
     * Desactiva una operación sin eliminarla del registro.
     * Útil para operaciones condicionales que se apagan temporalmente.
     */
    OperationPredicate NEVER = ctx -> false;
}
