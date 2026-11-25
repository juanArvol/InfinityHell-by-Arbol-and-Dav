package Sprites.Entity.Player.Animations;

import Sprites.Core.Animation;
import Sprites.Core.AssetLoader;
import Sprites.Core.Extractors.SpriteExtractors;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteSheet;
import Sprites.Entity.Player.PlayerAssets;

/**
 * PlayerWalkingAssets — cargador del spritesheet de caminar del jugador.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Cargar Player_Walking_Spritesheet.png y generar la animación de caminar.
 * No contiene paths hardcodeados excepto el del recurso.
 *
 * ── DIMENSIONES ───────────────────────────────────────────────────────────
 * Usa las dimensiones globales de PlayerAssets.FRAME_WIDTH y FRAME_HEIGHT.
 * Si este spritesheet específico tuviera dimensiones diferentes, se pueden
 * sobreescribir aquí definiendo constantes locales FRAME_W y FRAME_H.
 *
 * ── ESPECIFICACIONES ──────────────────────────────────────────────────────
 * Archivo:       Player_Walking_Spritesheet.png
 * Dimensiones:   225×24 px total (15 frames)
 * Frame:         15×24 px (heredado de PlayerAssets)
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

    // ── Dimensiones heredadas de PlayerAssets ─────────────────────────────
    // Si este spritesheet específico tuviera otro tamaño, descomenta y ajusta:
    // private static final int FRAME_W = 32;
    // private static final int FRAME_H = 48;
    
    private static final int TICKS_PER_FRAME = 1;

    private PlayerWalkingAssets() {}

    /**
     * Carga el spritesheet de caminar y construye la animación.
     *
     * @return Animation de walk con intro+loop, o null si falla la carga
     */
    public static Animation loadWalkAnimation() {
        var extractor = SpriteExtractors.grid(
            PlayerAssets.FRAME_WIDTH, 
            PlayerAssets.FRAME_HEIGHT
        );
        
        SpriteSheet sheet = AssetLoader.loadSheet(SHEET_PATH, extractor);

        if (sheet == null) {
            System.err.println("[PlayerWalkingAssets] No se pudo cargar: " + SHEET_PATH);
            return null;
        }

        SpriteFrame[] frames = sheet.getAllFrames();
        
        // ── VALIDACIÓN DE EXTRACCIÓN ──────────────────────────────────────
        if (frames == null || frames.length == 0) {
            System.err.println("[PlayerWalkingAssets] ERROR: No se extrajeron frames del spritesheet");
            System.err.println("  Path: " + SHEET_PATH);
            System.err.println("  Dimensiones configuradas: " + PlayerAssets.FRAME_WIDTH + "×" + PlayerAssets.FRAME_HEIGHT);
            System.err.println("  SOLUCION: Verifica que PlayerAssets.FRAME_WIDTH y FRAME_HEIGHT");
            System.err.println("            coincidan EXACTAMENTE con el tamaño de cada sprite");
            System.err.println("            en el archivo PNG.");
            return null;
        }

        System.out.println("[PlayerWalkingAssets] Cargados " + frames.length + " frames de " 
            + PlayerAssets.FRAME_WIDTH + "×" + PlayerAssets.FRAME_HEIGHT + " px");

        // Calcular índices de forma dinámica para adaptarse a cualquier cantidad de frames
        int lastFrame = frames.length - 1;
        int introEnd = Math.min(4, lastFrame); // Intro hasta frame 4 o hasta el último disponible
        int loopStart = Math.min(5, lastFrame); // Loop desde frame 5 o el último

        // Si hay menos de 6 frames, loop completo
        if (frames.length < 6) {
            return Animation.builder(frames)
                .loopFrames(0, lastFrame)
                .defaultDuration(TICKS_PER_FRAME)
                .build();
        }

        // Configuración normal: intro + loop
        return Animation.builder(frames)
            .playOnce(0, introEnd)           // Frames 0-4: transición única
            .thenLoop(loopStart, lastFrame)  // Frame 5+: loop infinito
            .defaultDuration(TICKS_PER_FRAME)
            .build();
    }
}
