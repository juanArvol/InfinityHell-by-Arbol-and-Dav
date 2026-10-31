package Sprites.Entity.Player.Animations;

import Sprites.Core.Animation;
import Sprites.Core.AssetLoader;
import Sprites.Core.Extractors.SpriteExtractors;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteSheet;

/**
 * PlayerJumpingAssets — cargador del spritesheet de salto del jugador.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Cargar Player_Jumping_Spritesheet.png y generar la animación de salto.
 * No contiene paths hardcodeados excepto el del recurso.
 *
 * ── ESPECIFICACIONES ──────────────────────────────────────────────────────
 * Archivo:       Player_Jumping_Spritesheet.png
 * Frame:         15×24 px (asumiendo mismo tamaño que otras animaciones)
 * Animación:     Secuencia de salto (play once, no loop)
 * Velocidad:     5 ticks por frame (salto rápido)
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 * La animación de salto se reproduce en loop mientras el jugador sube.
 * PlayerRenderer detecta cuando la velocidad Y cambia y transiciona a falling.
 * NOTA: Cambiado de playOnce a loop para evitar congelamiento en el último frame.
 */
public final class PlayerJumpingAssets {

    private static final String SHEET_PATH = 
        "/Sprites/Source/Entity/Player/Player_Jumping_Spritesheet.png";

    private static final int FRAME_W = 15;
    private static final int FRAME_H = 24;
    private static final int TICKS_PER_FRAME = 5;

    private PlayerJumpingAssets() {}

    /**
     * Carga el spritesheet de salto y construye la animación.
     *
     * @return Animation de jumping (loop para mantener compatibilidad), o null si falla la carga
     */
    public static Animation loadJumpingAnimation() {
        var extractor = SpriteExtractors.grid(FRAME_W, FRAME_H);
        SpriteSheet sheet = AssetLoader.loadSheet(SHEET_PATH, extractor);

        if (sheet == null) {
            System.err.println("[PlayerJumpingAssets] No se pudo cargar: " + SHEET_PATH);
            return null;
        }

        SpriteFrame[] frames = sheet.getAllFrames();
        System.out.println("[PlayerJumpingAssets] Cargados " + frames.length + " frames");

        int lastFrame = frames.length - 1;

        // La animación de salto ahora hace loop para evitar quedarse congelada
        // PlayerRenderer se encarga de cambiar a falling cuando corresponde
        return Animation.builder(frames)
            .loopFrames(0, lastFrame)    // Loop continuo en lugar de playOnce
            .defaultDuration(TICKS_PER_FRAME)
            .build();
    }
}
