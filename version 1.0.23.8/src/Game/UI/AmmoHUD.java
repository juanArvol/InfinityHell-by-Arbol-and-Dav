package Game.UI;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import Game.Weapons.WeaponInventory;
import Game.Weapons.WeaponSelected;
import Game.Weapons.WeaponType.WeaponStats;

public class AmmoHUD implements UIElement {
    
    private int x = 900;
    private int y = 30;
    private final WeaponInventory inventory;
    private final double originalXRatio;
    private final double originalYRatio;

    public AmmoHUD(WeaponInventory inventory, int screenWidth, int screenHeight) {
        this.inventory = inventory;
        this.originalXRatio = 0.7; // 70% horizontal
        this.originalYRatio = 0.02; // 2% vertical
    }

    @Override
    public void update() {}

    @Override
    public void onResize(int screenWidth, int screenHeight) {
    // Recalcular posición relativa a la nueva pantalla
    x = (int)(screenWidth * originalXRatio);
    y = (int)(screenHeight * originalYRatio);
    }
    @Override
public void draw(Graphics g){
    WeaponSelected weapon = inventory.getCurrentWeapon();
    if(weapon == null) return;

    WeaponStats stats = weapon.getStats();

    Rectangle bounds = g.getClipBounds();
    if(bounds != null){
        x = (int)(bounds.width * 0.7);  // por ejemplo, 70% desde izquierda
        y = (int)(bounds.height * 0.02); // 2% desde arriba
    }

    int width = 140;  // ancho fijo
    int height = 12;  // alto fijo

    // ---------- COOLDOWN BAR ----------
    int cooldown = weapon.getCooldown();
    int fireWait = weapon.getFireWait();

    g.setColor(Color.BLACK);
    g.drawRect(x, y, width, height);

    double percent = cooldown > 0 ? 1.0 - (fireWait / (double) cooldown) : 1.0;

    if(weapon.isReloading())
        g.setColor(Color.YELLOW);
    else if(fireWait == 0)
        g.setColor(Color.GREEN);
    else
        g.setColor(Color.RED);

    g.fillRect(x + 1, y + 1, (int)(percent * width), height - 1);

    // ---------- TEXTO ----------
    g.setColor(Color.BLACK);
    g.drawString("Ammo: " + weapon.getCurrentAmmo() + "/" + weapon.getMaxAmmo(), x, y + 30);
    if(weapon.isReloading()) g.drawString("RELOADING", x, y + 45);
    g.drawString("Damage: " + stats.getDamageBonusByWeapon(), x, y + 60);
    g.drawString("Speed: " + stats.getBulletSpeedBase(), x, y + 75);
    g.drawString("Pellets: " + stats.getBulletsPerShot(), x, y + 90);
    g.drawString("Spread: " + (int)stats.getSpread(), x, y + 105);
}

}