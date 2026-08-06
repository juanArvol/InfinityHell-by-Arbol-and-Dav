package Game.Items.Types.Bullets;

/**
 * Ciclo de vida de un proyectil.
 *
 * ── HRFC — Projectile System Refactor ────────────────────────────────────
 *
 * PROBLEMA ANTERIOR:
 *   tick() mezclaba dos responsabilidades:
 *     1. Avanzar el estado (decrementar lifeTime).
 *     2. Retornar si sigue vivo.
 *
 *   Esto causaba un bug en el pool: reset(count) hacía lifeTime += count.
 *   Si lifeTime llegaba a negativo (tick() no clampeaba), reset producía
 *   valores incorrectos al reutilizar un proyectil.
 *
 *   Además, tick() era el nombre del método de avance de estado, generando
 *   confusión con el término "tick" del game loop.
 *
 * SOLUCIÓN:
 *   Responsabilidades claramente separadas:
 *
 *   advance()   — avanza un tick. Llamar exactamente una vez por frame
 *                 desde Bullet.update(). Clampea lifeTime a 0 como mínimo.
 *                 Retorna true si el proyectil sigue vivo tras el avance.
 *
 *   isAlive()   — consulta pura sin efecto secundario. Seguro llamar
 *                 múltiples veces por frame.
 *
 *   kill()      — marca el proyectil como muerto inmediatamente.
 *                 Reemplaza setDead() para nomenclatura consistente.
 *
 *   revive()    — reactiva un proyectil muerto si aún tiene vida restante.
 *                 Usado por PiercingAmuletWrapper y BounceAmuletWrapper.
 *
 *   extend(int) — extiende la vida restante. Reemplaza reset(int count)
 *                 con semántica más clara. El valor se clampea >= 0.
 *
 *   resetTo(int)— reinicia completamente el ciclo de vida a un valor nuevo.
 *                 Usado por el pool al reutilizar un proyectil.
 *
 * ── COMPATIBILIDAD ────────────────────────────────────────────────────────
 *   tick()   → alias de advance() para no romper código existente.
 *   setDead()→ alias de kill() para no romper código existente.
 *   reset(int count) → internamente llama extend(count). Mantenido por
 *                      compatibilidad pero marcado @Deprecated.
 */
public final class BulletLife {

    private int     remaining; // ticks de vida restantes. Siempre >= 0.
    private boolean dead;      // true = muerto por kill() o por tiempo agotado.

    /**
     * @param initialLife ticks de vida iniciales. Debe ser > 0.
     */
    public BulletLife(int initialLife) {
        if (initialLife <= 0) initialLife = 1; // defensivo — nunca 0 de inicio
        this.remaining = initialLife;
        this.dead      = false;
    }

    // ── Avance de tiempo ──────────────────────────────────────────────────

    /**
     * Avanza un tick de vida. Llamar exactamente una vez por frame desde Bullet.update().
     *
     * @return true si el proyectil sigue vivo tras el avance; false si expiró.
     */
    public boolean advance() {
        if (dead) return false;
        remaining--;
        if (remaining <= 0) {
            remaining = 0; // clampeo explícito — nunca negativo
            dead = true;
        }
        return !dead;
    }

    /**
     * @deprecated Usar {@link #advance()}. Alias de compatibilidad.
     */
    @Deprecated
    public boolean tick() {
        return advance();
    }

    // ── Consulta de estado ────────────────────────────────────────────────

    /**
     * Consulta pura — ¿sigue vivo el proyectil?
     * No tiene efecto secundario. Seguro llamar múltiples veces por frame.
     */
    public boolean isAlive() {
        return !dead && remaining > 0;
    }

    /** @return ticks de vida restantes. Siempre >= 0. */
    public int getRemainingLife() { return remaining; }

    /** @return true si fue matado por kill() o expiró por tiempo. */
    public boolean isDead() { return dead; }

    // ── Mutación controlada ───────────────────────────────────────────────

    /**
     * Mata el proyectil inmediatamente.
     * El ProjectilePool lo devolverá al pool en el próximo flush.
     */
    public void kill() {
        dead = true;
    }

    /**
     * @deprecated Usar {@link #kill()}. Alias de compatibilidad.
     */
    @Deprecated
    public void setDead() {
        kill();
    }

    /**
     * Reactiva un proyectil muerto si aún tiene vida restante.
     *
     * Usado por PiercingAmuletWrapper (el proyectil perfora y sigue vivo)
     * y BounceAmuletWrapper (el proyectil redirige a otro objetivo).
     *
     * Si remaining == 0 (el tiempo expiró), revive no tiene efecto.
     * Solo funciona cuando el proyectil fue matado por kill() con vida restante.
     */
    public void revive() {
        if (remaining > 0) {
            dead = false;
        }
    }

    /**
     * Extiende la vida restante en {@code extraTicks} ticks.
     * Si el proyectil estaba muerto por kill(), también lo reactiva.
     *
     * @param extraTicks ticks adicionales a añadir (debe ser > 0; ignorado si <= 0)
     */
    public void extend(int extraTicks) {
        if (extraTicks <= 0) return;
        remaining += extraTicks;
        dead = false; // extender vida reactiva el proyectil
    }

    /**
     * @deprecated Usar {@link #extend(int)}. Alias de compatibilidad.
     *             La semántica es idéntica: añade ticks a la vida restante.
     */
    @Deprecated
    public void reset(int count) {
        extend(count);
    }

    /**
     * Reinicia completamente el ciclo de vida a un nuevo valor.
     * Usado por el ProjectilePool al reutilizar una instancia de Bullet.
     *
     * @param newLife nueva duración de vida en ticks. Debe ser > 0.
     */
    public void resetTo(int newLife) {
        if (newLife <= 0) newLife = 1;
        this.remaining = newLife;
        this.dead      = false;
    }
}
