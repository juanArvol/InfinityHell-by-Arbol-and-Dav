package Sprites.Core;

import Sprites.Core.Extractors.SpriteExtractors;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 * AssetLoader — carga de imágenes con caché interna.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Única clase del proyecto autorizada a tocar ImageIO y rutas de archivos.
 * Reemplaza completamente a Sprites.Loader.
 *
 * ── CACHÉ ─────────────────────────────────────────────────────────────────
 * Cada path se carga UNA sola vez. Las llamadas posteriores al mismo path
 * devuelven la misma BufferedImage sin releer el disco. Esto elimina las
 * cargas repetidas que existían antes (cada clase Animation/SingleSprite
 * llamaba Loader.imageLoader() de forma independiente).
 *
 * La caché es ConcurrentHashMap: thread-safe para init() paralelo futuro.
 *
 * ── PATHS ─────────────────────────────────────────────────────────────────
 * Los paths son relativos al classpath root (src/), con "/" como prefijo.
 * Los recursos gráficos viven en src/Sprites/Source/, por lo que la ruta
 * correcta sigue el patrón /Sprites/Source/<categoria>/<archivo>:
 *
 *   Ejemplo: "/Sprites/Source/player/eee.png"
 *
 * Estructura en disco:
 *   src/Sprites/Source/player/    ← sprites del jugador
 *   src/Sprites/Source/enemies/   ← sprites de enemigos
 *   src/Sprites/Source/bullets/   ← sprites de proyectiles
 *   src/Sprites/Source/ambiente/  ← tiles del mundo
 *   src/Sprites/Source/effects/   ← sonidos (.wav)
 *
 * ── IMÁGENES FALTANTES ────────────────────────────────────────────────────
 * Si una imagen no existe o no se puede cargar, se devuelve un SpriteFrame
 * vacío (null-safe) en lugar de null. Los logs de error van a stderr.
 * El juego sigue funcionando con sprites transparentes en lugar de crashear.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   // Carga simple con caché:
 *   SpriteFrame frame = AssetLoader.loadFrame("/Sprites/Source/player/eee.png");
 *
 *   // Carga de hoja de sprites:
 *   SpriteSheet sheet = AssetLoader.loadSheet("/Sprites/Source/player/walk.png", 32, 48);
 *   SpriteFrame[] frames = sheet.getRow(0, 4);
 *
 *   // Pre-carga explícita (opcional, para warm-up al inicio):
 *   AssetLoader.preload("/Sprites/Source/enemies/zotopia1.png");
 *
 *   // Limpiar caché (tests / recargas hot):
 *   AssetLoader.clearCache();
 */
public final class AssetLoader {

    /** Caché path → BufferedImage. Thread-safe. */
    private static final ConcurrentHashMap<String, BufferedImage> cache =
        new ConcurrentHashMap<>();

    // Clase utilitaria — no instanciable.
    private AssetLoader() {}

    // ── API principal ─────────────────────────────────────────────────────

    /**
     * Carga una imagen individual como SpriteFrame.
     * Cachea el resultado internamente.
     *
     * @param path path relativo al classpath (ej: "/Sprites/Source/player/eee.png")
     * @return SpriteFrame válido, o SpriteFrame.empty() si falló la carga
     */
    public static SpriteFrame loadFrame(String path) {
        BufferedImage img = loadRaw(path);
        return img != null ? new SpriteFrame(img) : SpriteFrame.empty();
    }

    /**
     * Carga una imagen individual como SpriteFrame con pivot configurable.
     *
     * @param path   path relativo al classpath
     * @param pivotX pivot X normalizado [0..1]
     * @param pivotY pivot Y normalizado [0..1]
     * @return SpriteFrame con pivot, o SpriteFrame.empty() si falló la carga
     */
    public static SpriteFrame loadFrame(String path, float pivotX, float pivotY) {
        BufferedImage img = loadRaw(path);
        return img != null ? new SpriteFrame(img, pivotX, pivotY) : SpriteFrame.empty();
    }

    /**
     * Carga un array de imágenes individuales como array de SpriteFrame.
     * Útil para animaciones definidas como archivos separados.
     *
     * @param paths array de paths relativos al classpath
     * @return array de SpriteFrame (los frames fallidos son SpriteFrame.empty())
     */
    public static SpriteFrame[] loadFrames(String... paths) {
        SpriteFrame[] frames = new SpriteFrame[paths.length];
        for (int i = 0; i < paths.length; i++) {
            frames[i] = loadFrame(paths[i]);
        }
        return frames;
    }

    /**
     * Carga una SpriteSheet usando el extractor indicado.
     * Cachea la imagen fuente internamente.
     *
     * @param path      path relativo al classpath
     * @param extractor estrategia de extracción (ej: SpriteExtractors.grid(24, 24))
     * @return SpriteSheet lista para acceder a frames, o null si falló la carga
     */
    public static SpriteSheet loadSheet(String path, SpriteExtractor extractor) {
        BufferedImage img = loadRaw(path);
        if (img == null) return null;
        return SpriteSheet.load(img, extractor);
    }

    /**
     * Carga una SpriteSheet en cuadrícula uniforme.
     * Cachea la imagen fuente internamente.
     *
     * Equivale a {@code loadSheet(path, SpriteExtractors.grid(frameWidth, frameHeight))}.
     *
     * @param path        path relativo al classpath
     * @param frameWidth  ancho de cada frame en píxeles
     * @param frameHeight alto de cada frame en píxeles
     * @return SpriteSheet lista para extraer frames, o null si falló la carga
     */
    public static SpriteSheet loadSheet(String path, int frameWidth, int frameHeight) {
        return loadSheet(path, SpriteExtractors.grid(frameWidth, frameHeight));
    }

    /**
     * Carga la imagen raw con caché y optimización de memoria.
     * Uso interno — úsalo si necesitás la BufferedImage directamente.
     *
     * ── OPTIMIZACIÓN ──────────────────────────────────────────────────────
     * Las imágenes se optimizan automáticamente:
     *   1. Downscaling si son >512px (cometa.png 800x600 → 512x384)
     *   2. Conversión a TYPE_INT_RGB si no usan transparencia (ahorra 25% RAM)
     *   3. Conversión a TYPE_INT_ARGB si usan transparencia (mejor rendering)
     *
     * @param path path relativo al classpath
     * @return BufferedImage o null si falló la carga
     */
    public static BufferedImage loadRaw(String path) {
        if (path == null || path.isBlank()) return null;

        // Hit en caché — retorno directo
        BufferedImage cached = cache.get(path);
        if (cached != null) return cached;

        // Miss en caché — cargar desde classpath
        try (InputStream is = AssetLoader.class.getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("[AssetLoader] Recurso no encontrado: " + path);
                return null;
            }
            BufferedImage original = ImageIO.read(is);
            if (original == null) {
                System.err.println("[AssetLoader] ImageIO no pudo leer: " + path);
                return null;
            }
            
            // Optimizar imagen (downscaling, formato, etc)
            BufferedImage optimized = AssetOptimizer.optimize(original);
            cache.put(path, optimized);
            return optimized;
        } catch (IOException e) {
            System.err.println("[AssetLoader] Error cargando '" + path + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Optimiza una imagen para uso eficiente en memoria y rendering.
     * Convierte a TYPE_INT_ARGB si no lo es ya (formato estándar de Java 2D).
     * 
     * @deprecated Usar AssetOptimizer.optimize() que incluye más optimizaciones
     */
    @Deprecated
    private static BufferedImage optimizeImage(BufferedImage original) {
        // Si ya está en el formato óptimo, retornar directo
        if (original.getType() == BufferedImage.TYPE_INT_ARGB) {
            return original;
        }
        
        // Convertir a formato optimizado
        int w = original.getWidth();
        int h = original.getHeight();
        BufferedImage optimized = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        
        java.awt.Graphics2D g = optimized.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();
        
        return optimized;
    }

    /**
     * Pre-carga una imagen sin usarla todavía.
     * Útil para warm-up al inicio del juego.
     *
     * @param path path relativo al classpath
     */
    public static void preload(String path) {
        loadRaw(path); // carga y cachea, descarta resultado
    }

    /**
     * Limpia toda la caché.
     * Uso: tests, recarga en caliente, o shutdown del juego.
     */
    public static void clearCache() {
        cache.clear();
        System.out.println("[AssetLoader] Caché de imágenes limpiada");
    }

    /**
     * Libera recursos al cerrar el juego.
     * Limpia la caché de imágenes para liberar memoria.
     */
    public static void shutdown() {
        int size = cache.size();
        cache.clear();
        System.out.println("[AssetLoader] Shutdown: " + size + " imágenes liberadas");
    }

    /** Cantidad de imágenes actualmente en caché. Útil para diagnóstico. */
    public static int getCacheSize() {
        return cache.size();
    }

    /** true si el path ya está en caché. */
    public static boolean isCached(String path) {
        return cache.containsKey(path);
    }
}
