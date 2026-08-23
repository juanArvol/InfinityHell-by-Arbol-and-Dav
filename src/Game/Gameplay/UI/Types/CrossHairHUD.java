package Game.Gameplay.UI.Types;

import Game.Engine.Camera.GameCamera;
import Game.Gameplay.UI.Aim.AimCapabilityManager;
import Game.Gameplay.UI.Aim.AimVisualizationCapability;
import Game.Gameplay.UI.Aim.BasicCrosshairCapability;
import Game.Gameplay.UI.UIElement;
import Game.Player.Player;
import Inputs.MouseInput;
import java.awt.Graphics2D;
import java.util.function.Supplier;

/**
 * HUD del crosshair — coordina capabilities de visualización de apuntado.
 *
 * ── REFACTORIZACIÓN — Capability-Based Architecture ──────────────────────
 *
 * CAMBIO ARQUITECTÓNICO:
 *   - CrossHairHUD ya NO implementa directamente el renderizado
 *   - Delega a capabilities activas del Player (AimCapabilityManager)
 *   - Capabilities son concedidas por amuletos de forma aditiva
 *   - Sin if/else para tipos específicos de visualización
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 * CrossHairHUD (coordinador):
 *   ✓ Obtiene capabilities activas desde player.getAimCapabilities()
 *   ✓ Itera sobre capabilities en orden de prioridad
 *   ✓ Convierte coordenadas (cámara offset)
 *   ✓ Controla visibilidad global (botón derecho)
 *
 * AimVisualizationCapability (implementación):
 *   ✓ Renderiza su aspecto visual específico
 *   ✓ Consulta estado del arma/proyectil según necesite
 *   ✓ No conoce otros capabilities
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *   CrossHairHUD
 *          │
 *          └── Player.getAimCapabilities()
 *               │
 *               └── List<AimVisualizationCapability> (ordenado por prioridad)
 *                    │
 *                    ├── BasicCrosshairCapability (siempre activo)
 *                    ├── TrajectoryVisualizationCapability (concedido por amuleto)
 *                    └── WeaponInfoCapability (concedido por amuleto)
 *
 * ── CAPABILITIES POR DEFECTO ──────────────────────────────────────────────
 *
 * Todos los Players tienen BasicCrosshairCapability desde el inicio.
 * Las capabilities adicionales se conceden mediante amuletos:
 *
 *   "Ojo del Tirador" → TrajectoryVisualizationCapability
 *   (futuros amuletos) → WeaponInfoCapability, RangeIndicatorCapability, etc.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 *
 * Añadir nuevas visualizaciones:
 *   1. Crear nueva clase que implemente AimVisualizationCapability
 *   2. Crear amuleto que conceda la capability via UICapabilityEffect
 *   3. Registrar amuleto en AmuletRegistry
 *   4. ¡Listo! — CrossHairHUD lo usa automáticamente sin modificación
 *
 * ── COORDINACIÓN CON CÁMARA ───────────────────────────────────────────────
 *
 * CrossHairHUD obtiene el offset de cámara y lo pasa a cada capability.
 * Las capabilities convierten coordenadas de mundo → pantalla virtual
 * restando el offset antes de dibujar.
 */
public class CrossHairHUD implements UIElement {

    private final Player               player;
    private final Supplier<GameCamera> cameraSupplier;
    private final int                  virtualWidth;
    private final int                  virtualHeight;

    // Capability básica — siempre presente
    private BasicCrosshairCapability basicCrosshair;

    /**
     * @param player          el jugador (para capabilities y estado)
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
        this.virtualWidth   = virtualWidth;
        this.virtualHeight  = virtualHeight;

        // Inicializar capability básica
        initializeDefaultCapabilities();
    }

    /**
     * Inicializa las capabilities por defecto que todos los Players tienen.
     */
    private void initializeDefaultCapabilities() {
        basicCrosshair = new BasicCrosshairCapability(virtualWidth, virtualHeight);
        
        // Añadir el crosshair básico al manager del Player
        AimCapabilityManager capabilities = player.getAimCapabilities();
        if (capabilities != null) {
            capabilities.add(basicCrosshair);
        }
    }

    @Override 
    public void update() {
        // No-op — las capabilities son stateless o se actualizan individualmente
    }

    @Override
    public void onResize(int virtualWidth, int virtualHeight) {
        // Recrear capability básica con nuevas dimensiones
        basicCrosshair = new BasicCrosshairCapability(virtualWidth, virtualHeight);
        
        // Reemplazar en el manager
        AimCapabilityManager capabilities = player.getAimCapabilities();
        if (capabilities != null) {
            capabilities.removeByClass(BasicCrosshairCapability.class);
            capabilities.add(basicCrosshair);
        }

        // Nota: Otras capabilities (concedidas por amuletos) podrían necesitar
        // actualización de dimensiones. Por ahora, son stateless respecto a
        // la resolución o se recalculan en cada frame.
    }

    @Override
    public void draw(Graphics2D g) {
        // Verificar visibilidad global
        boolean shouldShow = MouseInput.getButtonState("rightPressed");
        
        // Obtener offset de cámara
        GameCamera camera = cameraSupplier.get();
        double camX = (camera != null) ? camera.getX() : 0;
        double camY = (camera != null) ? camera.getY() : 0;

        // Obtener capabilities activas del Player
        AimCapabilityManager capabilityManager = player.getAimCapabilities();
        if (capabilityManager == null) return;

        // Renderizar cada capability en orden de prioridad
        for (AimVisualizationCapability capability : capabilityManager.getAll()) {
            // Verificar si esta capability requiere apuntado activo
            if (capability.requiresAiming() && !shouldShow) {
                continue; // Saltar capabilities que requieren botón derecho
            }

            // Renderizar la capability
            capability.render(g, player, camX, camY);
        }
    }
}

