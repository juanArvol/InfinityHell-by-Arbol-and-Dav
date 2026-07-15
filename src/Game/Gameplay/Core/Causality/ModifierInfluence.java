package Game.Gameplay.Core.Causality;

import Game.Gameplay.Core.Modifiers.PropertyModifier;

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
 * Diferencia clave:
 *   PropertyModifier  → opera sobre una propiedad (daño, velocidad, cooldown)
 *   ModifierInfluence → opera sobre otro PropertyModifier
 *
 * ── EJEMPLOS CONCEPTUALES (NO IMPLEMENTADOS) ─────────────────────────────
 * Los siguientes son ejemplos de influencias que existirán en el juego.
 * NINGUNO se implementa aquí — solo la infraestructura.
 *
 *   +40% duración de modificadores de hechizos
 *   +20% potencia (value) de modificadores de amuletos
 *   Duplicar el valor de modificadores de fuego
 *   Reducir el valor de modificadores del bando enemigo
 *   Convertir un modificador MULTIPLICATIVE en ADDITIVE
 *   Invertir el signo del valor de un modificador
 *   Cancelar (desactivar) un modificador por completo
 *   Convertir daño de fuego en daño de hielo (cambiar la key del modificador)
 *
 * ── DISEÑO: TRANSFORMACIÓN DE MODIFICADORES ──────────────────────────────
 * Una ModifierInfluence recibe:
 *   - El PropertyModifier que va a transformar (puede ser cualquiera)
 *   - El ModifierContext del punto de resolución
 *
 * Y retorna un PropertyModifier transformado.
 * Si no quiere transformar el modificador, retorna el mismo objeto sin cambios.
 * Si quiere cancelar el modificador, retorna null.
 *
 * El PropertyResolver aplica las influencias activas sobre cada modificador
 * del contenedor antes de incluirlo en el cálculo.
 *
 * ── INMUTABILIDAD DE PropertyModifier ────────────────────────────────────
 * PropertyModifier es final e inmutable. Una influencia no puede mutarlo.
 * En su lugar, retorna un NUEVO PropertyModifier con los valores transformados.
 * Los factory methods de PropertyModifier están disponibles para ese propósito:
 *
 *   // Duplicar el valor de un modificador aditivo:
 *   @Override
 *   public PropertyModifier apply(PropertyModifier modifier, ModifierContext ctx) {
 *       if (modifier.getPhase() != PropertyModifier.Phase.ADDITIVE) return modifier;
 *       return PropertyModifier.additive(
 *           modifier.getKey(),
 *           modifier.getValue() * 2.0,
 *           modifier.getSourceId() + "+doubled"
 *       ).withCausalityFrom(modifier); // preservar cadena causal
 *   }
 *
 * ── ENCADENAMIENTO ────────────────────────────────────────────────────────
 * Las influencias se aplican en secuencia sobre el mismo modificador.
 * Si la primera influencia retorna null (cancelación), las siguientes
 * no se ejecutan — el modificador se omite del pipeline.
 *
 * ── COMPATIBILIDAD ────────────────────────────────────────────────────────
 * Un PropertyModifier sin influencias sobre él pasa por el pipeline sin
 * cambios, preservando totalmente el comportamiento de CFCC-001.
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

    /**
     * Influencia identidad: no transforma el modificador.
     * Útil como valor por defecto y en composiciones.
     */
    ModifierInfluence IDENTITY = (modifier, ctx) -> modifier;

    /**
     * Influencia de cancelación: siempre retorna null, eliminando el modificador
     * del pipeline de resolución.
     */
    ModifierInfluence CANCEL = (modifier, ctx) -> null;

    // ── Composición ───────────────────────────────────────────────────────

    /**
     * Encadena esta influencia con otra: aplica esta primero, luego la siguiente
     * sobre el resultado. Si esta influencia cancela el modificador (retorna null),
     * la siguiente no se ejecuta.
     *
     * @param next influencia a aplicar después de esta
     * @return influencia compuesta
     */
    default ModifierInfluence andThen(ModifierInfluence next) {
        if (next == null) return this;
        return (modifier, ctx) -> {
            PropertyModifier transformed = this.apply(modifier, ctx);
            if (transformed == null) return null;   // cancelado — no continuar
            return next.apply(transformed, ctx);
        };
    }
}
