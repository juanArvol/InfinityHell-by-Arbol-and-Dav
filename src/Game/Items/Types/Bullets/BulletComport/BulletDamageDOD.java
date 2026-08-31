package Game.Items.Types.Bullets.BulletComport;

import Game.Engine.Simulation.SimulationHandle;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

/**
 * Adapter de daño de proyectiles sobre DOD storage.
 *
 * ── HRFC — Projectile DOD Migration ──────────────────────────────────────
 *
 * BulletDamageDOD elimina la duplicación del campo `damage` en Bullet.
 *
 * ANTES (OOP):
 *   class Bullet {
 *       private double damage; // duplicado
 *   }
 *
 * AHORA (DOD):
 *   PrimitiveStorage { float[] damage }
 *   BulletDamageDOD → adapter que lee/escribe storage.damage[index]
 *
 * ── API CONSERVADA ───────────────────────────────────────────────────────
 *
 * Los behaviors acceden a damage igual que antes:
 *   - bullet.getDamage()
 *   - bullet.setDamage(value)
 *
 * Internamente se traduce a acceso directo al array DOD.
 *
 * ── VALIDACIÓN DE HANDLE ─────────────────────────────────────────────────
 *
 * NO se valida el handle en cada acceso — asumimos que Bullet mantiene
 * un handle válido y lo revalida después de compact() si es necesario.
 */
public final class BulletDamageDOD {

    private final PrimitiveStorage storage;
    private final SimulationHandle handle;

    /**
     * Constructor desde EntityStore.
     *
     * @param storage PrimitiveStorage compartido
     * @param handle handle válido de la bullet
     */
    public BulletDamageDOD(PrimitiveStorage storage, SimulationHandle handle) {
        this.storage = storage;
        this.handle = handle;
    }

    /**
     * Retorna el daño que inflige este proyectil.
     */
    public double getDamage() {
        return storage.damage()[handle.index()];
    }

    /**
     * Configura el daño que inflige este proyectil.
     */
    public void setDamage(double damage) {
        storage.damage()[handle.index()] = (float) damage;
    }
}
