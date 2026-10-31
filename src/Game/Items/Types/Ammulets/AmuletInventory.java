package Game.Items.Types.Ammulets;

import Game.Engine.GameEventBus;
import Game.Items.Savement.Types.Inventory;

/**
 * Inventario de amuletos — autoridad del dominio Ammulets.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * HERENCIA:
 *   AmuletInventory extends Inventory<ItemDefinition>
 *   Implementa unicidad: cada amuleto solo puede poseerse una vez.
 *
 * RESPONSABILIDADES:
 *   • Almacenar definiciones de amuletos poseídos (sin duplicación)
 *   • Gestionar adquisición única por tipo mediante verificación en add()
 *   • Prevenir duplicados por verificación de ID
 *   • Exponer la lista para que AmuletEffectApplicator la itere
 *   • Llamar lifecycle hooks (onEquip/onUnequip) de los efectos
 *
 * SEPARACIÓN:
 *   AmuletInventory → qué amuletos posee el portador (únicos)
 *   AmuletType      → cómo se resuelven definiciones y comportamientos
 *
 * LIFECYCLE DE EVENTOS:
 *   Al añadir un amuleto → crea el effect y llama onEquip(eventBus)
 *   Al remover un amuleto → llama onUnequip() del effect
 *
 * NOTA:
 *   Almacena ItemDefinition (no AmuletType) porque los amuletos son
 *   efectos pasivos que se aplican automáticamente. No necesitamos
 *   el factory de AmuletType en el inventario.
 */
public final class AmuletInventory extends Inventory<AmuletType> {

    private final GameEventBus eventBus;

    /**
     * Constructor con GameEventBus para lifecycle hooks de efectos.
     *
     * @param maxSlots límite de amuletos únicos
     * @param eventBus bus de eventos para onEquip/onUnequip
     */
    public AmuletInventory(int maxSlots, GameEventBus eventBus) {
        super(maxSlots);
        this.eventBus = eventBus;
    }

    // ── Override add() con lógica de unicidad y lifecycle ─────────────────

    /**
     * Añade un amuleto al inventario.
     * Implementa UNICIDAD: si ya se posee (mismo ID), la operación es no-op.
     * Si se añade exitosamente, llama onEquip() del efecto.
     *
     * @param amulet definición del amuleto a añadir. No puede ser null.
     * @return true si se añadió (nueva adquisición), false si ya se poseía
     * @throws IllegalArgumentException si amulet es null
     */
    public boolean addAmulet(AmuletType amulet) {
        boolean added = addItem(amulet);
        
        if (added && eventBus != null) {
            // Crear el effect y llamar onEquip
            AmuletEffect effect = amulet.createEffect();
            effect.onEquip(eventBus);
        }
        
        return added;
    }

    /**
     * Remueve un amuleto del inventario y llama onUnequip().
     *
     * @param amulet amuleto a remover
     * @return true si se removió exitosamente
     */
    public boolean removeAmulet(AmuletType amulet) {
        boolean removed = removeItem(amulet);
        
        if (removed && eventBus != null) {
            // Crear el effect y llamar onUnequip
            AmuletEffect effect = amulet.createEffect();
            effect.onUnequip();
        }
        
        return removed;
    }

    /**
     * True si el portador posee el amuleto indicado (por ID).
     *
     * @param amulet definición del amuleto
     * @return true si se posee
     */
    public boolean hasAmulet(AmuletType amulet) {
        if (amulet == null) return false;
        
        for (AmuletType existing : inventoryItem) {
            if (existing.getItemId().equals(amulet.getItemId())) {
                return true;
            }
        }
        return containsItem(amulet);
    }
}
