package Game.Living.Stats.Modifier;

/**
 * Implementaciones estándar de ModifierOperation.
 *
 * ── HRFC-009 ──────────────────────────────────────────────────────────────
 * Contiene las tres operaciones base del sistema RPG:
 *   FLAT       — suma un valor absoluto.
 *   MULTIPLIER — multiplica el acumulador.
 *   OVERRIDE   — sustituye el resultado final.
 *
 * Son singleton inmutables. Cero allocations al usarlos como referencia.
 *
 * ── Uso (con StatContributor — HRFC-012) ─────────────────────────────────
 *   new StatModifier(StatTarget.COMBAT_DAMAGE,  ModifierOperations.FLAT,       +15.0, this)
 *   new StatModifier(StatTarget.MOVEMENT_SPEED, ModifierOperations.MULTIPLIER,   1.5, this)
 *
 *   // O desde contribute():
 *   writer.add(StatTarget.COMBAT_DAMAGE,  ModifierOperations.FLAT,  +15.0, "Rage dmg")
 *   writer.add(StatTarget.MOVEMENT_SPEED, ModifierOperations.FLAT,  +10.0, "Rage spd")
 *       .build()
 *
 * ── Extensión ─────────────────────────────────────────────────────────────
 * Para añadir una operación nueva (p.ej. ADDITIVE_PERCENT):
 *   1. Añadir una nueva constante aquí implementando ModifierOperation.
 *   2. Usar en StatModifier.
 *   ✓ RuntimeStats y ModifierBucket NO cambian.
 */
public final class ModifierOperations {

    // ── Operación FLAT ────────────────────────────────────────────────────

    /**
     * Añade un valor absoluto al acumulador.
     *
     * <p>Ejemplo: base=10, modificadores FLAT(+3) y FLAT(-1) → resultado=12.
     * <p>Prioridad 0 — se aplica primero.
     */
    public static final ModifierOperation FLAT = new ModifierOperation() {
        @Override
        public double apply(double accumulator, double modifierValue) {
            return accumulator + modifierValue;
        }

        @Override
        public int priority() { return 0; }

        @Override
        public String toString() { return "FLAT"; }
    };

    // ── Operación MULTIPLIER ──────────────────────────────────────────────

    /**
     * Multiplica el acumulador por el valor del modificador.
     *
     * <p>Ejemplo: base=10 + FLAT(+2) = 12, luego MULTIPLIER(1.5) → 18.
     * <p>1.0 = sin cambio. 1.5 = +50%. 0.8 = -20%.
     * <p>Prioridad 10 — se aplica después de todos los FLAT.
     */
    public static final ModifierOperation MULTIPLIER = new ModifierOperation() {
        @Override
        public double apply(double accumulator, double modifierValue) {
            return accumulator * modifierValue;
        }

        @Override
        public int priority() { return 10; }

        @Override
        public String toString() { return "MULTIPLIER"; }
    };

    // ── Operación OVERRIDE ────────────────────────────────────────────────

    /**
     * Sustituye el resultado final por el valor del modificador.
     *
     * <p>Ignora el valor base y todos los modificadores FLAT y MULTIPLIER
     * del bucket. Si hay múltiples OVERRIDE, el último insertado gana
     * (el de mayor índice en el bucket, por orden de inserción).
     * <p>Prioridad 100 — se evalúa al final.
     */
    public static final ModifierOperation OVERRIDE = new ModifierOperation() {
        @Override
        public double apply(double accumulator, double modifierValue) {
            // El valor de override se devuelve directamente.
            // ModifierBucket detecta isOverriding()=true y usa este valor.
            return modifierValue;
        }

        @Override
        public int priority() { return 100; }

        @Override
        public boolean isOverriding() { return true; }

        @Override
        public String toString() { return "OVERRIDE"; }
    };

    // ── Enum de compatibilidad ────────────────────────────────────────────

    /**
     * Enum de conveniencia para los tres tipos estándar.
     * Usado por los constructores de retrocompatibilidad de StatModifier.
     * Permite migración incremental del código existente que usaba
     * el enum Type embebido en la versión anterior de StatModifier.
     */
    public enum StandardType {
        /** Equivale a ModifierOperations.FLAT. */
        FLAT,
        /** Equivale a ModifierOperations.MULTIPLIER. */
        MULTIPLIER,
        /** Equivale a ModifierOperations.OVERRIDE. */
        OVERRIDE
    }

    // ── Constructor privado — clase de utilidad ───────────────────────────
    private ModifierOperations() {}
}
