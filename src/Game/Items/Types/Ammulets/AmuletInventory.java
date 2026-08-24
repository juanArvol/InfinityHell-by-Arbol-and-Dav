package Game.Items.Types.Ammulets;

import Game.Items.Savement.Inventory;

/**
 * Inventario de amuletos — autoridad del dominio Ammulets.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * HERENCIA:
 *   AmuletInventory extends Inventory<AmuletDefinition>
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
 *   AmuletRegistry  → cómo se resuelven definiciones y comportamientos
 */
public final class AmuletInventory extends Inventory<AmuletDefinition> {

    /**
     * Añade un amuleto al inventario.
     * Implementa UNICIDAD: si ya se posee (mismo ID), la operación es no-op.
     *
     * @param amulet definición del amuleto a añadir. No puede ser null.
     * @return true si se añadió (nueva adquisición), false si ya se poseía
     * @throws IllegalArgumentException si amulet es null
     */
    @Override
    public boolean add(AmuletDefinition amulet) {
        if (amulet == null)
            throw new IllegalArgumentException("amulet no puede ser null");
        
        // Verificar duplicidad por ID
        for (AmuletDefinition existing : items) {
            if (existing.getItemId().equals(amulet.getItemId())) {
                return false; // Ya se posee
            }
        }
        
        items.add(amulet);
        return true;
    }

    /**
     * True si el portador posee el amuleto indicado (por ID).
     *
     * @param amulet definición del amuleto
     * @return true si se posee
     */
    public boolean has(AmuletDefinition amulet) {
        if (amulet == null) return false;
        
        for (AmuletDefinition existing : items) {
            if (existing.getItemId().equals(amulet.getItemId())) {
                return true;
            }
        }
        return false;
    }
}
