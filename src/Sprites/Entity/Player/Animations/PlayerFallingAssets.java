package Sprites.Entity.Player.Animations;

import Sprites.Core.Animation;
import Sprites.Core.AssetLoader;
import Sprites.Core.Extractors.SpriteExtractors;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteSheet;

/**
 * PlayerFallingAssets — cargador del spritesheet de caída del jugador.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Cargar Player_Falling_Spritesheet.png y generar la animación de caída.
 * No contiene paths hardcodeados excepto el del recurso.
 *
 * ── ESPECIFICACIONES ──────────────────────────────────────────────────────
 * Archivo:       Player_Falling_Spritesheet.png
 * Frame:         15×24 px (asumiendo mismo tamaño que otras animaciones)
 * Animación:     Secuencia de caída (loop continuo)
 * Velocidad:     6 ticks por frame (caída normal)
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 * La animación de caída se reproduce en loop mientras el jugador está
 * cayendo (velocidad vertical positiva hacia abajo). Puede activarse tanto
 * después de un salto como al caer desde una plataforma sin haber saltado.
 */
public final class PlayerFallingAssets {

    private static final String SHEET_PATH = 
        "/Sprites/Source/Entity/Player/Player_Falling_Spritesheet.png";

    private static final int FRAME_W = 15;
    private static final int FRAME_H = 24;
    private static final int TICKS_PER_FRAME = 6;

    private PlayerFallingAssets() {}

    /**
     * Carga el spritesheet de caída y construye la animación.
     *
     * @return Animation de falling (loop continuo), o null si falla la carga
     */
    public static Animation loadFallingAnimation() {
        var extractor = SpriteExtractors.grid(FRAME_W, FRAME_H);
        SpriteSheet sheet = AssetLoader.loadSheet(SHEET_PATH, extractor);

        if (sheet == null) {
            System.err.println("[PlayerFallingAssets] No se pudo cargar: " + SHEET_PATH);
            return null;
        }

        SpriteFrame[] frames = sheet.getAllFrames();
        System.out.println("[PlayerFallingAssets] Cargados " + frames.length + " frames");

        int lastFrame = frames.length - 1;

        // La animación de caída se reproduce en loop continuo
        return Animation.builder(frames)
            .loopFrames(0, lastFrame)    // Loop de toda la secuencia
            .defaultDuration(TICKS_PER_FRAME)
            .build();
    }
}
