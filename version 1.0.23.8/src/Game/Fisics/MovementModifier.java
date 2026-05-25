package Game.Fisics;

/**
 * Interfaz para modificadores de movimiento.
 *
 * Cada implementación representa una capa independiente que escala
 * la aceleración resultante de forma multiplicativa:
 *
 *   vFinal = baseAccel
 *          × entityModifier     (masa, modo correr)
 *          × surfaceModifier    (friction, accelScale del material)
 *          × statusModifier     (estado interno: herido, stun, buff)
 *          × environmentModifier(zona externa: viento, agua, gravedad)
 *          × airControlModifier (control aéreo del surface)
 *
 * Un modificador retorna 1.0 si no tiene efecto sobre ese frame.
 * Retornar 0.0 detiene completamente el movimiento (stun, raíz).
 * Retornar >1.0 amplifica (buff de velocidad, viento a favor).
 *
 * ── Implementación mínima ─────────────────────────────────────────────
 *
 *   MovementModifier viento = ctx -> 1.4; // siempre amplifica
 *
 * ── Implementación con contexto ───────────────────────────────────────
 *
 *   MovementModifier herido = ctx -> ctx.onGround() ? 0.7 : 1.0;
 */
@FunctionalInterface
public interface MovementModifier {

    /**
     * Calcula el factor multiplicativo para este frame.
     *
     * @param ctx Snapshot de estado del objeto físico en este frame.
     * @return Factor escalar. 1.0 = sin efecto.
     */
    double compute(MovementContext ctx);

    // ── Modificadores predefinidos ────────────────────────────────────

    /** Sin efecto. Base neutral para subclases que no necesitan una capa. */
    MovementModifier IDENTITY = ctx -> 1.0;

    /** Detiene completamente el movimiento (stun, raíz, parálisis). */
    MovementModifier FROZEN   = ctx -> 0.0;
}
