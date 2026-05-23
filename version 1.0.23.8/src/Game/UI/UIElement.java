package Game.UI;

import java.awt.Graphics;

public interface UIElement {
    void update();
    void draw(Graphics g);
    // NUEVO: permitir que el elemento se ajuste al redimensionar
    void onResize(int screenWidth, int screenHeight);
}