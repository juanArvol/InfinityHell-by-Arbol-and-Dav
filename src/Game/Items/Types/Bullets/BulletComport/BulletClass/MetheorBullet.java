package Game.Items.Types.Bullets.BulletComport.BulletClass;

import Game.Engine.AbstractEntity;
import Game.Engine.Entity.Components.Physics2DComponent;
import Game.Engine.Entity.Components.PushableComponent;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.TrajectoryProvider;
import Game.Items.Types.Bullets.BulletID;
import Game.Items.Types.Bullets.Capability.SpatialQueryCapability;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileContext;
import Game.Items.Types.Bullets.Definition.ProjectileData;
import Game.Items.Types.Bullets.Movement.GravityMovement;
import Game.Items.Types.Bullets.ProjectileMovement;
import Game.Items.Types.Bullets.ProjectileTrajectoryPredictor;
import java.util.List;
import java.util.Set;

/**
 * Behavior del MetheorBullet — proyectil de alta masa con explosión al impacto.
 *
 * ── HRFC — Consolidación y Limpieza de Legacy (MetheorBullet Migration) ───
 *
 * Este behavior reconstruye conceptualmente el MetheorBullet del sistema legacy,
 * adaptándolo a la arquitectura actual de proyectiles.
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 *
 * Proyectil pesado que:
 *   - Tiene gravedad (composición via GravityMovement)
 *   - Explota al impactar enemigos o terreno
 *   - Genera daño en área escalado con velocidad de caída
 *   - Empuje radial a entities cercanas
 *
 * ── MIGRACIÓN DESDE SISTEMA LEGACY ────────────────────────────────────────
 *
 * Sistema antiguo:
 *   - onCollisionWith(Player/EnimyNormal/Ambiente) — manual dispatch
 *   - Acceso directo a player.getEnemies()
 *   - Mutación directa de posiciones de entities
 *
 * Sistema actual:
 *   - onCollision(bullet, hitEntity) — polimorfismo via BulletBehavior
 *   - ProjectileContext.findEntitiesInRadius() — explosión en área sin conocer entities
 *   - Engine de física maneja empuje via Physics2D.addForce(), no mutación directa
 *
 * ── ESCALADO DE EXPLOSIÓN ─────────────────────────────────────────────────
 *
 * La potencia de explosión escala con la velocidad de caída:
 *
 *   explosionPower = |velocityY| × 2.3
 *   maxRadius = 250 + (explosionPower × 1.5)
 *   damage = baseDamage + (explosionPower × (1 - distance/maxRadius))
 *
 * Mientras más cae, más destrucción causa. Fidelidad conceptual al diseño original.
 *
 * ── DIFERENCIAS CON IMPLEMENTACIÓN LEGACY ─────────────────────────────────
 *
 * 1. ProjectileContext abstrae el acceso al mundo (no conoce Player directamente)
 * 2. Explosión implementada via findEntitiesInRadius + damage() + addForce()
 * 3. No hay dependencia de EnimyNormal ni Ambiente (usa AbstractEntity)
 * 4. Movement composition (GravityMovement) en lugar de hasGravity() flag
 * 5. Empuje radial via Physics2D.addForce() o PushableComponent.applyPush()
 *
 * ── REGISTRO EN BulletType ────────────────────────────────────────────────
 *
 * Para activar en el juego, añadir a BulletType.java:
 *
 *   VOIDMETEOR (MetheorBullet::new, ItemRarity.RARE,
 *               "Meteoro del Vacío",
 *               "Proyectil de alta masa que genera explosiones devastadoras."),
 */
public class MetheorBullet extends BulletBehavior {

    private static final double BASE_DAMAGE          = 0;
    private static final double GRAVITY_STRENGTH     = 1200;
    private static final double EXPLOSION_POWER_MULT = 2500;
    private static final double RADIUS_BASE          = 250.0;
    private static final double RADIUS_SCALE         = 1.5;
    private static final int    DEFAULT_LIFETIME     = 300;  // 5 segundos a 60fps
    private static final double PUSH_FORCE_SCALE     = 0.1;  // escalado de fuerza de empuje

    // ── Propiedades físicas del meteoro (HRFC — Consolidación) ───────────
    // HRFC FASE 2: Coeficientes escalados para px/frame.
    // El meteoro es masivo y poco aerodinámico, resultando en velocidad
    // terminal moderada (~25-30 px/frame) que escala la potencia de explosión.
    private static final double METEOR_MASS              = 300.0;     // 3x más masivo que proyectil normal
    private static final double METEOR_EFFECTIVE_AREA    = 15;     // área grande
    private static final double METEOR_DRAG_COEFFICIENT  = 0.0021;  // poco aerodinámico (escalado para px/frame)

    @Override
    public Set<Class<?>> getRequiredCapabilities() {
        // MetheorBullet requires spatial query for explosion area effect
        return Set.of(SpatialQueryCapability.class);
    }

    @Override
    public ProjectileData getDefaultData() {
        return new ProjectileData(
                (int) BASE_DAMAGE, // damage base del proyectil
                1.0,               // speedFactor (base speed viene de WeaponStats o BulletType)
                DEFAULT_LIFETIME,
                GRAVITY_STRENGTH,               // gravityValue (manejado por GravityMovement)
                8,                 // width
                8,                 // height
                "bullet.comet"      // assetKey
        );
    }

    @Override
    public BulletID getBulletID() {
        return BulletID.METEOR_BULLET;
    }

    @Override
    public ProjectileMovement getDefaultMovement() {
        // GravityMovement se compondrá con el movimiento lineal base del proyectil
        return new GravityMovement(GRAVITY_STRENGTH);
    }

    /**
     * Provider de trayectoria custom para MetheorBullet.
     *
     * ── UI TRAJECTORY PREDICTION ──────────────────────────────────────────
     *
     * MetheorBullet tiene gravedad extremadamente alta (1200.0) y drag significativo
     * (0.0021), resultando en una trayectoria parabólica pronunciada con velocidad
     * terminal. La UI debe representar esto fielmente.
     *
     * Este provider usa exactamente los mismos valores que el comportamiento real
     * del proyectil, garantizando consistencia entre preview y gameplay.
     */
    @Override
    public TrajectoryProvider getTrajectoryProvider() {
        return (spawnPosition, direction, speed, lifeTime) ->
                ProjectileTrajectoryPredictor.predict(
                        spawnPosition,
                        direction,
                        speed,
                        lifeTime,
                        GRAVITY_STRENGTH,        // Gravedad alta (10x estándar)
                        METEOR_DRAG_COEFFICIENT  // Drag significativo
                );
    }

    /**
     * Configurar propiedades físicas del meteoro al ser adquirido del pool
     * o recién creado.
     *
     * HRFC FASE 2: El meteoro es masivo y poco aerodinámico, produciendo
     * velocidad terminal moderada (~25-30 px/frame) que escala la potencia
     * de explosión. Coeficientes escalados para px/frame.
     */
    @Override
    public void onAttached(Bullet bullet) {
        super.onAttached(bullet);

        var physics = bullet.getPhysics();
        physics.setMass(METEOR_MASS);
        physics.setEffectiveArea(METEOR_EFFECTIVE_AREA);
        physics.setDragCoefficient(METEOR_DRAG_COEFFICIENT);
    }

    @Override
    public void onCollision(Bullet bullet, GameObjects other) {
        if(other instanceof AbstractEntity entity){
            entity.gotDamage((int) bullet.getBulletDamage());
        }
        // MetheorBullet explota al impactar cualquier objetivo válido
        // El CollisionProfile ya garantiza que solo recibimos colisiones válidas
        explode(bullet);
        
        // Matar el proyectil tras la explosión
        bullet.getBulletLife().kill();
    }

    /**
     * Genera la explosión en área con daño escalado por velocidad de caída.
     *
     * Preserva las fórmulas originales del legacy:
     *   - explosionPower = |velocityY| × 2.3
     *   - maxRadius = 250 + (explosionPower × 1.5)
     *   - damage = baseDamage + (explosionPower × (1 - distance/maxRadius))
     *   - force = (maxRadius - distance) × 0.1
     */
    private void explode(Bullet bullet) {
        
        // Calcular potencia de explosión basada en velocidad de caída
        double velocityY = bullet.getPhysics().getYspeed();
        double explosionPower = Math.abs(velocityY) * EXPLOSION_POWER_MULT;
        
        // Radio de explosión escala con la potencia
        double maxRadius = RADIUS_BASE + (explosionPower * RADIUS_SCALE);
        
        // Posición del impacto (epicentro) — sin allocation de Vector2D
        double centerX = bullet.getPositionX();
        double centerY = bullet.getPositionY();
        
        // Buscar todas las entidades en el radio de explosión
        Vector2D centerVec = new Vector2D(centerX, centerY); // allocation necesaria para findEntitiesInRadius
        var entitiesInRange = findNearbyEntities(bullet, centerVec, maxRadius);
        
        // Aplicar daño y empuje radial a cada entidad afectada
        for (AbstractEntity entity : entitiesInRange) {
            
            Vector2D entityPos = entity.getTransform().getPosition();
            double dx = entityPos.getX() - centerX;
            double dy = entityPos.getY() - centerY;
            double distance = Math.sqrt((dx * dx) + (dy * dy));
            
            // Evitar división por cero si la entidad está exactamente en el centro
            if (distance < 1.0) {
                distance = 0.000000000000000001;
            }
            
            // Calcular daño escalado con distancia (preserva fórmula legacy)
            double damageFactor = 1.0 - (distance / maxRadius);
            double finalDamage = BASE_DAMAGE * (explosionPower * damageFactor);
            
            // Aplicar daño via AbstractEntity.damage()
            entity.gotDamage((int) finalDamage);
            
            // Calcular fuerza de empuje radial (preserva fórmula legacy)
            double force = (maxRadius - distance) * PUSH_FORCE_SCALE;
            
            // Dirección normalizada desde el epicentro hacia la entidad
            double dirX = dx / distance;
            double dirY = dy / distance;
            
            // Componentes de fuerza radial
            double pushX = dirX * force;
            double pushY = dirY * force;
            
            // Aplicar empuje via sistema de física existente
            applyRadialPush(entity, pushX, pushY);
        }
    }

    /**
     * Encuentra entidades cercanas para la explosión en área.
     *
     * Usa SpatialQueryCapability mediante getCapability() del ProjectileContext.
     * Este es el patrón composable: el behavior declara qué necesita (en
     * getRequiredCapabilities) y luego consume esa capacidad específica.
     *
     * @param bullet bullet que explota
     * @param center posición central del área de explosión
     * @param radius radio de búsqueda en unidades del mundo
     * @return lista de entidades encontradas, o lista vacía si no hay capacidad disponible
     */
    private List<? extends AbstractEntity> findNearbyEntities(Bullet bullet, Vector2D center, double radius) {
        ProjectileContext context = bullet.getProjectileContext();
        
        if (context == null || context == ProjectileContext.NULL) {
            return List.of();
        }
        
        // Get the spatial query capability from the composed context
        SpatialQueryCapability spatialQuery = context.getCapability(SpatialQueryCapability.class);
        
        if (spatialQuery == null) {
            // Context doesn't provide spatial query - shouldn't happen if blueprint requirements were met
            return List.of();
        }
        
        return spatialQuery.findEntitiesInRadius(center, radius);
    }

    /**
     * Aplica empuje radial a una entidad usando el sistema de física existente.
     *
     * Intenta usar PushableComponent si existe (para objetos del mundo),
     * o Physics2DComponent.addForce() directamente (para entidades con física).
     *
     * @param entity entidad a empujar
     * @param fx     componente X de la fuerza
     * @param fy     componente Y de la fuerza
     */
    private void applyRadialPush(AbstractEntity entity, double fx, double fy) {
        // Intentar usar PushableComponent primero (wrapper sobre addForce)
        PushableComponent pushable = entity.getComponent(PushableComponent.class);
        if (pushable != null) {
            pushable.applyPush(fx, fy);
            return;
        }
        
        // Si no hay PushableComponent, aplicar fuerza directamente via Physics2D
        Physics2DComponent physicsComp = entity.getComponent(Physics2DComponent.class);
        if (physicsComp != null) {
            physicsComp.getPhysics().addForce(fx, fy);
        }
        
        // Si la entidad no tiene física, el empuje no tiene efecto
        // (esto es correcto — solo objetos con física reaccionan al knockback)
    }

    @Override
    public void onExpire(Bullet bullet, ProjectileContext ctx) {
        // MetheorBullet no explota al expirar (solo al impactar)
        // Simplemente desaparece si no impacta nada
    }
}
