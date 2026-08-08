package Game.Items.Types.Bullets;

import Sprites.Core.AssetRegistry;
import Sprites.Entity.Bullets.BulletAssets;
import java.awt.image.BufferedImage;

/**
 * Resolución de sprites para proyectiles — único punto de conversión assetKey → BufferedImage.
 *
 * ── HRFC — Projectile Construction & Transformation Pipeline ─────────────
 *
 * Antes de esta clase, la resolución de sprites estaba hardcodeada en dos
 * lugares independientes:
 *
 *   BulletFactory.build()  → BulletAssets.balaHandle.resolveDefault().getImage()
 *   ProjectilePool.acquire()→ BulletAssets.balaHandle.resolveDefault().getImage()
 *
 * Ambos ignoraban ProjectileData.assetKey() — la feature estaba declarada
 * pero silenciosamente no implementada.
 *
 * BulletAssetResolver cierra esa deuda:
 *   - Un solo punto de resolución.
 *   - assetKey funciona realmente.
 *   - Default "bullet.bala" cuando assetKey == null.
 *   - Fallback seguro si el asset no existe (BulletAssets.balaHandle).
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   BufferedImage texture = BulletAssetResolver.resolve(blueprint.assetKey());
 *
 * ── CONVENCIÓN DE IDs ────────────────────────────────────────────────────
 *
 *   "bullet.bala"   → proyectil estándar (default)
 *   "bullet.cometa" → proyectil cometa/especial
 *   null            → mismo que "bullet.bala"
 *
 * Para añadir un nuevo sprite, registrarlo en BulletAssets.init() y luego
 * declarar su assetKey en ProjectileData.withAsset("bullet.nuevo").
 * No modificar este resolver ni BulletFactory.
 */
public final class BulletAssetResolver {

    /** Clave del asset por defecto cuando assetKey es null. */
    public static final String DEFAULT_ASSET_KEY = "bullet.bala";

    private BulletAssetResolver() {}

    /**
     * Resuelve un assetKey a BufferedImage.
     *
     * Orden de resolución:
     *   1. Si assetKey != null → buscar en AssetRegistry.
     *   2. Si no existe o assetKey == null → usar BulletAssets.balaHandle (default).
     *   3. Si balaHandle también falla → null (Bullet sin textura — invisible, para raycast).
     *
     * @param assetKey clave del asset o null para el sprite por defecto
     * @return BufferedImage del sprite, o null si no existe ningún asset
     */
    public static BufferedImage resolve(String assetKey) {
        if (assetKey != null && !assetKey.isBlank()) {
            // Intentar resolver por clave en el registro centralizado
            var handle = AssetRegistry.getInstance().get(assetKey);
            if (handle != null && handle.isValid()) {
                var frame = handle.resolveDefault();
                if (frame != null) {
                    return frame.getImage();
                }
            }
            // La clave no existe — loguear y caer al default
            System.err.println("[BulletAssetResolver] Asset no encontrado: '"
                    + assetKey + "'. Usando default.");
        }

        // Default: BulletAssets.balaHandle
        return resolveDefault();
    }

    /**
     * Resuelve el sprite por defecto (bullet.bala).
     * Usado cuando no hay assetKey declarado.
     */
    public static BufferedImage resolveDefault() {
        if (BulletAssets.balaHandle != null && BulletAssets.balaHandle.isValid()) {
            var frame = BulletAssets.balaHandle.resolveDefault();
            return frame != null ? frame.getImage() : null;
        }
        // BulletAssets.init() no fue llamado aún — proyectil sin textura
        return null;
    }
}
