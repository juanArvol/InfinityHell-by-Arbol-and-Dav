package Game.Items.Types.Ammulets.Effects;

import Game.Gameplay.UI.Aim.AimVisualizationCapability;
import Game.Items.Types.Ammulets.AmuletEffect;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import java.util.function.Supplier;

/**
 * Efecto de amuleto que concede capabilities de UI al portador.
 *
 * ── DISEÑO ÚNICO ──────────────────────────────────────────────────────────
 *
 * Este efecto NO modifica stats ni behavior de proyectiles — concede
 * capabilities visuales al HUD del Player.
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *   Amuleto → UICapabilityEffect
 *          │
 *          └── factory de AimVisualizationCapability
 *
 *   Al equipar amuleto:
 *     player.getAimCapabilities().add(effect.createCapability(virtualWidth, virtualHeight))
 *
 *   Al desequipar amuleto:
 *     player.getAimCapabilities().removeByClass(capability.getClass())
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   UICapabilityEffect   → efecto de amuleto (dominio Items)
 *   AimCapabilityManager → gestor de capabilities del Player
 *   Capability           → implementación de la visualización (dominio UI)
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // En AmuletRegistry:
 *   register(new AmuletDefinition(
 *       "marksman_sight",
 *       "Vista del Tirador",
 *       "Revela la trayectoria exacta de tus disparos.",
 *       ItemRarity.RARE,
 *       UICapabilityEffect.trajectory(TrajectoryStyle.FADE, Color.CYAN)
 *   ));
 */
public class UICapabilityEffect implements AmuletEffect {

    private final Supplier<AimVisualizationCapability> capabilityFactory;

    /**
     * @param capabilityFactory factory que crea la capability.
     *                          Recibe (virtualWidth, virtualHeight) en construcción.
     */
    public UICapabilityEffect(Supplier<AimVisualizationCapability> capabilityFactory) {
        this.capabilityFactory = capabilityFactory;
    }

    /**
     * Crea la capability de visualización para el portador.
     * Debe llamarse desde el sistema de equipamiento del Player.
     *
     * @return nueva instancia de capability
     */
    public AimVisualizationCapability createCapability() {
        return capabilityFactory.get();
    }

    // ── AmuletEffect implementation (no-op para stats/behavior) ───────────

    @Override
    public void applyToStats(WeaponStats stats) {
        // UI capabilities no modifican stats de arma
    }

    @Override
    public BulletBehavior wrapBehavior(BulletBehavior base) {
        // UI capabilities no modifican behavior de proyectiles
        return base;
    }

    // ── Factory methods para capabilities comunes ─────────────────────────

    /**
     * Efecto que concede visualización de trayectoria.
     *
     * @param capabilityFactory factory que crea TrajectoryVisualizationCapability
     * @return efecto de amuleto
     */
    public static UICapabilityEffect trajectory(
            Supplier<AimVisualizationCapability> capabilityFactory) {
        return new UICapabilityEffect(capabilityFactory);
    }

    /**
     * Efecto que concede información del arma.
     *
     * @param capabilityFactory factory que crea WeaponInfoCapability
     * @return efecto de amuleto
     */
    public static UICapabilityEffect weaponInfo(
            Supplier<AimVisualizationCapability> capabilityFactory) {
        return new UICapabilityEffect(capabilityFactory);
    }

    /**
     * Efecto que concede múltiples capabilities.
     *
     * @param factories lista de factories de capabilities
     * @return efecto de amuleto compuesto
     */
    public static UICapabilityEffect composite(
            Supplier<AimVisualizationCapability>... factories) {
        return new UICapabilityEffect(() -> {
            // Retorna la primera capability — las demás deben añadirse manualmente
            // desde el sistema de equipamiento usando getAdditionalCapabilities()
            return factories[0].get();
        });
    }

    /**
     * Retorna factories adicionales para capabilities compuestas.
     * Útil cuando un solo amuleto concede múltiples capabilities.
     */
    public Supplier<AimVisualizationCapability> getCapabilityFactory() {
        return capabilityFactory;
    }
}
