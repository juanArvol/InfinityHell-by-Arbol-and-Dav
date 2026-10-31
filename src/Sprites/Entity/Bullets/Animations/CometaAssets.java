package Sprites.Entity.Bullets.Animations;

import Sprites.Core.Animation;
import Sprites.Core.AssetLoader;
import Sprites.Core.Extractors.SpriteExtractors;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteSheet;

/**
 * CometaAssets — cargador del spritesheet de la bala cometa.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Cargar cometa_spritesheet.png y generar la animación animada.
 * No contiene paths hardcodeados excepto el del recurso.
 *
 * ── ESPECIFICACIONES ──────────────────────────────────────────────────────
 * Archivo:       cometa_spritesheet.png
 * Frame:         12×8 px
 * Animación:     Loop completo de todos los frames
 * Rotación:      Automática según velocidad (configurada en BulletRenderer)
 *
 * ── ORIENTACIÓN DINÁMICA ──────────────────────────────────────────────────
 * El sprite se rota automáticamente hacia la dirección de movimiento cuando
 * autoRotateToVelocity está activado en el SpriteRenderer del Bullet.
 */
public final class CometaAssets {

    private static final String SHEET_PATH = 
        "/Sprites/Source/Entity/Bullets/cometa_spritesheet.png";

    private static final int FRAME_W = 12;
    private static final int FRAME_H = 8;
    private static final int TICKS_PER_FRAME = 4; // Animación rápida

    private CometaAssets() {}

    /**
     * Carga el spritesheet de cometa y construye la animación.
     *
     * @return Animation de cometa, o null si falla la carga
     */
    public static Animation loadCometaAnimation() {
        var extractor = SpriteExtractors.grid(FRAME_W, FRAME_H);
        SpriteSheet sheet = AssetLoader.loadSheet(SHEET_PATH, extractor);

        if (sheet == null) {
            System.err.println("[CometaAssets] No se pudo cargar: " + SHEET_PATH);
            return null;
        }

        SpriteFrame[] frames = sheet.getAllFrames();
        System.out.println("[CometaAssets] Cargados " + frames.length + " frames");

        return Animation.builder(frames)
            .loopFrames(0, frames.length - 1)
            .defaultDuration(TICKS_PER_FRAME)
            .build();
    }

    /**
     * @return Frame por defecto (primer frame de la animación)
     */
    public static SpriteFrame loadDefaultFrame() {
        var extractor = SpriteExtractors.grid(FRAME_W, FRAME_H);
        SpriteSheet sheet = AssetLoader.loadSheet(SHEET_PATH, extractor);
        
        if (sheet == null) return null;
        
        SpriteFrame[] frames = sheet.getAllFrames();
        return frames.length > 0 ? frames[0] : null;
    }
}
