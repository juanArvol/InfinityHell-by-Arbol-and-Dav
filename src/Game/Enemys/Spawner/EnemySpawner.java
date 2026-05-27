package Game.Enemys.Spawner;

import Game.Enemys.*;
import Game.Enemys.Types.EnemyType;
import Game.Player.Player;
import Game.World.Core.World;
import GameMath.Vector2D;

public class EnemySpawner {

    private final Player player;

    public EnemySpawner(Player player){
        this.player = player;
    }

    public void spawn(World world, int count){
        for(int i = 0; i < count; i++){
            EnemyType type = randomType();
            Vector2D pos = randomPosition(world);

            Enemy enemy = EnemyFactory.createEnemy(type, pos, player);
            world.add(enemy);
        }
    }

    private EnemyType randomType(){
        EnemyType[] types = EnemyType.values();
        return types[(int)(Math.random() * types.length)];
    }

    private Vector2D randomPosition(World world){
        double x = 50 + Math.random() * (world.getWidth() - 100); // evitar bordes
        double y = 50 + Math.random() * (world.getHeight() - 100);

        return new Vector2D(x, y);
    }
}