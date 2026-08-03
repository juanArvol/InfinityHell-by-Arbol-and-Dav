package Game.Engine.GameMath.KineticPhysics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pila de {@link MovementModifier} con identidad por clave.
 *
 * Permite acumular múltiples buffs/debuffs de forma aditiva-multiplicativa
 * sin que uno sobreescriba al otro, y removerlos individualmente cuando expiran.
 *
 * ── Uso básico ────────────────────────────────────────────────────────────
 *
 *   // Registro (desde el sistema de efectos de estado)
 *   physics.statusStack().add("poison",  ctx -> 0.60);
 *   physics.statusStack().add("wounded", ctx -> ctx.onGround() ? 0.70 : 1.0);
 *   physics.statusStack().add("haste",   ctx -> 1.30);
 *
 *   // Factor resultante en ese frame: 0.60 × 0.70 × 1.30 = 0.546
 *
 *   // Remoción cuando el efecto termina
 *   physics.statusStack().remove("poison");
 *
 *   // Stun total: sobreescribe la stack con FROZEN y la limpia al terminar
 *   physics.statusStack().add("stun", MovementModifier.FROZEN);
 *   // ... (timer expira)
 *   physics.statusStack().remove("stun");
 *
 * ── Invariantes ───────────────────────────────────────────────────────────
 *
 *   - Stack vacía → compute() devuelve 1.0 (neutro).
 *   - El orden de inserción se respeta (LinkedHashMap), aunque para
 *     modificadores puramente multiplicativos el orden no afecta el resultado.
 *   - Thread-safety: ninguna. Usar desde el hilo de juego.
 */
public final class ModifierStack {

    private final LinkedHashMap<String, MovementModifier> slots = new LinkedHashMap<>();

    /**
     * Añade o reemplaza el modificador bajo {@code key}.
     * Si ya existe una entrada con esa clave, se actualiza en su lugar.
     *
     * @param key Identificador único del efecto (p.ej. "poison", "wind_zone").
     * @param mod Modificador a aplicar. Nunca null; usar {@link MovementModifier#IDENTITY} si no hay efecto.
     */
    public void add(String key, MovementModifier mod) {
        if (key == null || mod == null) throw new IllegalArgumentException("key/mod no pueden ser null");
        slots.put(key, mod);
    }

    /**
     * Elimina el modificador asociado a {@code key}.
     * No lanza excepción si la clave no existe.
     */
    public void remove(String key) {
        slots.remove(key);
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
