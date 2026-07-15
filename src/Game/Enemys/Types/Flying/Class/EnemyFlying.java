package Game.Enemys.Types.Flying.Class;

import Game.Enemys.AI.Behaviors.FlyingBehavior;
import Game.Enemys.EnemyPhysics;
import Game.Enemys.Types.Flying.FlyingTypeEnemy;
import Game.Engine.Components.Visuals.AnimationController;
import Game.Engine.Components.Visuals.ShadowComponent;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Sprites.Core.SpriteHandle;
import java.awt.image.BufferedImage;

/**
 * Enemigo volador concreto — persigue al jugador con steering suave.
 *
 * ── HRFC-002 ──────────────────────────────────────────────────────────────
 * Constructor ampliado con SpriteHandle para soportar el nuevo sistema de
 * animación orientado a datos. AnimationController toma el control del
 * SpriteRenderer desde el primer update().
 */
public class EnemyFlying extends FlyingTypeEnemy {

    public EnemyFlying(
            Vector2D position,
            BufferedImage texture,
            EnemyPhysics physics,
            SpriteHandle handle
    ) {
        super(
            position,
            texture,
            80,
            new FlyingBehavior(),
            physics
        );

        addComponent(new ShadowComponent(18, 7));
        addComponent(new AnimationController(handle));
    }
}
