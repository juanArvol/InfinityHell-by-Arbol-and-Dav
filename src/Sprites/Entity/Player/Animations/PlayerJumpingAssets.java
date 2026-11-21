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

    /**
     * Carga la animación de transición de salto a caminar.
     * 
     * Esta animación se usa cuando el jugador está saltando Y hay input horizontal.
     * Combina los primeros frames de jumping con los frames de loop de walking
     * para crear una transición visual suave.
     * 
     * ESTRUCTURA:
     *   1. Reproduce los primeros frames de jumping (0-3) una vez
     *   2. Transiciona a los frames de loop de walking (5-14)
     * 
     * @return Animation de jumping→walking, o null si falla la carga
     */
    public static Animation loadJumpingToWalkAnimation() {
        // Cargar jumping spritesheet
        var extractorJump = SpriteExtractors.grid(FRAME_W, FRAME_H);
        SpriteSheet jumpSheet = AssetLoader.loadSheet(SHEET_PATH, extractorJump);

        if (jumpSheet == null) {
            System.err.println("[PlayerJumpingAssets] No se pudo cargar jumping sheet para transición");
            return null;
        }

        // Cargar walking spritesheet para los frames de transición
        String walkPath = "/Sprites/Source/Entity/Player/Player_Walking_Spritesheet.png";
        var extractorWalk = SpriteExtractors.grid(FRAME_W, FRAME_H);
        SpriteSheet walkSheet = AssetLoader.loadSheet(walkPath, extractorWalk);

        if (walkSheet == null) {
            System.err.println("[PlayerJumpingAssets] No se pudo cargar walking sheet para transición");
            return null;
        }

        SpriteFrame[] jumpFrames = jumpSheet.getAllFrames();
        SpriteFrame[] walkFrames = walkSheet.getAllFrames();

        // Determinar cuántos frames usar de jumping (mínimo 4 o todos si hay menos)
        int jumpFrameCount = Math.min(4, jumpFrames.length);

        // Crear array combinado: frames de jumping + frames de walking loop (5-14)
        int walkLoopStart = 5;
        int walkLoopEnd = Math.min(14, walkFrames.length - 1);
        int walkLoopCount = walkLoopEnd - walkLoopStart + 1;
        
        SpriteFrame[] combined = new SpriteFrame[jumpFrameCount + walkLoopCount];
        
        // Copiar frames de jumping
        System.arraycopy(jumpFrames, 0, combined, 0, jumpFrameCount);
        
        // Copiar frames de walking loop
        System.arraycopy(walkFrames, walkLoopStart, combined, jumpFrameCount, walkLoopCount);

        // Construir animación: playOnce los frames de jumping, luego loop los de walking
        return Animation.builder(combined)
            .playOnce(0, jumpFrameCount - 1)           // Jumping frames una vez
            .thenLoop(jumpFrameCount, combined.length - 1)  // Walking frames en loop
            .defaultDuration(TICKS_PER_FRAME)
            .build();
    }
}
