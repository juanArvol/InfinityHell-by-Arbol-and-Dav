package Game.Gameplay.UI.Aim;

import Game.Items.Types.Weapons.ModifiedWeapon;
import Game.Player.Player;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Capability de crosshair básico — renderiza la cruz de apuntado central
 * con color dinámico según estado del arma.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 *   • Renderizar crosshair en el centro virtual de la pantalla
 *   • Color dinámico:
 *       - AMARILLO: Recargando
 *       - VERDE: Lista para disparar (fireWait == 0)
 *       - ROJO: En cooldown
 *   • Tamaño configurable (default 8px)
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 * Esta es la capability más básica — siempre activa por defecto.
 * Todos los Players tienen esta capability desde el inicio sin necesidad
 * de amuletos.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   BasicCrosshairCapability → renderiza el crosshair
 *   TrajectoryCapability     → renderiza la trayectoria proyectada
 *   RangeCapability          → renderiza el indicador de alcance
 *   CrossHairHUD             → coordina todas las capabilities
 */
public class BasicCrosshairCapability implements AimVisualizationCapability {

    private final int centerX;
    private final int centerY;
    private final int hairSize;

    /**
     * @param virtualWidth  ancho virtual de la pantalla
     * @param virtualHeight alto virtual de la pantalla
     * @param hairSize      tamaño del crosshair en píxeles (default 8)
     */
    public BasicCrosshairCapability(int virtualWidth, int virtualHeight, int hairSize) {
        this.centerX = virtualWidth / 2;
        this.centerY = virtualHeight / 2;
        this.hairSize = hairSize;
    }

    /**
     * Constructor con tamaño por defecto.
     */
    public BasicCrosshairCapability(int virtualWidth, int virtualHeight) {
        this(virtualWidth, virtualHeight, 8);
    }

    @Override
    public void render(Graphics2D g, Player player, double camX, double camY) {
        ModifiedWeapon weapon = player.getRuntime().getCurrentWeapon();
        if (weapon == null) return;

        Color prevColor = g.getColor();

        // Color según estado del arma
        if (player.getState().isReloading()) {
            g.setColor(Color.YELLOW);
        } else if (weapon.getFireWait() == 0) {
            g.setColor(Color.GREEN);
        } else {
            g.setColor(Color.RED);
        }

        // Dibujar crosshair
        g.drawLine(centerX - hairSize, centerY, centerX + hairSize, centerY);
        g.drawLine(centerX, centerY - hairSize, centerX, centerY + hairSize);

        g.setColor(prevColor);
    }

    @Override
    public int getRenderPriority() {
        return 30; // Renderiza antes que trayectorias (40-60)
    }

    @Override
    public boolean requiresAiming() {
        return true; // Solo visible con botón derecho
    }

    @Override
    public String getName() {
        return "Basic Crosshair";
    }

    // ── Actualización de posición en resize ───────────────────────────────

    /**
     * Recalcula la posición central del crosshair.
     * Debe llamarse desde CrossHairHUD.onResize().
     *
     * Nota: Como BasicCrosshairCapability es inmutable por diseño,
     * CrossHairHUD debe recrear la instancia en resize. Alternativa
     * futura: hacer centerX/centerY mutables con setter.
     */
    public BasicCrosshairCapability withNewDimensions(int virtualWidth, int virtualHeight) {
        return new BasicCrosshairCapability(virtualWidth, virtualHeight, hairSize);
    }
}
