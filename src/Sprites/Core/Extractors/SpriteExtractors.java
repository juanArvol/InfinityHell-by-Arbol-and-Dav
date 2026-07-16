package Sprites.Core.Extractors;

import Sprites.Core.SpriteExtractor;

/**
 * SpriteExtractors — fábrica de extractores de sprites.
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * Punto de entrada unificado para obtener cualquier SpriteExtractor.
 * Oculta las clases concretas al código consumidor y permite cambiar
 * implementaciones sin modificar los callers.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Grid uniforme (caso más común):
 *   SpriteSheet sheet = SpriteSheet.load(image, SpriteExtractors.grid(24, 24));
 *
 *   // Grid con tamaño de frame diferente:
 *   SpriteSheet sheet = SpriteSheet.load(image, SpriteExtractors.grid(32, 48));
 *
 *   // Grid limitado a las primeras 4 columnas de la fila 2:
 *   SpriteSheet sheet = SpriteSheet.load(image,
 *       SpriteExtractors.grid(GridExtractor.builder(24, 24)
 *           .startRow(2).columns(4).build()));
 *
 *   // Auto-detección de regiones para spritesheets artesanales:
 *   SpriteSheet sheet = SpriteSheet.load(image, SpriteExtractors.autoRegion());
 *
 *   // Auto-detección con configuración personalizada:
 *   SpriteSheet sheet = SpriteSheet.load(image,
 *       SpriteExtractors.autoRegion(
 *           AutoRegionExtractor.builder().alphaThreshold(20).padding(2).build()));
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Cuando se implemente un nuevo extractor (AtlasExtractor, JsonExtractor…)
 * bastará con añadir aquí un nuevo método de fábrica.
 * Ningún caller existente necesitará cambiar.
 */
public final class SpriteExtractors {

    private SpriteExtractors() {}

    // ── Grid ─────────────────────────────────────────────────────────────

    /**
     * Extractor de cuadrícula uniforme.
     * Extrae todos los frames que caben en la imagen con el tamaño indicado.
     *
     * @param frameWidth  ancho de cada frame en píxeles
     * @param frameHeight alto de cada frame en píxeles
     */
    public static SpriteExtractor grid(int frameWidth, int frameHeight) {
        return GridExtractor.of(frameWidth, frameHeight);
    }

    /**
     * Extractor de cuadrícula con límite de columnas y filas.
     *
     * @param frameWidth  ancho de cada frame en píxeles
     * @param frameHeight alto de cada frame en píxeles
     * @param columns     columnas a extraer (0 = todas)
     * @param rows        filas a extraer    (0 = todas)
     */
    public static SpriteExtractor grid(int frameWidth, int frameHeight, int columns, int rows) {
        return GridExtractor.of(frameWidth, frameHeight, columns, rows);
    }

    /**
     * Extractor de cuadrícula con configuración completa (Builder).
     * Usar para rangos de extracción avanzados (startRow, startCol).
     *
     * @param extractor instancia ya configurada de GridExtractor
     */
    public static SpriteExtractor grid(GridExtractor extractor) {
        return extractor;
    }

    // ── Auto-region ───────────────────────────────────────────────────────

    /**
     * Extractor automático de regiones con configuración por defecto.
     * Detecta regiones conectadas de píxeles no transparentes.
     */
    public static SpriteExtractor autoRegion() {
        return AutoRegionExtractor.withDefaults();
    }

    /**
     * Extractor automático de regiones con configuración personalizada.
     *
     * @param extractor instancia ya configurada de AutoRegionExtractor
     */
    public static SpriteExtractor autoRegion(AutoRegionExtractor extractor) {
        return extractor;
    }

    /**
     * Extractor automático de regiones con umbral de alpha personalizado.
     * Atajo sin necesidad de crear manualmente el AutoRegionExtractor.
     *
     * @param alphaThreshold umbral de alpha [0..255] para detectar píxeles opacos
     */
    public static SpriteExtractor autoRegion(int alphaThreshold) {
        return AutoRegionExtractor.builder()
            .alphaThreshold(alphaThreshold)
            .build();
    }
}
