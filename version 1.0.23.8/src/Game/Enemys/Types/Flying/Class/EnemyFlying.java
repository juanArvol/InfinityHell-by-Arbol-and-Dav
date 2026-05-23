package Game.Enemys.Types.Flying.Class;

import Game.Enemys.AI.Behaviors.FlyingBehavior;
import Game.Enemys.Types.Flying.FlyingTypeEnemy;
import Game.Fisics.EnemyPhysics;
import Game.Player.Player;
import GameMath.Vector2D;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class EnemyFlying extends FlyingTypeEnemy {

    private final BufferedImage texture;

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