package Main.Debug;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Overlay de FPS.
 *
 * Responsabilidad única: saber cómo dibujarse a sí mismo dado un valor de FPS.
 *
 * GameState antes conocía colores, fuentes y RenderingHints para este overlay.
 * Ese conocimiento no pertenece a la fachada del estado de juego: es un
 * detalle de presentación de debug que puede cambiar sin afectar el gameplay.
 *
 * Uso:
 *   FpsOverlay overlay = new FpsOverlay();
 *   overlay.draw(g, fpsPorSegundo);
 *
 * La posición (6, 14) está hardcodeada intencionalmente: el overlay de FPS
 * siempre va en la esquina superior izquierda. Si en el futuro se necesita
 * configurar posición, se añade un constructor con (x, y) sin romper nada.
 */
public final class FpsOverlay {

    private static final Font  FONT   = new Font("Monospaced", Font.BOLD, 12);
    private static final Color COLOR  = new Color(255, 255, 0, 220);
    private static final Color SHADOW = new Color(0, 0, 0, 160);

    public void draw(Graphics2D g, int fps) {
        String text      = "FPS: " + fps;
        Font   prevFont  = g.getFont();
        Color  prevColor = g.getColor();
        Object prevAA    = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);

        g.setFont(FONT);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(SHADOW);
        g.drawString(text, 7, 15);
        g.setColor(COLOR);
        g.drawString(text, 6, 14);

        g.setFont(prevFont);
        g.setColor(prevColor);
        if (prevAA != null) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, prevAA);
        }
    }
}
