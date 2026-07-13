package Game.World.Core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Caché de mundos generados indexada por coordenada.
 *
 * REFACTOR: List → HashMap<WorldCoordinator, World>
 *
 * PROBLEMA ANTERIOR:
 *   La implementación usaba un ArrayList y recorría todos los elementos en
 *   get() y contains(). Con pocos mundos era imperceptible, pero a medida
 *   que el jugador explora regiones lejanas la caché crece y cada lookup
 *   en WorldManager (getCurrentWorld, prewarmNeighbors, processTransitions)
 *   degrada de O(1) esperado a O(n).
 *
 *   WorldCoordinator ya implementa equals() y hashCode() correctamente
 *   (usando Objects.hash(x, y)), por lo que el cambio es trivial y seguro.
 *
 * SOLUCIÓN:
 *   HashMap<WorldCoordinator, World> — get() y contains() son O(1) constante
 *   independientemente de cuántos mundos haya en caché.
 *
 * COMPATIBILIDAD:
 *   La API pública (put, get, contains, clear, getAllWorlds) no cambia.
 *   Solo la implementación interna.
 *
 * THREAD SAFETY:
 *   Sin cambios. WorldManager sigue siendo responsable de sincronizar accesos
 *   externos con synchronized(cache). HashMap no es thread-safe por sí solo;
 *   el contrato existente se mantiene.
 */
public class WorldCache {

    private final Map<WorldCoordinator, World> worlds = new HashMap<>();

    public void put(World world) {
        worlds.putIfAbsent(world.getCoordinate(), world);
    }

    public World get(WorldCoordinator coord) {
        return worlds.get(coord);
    }

    public boolean contains(WorldCoordinator coord) {
        return worlds.containsKey(coord);
    }

    /** Vista no modificable de todos los mundos en caché. */
    public Collection<World> getAllWorlds() {
        return Collections.unmodifiableCollection(worlds.values());
    }

    public void clear() {
        worlds.clear();
    }
}
