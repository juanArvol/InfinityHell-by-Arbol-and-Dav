package Game.Enemys;

import Game.Enemys.Types.EnemyType;
import Game.Enemys.Types.Flying.Class.EnemyFlying;
import Game.Enemys.Types.Ground.Class.EnemyNormal;
import Game.Fisics.EnemyPhysics;
import Game.Player.Player;
import GameMath.Vector2D;
import Graficos.Enemys.EnemyAssets;

import java.awt.image.BufferedImage;

/**
 * Factory de enemigos.
 * REFACTOR: usa los métodos estáticos de EnemyPhysicsConfig en lugar de
 * construir la config inline con muchos parámetros sin nombre.
 */
public class EnemyFactory {

    public static Enemy createEnemy(EnemyType type, Vector2D position, Player player) {
        EnemyPhysics physics = createPhysics(type);
        BufferedImage texture = getRandomTexture(type);

        return switch (type) {
            case NORMAL_GROUND -> new EnemyNormal(position, texture, player, physics);
            case FLYING        -> new EnemyFlying(position, texture, player, physics);
        };
    }

    private static EnemyPhysics createPhysics(EnemyType type) {
        EnemyPhysicsConfig config = switch (type) {
            case NORMAL_GROUND -> EnemyPhysicsConfig.groundStandard();
            case FLYING        -> EnemyPhysicsConfig.flyingStandard();
        };
        return new EnemyPhysics(config);
    }

    private static BufferedImage getRandomTexture(EnemyType type) {
        BufferedImage[] frames = switch (type) {
            case NORMAL_GROUND -> EnemyAssets.Enormal.getFrames();
            case FLYING        -> EnemyAssets.Eflying.getFrames();
        };
        return frames[(int)(Math.random() * frames.length)];
    }
}
