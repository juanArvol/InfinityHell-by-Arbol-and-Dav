package Game.UI;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

public class UIManager {

    private final List<UIElement> elements = new ArrayList<>();

    public void add(UIElement element) {
        elements.add(element);
    }

    public void update() {
        for (UIElement e : elements) e.update();
    }

    public void draw(Graphics g) {
        for (UIElement e : elements) e.draw(g);
    }
    
    public void updateUI(int screenWidth, int screenHeight) {
        for (UIElement e : elements) {
            e.onResize(screenWidth, screenHeight);
        }
    }
}