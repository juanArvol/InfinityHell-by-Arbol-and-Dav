package Game.UI.Types;

import Game.UI.HealthView;
import Game.UI.UIAnchor;
import Game.UI.UIElement;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * HUD de vida — muestra la salud de cualquier entidad que implemente HealthView.
 *
 * ── CONTRATO RESTAURADO ───────────────────────────────────────────────────
 *
 * El refactor anterior hizo que LifeHUD recibiera HealthComponent directamente,
 * rompiendo dos cosas:
 *
 *   (a) La cadena de dependencia deseada:
 *       LifeHUD → PlayerStats → HealthComponent
 *
 *   (b) La extensibilidad hacia futuros HUDs:
 *       EnemyLifeHUD, BossLifeHUD también necesitarían mostrar vida,
 *       pero no todos los portadores tienen HealthComponent —
 *       o pueden querer exponerlo de forma distinta (vida por fase, etc.)
 *
 * SOLUCIÓN:
 *   LifeHUD depende de HealthView, la interfaz de solo lectura definida en
 *   Game.UI.HealthView. PlayerStats la implementa delegando en HealthComponent.
 *   Enemies, Bosses y cualquier otro tipo solo necesitan implementar
 *   HealthView para que este HUD los soporte sin modificación.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Player (UIBootstrap):
 *   new LifeHUD(player.getPlayerStats(), virtualWidth, virtualHeight)
 *                ↑ PlayerStats implements HealthView
 *
 *   // Futuro Enemy:
 *   new LifeHUD(enemy, virtualWidth, virtualHeight)
 *                ↑ Enemy implements HealthView
 *
 *   // Futuro Boss (vida de fase):
 *   new LifeHUD(boss.getCurrentPhaseHealth(), virtualWidth, virtualHeight)
 *                ↑ BossPhaseHealth implements HealthView
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 *  - Coordenadas virtuales + UIAnchor.TOP_LEFT.
 *  - Animación de shrink sobre displayedWidth.
 *  - Color dinámico según porcentaje de vida.
 */
public class LifeHUD implements UIElement {

    private static final int BAR_W    = 200;
    private static final int BAR_H    = 20;
    private static final int MARGIN_X = 15;
    private static final int MARGIN_Y = 15;

    private int x;
    private int y;

    private final HealthView healthView;

    // Animación de shrink (restaurada del diseño original)
    private double displayedWidth = -1;
    private static final double SHRINK_SPEED = 6.0;

    /**
     * @param healthView    proveedor de datos de salud (player.getStats(), enemy, boss...)
     * @param virtualWidth  DisplaySettings.virtualWidth
     * @param virtualHeight DisplaySettings.virtualHeight
     */
    public LifeHUD(HealthView healthView, int virtualWidth, int virtualHeight) {
        this.healthView = healthView;
        recalcPosition(virtualWidth, virtualHeight);
    }

    @Override
    public void update() {
        int life = healthView.getLife();
        int max  = healthView.getLifeMax();

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
        int life       = healthView.getLife();
        int max        = healthView.getLifeMax();
        double percent = healthView.getLifePercent();

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
