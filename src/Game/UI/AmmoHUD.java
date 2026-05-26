package Game.UI;

import java.awt.Color;
import java.awt.Graphics;

import Game.Weapons.WeaponInventory;
import Game.Weapons.WeaponSelected;
import Game.Weapons.WeaponType.WeaponStats;

/**
 * FIX BUG-17: el original usaba g.getClipBounds() en draw() para calcular la
 * posicion del HUD, lo que ignoraba completamente el sistema onResize() y causaba
 * que el HUD saltara de posicion entre frames (getClipBounds puede devolver null
 * o cambiar dependiendo del contexto grafico).
 *
 * Ahora el HUD usa x/y calculados en el constructor y actualizados en onResize(),
 * igual que CrossHairHUD. draw() ya no toca la posicion.
 */
public class AmmoHUD implements UIElement {

    private int x;
    private int y;
    private final WeaponInventory inventory;
    private final double originalXRatio;
    private final double originalYRatio;

    public AmmoHUD(WeaponInventory inventory, int screenWidth, int screenHeight) {
        this.inventory       = inventory;
        this.originalXRatio  = 0.7;   // 70% horizontal
        this.originalYRatio  = 0.02;  // 2% vertical
        // FIX: calcular posicion inicial en el constructor
        this.x = (int)(screenWidth  * originalXRatio);
        this.y = (int)(screenHeight * originalYRatio);
    }

    @Override
    public void update() {}

    @Override
    public void onResize(int screenWidth, int screenHeight) {
        x = (int)(screenWidth  * originalXRatio);
        y = (int)(screenHeight * originalYRatio);
    }

    @Override
    public void draw(Graphics g) {
        WeaponSelected weapon = inventory.getCurrentWeapon();
        if (weapon == null) return;

        WeaponStats stats = weapon.getStats();

        // FIX BUG-17: NO usar getClipBounds() aqui. x e y ya estan calculados.

        int width  = 140;
        int height = 12;

        // ---------- COOLDOWN BAR ----------
        int cooldown = weapon.getCooldown();
        int fireWait = weapon.getFireWait();

        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);

        double percent = cooldown > 0 ? 1.0 - (fireWait / (double) cooldown) : 1.0;

        if (weapon.isReloading())
            g.setColor(Color.YELLOW);
        else if (fireWait == 0)
            g.setColor(Color.GREEN);
        else
            g.setColor(Color.RED);

        g.fillRect(x + 1, y + 1, (int)(percent * width), height - 1);

        // ---------- TEXTO ----------
        g.setColor(Color.BLACK);
        g.drawString("Ammo: "    + weapon.getCurrentAmmo() + "/" + weapon.getMaxAmmo(), x, y + 30);
        if (weapon.isReloading()) g.drawString("RELOADING", x, y + 45);
        g.drawString("Damage: "  + stats.getDamageBonusByWeapon(),  x, y + 60);
        g.drawString("Speed: "   + stats.getBulletSpeedBase(),      x, y + 75);
        g.drawString("Pellets: " + stats.getBulletsPerShot(),        x, y + 90);
        g.drawString("Spread: "  + (int) stats.getSpread(),          x, y + 105);
    }
}
