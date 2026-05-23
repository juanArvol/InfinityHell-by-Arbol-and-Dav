package Game.Enemys.Types.Ground;

import Game.Enemys.Enemy;
import Game.Enemys.AI.EnemyComport;
import Game.Fisics.EnemyPhysics;
import Game.Player.Player;
import GameMath.Vector2D;

import java.awt.image.BufferedImage;

public abstract class GroundTypeEnemy extends Enemy {

    public GroundTypeEnemy(
            Vector2D position,
            BufferedImage texture,
            int hp,
            EnemyComport comport,
            Player player,
            EnemyPhysics physics
    ) {
        super(position, texture, hp, comport, player, physics);
    }

    @Override
    protected void updateTypePhysics() {

        var pc = getPhysicsComponent();
        if (pc == null) return;

        pc.getPhysics().applyGravity(
                getState().isEnElSuelo()
        );
    }
}