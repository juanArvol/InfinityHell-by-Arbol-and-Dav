package Game.UI;

/**
 * Puntos de anclaje para elementos de UI.
 *
 * La UI usa coordenadas VIRTUALES (0..VIRTUAL_WIDTH, 0..VIRTUAL_HEIGHT).
 * Los anchors permiten posicionar elementos relativos a los bordes de
 * la pantalla virtual sin hardcodear coordenadas absolutas.
 *
 * Uso:
 *   // Elemento anclado a esquina inferior derecha virtual:
 *   int x = UIAnchor.anchorX(UIAnchor.BOTTOM_RIGHT, elementWidth,  VIRTUAL_WIDTH);
 *   int y = UIAnchor.anchorY(UIAnchor.BOTTOM_RIGHT, elementHeight, VIRTUAL_HEIGHT);
 */
public enum UIAnchor {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT;

    /**
     * Calcula la coordenada X base para un anchor dado.
     *
     * @param anchor        el anchor deseado
     * @param elementWidth  ancho del elemento en píxeles virtuales
     * @param virtualWidth  ancho de la pantalla virtual
     * @param marginX       margen horizontal desde el borde (en px virtuales)
     * @return X en coordenadas virtuales
     */
    public static int anchorX(UIAnchor anchor, int elementWidth,
                               int virtualWidth, int marginX) {
        return switch (anchor) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT       ->  marginX;
            case TOP_CENTER, CENTER, BOTTOM_CENTER        -> (virtualWidth - elementWidth) / 2;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT    ->  virtualWidth - elementWidth - marginX;
        };
    }

    /**
     * Calcula la coordenada Y base para un anchor dado.
     *
     * @param anchor        el anchor deseado
     * @param elementHeight alto del elemento en píxeles virtuales
     * @param virtualHeight alto de la pantalla virtual
     * @param marginY       margen vertical desde el borde (en px virtuales)
     * @return Y en coordenadas virtuales
     */
    public static int anchorY(UIAnchor anchor, int elementHeight,
                               int virtualHeight, int marginY) {
        return switch (anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT          ->  marginY;
            case CENTER_LEFT, CENTER, CENTER_RIGHT        -> (virtualHeight - elementHeight) / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT ->  virtualHeight - elementHeight - marginY;
        };
    }

    // Sobrecarga sin margen (margin = 0)
    public static int anchorX(UIAnchor anchor, int elementWidth,  int virtualWidth)  {
        return anchorX(anchor, elementWidth,  virtualWidth,  0);
    }
    public static int anchorY(UIAnchor anchor, int elementHeight, int virtualHeight) {
        return anchorY(anchor, elementHeight, virtualHeight, 0);
    }
}
