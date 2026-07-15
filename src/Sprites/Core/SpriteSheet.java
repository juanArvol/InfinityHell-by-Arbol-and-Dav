package Sprites.Core;

import java.awt.image.BufferedImage;

/**
 * SpriteSheet — extrae frames de una imagen compuesta en cuadrícula.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Conoce la imagen fuente y sabe cómo cortarla en frames individuales.
 * No carga la imagen — recibe la BufferedImage ya cargada por AssetLoader.
 *
 * ── USO ACTUAL ────────────────────────────────────────────────────────────
 * El proyecto actualmente trabaja solo con imágenes individuales.
 * SpriteSheet queda listo para cuando lleguen las hojas de sprites reales.
 *
 *   // Hoja de 4 columnas x 2 filas, frames de 32x32
 *   SpriteSheet sheet = new SpriteSheet(image, 32, 32);
 *   SpriteFrame[] walkFrames = sheet.getRow(0, 4);   // fila 0, primeros 4 frames
 *   SpriteFrame   idle       = sheet.getFrame(1, 0); // fila 1, columna 0
 *
 * ── PREPARACIÓN FUTURA ────────────────────────────────────────────────────
 * La arquitectura permite añadir posteriormente:
 *   - Frames irregulares (x, y, w, h explícitos)
 *   - Metadata externa (JSON/XML)
 *   - Atlas con nombres de frame
 *   - Múltiples hojas por entidad
 */
public final class SpriteSheet {

    private final BufferedImage source;
    private final int frameWidth;
    private final int frameHeight;

    /** Columnas calculadas a partir del ancho de la imagen y el frame. */
    private final int columns;

    /** Filas calculadas a partir del alto de la imagen y el frame. */
    private final int rows;

    /**
     * @param source      imagen fuente completa (no null)
     * @param frameWidth  ancho de cada frame en píxeles
     * @param frameHeight alto de cada frame en píxeles
     */
    public SpriteSheet(BufferedImage source, int frameWidth, int frameHeight) {
        if (source == null)      throw new IllegalArgumentException("SpriteSheet: source no puede ser null");
        if (frameWidth  <= 0)    throw new IllegalArgumentException("SpriteSheet: frameWidth debe ser > 0");
        if (frameHeight <= 0)    throw new IllegalArgumentException("SpriteSheet: frameHeight debe ser > 0");

        this.source      = source;
        this.frameWidth  = frameWidth;
        this.frameHeight = frameHeight;
        this.columns     = source.getWidth()  / frameWidth;
        this.rows        = source.getHeight() / frameHeight;
    }

    // ── Extracción de frames ──────────────────────────────────────────────

    /**
     * Obtiene el frame en la posición (col, row) de la cuadrícula.
     *
     * @param col columna (0-indexed)
     * @param row fila   (0-indexed)
     * @return SpriteFrame con la subimagen extraída
     * @throws IndexOutOfBoundsException si col o row están fuera del rango
     */
    public SpriteFrame getFrame(int col, int row) {
        validateIndex(col, row);
        int x = col * frameWidth;
        int y = row * frameHeight;
        BufferedImage sub = source.getSubimage(x, y, frameWidth, frameHeight);
        return new SpriteFrame(sub);
    }

    /**
     * Extrae todos los frames de una fila completa.
     *
     * @param row      fila (0-indexed)
     * @param count    cantidad de frames a extraer (desde col 0)
     * @return array de SpriteFrame con los frames de esa fila
     */
    public SpriteFrame[] getRow(int row, int count) {
        if (row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException(
                "SpriteSheet.getRow: fila " + row + " fuera de rango [0," + rows + ")");
        }
        if (count > columns) {
            throw new IndexOutOfBoundsException(
                "SpriteSheet.getRow: count " + count + " supera las columnas disponibles " + columns);
        }

        SpriteFrame[] frames = new SpriteFrame[count];
        for (int col = 0; col < count; col++) {
            frames[col] = getFrame(col, row);
        }
        return frames;
    }

    /**
     * Extrae todos los frames de una fila completa (todos los columns).
     *
     * @param row fila (0-indexed)
     */
    public SpriteFrame[] getRow(int row) {
        return getRow(row, columns);
    }

    /**
     * Extrae todos los frames de una columna completa.
     *
     * @param col   columna (0-indexed)
     * @param count cantidad de frames a extraer (desde row 0)
     */
    public SpriteFrame[] getColumn(int col, int count) {
        if (col < 0 || col >= columns) {
            throw new IndexOutOfBoundsException(
                "SpriteSheet.getColumn: columna " + col + " fuera de rango [0," + columns + ")");
        }
        if (count > rows) {
            throw new IndexOutOfBoundsException(
                "SpriteSheet.getColumn: count " + count + " supera las filas disponibles " + rows);
        }

        SpriteFrame[] frames = new SpriteFrame[count];
        for (int row = 0; row < count; row++) {
            frames[row] = getFrame(col, row);
        }
        return frames;
    }

    /**
     * Extrae un rango lineal de frames (recorre la hoja de izquierda a derecha,
     * arriba a abajo). Útil para atlas planos sin estructura de cuadrícula lógica.
     *
     * @param startIndex índice lineal del primer frame
     * @param count      cantidad de frames a extraer
     */
    public SpriteFrame[] getRange(int startIndex, int count) {
        SpriteFrame[] frames = new SpriteFrame[count];
        for (int i = 0; i < count; i++) {
            int idx = startIndex + i;
            int col = idx % columns;
            int row = idx / columns;
            frames[i] = getFrame(col, row);
        }
        return frames;
    }

    // ── Validación ────────────────────────────────────────────────────────

    private void validateIndex(int col, int row) {
        if (col < 0 || col >= columns || row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException(
                "SpriteSheet: índice (" + col + "," + row
                + ") fuera de rango. Dimensiones: " + columns + "x" + rows);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public int getFrameWidth()  { return frameWidth;  }
    public int getFrameHeight() { return frameHeight; }
    public int getColumns()     { return columns;     }
    public int getRows()        { return rows;        }
    public int getTotalFrames() { return columns * rows; }

    /** Imagen fuente completa (uso interno del pipeline). */
    public BufferedImage getSource() { return source; }

    @Override
    public String toString() {
        return "SpriteSheet[" + columns + "x" + rows
               + " frames, " + frameWidth + "x" + frameHeight + "px each]";
    }
}
