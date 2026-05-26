package Game.UI;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de elementos de UI — refactorizado para coordenadas virtuales.
 *
 * CAMBIOS:
 *  - draw() recibe Graphics2D (framebuffer virtual)
 *  - onResize() recibe virtualWidth/virtualHeight, no screen reales
 *  - Se llama onResize() al agregar un elemento (para posición inicial correcta)
 */
public class UIManager {

    private final List<UIElement> elements = new ArrayList<>();

    private int currentVirtualWidth;
    private int currentVirtualHeight;

    public UIManager(int virtualWidth, int virtualHeight) {
        this.currentVirtualWidth  = virtualWidth;
        this.currentVirtualHeight = virtualHeight;
    }

    /**
     * Agrega un elemento y lo inicializa con la resolución virtual actual.
     */
    public void add(UIElement element) {
        element.onResize(currentVirtualWidth, currentVirtualHeight);
        elements.add(element);
    }

    public void remove(UIElement element) {
        elements.remove(element);
    }

    public void update() {
        for (UIElement e : elements) e.update();
    }

    /**
     * Dibuja todos los elementos en coordenadas virtuales.
     *
     * @param g Graphics2D del framebuffer virtual — ya en espacio virtual.
     */
    public void draw(Graphics2D g) {
        for (UIElement e : elements) e.draw(g);
    }

    /**
     * Notifica a todos los elementos que recalculen sus posiciones.
     *
     * En el nuevo sistema virtual, virtualWidth y virtualHeight son constantes,
     * pero se mantiene este método para cuando en el futuro se quiera
     * soportar cambio de resolución virtual en runtime.
     *
     * @param virtualWidth  DisplaySettings.virtualWidth
     * @param virtualHeight DisplaySettings.virtualHeight
     */
    public void onResize(int virtualWidth, int virtualHeight) {
        this.currentVirtualWidth  = virtualWidth;
        this.currentVirtualHeight = virtualHeight;
        for (UIElement e : elements) {
            e.onResize(virtualWidth, virtualHeight);
        }
    }
}
