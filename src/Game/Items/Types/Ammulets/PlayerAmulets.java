package Game.Items.Types.Ammulets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inventario de amuletos del jugador para la run actual.
 *
 * ── MODELO ───────────────────────────────────────────────────────────────
 * Los amuletos se almacenan como una lista de IDs (con repetición permitida).
 * Si el jugador tiene 3x "Punta Ósea", la lista contiene tres veces "bone_tip".
 *
 * Esto permite que AmuletRegistry.applyAll() los itere y aplique N veces,
 * logrando el efecto acumulativo sin necesidad de lógica especial.
 *
 * ── INTEGRACIÓN ───────────────────────────────────────────────────────────
 * El jugador accede a sus amuletos via Player.getAmulets().
 * ModifiedWeapon llama AmuletRegistry.applyAll(player.getAmulets().getIds(), ...).
 *
 * Al terminar una run, resetear con clear().
 */
public class PlayerAmulets {

    /** Lista de IDs con repetición (un ID por copia del amuleto). */
    private final List<String> ids = new ArrayList<>();

    /** Conteo rápido por ID para la UI (no recalcula en cada frame). */
    private final Map<String, Integer> counts = new HashMap<>();

    /**
     * Añade una copia de un amuleto al inventario del jugador.
     * No limita la cantidad — infinitamente acumulable por diseño.
     */
    public void add(String amuletId) {
        ids.add(amuletId);
        counts.merge(amuletId, 1, Integer::sum);
    }

    /**
     * Lista de IDs para que AmuletRegistry.applyAll() la itere.
     * Incluye repeticiones — un ID por copia.
     */
    public List<String> getIds() {
        return Collections.unmodifiableList(ids);
    }

    /**
     * Cuántas copias tiene el jugador de un amuleto específico.
     * Útil para la UI de inventario.
     */
    public int countOf(String amuletId) {
        return counts.getOrDefault(amuletId, 0);
    }

    /** Total de amuletos (incluyendo copias). */
    public int totalCount() {
        return ids.size();
    }

    /** Limpia todos los amuletos (al terminar una run). */
    public void clear() {
        ids.clear();
        counts.clear();
    }

    /** Snapshot de cuántas copias hay de cada amuleto (para UI). */
    public Map<String, Integer> getCounts() {
        return Collections.unmodifiableMap(counts);
    }
}
