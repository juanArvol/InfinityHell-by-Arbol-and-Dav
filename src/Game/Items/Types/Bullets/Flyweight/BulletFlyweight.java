package Game.Items.Types.Bullets.Flyweight;

import Game.Engine.Colisions.Filter.CollisionProfile;
import java.awt.image.BufferedImage;

/**
 * Recurso compartido e inmutable de un tipo de proyectil.
 *
 * ── PATRÓN FLYWEIGHT ──────────────────────────────────────────────────────
 *
 * Un BulletFlyweight agrupa los datos de construcción que son idénticos para
 * TODOS los proyectiles del mismo tipo:
 *
 *   - texture          → la misma BufferedImage para todas las instancias
 *   - collisionProfile → el mismo perfil de colisión para todas las instancias
 *   - width / height   → las mismas dimensiones de collider para todas las instancias
 *
 * En lugar de que 200 Bullets activas carguen 200 referencias independientes
 * a la misma BufferedImage (y recreen ColliderComponent con los mismos parámetros
 * en cada construcción), esos recursos se resuelven UNA SOLA VEZ y se comparten.
 *
 * ── QUÉ NO ESTÁ AQUÍ ──────────────────────────────────────────────────────
 *
 * BulletFlyweight NO contiene:
 *
 *   - BulletBehavior  → puede tener estado (wrappers de amuleto), tiene lifecycle
 *   - ProjectileMovement → HomingMovement tiene target individual; otros tienen
 *                         estado frame a frame (angle, frameCount)
 *   - damage          → calculado individualmente por ModifiedWeapon + amuletos
 *   - speed           → calculado individualmente
 *   - lifeTime        → calculado individualmente
 *   - target          → estado individual de cada proyectil
 *
 * Estos campos tienen ownership individual y pertenecen a Bullet, no aquí.
 *
 * ── GARANTÍAS DE INMUTABILIDAD ────────────────────────────────────────────
 *
 * BulletFlyweight es un record — todos sus campos son final.
 * La texture no se clona: es un recurso de asset gestionado por AssetRegistry
 * y es tratado como read-only por el sistema de render.
 *
 * ── ACCESO ────────────────────────────────────────────────────────────────
 *
 * Los Flyweights se obtienen exclusivamente a través de BulletFlyweightCache:
 *
 *   BulletFlyweight fw = BulletFlyweightCache.INSTANCE.get(blueprint);
 *
 * No instanciar directamente fuera del paquete Flyweight.
 */
public record BulletFlyweight(
        BufferedImage    texture,
        CollisionProfile collisionProfile,
        int              width,
        int              height
) {

    /**
     * Perfil de colisión efectivo para construir el ColliderComponent.
     *
     * Si collisionProfile es null (el Blueprint usó el default PLAYER_BULLET),
     * retorna CollisionProfile.PLAYER_BULLET para que Bullet no tenga que
     * hacer ese check en su constructor.
     */
    public CollisionProfile effectiveProfile() {
        return (collisionProfile != null)
                ? collisionProfile
                : CollisionProfile.PLAYER_BULLET;
    }

    /**
     * true si este Flyweight tiene un sprite válido para renderizar.
     * false = proyectil invisible (útil para raycast, proyectiles de área).
     */
    public boolean hasTexture() {
        return texture != null;
    }
}
