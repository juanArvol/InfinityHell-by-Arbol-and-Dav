package Game.Gameplay.UI.Types;

import Game.Gameplay.UI.UIAnchor;
import Game.Gameplay.UI.UIElement;
import Game.Items.Types.Weapons.ModifiedWeapon;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import Game.Player.PlayerRuntime;
import Game.Player.PlayerState;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * HUD de munición — refactorizado para coordenadas virtuales + UIAnchor.
 *
 * ── HRFC — Player Architecture Consolidation ──────────────────────────────
 *
 * CAMBIO ARQUITECTÓNICO:
 *   - AmmoHUD ahora usa PlayerRuntime en lugar del legacy WeaponInventory
 *   - PlayerRuntime.getCurrentWeapon() proporciona el arma activa
 *   - PlayerState.isReloading() es la fuente de verdad del estado de recarga
 *   - weapon.isReloading() representa únicamente la mecánica interna del arma
 *
 * ARQUITECTURA CORRECTA:
 *   AmmoHUD → PlayerRuntime → getCurrentWeapon()
 *   AmmoHUD → PlayerState → isReloading()
 *
 * CAMBIOS respecto al original:
 *
 *  1. Ya NO usa ratios (0.7, 0.02) multiplicados por dimensiones de pantalla.
 *     En cambio usa UIAnchor.TOP_RIGHT con offsets en píxeles virtuales.
 *     Resultado: la posición es siempre correcta independientemente de la
 *     resolución real del monitor.
 *
 *  2. draw() recibe Graphics2D (del framebuffer virtual), no Graphics.
 *
 *  3. onResize() recibe virtualWidth/virtualHeight — que son constantes —
 *     pero se recalcula igual para que el anchor sea robusto.
 *
 *  4. El elemento sigue siendo 140x12 px en espacio virtual.
 *     Escalar el HUD globalmente se hace ajustando DisplaySettings.uiScale
 *     (pendiente de implementar en ScalingManager si se quiere HUD overlay
 *     a escala diferente al mundo).
 */
public class AmmoHUD implements UIElement {

    private static final int ELEMENT_W  = 150;
    private static final int ELEMENT_H  = 120;  // FIX M-02: era 110, pero el texto llega
                                                  // a y+105 en baseline; con descenders
                                                  // reales se cortaba. 120 da margen seguro.
    private static final int MARGIN_X   = 10;
    private static final int MARGIN_Y   = 10;

    private int x;
    private int y;

    private final PlayerRuntime runtime;
    private final PlayerState playerState;

    /**
     * Constructor actualizado que usa PlayerRuntime.
     * 
     * @param runtime runtime del Player para obtener arma activa
     * @param playerState estado del Player para consultar recarga
     * @param virtualWidth ancho virtual
     * @param virtualHeight alto virtual
     */
    public AmmoHUD(PlayerRuntime runtime, PlayerState playerState, int virtualWidth, int virtualHeight) {
        this.runtime = runtime;
        this.playerState = playerState;
        recalcPosition(virtualWidth, virtualHeight);
    }

    @Override
    public void update() {}

    @Override
    public void onResize(int virtualWidth, int virtualHeight) {
        recalcPosition(virtualWidth, virtualHeight);
    }

    @Override
    public void draw(Graphics2D g) {
        ModifiedWeapon weapon = runtime.getCurrentWeapon();
        if (weapon == null) return;

        WeaponStats stats = weapon.getStats();

        // ── COOLDOWN BAR ──────────────────────────────────────────────────────
        // FIX M-02: antes usaba int width = 140 mientras ELEMENT_W = 150.
        // La barra declaraba un elemento de 150px pero dibujaba solo 140px.
        int width  = ELEMENT_W;
        int height = 12;

        // ── COOLDOWN BAR ──────────────────────────────────────────────────────
        int cooldown = weapon.getCooldown();
        int fireWait = weapon.getFireWait();

        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);

        double percent = cooldown > 0 ? 1.0 - (fireWait / (double) cooldown) : 1.0;

        // ── HRFC: Consultar PlayerState para recarga ─────────────────────────
        boolean isReloading = (playerState != null) ? playerState.isReloading() : weapon.isReloading();
        
        if (isReloading)
            g.setColor(Color.YELLOW);
        else if (fireWait == 0)
            g.setColor(Color.GREEN);
        else
            g.setColor(Color.RED);

        g.fillRect(x + 1, y + 1, (int)(percent * width), height - 1);

        // ── TEXTO ─────────────────────────────────────────────────────────────
        g.setColor(Color.BLACK);
        g.drawString("Ammo: "    + weapon.getCurrentAmmo() + "/" + weapon.getMaxAmmo(), x, y + 30);
        if (isReloading) g.drawString("RELOADING", x, y + 45);
        g.drawString("Damage: "  + stats.getDamageBonusByWeapon(),  x, y + 60);
        g.drawString("Speed: "   + stats.getBulletSpeedBase(),      x, y + 75);
        g.drawString("Pellets: " + stats.getBulletsPerShot(),        x, y + 90);
        g.drawString("Spread: "  + (int) stats.getSpread(),          x, y + 105);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void recalcPosition(int vw, int vh) {
        x = UIAnchor.anchorX(UIAnchor.TOP_RIGHT, ELEMENT_W, vw, MARGIN_X);
        y = UIAnchor.anchorY(UIAnchor.TOP_RIGHT, ELEMENT_H, vh, MARGIN_Y);
    }
}
