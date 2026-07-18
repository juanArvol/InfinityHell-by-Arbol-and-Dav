package Game.Engine.Entity.Stats.Modifier;

import Game.Engine.Entity.Stats.StatContributor;
import Game.Engine.Entity.Stats.StatModifier;
import Game.Engine.Entity.Stats.StatTarget;

/**
 * Agrupación interna de StatModifiers para una fuente del dominio.
 *
 * ── HRFC-012 — ModifierBundle como utilidad auxiliar opcional ───────────
 *
 * CAMBIOS RESPECTO A HRFC-011:
 *
 *   StatContributor ya no expone getContributions() — su contrato es ahora
 *   contribute(ModifierWriter). ModifierBundle deja de ser la herramienta
 *   canónica de construcción de contribuciones.
 *
 *   ModifierBundle sigue siendo válido como utilidad auxiliar interna para
 *   cualquier código que necesite construir arrays de StatModifiers fuera
 *   del flujo contributor→writer. Su API no cambia.
 *
 * ── Modelo de uso (interno al dominio) ────────────────────────────────────
 *   ModifierBundle puede seguir usándose como herramienta de construcción
 *   auxiliar dentro del método contribute() de un contributor, cuando se
 *   prefiere un flujo builder para preparar modificadores antes de escribirlos.
 *   No es necesario — contribute() puede llamar writer.add() directamente —
 *   pero sigue siendo válido como utilidad interna del objeto del dominio.
 *
 *   Ejemplo usando ModifierBundle como auxiliar dentro de contribute():
 *
 *       public class RageEffect implements StatContributor {
 *           private final double damage;
 *           private final double speed;
 *
 *           @Override
 *           public void contribute(ModifierWriter writer) {
 *               writer.add(StatTarget.COMBAT_DAMAGE,  ModifierOperations.FLAT, damage);
 *               writer.add(StatTarget.MOVEMENT_SPEED, ModifierOperations.FLAT, speed);
 *           }
 *       }
 *
 * ── Inmutabilidad ─────────────────────────────────────────────────────────
 *   El array de StatModifiers resultante es inmutable una vez construido.
 *   Cero allocations tras la construcción.
 *
 * ── Visibilidad ───────────────────────────────────────────────────────────
 *   Esta clase permanece package-visible y pública para que los objetos del
 *   dominio puedan usarla como herramienta de construcción cuando sea útil.
 *   No forma parte del contrato público de RuntimeStats.
 */
public final class ModifierBundle {

    // ── Constructor privado — no instanciar directamente ──────────────────

    private ModifierBundle() {}

    // ── Factory principal ─────────────────────────────────────────────────

    /**
     * Inicia la construcción de un array de StatModifiers para un contributor.
     *
     * <p>Este es el factory preferido cuando el contributor ya existe (el
     * caso más frecuente: se llama en el constructor del objeto del dominio
     * con {@code this}). La fuente se conoce de antemano — cero resolución
     * post-construcción, cero reconstrucciones internas.
     *
     * <pre>
     *   private final StatModifier[] contributions =
     *       ModifierBundle.forContributor(this)
     *           .add(StatTarget.COMBAT_DAMAGE, ModifierOperations.FLAT, +50.0)
     *           .build();
     * </pre>
     *
     * @param contributor contributor propietario de los modificadores. Nunca null.
     * @return builder con el contributor ya fijado como fuente.
     */
    public static Builder forContributor(StatContributor contributor) {
        if (contributor == null) throw new IllegalArgumentException("ModifierBundle.forContributor: contributor is required");
        return new Builder(contributor);
    }

    // ── Builder ───────────────────────────────────────────────────────────

    public static final class Builder {

        private static final int INITIAL_CAPACITY = 4;

        private final StatContributor contributor;
        private StatModifier[]        pending;
        private int                   count;

        private Builder(StatContributor contributor) {
            this.contributor = contributor;
            this.pending     = new StatModifier[INITIAL_CAPACITY];
        }

        /**
         * Añade un modificador con operación explícita y descripción textual.
         *
         * <p>El source del StatModifier se fija directamente al source del
         * builder — sin resolución diferida, sin reconstrucción posterior.
         *
         * @param target      estadística objetivo.
         * @param operation   estrategia matemática (FLAT, MULTIPLIER, OVERRIDE...).
         * @param value       valor numérico.
         * @param description descripción para logs/debug. No afecta lógica.
         */
        public Builder add(StatTarget target,
                           ModifierOperation operation,
                           double value,
                           String description) {
            ensureCapacity();
            pending[count++] = new StatModifier(target, operation, value, contributor, description);
            return this;
        }

        /** Añade un modificador sin descripción textual. */
        public Builder add(StatTarget target,
                           ModifierOperation operation,
                           double value) {
            return add(target, operation, value, "");
        }

        /**
         * Construye el array inmutable de StatModifiers.
         *
         * <p>Un único pass — cero reconstrucciones, cero objetos temporales.
         * Todos los StatModifiers ya llevan la fuente correcta desde su
         * construcción en {@link #add}.
         *
         * @return array de StatModifiers listo para uso interno.
         */
        public StatModifier[] build() {
            StatModifier[] result = new StatModifier[count];
            System.arraycopy(pending, 0, result, 0, count);
            return result;
        }

        private void ensureCapacity() {
            if (count < pending.length) return;
            StatModifier[] grown = new StatModifier[pending.length * 2];
            System.arraycopy(pending, 0, grown, 0, count);
            pending = grown;
        }
    }
}
