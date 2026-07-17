package Game.Living.Stats;

import Game.Living.Stats.Modifier.ModifierOperation;
import Game.Living.Stats.Modifier.ModifierOperations;

/**
 * Modificador de estadística — unidad mínima del sistema RPG de stats.
 *
 * ── HRFC-011A — Refinamiento final del modelo de contribuciones ──────────
 *
 * CAMBIOS RESPECTO A HRFC-011:
 *
 *   El campo source cambia de tipo Object a StatContributor.
 *   NO_SOURCE pasa de ser un Object anónimo a ser una implementación
 *   anónima de StatContributor sin contribuciones. Esto elimina Object
 *   como tipo arquitectónico de la API pública sin cambiar ningún
 *   comportamiento — el mecanismo sigue siendo identidad de referencia (==).
 *
 *   Los constructores con parámetro source ahora requieren StatContributor
 *   en lugar de Object. El compilador garantiza que cualquier fuente
 *   pasada como source pertenece al dominio y puede ser revocada
 *   por RuntimeStats.revoke(contributor).
 *
 * ── Responsabilidad única ────────────────────────────────────────────────
 *   StatModifier es una value-class inmutable. Encapsula exactamente cuatro
 *   datos de dominio: qué stat modifica, cómo la modifica, en cuánto, y quién
 *   la aporta. No tiene lógica propia — delega la matemática a ModifierOperation.
 *
 * ── El campo source ───────────────────────────────────────────────────────
 *   source es la referencia al StatContributor propietario de este modificador.
 *   ModifierBucket usa == (identidad de objeto) para localizar y eliminar todos
 *   los modificadores de una fuente al llamar removeBySource(source).
 *
 *   El contributor pasa 'this' como source al construir sus StatModifiers.
 *   Cuando RuntimeStats.revoke(contributor) se llama, se eliminan por
 *   identidad == todos los StatModifiers cuya fuente sea ese contributor.
 *
 *   Caso especial:
 *     NO_SOURCE — sentinel para modificadores permanentes sin fuente rastreable.
 *                 Para modificadores añadidos con RuntimeStats.addModifier()
 *                 que nunca necesitan revocarse. Es una implementación anónima
 *                 de StatContributor sin contribuciones — existe únicamente como
 *                 sentinel de identidad para removeBySource().
 *
 * ── Inmutabilidad ────────────────────────────────────────────────────────
 *   StatModifier es completamente inmutable. Cero riesgo de mutación
 *   accidental en un bucket compartido.
 *
 * ── Strings descriptivos ─────────────────────────────────────────────────
 *   description es el único String que permanece. Es información puramente
 *   humana: logs, debug, editores futuros. NUNCA se usa como clave lógica.
 *
 * ── Uso típico (con StatContributor) ─────────────────────────────────────
 *   // En el método contribute() del contributor (this implementa StatContributor):
 *   @Override
 *   public void contribute(ModifierWriter writer) {
 *       writer.add(StatTarget.COMBAT_DAMAGE,  ModifierOperations.FLAT, +50.0);
 *       writer.add(StatTarget.MOVEMENT_SPEED, ModifierOperations.FLAT, +20.0);
 *   }
 *
 * ── Uso típico (modificador permanente) ──────────────────────────────────
 *   // Modificador que nunca se revoca — sin fuente rastreable:
 *   runtimeStats.addModifier(
 *       new StatModifier(StatTarget.MOVEMENT_SPEED, ModifierOperations.MULTIPLIER, 1.5)
 *   );
 */
public final class StatModifier {

    /**
     * Sentinel para modificadores sin origen rastreable.
     *
     * <p>Usar para modificadores permanentes que nunca se revocarán por fuente
     * (añadidos con {@link RuntimeStats#addModifier(StatModifier)}). No es null
     * para evitar NPE en los recorridos de removeBySource().
     *
     * <p>Es una implementación anónima de StatContributor sin contribuciones —
     * existe únicamente como sentinel de identidad para removeBySource().
     */
    public static final StatContributor NO_SOURCE = new StatContributor() {
        @Override
        public void contribute(Game.Living.Stats.Modifier.ModifierWriter writer) {
            // sentinel — sin contribuciones
        }

        @Override
        public String toString() { return "StatContributor[NO_SOURCE]"; }
    };

    private final StatTarget        target;
    private final ModifierOperation operation;
    private final double            value;

    /**
     * La fuente que aporta este modificador.
     * Identidad por referencia (==). Nunca null — usa NO_SOURCE si no aplica.
     * Siempre es el StatContributor propietario de las contribuciones.
     */
    private final StatContributor source;

    /** Descripción legible. Solo para logs/debug. NUNCA lógica. */
    private final String description;

    // ── Constructor principal ─────────────────────────────────────────────

    /**
     * Constructor completo.
     *
     * @param target      estadística objetivo.
     * @param operation   estrategia matemática (FLAT, MULTIPLIER, OVERRIDE, etc.).
     * @param value       valor del modificador.
     * @param source      contributor propietario de este modificador.
     *                    Pasar {@code this} desde el contributor.
     *                    Usar {@link #NO_SOURCE} para modificadores permanentes
     *                    que nunca se revocan.
     * @param description texto descriptivo para logs/debug. No afecta lógica.
     */
    public StatModifier(StatTarget target,
                        ModifierOperation operation,
                        double value,
                        StatContributor source,
                        String description) {
        if (target    == null) throw new IllegalArgumentException("StatModifier: target is required");
        if (operation == null) throw new IllegalArgumentException("StatModifier: operation is required");

        this.target      = target;
        this.operation   = operation;
        this.value       = value;
        this.source      = (source != null) ? source : NO_SOURCE;
        this.description = (description != null) ? description : "";
    }

    /** Constructor sin descripción textual. */
    public StatModifier(StatTarget target,
                        ModifierOperation operation,
                        double value,
                        StatContributor source) {
        this(target, operation, value, source, "");
    }

    /**
     * Constructor sin source ni descripción. Usa NO_SOURCE como sentinel.
     * Para modificadores permanentes que nunca se revocan por fuente.
     */
    public StatModifier(StatTarget target,
                        ModifierOperation operation,
                        double value) {
        this(target, operation, value, NO_SOURCE, "");
    }

    // ── Constructores de conveniencia — tipos estándar ────────────────────

    /**
     * Constructor de conveniencia para los tres tipos estándar.
     *
     * <p>Equivalencias:
     * <ul>
     *   <li>{@code StandardType.FLAT}       → ModifierOperations.FLAT</li>
     *   <li>{@code StandardType.MULTIPLIER} → ModifierOperations.MULTIPLIER</li>
     *   <li>{@code StandardType.OVERRIDE}   → ModifierOperations.OVERRIDE</li>
     * </ul>
     */
    public StatModifier(StatTarget target,
                        ModifierOperations.StandardType type,
                        double value,
                        StatContributor source,
                        String description) {
        this(target, resolveStandard(type), value, source, description);
    }

    public StatModifier(StatTarget target,
                        ModifierOperations.StandardType type,
                        double value,
                        StatContributor source) {
        this(target, resolveStandard(type), value, source, "");
    }

    public StatModifier(StatTarget target,
                        ModifierOperations.StandardType type,
                        double value) {
        this(target, resolveStandard(type), value, NO_SOURCE, "");
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Estadística objetivo. Nunca null. */
    public StatTarget getTarget() { return target; }

    /** Estrategia matemática. Nunca null. */
    public ModifierOperation getOperation() { return operation; }

    /** Valor numérico del modificador. */
    public double getValue() { return value; }

    /**
     * Contributor propietario de este modificador.
     * Nunca null — usa {@link #NO_SOURCE} si no se especificó fuente.
     * Usado por ModifierBucket.removeBySource() para limpiar contribuciones
     * al revocar un StatContributor.
     */
    public StatContributor getSource() { return source; }

    /**
     * Descripción legible. Solo para logs, debug y serialización.
     * NUNCA usar como clave lógica en el engine.
     */
    public String getDescription() { return description; }

    // ── Diagnóstico ───────────────────────────────────────────────────────

    @Override
    public String toString() {
        String desc = description.isEmpty() ? "" : " \"" + description + "\"";
        return "StatModifier[" + target + " " + operation + " " + value + desc + "]";
    }

    // ── Auxiliar de constructores de conveniencia ─────────────────────────

    private static ModifierOperation resolveStandard(ModifierOperations.StandardType type) {
        if (type == null) return ModifierOperations.FLAT;
        return switch (type) {
            case FLAT       -> ModifierOperations.FLAT;
            case MULTIPLIER -> ModifierOperations.MULTIPLIER;
            case OVERRIDE   -> ModifierOperations.OVERRIDE;
        };
    }
}
