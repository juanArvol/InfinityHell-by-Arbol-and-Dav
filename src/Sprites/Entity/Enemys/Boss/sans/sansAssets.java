package Sprites.Entity.Enemys.Boss.sans;

import Sprites.Core.SpriteHandle;

/**
 * PlayerAssets — recursos visuales del jugador basados en SpriteSheet.
 *
 * ── HRFC-004: MIGRACIÓN A SpriteExtractor ────────────────────────────────
 * AssetLoader.loadSheet() ahora usa SpriteExtractors.grid(24, 24).
 * Cada frame extraído es una copia aislada del raster (drawImage interno
 * de GridExtractor) — elimina el bug de bleeding de píxeles del frame
 * vecino que se observaba en el render del Player.
 *
 * ── HRFC-004: MIGRACIÓN A Animation.Builder ──────────────────────────────
 * Las animaciones se construyen con el nuevo Builder DSL. El banco de
 * frames del sheet actúa como fuente de índices. Esto permite agregar
 * duración por frame, ranges parciales o variantes en el futuro sin
 * reescribir PlayerAssets.
 *
 * ── ESTRUCTURA DEL SPRITESHEET ───────────────────────────────────────────
 *   Player Spritesheet.png  (135×24 px — 9 frames de 15×24 px)
 *   Fila 0, 9 columnas, 15×24 px por frame:
 *     Frame 0      → idle (primer frame, mostrado estático)
 *     Frames 0..8  → walk (animación completa de caminar)
 *
 * ── ANIMACIONES ──────────────────────────────────────────────────────────
 *   "idle"       → frame 0 estático (Animation.still)
 *   "walk_right" → frames 0..8, loop, 6 ticks/frame
 *   "walk_left"  → NO existe. PlayerRenderer aplica flipH=true via SpriteRenderer.
 */
public final class sansAssets {

    private static final String SHEET_PATH =
        "/Sprites/Source/Entity/Player/Player Spritesheet.png";

    /**
     * Ancho de cada celda del spritesheet.
     *
     * Carlitos mide 15 px de ancho × 24 px de alto.
     * El sheet tiene 15 frames de 15×24 px cada uno → ancho total = 135 px.
     *
     * IMPORTANTE: este valor debe coincidir exactamente con el ancho de celda
     * del archivo PNG. Si el artefacto reaparece, usar GridExtractor.exportFrames()
     * para verificar que cada PNG exportado contenga únicamente su propio frame.
     */
    private static final int FRAME_W          = 15;
    private static final int FRAME_H          = 24;
    private static final int WALK_FRAME_COUNT = 15;
    private static final int TICKS_PER_FRAME  = 6;

    /**
     * Handle principal del jugador.
     * Contiene las animaciones "idle" y "walk_right".
     */
    public static SpriteHandle handle;

    /**
     * Cuando es true, exporta los frames extraídos como PNG a la carpeta
     * indicada en EXPORT_DIR. Activar solo para diagnóstico visual.
     * Cambiar a false antes de compilar para producción.
     */
    private static final boolean EXPORT_DEBUG_FRAMES = false;
    private static final String  EXPORT_DIR          = "C:/temp/player_frames";

    private sansAssets() {}

    /**
     * Carga el SpriteSheet, define las animaciones y registra el handle.
     * Llamar una sola vez durante la inicialización del juego (Assets.init()).
     */
    public static void init() {
        // ── Sans Assets desactivado temporalmente ─────────────────────────
        // TODO: Implementar sprites de Sans cuando estén disponibles
        System.out.println("[sansAssets] Sans sprites no implementados aún - usando handle vacío");
        handle = SpriteHandle.EMPTY;
    }
}

