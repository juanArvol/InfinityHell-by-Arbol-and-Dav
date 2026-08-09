package Game.Engine.Lifecycle;

/**
 * Contexto de lifecycle basado en tiempo de vida (ticks).
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * Un objeto con LifetimeContext está activo mientras le queden ticks de vida.
 * Cuando los ticks se agotan, el contexto deja de estar activo y el objeto
 * puede ser destruido por el LifecycleSystem.
 *
 * ── GENERALIZACIÓN DE BulletLife ──────────────────────────────────────────
 *
 * Esta es la abstracción general de lo que BulletLife implementa
 * específicamente para proyectiles. La diferencia es que LifetimeContext
 * es reutilizable por cualquier entidad (partículas, efectos temporales,
 * invocaciones con duración limitada, tokens de gameplay).
 *
 * BulletLife continúa existiendo porque tiene semántica específica de
 * proyectiles (kill/revive/extend para piercing/bounce). LifetimeContext
 * es la generalización conceptual para el Engine.
 *
 * ── USOS TÍPICOS ──────────────────────────────────────────────────────────
 *
 *   // Partícula que vive 30 frames:
 *   EntityContext ctx = new LifetimeContext(30);
 *
 *   // Efecto que dura 60 frames:
 *   EntityContext ctx = new LifetimeContext(60);
 *
 *   // Cada frame:
 *   if (!ctx.isActive()) destroyParticle();
 *   else ctx.advance();
 */
public final class LifetimeContext implements EntityContext {

    private int  remaining;
    private boolean dead;

    /**
     * Crea un contexto de vida finita con la duración especificada.
     *
     * @param initialTicks ticks de vida iniciales (debe ser > 0)
     */
    public LifetimeContext(int initialTicks) {
        this.remaining = Math.max(1, initialTicks);
        this.dead      = false;
    }

    // ── EntityContext ─────────────────────────────────────────────────────

    /**
     * Retorna true mientras queden ticks de vida y no haya sido terminado.
     */
    @Override
    public boolean isActive() {
        return !dead && remaining > 0;
    }

    // ── Avance de tiempo ──────────────────────────────────────────────────

    /**
     * Avanza un tick. Llamar exactamente una vez por frame.
     *
     * @return true si el contexto sigue activo tras el avance
     */
    public boolean advance() {
        if (dead) return false;
        remaining = Math.max(0, remaining - 1);
        if (remaining == 0) dead = true;
        return !dead;
    }

    /**
     * Termina el contexto inmediatamente, independientemente del tiempo restante.
     */
    public void terminate() {
        dead = true;
    }

    /**
     * Extiende la vida en los ticks dados y reactiva si estaba terminado.
     *
     * @param extraTicks ticks a añadir (ignorado si <= 0)
     */
    public void extend(int extraTicks) {
        if (extraTicks <= 0) return;
        remaining += extraTicks;
        dead = false;
    }

    /**
     * Resetea completamente a una nueva duración.
     * Útil para reutilización por pools.
     *
     * @param newTicks nueva duración (debe ser > 0)
     */
    public void resetTo(int newTicks) {
        this.remaining = Math.max(1, newTicks);
        this.dead      = false;
    }

    /** @return ticks de vida restantes */
    public int getRemaining() { return remaining; }
}
