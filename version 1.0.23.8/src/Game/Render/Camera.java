package Game.Render;

import GameMath.Vector2D;

public class Camera {

    private Vector2D position = new Vector2D();

    public void centerOn(double x, double y, int screenWidth, int screenHeight) {
        position.setX(x - screenWidth / 2.0);
        position.setY(y - screenHeight / 2.0);
    }

    public double getX() { return position.getX(); }
    public double getY() { return position.getY(); }
}