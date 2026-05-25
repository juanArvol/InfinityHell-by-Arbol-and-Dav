package Game.Fisics;

import Game.Enemys.EnemyPhysicsConfig;

/**
 * Física de enemigos.
 *
 * Restaura el campo slide de EnemyPhysicsConfig.
 * La fricción de superficie se aplica encima en Physics.moveX().
 */
public class EnemyPhysics extends Physics {

    public EnemyPhysics(EnemyPhysicsConfig config) {
        super(config.gravity);

        this.mass         = config.mass;
        this.aAir         = config.aAir;
        this.aGround      = config.aGround;
        this.speedMaxAir  = config.speedMaxAir;
        this.speedMaxPiso = config.speedMaxGround;
        this.slide        = config.slide; // restaurado
    }
}
