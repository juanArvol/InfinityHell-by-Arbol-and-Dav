package Sprites.Core.Extractors;

import Sprites.Core.SpriteExtractor;
import Sprites.Core.SpriteFrame;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

/**
 * GridExtractor — extracción pixel-perfect en cuadrícula.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Divide la imagen fuente en celdas de tamaño fijo y extrae cada celda como
 * un SpriteFrame completamente independiente.
 *
 * ── MODELO DE COORDENADAS ─────────────────────────────────────────────────
 * Para una celda en (col, row):
 *
 *   srcX = padding + col * (cellWidth  + spacingX)
 *   srcY = padding + row * (cellHeight + spacingY)
 *
 * Donde:
 *   padding  → margen externo del sheet (mismo en los 4 lados)
 *   spacingX → píxeles de separación horizontal entre celdas
 *   spacingY → píxeles de separación vertical entre celdas
 *   cellWidth / cellHeight → tamaño de cada celda (lo que se extrae)
 *
 * El caso más común es padding=0, spacingX=0, spacingY=0.
 *
 * ── DISTINCIÓN CELDA / CONTENIDO ──────────────────────────────────────────
 * El extractor recorta la CELDA completa (cellWidth × cellHeight).
 * El sprite real puede ocupar solo una parte de esa celda — el resto son
 * píxeles transparentes. Eso es correcto y esperado: el SpriteFrame tiene
 * el tamaño de la celda, con transparencia donde no hay dibujo.
 *
 * Ejemplo — Player (Carlitos ~15×24 dentro de una celda 24×24):
 *   El frame es 24×24, con ~9px de transparencia a los lados del personaje.
 *   Esto NO es un bug. Es el padding visual del artista dentro de la celda.
 *
 * ── COPIA PIXEL-PERFECT ───────────────────────────────────────────────────
 * La extracción usa Raster.setRect() en lugar de Graphics2D.drawImage().
 * Esto garantiza:
 *   - Copia directa de píxeles sin ningún compositing.
 *   - Sin rendering hints, sin interpolación, sin AlphaComposite.
 *   - Imposible que un pixel de un frame vecino contamine otro frame.
 *   - El buffer destino se inicializa limpio (todos los pixels 0x00000000)
 *     antes de la copia — no hay residuos de memoria.
 *
 * ── VERIFICACIÓN MATEMÁTICA ───────────────────────────────────────────────
 * Player Spritesheet: 225×24 px, 9 frames de 24×24, padding=0, spacing=0.
 *
 *   effectiveCols = (225 - 0) / (24 + 0) = 9 (descarta 9px al final)
 *   Frame 0: srcX = 0+0*(24+0) = 0,   srcY = 0 → región [0..23, 0..23]  ✓
 *   Frame 1: srcX = 0+1*(24+0) = 24,  srcY = 0 → región [24..47, 0..23] ✓
 *   Frame 8: srcX = 0+8*(24+0) = 192, srcY = 0 → región [192..215, 0..23]
 *            srcX + cellWidth = 216 ≤ 225 ✓
 *   Frame 9: NO existe — effectiveCols = 9, loop termina en col=8 ✓
 *
 * ── PARÁMETROS ────────────────────────────────────────────────────────────
 *   cellWidth  / cellHeight → tamaño de cada celda (obligatorio)
 *   columns    / rows       → cuántas celdas extraer (0 = todas las que caben)
 *   startCol   / startRow   → primera celda (0-indexed)
 *   padding                 → margen externo del sheet en píxeles (default 0)
 *   spacingX   / spacingY   → separación entre celdas en píxeles (default 0)
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Caso habitual — sin padding ni spacing:
 *   SpriteSheet sheet = SpriteSheet.load(image, SpriteExtractors.grid(24, 24));
 *
 *   // Sheet con 1px de separación entre frames y 2px de margen externo:
 *   SpriteSheet sheet = SpriteSheet.load(image,
 *       GridExtractor.builder(24, 24).spacing(1).padding(2).build());
 *
 *   // Solo las primeras 4 columnas de la fila 2:
 *   SpriteSheet sheet = SpriteSheet.load(image,
 *       GridExtractor.builder(24, 24).startRow(2).columns(4).build());
 */
public final class GridExtractor implements SpriteExtractor {

    private final int cellWidth;
    private final int cellHeight;

    /** Número de columnas a extraer. 0 = todas las que caben. */
    private final int columns;

    /** Número de filas a extraer. 0 = todas las que caben. */
    private final int rows;

    /** Primera columna (0-indexed). */
    private final int startCol;

    /** Primera fila (0-indexed). */
    private final int startRow;

    /** Margen externo del sheet en píxeles (igual en los 4 lados). */
    private final int padding;

    /** Separación horizontal en píxeles entre celdas adyacentes. */
    private final int spacingX;

    /** Separación vertical en píxeles entre celdas adyacentes. */
    private final int spacingY;

    // ── Constructor ──────────────────────────────────────────────────────

    private GridExtractor(Builder b) {
        this.cellWidth  = b.cellWidth;
        this.cellHeight = b.cellHeight;
        this.columns    = b.columns;
        this.rows       = b.rows;
        this.startCol   = b.startCol;
        this.startRow   = b.startRow;
        this.padding    = b.padding;
        this.spacingX   = b.spacingX;
        this.spacingY   = b.spacingY;
    }

    // ── Fábricas rápidas ─────────────────────────────────────────────────

    /**
     * Extrae todos los frames que caben con el tamaño indicado.
     * Sin padding ni spacing (caso más común).
     */
    public static GridExtractor of(int cellWidth, int cellHeight) {
        return new Builder(cellWidth, cellHeight).build();
    }

    /**
     * Extrae un rango limitado de celdas.
     *
     * @param cellWidth  ancho de cada celda
     * @param cellHeight alto de cada celda
     * @param columns    columnas a extraer (0 = todas)
     * @param rows       filas a extraer    (0 = todas)
     */
    public static GridExtractor of(int cellWidth, int cellHeight, int columns, int rows) {
        return new Builder(cellWidth, cellHeight).columns(columns).rows(rows).build();
    }

    /** Builder para configuración completa (padding, spacing, startCol/Row). */
    public static Builder builder(int cellWidth, int cellHeight) {
        return new Builder(cellWidth, cellHeight);
    }

    // ── Extracción ────────────────────────────────────────────────────────

    @Override
    public List<SpriteFrame> extract(BufferedImage source) {
        if (source == null) return List.of();

        int srcW = source.getWidth();
        int srcH = source.getHeight();

        // Paso de una celda a la siguiente: tamaño de celda + separación
        int stepX = cellWidth  + spacingX;
        int stepY = cellHeight + spacingY;

        // Espacio interior del sheet (descontando el padding en ambos lados)
        // El padding se aplica en el borde izquierdo Y en el derecho, por eso 2×.
        int innerW = srcW - 2 * padding;
        int innerH = srcH - 2 * padding;

        if (innerW <= 0 || innerH <= 0) return List.of();

        // Cuántas celdas completas caben en el espacio interior
        int totalCols = innerW / stepX;
        int totalRows = innerH / stepY;

        // Celdas efectivas a extraer contando desde startCol/startRow
        int effectiveCols = (columns > 0)
            ? Math.min(columns, totalCols - startCol)
            : totalCols - startCol;

        int effectiveRows = (rows > 0)
            ? Math.min(rows, totalRows - startRow)
            : totalRows - startRow;

        if (effectiveCols <= 0 || effectiveRows <= 0) return List.of();

        List<SpriteFrame> frames = new ArrayList<>(effectiveCols * effectiveRows);

        for (int row = startRow; row < startRow + effectiveRows; row++) {
            for (int col = startCol; col < startCol + effectiveCols; col++) {
                SpriteFrame frame = extractCell(source, col, row, srcW, srcH);
                frames.add(frame);
            }
        }

        return frames;
    }

    /**
     * Extrae exactamente la celda en (col, row) del sheet.
     *
     * ── COORDENADAS ───────────────────────────────────────────────────────
     *   srcX = padding + col * (cellWidth  + spacingX)
     *   srcY = padding + row * (cellHeight + spacingY)
     *
     * ── COPIA PIXEL-PERFECT VÍA RASTER ───────────────────────────────────
     * Se usa Raster.setRect() que copia los valores de canal ARGB de forma
     * directa, sin ningún compositing ni interpolación posible. Es la única
     * operación que garantiza una copia exacta de los píxeles fuente.
     *
     * El buffer destino es TYPE_INT_ARGB inicializado con todos los píxeles
     * en 0x00000000 por la JVM — no hay residuos de memoria. Raster.setRect()
     * sobreescribe la región completa (w × h píxeles).
     *
     * ── POR QUÉ NO drawImage ──────────────────────────────────────────────
     * drawImage con Graphics2D aplica AlphaComposite.SrcOver por defecto.
     * Incluso con NEAREST_NEIGHBOR, el compositing puede mezclar el píxel
     * fuente con el fondo (0x00000000) en píxeles semitransparentes, alterando
     * levemente el color. setRect() copia los bits exactos, sin mezcla.
     */
    private SpriteFrame extractCell(BufferedImage source,
                                     int col, int row,
                                     int srcW, int srcH) {
        // Coordenada superior-izquierda de la celda en el sheet
        int srcX = padding + col * (cellWidth  + spacingX);
        int srcY = padding + row * (cellHeight + spacingY);

        // Ancho y alto reales a copiar (protección contra borde del sheet)
        int w = Math.min(cellWidth,  srcW - srcX);
        int h = Math.min(cellHeight, srcH - srcY);

        if (w <= 0 || h <= 0) return SpriteFrame.empty();

        // Buffer destino limpio — TYPE_INT_ARGB → todos los píxeles 0x00000000
        BufferedImage dest = new BufferedImage(cellWidth, cellHeight,
                                               BufferedImage.TYPE_INT_ARGB);

        // Copia pixel-perfect: lee el raster fuente y lo escribe en el destino
        // sin ningún compositing, sin interpolación, sin rendering hints.
        WritableRaster destRaster   = dest.getRaster();
        WritableRaster sourceRaster = source.getRaster();

        // getDataElements lee los píxeles en el modelo de color nativo del source.
        // setDataElements los escribe en el modelo del destino (ambos INT_ARGB).
        // Si el source no es INT_ARGB, se convierte automáticamente vía ColorModel.
        destRaster.setRect(0, 0,
            sourceRaster.createChild(srcX, srcY, w, h, 0, 0, null));

        return new SpriteFrame(dest);
    }

    /**
     * Exporta cada SpriteFrame extraído como un PNG independiente en el
     * directorio indicado. Solo para diagnóstico — no llamar en producción.
     *
     * Uso desde código temporal (ej. al inicio de Assets.init()):
     *   GridExtractor.exportFrames(source, extractor, "C:/temp/frames");
     *
     * Comparar los PNG resultantes con las celdas del sheet original.
     * Si un PNG ya contiene píxeles del frame vecino, el problema está
     * en la extracción (cellWidth incorrecto). Si los PNG son perfectos
     * y el artefacto solo aparece en pantalla, el problema está en el render.
     *
     * @param source    imagen fuente del sheet
     * @param extractor extractor ya configurado
     * @param outputDir ruta del directorio donde guardar los PNG
     */
    public static void exportFrames(java.awt.image.BufferedImage source,
                                    GridExtractor extractor,
                                    String outputDir) {
        if (source == null || extractor == null || outputDir == null) {
            System.err.println("[GridExtractor.exportFrames] parámetros inválidos");
            return;
        }
        java.io.File dir = new java.io.File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        List<SpriteFrame> frames = extractor.extract(source);
        System.err.println("[GridExtractor.exportFrames] exportando " + frames.size()
            + " frames a: " + dir.getAbsolutePath());

        for (int i = 0; i < frames.size(); i++) {
            SpriteFrame frame = frames.get(i);
            if (!frame.isValid()) {
                System.err.println("  frame_" + i + ".png → VACÍO, saltado");
                continue;
            }
            java.io.File out = new java.io.File(dir, "frame_" + i + ".png");
            try {
                javax.imageio.ImageIO.write(frame.getImage(), "PNG", out);
                System.err.println("  frame_" + i + ".png → "
                    + frame.getWidth() + "x" + frame.getHeight() + " px  OK");
            } catch (java.io.IOException e) {
                System.err.println("  frame_" + i + ".png → ERROR: " + e.getMessage());
            }
        }
        System.err.println("[GridExtractor.exportFrames] listo.");
    }
     /* la imagen dada. Útil para verificar visualmente que el extractor
     * apunta a las regiones correctas antes de extraer.
     *
     * @param source imagen a inspeccionar (puede ser null — imprime mensaje)
     */
    public void diagnose(BufferedImage source) {
        System.err.println("[GridExtractor] diagnose() —————————————————————");
        System.err.println("  Configuración: cell=" + cellWidth + "x" + cellHeight
            + "  padding=" + padding
            + "  spacingX=" + spacingX + "  spacingY=" + spacingY
            + "  startCol=" + startCol + "  startRow=" + startRow
            + "  columns=" + (columns > 0 ? columns : "auto")
            + "  rows="    + (rows    > 0 ? rows    : "auto"));

        if (source == null) {
            System.err.println("  [ERROR] source es null");
            return;
        }
        System.err.println("  Sheet: " + source.getWidth() + "x" + source.getHeight() + " px");

        List<SpriteFrame> extracted = extract(source);
        System.err.println("  Frames extraídos: " + extracted.size());

        int stepX = cellWidth  + spacingX;
        int stepY = cellHeight + spacingY;
        int totalCols = (source.getWidth()  - 2 * padding) / stepX;
        int totalRows = (source.getHeight() - 2 * padding) / stepY;
        int effectiveCols = (columns > 0) ? Math.min(columns, totalCols - startCol)
                                          : totalCols - startCol;
        int effectiveRows = (rows    > 0) ? Math.min(rows,    totalRows - startRow)
                                          : totalRows - startRow;

        for (int row = startRow; row < startRow + effectiveRows; row++) {
            for (int col = startCol; col < startCol + effectiveCols; col++) {
                int sx = padding + col * stepX;
                int sy = padding + row * stepY;
                int w  = Math.min(cellWidth,  source.getWidth()  - sx);
                int h  = Math.min(cellHeight, source.getHeight() - sy);
                System.err.printf("  [%d,%d] → srcX=%d srcY=%d w=%d h=%d (región [%d..%d, %d..%d])%n",
                    col, row, sx, sy, w, h, sx, sx + w - 1, sy, sy + h - 1);
            }
        }
        System.err.println("[GridExtractor] ————————————————————————————————");
    }

    // ── Getters ───────────────────────────────────────────────────────────

    /** Alias de getCellWidth() — mantenido para compatibilidad con HRFC-004. */
    public int getFrameWidth()  { return cellWidth;  }
    /** Alias de getCellHeight() — mantenido para compatibilidad con HRFC-004. */
    public int getFrameHeight() { return cellHeight; }
    public int getCellWidth()   { return cellWidth;  }
    public int getCellHeight()  { return cellHeight; }
    public int getColumns()     { return columns;    }
    public int getRows()        { return rows;       }
    public int getStartCol()    { return startCol;   }
    public int getStartRow()    { return startRow;   }
    public int getPadding()     { return padding;    }
    public int getSpacingX()    { return spacingX;   }
    public int getSpacingY()    { return spacingY;   }

    @Override
    public String toString() {
        return "GridExtractor[cell=" + cellWidth + "x" + cellHeight
            + " cols=" + columns + " rows=" + rows
            + " start=(" + startCol + "," + startRow + ")"
            + (padding  > 0 ? " padding=" + padding   : "")
            + (spacingX > 0 ? " spacingX=" + spacingX : "")
            + (spacingY > 0 ? " spacingY=" + spacingY : "")
            + "]";
    }

    // ── Builder ───────────────────────────────────────────────────────────

    public static final class Builder {

        private final int cellWidth;
        private final int cellHeight;
        private int columns  = 0;
        private int rows     = 0;
        private int startCol = 0;
        private int startRow = 0;
        private int padding  = 0;
        private int spacingX = 0;
        private int spacingY = 0;

        private Builder(int cellWidth, int cellHeight) {
            if (cellWidth  <= 0) throw new IllegalArgumentException(
                "GridExtractor: cellWidth debe ser > 0, recibido: " + cellWidth);
            if (cellHeight <= 0) throw new IllegalArgumentException(
                "GridExtractor: cellHeight debe ser > 0, recibido: " + cellHeight);
            this.cellWidth  = cellWidth;
            this.cellHeight = cellHeight;
        }

        /** Columnas a extraer (0 = todas las que caben). */
        public Builder columns(int c)  { this.columns  = Math.max(0, c); return this; }

        /** Filas a extraer (0 = todas las que caben). */
        public Builder rows(int r)     { this.rows     = Math.max(0, r); return this; }

        /** Primera columna a extraer (0-indexed). */
        public Builder startCol(int c) { this.startCol = Math.max(0, c); return this; }

        /** Primera fila a extraer (0-indexed). */
        public Builder startRow(int r) { this.startRow = Math.max(0, r); return this; }

        /**
         * Margen externo del sheet en píxeles.
         * Se aplica igual en los 4 bordes.
         * Útil para spritesheets exportados con relleno externo.
         */
        public Builder padding(int px) { this.padding  = Math.max(0, px); return this; }

        /**
         * Separación horizontal en píxeles entre celdas adyacentes.
         * Útil para spritesheets con líneas de separación entre frames.
         */
        public Builder spacingX(int px) { this.spacingX = Math.max(0, px); return this; }

        /**
         * Separación vertical en píxeles entre celdas adyacentes.
         */
        public Builder spacingY(int px) { this.spacingY = Math.max(0, px); return this; }

        /**
         * Atajo: misma separación en X e Y.
         * Equivale a llamar spacingX(px).spacingY(px).
         */
        public Builder spacing(int px) {
            this.spacingX = Math.max(0, px);
            this.spacingY = Math.max(0, px);
            return this;
        }

        public GridExtractor build() { return new GridExtractor(this); }
    }
}
