package Game.Enemys.Types.Ground.Class;

import Game.Enemys.AI.Behaviors.AggressiveBehavior;
import Game.Enemys.Types.Ground.GroundTypeEnemy;
import Game.Fisics.EnemyPhysics;
import Game.Player.Player;
import GameMath.Vector2D;

import java.awt.image.BufferedImage;

/**
 * Enemigo terrestre estándar — persigue al jugador en el eje X.
 *
 * CAMBIO vs. original:
 *   - Eliminado draw(Graphics g) manual que duplicaba el renderer del engine.
 *     El SpriteRenderer del MovingObjects ya maneja el dibujo correctamente.
 *   - Eliminado campo `texture` duplicado (ya existe en MovingObjects/GameObjects).
 *   - AggressiveBehavior ya no recibe Player — lo recibe vía EnemyContext en update().
 *
 * Retro-compatible: EnemyFactory sigue funcionando con el constructor legacy.
 */
public class EnemyNormal extends GroundTypeEnemy {

    public EnemyNormal(
            Vector2D position,
            BufferedImage texture,
            Player player,
            EnemyPhysics physics
    ) {
        super(
            position,
            texture,
            100,
            new AggressiveBehavior(),   // Ya no recibe Player
            player,                     // Legacy path en Enemy base
            physics
        );
    }
}
