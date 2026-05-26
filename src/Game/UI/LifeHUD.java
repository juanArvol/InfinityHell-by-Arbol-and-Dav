package Game.UI;

import java.awt.Color;
import java.awt.Graphics2D;

import Game.Player.PlayerStats;

/**
 * HUD de vida del jugador — refactorizado para coordenadas virtuales + UIAnchor.
 *
 * FIX REFACTOR DISPLAY:
 *  1. draw() recibe Graphics2D (framebuffer virtual), no Graphics.
 *     El framebuffer virtual siempre produce Graphics2D — el cast es seguro.
 *
 *  2. Ya NO usa ratios (0.02) multiplicados por dimensiones de pantalla real.
 *     En cambio usa UIAnchor.TOP_LEFT con offsets en píxeles virtuales.
 *     Resultado: la posición es consistente en cualquier resolución real.
 *
 *  3. onResize() recibe virtualWidth/virtualHeight (constantes de DisplaySettings),
 *     no las dimensiones del monitor. Se recalcula igualmente por robustez.
 *
 *  4. Restaura la animación de shrink del diseño original (displayedWidth),
 *     que había sido eliminada en la versión de ratios.
 */
public class LifeHUD implements UIElement {

    private static final int BAR_W    = 200;
    private static final int BAR_H    = 20;
    private static final int MARGIN_X = 15;
    private static final int MARGIN_Y = 15;

    private int x;
    private int y;

    private final PlayerStats stats;

    // Animación de shrink (restaurada del diseño original)
    private double displayedWidth = -1;
    private static final double SHRINK_SPEED = 6.0;

    public LifeHUD(PlayerStats stats, int virtualWidth, int virtualHeight) {
        this.stats = stats;
        recalcPosition(virtualWidth, virtualHeight);
    }

    @Override
    public void update() {
        int life = stats.getLife();
        int max  = stats.getLifeMax();

        double targetWidth = (max > 0) ? ((double) life / max) * BAR_W : 0;

        // Primera vez: inicializar sin animación
        if (displayedWidth < 0) {
            displayedWidth = targetWidth;
            return;
        }

        // Animar la barra hacia el target
        if (displayedWidth > targetWidth) {
            displayedWidth -= SHRINK_SPEED;
            if (displayedWidth < targetWidth) displayedWidth = targetWidth;
        } else if (displayedWidth < targetWidth) {
            displayedWidth += SHRINK_SPEED;
            if (displayedWidth > targetWidth) displayedWidth = targetWidth;
        }
    }

    @Override
    public void onResize(int virtualWidth, int virtualHeight) {
        recalcPosition(virtualWidth, virtualHeight);
    }

    @Override
    public void draw(Graphics2D g) {
        int life = stats.getLife();
        int max  = stats.getLifeMax();

        double percent = (max > 0) ? (life / (double) max) : 0.0;

        // Borde de la barra
        g.setColor(Color.BLACK);
        g.drawRect(x, y, BAR_W, BAR_H);

        // Relleno con color dinámico según % de vida
        if (percent > 0.6)      g.setColor(Color.GREEN);
        else if (percent > 0.3) g.setColor(Color.ORANGE);
        else                    g.setColor(Color.RED);

        g.fillRect(x + 1, y + 1, (int) displayedWidth, BAR_H - 1);

        // Texto HP
        g.setColor(Color.BLACK);
        g.drawString("HP: " + life + "/" + max, x, y + BAR_H + 15);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void recalcPosition(int vw, int vh) {
        x = UIAnchor.anchorX(UIAnchor.TOP_LEFT, BAR_W, vw, MARGIN_X);
        y = UIAnchor.anchorY(UIAnchor.TOP_LEFT, BAR_H, vh, MARGIN_Y);
    }
}
