package Game.Enemys;

import Game.Engine.Physics.KineticPhysics.Types.Physics2D;

/**
 * Física de enemigos.
 *
 * Implementación específica del Game que extiende Physics2D del Engine.
 * Vive en Game.Enemys porque depende de EnemyPhysicsConfig, que es una
 * configuración específica del gameplay de Infinity Hell.
 *
 * ── HRFC — Consolidación Final de Kinetic Physics ────────────────────────
 *
 * MIGRADO DESDE: Game.Engine.GameMath.Physics.Implementation.EnemyPhysics
 * RAZÓN: EnemyPhysics importaba EnemyPhysicsConfig de Game.Enemys, creando
 * una dependencia Engine → Game que invierte la dirección correcta.
 * Al moverla a Game.Enemys, la dependencia queda dentro del Game.
 *
 * Ahora configura propiedades aerodinámicas desde EnemyPhysicsConfig,
 * eliminando la dependencia de maxFallSpeed artificial.
 *
 * Para un enemigo con comportamiento especial (inmune al hielo, ralentizado
 * por veneno), registrar modificadores externos sin subclasear:
 *
 *   physics.statusStack().add("poison", ctx -> 0.5);
 */
public class EnemyPhysics extends Physics2D {

    public EnemyPhysics(EnemyPhysicsConfig config) {
        super(config.gravity);
        this.mass            = config.mass;
        this.aAir            = config.aAir;
        this.aGround         = config.aGround;
        this.speedMaxAir     = config.speedMaxAir;
        this.speedMaxPiso    = config.speedMaxGround;
        this.slide           = config.slide;
        this.effectiveArea   = config.effectiveArea;
        this.dragCoefficient = config.dragCoefficient;
        // mediumDensity usa el default (1.225)
    }
}
