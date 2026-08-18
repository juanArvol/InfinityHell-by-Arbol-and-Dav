package Game.World.Spawn;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.World.Core.World;
import Game.World.Core.WorldCache;
import Game.World.Core.WorldCoordinator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Sistema central de spawn del Engine.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * SpawnSystem es el orquestador que evalúa cada tick los SpawnRequests
 * registrados y ejecuta los spawns cuando se cumplen las condiciones.
 *
 * SpawnSystem:
 *   1. Recibe SpawnRequests y los delega al SpawnRegistry.
 *   2. Cada tick: evalúa condiciones/triggers, genera posición, construye
 *      el objeto y lo añade al mundo correcto.
 *   3. Limpia requests completados.
 *   4. Resuelve en qué mundo spawnear cuando el SpawnPoint tiene un sector
 *      explícito (WorldCoordinator). Si no, usa el mundo actual.
 *
 * ── INDEPENDENCIA ─────────────────────────────────────────────────────────
 * SpawnSystem no conoce Enemy, Player ni ningún tipo concreto del Game.
 * Solo conoce GameObjects (resultado de SpawnStrategy.create()), World
 * y WorldCoordinator (para spawn multi-sector).
 *
 * ── INTEGRACIÓN CON WorldManager ─────────────────────────────────────────
 * WorldManager crea y posee el SpawnSystem. Cualquier subsistema que
 * quiera spawnear algo llama worldManager.getSpawnSystem().register(request).
 *
 * ── EJEMPLO DE USO ────────────────────────────────────────────────────────
 *
 *   // Spawn automático de zombies cada 120 ticks hasta 5 simultáneos:
 *   // Spawna en un radio alrededor del centro de simulación (player)
 *   SpawnDescriptor zombieWave = SpawnDescriptor.builder()
 *       .id("zombie_wave")
 *       .strategy(pos -> EnemyFactory.create(EnemyId.ZOMBIE, pos))
 *       .point(SpawnPoint.random(playerX, playerY, 300.0))  // radio 300 unidades del player
 *       .maxInstances(5)
 *       .cooldown(120)
 *       .build();
 *
 *   spawnSystem.register(SpawnRequest.withCondition(zombieWave,
 *       EntityCountCondition.lessThan(5, Enemy.class)));
 *
 *   // Spawn inmediato del boss en posición fija:
 *   SpawnDescriptor boss = SpawnDescriptor.builder()
 *       .id("sans_boss")
 *       .strategy(pos -> EnemyFactory.create(sansAssembler, pos))
 *       .point(SpawnPoint.at(640, 300))
 *       .maxInstances(1)
 *       .build();
 *
 *   spawnSystem.register(SpawnRequest.immediate(boss));
 */
public final class SpawnSystem {

    private final SpawnRegistry      registry;
    private final Supplier<World>    currentWorldSupplier;
    private final WorldCache         worldCache;

    /**
     * @param currentWorldSupplier proveedor del mundo activo (siempre dinámico)
     * @param worldCache           caché de mundos para spawn multi-sector
     */
    public SpawnSystem(Supplier<World> currentWorldSupplier,
                       WorldCache worldCache) {
        this.currentWorldSupplier = currentWorldSupplier;
        this.worldCache           = worldCache;
        this.registry             = new SpawnRegistry();
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Registra un SpawnRequest para evaluación en cada tick.
     *
     * @param request el request a registrar.
     */
    public void register(SpawnRequest request) {
        registry.register(request);
    }

    /**
     * Elimina un SpawnRequest por ID de descriptor.
     *
     * @param descriptorId ID del descriptor del request a eliminar.
     * @return true si fue encontrado y eliminado.
     */
    public boolean unregister(String descriptorId) {
        return registry.unregister(descriptorId);
    }

    /** Acceso directo al request activo con ese ID (para monitoreo externo). */
    public SpawnRequest getRequest(String descriptorId) {
        return registry.get(descriptorId);
    }

    /** Acceso al registro completo para monitoreo/debug. */
    public SpawnRegistry getRegistry() { return registry; }

    // ── Update (llamado por WorldManager cada tick) ────────────────────────

    /**
     * Evalúa todos los SpawnRequests registrados y ejecuta los spawns pendientes.
     *
     * Debe llamarse una vez por tick desde WorldManager.update().
     */
    public void update() {
        World activeWorld = currentWorldSupplier.get();
        if (activeWorld == null) return;

        // Snapshot para iteración segura (register puede añadir durante el tick)
        List<SpawnRequest> snapshot = List.copyOf(registry.getAll());

        for (SpawnRequest request : snapshot) {
            // Avanzar cooldown independientemente de si spawna
            request.tickCooldown(1);

            if (!request.shouldSpawnNow(activeWorld)) continue;

            executeSpawn(request, activeWorld);
        }

        // Limpiar requests completados al final del tick
        registry.purgeCompleted();
    }

    // ── Spawn multi-sector ────────────────────────────────────────────────

    /**
     * Registra un SpawnRequest en un sector específico en vez del activo.
     * Útil para pre-poblar un sector antes de que el jugador llegue.
     *
     * @param request    el request a ejecutar una vez en el sector indicado.
     * @param targetCoord sector donde spawnear.
     */
    public void spawnInSector(SpawnRequest request, WorldCoordinator targetCoord) {
        if (worldCache == null) return;
        World targetWorld;
        synchronized (worldCache) {
            targetWorld = worldCache.get(targetCoord);
        }
        if (targetWorld == null) return;
        executeSpawn(request, targetWorld);
    }

    // ── Spawn puntual (sin registro) ──────────────────────────────────────

    /**
     * Spawna inmediatamente un objeto en el mundo activo sin registrar
     * ningún request. Equivalente a un spawn manual directo.
     *
     * @param strategy la estrategia de construcción.
     * @param position la posición en coordenadas de mundo.
     * @return el objeto spawnado, o null si la estrategia no pudo crearlo.
     */
    public GameObjects spawnNow(SpawnStrategy strategy, Vector2D position) {
        World world = currentWorldSupplier.get();
        if (world == null) return null;
        GameObjects obj = strategy.create(position);
        if (obj != null) world.addDynamic(obj);
        return obj;
    }

    /**
     * Spawna inmediatamente usando un SpawnPoint para muestrear la posición.
     *
     * @param strategy  la estrategia de construcción.
     * @param spawnPoint zona de spawn.
     * @return el objeto spawnado, o null.
     */
    public GameObjects spawnNow(SpawnStrategy strategy, SpawnPoint spawnPoint) {
        return spawnNow(strategy, spawnPoint.samplePosition());
    }

    /** Limpia todos los requests. Llamar al cambiar de mundo. */
    public void clear() {
        registry.clear();
    }

    // ── Privado ───────────────────────────────────────────────────────────

    private void executeSpawn(SpawnRequest request, World defaultWorld) {
        SpawnDescriptor descriptor = request.getDescriptor();
        SpawnPoint point = descriptor.getSpawnPoint();

        // Resolver posición — en el nuevo modelo World no tiene dimensiones propias.
        // Si no hay SpawnPoint, usar el centro del primer chunk como fallback.
        Vector2D position = (point != null)
                ? point.samplePosition()
                : new Vector2D(640.0, 360.0);

        // Resolver mundo objetivo
        World targetWorld = resolveTargetWorld(point, defaultWorld);
        if (targetWorld == null) return;

        // Crear el objeto
        GameObjects obj = descriptor.getStrategy().create(position);
        if (obj == null) return; // estrategia no pudo construir — skip silencioso

        // Añadir al mundo como entidad dinámica
        targetWorld.addDynamic(obj);

        // Notificar al request
        request.onSpawned();
    }

    /**
     * Resuelve el mundo objetivo para un spawn.
     * Si el SpawnPoint tiene sector explícito y el sector existe en cache,
     * usa ese mundo. De lo contrario usa el mundo activo.
     */
    private World resolveTargetWorld(SpawnPoint point, World defaultWorld) {
        if (point == null || point.getSector() == null) return defaultWorld;

        if (worldCache != null) {
            synchronized (worldCache) {
                World sectorWorld = worldCache.get(point.getSector());
                if (sectorWorld != null) return sectorWorld;
            }
        }

        // Sector solicitado no existe aún → usar el activo como fallback
        return defaultWorld;
    }
}
