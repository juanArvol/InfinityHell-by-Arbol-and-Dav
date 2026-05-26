package Game.Render;

import GameMath.Vector2D;

/**
 * Cámara del juego.
 *
 * FIX DESIGN-009: en el original no había clamp de límites.
 * Si el jugador estaba en el borde del mundo, la cámara mostraba
 * área vacía fuera del mundo.
 *
 * Ahora centerOn() acepta límites opcionales del mundo para clampear.
 */
public class Camera {

    private Vector2D position = new Vector2D();

    /** Centra la cámara en (x, y) sin límites (comportamiento original). */
    public void centerOn(double x, double y, int screenWidth, int screenHeight) {
        position.setX(x - screenWidth / 2.0);
        position.setY(y - screenHeight / 2.0);
    }

    /**
     * Centra la cámara en (x, y) con clamp al área del mundo.
     * FIX DESIGN-009: sin esto, al acercarse a los bordes se ve fuera del mundo.
     *
     * @param worldWidth  ancho total del mundo en píxeles
     * @param worldHeight alto total del mundo en píxeles
     */
    public void centerOn(double x, double y,
                         int screenWidth, int screenHeight,
                         int worldWidth, int worldHeight) {

        double camX = x - screenWidth / 2.0;
        double camY = y - screenHeight / 2.0;

        // Clamp horizontal
        camX = Math.max(0, Math.min(camX, worldWidth - screenWidth));
        // Clamp vertical
        camY = Math.max(0, Math.min(camY, worldHeight - screenHeight));

        position.setX(camX);
        position.setY(camY);
    }

    public double getX() { return position.getX(); }
    public double getY() { return position.getY(); }
}
