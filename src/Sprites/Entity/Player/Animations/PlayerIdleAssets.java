package Sprites.Entity.Player.Animations;

import Sprites.Core.Animation;
import Sprites.Core.AssetLoader;
import Sprites.Core.Extractors.SpriteExtractors;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteSheet;

/**
 * PlayerIdleAssets — cargador del spritesheet de idle del jugador.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Cargar Player_Idle_Spritesheet.png y generar la animación idle.
 * No contiene paths hardcodeados excepto el del recurso.
 *
 * ── ESPECIFICACIONES ──────────────────────────────────────────────────────
 * Archivo:       Player_Idle_Spritesheet.png
 * Dimensiones:   60×24 px total (4 frames)
 * Frame:         15×24 px
 * Animación:     Loop completo de todos los frames
 * Velocidad:     8 ticks por frame (idle lento)
 */
public final class PlayerIdleAssets {

    private static final String SHEET_PATH = 
        "/Sprites/Source/Entity/Player/Player_Idle_Spritesheet.png";

    private static final int FRAME_W = 15;
    private static final int FRAME_H = 24;
    private static final int TICKS_PER_FRAME = 8;

    private PlayerIdleAssets() {}

    /**
     * Carga el spritesheet de idle y construye la animación.
     *
     * @return Animation de idle, o null si falla la carga
     */
    public static Animation loadIdleAnimation() {
        var extractor = SpriteExtractors.grid(FRAME_W, FRAME_H);
        SpriteSheet sheet = AssetLoader.loadSheet(SHEET_PATH, extractor);

        if (sheet == null) {
            System.err.println("[PlayerIdleAssets] No se pudo cargar: " + SHEET_PATH);
            return null;
        }

        SpriteFrame[] frames = sheet.getAllFrames();
        System.out.println("[PlayerIdleAssets] Cargados " + frames.length + " frames");

        return Animation.builder(frames)
            .loopFrames(0, frames.length - 1)
            .defaultDuration(TICKS_PER_FRAME)
            .build();
    }

    /**
     * @return Frame por defecto (primer frame del idle)
     */
    public static SpriteFrame loadDefaultFrame() {
        var extractor = SpriteExtractors.grid(FRAME_W, FRAME_H);
        SpriteSheet sheet = AssetLoader.loadSheet(SHEET_PATH, extractor);
        
        if (sheet == null) return null;
        
        SpriteFrame[] frames = sheet.getAllFrames();
        return frames.length > 0 ? frames[0] : null;
    }
}
