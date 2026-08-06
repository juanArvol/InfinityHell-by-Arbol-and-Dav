package Game.World.Core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Caché de mundos generados indexada por WorldCoordinator.
 *
 * ── HRFC: Preparación para eviction y mundos infinitos ───────────────────
 *
 * ANTES: WorldCache era un HashMap sin límite. Con exploración infinita,
 * la memoria crecía indefinidamente. No había forma de desalojar mundos
 * lejanos ni de configurar una política de retención.
 *
 * AHORA: WorldCache expone una EvictionPolicy inyectable. Si no se
 * configura una política, el comportamiento es idéntico al anterior
 * (sin límite, sin eviction) — retrocompatible.
 *
 * ── POLÍTICA DE EVICTION ─────────────────────────────────────────────────
 * Una EvictionPolicy decide qué mundos desalojar cuando se supera el
 * límite configurado. Implementaciones previstas:
 *
 *   LRUEvictionPolicy    → desaloja los menos recientemente usados
 *   DistanceEvictionPolicy → desaloja los más lejanos al jugador
 *   MaxSizeEvictionPolicy  → desaloja al superar N mundos en caché
 *
 * Configuración:
 *   cache.setEvictionPolicy(new LRUEvictionPolicy(16));  // máximo 16 mundos
 *
 * ── HOOK DE EVICTION ─────────────────────────────────────────────────────
 * onEvict es un callback opcional llamado cuando un mundo es desalojado.
 * Permite al código externo (WorldManager, audio, UI) reaccionar al
 * desalojo sin acoplarse al cache.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Sin cambios. WorldManager sigue siendo responsable de sincronizar
 * accesos externos con synchronized(cache).
 */
public class WorldCache {

    /**
     * Política de eviction inyectable.
     * null = sin eviction (comportamiento original ilimitado).
     */
    public interface EvictionPolicy {
        /**
         * Decide qué coordenadas desalojar dado el estado actual del cache.
         *
         * @param worlds    vista de solo lectura de los mundos actuales
         * @return colección de coordenadas a desalojar (puede ser vacía)
         */
        Collection<WorldCoordinator> selectForEviction(Map<WorldCoordinator, World> worlds);
    }

    // ── Estado ────────────────────────────────────────────────────────────

    private final Map<WorldCoordinator, World>  worlds     = new HashMap<>();
    private EvictionPolicy                       evictionPolicy = null;
    private BiConsumer<WorldCoordinator, World>  onEvict        = null;

    // ── Configuración ─────────────────────────────────────────────────────

    /**
     * Configura la política de eviction.
     * null = sin eviction (comportamiento por defecto, retrocompatible).
     */
    public void setEvictionPolicy(EvictionPolicy policy) {
        this.evictionPolicy = policy;
    }

    /**
     * Callback invocado cuando un mundo es desalojado del cache.
     * Útil para guardar estado, liberar recursos, actualizar UI.
     *
     * @param callback recibe (coord, world) del mundo desalojado.
     */
    public void setOnEvict(BiConsumer<WorldCoordinator, World> callback) {
        this.onEvict = callback;
    }

    // ── Operaciones de cache ──────────────────────────────────────────────

    /**
     * Añade un mundo al cache.
     * Si la política de eviction decide desalojar mundos, lo hace aquí.
     *
     * Comportamiento idéntico al anterior si no hay política configurada.
     */
    public void put(World world) {
        worlds.putIfAbsent(world.getCoordinate(), world);

        // Evaluar eviction después de añadir
        if (evictionPolicy != null) {
            Collection<WorldCoordinator> toEvict =
                evictionPolicy.selectForEviction(Collections.unmodifiableMap(worlds));
            for (WorldCoordinator coord : toEvict) {
                evict(coord);
            }
        }
    }

    public World get(WorldCoordinator coord) {
        return worlds.get(coord);
    }

    public boolean contains(WorldCoordinator coord) {
        return worlds.containsKey(coord);
    }

    /**
     * Desaloja un mundo explícitamente.
     * Invoca el callback onEvict si está configurado.
     */
    public void evict(WorldCoordinator coord) {
        World evicted = worlds.remove(coord);
        if (evicted != null && onEvict != null) {
            onEvict.accept(coord, evicted);
        }
    }

    /** Vista no modificable de todos los mundos en caché. */
    public Collection<World> getAllWorlds() {
        return Collections.unmodifiableCollection(worlds.values());
    }

    /** Vista no modificable del mapa completo (coord → world). */
    public Map<WorldCoordinator, World> getAllWorldsMap() {
        return Collections.unmodifiableMap(worlds);
    }

    public void clear() {
        if (onEvict != null) {
            worlds.forEach((coord, world) -> onEvict.accept(coord, world));
        }
        worlds.clear();
    }

    public int size() { return worlds.size(); }
}
