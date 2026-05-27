package Game.Enemys;

/**
 * Configuración inmutable de física para un tipo de enemigo.
 * Sin cambios respecto al original — ya era correcto (campos final, presets).
 */
public final class EnemyPhysicsConfig {

    public final double gravity;
    public final double mass;
    public final double aAir;
    public final double aGround;
    public final double speedMaxAir;
    public final double speedMaxGround;
    public final double slide;

    public EnemyPhysicsConfig(
            double gravity,
            double mass,
            double aAir,
            double aGround,
            double speedMaxAir,
            double speedMaxGround,
            double slide
    ) {
        this.gravity        = gravity;
        this.mass           = mass;
        this.aAir           = aAir;
        this.aGround        = aGround;
        this.speedMaxAir    = speedMaxAir;
        this.speedMaxGround = speedMaxGround;
        this.slide          = slide;
    }

    public static EnemyPhysicsConfig groundStandard() {
        return new EnemyPhysicsConfig(0.4, 1, 0.3, 0.5, 2, 3, 0.8);
    }

    public static EnemyPhysicsConfig flyingStandard() {
        return new EnemyPhysicsConfig(0, 1, 0.2, 0.2, 2, 2, 0.9);
    }
}
