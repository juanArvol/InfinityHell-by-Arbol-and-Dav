package Display.Background;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Fondo de color sólido configurable.
 *
 * Implementación por defecto de {@link DisplayBackground}.
 * Limpia el framebuffer con un único color opaco antes de cada frame.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * USO
 *
 *   // Negro puro (equivalente al comportamiento original hardcodeado)
 *   DisplayBackground bg = SolidColorBackground.BLACK;
 *
 *   // Azul noche personalizado
 *   DisplayBackground bg = new SolidColorBackground(new Color(15, 15, 35));
 *
 *   // Cambiar en runtime (e.g. al entrar en una zona especial)
 *   display.setBackground(new SolidColorBackground(new Color(100, 20, 20)));
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CONSTANTES
 *
 *   SolidColorBackground.BLACK   → Color.BLACK (estándar, la más común)
 *   SolidColorBackground.WHITE   → Color.WHITE
 *   SolidColorBackground.TRANSPARENT → Color(0,0,0,0) para compositing
 */
public final class SolidColorBackground implements DisplayBackground {

    /** Fondo negro. Equivalente al comportamiento original hardcodeado. */
    public static final SolidColorBackground BLACK       = new SolidColorBackground(Color.BLACK);

    /** Fondo blanco. */
    public static final SolidColorBackground WHITE       = new SolidColorBackground(Color.WHITE);

    /** Fondo transparente. Útil cuando el frame se compone sobre otro contexto. */
    public static final SolidColorBackground TRANSPARENT = new SolidColorBackground(new Color(0, 0, 0, 0));

    private final Color color;

    /**
     * @param color color con el que limpiar el framebuffer. No puede ser null.
     */
    public SolidColorBackground(Color color) {
        if (color == null) throw new IllegalArgumentException("color cannot be null");
        this.color = color;
    }

    /** Devuelve el color configurado. */
    public Color getColor() { return color; }

    @Override
    public void apply(Graphics2D g, int virtualWidth, int virtualHeight) {
        g.setBackground(color);
        g.clearRect(0, 0, virtualWidth, virtualHeight);
    }

    @Override
    public String toString() {
        return "SolidColorBackground[" + color + "]";
    }
}
