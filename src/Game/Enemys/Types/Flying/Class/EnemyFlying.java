package Game.Enemys.Types.Flying.Class;

import Game.Enemys.AI.Behaviors.FlyingBehavior;
import Game.Enemys.Types.Flying.FlyingTypeEnemy;
import Game.Engine.Components.Visuals.ShadowComponent;
import Game.Fisics.EnemyPhysics;
import Game.Player.Player;
import GameMath.Vector2D;

import java.awt.image.BufferedImage;

/**
 * Enemigo volador concreto — persigue al jugador con steering suave.
 *
 * CAMBIO vs. original:
 *   - FlyingBehavior ya no recibe Player en el constructor.
 *     El contexto llega via EnemyContext en cada update().
 *   - Sombra (ShadowComponent) conservada tal como estaba.
 *
 * El bloque comentado de Transform3D se conserva como referencia futura.
 */
public class EnemyFlying extends FlyingTypeEnemy {

    private static final double FLIGHT_Z = 80.0;

    public EnemyFlying(
            Vector2D position,
            BufferedImage texture,
            Player player,
            EnemyPhysics physics
    ) {
        super(
            position,
            texture,
            80,
            new FlyingBehavior(),   // Sin Player — recibe EnemyContext en update()
            player,                  // Legacy path en Enemy base
            physics
        );

        // Preparación futura 2.5D (ver EnemyFlying original para contexto):
        // Transform3D t3d = new Transform3D();
        // t3d.setPosition(position);
        // t3d.setZ(FLIGHT_Z);

        addComponent(new ShadowComponent(18, 7));
    }
}
