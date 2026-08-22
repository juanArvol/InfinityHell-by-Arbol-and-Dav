package Game.Enemys;

import Game.Engine.Physics.KineticPhysics.Types.Physics2D;

/**
 * Física de enemigos.
 *
 * Implementación específica del Game que extiende Physics2D del Engine.
 * Vive en Game.Enemys porque contiene parámetros específicos del gameplay
 * de Infinity Hell.
 *
 * ── HRFC — Enemy Physics & Domain Refactor ───────────────────────────────
 *
 * MIGRADO DESDE: Game.Engine.GameMath.Physics.Implementation.EnemyPhysics
 * RAZÓN: EnemyPhysics vive en Game.Enemys para mantener la separación
 * Engine ← Game correcta.
 *
 * ELIMINADO: EnemyPhysicsConfig como configuración monolítica.
 * AHORA: Constructores directos que permiten declarar únicamente los
 * parámetros relevantes para cada tipo de enemigo.
 *
 * Para un enemigo con comportamiento especial (inmune al hielo, ralentizado
 * por veneno), registrar modificadores externos sin subclasear:
 *
 *   physics.statusStack().add(poisonEffect, ctx -> 0.5);
 */
public class EnemyPhysics extends Physics2D {

    /**
     * Constructor 1: Propiedades físicas básicas.
     * Usa defaults para aerodinámica y fricción.
     * 
     * Ideal para enemigos con física simple.
     * 
     * @param gravity gravedad propia del enemigo en u/s²
     * @param mass masa del enemigo
     */
    public EnemyPhysics(double gravity, double mass) {
        super(gravity);
        this.mass = mass;
        // Defaults
        this.effectiveArea = 1.0;
        this.dragCoefficient = 0.0003;
        this.slide = 0.8;
    }

    /**
     * Constructor 2: Control completo de propiedades aerodinámicas.
     * 
     * @param gravity gravedad propia del enemigo en u/s²
     * @param mass masa del enemigo
     * @param effectiveArea área efectiva expuesta al flujo de aire
     * @param dragCoefficient coeficiente de drag aerodinámico
     * @param slide factor de deslizamiento superficial
     */
    public EnemyPhysics(
            double gravity,
            double mass,
            double effectiveArea,
            double dragCoefficient,
            double slide
    ) {
        super(gravity);
        this.mass = mass;
        this.effectiveArea = effectiveArea;
        this.dragCoefficient = dragCoefficient;
        this.slide = slide;
    }

    /**
     * Constructor 3: Enemigos terrestres con control de aceleración.
     * 
     * Incluye parámetros de movimiento ground/air y límites de velocidad.
     * 
     * @param gravity gravedad propia del enemigo en u/s²
     * @param mass masa del enemigo
     * @param effectiveArea área efectiva expuesta al flujo de aire
     * @param dragCoefficient coeficiente de drag aerodinámico
     * @param slide factor de deslizamiento superficial
     * @param aGround aceleración en tierra
     * @param aAir aceleración en aire (air control)
     * @param speedMaxGround velocidad máxima en tierra
     * @param speedMaxAir velocidad máxima en aire
     */
    public EnemyPhysics(
            double gravity,
            double mass,
            double effectiveArea,
            double dragCoefficient,
            double slide,
            double aGround,
            double aAir,
            double speedMaxGround,
            double speedMaxAir
    ) {
        this(gravity, mass, effectiveArea, dragCoefficient, slide);
        this.aGround = aGround;
        this.aAir = aAir;
        this.speedMaxPiso = speedMaxGround;
        this.speedMaxAir = speedMaxAir;
    }
}
