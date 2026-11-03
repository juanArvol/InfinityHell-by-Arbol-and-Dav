package Game.Items.Types.Bullets.BulletComport.BulletClass;

import Game.Engine.AbstractEntity;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.TrajectoryProvider;
import Game.Items.Types.Bullets.BulletID;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileData;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * BulletNormal — Proyectil Estándar
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * DESCRIPCIÓN:
 *   Proyectil básico sin efectos especiales. Trayectoria lineal, velocidad
 *   constante, destrucción al impacto. Proyectil base para armas rápidas.
 *
 * COMPORTAMIENTO DE COLISIÓN:
 *   • AbstractEntity → Daño + Destrucción
 *   • Terreno       → Destrucción
 *
 * USO TÍPICO:
 *   Pistolas, SMGs, rifles de alta cadencia
 */
public class BulletNormal extends BulletBehavior {

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 1: PROPIEDADES FÍSICAS
    // ═════════════════════════════════════════════════════════════════════
    // Define cómo se comporta el proyectil en el aire (masa, gravedad, drag)
    
    /** Masa del proyectil (afecta inercia y respuesta a fuerzas externas) */
    private static final double MASS = 1.0;
    
    /** Escala de gravedad (0.0 = sin caída, 1.0 = gravedad normal) */
    private static final double GRAVITY_SCALE = 0.0;
    
    /** Coeficiente de resistencia del aire (mayor = más frenado) */
    private static final double DRAG = 0.0001;
    
    /** Área efectiva expuesta al aire (documentado, no configurable aún) */
    private static final double EFFECTIVE_AREA = 0.3;

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 2: PROPIEDADES DE GAMEPLAY
    // ═════════════════════════════════════════════════════════════════════
    // Define valores base de daño, velocidad y lifetime
    
    /** Daño base del proyectil */
    private static final int BASE_DAMAGE = 15;
    
    /** Multiplicador de velocidad base */
    private static final double SPEED_FACTOR = 1.0;
    
    /** Tiempo de vida en frames (60 FPS) */
    private static final int LIFETIME = 10000;

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 3: BEHAVIOR IMPLEMENTATION
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public ProjectileData getDefaultData() {
        return ProjectileData.flat(BASE_DAMAGE, SPEED_FACTOR, LIFETIME);
    }

    @Override
    public BulletID getBulletID() {
        return BulletID.NORMAL_BULLET;
    }

    @Override
    public TrajectoryProvider getTrajectoryProvider() {
        return TrajectoryProvider.DEFAULT_LINEAR_GRAVITY;
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 4: LIFECYCLE HOOKS
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public void onAttached(Bullet bullet) {
        super.onAttached(bullet);
        configurePhysics(bullet);
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 5: COLLISION HANDLING
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public void onCollision(Bullet bullet, GameObjects other) {
        // Daño a entidades vivas
        if (other instanceof AbstractEntity entity) {
            entity.gotDamage((int) bullet.getBulletDamage());
        }
        
        // Destruir proyectil al impactar cualquier cosa
        /* bullet.getBulletLife().kill(); */
    }

    // ═════════════════════════════════════════════════════════════════════
    // SECCIÓN 6: CONFIGURACIÓN INTERNA
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
