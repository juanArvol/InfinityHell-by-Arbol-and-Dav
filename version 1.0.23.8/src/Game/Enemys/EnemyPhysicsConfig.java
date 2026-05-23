package Game.Enemys;

/**
 * Configuración de física de un tipo de enemigo.
 *
 * REFACTOR DESIGN-002: campos ahora son final (immutable value object).
 * En el original todos eran públicos y mutables, lo que permitía
 * modificar la configuración de física de un enemigo en runtime por error.
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
        this.gravity       = gravity;
        this.mass          = mass;
        this.aAir          = aAir;
        this.aGround       = aGround;
        this.speedMaxAir   = speedMaxAir;
        this.speedMaxGround = speedMaxGround;
        this.slide         = slide;
    }

    /** Configuración predefinida para enemigo terrestre estándar. */
    public static EnemyPhysicsConfig groundStandard() {
        return new EnemyPhysicsConfig(0.4, 1, 0.3, 0.5, 2, 3, 0.8);
    }

    /** Configuración predefinida para enemigo volador. */
    public static EnemyPhysicsConfig flyingStandard() {
        return new EnemyPhysicsConfig(0, 1, 0.2, 0.2, 2, 2, 0.9);
    }
}
