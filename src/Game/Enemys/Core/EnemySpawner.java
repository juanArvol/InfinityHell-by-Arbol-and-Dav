package Game.Enemys.Core;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.World.Core.World;
import Game.World.Spawn.SpawnDescriptor;
import Game.World.Spawn.SpawnPoint;
import Game.World.Spawn.SpawnRequest;
import Game.World.Spawn.SpawnSystem;
import Game.World.Spawn.Strategies.EnemySpawnStrategy;

/**
 * Fachada de spawning de enemigos — compatibilidad con la API legada.
 *
 * ── EVOLUCIÓN ─────────────────────────────────────────────────────────────
 * EnemySpawner era el único punto de spawn y conocía World directamente.
 * Ahora es una fachada ligera sobre SpawnSystem que:
 *
 *   1. Mantiene la API original (spawn, spawnAt, spawnWith) para que el
 *      código existente (GameWorldBootstrap, tests) compile sin cambios.
 *
 *   2. Delega en SpawnSystem cuando está disponible, preservando el spawn
 *      dinámico (usa el mundo activo, no el inicial).
 *
 *   3. Si no hay SpawnSystem (modo legado / tests), usa World directamente
 *      como antes — sin romper compatibilidad.
 *
 * ── USO PREFERIDO (nuevo código) ──────────────────────────────────────────
 * El nuevo código debe construir SpawnDescriptors y SpawnRequests y
 * registrarlos en SpawnSystem. EnemySpawner es para la API de conveniencia
 * y el código de bootstrap.
 *
 * ── EJEMPLO DE MIGRACIÓN ──────────────────────────────────────────────────
 *   // Antes:
 *   new EnemySpawner().spawn(world, 5);
 *
 *   // Ahora (con SpawnSystem):
 *   SpawnDescriptor desc = SpawnDescriptor.builder()
 *       .id("initial_zombies")
 *       .strategy(EnemySpawnStrategy.random())
 *       .point(SpawnPoint.worldBounds(world.getWidth(), world.getHeight(), 60))
 *       .build();
 *   for (int i = 0; i < 5; i++) {
 *       spawnSystem.register(SpawnRequest.immediate(desc));
 *   }
 *   // O más conciso:
 *   new EnemySpawner().spawnViaSystem(spawnSystem, world, 5);
 */
public class EnemySpawner {

    public EnemySpawner() {}

    // ── API legada (retrocompatible) ──────────────────────────────────────

    /**
     * Spawna {@code count} enemies aleatorios en el mundo.
     * Usa World directamente (API legada — preservada para compatibilidad).
     *
     * @param world el mundo donde se añaden los enemies.
     * @param count número de enemies a generar.
     */
    public void spawn(World world, int count) {
        EnemyFactory.EnemyId[] types = EnemyFactory.EnemyId.values();
        for (int i = 0; i < count; i++) {
            EnemyFactory.EnemyId type = types[(int)(Math.random() * types.length)];
            Vector2D pos   = randomPosition(world);
            Enemy    enemy = EnemyFactory.create(type, pos);
            world.add(enemy);
        }
    }

    /**
     * Spawna un enemy de tipo concreto en una posición concreta.
     *
     * @param world el mundo donde se añade el enemy.
     * @param type  tipo de enemy a crear.
     * @param pos   posición exacta de spawn.
     */
    public void spawnAt(World world, EnemyFactory.EnemyId type, Vector2D pos) {
        Enemy enemy = EnemyFactory.create(type, pos);
        world.add(enemy);
    }

    /**
     * Spawna un enemy usando un Assembler custom.
     *
     * @param world     el mundo donde se añade el enemy.
     * @param assembler el assembler que construye el enemy.
     * @param pos       posición de spawn.
     */
    public void spawnWith(World world, EnemyAssembler assembler, Vector2D pos) {
        Enemy enemy = EnemyFactory.create(assembler, pos);
        world.add(enemy);
    }

    // ── API nueva (via SpawnSystem) ────────────────────────────────────────

    /**
     * Spawna {@code count} enemies aleatorios a través del SpawnSystem.
     *
     * A diferencia de spawn(World, int), este método usa el mundo activo
     * en el momento del spawn (no el primer mundo cargado), eliminando
     * el bug de "enemigos solo en la pantalla inicial".
     *
     * @param spawnSystem el SpawnSystem activo
     * @param world       el mundo para calcular el área de spawn
     * @param count       número de enemies a spawnar
     */
    public void spawnViaSystem(SpawnSystem spawnSystem, World world, int count) {
        SpawnPoint point = SpawnPoint.worldBounds(
            world.getWidth(), world.getHeight(), 60
        );

        SpawnDescriptor desc = SpawnDescriptor.builder()
            .id("enemy_spawner_" + System.nanoTime())
            .strategy(EnemySpawnStrategy.random())
            .point(point)
            .build();

        for (int i = 0; i < count; i++) {
            spawnSystem.register(SpawnRequest.immediate(desc));
        }
    }

    /**
     * Spawna un enemy concreto a través del SpawnSystem en una posición específica.
     *
     * @param spawnSystem el SpawnSystem activo
     * @param type        tipo de enemy
     * @param pos         posición de spawn
     */
    public void spawnAtViaSystem(SpawnSystem spawnSystem,
                                  EnemyFactory.EnemyId type,
                                  Vector2D pos) {
        SpawnDescriptor desc = SpawnDescriptor.builder()
            .id("enemy_at_" + System.nanoTime())
            .strategy(EnemySpawnStrategy.of(type))
            .point(SpawnPoint.at(pos))
            .build();

        spawnSystem.register(SpawnRequest.immediate(desc));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Vector2D randomPosition(World world) {
        double margin = 60.0;
        double x = margin + Math.random() * Math.max(0, world.getWidth()  - margin * 2);
        double y = margin + Math.random() * Math.max(0, world.getHeight() - margin * 2);
        return new Vector2D(x, y);
    }
}
