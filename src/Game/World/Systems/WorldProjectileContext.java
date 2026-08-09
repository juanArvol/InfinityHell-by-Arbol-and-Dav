package Game.World.Systems;

import Game.Engine.AbstractEntity;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Spatial.LinearSpatialQuery;
import Game.Engine.Spatial.SpatialQuery;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileContext;
import Game.Items.Types.Bullets.Definition.ProjectilePool;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.World.Core.WorldManager;
import java.util.List;

/**
 * Implementación concreta de ProjectileContext respaldada por WorldManager.
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 * WorldProjectileContext conecta la abstracción ProjectileContext con el
 * WorldManager real, sin que los behaviors dependan directamente de World.
 *
 * La búsqueda espacial (findEntitiesInRadius) ahora delega a un SpatialQuery
 * del Engine en lugar de implementar el scan O(n) directamente aquí.
 * Esto desacopla la estrategia de búsqueda espacial de WorldProjectileContext:
 *
 *   WorldProjectileContext  →  SpatialQuery  →  entidades en radio
 *
 * La implementación actual usa LinearSpatialQuery (O(n), comportamiento
 * idéntico al anterior). Si en el futuro se necesita un QuadTree o
 * SpatialHash, basta con cambiar la implementación inyectada sin tocar
 * este contexto ni ningún BulletBehavior.
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
 * WorldManager que lo respalda está activo. shutdown() libera todas las
 * referencias para evitar retener el WorldManager, el pool y el
 * SpatialQuery al destruir el World.
 *
 * ── SIN DEPENDENCIAS CIRCULARES ───────────────────────────────────────────
 *
 *   BulletBehavior  →  ProjectileContext  (interfaz — no conoce World)
 *   WorldProjectileContext  →  WorldManager + ProjectilePool + SpatialQuery
 *
 * BulletBehavior nunca importa WorldManager, ProjectilePool ni SpatialQuery.
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
     * Implementación de búsqueda espacial del Engine.
     *
     * Construida sobre el globalDynamicRegistry del WorldManager mediante
     * una LinearSpatialQuery, que hace un scan O(n) idéntico al comportamiento
     * anterior de findEntitiesInRadius().
     *
     * Se nullifica en shutdown().
     */
    private SpatialQuery spatialQuery;

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

        // Construir la SpatialQuery del Engine respaldada por el registry global.
        // El Supplier captura la referencia al registry en el momento de la creación.
        // Cuando se llame findEntitiesInRadius(), el registry retornará su lista viva.
        this.spatialQuery = new LinearSpatialQuery(
            () -> worldManager.getGlobalDynamicRegistry().getAll()
        );
    }

    // ── Shutdown ──────────────────────────────────────────────────────────

    /**
     * Libera todas las referencias al WorldManager, al pool y al SpatialQuery.
     *
     * Después de llamar shutdown(), todas las operaciones son no-ops seguros.
     * Llamar cuando el World se destruye para evitar retener el WorldManager,
     * el pool y el SpatialQuery como raíces de GC.
     */
    public void shutdown() {
        this.worldManager = null;
        this.pool         = null;
        this.spatialQuery = null;
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
        WorldManager   wm = this.worldManager;
        ProjectilePool pl = this.pool;
        if (wm == null || pl == null) return; // World destruido — no-op seguro

        Bullet bullet = pl.acquire(blueprint, position, direction);
        wm.addDynamic(bullet);
    }

    /**
     * Busca entidades AbstractEntity vivas dentro de un radio dado.
     *
     * Delega la búsqueda espacial al SpatialQuery del Engine, que usa
     * LinearSpatialQuery (scan O(n) idéntico al comportamiento anterior).
     *
     * El filtro de !entity.isDead() se aplica después de la búsqueda
     * espacial para no mezclar responsabilidades en SpatialQuery.
     */
    @Override
    public List<? extends AbstractEntity> findEntitiesInRadius(
            Vector2D center, double radius) {

        SpatialQuery sq = this.spatialQuery;
        if (sq == null) return List.of(); // World destruido — no-op seguro

        // Buscar AbstractEntity en radio, luego filtrar vivas
        return sq.findInRadius(center, radius, AbstractEntity.class)
                 .stream()
                 .filter(entity -> !entity.isDead())
                 .toList();
    }

    /**
     * @return true si este contexto está activo (WorldManager no ha sido liberado)
     */
    public boolean isActive() {
        return worldManager != null;
    }

    /**
     * Acceso al SpatialQuery para sistemas externos que necesiten búsqueda
     * espacial sin pasar por ProjectileContext.
     *
     * Retorna SpatialQuery.NULL si el contexto ha sido destruido.
     *
     * @return SpatialQuery activo, o NULL si el mundo fue destruido
     */
    public SpatialQuery getSpatialQuery() {
        SpatialQuery sq = this.spatialQuery;
        return sq != null ? sq : SpatialQuery.NULL;
    }
}
