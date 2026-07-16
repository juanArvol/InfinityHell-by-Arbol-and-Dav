package Sprites.Player;

import Sprites.Core.Animation;
import Sprites.Core.AssetLoader;
import Sprites.Core.AssetRegistry;
import Sprites.Core.Extractors.GridExtractor;
import Sprites.Core.Extractors.SpriteExtractors;
import Sprites.Core.SpriteDefinition;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteHandle;
import Sprites.Core.SpriteSheet;

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
public final class PlayerAssets {

    private static final String SHEET_PATH =
        "/Sprites/Source/player/Player Spritesheet.png";

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

    private PlayerAssets() {}

    /**
     * Carga el SpriteSheet, define las animaciones y registra el handle.
     * Llamar una sola vez durante la inicialización del juego (Assets.init()).
     */
    public static void init() {
        // ── Carga ──────────────────────────────────────────────────────────
        var extractor = SpriteExtractors.grid(FRAME_W, FRAME_H);
        SpriteSheet sheet = AssetLoader.loadSheet(SHEET_PATH, extractor);

        if (sheet == null) {
            System.err.println("[PlayerAssets] No se pudo cargar: " + SHEET_PATH
                + ". Usando handle vacío.");
            handle = SpriteHandle.EMPTY;
            return;
        }

        // ── Exportación de diagnóstico (solo cuando EXPORT_DEBUG_FRAMES = true) ──
        // Activa esto para comparar cada PNG exportado con el sheet original.
        // Si un PNG ya contiene parte del frame vecino → FRAME_W está mal.
        // Si los PNG son perfectos → el problema está en una etapa posterior.
        if (EXPORT_DEBUG_FRAMES) {
            GridExtractor ge = (GridExtractor) extractor;
            GridExtractor.exportFrames(AssetLoader.loadRaw(SHEET_PATH), ge, EXPORT_DIR);
        }

        // ── Banco completo de frames (todos los extraídos del sheet) ──────
        SpriteFrame[] bank = sheet.getAllFrames();

        // ── Frame 0: idle ─────────────────────────────────────────────────
        SpriteFrame idleFrame = sheet.getFrame(0);

        // ── Animación idle — frame único estático ─────────────────────────
        Animation idle = Animation.still(idleFrame);

        // ── Animación walk_right — frames 0..8 via Builder ────────────────
        // El Builder trabaja con índices del banco; producirá los frames
        // 0,1,2,3,4,5,6,7,8 en orden de reproducción.
        Animation walkRight = Animation.builder(bank)
            .frame(0, WALK_FRAME_COUNT - 1)   // frames 0..8 inclusive
            .defaultDuration(TICKS_PER_FRAME)
            .loop()
            .build();

        // ── Definición ────────────────────────────────────────────────────
        SpriteDefinition def = new SpriteDefinition(idleFrame)
            .addAnimation("idle",       idle)
            .addAnimation("walk_right", walkRight);
        // "walk_left" no existe — PlayerRenderer aplica flipH=true en el renderer.

        // ── Registrar ─────────────────────────────────────────────────────
        handle = new SpriteHandle(def, "player");
        AssetRegistry.getInstance().register("player", handle);
    }
}
