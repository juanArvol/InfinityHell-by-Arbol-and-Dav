package Game.Player;

import Game.Gameplay.UI.Aim.AimCapabilityManager;
import Game.Items.Core.ObjectTypeFactory;
import Game.Items.Creation.ItemDefinition;
import Game.Items.Types.Ammulets.AmuletInventory;
import Game.Items.Types.Ammulets.AmuletType;
import Game.Items.Types.Ammulets.Effects.UICapabilityEffect;

/**
 * Manejador de equipamiento de amuletos del Player — aplica efectos de UI
 * cuando se equipa/desequipa un amuleto.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 *   • Detectar cuando se añade un amuleto al inventario
 *   • Si el amuleto concede UICapabilityEffect, añadir capability al Player
 *   • Si el amuleto se remueve, quitar capability correspondiente
 *   • Mantener sincronización entre AmuletInventory y AimCapabilityManager
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *   AmuletInventory.add(amulet)
 *          │
 *          ▼
 *   PlayerAmuletEquipHandler.onAmuletEquipped(amulet, player)
 *          │
 *          ├── si effect es UICapabilityEffect:
 *          │   └── capability = effect.createCapability()
 *          │   └── player.getAimCapabilities().add(capability)
 *          │
 *          └── si effect es otro tipo:
 *              └── aplicar via AmuletRegistry.applyAll()
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   AmuletInventory        → almacena amuletos poseídos
 *   AmuletRegistry         → aplica efectos a armas/proyectiles
 *   PlayerAmuletEquipHandler → aplica efectos de UI al Player
 *   AimCapabilityManager   → gestiona capabilities activas
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Al obtener amuleto (desde loot, tienda, etc.):
 *   ItemDefinition amulet = AmuletRegistry.get("marksman_sight");
 *   if (player.getInventory().amulets().add(amulet)) {
 *       PlayerAmuletEquipHandler.onAmuletEquipped(amulet, player);
 *   }
 *
 *   // Al remover amuleto (drop, venta, etc.):
 *   if (player.getInventory().amulets().remove(amulet)) {
 *       PlayerAmuletEquipHandler.onAmuletUnequipped(amulet, player);
 *   }
 */
public final class PlayerAmuletEquipHandler {

    private PlayerAmuletEquipHandler() {
        // Utility class — no instanciar
    }

    /**
     * Procesa el equipamiento de un amuleto.
     * Aplica efectos de UI si el amuleto concede capabilities.
     *
     * @param amulet amuleto que se acaba de equipar
     * @param player jugador que equipa el amuleto
     */
    public static void onAmuletEquipped(ItemDefinition amulet, Player player) {
        if (amulet == null || player == null) return;

        // Resolver el AmuletType desde la definición
        AmuletType amuletType = ObjectTypeFactory.find(
            AmuletType.class, 
            amulet.getItemId()
        );
        
        if (amuletType == null) return;
        
        // Crear una instancia del efecto
        var effect = amuletType.createEffect();

        // Detectar si el efecto concede capabilities de UI
        if (effect instanceof UICapabilityEffect uiEffect) {
            AimCapabilityManager aimCapabilities = player.getAimCapabilities();
            if (aimCapabilities != null) {
                // Crear y añadir la capability
                aimCapabilities.add(uiEffect.createCapability());
            }
        }

        // Otros efectos (stats, behavior) se aplican automáticamente via
        // AmuletRegistry.applyAll() en el pipeline de disparo de ModifiedWeapon.
        // No necesitan procesamiento aquí.
    }

    /**
     * Procesa el desequipamiento de un amuleto.
     * Remueve efectos de UI si el amuleto concedía capabilities.
     *
     * @param amulet amuleto que se acaba de desequipar
     * @param player jugador que desequipa el amuleto
     */
    public static void onAmuletUnequipped(ItemDefinition amulet, Player player) {
        if (amulet == null || player == null) return;

        // Resolver el AmuletType desde la definición
        AmuletType amuletType = ObjectTypeFactory.find(
            AmuletType.class, 
            amulet.getItemId()
        );
        
        if (amuletType == null) return;
        
        // Crear una instancia del efecto
        var effect = amuletType.createEffect();

        // Detectar si el efecto concede capabilities de UI
        if (effect instanceof UICapabilityEffect uiEffect) {
            AimCapabilityManager aimCapabilities = player.getAimCapabilities();
            if (aimCapabilities != null) {
                // Obtener la clase de capability y removerla
                // Nota: esto asume que cada amuleto concede una capability única
                // Si un amuleto concede múltiples capabilities, necesitaremos
                // trackear qué capabilities concede cada amuleto
                var capability = uiEffect.createCapability();
                aimCapabilities.removeByClass(capability.getClass());
            }
        }
    }

    /**
     * Sincroniza todas las capabilities de UI con el inventario de amuletos actual.
     * Útil para restaurar estado al cargar una partida guardada.
     *
     * @param player jugador cuyas capabilities sincronizar
     */
    public static void syncAllCapabilities(Player player) {
        if (player == null) return;

        AmuletInventory amulets = player.getAmulets();
        AimCapabilityManager aimCapabilities = player.getAimCapabilities();

        if (amulets == null || aimCapabilities == null) return;

        // Limpiar capabilities actuales
        aimCapabilities.clear();

        // Re-aplicar todas las capabilities desde el inventario
        for (AmuletType amulet : amulets.getAll()) {
            // Resolver el AmuletType desde la definición
            AmuletType amuletType = ObjectTypeFactory.find(
                AmuletType.class, 
                amulet.getItemId()
            );
            
            if (amuletType == null) continue;
            
            // Crear una instancia del efecto
            var effect = amuletType.createEffect();
            
            if (effect instanceof UICapabilityEffect uiEffect) {
                aimCapabilities.add(uiEffect.createCapability());
            }
        }
    }
}
