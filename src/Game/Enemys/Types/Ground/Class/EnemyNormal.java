package Game.Enemys.Types.Ground.Class;

import Game.Enemys.AI.Behaviors.AggressiveBehavior;
import Game.Enemys.EnemyPhysics;
import Game.Enemys.Types.Ground.GroundTypeEnemy;
import Game.Engine.Components.Visuals.AnimationController;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Sprites.Core.SpriteHandle;
import java.awt.image.BufferedImage;

/**
 * Enemigo terrestre estándar — persigue al jugador en el eje X.
 *
 * ── HRFC-002 ──────────────────────────────────────────────────────────────
 * Constructor ampliado con SpriteHandle para soportar el nuevo sistema de
 * animación orientado a datos. AnimationController toma el control del
 * SpriteRenderer desde el primer update().
 */
public class EnemyNormal extends GroundTypeEnemy {

    public EnemyNormal(
            Vector2D position,
            BufferedImage texture,
            EnemyPhysics physics,
            SpriteHandle handle
    ) {
        super(
            position,
            texture,
            100,
            new AggressiveBehavior(),
            physics
        );

        // AnimationController gestiona la animación "idle" en loop.
        // play("idle") se llama en start() automáticamente si se configura
        // un defaultKey, o bien desde un EnemyRenderer futuro.
        addComponent(new AnimationController(handle));
    }
}
