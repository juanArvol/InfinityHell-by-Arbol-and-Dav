package Sprites.Core;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * AssetOptimizer — herramientas de optimización de sprites en tiempo de carga.
 *
 * ── OPTIMIZACIONES IMPLEMENTADAS ──────────────────────────────────────────
 * 1. **Downscaling automático**: Reduce sprites grandes a un tamaño máximo
 * 2. **Detección de transparencia**: Identifica si un PNG usa alpha channel
 * 3. **Conversión de formato**: Convierte a TYPE_INT_RGB si no hay transparencia
 * 4. **Crop de transparencia**: Elimina bordes transparentes innecesarios
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 * AssetOptimizer se usa en AssetLoader para optimizar automáticamente
 * imágenes grandes durante la carga:
 *
 *   BufferedImage original = ImageIO.read(...);
 *   BufferedImage optimized = AssetOptimizer.optimize(original);
 *
 * ── CUÁNDO SE ACTIVA ──────────────────────────────────────────────────────
 * - Downscaling: Solo si la imagen supera MAX_SPRITE_DIMENSION (512px)
 * - Conversión RGB: Solo si la imagen no tiene píxeles semi-transparentes
 * - Crop: Solo si está habilitado y hay bordes transparentes >10px
 */
public final class AssetOptimizer {

    /** Dimensión máxima permitida para sprites (ancho o alto). */
    private static final int MAX_SPRITE_DIMENSION = 512;

    /** Tamaño mínimo de borde transparente para activar crop (px). */
    private static final int MIN_CROP_THRESHOLD = 10;

    /** Control global de optimizaciones. */
    private static boolean ENABLE_DOWNSCALING = true;
    private static boolean ENABLE_FORMAT_CONVERSION = true;
    private static boolean ENABLE_AUTOCROP = false; // Desactivado por defecto (puede romper pivots)

    private AssetOptimizer() {}

    // ══════════════════════════════════════════════════════════════════════
    // API Pública
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Optimiza una imagen aplicando todas las optimizaciones habilitadas.
     *
     * @param original imagen a optimizar
     * @return imagen optimizada (puede ser la misma si no se aplicó nada)
     */
    public static BufferedImage optimize(BufferedImage original) {
        if (original == null) return null;

        BufferedImage result = original;

        // 1. Downscaling si es muy grande
        if (ENABLE_DOWNSCALING) {
            result = downscaleIfNeeded(result);
        }

        // 2. Conversión de formato si no usa transparencia
        if (ENABLE_FORMAT_CONVERSION) {
            result = convertFormatIfPossible(result);
        }

        // 3. Crop de transparencia (solo si está habilitado)
        if (ENABLE_AUTOCROP) {
            result = autocropIfNeeded(result);
        }

        return result;
    }

    /**
     * Reduce el tamaño de una imagen si supera MAX_SPRITE_DIMENSION.
     * Mantiene el aspect ratio original.
     */
    public static BufferedImage downscaleIfNeeded(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        // No hace falta optimizar
        if (w <= MAX_SPRITE_DIMENSION && h <= MAX_SPRITE_DIMENSION) {
            return img;
        }

        // Calcular nuevo tamaño manteniendo aspect ratio
        float scale = Math.min(
            (float) MAX_SPRITE_DIMENSION / w,
            (float) MAX_SPRITE_DIMENSION / h
        );

        int newW = Math.round(w * scale);
        int newH = Math.round(h * scale);

        System.out.println("[AssetOptimizer] Downscaling " + w + "x" + h + " → " + newW + "x" + newH);

        return resize(img, newW, newH);
    }

    /**
     * Convierte a TYPE_INT_RGB si la imagen no tiene transparencia real.
     * Ahorra ~25% de memoria para imágenes opacas.
     */
    public static BufferedImage convertFormatIfPossible(BufferedImage img) {
        // Ya es ARGB o necesita alpha
        if (img.getType() == BufferedImage.TYPE_INT_ARGB || hasTransparency(img)) {
            // Convertir a ARGB si no lo es (optimización de AssetLoader)
            if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
                return convertTo(img, BufferedImage.TYPE_INT_ARGB);
            }
            return img;
        }

        // No tiene transparencia → convertir a RGB
        System.out.println("[AssetOptimizer] Convirtiendo a TYPE_INT_RGB (sin alpha)");
        return convertTo(img, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Elimina bordes transparentes innecesarios.
     * CUIDADO: Puede romper pivots si no se ajustan después.
     */
    public static BufferedImage autocropIfNeeded(BufferedImage img) {
        if (!hasTransparency(img)) return img;

        int[] bounds = getOpaqueBounds(img);
        if (bounds == null) return img; // Imagen completamente transparente

        int x = bounds[0];
        int y = bounds[1];
        int w = bounds[2];
        int h = bounds[3];

        // ¿Vale la pena el crop?
        int savedX = x + (img.getWidth() - (x + w));
        int savedY = y + (img.getHeight() - (y + h));

        if (savedX < MIN_CROP_THRESHOLD && savedY < MIN_CROP_THRESHOLD) {
            return img; // No hay suficiente espacio para justificar el crop
        }

        System.out.println("[AssetOptimizer] Autocrop: " + img.getWidth() + "x" + img.getHeight() 
            + " → " + w + "x" + h + " (ahorró " + savedX + "x" + savedY + " px)");

        return img.getSubimage(x, y, w, h);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Utilidades Internas
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Redimensiona una imagen con interpolación de alta calidad.
     */
    private static BufferedImage resize(BufferedImage original, int newWidth, int newHeight) {
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(original, 0, 0, newWidth, newHeight, null);
        } finally {
            g.dispose();
        }
        return resized;
    }

    /**
     * Convierte una imagen a un formato específico.
     */
    private static BufferedImage convertTo(BufferedImage original, int targetType) {
        if (original.getType() == targetType) return original;

        int w = original.getWidth();
        int h = original.getHeight();
        BufferedImage converted = new BufferedImage(w, h, targetType);

        Graphics2D g = converted.createGraphics();
        try {
            g.drawImage(original, 0, 0, null);
        } finally {
            g.dispose();
        }

        return converted;
    }

    /**
     * Detecta si la imagen tiene píxeles con transparencia (alpha < 255).
     */
    private static boolean hasTransparency(BufferedImage img) {
        if (!img.getColorModel().hasAlpha()) return false;

        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                if (alpha < 255) return true;
            }
        }

        return false;
    }

    /**
     * Calcula el bounding box de píxeles opacos.
     * Retorna [x, y, width, height] o null si todo es transparente.
     */
    private static int[] getOpaqueBounds(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        int minX = w, minY = h, maxX = 0, maxY = 0;
        boolean foundOpaque = false;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;

                if (alpha > 0) { // Píxel visible
                    foundOpaque = true;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (!foundOpaque) return null;

        return new int[]{ minX, minY, maxX - minX + 1, maxY - minY + 1 };
    }

    // ══════════════════════════════════════════════════════════════════════
    // Configuración
    // ══════════════════════════════════════════════════════════════════════

    public static void setEnableDownscaling(boolean enable) {
        ENABLE_DOWNSCALING = enable;
    }

    public static void setEnableFormatConversion(boolean enable) {
        ENABLE_FORMAT_CONVERSION = enable;
    }

    public static void setEnableAutocrop(boolean enable) {
        ENABLE_AUTOCROP = enable;
    }

    public static boolean isDownscalingEnabled() {
        return ENABLE_DOWNSCALING;
    }

    public static boolean isFormatConversionEnabled() {
        return ENABLE_FORMAT_CONVERSION;
    }

    public static boolean isAutocropEnabled() {
        return ENABLE_AUTOCROP;
    }
}
