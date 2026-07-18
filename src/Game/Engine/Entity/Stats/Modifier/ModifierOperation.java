package Game.Engine.Entity.Stats.Modifier;

/**
 * Estrategia de operación matemática aplicada por un StatModifier.
 *
 * ── HRFC-009 — Open/Closed en la matemática de modificadores ─────────────
 *
 * PROBLEMA (pre-HRFC-009):
 *   RuntimeStats conocía directamente las operaciones FLAT, MULTIPLIER y
 *   OVERRIDE mediante un switch sobre StatModifier.Type:
 *
 *       case FLAT       -> result += m.getValue();
 *       case MULTIPLIER -> result *= m.getValue();
 *       case OVERRIDE   -> override = m.getValue();
 *
 *   Añadir una nueva operación (p.ej. ADDITIVE_PERCENT, CONDITIONAL,
 *   SCALING_BY_ATTRIBUTE) requería modificar RuntimeStats. Esto viola OCP.
 *
 * SOLUCIÓN:
 *   ModifierOperation es la interfaz Strategy. Cada operación matemática
 *   es una implementación concreta. RuntimeStats y ModifierBucket NUNCA
 *   conocen las operaciones — únicamente invocan apply().
 *
 *   Las implementaciones estándar (flat, multiplier, override) se exponen
 *   como constantes en ModifierOperations para uso conveniente.
 *
 * ── Arquitectura ──────────────────────────────────────────────────────────
 *
 *   ModifierOperation (interfaz)
 *       ├── ModifierOperations.FLAT       — base + valor
 *       ├── ModifierOperations.MULTIPLIER — base × valor
 *       ├── ModifierOperations.OVERRIDE   — reemplaza el resultado
 *       └── (futuras operaciones por composición/extensión)
 *
 * ── Contrato de apply() ───────────────────────────────────────────────────
 *   ModifierBucket llama apply(accumulator, modifierValue) para cada
 *   modificador y pasa el resultado acumulado al siguiente. El orden de
 *   evaluación está definido en ModifierBucket, no aquí.
 *
 *   apply() NO debe tener efectos secundarios.
 *   apply() NO debe acceder a estado externo.
 *   apply() debe ser una función pura: mismo input → mismo output.
 *
 * ── Extensión ─────────────────────────────────────────────────────────────
 *   Para añadir una nueva operación matemática:
 *     1. Implementar ModifierOperation.
 *     2. Usar la nueva implementación en StatModifier.
 *   ✓ RuntimeStats NO cambia.
 *   ✓ ModifierBucket NO cambia.
 *   ✓ ModifierContainer NO cambia.
 *   ✓ Principio Open/Closed respetado completamente.
 *
 * ── Separación de fases (para operaciones en fases separadas) ─────────────
 *   Algunas operaciones requieren una fase de acumulación y una de resolución
 *   (p.ej. OVERRIDE necesita saber si algún modificador en el bucket es OVERRIDE
 *   antes de decidir el resultado final). Para esos casos, ModifierBucket usa
 *   el método resolve() que recibe el resultado acumulado de la fase apply.
 *
 *   La mayoría de operaciones simples pueden ignorar resolve().
 */
public interface ModifierOperation {

    /**
     * Aplica este modificador sobre el acumulador actual del cálculo.
     *
     * <p>Semántica: el valor retornado se convierte en el nuevo acumulador
     * para el siguiente modificador del mismo bucket. ModifierBucket define
     * el orden de evaluación.
     *
     * @param accumulator resultado acumulado hasta este punto.
     * @param modifierValue valor del StatModifier que usa esta operación.
     * @return nuevo valor acumulado tras aplicar esta operación.
     */
    double apply(double accumulator, double modifierValue);

    /**
     * Prioridad de la operación dentro del bucket.
     *
     * <p>ModifierBucket ordena los modificadores por prioridad (menor = primero)
     * antes de aplicarlos. Esto garantiza un orden de evaluación determinista
     * y correcto independientemente del orden de inserción.
     *
     * <p>Las prioridades estándar están definidas en {@link ModifierOperations}:
     * <ul>
     *   <li>FLAT = 0 — primero</li>
     *   <li>MULTIPLIER = 10 — después de todos los FLAT</li>
     *   <li>OVERRIDE = 100 — último (gana sobre todo)</li>
     * </ul>
     *
     * <p>Las operaciones con la misma prioridad se aplican en orden de inserción.
     *
     * @return prioridad numérica. Menor = se aplica antes.
     */
    default int priority() {
        return 0;
    }

    /**
     * Indica si esta operación puede sustituir el resultado final del bucket.
     *
     * <p>ModifierBucket usa este flag para optimizar: si ningún modificador
     * activo en el bucket tiene {@code isOverriding() == true}, el resultado
     * acumulado se devuelve directamente sin necesidad de un segundo recorrido.
     *
     * <p>Solo las operaciones de tipo OVERRIDE (o equivalentes) deben devolver
     * {@code true}.
     *
     * @return true si esta operación puede sustituir el resultado final.
     */
    default boolean isOverriding() {
        return false;
    }
}
