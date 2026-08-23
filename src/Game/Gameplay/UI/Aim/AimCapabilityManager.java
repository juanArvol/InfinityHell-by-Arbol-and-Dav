package Game.Gameplay.UI.Aim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Gestor de capabilities de visualización de apuntado — mantiene la lista
 * de capabilities activas del portador.
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 *   • Almacenar capabilities activas concedidas por amuletos/items
 *   • Mantener orden de renderizado por prioridad
 *   • Prevenir duplicados de la misma clase de capability
 *   • Exponer lista ordenada para renderizado en HUD
 *
 * ── OWNERSHIP ─────────────────────────────────────────────────────────────
 *
 * Cada portador (Player, Enemy, Turret) puede tener su propio manager.
 * Las capabilities se conceden/remueven dinámicamente según items equipados.
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *   Player
 *     └── AimCapabilityManager
 *          └── List<AimVisualizationCapability> (ordenado por prioridad)
 *
 *   AmuletDefinition.onEquip(player):
 *     player.getAimCapabilities().add(new TrajectoryVisualizationCapability())
 *
 *   CrossHairHUD.draw():
 *     for (capability in player.getAimCapabilities().getAll())
 *       capability.render(g, player, camX, camY)
 *
 * ── ORDEN DE RENDERIZADO ──────────────────────────────────────────────────
 *
 * Las capabilities se ordenan automáticamente por getRenderPriority():
 *   0-20:  Capas de fondo (range indicator)
 *   20-40: Crosshair base
 *   40-60: Trayectorias y predicciones
 *   60-80: Overlays de información
 *   80-100: Efectos especiales
 */
public final class AimCapabilityManager {

    private final List<AimVisualizationCapability> capabilities = new ArrayList<>();
    private boolean needsSort = false;

    // ── Gestión de capabilities ───────────────────────────────────────────

    /**
     * Añade una capability al manager.
     * Si ya existe una capability de la misma clase, la operación es idempotente (no-op).
     *
     * @param capability capability a añadir. No puede ser null.
     * @return true si se añadió, false si ya existía
     * @throws IllegalArgumentException si capability es null
     */
    public boolean add(AimVisualizationCapability capability) {
        if (capability == null) {
            throw new IllegalArgumentException("capability no puede ser null");
        }

        // Verificar duplicidad por clase
        for (AimVisualizationCapability existing : capabilities) {
            if (existing.getClass().equals(capability.getClass())) {
                return false; // Ya se posee
            }
        }

        capabilities.add(capability);
        needsSort = true;
        return true;
    }

    /**
     * Elimina una capability del manager.
     *
     * @param capability capability a eliminar
     * @return true si se eliminó, false si no se encontró
     */
    public boolean remove(AimVisualizationCapability capability) {
        return capabilities.remove(capability);
    }

    /**
     * Elimina todas las capabilities de una clase específica.
     *
     * @param capabilityClass clase de capability a eliminar
     * @return número de capabilities eliminadas
     */
    public int removeByClass(Class<? extends AimVisualizationCapability> capabilityClass) {
        int removed = 0;
        for (int i = capabilities.size() - 1; i >= 0; i--) {
            if (capabilityClass.isInstance(capabilities.get(i))) {
                capabilities.remove(i);
                removed++;
            }
        }
        return removed;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Lista ordenada de capabilities activas por prioridad de renderizado.
     * Orden: menor prioridad → mayor prioridad (renderiza en ese orden).
     *
     * @return lista ordenada. Nunca null, puede estar vacía.
     */
    public List<AimVisualizationCapability> getAll() {
        if (needsSort) {
            capabilities.sort(Comparator.comparingInt(AimVisualizationCapability::getRenderPriority));
            needsSort = false;
        }
        return Collections.unmodifiableList(capabilities);
    }

    /**
     * True si el portador posee una capability de la clase indicada.
     *
     * @param capabilityClass clase de capability a verificar
     * @return true si se posee
     */
    public boolean has(Class<? extends AimVisualizationCapability> capabilityClass) {
        for (AimVisualizationCapability cap : capabilities) {
            if (capabilityClass.isInstance(cap)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Total de capabilities activas.
     */
    public int size() {
        return capabilities.size();
    }

    /**
     * True si no hay capabilities activas.
     */
    public boolean isEmpty() {
        return capabilities.isEmpty();
    }

    // ── Limpieza ──────────────────────────────────────────────────────────

    /**
     * Limpia todas las capabilities — útil al terminar una run o en testing.
     */
    public void clear() {
        capabilities.clear();
        needsSort = false;
    }
}
