package Sprites.Entity.Player.Animations;

import Sprites.Core.Animation;
import Sprites.Core.AssetLoader;
import Sprites.Core.Extractors.SpriteExtractors;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteSheet;
import Sprites.Entity.Player.PlayerAssets;

/**
 * PlayerFallingAssets — cargador del spritesheet de caída del jugador.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Cargar Player_Falling_Spritesheet.png y generar la animación de caída.
 * No contiene paths hardcodeados excepto el del recurso.
 *
 * ── DIMENSIONES ───────────────────────────────────────────────────────────
 * Usa las dimensiones globales de PlayerAssets.FRAME_WIDTH y FRAME_HEIGHT.
 * Si este spritesheet específico tuviera dimensiones diferentes, se pueden
 * sobreescribir aquí definiendo constantes locales FRAME_W y FRAME_H.
 *
 * ── ESPECIFICACIONES ──────────────────────────────────────────────────────
 * Archivo:       Player_Falling_Spritesheet.png
 * Frame:         15×24 px (heredado de PlayerAssets)
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

    // ── Dimensiones heredadas de PlayerAssets ─────────────────────────────
    // Si este spritesheet específico tuviera otro tamaño, descomenta y ajusta:
    // private static final int FRAME_W = 32;
    // private static final int FRAME_H = 48;
    
    private static final int TICKS_PER_FRAME = 6;

    private PlayerFallingAssets() {}

    /**
     * Carga el spritesheet de caída y construye la animación.
     *
     * @return Animation de falling (loop continuo), o null si falla la carga
     */
    public static Animation loadFallingAnimation() {
        var extractor = SpriteExtractors.grid(
            PlayerAssets.FRAME_WIDTH, 
            PlayerAssets.FRAME_HEIGHT
        );
        SpriteSheet sheet = AssetLoader.loadSheet(SHEET_PATH, extractor);

        if (sheet == null) {
            System.err.println("[PlayerFallingAssets] No se pudo cargar: " + SHEET_PATH);
            return null;
        }

        SpriteFrame[] frames = sheet.getAllFrames();
        System.out.println("[PlayerFallingAssets] Cargados " + frames.length + " frames de " 
            + PlayerAssets.FRAME_WIDTH + "×" + PlayerAssets.FRAME_HEIGHT + " px");

        int lastFrame = frames.length - 1;

        // La animación de caída se reproduce en loop continuo
        return Animation.builder(frames)
            .loopFrames(0, lastFrame)    // Loop de toda la secuencia
            .defaultDuration(TICKS_PER_FRAME)
            .build();
    }

    /**
     * Carga la animación de transición de caída a caminar.
     * 
     * Esta animación se usa cuando el jugador está cayendo Y hay input horizontal.
     * Combina los primeros frames de falling con los frames de loop de walking
     * para crear una transición visual suave al aterrizar con movimiento.
     * 
     * ESTRUCTURA:
     *   1. Reproduce los primeros frames de falling (0-3) una vez
     *   2. Transiciona a los frames de loop de walking (5-14)
     * 
     * @return Animation de falling→walking, o null si falla la carga
     */
    public static Animation loadFallingToWalkAnimation() {
        // Cargar falling spritesheet
        var extractorFall = SpriteExtractors.grid(
            PlayerAssets.FRAME_WIDTH, 
            PlayerAssets.FRAME_HEIGHT
        );
        SpriteSheet fallSheet = AssetLoader.loadSheet(SHEET_PATH, extractorFall);

        if (fallSheet == null) {
            System.err.println("[PlayerFallingAssets] No se pudo cargar falling sheet para transición");
            return null;
        }

        // Cargar walking spritesheet para los frames de transición
        String walkPath = "/Sprites/Source/Entity/Player/Player_Walking_Spritesheet.png";
        var extractorWalk = SpriteExtractors.grid(
            PlayerAssets.FRAME_WIDTH, 
            PlayerAssets.FRAME_HEIGHT
        );
        SpriteSheet walkSheet = AssetLoader.loadSheet(walkPath, extractorWalk);

        if (walkSheet == null) {
            System.err.println("[PlayerFallingAssets] No se pudo cargar walking sheet para transición");
            return null;
        }

        SpriteFrame[] fallFrames = fallSheet.getAllFrames();
        SpriteFrame[] walkFrames = walkSheet.getAllFrames();

        // Determinar cuántos frames usar de falling (mínimo 4 o todos si hay menos)
        int fallFrameCount = Math.min(4, fallFrames.length);

        // Crear array combinado: frames de falling + frames de walking loop (5-14)
        int walkLoopStart = 5;
        int walkLoopEnd = Math.min(14, walkFrames.length - 1);
        int walkLoopCount = walkLoopEnd - walkLoopStart + 1;
        
        SpriteFrame[] combined = new SpriteFrame[fallFrameCount + walkLoopCount];
        
        // Copiar frames de falling
        System.arraycopy(fallFrames, 0, combined, 0, fallFrameCount);
        
        // Copiar frames de walking loop
        System.arraycopy(walkFrames, walkLoopStart, combined, fallFrameCount, walkLoopCount);

        // Construir animación: playOnce los frames de falling, luego loop los de walking
        return Animation.builder(combined)
            .playOnce(0, fallFrameCount - 1)           // Falling frames una vez
            .thenLoop(fallFrameCount, combined.length - 1)  // Walking frames en loop
            .defaultDuration(TICKS_PER_FRAME)
            .build();
    }
}
