package Game.Items.Types.Bullets.BulletComport.BulletClass;

import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.TrajectoryProvider;
import Game.Items.Types.Bullets.BulletID;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileData;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * EJEMPLO DE BULLET CUSTOM - FrostBullet
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Este es un ejemplo de cómo implementar un BulletBehavior custom sin modificar
 * el sistema central de Items.
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 * • Daño: 10
 * • Velocidad: Normal (1.0x)
 * • Lifetime: 60 frames (~1 segundo a 60 FPS)
 * • Piercing: 1 enemigo
 * • Efecto especial: Congela enemigos al impactar durante 2 segundos
 *
 * ── INTEGRACIÓN ───────────────────────────────────────────────────────────
 * Este comportamiento se registra en BulletType sin modificar BulletType.java:
 *
 * ```java
 * BulletType FROSTBOLT = BulletType.register(new BulletType(
 *     "frost_bolt",
 *     FrostBullet::new,
 *     ItemRarity.RARE,
 *     "Rayo Congelante",
 *     "Congela enemigos al impactar."
 * ));
 * ```
 *
 * Una vez registrado, funciona igual que los bullets builtin:
 *
 * ```java
 * BulletBehavior behavior = FROSTBOLT.create();
 * ```
 *
 * @see Game.Items.Types.Bullets.BulletComport.BulletBehavior interfaz base
 * @see Game.Mods.ExampleMod.ExampleModInitializer inicialización del mod
 */
public class FrostBullet extends BulletBehavior {

    // ══════════════════════════════════════════════════════════════════════
    // DATOS BASE
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public ProjectileData getDefaultData() {
        return new ProjectileData(
            10,   // damage - daño base moderado
            1.0,    // speedFactor - velocidad normal
            60,     // lifetime - ~1 segundo
            0  // hasGravity - sin gravedad
        );
    }

    @Override
    public BulletID getBulletID() {
        return BulletID.FROST_BULLET;
    }

    // ══════════════════════════════════════════════════════════════════════
    // EVENTOS DE IMPACTO
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void onCollision(Bullet bullet, GameObjects other) {
        // TODO: Implementar lógica de congelación
        // Ejemplo:
        // if (target instanceof Enemy) {
        //     Enemy enemy = (Enemy) target;
        //     enemy.applyStatusEffect(new FrozenEffect(2.0)); // 2 segundos
        // }
        
        // Por ahora, solo logging
        System.out.println("❄️ FrostBullet impactó: " + other.getClass().getSimpleName());
    }


    // ══════════════════════════════════════════════════════════════════════
    // TRAYECTORIA
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public TrajectoryProvider getTrajectoryProvider() {
        // Trayectoria recta estándar
        return TrajectoryProvider.DEFAULT_LINEAR_GRAVITY;
    }

    // ══════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void onUpdate(Bullet bullet) {
        // TODO: Agregar trail de partículas de hielo (opcional)
        // Ejemplo:
        // if (bullet.getAge() % 3 == 0) {
        //     spawnFrostParticle(bullet.getX(), bullet.getY());
        // }
    }

    /* @Override
    public void onSpawn(Bullet bullet) {
        // Inicialización al crear el proyectil
        System.out.println("❄️ FrostBullet spawned");
    } */

    /* @Override
    public void onDespawn(Bullet bullet) {
        // Limpieza al destruir el proyectil
    } */

    /* @Override
    public void onLifetimeExpired(Bullet bullet) {
        // Al expirar el lifetime, crear efecto visual (opcional)
    } */

    // ══════════════════════════════════════════════════════════════════════
    // NOTAS DE IMPLEMENTACIÓN
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Este es un stub de ejemplo. Una implementación completa incluiría:
     *
     * 1. Sistema de status effects (FrozenEffect)
     * 2. Partículas visuales (trail de hielo)
     * 3. Sonidos de impacto custom
     * 4. Lógica de combos con otros amuletos
     * 5. Interacción con el entorno (congelar agua, etc.)
     *
     * El punto clave es que TODO esto se puede implementar sin modificar
     * ningún archivo del sistema central de Items.
     */
}
