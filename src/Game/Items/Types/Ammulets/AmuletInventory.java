package Game.Items.Types.Ammulets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 *         └── posee IDs de amuletos (con repetición permitida)
 *                       │
 *                       ▼
 *               AmuletRegistry
 *                       │
 *                       ▼
 *             definición / comportamiento
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 *   • Almacenar IDs de amuletos poseídos (con repetición)
 *   • Gestionar adquisición de amuletos
 *   • Mantener conteo por ID para la UI
 *   • Exponer la lista de IDs para que AmuletRegistry.applyAll() la itere
 *
 * ── MODELO ────────────────────────────────────────────────────────────────
 *
 * Los amuletos se almacenan como una lista de IDs (con repetición permitida).
 * Si el jugador tiene 3x "Punta Ósea", la lista contiene tres veces "bone_tip".
 *
 * Esto permite que AmuletRegistry.applyAll() los itere y aplique N veces,
 * logrando el efecto acumulativo sin necesidad de lógica especial.
 *
 * Los amuletos son infinitamente acumulables por diseño.
 *
 * ── SEPARACIÓN AmuletInventory ≠ AmuletRegistry ──────────────────────────
 *
 *   AmuletInventory → qué amuletos posee el portador
 *   AmuletRegistry  → cómo se resuelven definiciones y comportamientos
 *
 * AmuletInventory NO aplica efectos. Solo almacena posesión.
 *
 * ── INTEGRACIÓN ──────────────────────────────────────────────────────────
 *
 * ModifiedWeapon llama AmuletRegistry.applyAll(amulets.getIds(), stats, behavior)
 * para aplicar efectos acumulativos durante el disparo.
 */
public final class AmuletInventory {

    /** Lista de IDs con repetición (un ID por copia del amuleto). */
    private final List<String> ids = new ArrayList<>();

    /** Conteo rápido por ID para la UI (evita recalcular en cada frame). */
    private final Map<String, Integer> counts = new HashMap<>();

    // ── Gestión de amuletos ───────────────────────────────────────────────

    /**
     * Añade una copia de un amuleto al inventario.
     * Los amuletos son infinitamente acumulables por diseño.
     *
     * @param amuletId ID del amuleto a añadir. No puede ser null.
     * @throws IllegalArgumentException si amuletId es null
     */
    public void add(String amuletId) {
        if (amuletId == null) throw new IllegalArgumentException("amuletId no puede ser null");
        ids.add(amuletId);
        counts.merge(amuletId, 1, Integer::sum);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Lista de IDs para que AmuletRegistry.applyAll() la itere.
     * Incluye repeticiones — un ID por copia del amuleto.
     *
     * @return lista inmutable de IDs. Nunca null.
     */
    public List<String> getIds() {
        return Collections.unmodifiableList(ids);
    }

    /**
     * Cuántas copias tiene el portador de un amuleto específico.
     * Útil para la UI de inventario.
     *
     * @param amuletId ID del amuleto
     * @return cantidad de copias (0 si no se posee)
     */
    public int countOf(String amuletId) {
        return counts.getOrDefault(amuletId, 0);
    }

    /**
     * True si el portador posee al menos una copia del amuleto indicado.
     *
     * @param amuletId ID del amuleto
     * @return true si se posee
     */
    public boolean has(String amuletId) {
        return counts.containsKey(amuletId);
    }

    /** Total de amuletos (incluyendo copias). */
    public int totalCount() {
        return ids.size();
    }

    /** True si no se poseen amuletos. */
    public boolean isEmpty() {
        return ids.isEmpty();
    }

    /**
     * Snapshot de cuántas copias hay de cada amuleto.
     * Útil para la UI de inventario.
     *
     * @return mapa inmutable de ID → cantidad
     */
    public Map<String, Integer> getCounts() {
        return Collections.unmodifiableMap(counts);
    }

    // ── Limpieza ──────────────────────────────────────────────────────────

    /**
     * Limpia todos los amuletos — útil al terminar una run o en testing.
     */
    public void clear() {
        ids.clear();
        counts.clear();
    }
}
