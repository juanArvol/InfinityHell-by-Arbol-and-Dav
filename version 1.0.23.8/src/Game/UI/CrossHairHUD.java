package Game.UI;

import java.awt.Color;
import java.awt.Graphics;

import Entradas.MouseInput;
import Game.Player.Player;
import Game.Weapons.WeaponSelected;
import Game.Bullets.BulletFactory;
import Game.Bullets.BulletComport.BulletStats;
import GameMath.Vector2D;

/**
 * HUD del crosshair y preview de trayectoria.
 *
 * REFACTOR BUG-011: en el original se creaba un Bullet completo (con SpriteRenderer,
 * ColliderComponent, HitBoxComponent, BulletPhysics) CADA FRAME para simular
 * la trayectoria. Eso generaba garbage collection pressure masiva a 30 FPS.
 *
 * Solución: calcular la trayectoria matemáticamente usando solo vectores,
 * sin instanciar ningún objeto de juego.
 */
public class CrossHairHUD implements UIElement {

    private final Player player;
    private int x;
    private int y;
    private final double originalXRatio;
    private final double originalYRatio;

    public CrossHairHUD(Player player, int screenWidth, int screenHeight) {
        this.player = player;
        this.originalXRatio = 0.5;
        this.originalYRatio = 0.5;
        this.x = (int)(screenWidth * originalXRatio);
        this.y = (int)(screenHeight * originalYRatio);
    }

    @Override
    public void update() {}

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

        // Color según estado del arma
        if (weapon.isReloading())       g.setColor(Color.YELLOW);
        else if (weapon.getFireWait() == 0) g.setColor(Color.GREEN);
        else                            g.setColor(Color.RED);

        // REFACTOR BUG-011: en lugar de instanciar un Bullet y simular física,
        // calculamos la trayectoria directamente con matemáticas.
        BulletStats stats = BulletFactory.getStats(
            weapon.getBulletType(),
            weapon.getStats().getBulletSpeedBase(),
            weapon.getStats().getDamageBonusByWeapon()
        );

        double spawnX = player.getPosition().getX() + 20;
        double spawnY = player.getPosition().getY() + 20;

        Vector2D aim  = player.getState().getAimDirection();
        double velX   = aim.getX() * stats.getSpeed();
        double velY   = aim.getY() * stats.getSpeed();

        double gravity = stats.hasGravity() ? 0.4 : 0.0; // valor estándar de BulletPhysics
        int steps = stats.getLifeTime();

        double px = spawnX;
        double py = spawnY;
        double vy = velY;

        // Simulación puramente matemática: sin objetos, sin GC
        for (int i = 0; i < steps; i++) {
            px += velX;
            py += vy;
            if (stats.hasGravity()) vy += gravity;
            g.fillOval((int)px, (int)py, 4, 4);
        }
    }
}
