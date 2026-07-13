package Game.Enemys.Types.Flying.Class;

import Game.Enemys.AI.Behaviors.FlyingBehavior;
import Game.Enemys.EnemyPhysics;
import Game.Enemys.Types.Flying.FlyingTypeEnemy;
import Game.Engine.Components.Visuals.ShadowComponent;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import java.awt.image.BufferedImage;

/**
 * Enemigo volador concreto — persigue al jugador con steering suave.
 *
 * MIGRACIÓN: eliminado el constructor legacy con Player.
 * FlyingBehavior recibe EnemyContext en cada update(); no necesita Player en construcción.
 */
public class EnemyFlying extends FlyingTypeEnemy {

    public EnemyFlying(
            Vector2D position,
            BufferedImage texture,
            EnemyPhysics physics
    ) {
        super(
            position,
            texture,
            80,
            new FlyingBehavior(),
            physics
        );

        addComponent(new ShadowComponent(18, 7));
    }
}
