package Game.Enemys.Types.Ground.Class;

import Game.Enemys.AI.Behaviors.AggressiveBehavior;
import Game.Enemys.Types.Ground.GroundTypeEnemy;
import Game.Fisics.EnemyPhysics;
import Game.Player.Player;
import GameMath.Vector2D;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class EnemyNormal extends GroundTypeEnemy {

    private final BufferedImage texture;

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
                new AggressiveBehavior(),
                player,
                physics
        );

        this.texture = texture;
    }

    public void draw(Graphics g) {
        var pos = getTransform().getPosition();
        g.drawImage(texture,
                (int) pos.getX(),
                (int) pos.getY(),
                null);
    }
}