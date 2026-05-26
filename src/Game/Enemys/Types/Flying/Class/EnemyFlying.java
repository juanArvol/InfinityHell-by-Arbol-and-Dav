package Game.Enemys.Types.Flying.Class;

import Game.Enemys.AI.Behaviors.FlyingBehavior;
import Game.Enemys.Types.Flying.FlyingTypeEnemy;
import Game.Engine.Components.Visuals.ShadowComponent;
import Game.Engine.Transform3D;
import Game.Fisics.EnemyPhysics;
import Game.Player.Player;
import GameMath.Vector2D;

import java.awt.image.BufferedImage;

/**
 * Enemigo volador concreto.
 *
 * NUEVO 2.5D: usa Transform3D para estar elevado en el eje Z.
 * La sombra proyectada (ShadowComponent) se dibuja en el suelo.
 */
public class EnemyFlying extends FlyingTypeEnemy {

    /** Altura de vuelo en unidades de Z. */
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
            new FlyingBehavior(player),
            player,
            physics
        );

        // NUEVO 2.5D: si queremos soporte visual de altura, reemplazamos el Transform
        // por Transform3D y lo elevamos. (Requiere que GameObjects use Transform3D)
        // Por ahora lo mantenemos como preparación futura con comentario claro:
        //
        // Transform3D t3d = new Transform3D();
        // t3d.setPosition(position);
        // t3d.setZ(FLIGHT_Z);
        // → requiere que getTransform() retorne Transform3D (cambio en GameObjects)

        // Sombra proyectada al suelo (visible cuando Z > 0 en futuro)
        addComponent(new ShadowComponent(18, 7));
    }
}
