package Game.Fisics;

import Game.Enemys.EnemyPhysicsConfig;

/**
 * Física de enemigos.
 *
 * Delega completamente a Physics.moveX() sin overrides de lógica.
 * Los modificadores de superficie y entorno se aplican automáticamente.
 *
 * Para un enemigo con comportamiento especial (e.g. un jefe inmune al hielo
 * o un minion ralentizado por veneno), usar setStatusModifier() desde
 * el propio Enemy, sin necesidad de subclasear EnemyPhysics:
 *
 *   physics.setStatusModifier(ctx -> poisoned ? 0.5 : 1.0);
 *
 * Si el enemigo tiene atributos de entidad distintos (e.g. más pesado),
 * sobreescribir computeEntityModifier() en una subclase de EnemyPhysics.
 */
public class EnemyPhysics extends Physics {

    public EnemyPhysics(EnemyPhysicsConfig config) {
        super(config.gravity);
        this.mass         = config.mass;
        this.aAir         = config.aAir;
        this.aGround      = config.aGround;
        this.speedMaxAir  = config.speedMaxAir;
        this.speedMaxPiso = config.speedMaxGround;
        this.slide        = config.slide;
    }
}
