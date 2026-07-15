package Game.Gameplay.Core.Causality;

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
 * ── DESACOPLAMIENTO TOTAL ─────────────────────────────────────────────────
 * ModifierPredicate:
 *
 *   - No usa instanceof.
 *   - No conoce clases concretas de entidades, armas, hechizos, ni enemigos.
 *   - Solo puede consultar el ModifierContext que recibe.
 *   - Es completamente independiente del sistema de gameplay concreto.
 *
 * ── COMPOSICIÓN ──────────────────────────────────────────────────────────
 * Los predicados se componen con los operadores estáticos AND, OR y NOT.
 * La composición es perezosa (lazy): no evalúa hasta que se llama test().
 *
 * Ejemplo de composición:
 *
 *   // El modificador aplica si la fuente es un hechizo Y la entidad tiene tag de fuego
 *   ModifierPredicate pred = ModifierPredicate
 *       .and(
 *           ctx -> ctx.getSource() != null
 *               && ctx.getTags() != null
 *               && ctx.getTags().hasTagOrAncestor(GameplayTags.FIRE),
 *           ctx -> ctx.getModifierSource() != null
 *               && ctx.getModifierSource().isOrDescendantOf(SpellSources.SPELL)
 *       );
 *
 *   // Negación:
 *   ModifierPredicate notBoss = ModifierPredicate.not(
 *       ctx -> ctx.getTags() != null && ctx.getTags().hasTag(GameplayTags.BOSS)
 *   );
 *
 * ── ALWAYS / NEVER ────────────────────────────────────────────────────────
 * ALWAYS es el predicado neutro: siempre retorna true.
 * Un modificador sin predicado explícito usa ALWAYS, garantizando
 * compatibilidad total con CFCC-001 — los modificadores existentes
 * se comportan exactamente igual que antes.
 *
 * NEVER es el predicado nulo: siempre retorna false.
 * Útil para desactivar un modificador sin eliminarlo del contenedor.
 *
 * ── IMPLEMENTACIÓN ────────────────────────────────────────────────────────
 * ModifierPredicate es una interfaz funcional: cualquier lambda que tome
 * un ModifierContext y retorne boolean es un predicado válido.
 *
 *   ModifierPredicate p = ctx -> ctx.getCurrentValue() > 0;
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

    /**
     * Retorna un predicado que evalúa true solo si TODOS los predicados dados
     * evalúan true (AND lógico). Cortocircuita al primer false.
     *
     * Si no se pasan predicados, retorna ALWAYS.
     *
     * @param predicates predicados a combinar con AND
     * @return predicado compuesto
     */
    static ModifierPredicate and(ModifierPredicate... predicates) {
        if (predicates == null || predicates.length == 0) return ALWAYS;
        return ctx -> {
            for (ModifierPredicate p : predicates) {
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
    static ModifierPredicate or(ModifierPredicate... predicates) {
        if (predicates == null || predicates.length == 0) return NEVER;
        return ctx -> {
            for (ModifierPredicate p : predicates) {
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
    static ModifierPredicate not(ModifierPredicate predicate) {
        if (predicate == null) return NEVER;
        return ctx -> !predicate.test(ctx);
    }

    // ── Predicados constantes ─────────────────────────────────────────────

    /**
     * Predicado siempre verdadero.
     *
     * Es el predicado por defecto de todo PropertyModifier. Un modificador
     * sin predicado usa ALWAYS, preservando la compatibilidad con CFCC-001.
     */
    ModifierPredicate ALWAYS = ctx -> true;

    /**
     * Predicado siempre falso.
     *
     * Desactiva un modificador sin eliminarlo del contenedor.
     * Útil para modificadores condicionales que se apagan temporalmente.
     */
    ModifierPredicate NEVER  = ctx -> false;
}
