package Game.Gameplay.UI.Aim;

import Game.Items.Types.Weapons.ModifiedWeapon;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import Game.Player.Player;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Capability de información del arma — muestra stats del arma actual
 * cerca del crosshair cuando se está apuntando.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 *   • Renderizar información del arma activa:
 *       - Nombre del arma
 *       - Cadencia (cooldown/fireWait)
 *       - Dispersión (spread)
 *       - Munición actual/máxima
 *       - Estado de recarga
 *   • Posicionarse relativamente al crosshair
 *   • Estilo configurable (overlay compacto o panel extendido)
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   WeaponInfoCapability    → muestra stats del arma cerca del crosshair
 *   AmmoHUD                 → muestra munición completa en esquina de pantalla
 *   BasicCrosshairCapability → muestra la cruz de apuntado
 *   TrajectoryCapability    → muestra la trayectoria proyectada
 *
 * WeaponInfoCapability es opcional — se concede por amuletos que mejoran
 * la UI de apuntado.
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 * Esta capability puede coexistir con AmmoHUD. Mientras AmmoHUD muestra
 * información completa en la esquina, WeaponInfoCapability muestra un
 * resumen compacto cerca del crosshair durante el apuntado activo.
 */
public class WeaponInfoCapability implements AimVisualizationCapability {

    private final int centerX;
    private final int centerY;
    private final InfoStyle style;
    private final int offsetX;
    private final int offsetY;

    /**
     * Estilo de visualización de información del arma.
     */
    public enum InfoStyle {
        /** Compacto: solo nombre y estado (1-2 líneas) */
        COMPACT,
        
        /** Estándar: nombre, estado, cadencia, dispersión (3-4 líneas) */
        STANDARD,
        
        /** Extendido: toda la información disponible (5+ líneas) */
        EXTENDED
    }

    /**
     * @param virtualWidth  ancho virtual de la pantalla
     * @param virtualHeight alto virtual de la pantalla
     * @param style         estilo de visualización
     * @param offsetX       offset horizontal desde el centro (píxeles)
     * @param offsetY       offset vertical desde el centro (píxeles)
     */
    public WeaponInfoCapability(int virtualWidth, int virtualHeight, 
                                InfoStyle style, int offsetX, int offsetY) {
        this.centerX = virtualWidth / 2;
        this.centerY = virtualHeight / 2;
        this.style = style;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    /**
     * Constructor con estilo y offset por defecto.
     * Posiciona la info a la derecha y arriba del crosshair.
     */
    public WeaponInfoCapability(int virtualWidth, int virtualHeight) {
        this(virtualWidth, virtualHeight, InfoStyle.STANDARD, 20, -30);
    }

    @Override
    public void render(Graphics2D g, Player player, double camX, double camY) {
        ModifiedWeapon weapon = player.getRuntime().getCurrentWeapon();
        if (weapon == null) return;

        WeaponStats stats = weapon.getStats();
        Color prevColor = g.getColor();

        int x = centerX + offsetX;
        int y = centerY + offsetY;

        // Renderizar según estilo
        switch (style) {
            case COMPACT -> renderCompact(g, weapon, stats, x, y, player);
            case STANDARD -> renderStandard(g, weapon, stats, x, y, player);
            case EXTENDED -> renderExtended(g, weapon, stats, x, y, player);
        }

        g.setColor(prevColor);
    }

    private void renderCompact(Graphics2D g, ModifiedWeapon weapon, 
                               WeaponStats stats, int x, int y, Player player) {
        g.setColor(Color.WHITE);
        
        // Solo nombre del arma y estado de recarga
        String weaponName = "weapon.getWeaponType().name()";
        g.drawString(weaponName, x, y);
        
        if (player.getState().isReloading()) {
            g.setColor(Color.YELLOW);
            g.drawString("RELOADING", x, y + 15);
        }
    }

    private void renderStandard(Graphics2D g, ModifiedWeapon weapon, 
                                WeaponStats stats, int x, int y, Player player) {
        g.setColor(Color.WHITE);
        
        // Nombre del arma
        String weaponName = "weapon.getWeaponType().name()";
        g.drawString(weaponName, x, y);
        
        // Estado de recarga o cadencia
        if (player.getState().isReloading()) {
            g.setColor(Color.YELLOW);
            g.drawString("RELOADING", x, y + 15);
        } else {
            // Indicador de cadencia
            double cooldownPercent = weapon.getCooldown() > 0 
                ? 1.0 - (weapon.getFireWait() / weapon.getCooldown())
                : 1.0;
            
            if (weapon.getFireWait() == 0) {
                g.setColor(Color.GREEN);
                g.drawString("READY", x, y + 15);
            } else {
                g.setColor(Color.ORANGE);
                g.drawString(String.format("%.0f%%", cooldownPercent * 100), x, y + 15);
            }
        }
        
        // Dispersión
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("Spread: " + (int) stats.getSpread(), x, y + 30);
    }

    private void renderExtended(Graphics2D g, ModifiedWeapon weapon, 
                                WeaponStats stats, int x, int y, Player player) {
        g.setColor(Color.WHITE);
        
        // Nombre del arma
        String weaponName = "weapon.getWeaponType().name()";
        g.drawString(weaponName, x, y);
        
        int lineHeight = 15;
        int currentY = y + lineHeight;
        
        // Munición
        g.setColor(Color.CYAN);
        g.drawString("Ammo: " + weapon.getCurrentAmmo() + "/" + weapon.getMaxAmmo(), 
                     x, currentY);
        currentY += lineHeight;
        
        // Estado de recarga o cadencia
        if (player.getState().isReloading()) {
            g.setColor(Color.YELLOW);
            g.drawString("RELOADING", x, currentY);
        } else {
            double cooldownPercent = weapon.getCooldown() > 0 
                ? 1.0 - (weapon.getFireWait() / weapon.getCooldown())
                : 1.0;
            
            if (weapon.getFireWait() == 0) {
                g.setColor(Color.GREEN);
                g.drawString("READY", x, currentY);
            } else {
                g.setColor(Color.ORANGE);
                g.drawString(String.format("Cooldown: %.0f%%", cooldownPercent * 100), 
                             x, currentY);
            }
        }
        currentY += lineHeight;
        
        // Daño
        g.setColor(Color.RED);
        g.drawString("Damage: " + stats.getDamageBonusByWeapon(), x, currentY);
        currentY += lineHeight;
        
        // Velocidad de bala
        g.setColor(Color.YELLOW);
        g.drawString("Speed: " + stats.getBulletSpeedBase(), x, currentY);
        currentY += lineHeight;
        
        // Pellets por disparo
        g.setColor(Color.WHITE);
        g.drawString("Pellets: " + stats.getBulletsPerShot(), x, currentY);
        currentY += lineHeight;
        
        // Dispersión
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("Spread: " + (int) stats.getSpread(), x, currentY);
    }

    @Override
    public int getRenderPriority() {
        return 70; // Renderiza después de trayectorias, como overlay
    }

    @Override
    public boolean requiresAiming() {
        return true; // Solo visible con botón derecho
    }

    @Override
    public String getName() {
        return "Weapon Info (" + style.name() + ")";
    }

    // ── Actualización en resize ───────────────────────────────────────────

    /**
     * Recalcula la posición del info panel.
     * Debe llamarse desde CrossHairHUD.onResize().
     */
    public WeaponInfoCapability withNewDimensions(int virtualWidth, int virtualHeight) {
        return new WeaponInfoCapability(virtualWidth, virtualHeight, style, offsetX, offsetY);
    }
}
