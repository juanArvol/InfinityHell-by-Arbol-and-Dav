package Sprites.Entity.Bullets.SingleSprites;

import Sprites.Core.AssetLoader;
import Sprites.Core.SpriteFrame;

/**
 * BalaAssets — cargador del sprite estático de bala estándar.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Cargar bala.png como sprite único (sin animación).
 * No contiene paths hardcodeados excepto el del recurso.
 *
 * ── ESPECIFICACIONES ──────────────────────────────────────────────────────
 * Archivo:       bala.png
 * Tipo:          Sprite estático (single frame)
 * Rotación:      Automática según velocidad (configurada en BulletRenderer)
 */
public final class BalaAssets {

    private static final String SPRITE_PATH = 
        "/Sprites/Source/Entity/Bullets/bala.png";

    private BalaAssets() {}

    /**
     * Carga el sprite estático de bala.
     *
     * @return SpriteFrame de bala, o null si falla la carga
     */
    public static SpriteFrame loadBalaFrame() {
        SpriteFrame frame = AssetLoader.loadFrame(SPRITE_PATH);

        if (frame == null) {
            System.err.println("[BalaAssets] No se pudo cargar: " + SPRITE_PATH);
            return null;
        }

        System.out.println("[BalaAssets] Sprite cargado exitosamente");
        return frame;
    }
}
