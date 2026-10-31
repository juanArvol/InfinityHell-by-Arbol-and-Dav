package Sprites.Entity.Bullets;

import Sprites.Core.Animation;
import Sprites.Core.AssetRegistry;
import Sprites.Core.SpriteDefinition;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteHandle;
import Sprites.Entity.Bullets.Animations.CometaAssets;
import Sprites.Entity.Bullets.SingleSprites.BalaAssets;

/**
 * BulletAssets — orquestador de recursos visuales de proyectiles.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Este archivo SOLO carga recursos desde las subcarpetas especializadas:
 *
 *   • SingleSprites/ → Sprites estáticos (bala.png)
 *   • Animations/    → Spritesheets animados (cometa_spritesheet.png)
 *
 * NO contiene paths hardcodeados. Los loaders específicos (BalaAssets,
 * CometaAssets) encapsulan la carga de cada asset.
 *
 * ── CAMBIO ARQUITECTÓNICO ────────────────────────────────────────────────
 * ANTES:  BulletAssets cargaba directamente con paths hardcodeados.
 *
 * AHORA:  BulletAssets delega la carga a loaders especializados:
 *           - BalaAssets.loadBalaFrame()
 *           - CometaAssets.loadCometaAnimation()
 *
 * ── ORIENTACIÓN DINÁMICA ──────────────────────────────────────────────────
 * Las balas soportan rotación automática según su trayectoria.
 * Cuando se activa autoRotateToVelocity en el SpriteRenderer, el sprite
 * se orienta automáticamente hacia la dirección de movimiento.
 *
 * ── INTEGRACIÓN CON BULLET FLYWEIGHT ──────────────────────────────────────
 * Los SpriteHandles registrados aquí son consumidos por BulletFlyweightCache
 * al construir flyweights. El cache busca el handle desde AssetRegistry usando
 * la clave del asset (e.g., "bullet.bala", "bullet.cometa").
 *
 * Flujo completo:
 *   1. BulletAssets.init() → carga sprites y registra en AssetRegistry
 *   2. BulletFactory crea Bullet con ProjectileBlueprint
 *   3. BulletFlyweightCache.get(blueprint) → busca/crea flyweight
 *   4. Flyweight obtiene SpriteHandle desde AssetRegistry
 *   5. Bullet constructor usa flyweight.spriteHandle() para SpriteRendererComponent
 *   6. BulletBatchRenderer usa handle.resolveDefault() sin crear frames temporales
 *
 * ── CLAVES ───────────────────────────────────────────────────────────────
 *   "bullet.bala"   → proyectil estándar (sprite estático)
 *   "bullet.cometa" → proyectil cometa (spritesheet animado)
 */
public final class BulletAssets {

    /** Handle de bala estándar. */
    public static SpriteHandle balaHandle;

    /** Handle de bala cometa. */
    public static SpriteHandle cometaHandle;

    private BulletAssets() {}

    public static void init() {

        // ── Bala estándar (sprite estático) ───────────────────────────────
        SpriteFrame balaFrame = BalaAssets.loadBalaFrame();
        
        if (balaFrame != null) {
            SpriteDefinition balaDef = new SpriteDefinition(balaFrame);
            balaHandle = new SpriteHandle(balaDef, "bullet.bala");
            AssetRegistry.getInstance().register("bullet.bala", balaHandle);
        } else {
            System.err.println("[BulletAssets] Error cargando bala estándar");
            balaHandle = SpriteHandle.EMPTY;
        }

        // ── Bala cometa (spritesheet animado) ─────────────────────────────
        Animation cometaAnim = CometaAssets.loadCometaAnimation();
        SpriteFrame cometaDefault = CometaAssets.loadDefaultFrame();
        
        if (cometaAnim != null && cometaDefault != null) {
            SpriteDefinition cometaDef = new SpriteDefinition(cometaDefault)
                .addAnimation("default", cometaAnim);
            
            cometaHandle = new SpriteHandle(cometaDef, "bullet.cometa");
            AssetRegistry.getInstance().register("bullet.cometa", cometaHandle);
        } else {
            System.err.println("[BulletAssets] Error cargando cometa");
            cometaHandle = SpriteHandle.EMPTY;
        }
        
        System.out.println("[BulletAssets] Carga completa desde subcarpetas especializadas");
    }
}
