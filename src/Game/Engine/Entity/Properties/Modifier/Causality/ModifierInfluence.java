package Game.Engine.Entity.Properties.Modifier.Causality;

import Game.Engine.Entity.Properties.Modifier.PropertyModifier;

/**
 * Influencia que un modificador puede ejercer sobre otro modificador.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * ModifierInfluence permite que un modificador altere el comportamiento de
 * otro modificador ANTES de que el pipeline de resolución lo procese.
 *
 * NO modifica directamente propiedades.
 * MODIFICA modificadores.
 *
 * ── DISEÑO: TRANSFORMACIÓN DE MODIFICADORES ──────────────────────────────
 * Una ModifierInfluence recibe:
 *   - El PropertyModifier que va a transformar
 *   - El ModifierContext del punto de resolución
 *
 * Y retorna un PropertyModifier transformado.
 * Si no quiere transformar el modificador, retorna el mismo objeto sin cambios.
 * Si quiere cancelar el modificador, retorna null.
 *
 * ── INMUTABILIDAD DE PropertyModifier ────────────────────────────────────
 * PropertyModifier es final e inmutable. Una influencia no puede mutarlo.
 * En su lugar, retorna un NUEVO PropertyModifier con los valores transformados.
 *
 * @see PropertyModifier
 * @see ModifierContext
 */
@FunctionalInterface
public interface ModifierInfluence {

    /**
     * Transforma un modificador dado el contexto de resolución.
     *
     * @param modifier el modificador a transformar (nunca null)
     * @param context  contexto del punto de resolución (nunca null)
     * @return el modificador transformado, el mismo si no aplica, o null
     *         para cancelar el modificador (excluirlo del pipeline)
     */
    PropertyModifier apply(PropertyModifier modifier, ModifierContext context);

    // ── Influencias predefinidas ──────────────────────────────────────────

    /** Influencia identidad: no transforma el modificador. */
    ModifierInfluence IDENTITY = (modifier, ctx) -> modifier;

    /** Influencia de cancelación: siempre retorna null. */
    ModifierInfluence CANCEL = (modifier, ctx) -> null;

    // ── Composición ───────────────────────────────────────────────────────

    /**
     * Encadena esta influencia con otra: aplica esta primero, luego la siguiente.
     * Si esta influencia cancela el modificador, la siguiente no se ejecuta.
     */
    default ModifierInfluence andThen(ModifierInfluence next) {
        if (next == null) return this;
        return (modifier, ctx) -> {
            PropertyModifier transformed = this.apply(modifier, ctx);
            if (transformed == null) return null;
            return next.apply(transformed, ctx);
        };
    }
}
