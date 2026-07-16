package Sprites.Core;

import Sprites.Core.Extractors.GridExtractor;
import Sprites.Core.Extractors.SpriteExtractors;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * SpriteSheet — contenedor inmutable de frames extraídos de una imagen compuesta.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Almacena la lista de SpriteFrame producida por un SpriteExtractor y la
 * expone con acceso por índice lineal o por (col, row) para cuadrículas.
 *
 * No contiene lógica de extracción propia — esa responsabilidad es del
 * SpriteExtractor elegido al construir el sheet.
 *
 * ── CELDA VS CONTENIDO ────────────────────────────────────────────────────
 * Cada SpriteFrame tiene el tamaño de la CELDA del sheet, no del sprite
 * dibujado dentro de ella. El sprite real puede ocupar solo una parte
 * de la celda; el resto son píxeles transparentes.
 *
 * Ejemplo — Player Spritesheet (225×24 px, 9 frames de 24×24):
 *   Carlitos mide aproximadamente 15×24 px dentro de cada celda 24×24.
 *   Los ~9 px laterales son transparencia del artista. Correcto y esperado.
 *   getFrameWidth() devuelve 24 (tamaño de celda), no 15 (tamaño del dibujo).
 *
 * ── GARANTÍAS DE LOS FRAMES ──────────────────────────────────────────────
 * Los SpriteFrame almacenados son completamente independientes entre sí:
 *   - Cada uno tiene su propio buffer de píxeles (sin raster compartido).
 *   - Ningún frame puede contaminar a otro.
 *   - Son inmutables — safe para compartir entre múltiples entidades.
 *
 * Estas garantías las provee el extractor (GridExtractor usa Raster.setRect()
 * para copias pixel-perfect sin compositing).
 *
 * ── PRINCIPIO OPEN/CLOSED ─────────────────────────────────────────────────
 * SpriteSheet desconoce la implementación concreta del extractor.
 * Para soportar un nuevo formato basta implementar SpriteExtractor.
 *
 * ── API DE CONSTRUCCIÓN ───────────────────────────────────────────────────
 *
 *   // Cuadrícula uniforme:
 *   SpriteSheet sheet = SpriteSheet.load(image, SpriteExtractors.grid(24, 24));
 *
 *   // Con padding y spacing:
 *   SpriteSheet sheet = SpriteSheet.load(image,
 *       GridExtractor.builder(24, 24).padding(2).spacing(1).build());
 *
 *   // Detección automática de regiones:
 *   SpriteSheet sheet = SpriteSheet.load(image, SpriteExtractors.autoRegion());
 *
 *   // Via AssetLoader (recomendado — gestiona caché de la imagen fuente):
 *   SpriteSheet sheet = AssetLoader.loadSheet("/player/walk.png",
 *                           SpriteExtractors.grid(24, 24));
 *
 * ── COMPATIBILIDAD ────────────────────────────────────────────────────────
 * El constructor SpriteSheet(image, frameWidth, frameHeight) se mantiene
 * por compatibilidad. Internamente usa GridExtractor sin padding/spacing.
 */
public final class SpriteSheet {

    /** Frames extraídos por el SpriteExtractor. Inmutable tras la construcción. */
    private final List<SpriteFrame> frames;

    /**
     * Columnas lógicas — relevante solo para spritesheets en cuadrícula.
     * Para otros extractores vale 0 (sin semántica de cuadrícula).
     */
    private final int columns;

    /**
     * Filas lógicas — relevante solo para spritesheets en cuadrícula.
     * Para otros extractores vale 0.
     */
    private final int rows;

    // ── Constructor principal ─────────────────────────────────────────────

    /**
     * Crea un SpriteSheet extrayendo los frames con el extractor indicado.
     *
     * @param source    imagen fuente completa (no null)
     * @param extractor estrategia de extracción (no null)
     */
    private SpriteSheet(BufferedImage source, SpriteExtractor extractor, int columns, int rows) {
        this.frames  = List.copyOf(extractor.extract(source));
        this.columns = columns;
        this.rows    = rows;
    }

    // ── Fábricas estáticas ────────────────────────────────────────────────

    /**
     * Carga un SpriteSheet usando el extractor indicado.
     *
     * @param source    imagen fuente (no null)
     * @param extractor estrategia de extracción (no null)
     * @return SpriteSheet con los frames extraídos
     */
    public static SpriteSheet load(BufferedImage source, SpriteExtractor extractor) {
        if (source    == null) throw new IllegalArgumentException("SpriteSheet.load: source no puede ser null");
        if (extractor == null) throw new IllegalArgumentException("SpriteSheet.load: extractor no puede ser null");

        int cols = 0, rows = 0;
        if (extractor instanceof GridExtractor ge) {
            int fw = ge.getCellWidth();
            int fh = ge.getCellHeight();
            int stepX = fw + ge.getSpacingX();
            int stepY = fh + ge.getSpacingY();
            int innerW = source.getWidth()  - 2 * ge.getPadding();
            int innerH = source.getHeight() - 2 * ge.getPadding();
            if (fw > 0 && fh > 0 && innerW > 0 && innerH > 0) {
                cols = innerW / stepX;
                rows = innerH / stepY;
            }
        }

        return new SpriteSheet(source, extractor, cols, rows);
    }

    /**
     * Constructor de compatibilidad hacia atrás.
     * Equivale a SpriteSheet.load(source, SpriteExtractors.grid(frameWidth, frameHeight)).
     *
     * ── NOTA DE IMPLEMENTACIÓN ────────────────────────────────────────────
     * Java exige que this() sea la primera instrucción del constructor, por
     * lo que la validación no puede preceder a la llamada al constructor
     * privado. Se usa un método estático auxiliar (requireValid) que valida
     * los parámetros dentro de la lista de argumentos de this(), antes de
     * que cualquier lógica real se ejecute.
     *
     * Orden garantizado:
     *   1. requireValidSource(source)       → lanza si source es null
     *   2. requirePositive(frameWidth, ...)  → lanza si ≤ 0
     *   3. requirePositive(frameHeight, ...) → lanza si ≤ 0
     *   4. this(source, extractor, cols, rows) → construye
     *
     * @deprecated Preferir {@link #load(BufferedImage, SpriteExtractor)} con
     *             {@code SpriteExtractors.grid(frameWidth, frameHeight)}.
     */
    @Deprecated(since = "hrfc-004", forRemoval = false)
    public SpriteSheet(BufferedImage source, int frameWidth, int frameHeight) {
        this(
            requireValidSource(source),
            SpriteExtractors.grid(
                requirePositive(frameWidth,  "SpriteSheet: frameWidth debe ser > 0"),
                requirePositive(frameHeight, "SpriteSheet: frameHeight debe ser > 0")
            ),
            source.getWidth()  / frameWidth,
            source.getHeight() / frameHeight
        );
    }

    /** Valida que source no sea null. Retorna source para uso en this(). */
    private static BufferedImage requireValidSource(BufferedImage source) {
        if (source == null) throw new IllegalArgumentException("SpriteSheet: source no puede ser null");
        return source;
    }

    /** Valida que value > 0. Retorna value para uso en this(). */
    private static int requirePositive(int value, String message) {
        if (value <= 0) throw new IllegalArgumentException(message);
        return value;
    }

    // ── Acceso a frames (API principal) ───────────────────────────────────

    /**
     * Devuelve el frame en la posición lineal indicada.
     * Los frames están ordenados fila por fila (izquierda→derecha, arriba→abajo).
     *
     * @param index índice lineal del frame [0, getTotalFrames())
     * @return SpriteFrame o SpriteFrame.empty() si el índice está fuera de rango
     */
    public SpriteFrame getFrame(int index) {
        if (index < 0 || index >= frames.size()) return SpriteFrame.EMPTY;
        return frames.get(index);
    }

    /**
     * Devuelve el frame en la posición (col, row) de la cuadrícula.
     * Solo funciona correctamente para sheets extraídos con GridExtractor.
     *
     * Si el sheet no tiene semántica de cuadrícula (columns == 0), aplica
     * el mapeo lineal row-major: índice = row * columns_estimadas + col.
     * En ese caso se recomienda usar {@link #getFrame(int)} directamente.
     *
     * @param col columna (0-indexed)
     * @param row fila   (0-indexed)
     * @return SpriteFrame o SpriteFrame.EMPTY si fuera de rango
     */
    public SpriteFrame getFrame(int col, int row) {
        if (columns <= 0) {
            // Sin semántica de cuadrícula — tratar col como índice lineal por fila
            // Esto es un fallback seguro; usar getFrame(int) es más claro.
            return getFrame(col);
        }
        int index = row * columns + col;
        return getFrame(index);
    }

    /**
     * Extrae todos los frames de una fila completa.
     *
     * @param row   fila (0-indexed)
     * @param count cantidad de frames a extraer desde la columna 0
     * @return array de SpriteFrame
     */
    public SpriteFrame[] getRow(int row, int count) {
        SpriteFrame[] result = new SpriteFrame[count];
        for (int col = 0; col < count; col++) {
            result[col] = getFrame(col, row);
        }
        return result;
    }

    /**
     * Extrae todos los frames de una fila completa (todas las columnas).
     *
     * @param row fila (0-indexed)
     * @return array de SpriteFrame
     */
    public SpriteFrame[] getRow(int row) {
        if (columns <= 0) return new SpriteFrame[0];
        return getRow(row, columns);
    }

    /**
     * Extrae todos los frames de una columna completa.
     *
     * @param col   columna (0-indexed)
     * @param count cantidad de frames desde la fila 0
     * @return array de SpriteFrame
     */
    public SpriteFrame[] getColumn(int col, int count) {
        SpriteFrame[] result = new SpriteFrame[count];
        for (int row = 0; row < count; row++) {
            result[row] = getFrame(col, row);
        }
        return result;
    }

    /**
     * Extrae un rango lineal de frames (de startIndex a startIndex+count-1).
     *
     * @param startIndex índice lineal del primer frame
     * @param count      cantidad de frames a extraer
     * @return array de SpriteFrame
     */
    public SpriteFrame[] getRange(int startIndex, int count) {
        SpriteFrame[] result = new SpriteFrame[count];
        for (int i = 0; i < count; i++) {
            result[i] = getFrame(startIndex + i);
        }
        return result;
    }

    /**
     * Devuelve todos los frames del sheet como array.
     * Útil para pasar la secuencia completa a Animation.loop() o al Builder.
     */
    public SpriteFrame[] getAllFrames() {
        return frames.toArray(new SpriteFrame[0]);
    }

    // ── Diagnóstico ───────────────────────────────────────────────────────

    /**
     * Imprime a stderr un resumen del estado del sheet.
     * Útil para verificar que la extracción produjo el número correcto de
     * frames con las dimensiones esperadas.
     *
     * Ejemplo de salida:
     *   [SpriteSheet] diagnose() ————————————————
     *   Total frames: 9  |  grid: 9×1
     *   Frame  0: 24×24 px  valid=true
     *   Frame  1: 24×24 px  valid=true
     *   ...
     */
    public void diagnose() {
        System.err.println("[SpriteSheet] diagnose() ————————————————————————");
        System.err.println("  Total frames: " + frames.size()
            + (columns > 0 ? "  grid: " + columns + "x" + rows : "  (sin semántica de cuadrícula)"));
        for (int i = 0; i < frames.size(); i++) {
            SpriteFrame f = frames.get(i);
            System.err.printf("  Frame %2d: %dx%d px  valid=%-5b%n",
                i, f.getWidth(), f.getHeight(), f.isValid());
        }
        System.err.println("[SpriteSheet] ———————————————————————————————————");
    }

    // ── Getters de metadatos ──────────────────────────────────────────────

    /** Total de frames extraídos. */
    public int getTotalFrames() { return frames.size(); }

    /**
     * Columnas lógicas (válido solo para sheets en cuadrícula).
     * 0 si el extractor no produce frames en cuadrícula.
     */
    public int getColumns() { return columns; }

    /**
     * Filas lógicas (válido solo para sheets en cuadrícula).
     * 0 si el extractor no produce frames en cuadrícula.
     */
    public int getRows() { return rows; }

    /**
     * Ancho del primer frame (referencia).
     * 0 si el sheet no tiene frames.
     */
    public int getFrameWidth() {
        return frames.isEmpty() ? 0 : frames.get(0).getWidth();
    }

    /**
     * Alto del primer frame (referencia).
     * 0 si el sheet no tiene frames.
     */
    public int getFrameHeight() {
        return frames.isEmpty() ? 0 : frames.get(0).getHeight();
    }

    @Override
    public String toString() {
        return "SpriteSheet[" + frames.size() + " frames"
               + (columns > 0 ? ", " + columns + "x" + rows + " grid" : "")
               + "]";
    }
}
