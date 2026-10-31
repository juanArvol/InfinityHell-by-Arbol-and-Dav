package Game.Enemys;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;
import Game.Engine.Simulation.SimulationHandle;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

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
 *
 * ── DOD Integration ───────────────────────────────────────────────────────
 *
 * EnemyPhysics ahora lee/escribe velocity desde PrimitiveStorage en lugar
 * de usar el Vector2D interno heredado de Physics2D.
 * 
 * Toda la lógica de moveX(), modificadores, fricción, etc. permanece igual.
 * Solo cambia DÓNDE se almacena la velocity.
 */
public class EnemyPhysics extends Physics2D {

    // ── DOD Storage ───────────────────────────────────────────────────────
    // Public para que Physics2D pueda acceder en logging debug
    public PrimitiveStorage storage;
    public SimulationHandle handle;
    
    // Máscaras de bits para contactFlags
    private static final byte FLAG_ON_GROUND  = 0x01; // bit 0
    private static final byte FLAG_ON_WALL    = 0x02; // bit 1
    private static final byte FLAG_ON_CEILING = 0x04; // bit 2

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

    // ── DOD Integration ───────────────────────────────────────────────────

    /**
     * Conecta este EnemyPhysics al storage DOD.
     * Debe llamarse DESPUÉS de crear la entidad en EntityStore.
     * 
     * @param storage PrimitiveStorage compartido
     * @param handle handle válido de este enemy
     */
    public void connectDOD(PrimitiveStorage storage, SimulationHandle handle) {
        this.storage = storage;
        this.handle = handle;
        
        // Sincronizar Vector2D interno con DOD storage inicial
        int idx = handle.index();
        velocity.setX(storage.velocitiesX()[idx]);
        velocity.setY(storage.velocitiesY()[idx]);
    }

    /**
     * Override de getVelocity() para leer desde DOD storage.
     * 
     * CollisionsSystem llama este método, por eso DEBE sincronizar desde DOD.
     */
    @Override
    public Vector2D getVelocity() {
        if (storage != null && handle != null) {
            int idx = handle.index();
            // Sincronizar desde DOD antes de retornar
            float vx = storage.velocitiesX()[idx];
            float vy = storage.velocitiesY()[idx];
            velocity.setX(vx);
            velocity.setY(vy);
        }
        return velocity;
    }
    
    /**
     * Override de getOnGround() para leer desde DOD storage.
     * 
     * CollisionsSystem y GroundMovement llaman este método.
     */
    @Override
    public boolean getOnGround() {
        if (storage != null && handle != null) {
            int idx = handle.index();
            byte flags = storage.contactFlags()[idx];
            return (flags & FLAG_ON_GROUND) != 0;
        }
        return super.getOnGround();
    }
    
    /**
     * Override de setOnGround() para escribir en DOD storage.
     * 
     * CollisionsSystem llama este método cuando detecta colisión con el suelo.
     */
    @Override
    public void setOnGround(boolean value) {
        if (storage != null && handle != null) {
            int idx = handle.index();
            byte[] flags = storage.contactFlags();
            if (value) {
                flags[idx] |= FLAG_ON_GROUND;  // Activar bit 0
            } else {
                flags[idx] &= ~FLAG_ON_GROUND; // Desactivar bit 0
            }
            // También sincronizar a variable interna OOP
            onGround = value;
        } else {
            super.setOnGround(value);
        }
    }
    
    /**
     * Override de getOnWall() para leer desde DOD storage.
     */
    @Override
    public boolean getOnWall() {
        if (storage != null && handle != null) {
            int idx = handle.index();
            byte flags = storage.contactFlags()[idx];
            return (flags & FLAG_ON_WALL) != 0;
        }
        return super.getOnWall();
    }
    
    /**
     * Override de setOnWall() para escribir en DOD storage.
     */
    @Override
    public void setOnWall(boolean value) {
        if (storage != null && handle != null) {
            int idx = handle.index();
            byte[] flags = storage.contactFlags();
            if (value) {
                flags[idx] |= FLAG_ON_WALL;  // Activar bit 1
            } else {
                flags[idx] &= ~FLAG_ON_WALL; // Desactivar bit 1
            }
            onWall = value;
        } else {
            super.setOnWall(value);
        }
    }
    
    /**
     * Override de getOnCeiling() para leer desde DOD storage.
     */
    @Override
    public boolean getOnCeiling() {
        if (storage != null && handle != null) {
            int idx = handle.index();
            byte flags = storage.contactFlags()[idx];
            return (flags & FLAG_ON_CEILING) != 0;
        }
        return super.getOnCeiling();
    }
    
    /**
     * Override de setOnCeiling() para escribir en DOD storage.
     */
    @Override
    public void setOnCeiling(boolean value) {
        if (storage != null && handle != null) {
            int idx = handle.index();
            byte[] flags = storage.contactFlags();
            if (value) {
                flags[idx] |= FLAG_ON_CEILING;  // Activar bit 2
            } else {
                flags[idx] &= ~FLAG_ON_CEILING; // Desactivar bit 2
            }
            onCeiling = value;
        } else {
            super.setOnCeiling(value);
        }
    }

    /**
     * Override de vSetX() para escribir en DOD storage.
     * 
     * Llamado por moveX() al final de su cálculo.
     */
    @Override
    public void vSetX(double vX) {
        if (storage != null && handle != null) {
            int idx = handle.index();
            // Escribir a DOD storage
            storage.velocitiesX()[idx] = (float) vX;
            // Sincronizar Vector2D interno
            velocity.setX(vX);
        } else {
            super.vSetX(vX);
        }
    }

    /**
     * Override de vSetY() para escribir en DOD storage.
     */
    @Override
    public void vSetY(double vY) {
        if (storage != null && handle != null) {
            int idx = handle.index();
            // Escribir a DOD storage
            storage.velocitiesY()[idx] = (float) vY;
            // Sincronizar Vector2D interno
            velocity.setY(vY);
        } else {
            super.vSetY(vY);
        }
    }
}
