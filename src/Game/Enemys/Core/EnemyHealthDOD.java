package Game.Enemys.Core;

import Game.Engine.Simulation.SimulationHandle;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

/**
 * Adapter de salud de enemies sobre DOD storage.
 *
 * ── HRFC — Enemy DOD Migration ────────────────────────────────────────────
 *
 * Proporciona API OOP sobre health/maxHealth arrays del PrimitiveStorage.
 *
 * ÚNICA FUENTE DE VERDAD: PrimitiveStorage
 *   - health[]
 *   - maxHealth[]
 *
 * Los componentes HealthComponent y controladores usan este adapter.
 */
public final class EnemyHealthDOD {

    private final PrimitiveStorage storage;
    private final SimulationHandle handle;

    public EnemyHealthDOD(PrimitiveStorage storage, SimulationHandle handle) {
        this.storage = storage;
        this.handle = handle;
    }

    // ── Health (read) ─────────────────────────────────────────────────────

    public float getCurrent() {
        return storage.health()[handle.index()];
    }

    public float getMax() {
        return storage.maxHealth()[handle.index()];
    }

    public boolean isAlive() {
        return storage.health()[handle.index()] > 0f;
    }

    public boolean isDead() {
        return storage.health()[handle.index()] <= 0f;
    }

    // ── Health (write) ────────────────────────────────────────────────────

    public void setCurrent(float value) {
        int idx = handle.index();
        storage.health()[idx] = Math.max(0f, Math.min(value, storage.maxHealth()[idx]));
    }

    public void setMax(float value) {
        storage.maxHealth()[handle.index()] = Math.max(1f, value);
    }

    public void damage(float amount) {
        if (amount <= 0f) return;
        int idx = handle.index();
        storage.health()[idx] = Math.max(0f, storage.health()[idx] - amount);
    }

    public void heal(float amount) {
        if (amount <= 0f) return;
        int idx = handle.index();
        float current = storage.health()[idx];
        float max = storage.maxHealth()[idx];
        storage.health()[idx] = Math.min(max, current + amount);
    }

    public void resetToMax() {
        int idx = handle.index();
        storage.health()[idx] = storage.maxHealth()[idx];
    }
}
