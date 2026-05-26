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
 * FIX REFACTOR DISPLAY:
 *  1. draw() recibe Graphics2D (framebuffer virtual), no Graphics.
 *
 *  2. Ya NO usa ratios (0.5 * screenWidth/screenHeight) para calcular la
 *     posición del crosshair. El crosshair se dibuja en el CENTRO VIRTUAL
 *     (virtualWidth/2, virtualHeight/2) — que es donde siempre debe estar
 *     en un shooter con cámara centrada en el player.
 *
 *  3. onResize() recibe virtualWidth/virtualHeight y recalcula correctamente.
 *
 *  4. La simulación de trayectoria sigue siendo matemática (sin instanciar
 *     Bullet — REFACTOR BUG-011 conservado).
 *
 * NOTA sobre coordenadas de trayectoria:
 * La posición del player (spawnX, spawnY) y la dirección de aim están en
 * coordenadas de MUNDO. La simulación dibuja sobre el framebuffer virtual
 * que tiene la transformación de cámara aplicada por el RenderContext,
 * por lo que el resultado queda alineado con el mundo correctamente.
 */
public class CrossHairHUD implements UIElement {

    private final Player player;

    // Centro virtual — donde el player aparece en pantalla
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
        if (!MouseInput.rightPressed) return;

        WeaponSelected weapon = player.getCombat().getInventory().getCurrentWeapon();
        if (weapon == null) return;

        // Color según estado del arma
        if (weapon.isReloading())           g.setColor(Color.YELLOW);
        else if (weapon.getFireWait() == 0) g.setColor(Color.GREEN);
        else                                g.setColor(Color.RED);

        // REFACTOR BUG-011: trayectoria matemática, sin instanciar Bullet
        BulletStats stats = BulletFactory.getStats(
            weapon.getBulletType(),
            weapon.getStats().getBulletSpeedBase(),
            weapon.getStats().getDamageBonusByWeapon()
        );

        // Spawn del proyectil en coordenadas de MUNDO
        double spawnX = player.getPosition().getX() + 20;
        double spawnY = player.getPosition().getY() + 20;

        Vector2D aim = player.getState().getAimDirection();
        double velX  = aim.getX() * stats.getSpeed();
        double velY  = aim.getY() * stats.getSpeed();

        double gravity = stats.hasGravity() ? 0.4 : 0.0;
        int    steps   = stats.getLifeTime();

        double px = spawnX;
        double py = spawnY;
        double vy = velY;

        // Simulación puramente matemática: sin objetos, sin GC
        for (int i = 0; i < steps; i++) {
            px += velX;
            py += vy;
            if (stats.hasGravity()) vy += gravity;
            g.fillOval((int) px, (int) py, 4, 4);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void recalcPosition(int virtualWidth, int virtualHeight) {
        // El crosshair siempre está en el centro virtual
        centerX = virtualWidth  / 2;
        centerY = virtualHeight / 2;
    }
}
