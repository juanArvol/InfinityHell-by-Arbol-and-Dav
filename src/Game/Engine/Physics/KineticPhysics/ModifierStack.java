package Game.Engine.Physics.KineticPhysics;

import java.util.IdentityHashMap;

/**
 * Pila de {@link MovementModifier} con identidad por source.
 *
 * ── HRFC-FASE3 — Identidad type-safe ─────────────────────────────────────
 * ── HRFC-FASE3.5 — Eliminación de String Identity ────────────────────────
 *
 * CAMBIOS RESPECTO A VERSIÓN ANTERIOR:
 *
 *   ELIMINADO: LinkedHashMap<String, MovementModifier>
 *   AÑADIDO:   IdentityHashMap<MovementModifierSource, MovementModifier>
 *   ELIMINADO: Métodos deprecated add(String) y remove(String)
 *
 *   La identidad es por referencia de objeto (==), no por String.
 *   Esto elimina colisiones de nombres y hace el sistema type-safe.
 *
 * ── Uso básico ────────────────────────────────────────────────────────────
 *
 *   // Registro (desde el sistema de efectos de estado)
 *   physics.statusStack().add(poisonEffect,  ctx -> 0.60);
 *   physics.statusStack().add(woundedEffect, ctx -> ctx.onGround() ? 0.70 : 1.0);
 *   physics.statusStack().add(hasteEffect,   ctx -> 1.30);
 *
 *   // Factor resultante en ese frame: 0.60 × 0.70 × 1.30 = 0.546
 *
 *   // Remoción cuando el efecto termina
 *   physics.statusStack().remove(poisonEffect);
 *
 *   // Stun total: registra y remueve cuando expira
 *   physics.statusStack().add(stunEffect, MovementModifier.FROZEN);
 *   // ... (timer expira)
 *   physics.statusStack().remove(stunEffect);
 *
 * ── Invariantes ───────────────────────────────────────────────────────────
 *
 *   - Stack vacía → compute() devuelve 1.0 (neutro).
 *   - El orden NO importa (todos los modificadores son multiplicativos).
 *   - IdentityHashMap garantiza identidad fuerte por referencia (==).
 *   - Thread-safety: ninguna. Usar desde el hilo de juego.
 *
 * ── Migración desde String keys ──────────────────────────────────────────
 *
 *   ANTES (String Identity - prohibido):
 *     stack.add("poison", ctx -> 0.6);
 *     stack.remove("poison");
 *
 *   AHORA (Identidad tipada - correcto):
 *     MovementModifierSource poisonEffect = ...;
 *     stack.add(poisonEffect, ctx -> 0.6);
 *     stack.remove(poisonEffect);
 */
public final class ModifierStack {

    private final IdentityHashMap<MovementModifierSource, MovementModifier> slots = new IdentityHashMap<>();

    /**
     * Añade o reemplaza el modificador bajo {@code source}.
     * Si ya existe una entrada con ese source, se actualiza en su lugar.
     *
     * @param source Source que registra este modificador (para posterior remoción).
     * @param mod    Modificador a aplicar. Nunca null; usar {@link MovementModifier#IDENTITY} si no hay efecto.
     */
    public void add(MovementModifierSource source, MovementModifier mod) {
        if (source == null || mod == null)
            throw new IllegalArgumentException("source/mod no pueden ser null");
        slots.put(source, mod);
    }

    /**
     * Elimina el modificador asociado a {@code source}.
     * No lanza excepción si el source no existe.
     *
     * @param source Source del modificador a eliminar.
     */
    public void remove(MovementModifierSource source) {
        if (source == null) return;
        slots.remove(source);
    }

    /** Elimina todos los modificadores (reset de estado). */
    public void clear() {
        slots.clear();
    }

    /** Devuelve true si no hay ningún modificador registrado. */
    public boolean isEmpty() {
        return slots.isEmpty();
    }

    /**
     * Calcula el factor compuesto multiplicando todos los modificadores activos.
     *
     * <pre>
     *   finalFactor = mod1(ctx) × mod2(ctx) × ... × modN(ctx)
     * </pre>
     *
     * Si la stack está vacía devuelve 1.0 (neutro).
     *
     * @param ctx Snapshot del frame actual.
     * @return Factor escalar listo para multiplicar en la cadena de moveX().
     */
    public double compute(MovementContext ctx) {
        double result = 1.0;
        for (MovementModifier mod : slots.values()) {
            result *= mod.compute(ctx);
        }
        return result;
    }
}
