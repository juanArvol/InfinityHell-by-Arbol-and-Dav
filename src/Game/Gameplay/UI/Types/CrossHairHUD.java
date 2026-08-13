package Game.Gameplay.UI.Types;

import Game.Engine.Camera.GameCamera;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Gameplay.UI.UIElement;
import Game.Items.Types.Weapons.ModifiedWeapon.ProjectilePreview;
import Game.Player.Player;
import Inputs.MouseInput;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.function.Supplier;

/**
 * HUD del crosshair y preview de trayectoria.
 *
 * ── HRFC — Player Inventory & Domain Ownership Consolidation ──────────────
 *
 * CAMBIO ARQUITECTÓNICO:
 *   - CrossHairHUD ahora usa player.getCombat().getProjectilePreview()
 *   - Eliminado uso directo de weapon.getBulletType(), BulletFactory, ProjectileBlueprint
 *   - PlayerState es la fuente de verdad del estado lógico del Player
 *   - weapon.isReloading() representa únicamente la mecánica interna del arma
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 * CrossHairHUD NO debe:
 *   ✗ crear BulletBehavior
 *   ✗ crear ProjectileBlueprint  
 *   ✗ invocar BulletFactory
 *   ✗ resolver BulletType
 *   ✗ conocer detalles internos del pipeline de disparo
 *   ✗ depender de ModifiedWeapon.getBulletType()
 *
 * CrossHairHUD SÍ debe:
 *   ✓ consultar ProjectilePreview desde PlayerCombat
 *   ✓ renderizar crosshair según estado del arma
 *   ✓ dibujar trayectoria matemática usando stats de preview
 *   ✓ convertir coordenadas mundo → pantalla virtual
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

        // ── HRFC — Player Inventory & Domain Ownership Consolidation ─────
        // Usar ProjectilePreview desde PlayerCombat en lugar de reconstruir
        // manualmente el pipeline de disparo desde CrossHairHUD.
        ProjectilePreview preview = player.getCombat().getProjectilePreview();
        
        if (preview == null) return; // Sin arma o bala activa

        Color prevColor = g.getColor();

        // Color según estado del arma
        // ── HRFC: Consultar PlayerState para recarga ─────────────────────────
        if (player.getState().isReloading()) {
            g.setColor(Color.YELLOW);
        } else if (player.getRuntime().getCurrentWeapon() != null 
                   && player.getRuntime().getCurrentWeapon().getFireWait() == 0) {
            g.setColor(Color.GREEN);
        } else {
            g.setColor(Color.RED);
        }

        // ── Crosshair en el centro virtual ────────────────────────────────
        final int HAIR_SIZE = 8;
        g.drawLine(centerX - HAIR_SIZE, centerY, centerX + HAIR_SIZE, centerY);
        g.drawLine(centerX, centerY - HAIR_SIZE, centerX, centerY + HAIR_SIZE);

        // ── Trayectoria matemática desde ProjectilePreview ───────────────
        // Offset de cámara para convertir coordenadas de mundo → pantalla virtual.
        GameCamera camera = cameraSupplier.get();
        double camX = (camera != null) ? camera.getX() : 0;
        double camY = (camera != null) ? camera.getY() : 0;

        // Convertir spawn de MUNDO a PANTALLA VIRTUAL
        double screenSpawnX = preview.spawnPosition().getX() - camX;
        double screenSpawnY = preview.spawnPosition().getY() - camY;

        Vector2D aim = player.getState().getAimDirection();

        double velX    = aim.getX() * preview.speed();
        double velY    = aim.getY() * preview.speed();
        double gravity = preview.hasGravity() ? 0.4 : 0.0;
        int    steps   = preview.lifeTime();

        double px = screenSpawnX;
        double py = screenSpawnY;
        double vy = velY;

        for (int i = 0; i < steps; i++) {
            px += velX;
            py += vy;
            if (preview.hasGravity()) vy += gravity;
            g.fillOval((int) px, (int) py, 4, 4);
        }

        g.setColor(prevColor);
    }

    private void recalcPosition(int virtualWidth, int virtualHeight) {
        centerX = virtualWidth  / 2;
        centerY = virtualHeight / 2;
    }
}
