package Game.Items.Types.Bullets.BulletComport.BulletClass;

import Game.Engine.AbstractEntity;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletID;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileData;
import Game.Items.Types.Bullets.Movement.GravityMovement;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * BulletJump — Proyectil Rebotante
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * DESCRIPCIÓN:
 *   Proyectil que rebota en superficies con conservación parcial de energía.
 *   Usa gravedad para caída parabólica natural. Destruye entidades al impactar.
 *
 * COMPORTAMIENTO DE COLISIÓN:
 *   • AbstractEntity → Daño + Destrucción
 *   • Suelo (ny=-1) → Rebote vertical adaptativo (boost o reflejo)
 *   • Techo (ny=+1) → Reflejo vertical natural
 *   • Pared (nx!=0) → Reflejo horizontal con fricción
 *
 * FÍSICA DE REBOTE:
 *   - Impacto débil (< MIN_BOUNCE_SPEED): Boost hasta velocidad mínima
 *   - Impacto fuerte (>= MIN_BOUNCE_SPEED): Reflejo natural (conserva energía)
 *   - Fricción horizontal: Pierde 1% de velocidad por rebote
 *
 * USO TÍPICO:
 *   Armas de control de área, proyectiles tácticos
 */
public class BulletJump extends BulletBehavior {

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 1: PROPIEDADES FÍSICAS
    // ═════════════════════════════════════════════════════════════════════
    
    /** Masa del proyectil (más pesado que Normal para rebotes estables) */
    private static final double MASS = 1.5;
    
    /** Escala de gravedad (1.0 = gravedad completa — caída natural) */
    private static final double GRAVITY_SCALE = 1.0;
    
    /** Coeficiente de resistencia del aire (ligeramente más que Normal) */
    private static final double DRAG = 0.00015;
    
    /** Área efectiva expuesta al aire (documentado, no configurable aún) */
    private static final double EFFECTIVE_AREA = 0.0001;

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 2: PROPIEDADES DE REBOTE
    // ═════════════════════════════════════════════════════════════════════
    
    /** Velocidad mínima de rebote vertical en units/s */
    private static final double MIN_BOUNCE_SPEED = 90.0;
    
    /** Factor de fricción por rebote (1.01 = pierde 1% de velocidad horizontal) */
    private static final double FRICTION = 0.9;

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 3: PROPIEDADES DE GAMEPLAY
    // ═════════════════════════════════════════════════════════════════════
    
    /** Daño base del proyectil */
    private static final int BASE_DAMAGE = 0;
    
    /** Multiplicador de velocidad base */
    private static final double SPEED_FACTOR = 1.6;
    
    /** Tiempo de vida en segundos */
    private static final int LIFETIME = 10;
    
    /** Radio de interacción con entidades */
    private static final double INTERACTION_RADIUS = 100.0;

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 4: BEHAVIOR IMPLEMENTATION
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public ProjectileData getDefaultData() {
        return ProjectileData.flat(BASE_DAMAGE, SPEED_FACTOR, LIFETIME);
    }

    @Override
    public BulletID getBulletID() {
        return BulletID.SPRING_BULLET;
    }

    @Override
    public ProjectileMovement getDefaultMovement() {
        return new GravityMovement(GRAVITY_SCALE);
    }

    @Override
    public double getInteractionRadius(Bullet bullet) {
        return INTERACTION_RADIUS;
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 5: LIFECYCLE HOOKS
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public void onAttached(Bullet bullet) {
        super.onAttached(bullet);
        configurePhysics(bullet);
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 6: COLLISION HANDLING
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public void onCollision(Bullet bullet, GameObjects other) {
        // Colisión con entidad → Daño + Destrucción
        if (other instanceof AbstractEntity entity) {
            entity.gotDamage((int) bullet.getBulletDamage());
            bullet.getBulletLife().kill();
            return;
        }

        // Colisión con terreno → Rebote según normal de contacto
        var physics = bullet.getPhysics();
        int nx = physics.getLastContactNormalX();
        int ny = physics.getLastContactNormalY();

        bullet.getBulletLife().extend(0.5);
        
        if (ny == -1) {
            // ── Suelo: Rebote vertical adaptativo ─────────────────────────
            // FÍSICA DE REBOTE MEJORADA:
            //   - Velocidad baja (< MIN_BOUNCE_SPEED): Boost hasta mínimo
            //   - Velocidad alta (>= MIN_BOUNCE_SPEED): Reflejo natural
            double currentVy = physics.getYspeed();
            double currentVx = physics.getXspeed();
            double impactSpeed = Math.abs(currentVy);
            
            double targetVy = (impactSpeed < MIN_BOUNCE_SPEED) 
                ? -MIN_BOUNCE_SPEED 
                : -impactSpeed;
            
            double targetVx = currentVx / FRICTION;
            physics.addVelocity(targetVx - currentVx, targetVy - currentVy);

        } else if (ny == 1) {
            // ── Techo: Reflejo vertical hacia abajo ───────────────────────
            double currentVy = physics.getYspeed();
            double currentVx = physics.getXspeed();
            
            double targetVy = Math.abs(currentVy);
            double targetVx = currentVx / FRICTION;
            physics.addVelocity(targetVx - currentVx, targetVy - currentVy);

        } else if (nx != 0) {
            // ── Pared: Reflejo horizontal ─────────────────────────────────
            double currentVx = physics.getXspeed();
            double targetVx = Math.abs(currentVx) * nx / FRICTION;
            physics.addVelocity(targetVx - currentVx, 0);

        } else {
            // ── Fallback: Sin normal disponible ───────────────────────────
            double vy = physics.getYspeed();
            double vx = physics.getXspeed();
            
            if (vy > 0) {
                // Venía bajando → tratar como suelo
                double impactSpeed = Math.abs(vy);
                double targetVy = (impactSpeed < MIN_BOUNCE_SPEED) ? -MIN_BOUNCE_SPEED : -impactSpeed;
                double targetVx = vx / FRICTION;
                physics.addVelocity(targetVx - vx, targetVy - vy);
                bullet.getBulletLife().extend(1);
                
            } else if (vy < 0) {
                // Venía subiendo → tratar como techo
                double targetVy = Math.abs(vy);
                double targetVx = vx / FRICTION;
                physics.addVelocity(targetVx - vx, targetVy - vy);
                
            } else if (vx != 0) {
                // Solo horizontal → tratar como pared
                double targetVx = -vx / FRICTION;
                physics.addVelocity(targetVx - vx, 0);
                
            } else {
                // Completamente estático → aplicar impulso mínimo
                physics.addVelocity(0, -MIN_BOUNCE_SPEED - vy);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 7: CONFIGURACIÓN INTERNA
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Aplica propiedades físicas al proyectil.
     * Llamado automáticamente en onAttached().
     */
    private void configurePhysics(Bullet bullet) {
        var physics = bullet.getPhysics();
        physics.setMass(MASS);
        physics.setGravityScale(GRAVITY_SCALE);
        physics.setDrag(DRAG);
    }
}
