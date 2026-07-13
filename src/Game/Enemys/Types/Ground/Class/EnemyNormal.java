package Game.Enemys.Types.Ground.Class;

import Game.Enemys.AI.Behaviors.AggressiveBehavior;
import Game.Enemys.EnemyPhysics;
import Game.Enemys.Types.Ground.GroundTypeEnemy;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import java.awt.image.BufferedImage;

/**
 * Enemigo terrestre estándar — persigue al jugador en el eje X.
 *
 * MIGRACIÓN: eliminado el constructor legacy con Player.
 * EnemyFactory ya no pasa Player; el contexto llega vía EnemyContext en update().
 */
public class EnemyNormal extends GroundTypeEnemy {

    public EnemyNormal(
            Vector2D position,
            BufferedImage texture,
            EnemyPhysics physics
    ) {
        super(
            position,
            texture,
            100,
            new AggressiveBehavior(),
            physics
        );
    }
}
