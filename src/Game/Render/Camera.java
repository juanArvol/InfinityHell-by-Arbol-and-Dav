package Game.Render;

import GameMath.Vector2D;

/**
 * Cámara del juego — refactorizada para usar solo coordenadas virtuales.
 *
 * CAMBIO ARQUITECTURAL: la cámara ya NO recibe screenWidth/screenHeight reales.
 * Usa exclusivamente VIRTUAL_WIDTH y VIRTUAL_HEIGHT.
 *
 * Por qué: si la cámara usara dimensiones reales del monitor, la cantidad de
 * mundo visible cambiaría según la resolución (un monitor 4K mostraría más
 * mundo que uno 720p). Eso rompe el diseño de niveles y el balance del juego.
 *
 * Con resolución virtual fija:
 *  - Todos los jugadores ven exactamente el mismo área de mundo
 *  - El diseño de niveles es consistente
 *  - Las físicas y hitboxes son correctas a cualquier resolución real
 *
 * INTEGRACIÓN CON DISPLAY:
 * La transformación virtual → pantalla la hace ScalingManager al presentar
 * el framebuffer. La cámara no sabe nada de eso.
 */
public class Camera {

    private final Vector2D position = new Vector2D();

    /**
     * Centra la cámara en (x, y) usando dimensiones virtuales.
     *
     * @param x             posición X del objetivo (en coordenadas virtuales)
     * @param y             posición Y del objetivo (en coordenadas virtuales)
     * @param virtualWidth  siempre DisplaySettings.virtualWidth
     * @param virtualHeight siempre DisplaySettings.virtualHeight
     */
    public void centerOn(double x, double y, int virtualWidth, int virtualHeight) {
        position.setX(x - virtualWidth  / 2.0);
        position.setY(y - virtualHeight / 2.0);
    }

    /**
     * Centra la cámara con clamp a los límites del mundo.
     *
     * @param x             posición X del objetivo (coordenadas virtuales)
     * @param y             posición Y del objetivo (coordenadas virtuales)
     * @param virtualWidth  siempre DisplaySettings.virtualWidth
     * @param virtualHeight siempre DisplaySettings.virtualHeight
     * @param worldWidth    ancho total del mundo en unidades virtuales
     * @param worldHeight   alto total del mundo en unidades virtuales
     */
    public void centerOn(double x, double y,
                         int virtualWidth, int virtualHeight,
                         int worldWidth, int worldHeight) {

        double camX = x - virtualWidth  / 2.0;
        double camY = y - virtualHeight / 2.0;

        // Clamp: la cámara no sale del mundo
        camX = Math.max(0, Math.min(camX, worldWidth  - virtualWidth));
        camY = Math.max(0, Math.min(camY, worldHeight - virtualHeight));

        position.setX(camX);
        position.setY(camY);
    }

    /**
     * Offset de cámara en X.
     * Restar este valor a la posición de un objeto para obtener su posición en pantalla virtual.
     *
     * Ejemplo: screenX = obj.getX() - camera.getX()
     */
    public double getX() { return position.getX(); }

    /**
     * Offset de cámara en Y.
     */
    public double getY() { return position.getY(); }

    /** Posición completa como Vector2D (solo lectura — no modificar el retorno). */
    public Vector2D getPosition() { return position; }
}
