package Game.Enemys;

/**
 * Configuración inmutable de física para un tipo de enemigo.
 *
 * ── HRFC — Consolidación Final de Kinetic Physics ────────────────────────
 *
 * Añadidas propiedades aerodinámicas (effectiveArea, dragCoefficient)
 * para eliminar la dependencia de maxFallSpeed artificial.
 */
public final class EnemyPhysicsConfig {

    public final double gravity;
    public final double mass;
    public final double aAir;
    public final double aGround;
    public final double speedMaxAir;
    public final double speedMaxGround;
    public final double slide;

    // ── Propiedades aerodinámicas (HRFC — Consolidación) ─────────────────
    public final double effectiveArea;
    public final double dragCoefficient;

    public EnemyPhysicsConfig(
            double gravity,
            double mass,
            double aAir,
            double aGround,
            double speedMaxAir,
            double speedMaxGround,
            double slide,
            double effectiveArea,
            double dragCoefficient
    ) {
        this.gravity          = gravity;
        this.mass             = mass;
        this.aAir             = aAir;
        this.aGround          = aGround;
        this.speedMaxAir      = speedMaxAir;
        this.speedMaxGround   = speedMaxGround;
        this.slide            = slide;
        this.effectiveArea    = effectiveArea;
        this.dragCoefficient  = dragCoefficient;
    }

    /**
     * Configuración estándar para enemigos terrestres.
     * Velocidad terminal ≈ 20 px/frame con gravity=0.4.
     */
    public static EnemyPhysicsConfig groundStandard() {
        return new EnemyPhysicsConfig(
            0.4,  // gravity
            1.0,  // mass
            0.3,  // aAir
            0.5,  // aGround
            2.0,  // speedMaxAir
            3.0,  // speedMaxGround
            0.8,  // slide
            1.0,  // effectiveArea
            0.7   // dragCoefficient
        );
    }

    /**
     * Configuración estándar para enemigos voladores.
     * Sin gravedad, drag mínimo (aerodinámica optimizada).
     */
    public static EnemyPhysicsConfig flyingStandard() {
        return new EnemyPhysicsConfig(
            0.0,  // gravity (voladores no caen)
            1.0,  // mass
            0.2,  // aAir
            0.2,  // aGround
            2.0,  // speedMaxAir
            2.0,  // speedMaxGround
            0.9,  // slide
            0.5,  // effectiveArea (más aerodinámico)
            0.3   // dragCoefficient (forma optimizada)
        );
    }
}
