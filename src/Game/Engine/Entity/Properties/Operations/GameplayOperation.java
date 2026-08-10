package Game.Engine.Entity.Properties.Operations;

/**
 * Consecuencia ejecutable que ocurre en el mundo cuando una condición se cumple.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * GameplayOperation responde a una sola pregunta:
 *
 *   "¿Qué acción ocurre en el mundo cuando una condición se cumple?"
 *
 * NO modifica números directamente.
 * NO resuelve propiedades.
 * NO reemplaza PropertyModifier — los modificadores calculan valores.
 * NO reemplaza GameplayEventChannel — los eventos son interceptables antes de ocurrir.
 *
 * GameplayOperation representa las CONSECUENCIAS del mundo: lo que ocurre
 * DESPUÉS de que un valor fue resuelto y una condición fue evaluada.
 *
 * ── DIFERENCIAS CLAVE ────────────────────────────────────────────────────
 *   PropertyModifier   → calcula un valor numérico
 *   GameplayEventChannel → interceptable antes de ocurrir (puede cancelarse)
 *   GameplayOperation  → consecuencia que SE EJECUTA (no se cancela)
 *
 * ── COMPOSICIÓN ──────────────────────────────────────────────────────────
 *   GameplayOperation combined = freezeOp.andThen(playFreezeSound);
 *   combined.execute(ctx);
 *
 * @see OperationContext
 * @see OperationPredicate
 * @see OperationRegistry
 */
@FunctionalInterface
public interface GameplayOperation {

    /**
     * Ejecuta la consecuencia de mundo descrita por esta operación.
     *
     * @param context toda la información disponible en el momento de la ejecución
     */
    void execute(OperationContext context);

    // ── Composición ───────────────────────────────────────────────────────

    /** Retorna una operación compuesta: ejecuta esta primero, luego {@code next}. */
    default GameplayOperation andThen(GameplayOperation next) {
        if (next == null) return this;
        return ctx -> {
            this.execute(ctx);
            next.execute(ctx);
        };
    }

    /** Operación nula: no hace nada. */
    GameplayOperation NO_OP = ctx -> {};
}
