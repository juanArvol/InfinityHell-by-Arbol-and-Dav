package Game.UI.Types;

import Game.Engine.Camera.GameCamera;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Items.Types.Bullets.BulletComport.BulletStats;
import Game.Items.Types.Bullets.BulletFactory;
import Game.Items.Types.Weapons.WeaponSelected;
import Game.Player.Player;
import Game.UI.UIElement;
import Inputs.MouseInput;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.function.Supplier;

/**
 * HUD del crosshair y preview de trayectoria.
 *
 * ── HRFC-001: usa GameCamera (entidad del Engine) ─────────────────────────
 *
 * La cámara se inyecta como Supplier<GameCamera>. La conversión de
 * coordenadas de mundo a pantalla virtual sigue siendo la misma:
 *   screenX = worldX - camera.getX()
 *   screenY = worldY - camera.getY()
 *
 * Con zoom=1 y rotation=0 (caso actual), el resultado es idéntico al anterior.
 * Con zoom≠1, la conversión correcta requeriría aplicar el viewTransform, pero
 * para el crosshair (UI en espacio de pantalla) la posición central no cambia.
 * La trayectoria que parte del player SÍ necesita el offset correcto.
 *
 * ── CORRECCIÓN BUG-9 — COORDENADAS DE PANTALLA ───────────────────────────
 *
 * La trayectoria de la bala se calcula en coordenadas de MUNDO y se convierte
 * a coordenadas de PANTALLA VIRTUAL restando el offset de cámara antes de dibujar.
 */
public class CrossHairHUD implements UIElement {

    private final Player              player;
    private final Supplier<GameCamera> cameraSupplier;

    private int centerX;
    private int centerY;

    /**
     * @param player          el jugador (para posición y estado del arma)
     * @param cameraSupplier  proveedor de la GameCamera activa del Engine
     * @param virtualWidth    DisplaySettings.virtualWidth
     * @param virtualHeight   DisplaySettings.virtualHeight
     */
    public CrossHairHUD(Player player,
                        Supplier<GameCamera> cameraSupplier,
                        int virtualWidth,
                        int virtualHeight) {
        this.player         = player;
        this.cameraSupplier = cameraSupplier;
        recalcPosition(virtualWidth, virtualHeight);
    }

    @Override public void update() {}

    @Override
    public void onResize(int virtualWidth, int virtualHeight) {
        recalcPosition(virtualWidth, virtualHeight);
    }

    @Override
    public void draw(Graphics2D g) {

        if (!MouseInput.getButtonState("rightPressed")) return;

        WeaponSelected weapon = player.getCombat()
                                      .getInventory()
                                      .getCurrentWeapon();
        if (weapon == null) return;

        Color prevColor = g.getColor();

        // Color según estado del arma
        if (weapon.isReloading()) {
            g.setColor(Color.YELLOW);
        } else if (weapon.getFireWait() == 0) {
            g.setColor(Color.GREEN);
        } else {
            g.setColor(Color.RED);
        }

        // ── Crosshair en el centro virtual ────────────────────────────────
        final int HAIR_SIZE = 8;
        g.drawLine(centerX - HAIR_SIZE, centerY, centerX + HAIR_SIZE, centerY);
        g.drawLine(centerX, centerY - HAIR_SIZE, centerX, centerY + HAIR_SIZE);

        // ── Trayectoria matemática ────────────────────────────────────────
        BulletStats stats = BulletFactory.getStats(
            weapon.getBulletType(),
            weapon.getStats().getBulletSpeedBase(),
            weapon.getStats().getDamageBonusByWeapon()
        );

        // Offset de cámara para convertir coordenadas de mundo → pantalla virtual.
        // GameCamera.getX/Y() devuelve el offset top-left (igual que Camera.getX/Y()).
        GameCamera camera = cameraSupplier.get();
        double camX = (camera != null) ? camera.getX() : 0;
        double camY = (camera != null) ? camera.getY() : 0;

        // Spawn del proyectil en coordenadas MUNDO
        double worldSpawnX = player.getPosition().getX() + 20;
        double worldSpawnY = player.getPosition().getY() + 20;

        // Convertir a coordenadas de PANTALLA VIRTUAL
        double screenSpawnX = worldSpawnX - camX;
        double screenSpawnY = worldSpawnY - camY;

        Vector2D aim = player.getState().getAimDirection();

        double velX    = aim.getX() * stats.getSpeed();
        double velY    = aim.getY() * stats.getSpeed();
        double gravity = stats.hasGravity() ? 0.4 : 0.0;
        int    steps   = stats.getLifeTime();

        double px = screenSpawnX;
        double py = screenSpawnY;
        double vy = velY;

        for (int i = 0; i < steps; i++) {
            px += velX;
            py += vy;
            if (stats.hasGravity()) vy += gravity;
            g.fillOval((int) px, (int) py, 4, 4);
        }

        g.setColor(prevColor);
    }

    private void recalcPosition(int virtualWidth, int virtualHeight) {
        centerX = virtualWidth  / 2;
        centerY = virtualHeight / 2;
    }
}
