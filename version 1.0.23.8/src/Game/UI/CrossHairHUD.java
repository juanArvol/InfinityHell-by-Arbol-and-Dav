package Game.UI;

import java.awt.Color;
import java.awt.Graphics;

import Entradas.MouseInput;
import Game.Player.Player;
import Game.Weapons.WeaponSelected;
import GameMath.Vector2D;
import Game.Bullets.Bullet;
import Game.Bullets.BulletFactory;
import Game.Bullets.BulletComport.BulletStats;

public class CrossHairHUD implements UIElement {

    private final Player player;

    private int x;
    private int y;
    private final double originalXRatio;
    private final double originalYRatio;

    public CrossHairHUD(Player player, int screenWidth, int screenHeight) {
        this.player = player;
        this.originalXRatio = 0.5; // centro horizontal relativo
        this.originalYRatio = 0.5; // centro vertical relativo

        this.x = (int)(screenWidth * originalXRatio);
        this.y = (int)(screenHeight * originalYRatio);
    }

    @Override
    public void update() {
    }

    @Override
    public void onResize(int screenWidth, int screenHeight) {
        x = (int)(screenWidth * originalXRatio);
        y = (int)(screenHeight * originalYRatio);
    }

    @Override
    public void draw(Graphics g) {
        if (!MouseInput.rightPressed) return;

        WeaponSelected weapon = player.getCombat().getInventory().getCurrentWeapon();
        if (weapon == null) return;

        int fireWait = weapon.getFireWait();

        // Color dinámico según estado del arma
        if (weapon.isReloading()) g.setColor(Color.YELLOW);
        else if (fireWait == 0) g.setColor(Color.GREEN);
        else g.setColor(Color.RED);

        // Posición inicial del proyectil (puedes ajustar offsets)
        double spawnX = player.getPosition().getX() + 20;
        double spawnY = player.getPosition().getY() + 20;

        Vector2D aim = player.getState().getAimDirection();

        BulletStats stats = BulletFactory.getStats(
            weapon.getBulletType(),
            weapon.getStats().getBulletSpeedBase(),
            weapon.getStats().getDamageBonusByWeapon()
        );

        int steps = (int)weapon.getBulletSpeedBase();

        Bullet ghost = BulletFactory.createBullet(
            spawnX,
            spawnY,
            aim,
            weapon.getBulletType(),
            stats.getLifeTime() * weapon.getBulletSpeedBase(),
            0
        );

        // Dibujar trayectoria proyectil
        for (int i = 0; i < steps; i++) {
            ghost.update();
            double px = ghost.getTransform().getPosition().getX();
            double py = ghost.getTransform().getPosition().getY();
            g.fillOval((int)px, (int)py, 4, 4);
        }

        // crosshair central 
        /* int size = 7;
        g.setColor(Color.BLACK);
        g.drawLine(x - size, y, x + size, y);
        g.drawLine(x, y - size, x, y + size); */
    }
}