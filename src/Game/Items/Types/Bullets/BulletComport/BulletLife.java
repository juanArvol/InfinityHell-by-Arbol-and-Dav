package Game.Items.Types.Bullets.BulletComport;

/**
 * Ciclo de vida de un proyectil.
 *
 * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────────
 *
 * MIGRACIÓN TEMPORAL:
 *   BulletLife ahora usa tiempo real en segundos en lugar de ticks discretos.
 *   Esto garantiza que la duración de vida de los proyectiles es independiente
 *   del framerate.
 *
 *   ANTES (frame-based):
 *     lifeTime = 120 ticks → 2 segundos a 60 FPS, 30 segundos a 4 FPS
 *
 *   AHORA (time-based):
 *     lifeTime = 2.0 segundos → 2 segundos reales independientemente del FPS
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
 *   advance(deltaTime) — avanza el tiempo de vida en deltaTime segundos.
 *                        Llamar exactamente una vez por frame desde Bullet.update().
 *                        Clampea remaining a 0 como mínimo.
 *                        Retorna true si el proyectil sigue vivo tras el avance.
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
 *   extend(double) — extiende la vida restante en segundos. Reemplaza reset(int count)
 *                    con semántica más clara. El valor se clampea >= 0.
 *
 *   resetTo(double)— reinicia completamente el ciclo de vida a un valor nuevo.
 *                    Usado por el pool al reutilizar un proyectil.
 */
public final class BulletLife {

    private double  remaining; // segundos de vida restantes. Siempre >= 0.
    private boolean dead;      // true = muerto por kill() o por tiempo agotado.

    /**
     * @param initialLife duración de vida inicial en segundos. Debe ser > 0.
     */
    public BulletLife(double initialLife) {
        if (initialLife <= 0.0) initialLife = 0.001; // defensivo — nunca 0 de inicio
        this.remaining = initialLife;
        this.dead      = false;
    }

    // ── Avance de tiempo ──────────────────────────────────────────────────

    /**
     * Avanza el tiempo de vida en deltaTime segundos.
     * Llamar exactamente una vez por frame desde Bullet.update().
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * CAMBIO: Ahora recibe deltaTime del simulation step y decrementa
     * en tiempo real, no en ticks discretos.
     *
     * @param deltaTime tiempo transcurrido en el simulation step (segundos)
     * @return true si el proyectil sigue vivo tras el avance; false si expiró.
     */
    public boolean advance(double deltaTime) {
        if (dead) return false;
        remaining -= deltaTime;
        if (remaining <= 0.0) {
            remaining = 0.0; // clampeo explícito — nunca negativo
            dead = true;
        }
        return !dead;
    }

    // ── HRFC — Consolidación y Limpieza de Legacy ────────────────────────
    // tick() fue eliminado. Usar advance() en su lugar.

    // ── Consulta de estado ────────────────────────────────────────────────

    /**
     * Consulta pura — ¿sigue vivo el proyectil?
     * No tiene efecto secundario. Seguro llamar múltiples veces por frame.
     */
    public boolean isAlive() {
        return !dead && remaining > 0.0;
    }

    /** @return segundos de vida restantes. Siempre >= 0. */
    public double getRemainingLife() { return remaining; }

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

    // ── HRFC — Consolidación y Limpieza de Legacy ────────────────────────
    // setDead() fue eliminado. Usar kill() en su lugar.

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
        if (remaining > 0.0) {
            dead = false;
        }
    }

    /**
     * Extiende la vida restante en {@code extraTime} segundos.
     * Si el proyectil estaba muerto por kill(), también lo reactiva.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * CAMBIO: Ahora recibe tiempo en segundos en lugar de ticks.
     *
     * @param extraTime segundos adicionales a añadir (debe ser > 0; ignorado si <= 0)
     */
    public void extend(double extraTime) {
        if (extraTime <= 0.0) return;
        remaining += extraTime;
        dead = false; // extender vida reactiva el proyectil
    }

    // ── HRFC — Consolidación y Limpieza de Legacy ────────────────────────
    // reset(int) fue eliminado. Usar extend(double) en su lugar.

    /**
     * Reinicia completamente el ciclo de vida a un nuevo valor.
     * Usado por el ProjectilePool al reutilizar una instancia de Bullet.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * CAMBIO: Ahora recibe tiempo en segundos en lugar de ticks.
     *
     * @param newLife nueva duración de vida en segundos. Debe ser > 0.
     */
    public void resetTo(double newLife) {
        if (newLife <= 0.0) newLife = 0.001;
        this.remaining = newLife;
        this.dead      = false;
    }
}
