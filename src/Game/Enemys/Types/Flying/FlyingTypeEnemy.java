package Game.Enemys.Types.Flying;

import Game.Enemys.AI.EnemyComport;
import Game.Enemys.Enemy;
import Game.Enemys.EnemyPhysics;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import java.awt.image.BufferedImage;

/**
 * Base para enemigos voladores — sin gravedad, movimiento por steering puro.
 *
 * MIGRACIÓN: eliminado el constructor legacy con Player.
 * Toda la cadena EnemyFlying → FlyingTypeEnemy → Enemy usa el flujo limpio.
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

    @Override
    protected void updateTypePhysics() {
        // Los voladores no tienen gravedad — el steering maneja todo el movimiento.
    }
}
