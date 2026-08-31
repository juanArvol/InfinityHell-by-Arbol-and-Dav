package Game.Items.Types.Bullets.BulletComport;

import Game.Engine.Simulation.SimulationHandle;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

/**
 * Adapter de lifetime de proyectiles sobre DOD storage.
 *
 * ── HRFC — Projectile DOD Migration ──────────────────────────────────────
 *
 * BulletLifeDOD conserva la API de BulletLife pero lee/escribe directamente
 * en PrimitiveStorage via SimulationHandle.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 * BulletLifeDOD     → API de dominio (isAlive, kill, extend, revive)
 * PrimitiveStorage  → array denso storage.lifetimes[]
 * LifetimeSystem    → batch processing: lifetimes[i] -= dt
 *
 * ── CAMBIO FUNDAMENTAL ───────────────────────────────────────────────────
 *
 * ANTES (BulletLife):
 *   advance(dt) {
 *       remaining -= dt;
 *       if (remaining <= 0) dead = true;
 *   }
 *
 * DESPUÉS (BulletLifeDOD):
 *   advance(dt) ya NO decrece lifetime.
 *   LifetimeSystem decrece storage.lifetimes[] en batch.
 *   advance() solo CONSULTA si sigue vivo y actualiza flag local.
 *
 * ── API CONSERVADA ───────────────────────────────────────────────────────
 *
 * Todos los métodos de BulletLife están disponibles:
 * - advance(dt) — ahora solo consulta + actualiza flag
 * - isAlive() — consulta pura
 * - kill() — marca muerto inmediatamente
 * - revive() — reactiva si tiene vida
 * - extend(dt) — añade tiempo
 * - resetTo(dt) — reinicia para pool
 *
 * Los behaviors NO necesitan cambios — BulletLifeDOD es drop-in replacement.
 *
 * ── FLAGS EN DOD ─────────────────────────────────────────────────────────
 *
 * storage.flags[] contiene:
 *   FLAG_DEAD     (bit 0) — kill() fue llamado
 *   FLAG_EXPIRED  (bit 1) — LifetimeSystem marcó lifetime agotado
 *
 * isAlive() retorna true si:
 *   - lifetime > 0 (temporal)
 *   - FLAG_DEAD no está activo (manual)
 */
public final class BulletLifeDOD {

    private static final int FLAG_DEAD = 1 << 0;
    private static final int FLAG_EXPIRED = 1 << 1;

    private final PrimitiveStorage storage;
    private final SimulationHandle handle;

    /**
     * Constructor desde EntityStore.
     *
     * @param storage PrimitiveStorage compartido
     * @param handle handle válido de la bullet
     */
    public BulletLifeDOD(PrimitiveStorage storage, SimulationHandle handle) {
        this.storage = storage;
        this.handle = handle;
    }

    // ── Avance de tiempo ──────────────────────────────────────────────────

    /**
     * Consulta si el proyectil sigue vivo tras el avance temporal.
     *
     * IMPORTANTE: Ya NO decrece lifetime — eso lo hace LifetimeSystem.
     * Solo consulta el estado actual y retorna si sigue vivo.
     *
     * Mantiene la firma advance(dt) para compatibilidad con Bullet.update(),
     * pero el parámetro dt NO se usa aquí (LifetimeSystem lo consume).
     *
     * @param deltaTime ignorado — LifetimeSystem ya procesó el tiempo
     * @return true si sigue vivo
     */
    public boolean advance(double deltaTime) {
        return isAlive();
    }

    // ── Consulta de estado ────────────────────────────────────────────────

    /**
     * Consulta pura — ¿sigue vivo el proyectil?
     *
     * Retorna true si:
     * - lifetime > 0 (aún tiene tiempo)
     * - FLAG_DEAD no está activo (no fue killed manualmente)
     */
    public boolean isAlive() {
        int idx = handle.index();
        float lifetime = storage.lifetimes()[idx];
        int flags = storage.flags()[idx];
        return lifetime > 0f && (flags & FLAG_DEAD) == 0;
    }

    /**
     * Retorna lifetime restante en segundos.
     * Puede ser <= 0 si expiró temporalmente.
     */
    public double getRemainingLife() {
        return storage.lifetimes()[handle.index()];
    }

    /**
     * Retorna true si fue matado (manualmente o por tiempo).
     */
    public boolean isDead() {
        return !isAlive();
    }

    // ── Mutación controlada ───────────────────────────────────────────────

    /**
     * Mata el proyectil inmediatamente.
     * Marca FLAG_DEAD en el storage — ProjectileBehaviorSystem lo detectará.
     */
    public void kill() {
        int idx = handle.index();
        storage.flags()[idx] |= FLAG_DEAD;
        storage.lifetimes()[idx] = 0f; // asegurar que lifetime también es 0
    }

    /**
     * Reactiva un proyectil muerto si aún tiene vida restante.
     *
     * Usado por PiercingAmuletWrapper y BounceAmuletWrapper.
     * Solo funciona si kill() fue llamado pero lifetime > 0.
     */
    public void revive() {
        int idx = handle.index();
        if (storage.lifetimes()[idx] > 0f) {
            storage.flags()[idx] &= ~FLAG_DEAD; // clear FLAG_DEAD
        }
    }

    /**
     * Extiende la vida restante en extraTime segundos.
     * También reactiva el proyectil si estaba muerto.
     *
     * @param extraTime segundos adicionales (ignorado si <= 0)
     */
    public void extend(double extraTime) {
        if (extraTime <= 0) return;
        int idx = handle.index();
        storage.lifetimes()[idx] += (float) extraTime;
        storage.flags()[idx] &= ~FLAG_DEAD; // clear FLAG_DEAD al extender
    }

    /**
     * Reinicia completamente el ciclo de vida a un nuevo valor.
     * Usado por ProjectilePool al reutilizar una instancia de Bullet.
     *
     * @param newLife nueva duración de vida en segundos
     */
    public void resetTo(double newLife) {
        if (newLife <= 0.0) newLife = 0.001;
        int idx = handle.index();
        storage.lifetimes()[idx] = (float) newLife;
        storage.flags()[idx] = 0; // clear all flags
    }
}
