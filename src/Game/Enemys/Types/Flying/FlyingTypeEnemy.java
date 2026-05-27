package Game.Enemys.Types.Flying;

import Game.Enemys.Enemy;
import Game.Enemys.AI.EnemyComport;
import Game.Fisics.EnemyPhysics;
import Game.Player.Player;
import GameMath.Vector2D;

import java.awt.image.BufferedImage;

/**
 * Base para enemigos voladores — sin gravedad, movimiento por steering puro.
 * Sin cambios funcionales respecto al original.
 */
public abstract class FlyingTypeEnemy extends Enemy {

    public FlyingTypeEnemy(
            Vector2D position,
            BufferedImage texture,
            int hp,
            EnemyComport comport,
            EnemyPhysics physics
    ) {
        super(position, texture, hp, comport, physics);
    }

    @Deprecated
    public FlyingTypeEnemy(
            Vector2D position,
            BufferedImage texture,
            int hp,
            EnemyComport comport,
            Player player,
            EnemyPhysics physics
    ) {
        super(position, texture, hp, comport, player, physics);
    }

    @Override
    protected void updateTypePhysics() {
        // Los voladores no tienen gravedad — el steering maneja todo el movimiento.
    }
}
