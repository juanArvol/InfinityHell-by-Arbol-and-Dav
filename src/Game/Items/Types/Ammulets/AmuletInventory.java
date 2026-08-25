package Game.Items.Types.Ammulets;

import Game.Items.Creation.ItemDefinition;
import Game.Items.Savement.Inventory;

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
 *
 * SEPARACIÓN:
 *   AmuletInventory → qué amuletos posee el portador (únicos)
 *   AmuletType      → cómo se resuelven definiciones y comportamientos
 *
 * NOTA:
 *   Almacena ItemDefinition (no AmuletType) porque los amuletos son
 *   efectos pasivos que se aplican automáticamente. No necesitamos
 *   el factory de AmuletType en el inventario.
 */
public final class AmuletInventory extends Inventory<ItemDefinition> {

    /**
     * Constructor sin límite de slots.
     */
    public AmuletInventory() {
        super();
    }

    // ── Override add() con lógica de unicidad ─────────────────────────────

    /**
     * Añade un amuleto al inventario.
     * Implementa UNICIDAD: si ya se posee (mismo ID), la operación es no-op.
     *
     * @param amulet definición del amuleto a añadir. No puede ser null.
     * @return true si se añadió (nueva adquisición), false si ya se poseía
     * @throws IllegalArgumentException si amulet es null
     */
    public boolean addAmulet(ItemDefinition amulet) {
        
        return addItem(amulet);
    }

    /**
     * True si el portador posee el amuleto indicado (por ID).
     *
     * @param amulet definición del amuleto
     * @return true si se posee
     */
    public boolean hasAmulet(ItemDefinition amulet) {
        if (amulet == null) return false;
        
        for (ItemDefinition existing : inventoryItem) {
            if (existing.getItemId().equals(amulet.getItemId())) {
                return true;
            }
        }
        return contains(amulet);
    }
}
