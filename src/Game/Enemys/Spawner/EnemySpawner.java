package Game.Enemys.Spawner;

import Game.Enemys.Enemy;
import Game.Enemys.EnemyFactory;
import Game.Enemys.Types.EnemyType;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.World.Core.World;

/**
 * Generador de enemigos en el mundo.
 *
 * MIGRACIÓN: eliminado el campo Player y el parámetro del constructor.
 * EnemyFactory.createEnemy() ya no necesita Player — el contexto del jugador
 * llega a cada enemigo vía EnemyContext en update(), no en construcción.
 *
 * GameWorldBootstrap solo necesita llamar new EnemySpawner() y spawn(world, n).
 */
public class EnemySpawner {

    public EnemySpawner() {}

    public void spawn(World world, int count) {
        for (int i = 0; i < count; i++) {
            EnemyType type = randomType();
            Vector2D  pos  = randomPosition(world);
            Enemy     enemy = EnemyFactory.createEnemy(type, pos);
            world.add(enemy);
        }
    }

    private EnemyType randomType() {
        EnemyType[] types = EnemyType.values();
        return types[(int)(Math.random() * types.length)];
    }

    private Vector2D randomPosition(World world) {
        double x = 50 + Math.random() * (world.getWidth()  - 100);
        double y = 50 + Math.random() * (world.getHeight() - 100);
        return new Vector2D(x, y);
    }
}
