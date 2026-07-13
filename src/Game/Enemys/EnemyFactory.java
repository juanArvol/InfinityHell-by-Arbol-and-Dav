package Game.Enemys;

import Game.Enemys.Types.EnemyType;
import Game.Enemys.Types.Flying.Class.EnemyFlying;
import Game.Enemys.Types.Ground.Class.EnemyNormal;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Graficos.Enemys.EnemyAssets;
import java.awt.image.BufferedImage;

/**
 * Fábrica de enemigos.
 *
 * MIGRACIÓN: createEnemy() ya no recibe Player.
 * El contexto del jugador llega vía EnemyContext en cada update() del Enemy.
 * EnemySpawner y cualquier sistema de spawn tampoco necesitan conocer Player.
 */
public class EnemyFactory {

    public static Enemy createEnemy(EnemyType type, Vector2D position) {
        EnemyPhysics  physics = createPhysics(type);
        BufferedImage texture = getRandomTexture(type);

        return switch (type) {
            case NORMAL_GROUND -> new EnemyNormal(position, texture, physics);
            case FLYING        -> new EnemyFlying(position, texture, physics);
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
        if (frames == null || frames.length == 0) return null;
        return frames[(int)(Math.random() * frames.length)];
    }
}
