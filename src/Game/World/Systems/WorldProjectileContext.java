package Game.World.Systems;

import Game.Engine.AbstractEntity;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileContext;
import Game.Items.Types.Bullets.Definition.ProjectilePool;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.World.Core.WorldManager;
import java.util.List;

/**
 * Implementación concreta de ProjectileContext respaldada por WorldManager.
 *
 * ── HRFC — Projectile Context ────────────────────────────────────────────
 *
 * WorldProjectileContext conecta la abstracción ProjectileContext con el
 * WorldManager real, sin que los behaviors dependan directamente de World.
 *
 * ── POOL UNIFICADO ────────────────────────────────────────────────────────
 *
 * spawnProjectile() usa el ProjectilePool inyectado en lugar de llamar
 * BulletFactory.build() directamente. Esto garantiza que los proyectiles
 * secundarios (generados desde onExpire) pasan por el mismo lifecycle que
 * cualquier otro proyectil del juego:
 *
 *   Pool → reutilización o creación → configuración → uso → release → pool
 *
 * Sin esta corrección existían dos caminos paralelos de creación:
 *   - Proyectiles normales:     ProjectileRegistry → pool.acquire()
 *   - Proyectiles secundarios:  BulletFactory.build() directamente
 *
 * Ahora ambos pasan por el pool. La decisión de reutilizar o crear la toma
 * el pool según el blueprint (behavior stateless, ResettableMovement).
 *
 * ── INYECCIÓN ─────────────────────────────────────────────────────────────
 *
 * El bootstrap lo crea e inyecta en el ProjectilePool del registry:
 *
 *   ProjectilePool pool = projectileRegistry.getPool();
 *   ProjectileContext ctx = new WorldProjectileContext(worldManager, pool);
 *   projectileRegistry.setProjectileContext(ctx);
 *
 * ── LIFECYCLE ─────────────────────────────────────────────────────────────
 *
 * WorldProjectileContext tiene lifecycle de World — vive mientras el
 * WorldManager que lo respalda está activo. shutdown() libera ambas
 * referencias para evitar retener el WorldManager y el pool al destruir el World.
 *
 * ── SIN DEPENDENCIAS CIRCULARES ───────────────────────────────────────────
 *
 *   BulletBehavior  →  ProjectileContext  (interfaz — no conoce World)
 *   WorldProjectileContext  →  WorldManager + ProjectilePool  (implementación)
 *
 * BulletBehavior nunca importa WorldManager ni ProjectilePool directamente.
 *
 * ── FINDENTITIESINRADIUS ──────────────────────────────────────────────────
 *
 * Usa el globalDynamicRegistry del WorldManager para retornar todas las
 * AbstractEntity dentro del radio dado. La búsqueda es O(n) sobre la lista
 * de entidades dinámicas vivas — suficiente para efectos de área de proyectil.
 */
public final class WorldProjectileContext implements ProjectileContext {

    /**
     * Referencia al WorldManager activo.
     * Se nullifica en shutdown() para liberar la referencia cuando el World muere.
     */
    private WorldManager worldManager;

    /**
     * Pool compartido con ProjectileRegistry.
     * Los proyectiles secundarios pasan por el mismo pool que los normales.
     * Se nullifica en shutdown().
     */
    private ProjectilePool pool;

    /**
     * @param worldManager el WorldManager del mundo activo (no debe ser null)
     * @param pool         el ProjectilePool del registry (no debe ser null)
     */
    public WorldProjectileContext(WorldManager worldManager, ProjectilePool pool) {
        if (worldManager == null) throw new IllegalArgumentException(
            "WorldProjectileContext requiere un WorldManager no-null");
        if (pool == null) throw new IllegalArgumentException(
            "WorldProjectileContext requiere un ProjectilePool no-null");
        this.worldManager = worldManager;
        this.pool         = pool;
    }

    // ── Shutdown ──────────────────────────────────────────────────────────

    /**
     * Libera las referencias al WorldManager y al pool.
     *
     * Después de llamar shutdown(), todas las operaciones son no-ops seguros.
     * Llamar cuando el World se destruye para evitar retener el WorldManager
     * y el pool como raíces de GC después de que el World termine.
     */
    public void shutdown() {
        this.worldManager = null;
        this.pool         = null;
    }

    // ── ProjectileContext ─────────────────────────────────────────────────

    /**
     * Spawnea un proyectil secundario usando el pool compartido.
     *
     * El pool decide si reutilizar una instancia existente o construir una nueva
     * según las propiedades del blueprint. El proyectil resultante pasa por el
     * mismo lifecycle que cualquier proyectil normal del juego.
     */
    @Override
    public void spawnProjectile(ProjectileBlueprint blueprint,
                                Vector2D position,
                                Vector2D direction) {
        WorldManager wm   = this.worldManager;
        ProjectilePool pl = this.pool;
        if (wm == null || pl == null) return; // World destruido — no-op seguro

        Bullet bullet = pl.acquire(blueprint, position, direction);
        wm.addDynamic(bullet);
    }

    @Override
    public List<? extends AbstractEntity> findEntitiesInRadius(Vector2D center, double radius) {
        WorldManager wm = this.worldManager;
        if (wm == null) return List.of();

        double radiusSq = radius * radius;

        return wm.getGlobalDynamicRegistry()
            .getAll()
            .stream()
            .filter(obj -> obj instanceof AbstractEntity)
            .map(obj -> (AbstractEntity) obj)
            .filter(entity -> !entity.isDead())
            .filter(entity -> {
                Vector2D pos = entity.getTransform().getPosition();
                double dx = pos.getX() - center.getX();
                double dy = pos.getY() - center.getY();
                return (dx * dx + dy * dy) <= radiusSq;
            })
            .toList();
    }

    /**
     * @return true si este contexto está activo (WorldManager no ha sido liberado)
     */
    public boolean isActive() {
        return worldManager != null;
    }
}
