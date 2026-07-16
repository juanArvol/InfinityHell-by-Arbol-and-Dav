package Sprites.Core.Extractors;

import Sprites.Core.SpriteExtractor;
import Sprites.Core.SpriteFrame;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * AutoRegionExtractor — extracción automática por regiones conectadas.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Detecta automáticamente regiones de píxeles no transparentes mediante un
 * flood-fill BFS y calcula el bounding box de cada región.
 *
 * No depende de ninguna cuadrícula. Funciona con cualquier spritesheet
 * artesanal donde los sprites estén separados por cualquier cantidad de
 * transparencia.
 *
 * ── ALGORITMO ─────────────────────────────────────────────────────────────
 * 1. Escanear la imagen de izquierda a derecha, arriba a abajo.
 * 2. Al encontrar un píxel no-transparente no visitado, iniciar BFS.
 * 3. El BFS expande a los 4 vecinos ortogonales (o 8 si diagonals=true).
 * 4. Marcar todos los píxeles visitados para evitar procesarlos de nuevo.
 * 5. Al terminar el BFS, calcular el bounding box de la región.
 * 6. Copiar la sub-región a una nueva BufferedImage (frame aislado).
 * 7. Repetir hasta procesar toda la imagen.
 *
 * ── ORDEN DE FRAMES (contrato determinista) ──────────────────────────────
 * Los frames se producen en el orden de descubrimiento del PRIMER píxel
 * opaco de cada región durante el escaneo. El escaneo es estrictamente
 * row-major: fila 0 completa (izquierda→derecha), luego fila 1, etc.
 *
 *   Criterio de orden: (firstPixelY × imageWidth + firstPixelX) creciente.
 *
 * Dos ejecuciones sobre el mismo SpriteSheet con los mismos parámetros
 * producen siempre exactamente el mismo orden de frames.
 * No depende de HashMap ni de ninguna estructura con orden no determinista.
 * El orden es estable e independiente de la JVM o plataforma.
 *
 * Para spritesheets en fila horizontal esto produce el orden natural
 * (izquierda→derecha). Para layouts en cuadrícula produce orden fila a fila.
 *
 * ── PARÁMETROS ────────────────────────────────────────────────────────────
 * alphaThreshold  → umbral de alpha [0..255] para considerar un píxel opaco
 *                   Default: 10 (ignora píxeles casi transparentes)
 * padding         → píxeles extra alrededor de cada bounding box en el frame
 *                   Default: 0
 * diagonals       → si true, conectividad 8; si false, conectividad 4
 *                   Default: false (4-conectividad, más conservadora)
 *
 * ── NO REEMPLAZA GridExtractor ────────────────────────────────────────────
 * Ambos extractores coexisten. El desarrollador elige el adecuado.
 * Para hojas perfectamente alineadas, GridExtractor es más rápido.
 * AutoRegionExtractor es para hojas artesanales o de tamaño variable.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   SpriteSheet sheet = SpriteSheet.load(image, SpriteExtractors.autoRegion());
 *
 *   // Con configuración personalizada:
 *   SpriteSheet sheet = SpriteSheet.load(image,
 *       AutoRegionExtractor.builder().alphaThreshold(20).padding(2).build());
 */
public final class AutoRegionExtractor implements SpriteExtractor {

    /** Alpha mínimo para que un píxel se considere parte de un sprite. */
    private final int alphaThreshold;

    /** Píxeles de padding alrededor del bounding box de cada región. */
    private final int padding;

    /** Si true, conectividad 8 (incluye diagonales). */
    private final boolean diagonals;

    // ── Constructor ──────────────────────────────────────────────────────

    private AutoRegionExtractor(Builder b) {
        this.alphaThreshold = b.alphaThreshold;
        this.padding        = b.padding;
        this.diagonals      = b.diagonals;
    }

    // ── Fábricas ─────────────────────────────────────────────────────────

    /** Extractor con configuración por defecto. */
    public static AutoRegionExtractor withDefaults() {
        return new Builder().build();
    }

    /** Builder para configuración avanzada. */
    public static Builder builder() {
        return new Builder();
    }

    // ── Extracción ────────────────────────────────────────────────────────

    @Override
    public List<SpriteFrame> extract(BufferedImage source) {
        if (source == null) return List.of();

        int w = source.getWidth();
        int h = source.getHeight();

        // Mapa de píxeles visitados (evita re-procesar píxeles ya asignados a una región)
        boolean[][] visited = new boolean[h][w];

        List<SpriteFrame> frames = new ArrayList<>();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!visited[y][x] && isOpaque(source, x, y)) {
                    // Nuevo píxel opaco no visitado — encontramos una nueva región
                    RegionBounds bounds = floodFill(source, visited, x, y, w, h);
                    SpriteFrame  frame  = cropRegion(source, bounds);
                    if (frame.isValid()) {
                        frames.add(frame);
                    }
                }
            }
        }

        return frames;
    }

    // ── Flood fill BFS ────────────────────────────────────────────────────

    /**
     * BFS desde (startX, startY) para identificar todos los píxeles de la región.
     * Retorna el bounding box de la región encontrada.
     */
    private RegionBounds floodFill(BufferedImage source,
                                   boolean[][] visited,
                                   int startX, int startY,
                                   int w, int h) {

        int minX = startX, maxX = startX;
        int minY = startY, maxY = startY;

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;

        // Vecinos: 4-conectividad o 8-conectividad
        int[][] neighbors = diagonals
            ? new int[][]{{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{1,-1},{-1,1},{1,1}}
            : new int[][]{{-1,0},{1,0},{0,-1},{0,1}};

        while (!queue.isEmpty()) {
            int[] pixel = queue.poll();
            int px = pixel[0];
            int py = pixel[1];

            // Expandir bounding box
            if (px < minX) minX = px;
            if (px > maxX) maxX = px;
            if (py < minY) minY = py;
            if (py > maxY) maxY = py;

            for (int[] d : neighbors) {
                int nx = px + d[0];
                int ny = py + d[1];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h
                        && !visited[ny][nx] && isOpaque(source, nx, ny)) {
                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        return new RegionBounds(minX, minY, maxX, maxY);
    }

    /**
     * Recorta la región del source y crea un frame aislado con padding opcional.
     *
     * La copia es independiente del raster fuente (misma técnica que GridExtractor)
     * para evitar cualquier bleeding de píxeles adyacentes.
     */
    private SpriteFrame cropRegion(BufferedImage source, RegionBounds b) {
        int x = Math.max(0, b.minX - padding);
        int y = Math.max(0, b.minY - padding);
        int x2 = Math.min(source.getWidth(),  b.maxX + 1 + padding);
        int y2 = Math.min(source.getHeight(), b.maxY + 1 + padding);
        int w  = x2 - x;
        int h  = y2 - y;

        if (w <= 0 || h <= 0) return SpriteFrame.empty();

        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = copy.createGraphics();
        try {
            g.drawImage(source, 0, 0, w, h, x, y, x2, y2, null);
        } finally {
            g.dispose();
        }

        return new SpriteFrame(copy);
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    /** true si el píxel en (x, y) supera el umbral de alpha. */
    private boolean isOpaque(BufferedImage source, int x, int y) {
        int rgba  = source.getRGB(x, y);
        int alpha = (rgba >>> 24) & 0xFF;
        return alpha >= alphaThreshold;
    }

    // ── Tipos internos ────────────────────────────────────────────────────

    /** Bounding box de una región detectada. */
    private record RegionBounds(int minX, int minY, int maxX, int maxY) {}

    // ── Getters ───────────────────────────────────────────────────────────

    public int     getAlphaThreshold() { return alphaThreshold; }
    public int     getPadding()        { return padding;        }
    public boolean isDiagonals()       { return diagonals;      }

    @Override
    public String toString() {
        return "AutoRegionExtractor[threshold=" + alphaThreshold
               + " padding=" + padding
               + " diagonals=" + diagonals + "]";
    }

    // ── Builder ───────────────────────────────────────────────────────────

    public static final class Builder {
        private int     alphaThreshold = 10;
        private int     padding        = 0;
        private boolean diagonals      = false;

        private Builder() {}

        /**
         * Umbral de alpha [0..255] para considerar opaco un píxel.
         * Default: 10 (ignora artefactos casi invisibles).
         */
        public Builder alphaThreshold(int threshold) {
            this.alphaThreshold = Math.max(0, Math.min(255, threshold));
            return this;
        }

        /**
         * Padding en píxeles alrededor de cada bounding box detectado.
         * Útil cuando el sprite incluye bordes brillantes o glows.
         */
        public Builder padding(int px) {
            this.padding = Math.max(0, px);
            return this;
        }

        /**
         * Si true, usa conectividad 8 (incluye diagonales).
         * Default: false (conectividad 4, más conservadora).
         * Usar true cuando sprites en diagonal deben considerarse una misma región.
         */
        public Builder diagonals(boolean d) {
            this.diagonals = d;
            return this;
        }

        public AutoRegionExtractor build() { return new AutoRegionExtractor(this); }
    }
}
