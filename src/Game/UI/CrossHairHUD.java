package Game.UI;

import java.awt.Color;
import java.awt.Graphics2D;

import Entradas.MouseInput;
import Game.Player.Player;
import Game.Weapons.WeaponSelected;
import Game.Bullets.BulletFactory;
import Game.Bullets.BulletComport.BulletStats;
import GameMath.Vector2D;

/**
 * HUD del crosshair y preview de trayectoria.
 *
 * Adaptado al nuevo sistema de input:
 *  - MouseInput.rightPressed → MouseInput.getButtonState("rightPressed")
 */
public class CrossHairHUD implements UIElement {

    private final Player player;

    // Centro virtual
    private int centerX;
    private int centerY;

    public CrossHairHUD(Player player, int virtualWidth, int virtualHeight) {
        this.player = player;
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

        // FIX INPUT REFACTOR
        if (!MouseInput.getButtonState("rightPressed")) return;

        WeaponSelected weapon = player.getCombat()
                                      .getInventory()
                                      .getCurrentWeapon();

        if (weapon == null) return;

        // Color según estado del arma
        if (weapon.isReloading()) {
            g.setColor(Color.YELLOW);
        } else if (weapon.getFireWait() == 0) {
            g.setColor(Color.GREEN);
        } else {
            g.setColor(Color.RED);
        }

        // Trayectoria matemática sin instanciar bullets
        BulletStats stats = BulletFactory.getStats(
            weapon.getBulletType(),
            weapon.getStats().getBulletSpeedBase(),
            weapon.getStats().getDamageBonusByWeapon()
        );

        // Spawn en coordenadas mundo
        double spawnX = player.getPosition().getX() + 20;
        double spawnY = player.getPosition().getY() + 20;

        Vector2D aim = player.getState().getAimDirection();

        double velX = aim.getX() * stats.getSpeed();
        double velY = aim.getY() * stats.getSpeed();

        double gravity = stats.hasGravity() ? 0.4 : 0.0;
        int steps = stats.getLifeTime();

        double px = spawnX;
        double py = spawnY;
        double vy = velY;

        for (int i = 0; i < steps; i++) {

            px += velX;
            py += vy;

            if (stats.hasGravity()) {
                vy += gravity;
            }

            g.fillOval((int) px, (int) py, 4, 4);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void recalcPosition(int virtualWidth, int virtualHeight) {
        centerX = virtualWidth / 2;
        centerY = virtualHeight / 2;
    }
}