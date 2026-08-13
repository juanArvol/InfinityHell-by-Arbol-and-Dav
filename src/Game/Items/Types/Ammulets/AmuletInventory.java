package Game.Items.Types.Ammulets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inventario de amuletos — autoridad del dominio Ammulets.
 *
 * ── HRFC — Player Inventory & Domain Ownership Consolidation ──────────────
 *
 * ── OWNERSHIP ─────────────────────────────────────────────────────────────
 *
 * AmuletInventory pertenece al dominio Ammulets y responde la pregunta:
 *   "¿Qué amuletos posee el portador?"
 *
 * AmuletRegistry responde: "¿Cómo se resuelven definiciones y comportamientos?"
 *
 * La relación correcta es:
 *
 *   AmuletInventory
 *         │
 *         └── posee AmuletDefinitions únicas (sin repetición)
 *                       │
 *                       ▼
 *               AmuletRegistry
 *                       │
 *                       ▼
 *             definición / comportamiento
 *
 * ── CAMBIO DE MODELO ──────────────────────────────────────────────────────
 *
 * ANTES (acumulativo):
 *   List<String> ids + Map<String, Integer> counts
 *   bone_tip x3, swift_quill x2
 *
 * AHORA (único por tipo):
 *   List<AmuletDefinition> amulets
 *   bone_tip (una vez), swift_quill (una vez)
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 *   • Almacenar definiciones de amuletos poseídos (sin duplicación)
 *   • Gestionar adquisición única por tipo mediante verificación en add()
 *   • Prevenir duplicados por verificación de ID
 *   • Exponer la lista de AmuletDefinitions para que AmuletRegistry.applyAll() la itere
 *
 * ── SEPARACIÓN AmuletInventory ≠ AmuletRegistry ──────────────────────────
 *
 *   AmuletInventory → qué amuletos posee el portador (únicos)
 *   AmuletRegistry  → cómo se resuelven definiciones y comportamientos
 *
 * AmuletInventory NO aplica efectos. Solo almacena posesión única.
 *
 * ── INTEGRACIÓN ──────────────────────────────────────────────────────────
 *
 * ModifiedWeapon llama AmuletRegistry.applyAll(amulets.getAll(), stats, behavior)
 * Los efectos se aplican una vez por tipo de amuleto, no acumulativamente.
 */
public final class AmuletInventory {

    /** Definiciones de amuletos poseídos — unicidad garantizada por verificación en add(). */
    private final List<AmuletDefinition> amulets = new ArrayList<>();

    // ── Gestión de amuletos ───────────────────────────────────────────────

    /**
     * Añade un amuleto al inventario.
     * Si ya se posee (misma referencia o mismo ID), la operación es idempotente (no-op).
     *
     * @param amulet definición del amuleto a añadir. No puede ser null.
     * @return true si se añadió (nueva adquisición), false si ya se poseía
     * @throws IllegalArgumentException si amulet es null
     */
    public boolean add(AmuletDefinition amulet) {
        if (amulet == null) throw new IllegalArgumentException("amulet no puede ser null");
        
        // Verificar duplicidad por ID (ya que AmuletDefinition no implementa equals)
        for (AmuletDefinition existing : amulets) {
            if (existing.id.equals(amulet.id)) {
                return false; // Ya se posee
            }
        }
        
        amulets.add(amulet);
        return true;
    }



    /**
     * Elimina un amuleto del inventario.
     *
     * @param amulet definición del amuleto a eliminar
     * @return true si se eliminó, false si no se encontró
     */
    public boolean remove(AmuletDefinition amulet) {
        return amulets.remove(amulet);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Lista inmutable de definiciones de amuletos poseídos.
     *
     * @return lista de definiciones. Nunca null, puede estar vacía.
     */
    public List<AmuletDefinition> getAll() {
        return Collections.unmodifiableList(amulets);
    }

    /**
     * True si el portador posee el amuleto indicado.
     *
     * @param amulet definición del amuleto
     * @return true si se posee
     */
    public boolean has(AmuletDefinition amulet) {
        if (amulet == null) return false;
        
        // Verificar por ID ya que AmuletDefinition no implementa equals
        for (AmuletDefinition existing : amulets) {
            if (existing.id.equals(amulet.id)) {
                return true;
            }
        }
        return false;
    }

    /** Total de amuletos únicos poseídos. */
    public int size() {
        return amulets.size();
    }

    /** True si no se poseen amuletos. */
    public boolean isEmpty() {
        return amulets.isEmpty();
    }

    // ── Limpieza ──────────────────────────────────────────────────────────

    /**
     * Limpia todos los amuletos — útil al terminar una run o en testing.
     */
    public void clear() {
        amulets.clear();
    }
}
