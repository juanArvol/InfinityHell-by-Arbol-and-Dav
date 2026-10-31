package Sprites.Entity.Player.Animations;

import Sprites.Core.Animation;
import Sprites.Core.AssetLoader;
import Sprites.Core.Extractors.SpriteExtractors;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteSheet;

/**
 * PlayerWalkingAssets — cargador del spritesheet de caminar del jugador.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Cargar Player_Walking_Spritesheet.png y generar la animación de caminar.
 * No contiene paths hardcodeados excepto el del recurso.
 *
 * ── ESPECIFICACIONES ──────────────────────────────────────────────────────
 * Archivo:       Player_Walking_Spritesheet.png
 * Dimensiones:   225×24 px total (15 frames)
 * Frame:         15×24 px
 * Animación:     Frames 0-4 una vez (transición), luego loop desde frame 5
 * Velocidad:     6 ticks por frame (walk normal)
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 * La animación tiene dos fases:
 *   1. Intro (frames 0-4): Se reproduce una sola vez al comenzar
 *   2. Loop (frame 5 en adelante): Se repite infinitamente
 *
 * Esto permite una transición suave de idle → walk → loop continuo.
 */
public final class PlayerWalkingAssets {

    private static final String SHEET_PATH = 
        "/Sprites/Source/Entity/Player/Player_Walking_Spritesheet.png";

    private static final int FRAME_W = 15;
    private static final int FRAME_H = 24;
    private static final int TICKS_PER_FRAME = 6;

    private PlayerWalkingAssets() {}

    /**
     * Carga el spritesheet de caminar y construye la animación.
     *
     * @return Animation de walk con intro+loop, o null si falla la carga
     */
    public static Animation loadWalkAnimation() {
        var extractor = SpriteExtractors.grid(FRAME_W, FRAME_H);
        SpriteSheet sheet = AssetLoader.loadSheet(SHEET_PATH, extractor);

        if (sheet == null) {
            System.err.println("[PlayerWalkingAssets] No se pudo cargar: " + SHEET_PATH);
            return null;
        }

        SpriteFrame[] frames = sheet.getAllFrames();
        System.out.println("[PlayerWalkingAssets] Cargados " + frames.length + " frames");

        int lastFrame = frames.length - 1;

        return Animation.builder(frames)
            .playOnce(0, 4)              // Frames 0-4: transición única
            .thenLoop(5, lastFrame)      // Frame 5+: loop infinito
            .defaultDuration(TICKS_PER_FRAME)
            .build();
    }
}
