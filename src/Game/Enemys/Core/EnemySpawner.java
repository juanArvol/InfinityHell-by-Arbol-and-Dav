package Game.Enemys.Core;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.World.Core.World;

/**
 * Generador de enemigos en el mundo.
 *
 * ── HRFC-005 ─────────────────────────────────────────────────────────────
 * Reemplaza Game.Enemys.Spawner.EnemySpawner (legacy).
 *
 * Usa EnemyFactory.EnemyId y EnemyFactory.create() en lugar de EnemyType
 * y el legacy EnemyFactory. El Core nunca se modifica para agregar tipos —
 * solo se añade el EnemyId correspondiente en EnemyFactory.
 *
 * ── Responsabilidad ──────────────────────────────────────────────────────
 * Crear enemies en posiciones aleatorias del mundo y añadirlos a él.
 * No decide comportamiento, no configura physics, no conoce Assemblers.
 * Solo decide cuántos, de qué tipo y dónde.
 */
public class EnemySpawner {

    public EnemySpawner() {}

    /**
     * Spawna {@code count} enemies aleatorios en el mundo.
     *
     * @param world el mundo donde se añaden los enemies.
     * @param count número de enemies a generar.
     */
    public void spawn(World world, int count) {
        EnemyFactory.EnemyId[] types = EnemyFactory.EnemyId.values();
        for (int i = 0; i < count; i++) {
            EnemyFactory.EnemyId type = types[(int)(Math.random() * types.length)];
            Vector2D pos  = randomPosition(world);
            Enemy    enemy = EnemyFactory.create(type, pos);
            world.add(enemy);
        }
    }

    /**
     * Spawna un enemy de tipo concreto en una posición concreta.
     * Útil para spawns scripted (triggers de nivel, eventos de gameplay).
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
     * Útil para Bosses (SansAssembler) o variantes especiales.
     *
     * @param world     el mundo donde se añade el enemy.
     * @param assembler el assembler que construye el enemy.
     * @param pos       posición de spawn.
     */
    public void spawnWith(World world, EnemyAssembler assembler, Vector2D pos) {
        Enemy enemy = EnemyFactory.create(assembler, pos);
        world.add(enemy);
    }

    private Vector2D randomPosition(World world) {
        double x = 50 + Math.random() * (world.getWidth()  - 100);
        double y = 50 + Math.random() * (world.getHeight() - 100);
        return new Vector2D(x, y);
    }
}
