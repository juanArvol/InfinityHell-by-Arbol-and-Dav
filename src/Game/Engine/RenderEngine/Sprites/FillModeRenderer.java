package Game.Engine.RenderEngine.Sprites;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * FillModeRenderer — aplica FillMode y Alignment en un Graphics2D.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Contiene toda la lógica de cálculo y dibujo para cada FillMode.
 * SpriteRenderer delega aquí cuando fillMode != STRETCH (el modo legacy).
 * No depende de SpriteFrame ni de TransformData — solo de BufferedImage y
 * coordenadas. Stateless, todos los métodos son estáticos.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 * SpriteRenderer  → decide cuándo llamar FillModeRenderer
 * FillModeRenderer → calcula geometría y dibuja
 * SpriteDrawer    → sigue siendo la única fuente de verdad para el render
 *                   con TransformData. FillModeRenderer opera sobre la imagen
 *                   ya resuelta (BufferedImage), antes de que TransformData
 *                   sea necesario, o cuando FillMode requiere múltiples draws.
 *
 * ── NOTA SOBRE TILE Y SPRITEDRAWER ───────────────────────────────────────
 * Los modos TILE/TILE_X/TILE_Y requieren múltiples drawImage() para
 * llenar el área. Esto es una excepción justificada al principio de "único
 * drawImage por sprite": el sprite individual sí se dibuja una sola vez por
 * celda; lo que se repite es la celda, no el pipeline de transformación.
 * TransformData se aplica a cada celda individualmente.
 */
public final class FillModeRenderer {

    private FillModeRenderer() {}

    // ── API principal ─────────────────────────────────────────────────────

    /**
     * Dibuja {@code img} en el área (areaX, areaY, areaW, areaH) aplicando
     * el FillMode y Alignment indicados.
     *
     * @param g       Graphics2D sobre el que dibujar (no disponer)
     * @param img     imagen del frame a dibujar
     * @param areaX   coordenada X del área de destino en pantalla
     * @param areaY   coordenada Y del área de destino en pantalla
     * @param areaW   ancho del área de destino en píxeles
     * @param areaH   alto del área de destino en píxeles
     * @param mode    FillMode a aplicar
     * @param alignH  alineación horizontal
     * @param alignV  alineación vertical
     */
    public static void draw(Graphics2D g,
                            BufferedImage img,
                            int areaX, int areaY,
                            int areaW, int areaH,
                            FillMode mode,
                            Alignment alignH,
                            Alignment alignV) {

        if (img == null || areaW <= 0 || areaH <= 0) return;

        switch (mode) {
            case STRETCH -> drawStretch(g, img, areaX, areaY, areaW, areaH);
            case FIT     -> drawFit    (g, img, areaX, areaY, areaW, areaH, alignH, alignV);
            case COVER   -> drawCover  (g, img, areaX, areaY, areaW, areaH, alignH, alignV);
            case CENTER  -> drawCenter (g, img, areaX, areaY, areaW, areaH, alignH, alignV);
            case TILE    -> drawTile   (g, img, areaX, areaY, areaW, areaH, alignH, alignV, true,  true);
            case TILE_X  -> drawTile   (g, img, areaX, areaY, areaW, areaH, alignH, alignV, true,  false);
            case TILE_Y  -> drawTile   (g, img, areaX, areaY, areaW, areaH, alignH, alignV, false, true);
        }
    }

    // ── STRETCH ───────────────────────────────────────────────────────────

    private static void drawStretch(Graphics2D g, BufferedImage img,
                                    int ax, int ay, int aw, int ah) {
        g.drawImage(img, ax, ay, aw, ah, null);
    }

    // ── FIT ───────────────────────────────────────────────────────────────

    private static void drawFit(Graphics2D g, BufferedImage img,
                                int ax, int ay, int aw, int ah,
                                Alignment alignH, Alignment alignV) {
        int iw = img.getWidth();
        int ih = img.getHeight();
        if (iw <= 0 || ih <= 0) return;

        float scaleX = (float) aw / iw;
        float scaleY = (float) ah / ih;
        float scale  = Math.min(scaleX, scaleY);   // escala que hace FIT sin recorte

        int drawW = Math.round(iw * scale);
        int drawH = Math.round(ih * scale);

        int drawX = ax + offsetFor(alignH, aw, drawW);
        int drawY = ay + offsetFor(alignV, ah, drawH);

        g.drawImage(img, drawX, drawY, drawW, drawH, null);
    }

    // ── COVER ─────────────────────────────────────────────────────────────

    private static void drawCover(Graphics2D g, BufferedImage img,
                                  int ax, int ay, int aw, int ah,
                                  Alignment alignH, Alignment alignV) {
        int iw = img.getWidth();
        int ih = img.getHeight();
        if (iw <= 0 || ih <= 0) return;

        float scaleX = (float) aw / iw;
        float scaleY = (float) ah / ih;
        float scale  = Math.max(scaleX, scaleY);   // escala que garantiza cobertura total

        int drawW = Math.round(iw * scale);
        int drawH = Math.round(ih * scale);

        // El exceso se recorta; el clip limita el dibujo al área
        int drawX = ax + offsetFor(alignH, aw, drawW);
        int drawY = ay + offsetFor(alignV, ah, drawH);

        var savedClip = g.getClip();
        g.setClip(ax, ay, aw, ah);
        g.drawImage(img, drawX, drawY, drawW, drawH, null);
        g.setClip(savedClip);
    }

    // ── CENTER ────────────────────────────────────────────────────────────

    private static void drawCenter(Graphics2D g, BufferedImage img,
                                   int ax, int ay, int aw, int ah,
                                   Alignment alignH, Alignment alignV) {
        int iw = img.getWidth();
        int ih = img.getHeight();

        int drawX = ax + offsetFor(alignH, aw, iw);
        int drawY = ay + offsetFor(alignV, ah, ih);

        // Aplicar clip para no pintar fuera del área si el sprite es mayor
        var savedClip = g.getClip();
        g.setClip(ax, ay, aw, ah);
        g.drawImage(img, drawX, drawY, null);
        g.setClip(savedClip);
    }

    // ── TILE ──────────────────────────────────────────────────────────────

    /**
     * Rellena el área repitiendo la imagen.
     *
     * @param tileX si true, repite en el eje horizontal; si false, estira en X
     * @param tileY si true, repite en el eje vertical;   si false, estira en Y
     */
    private static void drawTile(Graphics2D g, BufferedImage img,
                                 int ax, int ay, int aw, int ah,
                                 Alignment alignH, Alignment alignV,
                                 boolean tileX, boolean tileY) {
        int iw = img.getWidth();
        int ih = img.getHeight();
        if (iw <= 0 || ih <= 0) return;

        // Dimensión efectiva de cada celda
        int cellW = tileX ? iw : aw;
        int cellH = tileY ? ih : ah;

        // Cuántas repeticiones caben en cada eje
        int repsX = tileX ? (int) Math.ceil((double) aw / cellW) : 1;
        int repsY = tileY ? (int) Math.ceil((double) ah / cellH) : 1;

        // Tamaño total de la banda de tiles
        int totalW = cellW * repsX;
        int totalH = cellH * repsY;

        // Offset de alineación para centrar/end la banda dentro del área
        int startX = ax + offsetFor(alignH, aw, totalW);
        int startY = ay + offsetFor(alignV, ah, totalH);

        // Clip al área para recortar las celdas que se salen
        var savedClip = g.getClip();
        g.setClip(ax, ay, aw, ah);

        for (int row = 0; row < repsY; row++) {
            for (int col = 0; col < repsX; col++) {
                int cx = startX + col * cellW;
                int cy = startY + row * cellH;
                g.drawImage(img, cx, cy, cellW, cellH, null);
            }
        }

        g.setClip(savedClip);
    }

    // ── Utilidad ──────────────────────────────────────────────────────────

    /**
     * Calcula el offset del contenido respecto al área según la alineación.
     *
     * @param align    alineación deseada
     * @param areaSize tamaño del área (ancho o alto)
     * @param contSize tamaño del contenido (ancho o alto)
     * @return offset en píxeles desde el borde inicio del área
     */
    static int offsetFor(Alignment align, int areaSize, int contSize) {
        return switch (align) {
            case START  -> 0;
            case CENTER -> (areaSize - contSize) / 2;
            case END    -> areaSize - contSize;
        };
    }
}
