package Game.Fisics;

import Game.Enemys.EnemyPhysicsConfig;

public class EnemyPhysics extends Physics {

    public EnemyPhysics(EnemyPhysicsConfig config) {
        super(config.gravity);

        this.mass = config.mass;

        this.aAir = config.aAir;
        this.aGround = config.aGround;

        this.speedMaxAir = config.speedMaxAir;
        this.speedMaxPiso = config.speedMaxGround;

        this.slide = config.slide;
    }
}